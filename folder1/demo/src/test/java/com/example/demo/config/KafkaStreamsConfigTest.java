package com.example.demo.config;

import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaStreamsConfigTest {

    @Test
    void kafkaStreamsConfig_hasApplicationIdAndBootstrapServers() {
        KafkaStreamsConfig config = new KafkaStreamsConfig();

        KafkaStreamsConfiguration ksc = config.kafkaStreamsConfig();
        var props = ksc.asProperties();

        assertThat(props).containsKeys(
                StreamsConfig.APPLICATION_ID_CONFIG,
                StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
                StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG
        );

        assertThat(props.getProperty(StreamsConfig.APPLICATION_ID_CONFIG))
                .isEqualTo("json-to-csv-app");
        assertThat(props.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG))
                .isEqualTo("localhost:9092");
    }
}
