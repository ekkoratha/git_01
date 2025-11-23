package com.example.demo.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertiesPojoTest {

    @Test
    void csvProperties_gettersAndSettersWork() {
        CsvProperties props = new CsvProperties();
        props.setOutputDir("my-output");

        assertThat(props.getOutputDir()).isEqualTo("my-output");
    }

    @Test
    void kafkaTopicsProperties_gettersAndSettersWork() {
        KafkaTopicsProperties props = new KafkaTopicsProperties();
        props.setJsonInput("json-input-topic");

        assertThat(props.getJsonInput()).isEqualTo("json-input-topic");
    }
}
