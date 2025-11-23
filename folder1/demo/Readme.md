# JSON-to-CSV Kafka Streams Demo

## Overview

This project is a Spring Boot 4 application demonstrating an end-to-end **Kafka Streams** pipeline with a REST-driven test producer and CSV output generation.

It provides a complete reference implementation for:

* Kafka Streams processing
* JSON flattening and transformation
* CSV file generation
* REST-based message injection
* Automated testing using Kafka Streams Test Driver
* OpenAPI / Swagger UI documentation

## Functional Flow
```
jsonInput.json
↓
REST Endpoint (/api/produce/json-input)
↓
Kafka Topic (json-input)
↓
Kafka Streams Topology
↓
CsvRecordProcessor
↓
CSV files written to /output directory
```

## Core Features
1. **Kafka Streams Processing**  
   Defined in KafkaStreamsTopologyConfig
    * Consumes from topic: json-input
    * Parses each JSON message
    * Delegates processing to CsvRecordProcessor

2. **CSV Record Processing**  
   Implemented in CsvRecordProcessor  
   Capabilities:
    * Parses JSON using Jackson
    * Flattens nested objects into keys like meta_address_city
    * Detects array fields (e.g. employees, projects)
    * Writes one CSV per array field
    * Combines base fields + array row fields
    * Ensures headers written once per CSV file

3. **REST Provider**   
   Endpoint:
   ```
   POST /api/produce/json-input
   ```  
   Function:
    * Reads /src/main/resources/jsonInput.json
    * Sends JSON to Kafka topic json-input
    * Returns HTTP 202 Accepted   
      This endpoint is implemented in:
   ```
   JsonProducerController
   ```
4. **OpenAPI / Swagger**   
   Interactive API documentation is auto-generated.
   URL	                                     Purpose  
   http://localhost:8080/swagger-ui.html	Swagger UI
   http://localhost:8080/v3/api-docs	    Raw OpenAPI JSON

## Technologies Used
* Java 17
* Spring Boot 4
* Spring Kafka & Kafka Streams
* Apache Kafka 3.4
* Jackson
* Apache Commons CSV
* springdoc-openapi
* JUnit 5 + Mockito
* Kafka Streams Test Driver

## Project Structure
```text
src/
├── main
│   ├── java/com/example/demo
│   │   ├── config
│   │   │   ├── KafkaStreamsConfig.java   # Kafka Streams configuration (application id, bootstrap servers)
│   │   │   ├── KafkaProducerConfig.java  # Kafka producer + KafkaTemplate (if not using starter)
│   │   │   ├── CsvProperties.java        # CSV output directory configuration
│   │   │   └── OpenApiConfig.java        # OpenAPI metadata configuration
│   │   ├── streams
│   │   │   └── KafkaStreamsTopologyConfig.java # Defines KStream topology and connects it to CsvRecordProcessor
│   │   ├── service
│   │   │   └── CsvRecordProcessor.java   # Core logic: JSON flattening, array extraction, CSV writing
│   │   ├── producer
│   │   │   └── JsonFileKafkaProducer.java # Reads jsonInput.json from classpath, sends to Kafka
│   │   ├── web
│   │   │   └── JsonProducerController.java # REST endpoint /api/produce/json-input
│   │   └── DemoApplication.java
│   └── resources
│       ├── application.yml
│       └── jsonInput.json
└── test
├── java
│   └── com/example/demo
│       ├── service/CsvRecordProcessorTest.java
│       ├── producer/JsonFileKafkaProducerTest.java
│       ├── streams/KafkaStreamsTopologyConfigTest.java
│       └── web/JsonProducerControllerTest.java
└── resources
└── jsonInput.json
```
## How To Run
### Start Kafka (Docker)
```
docker compose up -d
```
### Run Application
```
mvn spring-boot:run
```
### Trigger Test Payload
```
curl -X POST http://localhost:8080/api/produce/json-input
```
## Output

CSV files are generated in:
```
/demo/output/
```
Each array field in JSON generates its own CSV file:
* employees.csv
* projects.csv

## Testing

Run all tests:
```
mvn test
```
Includes:
* Kafka topology validation
* REST controller validation
* Producer validation
* CSV formatting tests

