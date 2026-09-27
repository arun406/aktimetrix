# Configuration & API reference

[← Back to README](../README.md)

## Configuration

Aktimetrix uses standard Spring Boot, Spring Data MongoDB, and Spring Cloud Stream properties. A minimal
`application.yml`:

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/svm}
  jackson:
    serialization:
      write-dates-as-timestamps: false
  kafka:
    properties:
      bootstrap.servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
  cloud:
    stream:
      function:
        definition: processor;measure            # the two Aktimetrix pipelines
        bindings:
          processor-in-0: event-processor
          measure-in-0: step-event-processor
      bindings:
        event-processor:
          destination: order-event-topic        # your inbound business events
          group: processor.group.0
        step-event-processor:
          destination: step-instance-out-0      # Aktimetrix's own step events
          group: step.group.0
      source: process-instance;step-instance;measurement-instance
      kafka:
        bindings:
          process-instance-out-0:
            producer:
              configuration:
                "[key.serializer]": org.apache.kafka.common.serialization.StringSerializer
          step-instance-out-0:
            producer:
              configuration:
                "[key.serializer]": org.apache.kafka.common.serialization.StringSerializer
          measurement-instance-out-0:
            producer:
              configuration:
                "[key.serializer]": org.apache.kafka.common.serialization.StringSerializer
          step-event-processor:
            consumer:
              enableDlq: true
              dlqName: input-topic-dlq
logging:
  level:
    com.aktimetrix: DEBUG
```

| Binding | Direction | Payload |
|---|---|---|
| `processor-in-0` | in | Business events (`Event<entity, details>`), routed by `eventCode`. |
| `measure-in-0` | in | Step-instance events (normally bound to `step-instance-out-0`). |
| `process-instance-out-0` | out | `Process_Event` / `CREATED`, keyed by process instance id. |
| `step-instance-out-0` | out | `Step_Event` / `CREATED`, keyed by step instance id. |
| `measurement-instance-out-0` | out | `Measurement_Event` / `CREATED`, keyed by measurement instance id. |

> **Using Confluent Cloud or another secured cluster?** Add `security.protocol`, `sasl.mechanism`, and
> `sasl.jaas.config` under `spring.kafka.properties`, and supply the credentials through environment variables
> or a secret store. Never commit them to source control.

### MongoDB collections

| Collection | Contents |
|---|---|
| `processDefinitions`, `stepDefinitions`, `eventTypeDefinitions`, `measurementTypeDefinitions` | Reference data |
| `processInstances`, `stepInstances`, `measurement-instance` | Runtime state |

## Reference data REST API

| Method | Path | Description |
|---|---|---|
| `GET` / `POST` | `/reference-data/process-definitions` | List or create process definitions |
| `GET` / `POST` | `/reference-data/step-definitions` | List or create step definitions |
| `GET` / `POST` | `/reference-data/measurement-type-definitions` | List or create measurement types |

```bash
curl -X POST http://localhost:8080/reference-data/step-definitions \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"AA","stepCode":"SHIP","stepName":"Order Shipped Step","status":"CONFIRMED",
       "measurements":[{"measurementCode":"TIME","type":"P"}]}'
```

---

**Next:** [Back to README](../README.md)
