# Configuration & API reference

[← Back to README](../README.md)

> This guide describes the **reference implementation**, which binds the message broker to Apache Kafka and the
> state store to MongoDB. The model itself is technology-neutral: see the [README](../README.md#42-infrastructure-contract).

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
| `aktimetrix.outbox.relay-interval` | `PT1S` | How often the outbox relay publishes pending events to Kafka. |
| `aktimetrix.outbox.batch-size` | `100` | Most events published per relay run. |
| `aktimetrix.outbox.lease` | `PT30S` | How long a relay holds a claimed event before another instance may retry it. |
| `aktimetrix.outbox.retention` | `P7D` | How long sent events stay in the outbox before being purged. |

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
| `step-instance-out-0` | out | `Step_Event` / `CREATED`, `PLANNED`, `STARTED`, `COMPLETED`, `AT_RISK` or `OVERDUE`: a step with its `plannedAt`, `lateAfter`, `expectedAt`, `actualAt` and `timeliness`, keyed by step instance id. |
| `measurement-instance-out-0` | out | `Measurement_Event` / `CREATED`: a planned (`P`) or actual (`A`) measurement, keyed by measurement instance id. |

A dashboard or alerting service subscribes to `step-instance-out-0` and acts on `AT_RISK` and `OVERDUE`, or on
`COMPLETED` with `timeliness` `LATE`.

### Delivery guarantees

Outbound events are first written to the `outbox` collection, next to the state they describe, and a relay publishes
them to Kafka in order. When Kafka is unavailable, events wait in the outbox and are sent once it is back, instead of
being lost. Delivery is **at least once**: after a crash between sending and recording an event, it is sent again, so
consumers should de-duplicate on the event's `eventId`. Relays in several application instances share the work
safely. With more than one instance, events of different entities may be published in a slightly different order
than they happened.

## MongoDB collections

| Collection | Contents |
|---|---|
| `processDefinitions`, `stepDefinitions`, `eventTypeDefinitions`, `measurementTypeDefinitions` | Definitions |
| `processInstances`, `stepInstances`, `measurement-instance` | Runtime state |
| `outbox` | Events waiting to be, or recently, published to Kafka |

For large volumes, index `stepInstances` on `{ status: 1, lateAfter: 1 }` for the overdue monitor and `outbox` on
`{ sentAt: 1, createdAt: 1 }` for the relay.

## Metrics

Aktimetrix records [Micrometer](https://micrometer.io/) metrics in the application's `MeterRegistry`. Add
`spring-boot-starter-actuator` and a registry such as `micrometer-registry-prometheus` to export them, e.g. at
`/actuator/prometheus`.

| Metric | Type | Tags |
|---|---|---|
| `aktimetrix.events` | counter | `tenant`, `event`, `outcome` (`handled`, `invalid`, `failed`) |
| `aktimetrix.processes.started` / `.completed` | counter | `tenant`, `process` |
| `aktimetrix.steps.completed` | counter | `tenant`, `step`, `timeliness` |
| `aktimetrix.steps.lateness` | timer | `tenant`, `step`: how long after its planned time a step completed |
| `aktimetrix.steps.at.risk` / `.overdue` | counter | `tenant`, `step` |
| `aktimetrix.outbox.pending` | gauge | events not yet published to Kafka |

## Process definition fields

| Field | Purpose |
|---|---|
| `tenant`, `processCode`, `processName`, `status` | Identity; only `CONFIRMED` definitions are used. |
| `processType` | Selects the process handler and pre- and post-processors; defaults to `processCode`. |
| `entityType` | The type of business entity the process follows; must match the events' `entityType`. |
| `startEventCodes` | The events that create a process instance. |
| `steps` | The step codes, in order. |
| `measurements` | Planned (`P`) measurements of the process as a whole, computed by process-level meters, e.g. `{ "measurementCode": "DISTANCE", "type": "P" }`. |

## Step definition fields

| Field | Purpose |
|---|---|
| `tenant`, `stepCode`, `stepName`, `status` | Identity; only `CONFIRMED` definitions are used. |
| `startEventCodes`, `endEventCodes` | The events that start and complete the step; see [the step lifecycle](concepts.md#step-lifecycle-plan-and-actual). |
| `optionalInd` | `Y` if the process can complete without the step. |
| `measurements` | Planned (`P`) measurements computed by meters, e.g. `{ "measurementCode": "TIME", "type": "P" }`. |
| `plannedWithin`, `plannedAfter` | Plan the step by an ISO-8601 duration from the process start, or from the completion of `plannedAfter`. |
| `tolerance` | ISO-8601 duration past the planned time before the step counts as late. |

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
