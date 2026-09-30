# Configuration & API reference

[← Back to README](../README.md)

> This guide describes the **reference implementation**: its modules, their settings, and its API. The model itself
> is technology-neutral: see the [white paper](./white-paper.md#52-infrastructure-contract).

## Choosing a store and a broker

An application depends on `aktimetrix-core`, one store module and one broker module, and supplies their connection
settings with the standard Spring Boot properties.

| Module | Connection settings | Notes |
|---|---|---|
| `aktimetrix-store-mongodb` | `spring.data.mongodb.uri` | Atomic units of work on a replica set or sharded cluster (a single-node replica set is enough). |
| `aktimetrix-store-jdbc` | `spring.datasource.url`, `.username`, `.password`, and the database's JDBC driver | Written for PostgreSQL; atomic units of work. |
| `aktimetrix-store-memory` | none | Not durable, not shared between instances, not atomic: for tests and demos. |
| `aktimetrix-broker-kafka` | `spring.kafka.properties.bootstrap.servers` (or `spring.cloud.stream.kafka.binder.brokers`) | Events of one entity are processed in order through partitions keyed by entity id. |
| `aktimetrix-broker-rabbitmq` | `spring.rabbitmq.host`, `.port`, `.username`, `.password` | Events are processed in order by a single active consumer; see [RabbitMQ](#rabbitmq). |

For example, MongoDB and Kafka:

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

or PostgreSQL and RabbitMQ:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/aktimetrix
    username: aktimetrix
    password: ${DB_PASSWORD}
  rabbitmq:
    host: localhost

aktimetrix:
  events:
    topic: order-events
```

### `aktimetrix.*` properties

| Property | Default | Purpose |
|---|---|---|
| `aktimetrix.events.topic` | `business-events` | Destination of the inbound business events: a Kafka topic, or a RabbitMQ exchange. |
| `aktimetrix.events.group` | `aktimetrix` | Consumer group of the inbound business events. |
| `aktimetrix.events.dead-letter.enabled` | `true` | Send events that cannot be processed to a dead-letter topic instead of dropping them. |
| `aktimetrix.events.dead-letter.topic` | *events topic*`.dlq` | The dead-letter topic. |
| `aktimetrix.time-zone` | `UTC` | Zone of all planned and actual times. Event times are converted to it, and alarms compare deadlines with the current time in it. |
| `aktimetrix.definitions.load-on-startup` | `true` | Load process and step definitions from the classpath at startup. |
| `aktimetrix.definitions.processes` | `classpath*:aktimetrix/process-definitions.json` | Location of the process definitions: a JSON array. |
| `aktimetrix.definitions.steps` | `classpath*:aktimetrix/step-definitions.json` | Location of the step definitions: a JSON array. |
| `aktimetrix.definitions.files` | `classpath*:aktimetrix/*.yaml,classpath*:aktimetrix/*.yml` | Locations of YAML definition files, comma-separated; each holds a `tenant` and its `steps` and `processes`. `Definitions` beans built with the Java DSL are loaded as well. Every definition is validated at startup: an unknown field or an invalid value stops the application, with a message naming the file. |
| `aktimetrix.alarms.enabled` | `true` | Fire the alarms set at the deadlines of steps and processes. Alarms are set whatever this is, so an instance that does not fire them still keeps them up to date. |
| `aktimetrix.alarms.check-interval` | `PT5S` | How often to look for alarms that are due, as an ISO-8601 duration: at most this long after its deadline, a step or process without its event is marked overdue. |
| `aktimetrix.alarms.batch-size` | `100` | Most alarms claimed at once; an instance claims further batches while they are full. |
| `aktimetrix.alarms.lease` | `PT30S` | How long an instance holds a claimed alarm before another may fire it. |
| `aktimetrix.monitor.enabled` | `true` | Run the overdue sweep: a safety net that finds steps and processes past their deadline without an alarm, such as those saved before alarms existed. |
| `aktimetrix.monitor.overdue-check-interval` | `PT10M` | How often the sweep runs, as an ISO-8601 duration. |
| `aktimetrix.outbox.relay-interval` | `PT1S` | How often the outbox relay publishes pending events to Kafka. |
| `aktimetrix.outbox.batch-size` | `100` | Most events published per relay run. |
| `aktimetrix.outbox.lease` | `PT30S` | How long a relay holds a claimed event before another instance may retry it. |
| `aktimetrix.outbox.retention` | `P7D` | How long sent events stay in the outbox before being purged. |
| `aktimetrix.storage.type` | none | The store module to use when several are on the classpath: `mongodb`, `jdbc` or `memory`. |
| `aktimetrix.storage.transactions` | `auto` | Process each event, and each overdue step or process, in a transaction: `auto` when the store supports it (MongoDB as a replica set or sharded cluster; always with JDBC), `always`, or `never`. |
| `aktimetrix.storage.create-indexes` | `true` | Create the indexes (MongoDB) or the tables and indexes (JDBC) Aktimetrix relies on at startup. |

### Defaults Aktimetrix provides

These are set with the lowest precedence, so your own configuration always wins:

| Property | Default | Set by |
|---|---|---|
| `spring.cloud.stream.function.definition` | `processor` | core |
| `spring.cloud.stream.bindings.processor-in-0.destination` | `${aktimetrix.events.topic}` | core |
| `spring.cloud.stream.bindings.processor-in-0.group` | `${aktimetrix.events.group}` | core |
| `spring.cloud.stream.bindings.dead-letter-out-0.destination` | `${aktimetrix.events.dead-letter.topic}` | core |
| `spring.jackson.serialization.write-dates-as-timestamps` | `false` | core |
| `spring.cloud.stream.kafka.bindings.processor-in-0.consumer.enableDlq`, `.dlqName` | `true`, the dead-letter topic | Kafka module |
| `spring.cloud.stream.kafka.bindings.*-out-0.producer.messageKeyExpression` | `headers['aktimetrixKey']` | Kafka module |
| `spring.cloud.stream.kafka.bindings.*-out-0.producer.configuration.key.serializer` | `StringSerializer` | Kafka module |
| `spring.cloud.stream.rabbit.bindings.processor-in-0.consumer.singleActiveConsumer` | `true` | RabbitMQ module |
| `spring.cloud.stream.rabbit.bindings.processor-in-0.consumer.republishToDlq`, `.deadLetterExchange`, `.deadLetterRoutingKey` | `true`, the dead-letter exchange and its name | RabbitMQ module |
| `spring.cloud.stream.rabbit.bindings.*-out-0.producer.routingKeyExpression` | `headers['aktimetrixKey']`; for dead letters, the dead-letter queue's name | RabbitMQ module |

If your application defines Spring Cloud Stream functions of its own, include `processor` in your
`spring.cloud.stream.function.definition`, e.g. `processor;myFunction`.

The alarm scheduler, the overdue sweep and the outbox relay are `@Scheduled` tasks, so Aktimetrix enables Spring's scheduling in the application.

> **Using Confluent Cloud or another secured cluster?** Add `security.protocol`, `sasl.mechanism`, and
> `sasl.jaas.config` under `spring.kafka.properties`, and supply the credentials through environment variables or a
> secret store. Never commit them to source control. The reference project's `confluent` profile shows how.

## Channels

| Binding | Direction | Messages |
|---|---|---|
| `processor-in-0` (`aktimetrix.events.topic`) | in | Your business events; see [the event format](getting-started.md#the-event-format). |
| `process-instance-out-0` | out | `Process_Event` / `CREATED`, `COMPLETED`, `CANCELLED` or `OVERDUE`: a process instance with its steps. |
| `step-instance-out-0` | out | `Step_Event` / `CREATED`, `PLANNED`, `STARTED`, `COMPLETED`, `AT_RISK`, `OVERDUE`, `SKIPPED` or `CANCELLED` (`AT_RISK` again whenever its forecast moves later): a step instance. |
| `measurement-instance-out-0` | out | `Measurement_Event` / `PLANNED`, `RECORDED`, `READING` or `METRIC`: a planned value, a final actual, an interim reading, or a process metric. |
| `dead-letter-out-0` (`aktimetrix.events.dead-letter.topic`) | out | Inbound events that could not be processed, unchanged: invalid ones at once, failing ones after 3 attempts. |

Every event is keyed by its process instance id, so all events of one process, whatever their type, stay in order.
With Kafka, each binding is a topic of the same name, and the key is the record key. With RabbitMQ, each outbound
binding is a topic exchange of the same name, and the key is the routing key: bind a queue to it with `#` to receive
everything. A dashboard or alerting service subscribes to `step-instance-out-0` and acts on `AT_RISK` and `OVERDUE`,
or on `COMPLETED` with `timeliness` `LATE`.

### RabbitMQ

- **Events exchange and queue.** Source systems publish to the topic exchange `aktimetrix.events.topic`; Aktimetrix
  consumes from the queue `<topic>.<group>`, for example `order-events.aktimetrix`, bound to it with `#`.
- **Order.** The queue has a single active consumer: whichever instance holds it processes the events in order, and
  another takes over if it stops. To process in parallel while keeping each entity's events in order, use Spring
  Cloud Stream partitioning on `processor-in-0`, keyed by the entity id.
- **Dead letters.** The RabbitMQ module declares a durable direct exchange and a durable queue, both named
  `aktimetrix.events.dead-letter.topic`, bound by that name. Failing events are republished there by the binder, and
  invalid ones are published there by Aktimetrix. Only standard AMQP 0-9-1 features are used for this, not RabbitMQ's
  dead-letter queue arguments.

### Delivery guarantees

Outbound events are first written to the outbox, in the state store next to the state they describe, and a relay
publishes them to the broker in order. When the store supports transactions, each business event is processed in one,
so its state and its outbound events are saved together or not at all. Without them (a standalone MongoDB server, or
the in-memory store), Aktimetrix logs a warning at startup, and a crash in the middle of an event can keep its state
without its outbound events. When the broker is unavailable, events wait in the outbox and are sent once it is back,
instead of being lost. Delivery is **at least once**: after a crash between sending and recording an event, it is
sent again, so consumers should de-duplicate on the event's `eventId`. Relays in several application instances share
the work safely. With more than one instance, events of different entities may be published in a slightly different
order than they happened.

### Failed events

An inbound event that is not valid JSON, or has no `tenantKey`, `eventCode` or `entityId`, can never be processed: it
is sent unchanged to the dead-letter channel at once, and counted in `aktimetrix.events` with outcome `invalid`. An
event whose processing throws is retried by the binder (3 attempts by default,
`spring.cloud.stream.bindings.processor-in-0.consumer.max-attempts`), counted with outcome `failed` on each attempt,
and then sent to the same channel. Fix the cause, then replay the dead letters into the events topic: processing is
idempotent.

### Running several instances

Every instance consumes from the events topic, runs the outbox relay and fires due alarms. Step and process
instances carry a `revision` and are saved with a version check, so when two instances change the same step or
process, the second change fails instead of overwriting the first: an alarm is fired again after its lease
expires, and an event is retried. The in-memory store is not shared between instances: run a single one.

## Published event payloads

Every outbound message has three parts: the envelope, the `entity`, which is the instance that changed, and the
context, in `eventDetails`. The [white paper](./white-paper.md#45-published-events) describes the event catalogue. JSON
Schemas of the three event types ship in `aktimetrix-core`, under `META-INF/aktimetrix/schemas/`:
`process-event.schema.json`, `step-event.schema.json` and `measurement-event.schema.json`.

**Envelope**

| Field | Meaning |
|---|---|
| `eventId` | Unique id of the event; de-duplicate on it. |
| `eventType`, `eventCode`, `eventName` | `Process_Event`, `Step_Event` or `Measurement_Event`; what happened, from the catalogue; and the same for people, e.g. `Step at risk`. |
| `eventTime`, `eventUTCTime` | When Aktimetrix published the event, in UTC. The business time of the change is `eventDetails.occurredAt`. |
| `source` | `aktimetrix`. |
| `tenantKey` | The tenant. |
| `entityType`, `entityId` | The kind of instance (`com.aktimetrix.process.instance`, `…step.instance` or `…measurement.instance`) and its id. |

**Context** (`eventDetails`)

| Field | Meaning |
|---|---|
| `schemaVersion` | `1`. |
| `businessEntity` | `entityType` and `entityId` of the business entity the process follows, e.g. `com.ecom.order` `1234`. |
| `processCode`, `processInstanceId`, `definitionRevision` | The process, its instance, and the revision of the definition it follows. |
| `stepCode`, `stepInstanceId` | The step, for step events and step measurements. |
| `revision` | The revision of the process or step instance after the change: of two events about one instance, the higher is the more recent. |
| `occurredAt` | When the change happened in the business, in `aktimetrix.time-zone`: the time of the business event that caused it, or of the deadline check. |
| `cause` | `type` `EVENT`, with the `eventId` and `eventCode` of the business event; or `type` `DEADLINE`, for a change made when a deadline passed: an alarm or the overdue sweep. |

**`Process_Event`** (`entityType` `com.aktimetrix.process.instance`)

| Field | Meaning |
|---|---|
| `id`, `tenant`, `processCode`, `processName`, `entityType`, `entityId` | The process instance, its name, and the business entity it follows. |
| `status` | `Created`, `Completed` or `Cancelled`. |
| `complete` | `true` once completed or cancelled. |
| `startedAt` | Business time of the event that started it. |
| `plannedAt`, `lateAfter` | Its own deadline, if the definition has `plannedWithin` or a planned `TIME` set by a meter: planned completion, and that plus the tolerance. |
| `endedAt` | Business time of the event that completed or cancelled it. |
| `timeliness` | `ON_TIME` or `LATE` at completion, or `OVERDUE`; empty without a deadline. |
| `definitionRevision` | The revision of the process definition the instance follows: the one it started with. |
| `metadata` | The process metadata. |
| `steps` | Its steps, as in `Step_Event` (on `CREATED`). |

**`Step_Event`** (`entityType` `com.aktimetrix.step.instance`)

| Field | Meaning |
|---|---|
| `id`, `processInstanceId`, `tenant`, `stepCode`, `stepName`, `sequence` | The step instance, its process, its name, and its position from 0. |
| `optional` | Whether the process can complete without it. |
| `status` | `Created`, `Started`, `Completed`, `Skipped` or `Cancelled`. |
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
| `interim` | `true` for a reading reported while its step was in progress; the final actual has `false`. |
| `derivedFrom` | For a process metric, its expression, e.g. `FUEL / DISTANCE`. |
| `plannedValue`, `deviation`, `conformance` | For an actual: the plan it is compared with, actual minus planned (a number, or an ISO-8601 duration for `TIME`), and `WITHIN_TOLERANCE` / `OUT_OF_TOLERANCE` when a tolerance is declared. |

Times inside `entity` are local date-times in `aktimetrix.time-zone`.

## Storage

Each store keeps the same objects: definitions, process, step and measurement instances, and the outbox. A process
instance also keeps the definition it started with. Every store enforces at most one process instance per tenant,
process, entity type and entity id.

### MongoDB

| Collection | Contents |
|---|---|
| `processDefinitions`, `stepDefinitions`, `measurementTypeDefinitions` | Definitions |
| `processInstances`, `stepInstances`, `measurement-instance` | Runtime state |
| `outbox` | Events waiting to be, or recently, published |
| `alarms` | Alarms at the deadlines of open steps and processes, indexed by due time |

The store creates the indexes its queries need at startup, including a unique index on process instances by
tenant, process, entity type and entity id. If existing data violates a unique index, the index is not created and
the error is logged. Set `aktimetrix.storage.create-indexes=false` to manage indexes yourself. At startup it also
upgrades data saved by earlier versions: it adds a `revision` to step and process instances that have none, and
stores references to process and step instances as strings rather than object ids.

### JDBC

| Table | Contents |
|---|---|
| `aktimetrix_process_definition`, `aktimetrix_step_definition`, `aktimetrix_measurement_type` | Definitions |
| `aktimetrix_process_instance`, `aktimetrix_step_instance`, `aktimetrix_measurement_instance` | Runtime state |
| `aktimetrix_outbox` | Events waiting to be, or recently, published |
| `aktimetrix_alarm` | Alarms at the deadlines of open steps and processes, indexed by due time |

Each row keeps its object as a JSON document, next to the columns its queries, constraints and version checks use.
The store creates the tables and indexes that do not exist at startup, from `com/aktimetrix/store/jdbc/schema.sql`
in the module; set `aktimetrix.storage.create-indexes=false` to manage the schema yourself from that file. The SQL is
written for PostgreSQL.

### In memory

Nothing to configure. State is lost when the application stops.

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
| `aktimetrix.alarms.pending` | gauge | alarms set and not fired yet |
| `aktimetrix.alarms.fired` | counter | `tenant`, `kind` (`STEP`, `PROCESS`): alarms that marked a step or process overdue |
| `aktimetrix.alarms.delay` | timer | `kind`: how long after its due time an alarm fired |
| `aktimetrix.outbox.pending` | gauge | events not yet published to the broker |

## Process definition fields

| Field | Purpose |
|---|---|
| `tenant`, `processCode`, `processName`, `status` | Identity; only `CONFIRMED` definitions are used. |
| `revision` | Set by Aktimetrix: 1 for a new definition, incremented each time a saved definition differs from the stored one. A process instance keeps the definition, at the revision it started with. |
| `processType` | Selects the process handler and pre- and post-processors; defaults to `processCode`. |
| `entityType` | The type of business entity the process follows; must match the events' `entityType`. |
| `startEventCodes` | The events that create a process instance: a business event such as *order created*, which can also complete the first step, or a dedicated start event. |
| `endEventCodes` | Optional. Events that explicitly end a running instance (*order closed*): it completes on them, not when its last mandatory step completes; mandatory steps still open become `Skipped`, optional ones stay open. Without them, the process ends implicitly with its last mandatory step. |
| `cancelEventCodes` | The events that cancel a running instance: the process and its open steps become `Cancelled` and are no longer monitored. |
| `plannedWithin`, `tolerance` | The whole process's own deadline: an ISO-8601 duration from its start, plus the time it may run over before it counts as late or overdue. For a deadline set by a rule, such as 1 day for priority customers and 3 otherwise, declare a planned `TIME` measurement on the process and a process-level meter for it instead. |
| `steps` | The steps, in order. Each names a `stepCode` and may set any step definition field, which then applies to this process only: see below. |
| `measurements` | Measurements of the process as a whole, e.g. its planned `TIME` (its deadline, set by a meter) or the order's cost; see [Measurement fields](#measurement-fields). Planned ones are set when the process starts, actual ones recorded when it completes. |
| `metrics` | Optional. Metrics computed when the process completes, e.g. `{ "code": "FUEL_PER_KM", "expression": "FUEL / DISTANCE", "unit": "L/KM", "tolerance": "10%", "worseWhen": "HIGHER" }`. In the expression, arithmetic over measurement codes (`+ - * /`, parentheses), each code is the sum of that measurement's final values across the process and its steps. It is computed from the actuals and from the plans, and published as a process-level actual measurement with `derivedFrom`. |

## Step definition fields

Steps are defined in `step-definitions.json`, shared by every process of the tenant, or directly in a process's
`steps`, or both: a field set in the process overrides the shared definition for that process only. A step used by
a single process needs no shared definition at all.

```json
{ "tenant": "AA", "processCode": "EXPRESS_DELIVERY", "startEventCodes": ["EXPRESS_ORDER_CREATED_EVENT"],
  "status": "CONFIRMED",
  "steps": [
    { "stepCode": "CONFIRM" },
    { "stepCode": "HANDOVER", "plannedWithin": "PT30M" },
    { "stepCode": "DROP_AT_LOCKER", "startEventCodes": ["PARCEL_IN_LOCKER_EVENT"], "plannedAfter": "HANDOVER", "plannedWithin": "PT1H" }
  ] }
```

Here `HANDOVER` keeps its shared definition, such as the event that completes it, but must happen within 30 minutes
instead of the shared plan, and `DROP_AT_LOCKER` exists only in this process. Lists such as `startEventCodes` or
`measurements` are replaced as a whole.

| Field | Purpose |
|---|---|
| `tenant`, `stepCode`, `stepName`, `status` | Identity; only `CONFIRMED` definitions are used. |
| `startEventCodes`, `endEventCodes` | The events that start and complete the step; see [the step lifecycle](concepts.md#step-lifecycle-plan-and-actual). |
| `optionalInd` | `Y` if the process can complete without the step. |
| `measurements` | The step's measurements; see [Measurement fields](#measurement-fields). The actual `TIME` is always recorded. |
| `progressEventCodes` | Events that report progress while the step is open, e.g. `LOCATION_UPDATED`: each records interim readings (`interim: true`) of the step's actual measurements it carries (`valueFrom`), compared with the plan, without completing the step. |
| `plannedWithin`, `plannedAfter` | Plan the step by an ISO-8601 duration from the process start, or from the completion of `plannedAfter`. |
| `tolerance` | ISO-8601 duration past the planned time before the step counts as late. |

## Measurement fields

A measurement is declared in the `measurements` of a step or process definition, once for its plan (`P`) and once
for its actual value (`A`):

```json
"measurements": [
  { "measurementCode": "DISTANCE", "type": "P", "unit": "KM", "tolerance": "20%", "worseWhen": "HIGHER" },
  { "measurementCode": "DISTANCE", "type": "A", "unit": "KM", "valueFrom": "route.distanceKm" },
  { "measurementCode": "RATING",   "type": "P", "value": "5", "unit": "STARS", "tolerance": "1", "worseWhen": "LOWER" },
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
| `worseWhen` | either | `HIGHER` (distance, fuel, temperature) or `LOWER` (rating): only a deviation that way counts; the other way is always within tolerance. Without a tolerance, the plan itself is the limit: at most, or at least, the planned value. |

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
  -d '{"tenant":"AA","stepCode":"CONFIRM","status":"CONFIRMED","startEventCodes":["ORDER_CONFIRMED_EVENT"],
       "measurements":[{"measurementCode":"TIME","type":"P"}]}'
```

A posted definition is checked as strictly as a definition file. When it is not valid, nothing is saved and the answer
is `400 Bad Request` listing every problem, such as
`{"problems":["process ORDER_DELIVERY: startEventCodes is missing, so the process can never start"]}`.

The API has no authentication of its own. Protect it as you would any internal service, for example with Spring
Security or a gateway.

---

**Next:** [Back to README](../README.md)
