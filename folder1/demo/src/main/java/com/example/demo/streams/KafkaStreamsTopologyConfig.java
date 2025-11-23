package com.example.demo.streams;

import com.example.demo.config.KafkaTopicsProperties;
import com.example.demo.service.CsvRecordProcessor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaStreamsTopologyConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaStreamsTopologyConfig.class);

    private final CsvRecordProcessor csvRecordProcessor;
    private final KafkaTopicsProperties topicsProperties;

    public KafkaStreamsTopologyConfig(CsvRecordProcessor csvRecordProcessor,
                                      KafkaTopicsProperties topicsProperties) {
        this.csvRecordProcessor = csvRecordProcessor;
        this.topicsProperties = topicsProperties;
    }

    @Bean
    public KStream<String, String> jsonInputStream(StreamsBuilder builder) {
        String inputTopic = topicsProperties.getJsonInput();

        KStream<String, String> stream = builder.stream(
                inputTopic,
                Consumed.with(Serdes.String(), Serdes.String())
        );

        stream.peek((key, value) ->
                        log.debug("Received record from topic={} key={}", inputTopic, key))
                .foreach((key, value) -> csvRecordProcessor.processJsonRecord(value));

        return stream;
    }
}
