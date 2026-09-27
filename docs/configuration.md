# Configuration & API reference

[← Back to README](../README.md)

## Configuration

An Aktimetrix application needs only its MongoDB and Kafka connection and the topic its events arrive on:

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/order-monitor}
  kafka:
    properties:
      bootstrap.servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

aktimetrix:
  events:
    topic: order-events
```

### `aktimetrix.*` properties

| Property | Default | Purpose |
|---|---|---|
| `aktimetrix.events.topic` | `business-events` | Kafka topic of the inbound business events. |
| `aktimetrix.events.group` | `aktimetrix` | Consumer group of the inbound business events. |
| `aktimetrix.time-zone` | `UTC` | Zone of all planned and actual times. Event times are converted to it, and the overdue monitor compares planned times with the current time in it. |
| `aktimetrix.definitions.load-on-startup` | `true` | Load process and step definitions from the classpath at startup. |
| `aktimetrix.definitions.processes` | `classpath*:aktimetrix/process-definitions.json` | Location of the process definitions: a JSON array. |
| `aktimetrix.definitions.steps` | `classpath*:aktimetrix/step-definitions.json` | Location of the step definitions: a JSON array. |
| `aktimetrix.monitor.enabled` | `true` | Run the overdue monitor. |
| `aktimetrix.monitor.overdue-check-interval` | `PT1M` | How often to look for overdue steps, as an ISO-8601 duration. |

### Defaults Aktimetrix provides

These are set with the lowest precedence, so your own configuration always wins:

| Property | Default |
|---|---|
| `spring.cloud.stream.function.definition` | `processor` |
| `spring.cloud.stream.bindings.processor-in-0.destination` | `${aktimetrix.events.topic}` |
| `spring.cloud.stream.bindings.processor-in-0.group` | `${aktimetrix.events.group}` |
| `spring.cloud.stream.kafka.bindings.{process,step,measurement}-instance-out-0.producer.configuration.key.serializer` | `StringSerializer` |
| `spring.jackson.serialization.write-dates-as-timestamps` | `false` |

If your application defines Spring Cloud Stream functions of its own, include `processor` in your
`spring.cloud.stream.function.definition`, e.g. `processor;myFunction`.

The overdue monitor is a `@Scheduled` task, so Aktimetrix enables Spring's scheduling in the application.

> **Using Confluent Cloud or another secured cluster?** Add `security.protocol`, `sasl.mechanism`, and
> `sasl.jaas.config` under `spring.kafka.properties`, and supply the credentials through environment variables or a
> secret store. Never commit them to source control. The reference project's `confluent` profile shows how.

## Kafka topics

| Topic | Direction | Messages |
|---|---|---|
| `aktimetrix.events.topic` | in | Your business events; see [the event format](getting-started.md#the-event-format). |
| `process-instance-out-0` | out | `Process_Event` / `CREATED`: a process instance with its steps, keyed by process instance id. |
| `step-instance-out-0` | out | `Step_Event` / `CREATED`, `STARTED`, `COMPLETED` or `OVERDUE`: a step with its `plannedAt`, `actualAt` and `timeliness`, keyed by step instance id. |
| `measurement-instance-out-0` | out | `Measurement_Event` / `CREATED`: a planned (`P`) or actual (`A`) measurement, keyed by measurement instance id. |

A dashboard or alerting service subscribes to `step-instance-out-0` and acts on `OVERDUE`, or on `COMPLETED` with
`timeliness` `LATE`.

## MongoDB collections

| Collection | Contents |
|---|---|
| `processDefinitions`, `stepDefinitions`, `eventTypeDefinitions`, `measurementTypeDefinitions` | Definitions |
| `processInstances`, `stepInstances`, `measurement-instance` | Runtime state |

## REST API

| Method | Path | Description |
|---|---|---|
| `GET` | `/process-instances?tenant=&entityId=[&entityType=]` | The entity's process instances, each with its steps' status, `plannedAt`, `actualAt` and `timeliness`. |
| `GET` / `POST` | `/reference-data/process-definitions` | List process definitions, or create or replace one (by tenant and code). |
| `GET` / `POST` | `/reference-data/step-definitions` | List step definitions, or create or replace one (by tenant and code). |
| `GET` / `POST` | `/reference-data/measurement-type-definitions` | List or create measurement types. |

```bash
curl -s 'http://localhost:8080/process-instances?tenant=AA&entityId=1234'

curl -X POST http://localhost:8080/reference-data/step-definitions \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"AA","stepCode":"SHIP","status":"CONFIRMED","startEventCodes":["ORDER_SHIPPED_EVENT"],
       "measurements":[{"measurementCode":"TIME","type":"P"}]}'
```

The API has no authentication of its own. Protect it as you would any internal service, for example with Spring
Security or a gateway.

---

**Next:** [Back to README](../README.md)
