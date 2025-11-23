package com.example.demo.producer;

import com.example.demo.config.KafkaTopicsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

@Service
public class JsonFileKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(JsonFileKafkaProducer.class);
    private static final String JSON_RESOURCE = "jsonInput.json";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicsProperties topicsProperties;

    public JsonFileKafkaProducer(KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper,
                                 KafkaTopicsProperties topicsProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicsProperties = topicsProperties;
    }

    /**
     * Reads the given JSON file from the application classpath and publishes its content
     * to the {@code json-input} Kafka topic.
     *
     * <p>Behavior:
     * <ul>
     *   <li>If the JSON root is an array, each element is published as an individual Kafka message.</li>
     *   <li>If the JSON root is a single object, it is published as one Kafka message.</li>
     * </ul>
     *
     * @param fileName the JSON file name located in the classpath (e.g. {@code jsonInput.json})
     * @return the number of Kafka messages successfully produced
     */
    public int sendFileToTopic(String fileName) {

        String topic = topicsProperties.getJsonInput();

        try {
            String jsonContent = readResourceAsString(JSON_RESOURCE);
            JsonNode rootNode = objectMapper.readTree(jsonContent);

            if (rootNode.isArray()) {
                int count = 0;
                for (JsonNode element : rootNode) {
                    String payload = objectMapper.writeValueAsString(element);
                    sendSync(topic, null, payload);
                    count++;
                }
                log.info("Produced {} messages to topic {}", count, topic);
                return count;
            } else {
                String payload = objectMapper.writeValueAsString(rootNode);
                sendSync(topic, null, payload);
                log.info("Produced 1 message to topic {}", topic);
                return 1;
            }

        } catch (IOException e) {
            log.error("Failed to read or parse resource {}", JSON_RESOURCE, e);
            throw new IllegalStateException("Could not read JSON resource", e);
        }
    }

    private String readResourceAsString(String resourceName) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourceName);
        try (InputStream is = resource.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private void sendSync(String topic, String key, String value) {
        try {
            RecordMetadata metadata = kafkaTemplate.send(topic, key, value).get().getRecordMetadata();
            log.debug("Sent record to {}-{}@offset {}",
                    metadata.topic(), metadata.partition(), metadata.offset());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sending to Kafka", ie);
        } catch (ExecutionException ee) {
            throw new IllegalStateException("Failed to send record to Kafka", ee);
        }
    }
}
