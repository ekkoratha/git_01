package com.example.demo.web;

import com.example.demo.producer.JsonFileKafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JsonProducerControllerTest {

    private MockMvc mockMvc;
    private JsonFileKafkaProducer producer;

    @BeforeEach
    void setUp() {
        // mock the producer – no real Kafka involved
        producer = Mockito.mock(JsonFileKafkaProducer.class);

        JsonProducerController controller = new JsonProducerController(producer);

        // IMPORTANT: standalone setup – no Spring Boot context
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void publishJsonFromClasspath_returns202() throws Exception {
        mockMvc.perform(post("/api/publish/json"))
                .andExpect(status().isAccepted());

        // verify default fileName is used
        verify(producer, times(1)).sendFileToTopic("jsonInput.json");
    }
}
