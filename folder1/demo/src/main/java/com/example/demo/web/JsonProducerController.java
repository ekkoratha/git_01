package com.example.demo.web;

import com.example.demo.producer.JsonFileKafkaProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class JsonProducerController {

    private final JsonFileKafkaProducer producer;

    public JsonProducerController(JsonFileKafkaProducer producer) {
        this.producer = producer;
    }

    /**
     * Reads a JSON file from the classpath and publishes it to the Kafka topic.
     *
     * <p>If the root JSON node is an array, each element is sent as a separate message.
     * If the root JSON node is an object, that object is sent as a single message.</p>
     *
     * @param fileName the classpath resource name (for example {@code jsonInput.json})
     */
    @PostMapping("/publish/json")
    public ResponseEntity<Void> publishJsonFromClasspath(
            @RequestParam(name = "fileName", defaultValue = "jsonInput.json") String fileName) {

        producer.sendFileToTopic(fileName);
        return ResponseEntity.accepted().build();
    }
}
