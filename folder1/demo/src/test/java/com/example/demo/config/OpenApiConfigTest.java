package com.example.demo.config;

import static org.junit.jupiter.api.Assertions.*;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void jsonToCsvOpenApi_hasExpectedTitleAndDescription() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.jsonToCsvOpenApi();
        Info info = openAPI.getInfo();

        assertThat(info).isNotNull();
        assertThat(info.getTitle()).isEqualTo("JSON-to-CSV Kafka Streams API");
        assertThat(info.getVersion()).isEqualTo("1.0.0");
        assertThat(info.getDescription()).contains("JSON", "CSV", "Kafka Streams");
    }
}
