package com.example.demo.streams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import com.example.demo.config.KafkaTopicsProperties;
import com.example.demo.service.CsvRecordProcessor;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KafkaStreamsTopologyConfigTest {

    @Test
    void jsonInputStream_topologyCallsCsvRecordProcessor() {
        // Arrange
        CsvRecordProcessor csvRecordProcessor = mock(CsvRecordProcessor.class);
        KafkaTopicsProperties topicsProperties = new KafkaTopicsProperties();
        topicsProperties.setJsonInput("json-input");

        KafkaStreamsTopologyConfig config =
                new KafkaStreamsTopologyConfig(csvRecordProcessor, topicsProperties);

        StreamsBuilder builder = new StreamsBuilder();
        config.jsonInputStream(builder); // build the topology

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        try (TopologyTestDriver testDriver = new TopologyTestDriver(builder.build(), props)) {
            TestInputTopic<String, String> inputTopic =
                    testDriver.createInputTopic("json-input",
                            Serdes.String().serializer(),
                            Serdes.String().serializer());

            String jsonValue = "{\"foo\":\"bar\"}";

            // Act
            inputTopic.pipeInput("key1", jsonValue);

            // Assert
            verify(csvRecordProcessor, times(1)).processJsonRecord(eq(jsonValue));
        }
    }
}
