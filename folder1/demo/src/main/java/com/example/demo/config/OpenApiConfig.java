package com.example.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jsonToCsvOpenApi() {

        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("JSON-to-CSV Kafka Streams API")
                        .description("Spring Boot + Kafka Streams application that reads JSON from Kafka, flattens array fields, and writes CSVs.")
                        .version("1.0.0")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0"))
                        .contact(new Contact()
                                .name("JSON-to-CSV Service")
                                .email("noreply@example.com")));
    }
}
