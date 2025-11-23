package com.example.demo.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaProducerConfigTest {

    @Test
    void producerConfigs_containsCoreProperties() {
        KafkaProducerConfig config = new KafkaProducerConfig();

        Map<String, Object> props = config.producerConfigs();

        assertThat(props)
                .containsKeys(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
                );
    }

    @Test
    void kafkaTemplate_isCreated() {
        KafkaProducerConfig config = new KafkaProducerConfig();

        KafkaTemplate<String, String> template = config.kafkaTemplate();

        assertThat(template).isNotNull();
    }
}
