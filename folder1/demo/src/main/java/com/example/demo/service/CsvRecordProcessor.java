package com.example.demo.service;

import com.example.demo.config.CsvProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class CsvRecordProcessor {

    private static final Logger log = LoggerFactory.getLogger(CsvRecordProcessor.class);

    private final ObjectMapper objectMapper;
    private final CsvProperties csvProperties;

    // fileName -> ordered headers
    private final ConcurrentMap<String, List<String>> headersByFile = new ConcurrentHashMap<>();
    // set of file names whose headers have already been written
    private final Set<String> headersWritten = ConcurrentHashMap.newKeySet();

    private Path outputDir;

    public CsvRecordProcessor(ObjectMapper objectMapper, CsvProperties csvProperties) {
        this.objectMapper = objectMapper;
        this.csvProperties = csvProperties;
    }

    @PostConstruct
    public void init() {
        this.outputDir = Paths.get(csvProperties.getOutputDir());
        try {
            Files.createDirectories(outputDir);
            log.info("CSV output directory: {}", outputDir.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create output directory {}", outputDir, e);
            throw new IllegalStateException("Cannot create output directory", e);
        }
    }

    /**
     * Entry point from Kafka Streams.
     */
    public void processJsonRecord(String jsonRecord) {
        if (jsonRecord == null || jsonRecord.isBlank()) {
            log.warn("Received empty JSON record, skipping");
            return;
        }

        try {
            Map<String, Object> data = objectMapper.readValue(
                    jsonRecord,
                    new TypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> nonListFields = extractNonListFields(data);
            Map<String, List<Map<String, Object>>> arrayFields = extractArrayFields(data);

            if (arrayFields.isEmpty()) {
                log.info("No array fields found in record, nothing to write");
                return;
            }

            log.debug("Non-list fields: {}", nonListFields.keySet());
            log.debug("Array fields: {}", arrayFields.keySet());

            for (Map.Entry<String, List<Map<String, Object>>> entry : arrayFields.entrySet()) {
                String arrayFieldName = entry.getKey();
                List<Map<String, Object>> arrayItems = entry.getValue();

                if (arrayItems == null || arrayItems.isEmpty()) {
                    log.debug("Skipping empty array field: {}", arrayFieldName);
                    continue;
                }

                writeArrayToCsv(arrayFieldName, nonListFields, arrayItems);
            }

        } catch (Exception e) {
            log.error("Failed to process JSON record: {}", e.getMessage(), e);
        }
    }

    // ---------- JSON flattening & extraction ----------

    /**
     * Fully flattens nested maps using an underscore separator.
     * Example: {"a":{"b":1}} -> {"a_b":1}
     */
    public Map<String, Object> flattenMap(Map<String, Object> map) {
        return flattenMap(map, "", "_");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> flattenMap(Map<String, Object> map, String parentKey, String separator) {
        Map<String, Object> flattened = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = parentKey.isEmpty()
                    ? entry.getKey()
                    : parentKey + separator + entry.getKey();

            Object value = entry.getValue();

            if (value instanceof Map<?, ?> nested) {
                flattened.putAll(flattenMap((Map<String, Object>) nested, key, separator));
            } else {
                flattened.put(key, value);
            }
        }

        return flattened;
    }

    /**
     * Returns all non-list, non-map fields from a flattened view of the JSON.
     */
    public Map<String, Object> extractNonListFields(Map<String, Object> data) {

        Map<String, Object> flattened = flattenMap(data);
        Map<String, Object> nonListFields = new LinkedHashMap<>();

        flattened.forEach((key, value) -> {
            if (!(value instanceof List) && !(value instanceof Map)) {
                nonListFields.put(key, value);
            }
        });

        return nonListFields;
    }

    /**
     * Recursively finds array/list fields, flattens each item, and returns:
     *   "fieldName" -> [ {flattened item 1}, {flattened item 2}, ... ]
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<Map<String, Object>>> extractArrayFields(Map<String, Object> data) {

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        extractArrayFieldsRecursive(data, "", result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private void extractArrayFieldsRecursive(
            Map<String, Object> current,
            String parentKey,
            Map<String, List<Map<String, Object>>> accumulator
    ) {
        for (Map.Entry<String, Object> entry : current.entrySet()) {
            String key = parentKey.isEmpty()
                    ? entry.getKey()
                    : parentKey + "_" + entry.getKey();

            Object value = entry.getValue();

            if (value instanceof List<?> list) {
                List<Map<String, Object>> items = new ArrayList<>();

                for (Object item : list) {
                    if (item instanceof Map<?, ?> itemMap) {
                        Map<String, Object> flattened = flattenMap((Map<String, Object>) itemMap);
                        items.add(flattened);
                    } else {
                        // Primitive or simple type
                        Map<String, Object> simple = new LinkedHashMap<>();
                        simple.put("value", item);
                        items.add(simple);
                    }
                }

                accumulator.put(key, items);
            } else if (value instanceof Map<?, ?> nested) {
                extractArrayFieldsRecursive(
                        (Map<String, Object>) nested,
                        key,
                        accumulator
                );
            }
        }
    }

    // ---------- CSV writing ----------

    private void writeArrayToCsv(
            String arrayFieldName,
            Map<String, Object> nonListFields,
            List<Map<String, Object>> arrayItems
    ) {
        List<String> headers = headersByFile.computeIfAbsent(arrayFieldName, key -> {
            List<String> headerList = new ArrayList<>(nonListFields.keySet());

            // Use first array item to derive array headers
            Map<String, Object> firstItem = arrayItems.get(0);
            for (String itemKey : firstItem.keySet()) {
                if (!headerList.contains(itemKey)) {
                    headerList.add(itemKey);
                }
            }

            return Collections.unmodifiableList(headerList);
        });

        Path csvPath = outputDir.resolve(arrayFieldName + ".csv");

        boolean shouldWriteHeader = headersWritten.add(arrayFieldName);

        try (BufferedWriter writer = Files.newBufferedWriter(
                csvPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            if (shouldWriteHeader) {
                writeHeaderLine(writer, headers);
            }

            for (Map<String, Object> arrayItem : arrayItems) {
                writeRow(writer, headers, nonListFields, arrayItem);
            }

        } catch (IOException e) {
            log.error("Failed to write CSV for array field {} at {}",
                    arrayFieldName, csvPath.toAbsolutePath(), e);
        }
    }

    private void writeHeaderLine(BufferedWriter writer, List<String> headers) throws IOException {
        String line = String.join(",", escapeCsvFields(headers));
        writer.write(line);
        writer.newLine();
    }

    private void writeRow(
            BufferedWriter writer,
            List<String> headers,
            Map<String, Object> nonListFields,
            Map<String, Object> arrayItem
    ) throws IOException {
        List<String> values = new ArrayList<>(headers.size());

        for (String header : headers) {
            Object value;
            if (nonListFields.containsKey(header)) {
                value = nonListFields.get(header);
            } else {
                value = arrayItem.getOrDefault(header, "");
            }
            values.add(escapeCsvField(value));
        }

        String line = String.join(",", values);
        writer.write(line);
        writer.newLine();
    }

    private List<String> escapeCsvFields(List<String> fields) {
        List<String> escaped = new ArrayList<>(fields.size());
        for (String field : fields) {
            escaped.add(escapeCsvField(field));
        }
        return escaped;
    }

    private String escapeCsvField(Object field) {
        if (field == null) {
            return "";
        }

        String s = field.toString();

        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }

        return s;
    }
}
