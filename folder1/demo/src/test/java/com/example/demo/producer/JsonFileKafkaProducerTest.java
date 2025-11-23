package com.example.demo.producer;

import com.example.demo.config.KafkaTopicsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JsonFileKafkaProducerTest {

    private KafkaTemplate<String, String> kafkaTemplate;
    private KafkaTopicsProperties topicsProperties;
    private JsonFileKafkaProducer producer;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);

        topicsProperties = new KafkaTopicsProperties();
        topicsProperties.setJsonInput("json-input");

        producer = new JsonFileKafkaProducer(
                kafkaTemplate,
                new ObjectMapper(),
                topicsProperties
        );
    }

    @Test
    void sendFileToTopic_rootArray_sendsOneMessagePerElement() throws Exception {
        // Arrange: mock send(...) to return a completed future with dummy metadata
        when(kafkaTemplate.send(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    String topic = invocation.getArgument(0);
                    RecordMetadata metadata = new RecordMetadata(
                            new TopicPartition(topic, 0),
                            0, 0, 0L, 0L, 0, 0);
                    return CompletableFuture.completedFuture(new org.springframework.kafka.support.SendResult<>(null, metadata));
                });

        // Act
        int producedCount = producer.sendFileToTopic("jsonInput.json");

        // Assert
        assertThat(producedCount).isEqualTo(2); // matches test jsonInput.json array size

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), isNull(), anyString());
        assertThat(topicCaptor.getAllValues()).allMatch("json-input"::equals);
    }
}
