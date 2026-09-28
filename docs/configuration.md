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
| `aktimetrix.events.dead-letter.enabled` | `true` | Send events that cannot be processed to a dead-letter topic instead of dropping them. |
| `aktimetrix.events.dead-letter.topic` | *events topic*`.dlq` | The dead-letter topic. |
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
| `aktimetrix.storage.transactions` | `auto` | Process each event, and each overdue step, in a MongoDB transaction: `auto` when MongoDB supports it (replica set or sharded cluster), `always`, or `never`. |
| `aktimetrix.storage.create-indexes` | `true` | Create the indexes Aktimetrix relies on at startup. |

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
| `process-instance-out-0` | out | `Process_Event` / `CREATED`, `COMPLETED`, `CANCELLED` or `OVERDUE`: a process instance with its steps, `plannedAt`, `lateAfter`, `endedAt` and `timeliness`, keyed by process instance id. |
| `step-instance-out-0` | out | `Step_Event` / `CREATED`, `PLANNED`, `STARTED`, `COMPLETED`, `AT_RISK`, `OVERDUE` or `CANCELLED`: a step with its `plannedAt`, `lateAfter`, `expectedAt`, `actualAt` and `timeliness`, keyed by step instance id. |
| `measurement-instance-out-0` | out | `Measurement_Event` / `CREATED`: a planned (`P`) or actual (`A`) measurement, keyed by measurement instance id. |
| `aktimetrix.events.dead-letter.topic` | out | Inbound events that could not be processed, unchanged: invalid ones at once, failing ones after 3 attempts. |

A dashboard or alerting service subscribes to `step-instance-out-0` and acts on `AT_RISK` and `OVERDUE`, or on
`COMPLETED` with `timeliness` `LATE`.

### Delivery guarantees

Outbound events are first written to the `outbox` collection, next to the state they describe, and a relay publishes
them to Kafka in order. When MongoDB runs as a replica set or sharded cluster, each business event is processed in a
transaction, so its state and its outbound events are saved together or not at all. A standalone MongoDB server has
no transactions: Aktimetrix then logs a warning at startup, and a crash in the middle of an event can keep its state
without its outbound events. A single-node replica set is enough for transactions. When Kafka is unavailable, events wait in the outbox and are sent once it is back, instead of
being lost. Delivery is **at least once**: after a crash between sending and recording an event, it is sent again, so
consumers should de-duplicate on the event's `eventId`. Relays in several application instances share the work
safely. With more than one instance, events of different entities may be published in a slightly different order
than they happened.

### Failed events

An inbound event that is not valid JSON, or has no `tenantKey`, `eventCode` or `entityId`, can never be processed: it
is sent unchanged to the dead-letter topic at once, and counted in `aktimetrix.events` with outcome `invalid`. An
event whose processing throws is retried by the Kafka binder (3 attempts by default,
`spring.cloud.stream.bindings.processor-in-0.consumer.max-attempts`) and then sent to the same topic. Fix the cause,
then replay the dead-letter topic into the events topic: processing is idempotent.

### Running several instances

Every instance consumes a share of the events topic's partitions, runs the outbox relay and runs the overdue monitor.
Step instances carry a `revision` and are saved with a version check, so when two instances change the same step,
the second change fails instead of overwriting the first: the monitor skips such a step until its next check, and an
event is retried.

## Published event payloads

Every outbound message is an [event envelope](getting-started.md#the-event-format) whose `entity` is the instance
that changed. The envelope sets `eventId` (unique; de-duplicate on it), `eventType`, `eventCode`, `eventTime`,
`tenantKey`, `entityType` and `entityId` (the instance id, also the message key).

**`Process_Event`** (`entityType` `com.aktimetrix.process.instance`)

| Field | Meaning |
|---|---|
| `id`, `tenant`, `processCode`, `entityType`, `entityId` | The process instance, and the business entity it follows. |
| `status` | `Created`, `Completed` or `Cancelled`. |
| `complete` | `true` once completed or cancelled. |
| `startedAt` | Business time of the event that started it. |
| `plannedAt`, `lateAfter` | Its own deadline, if the definition has `plannedWithin`: planned completion, and that plus the tolerance. |
| `endedAt` | Business time of the event that completed or cancelled it. |
| `timeliness` | `ON_TIME` or `LATE` at completion, or `OVERDUE`; empty without a deadline. |
| `metadata` | The process metadata. |
| `steps` | Its steps, as in `Step_Event` (on `CREATED`). |

**`Step_Event`** (`entityType` `com.aktimetrix.step.instance`)

| Field | Meaning |
|---|---|
| `id`, `processInstanceId`, `tenant`, `stepCode`, `sequence` | The step instance, its process, and its position from 0. |
| `status` | `Created`, `Started`, `Completed` or `Cancelled`. |
| `plannedAt`, `lateAfter` | When it should happen, and its deadline (planned plus tolerance). |
| `expectedAt` | Forecast, when an earlier step ran late. |
| `actualAt` | Business time of the event that completed it. |
| `timeliness` | `ON_TIME`, `LATE`, `AT_RISK` or `OVERDUE`; empty until it can be judged. |
| `metadata` | The step metadata. |

**`Measurement_Event`** (`entityType` `com.aktimetrix.measurement.instance`)

| Field | Meaning |
|---|---|
| `id`, `tenant`, `processInstanceId` | The measurement, and the process it belongs to. |
| `stepInstanceId`, `stepCode` | The step it belongs to; empty for a process-level measurement. |
| `code`, `value`, `unit` | What was measured, e.g. `DISTANCE`, `12`, `KM`. Values are strings; times are ISO-8601 local date-times. |
| `type` | `P` planned or `A` actual. |
| `plannedValue`, `deviation`, `conformance` | For an actual: the plan it is compared with, actual minus planned (a number, or an ISO-8601 duration for `TIME`), and `WITHIN_TOLERANCE` / `OUT_OF_TOLERANCE` when a tolerance is declared. |

Times inside `entity` are local date-times in `aktimetrix.time-zone`.

## MongoDB collections

| Collection | Contents |
|---|---|
| `processDefinitions`, `stepDefinitions`, `eventTypeDefinitions`, `measurementTypeDefinitions` | Definitions |
| `processInstances`, `stepInstances`, `measurement-instance` | Runtime state |
| `outbox` | Events waiting to be, or recently, published to Kafka |

Aktimetrix creates the indexes its queries need at startup, including a unique index on process instances by
tenant, process, entity type and entity id. If existing data violates a unique index, the index is not created and
the error is logged. Set `aktimetrix.storage.create-indexes=false` to manage indexes yourself. At startup it also adds
a `revision` to step instances saved by earlier builds, which had none.

## Metrics

Aktimetrix records [Micrometer](https://micrometer.io/) metrics in the application's `MeterRegistry`. Add
`spring-boot-starter-actuator` and a registry such as `micrometer-registry-prometheus` to export them, e.g. at
`/actuator/prometheus`.

| Metric | Type | Tags |
|---|---|---|
| `aktimetrix.events` | counter | `tenant`, `event`, `outcome` (`handled`, `ignored`, `invalid`, `failed`) |
| `aktimetrix.processes.started` / `.completed` / `.cancelled` / `.overdue` | counter | `tenant`, `process` |
| `aktimetrix.steps.completed` | counter | `tenant`, `step`, `timeliness` |
| `aktimetrix.steps.lateness` | timer | `tenant`, `step`: how long after its planned time a step completed |
| `aktimetrix.steps.at.risk` / `.overdue` | counter | `tenant`, `step` |
| `aktimetrix.measurements.actual` | counter | `tenant`, `measurement`, `conformance`: actual measurements recorded |
| `aktimetrix.measurements.deviation` | distribution summary | `tenant`, `measurement`: actual minus planned, in the measurement's unit (not `TIME`) |
| `aktimetrix.outbox.pending` | gauge | events not yet published to Kafka |

## Process definition fields

| Field | Purpose |
|---|---|
| `tenant`, `processCode`, `processName`, `status` | Identity; only `CONFIRMED` definitions are used. |
| `processType` | Selects the process handler and pre- and post-processors; defaults to `processCode`. |
| `entityType` | The type of business entity the process follows; must match the events' `entityType`. |
| `startEventCodes` | The events that create a process instance. |
| `cancelEventCodes` | The events that cancel a running instance: the process and its open steps become `Cancelled` and are no longer monitored. |
| `plannedWithin`, `tolerance` | The whole process's own deadline: an ISO-8601 duration from its start, plus the time it may run over before it counts as late or overdue. |
| `steps` | The steps, in order. Each names a `stepCode` and may set any step definition field, which then applies to this process only: see below. |
| `measurements` | Measurements of the process as a whole, e.g. the total distance or the customer's rating; see [Measurement fields](#measurement-fields). Planned ones are set when the process starts, actual ones recorded when it completes. |

## Step definition fields

Steps are defined in `step-definitions.json`, shared by every process of the tenant, or directly in a process's
`steps`, or both: a field set in the process overrides the shared definition for that process only. A step used by
a single process needs no shared definition at all.

```json
{ "tenant": "AA", "processCode": "EXPRESS_DELIVERY", "startEventCodes": ["EXPRESS_ORDER_PLACED_EVENT"],
  "status": "CONFIRMED",
  "steps": [
    { "stepCode": "PLACE" },
    { "stepCode": "SHIP", "plannedWithin": "PT2H" },
    { "stepCode": "DELIVER", "startEventCodes": ["ORDER_DELIVERED_EVENT"], "plannedAfter": "SHIP", "plannedWithin": "PT6H" }
  ] }
```

Here `SHIP` keeps its shared definition, such as the event that completes it, but must happen within 2 hours
instead of the shared plan. Lists such as `startEventCodes` or `measurements` are replaced as a whole.


| Field | Purpose |
|---|---|
| `tenant`, `stepCode`, `stepName`, `status` | Identity; only `CONFIRMED` definitions are used. |
| `startEventCodes`, `endEventCodes` | The events that start and complete the step; see [the step lifecycle](concepts.md#step-lifecycle-plan-and-actual). |
| `optionalInd` | `Y` if the process can complete without the step. |
| `measurements` | The step's measurements; see [Measurement fields](#measurement-fields). The actual `TIME` is always recorded. |
| `plannedWithin`, `plannedAfter` | Plan the step by an ISO-8601 duration from the process start, or from the completion of `plannedAfter`. |
| `tolerance` | ISO-8601 duration past the planned time before the step counts as late. |

## Measurement fields

A measurement is declared in the `measurements` of a step or process definition, once for its plan (`P`) and once
for its actual value (`A`):

```json
"measurements": [
  { "measurementCode": "DISTANCE", "type": "P", "unit": "KM", "tolerance": "20%" },
  { "measurementCode": "DISTANCE", "type": "A", "unit": "KM", "valueFrom": "route.distanceKm" },
  { "measurementCode": "RATING",   "type": "P", "value": "5", "unit": "STARS", "tolerance": "1" },
  { "measurementCode": "RATING",   "type": "A", "valueFrom": "review.stars" }
]
```

| Field | For | Purpose |
|---|---|---|
| `measurementCode` | both | The dimension, e.g. `TIME`, `DISTANCE`, `FUEL`, `TEMPERATURE`, `RATING`. |
| `type` | both | `P` planned, set when the instance is created; `A` actual, recorded when it completes. |
| `value` | `P` | A fixed planned value, used when no meter is registered for the measurement. |
| `valueFrom` | `A` | Where to read the actual value in the completing event's entity, as a dot path. Without it, the meter's `getActualValue` computes it. |
| `unit` | both | The unit of a `value` or of a value read with `valueFrom`. |
| `tolerance` | either | How far the actual may deviate from the plan: absolute (`2`) or relative (`20%`). Without it, the deviation is recorded but not judged. |

A planned value may also come from a meter: `@Measurement(code = "DISTANCE", stepCode = "TRAVEL")`, e.g. the route
length to the customer's address. When the actual value is recorded, it is compared with the plan: its
`plannedValue`, `deviation` (actual minus planned) and `conformance` (`WITHIN_TOLERANCE` or `OUT_OF_TOLERANCE`) are
stored and published with it.

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
