package com.example.demo.service;


import com.example.demo.config.CsvProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CsvRecordProcessorTest {
    private CsvRecordProcessor processor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        CsvProperties csvProperties = new CsvProperties();
        csvProperties.setOutputDir(tempDir.toString());

        processor = new CsvRecordProcessor(new ObjectMapper(), csvProperties);
        processor.init();
    }


    @Test
    void processJsonRecord_writesCsvFilesPerArrayField() throws IOException {
        String json = """
                {
                  "name": "John Doe",
                  "age": 30,
                  "employees": [
                    { "id": 101, "department": "Engineering", "salary": 75000 },
                    { "id": 102, "department": "Marketing",  "salary": 65000 }
                  ],
                  "projects": [
                    { "project_id": "P001", "status": "completed" },
                    { "project_id": "P002", "status": "in_progress" }
                  ]
                }
                """;

        processor.processJsonRecord(json);

        Path employeesCsv = tempDir.resolve("employees.csv");
        Path projectsCsv  = tempDir.resolve("projects.csv");

        assertThat(employeesCsv).exists().isRegularFile();
        assertThat(projectsCsv).exists().isRegularFile();

        String employeesContent = Files.readString(employeesCsv);
        String projectsContent  = Files.readString(projectsCsv);

        // Headers
        assertThat(employeesContent.lines().findFirst().orElse(""))
                .isEqualTo("name,age,id,department,salary");

        assertThat(projectsContent.lines().findFirst().orElse(""))
                .isEqualTo("name,age,project_id,status");

        // Rows (order-sensitive but deterministic)
        assertThat(employeesContent).contains("John Doe,30,101,Engineering,75000");
        assertThat(employeesContent).contains("John Doe,30,102,Marketing,65000");

        assertThat(projectsContent).contains("John Doe,30,P001,completed");
        assertThat(projectsContent).contains("John Doe,30,P002,in_progress");
    }

    @Test
    void processJsonRecord_withNoArrays_doesNotCreateCsvFiles() {
        String json = """
                {
                  "id": 1,
                  "name": "No Arrays Here"
                }
                """;

        processor.processJsonRecord(json);

        // directory should be empty
        assertThat(tempDir.toFile().listFiles())
                .isEmpty();
    }
    @Test
    void flattenMap_flattensNestedObjectsWithUnderscoreSeparator() {
        Map<String, Object> input = Map.of(
                "name", "John",
                "address", Map.of(
                        "street", "Main St",
                        "city", "London",
                        "geo", Map.of("lat", 1.23, "lon", 4.56)
                )
        );

        Map<String, Object> flattened = processor.flattenMap(input);

        assertThat(flattened)
                .containsEntry("name", "John")
                .containsEntry("address_street", "Main St")
                .containsEntry("address_city", "London")
                .containsEntry("address_geo_lat", 1.23)
                .containsEntry("address_geo_lon", 4.56);
    }

    @Test
    void extractNonListFields_excludesListsAndMaps() {

            Map<String, Object> input = Map.of(
                    "id", 1,
                    "name", "John",
                    "tags", List.of("a", "b"),
                    "meta", Map.of("inner", "value")
            );

            Map<String, Object> nonList = processor.extractNonListFields(input);

            assertThat(nonList)
                    .containsEntry("id", 1)
                    .containsEntry("name", "John")
                    .containsEntry("meta_inner", "value")   // ✅ flattened nested scalar
                    .doesNotContainKeys("tags");            // ✅ list is excluded
        }


        @Test
    void extractArrayFields_findsTopLevelAndNestedArrays() {

        Map<String, Object> input = Map.of(
                "name", "John",
                "employees", List.of(
                        Map.of("id", 1, "dept", "A"),
                        Map.of("id", 2, "dept", "B")
                ),
                "details", Map.of(
                        "projects", List.of(
                                Map.of("code", "P1"),
                                Map.of("code", "P2")
                        )
                )
        );

        Map<String, java.util.List<Map<String, Object>>> arrays = processor.extractArrayFields(input);

        assertThat(arrays)
                .containsKeys("employees", "details_projects");

        assertThat(arrays.get("employees"))
                .hasSize(2)
                .allSatisfy(item -> assertThat(item).containsKeys("id", "dept"));

        assertThat(arrays.get("details_projects"))
                .hasSize(2)
                .allSatisfy(item -> assertThat(item).containsKey("code"));
    }
}