<p align="center">
  <img src="./img/aktimetrix-banner.svg" alt="Aktimetrix — event-driven business process monitoring for the JVM" width="100%">
</p>

<p align="center">
  <a href="https://github.com/arun406/aktimetrix/actions/workflows/ci.yml"><img src="https://github.com/arun406/aktimetrix/actions/workflows/ci.yml/badge.svg?branch=develop" alt="CI"></a>
  <a href="#9-reference-implementation"><img src="https://img.shields.io/badge/java-11%2B-0F4C5C?logo=openjdk&logoColor=white" alt="Java 11+"></a>
  <a href="./LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-blue" alt="License: Apache 2.0"></a>
  <a href="#11-status-and-roadmap"><img src="https://img.shields.io/badge/status-alpha-F5A524" alt="Status: alpha"></a>
</p>

<h1 align="center">Aktimetrix</h1>

<p align="center">
  <b>Plan-versus-actual monitoring of long-running business processes, derived from the events your systems already emit.</b><br>
  <sub>A white paper and its open-source reference implementation</sub>
</p>

<p align="center">
  <a href="#abstract">Abstract</a> ·
  <a href="#4-architecture">Architecture</a> ·
  <a href="#5-execution-semantics">Semantics</a> ·
  <a href="#9-reference-implementation">Reference implementation</a> ·
  <a href="./docs/getting-started.md">Guide</a> ·
  <a href="https://github.com/arun406/aktimetrix-reference-project-order-monitor">Example project</a>
</p>

---

## Abstract

Business processes such as *order → ship → deliver*, *apply → approve → disburse* or *book → accept → fly →
deliver* span many independent systems. Each system records its own step, but no system holds the whole journey,
so a delay is usually discovered by the customer before it is discovered by the business.

**Aktimetrix** is a model and a runtime for closing that gap. A process is declared once as an ordered set of
milestones. For each business entity, such as an order or a loan application, the runtime derives a **plan** (when
each milestone should occur), records the **actuals** as business events arrive, and classifies every milestone as
**on time**, **late**, **at risk** or **overdue**. The results can be queried ("where is order 1234?") and are
published as events of their own, so that dashboards, alerting and analytics can act on them.

The model depends on only two infrastructure capabilities: a **message broker** that delivers business events, and
a **state store** that holds definitions and running instances. Neither is tied to a particular product. This
document describes the model, its execution semantics and its reliability guarantees in those terms. The
[reference implementation](#9-reference-implementation) is one concrete realisation of it, on the JVM.

## Contents

1. [Introduction](#1-introduction)
2. [Design goals](#2-design-goals)
3. [The monitoring model](#3-the-monitoring-model)
4. [Architecture](#4-architecture)
5. [Execution semantics](#5-execution-semantics)
6. [Reliability and consistency](#6-reliability-and-consistency)
7. [Extensibility](#7-extensibility)
8. [Observability](#8-observability)
9. [Reference implementation](#9-reference-implementation)
10. [Applicability and limitations](#10-applicability-and-limitations)
11. [Status and roadmap](#11-status-and-roadmap)
12. [Further reading](#12-further-reading) · [Contributing](#contributing) · [License](#license)

## 1. Introduction

A long-running business process is a sequence of milestones that happen in different systems, often owned by
different teams or organisations. An e-commerce order is placed in a shop, shipped by a warehouse and delivered by
a carrier. A loan is submitted in a portal, checked by a credit bureau, approved by an underwriter and disbursed by
core banking.

Each of these systems already announces what it did, as a business event. What is missing is the layer that joins
those events into a single account of each entity and compares it with what was supposed to happen. Without it,
organisations rely on batch reports that arrive after the fact, or on bespoke integrations rebuilt for every
process.

**Business process monitoring** provides that layer. It follows each business entity through a defined sequence of
milestones and continuously compares the plan with the actuals. Aktimetrix treats this as a general, reusable
capability: the process is data, the planning rules are configuration or small pieces of code, and everything else,
from event routing to state management, deadline tracking and publication of results, is provided by the runtime.

## 2. Design goals

| Goal | Consequence in the design |
|---|---|
| **Non-invasive** | Source systems are not changed. They publish the business events they already produce, in their own format; an event mapper translates them, and Aktimetrix only consumes them. |
| **Declarative** | Processes, steps and simple deadlines are data, versioned with the application or managed at run time. |
| **Minimal code** | A working monitor needs definitions and, only for computed deadlines, a meter. Every other component has a default. |
| **Infrastructure-neutral model** | The model assumes only a message broker and a state store with the properties listed in [§4.2](#42-infrastructure-contract). |
| **Reliable** | Results are persisted before they are published; an unavailable broker delays results but does not lose them. |
| **Idempotent** | Replayed or duplicated events do not create duplicate processes or complete a step twice. |
| **Multi-tenant** | Every definition and instance belongs to a tenant; definitions never apply across tenants. |
| **Observable** | The runtime reports its own throughput and the timeliness of the processes it monitors as metrics. |

## 3. The monitoring model

<p align="center">
  <img src="./img/domain-model.svg" alt="Definitions and instances in the Aktimetrix model" width="100%">
</p>

### 3.1 Definitions and instances

The model separates **what a process looks like** from **what is happening to one business entity**.

| Definition (design time) | Instance (run time, one per business entity) |
|---|---|
| **Process**: a named business process, such as `ORDER_DELIVERY`, the entity type it follows and the events that start it | **Process instance**: that process for one entity, such as order `#1234` |
| **Step**: one milestone, such as `SHIP`, and the events that start and complete it; shared by the tenant's processes, and adaptable per process | **Step instance**: `SHIP` for order `#1234`, with its planned time, actual time and timeliness |
| **Measurement**: a user-defined dimension observed at the process or at a step (`TIME`, distance, rating, weight…), either **P**lanned or **A**ctual | **Measurement instance**: one value for one process or step instance, such as *planned TIME of SHIP = 2022-05-23T01:46* |

A **business entity** is the real-world object being followed. It is identified by an entity type and an entity id,
and it is owned by the source systems, not by Aktimetrix.

### 3.2 Measurements

A **measurement** is a dimension of the process that can be planned and observed. Measurement types are defined by
the user, each with a code and a unit: `TIME` (a timestamp), `DISTANCE` (km), `WEIGHT` (kg), `PIECES`, `RATING`
(1–5), or any other quantity the business cares about.

- **Level.** A measurement is attached either to a **process**, when it describes the entity as a whole (the total
  distance of a delivery, the customer's rating of an order), or to a **step**, when it describes one milestone (the
  weight accepted at `ACCEPT`, the time of `SHIP`).
- **Kind.** Each measurement is **planned** (`P`), computed by a process-level or step-level meter when the
  instance is created, or **actual**
  (`A`), recorded when the milestone happens. Comparing the two, for any dimension, is the core of the model.
- **Time is special.** `TIME` is the one dimension the runtime interprets itself: a step's planned `TIME` becomes
  its planned time, from which its deadline and its timeliness follow ([§5.3](#53-timeliness)). Other dimensions are
  computed, stored and published as measurement instances, so that consumers can compare plan and actual in the
  terms of their own domain.

### 3.3 Metadata

Instances carry **metadata**: key/value pairs taken from the domain, such as an order's id, customer and order time.
Metadata is the input to planning (a meter reads the order time to compute the delivery deadline) and travels with
every published result, so consumers do not need to query the source systems.

### 3.4 Business events

Every inbound event uses one envelope, whatever its source. The fields the model depends on are:

| Field | Role |
|---|---|
| `tenantKey` | Selects the tenant's definitions and instances. |
| `eventCode` | Determines which processes the event starts and which steps it starts or completes. |
| `entityType`, `entityId` | Identify the business entity. All events of order `1234` carry `1234`. |
| `eventTime` | When the event happened in the business; it becomes the actual time of the step. |
| `entity` | The domain object, which becomes metadata. |

The full envelope is specified in the [event format](./docs/getting-started.md#the-event-format). Source systems do not
need to adopt it: an **event mapper** translates each system's own messages into this envelope as they are consumed
([§7](#7-extensibility)).

## 4. Architecture

### 4.1 Logical architecture

<p align="center">
  <img src="./img/architecture.svg" alt="Logical architecture of an Aktimetrix monitor" width="100%">
</p>

Source systems publish business events to an **inbound channel** on the message broker. The Aktimetrix runtime
consumes them and passes each one to an **event router**, which starts processes and records milestones. **Process
handlers** and **meters**, the application's own code (shown dashed), decide an instance's metadata and plan.
Built-in components advance steps, watch deadlines, and keep all definitions and instances in the **state store**.
Every change to a process, step or measurement is published to an **outbound channel** for downstream consumers, and
a query API serves the current state of any entity.

### 4.2 Infrastructure contract

The runtime is written against two capabilities rather than two products.

| Capability | The model requires | Used for |
|---|---|---|
| **Message broker** | Durable publish/subscribe; ordered delivery of messages with the same key; consumer groups for horizontal scaling | Inbound business events; outbound process, step and measurement events |
| **State store** | Durable documents queried by tenant, entity and status; an atomic conditional update (compare-and-set) | Definitions, instances, deadline queries, the outbox and its lease |

Any broker and store with these properties can host the model. The technologies used by the reference
implementation are listed in [§9.1](#91-technology-bindings).

## 5. Execution semantics

Every business event passes through the same four stages.

<p align="center">
  <img src="./img/sequence.svg" alt="Sequence of an order being placed and then shipped" width="100%">
</p>

1. **Start.** If the event's code is one of a process's start events, a process instance and its step instances
   are created for the entity, unless one already exists.
2. **Plan.** Each new step receives a planned time and a **deadline**: the planned time plus the step's tolerance.
3. **Record.** The event is applied to every running process of the entity. Steps that list it are started or
   completed, with the event's time as their actual time.
4. **Watch.** Independently of events, a monitor looks for steps whose deadline has passed without the event that
   completes them.

### 5.1 Step lifecycle

| Status | Entered when |
|---|---|
| `Created` | The process instance is created. |
| `Started` | An event in the step's start events arrives, and the step also defines end events. |
| `Completed` | An event in the step's end events arrives. A step with no end events is a single milestone and completes on its start event. |

A process instance completes when all of its mandatory steps have completed.

### 5.2 Planning

A step's planned time is derived in one of three ways:

| Method | Declared as | Planned time |
|---|---|---|
| **Duration from the start** | `"plannedWithin": "PT2H"` | process start + 2 h |
| **Duration from another step** | `"plannedAfter": "SHIP", "plannedWithin": "PT8H"` | actual completion of `SHIP` + 8 h, set when `SHIP` completes |
| **Meter** | a planned `TIME` measurement and a meter for the step | whatever the meter computes, for example from business hours or a customer's service level |

`"tolerance": "PT15M"` allows a step to run 15 minutes past its planned time before it is considered late.

### 5.3 Timeliness

Timeliness is defined on the `TIME` dimension only: it compares when a step happened with when it was planned.
Plan-versus-actual for other dimensions is expressed by their measurement instances rather than by a timeliness.

| Timeliness | Assigned when |
|---|---|
| `ON_TIME` | The step completes no later than its deadline. |
| `LATE` | The step completes after its deadline. |
| `AT_RISK` | The step has not completed, and an earlier step has run late by enough to push its forecast past its deadline. |
| `OVERDUE` | The deadline has passed and the step's event has not arrived. |

The distinction between `LATE` and `OVERDUE` matters in practice. `LATE` is known only once the step happens;
`OVERDUE` is raised precisely because it has *not* happened, which is often the case that most needs attention.

### 5.4 Forecasting

When a step completes late, the delay is propagated: each later step receives an **expected time**, its planned time
shifted by the same delay. A step whose expected time falls after its deadline becomes `AT_RISK` and is published as
such before its own deadline passes, which gives operators time to intervene.

<p align="center">
  <img src="./img/order-timeline.svg" alt="Planned and actual timeline of order 1234" width="100%">
</p>

## 6. Reliability and consistency

**Transactional outbox.** Each business event is processed as one unit of work. The state it changes and the
results it produces are written to the state store together, in one transaction, and a relay then publishes the
results to the broker. If the broker is unavailable, results accumulate in the outbox and are sent when it returns,
instead of being lost; if processing fails half-way, nothing of it is kept and the event is processed again. Relays
on several runtime instances share the work by leasing entries with an atomic conditional update, so an entry held
by a failed instance is taken over by another once its lease expires.

The atomicity relies on the state store's transactions. A store without them (in the reference implementation, a
standalone MongoDB server rather than a replica set) still works, but a crash in the middle of a unit of work can then
keep its state without its results. The runtime detects this and says so at startup.

**Concurrency.** Every step carries a revision, and a save based on a stale copy is rejected rather than overwriting
a newer state. The deadline monitor can therefore run on every runtime instance: when two instances find the same
overdue step, or the step's event arrives at the same moment, only one change wins and only it is published.

**Delivery guarantee.** Delivery is *at least once*. A failure between sending a result and recording it as sent
causes it to be sent again, so consumers de-duplicate on the event id.

**Ordering.** Producers publish all events of an entity with the entity id as the message key. The broker's per-key
ordering then guarantees that one entity's events are processed in sequence, while different entities are processed
in parallel.

**Idempotency.** There is at most one process instance per *tenant + process + entity*, enforced by a unique index,
and a completed step is never completed again. Replaying an event stream is therefore safe.

**Failed events.** An event that cannot be parsed, or lacks its tenant, event code or entity id, can never succeed
and goes straight to a **dead-letter channel**. An event whose processing fails is retried, and goes to the same
channel if it still fails, so no event is lost silently and each can be inspected and replayed.

## 7. Extensibility

The runtime is a set of defaults with well-defined extension points. Applications contribute components, which are
discovered at startup.

| Extension point | Purpose | Default when absent |
|---|---|---|
| **Meter** | Computes the planned value of a measurement, in any dimension, for a process or for a step. | None; durations in the step definition still apply. |
| **Process handler** | Chooses the metadata of a process and its steps. | The event's entity becomes the metadata. |
| **Event mapper** | Reads the source systems' own event format. | Messages are expected in the Aktimetrix envelope. |
| **Event handler** | Changes how an event is interpreted, such as where its business time is read from. | Generic handling of the envelope. |
| **Pre-processor** | Validates or enriches an entity before a process instance is created. | None. |
| **Post-processor** | Acts on a newly created and planned process instance. | None. |

The [extension guide](./docs/extending.md) documents each one with examples.

## 8. Observability

The runtime reports its own behaviour and the health of the monitored processes as metrics:

| Metric | Meaning |
|---|---|
| `aktimetrix.events` | Events received, by tenant, event code and outcome (handled, ignored, invalid, failed). |
| `aktimetrix.processes.started` / `.completed` | Process instances started and completed, by process. |
| `aktimetrix.steps.completed` | Steps completed, by step and timeliness. |
| `aktimetrix.steps.lateness` | How long after its planned time each step completed. |
| `aktimetrix.steps.at.risk` / `.overdue` | Steps forecast to be late, and steps past their deadline. |
| `aktimetrix.outbox.pending` | Results not yet published. |

Together they answer operational questions directly, such as "what share of shipments were late this week?", without
a separate analytics pipeline.

## 9. Reference implementation

This repository contains `aktimetrix-core`, a reference implementation of the model for the JVM, released under the
Apache License 2.0. It is packaged as a library: an application adds the dependency, supplies its definitions and
meters, and receives the complete runtime described above through auto-configuration.

### 9.1 Technology bindings

| Concern | Reference implementation |
|---|---|
| Runtime | Java 11+, Spring Boot 2.7 |
| Message broker | Apache Kafka, through the Spring Cloud Stream binder abstraction |
| State store | MongoDB, through Spring Data MongoDB |
| Metrics | Micrometer, exportable to Prometheus and other back ends |
| Query API | HTTP/JSON |

These choices belong to the implementation, not the model. The broker is reached through Spring Cloud Stream, whose
binder abstraction also targets other brokers; the state store is currently bound to MongoDB. Broadening both
bindings is part of the [roadmap](#11-status-and-roadmap).

### 9.2 Running the example

The [Order Monitor](https://github.com/arun406/aktimetrix-reference-project-order-monitor) reference project monitors
the rule *"an order ships within 2 hours of being placed and is delivered within 10 hours"*. It needs **JDK 11+**
and **Docker**, which starts a local message broker and state store.

```bash
git clone https://github.com/arun406/aktimetrix.git
(cd aktimetrix && ./mvnw install -DskipTests)      # not yet published to Maven Central

git clone https://github.com/arun406/aktimetrix-reference-project-order-monitor.git
cd aktimetrix-reference-project-order-monitor
docker compose up -d                               # local message broker and state store
./mvnw spring-boot:run
```

The project's README shows how to send the sample events for order `1234` and follow the order through the query
API:

```bash
curl -s 'http://localhost:8080/process-instances?tenant=AA&entityId=1234'
```

### 9.3 Building a monitor

A monitor consists of a dependency, two definition files and, for computed deadlines, meters.

**Dependency.**

```xml
<dependency>
    <groupId>com.aktimetrix</groupId>
    <artifactId>aktimetrix-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

**Process definition** in `src/main/resources/aktimetrix/process-definitions.json`:

```json
[{
  "tenant": "AA", "processCode": "ORDER_DELIVERY", "entityType": "com.ecom.order", "status": "CONFIRMED",
  "startEventCodes": ["ORDER_PLACED_EVENT"],
  "steps": [{ "stepCode": "PLACE" }, { "stepCode": "SHIP" }, { "stepCode": "DELIVER" }]
}]
```

**Step definitions** in `src/main/resources/aktimetrix/step-definitions.json`: the event that completes each step,
and its plan. A fixed duration needs no code.

```json
[
  { "tenant": "AA", "stepCode": "PLACE",   "status": "CONFIRMED", "startEventCodes": ["ORDER_PLACED_EVENT"] },
  { "tenant": "AA", "stepCode": "SHIP",    "status": "CONFIRMED", "startEventCodes": ["ORDER_SHIPPED_EVENT"],
    "plannedWithin": "PT2H", "tolerance": "PT15M" },
  { "tenant": "AA", "stepCode": "DELIVER", "status": "CONFIRMED", "startEventCodes": ["ORDER_DELIVERED_EVENT"],
    "measurements": [{ "measurementCode": "TIME", "type": "P" }] }
]
```

**Meter**, for a deadline that is computed rather than fixed:

```java
@Component
@Measurement(code = "TIME", stepCode = "DELIVER")
public class DeliveryPlanTimeMeter extends AbstractMeter {

    @Override
    protected String getMeasurementUnit(String tenant, StepInstance step) {
        return "TIMESTAMP";
    }

    @Override
    protected String getMeasurementValue(String tenant, StepInstance step) {
        // orders are delivered within 10 hours of being placed
        return String.valueOf(metadataTime(step, "orderedOn").plusHours(10));
    }
}
```

A **process-level meter** measures the process as a whole. Declare the measurement on the process definition,
`"measurements": [{ "measurementCode": "DISTANCE", "type": "P" }]`, and name the process instead of a step:

```java
@Component
@Measurement(code = "DISTANCE", processCode = "ORDER_DELIVERY")
public class DeliveryDistanceMeter extends AbstractProcessMeter {

    private final RouteService routes;   // your own service

    public DeliveryDistanceMeter(RouteService routes) {
        this.routes = routes;
    }

    @Override
    protected String getMeasurementUnit(String tenant, ProcessInstance process) {
        return "KM";
    }

    @Override
    protected String getMeasurementValue(String tenant, ProcessInstance process) {
        Map<String, Object> order = process.getMetadata();
        return String.valueOf(routes.distanceKm(order.get("warehouse"), order.get("postcode")));
    }
}
```

**Configuration.** The application names its inbound channel and supplies the connection settings of its broker and
state store; with the reference bindings, these are the standard Spring Boot properties for Kafka and MongoDB.

```yaml
aktimetrix:
  events:
    topic: order-events
```

The [getting-started guide](./docs/getting-started.md) builds this monitor step by step, and the
[configuration reference](./docs/configuration.md) lists every property, channel and endpoint.

## 10. Applicability and limitations

**Applicability.** The model applies wherever an entity passes through time-bound milestones that are reported by
events:

| Domain | Entity | Milestones |
|---|---|---|
| E-commerce | Order | placed → shipped → delivered |
| Retail banking | Loan application | submitted → KYC → credit check → approved → disbursed |
| Air cargo | Air waybill | booked → accepted → departed → arrived → delivered |
| Customer service | Ticket | opened → acknowledged → resolved |

**Limitations.** Aktimetrix monitors processes; it does not orchestrate them, and it never calls back into source
systems. Processes are modelled as sequences of milestones rather than arbitrary graphs. The quality of the results
depends on the source systems publishing an event, with an accurate business time, for each milestone.

## 11. Status and roadmap

The reference implementation is **alpha** (`0.0.1-SNAPSHOT`). The complete loop of plan, actual, at risk and overdue
works end to end and is verified by tests on JDK 11, 17 and 21. APIs may still change.

- [x] Plan-versus-actual timeliness (`ON_TIME`, `LATE`)
- [x] Overdue detection for events that never arrive
- [x] At-risk forecasting from the delays of earlier steps
- [x] Planned durations and tolerances in step definitions
- [x] Meters at process and step level, for any user-defined dimension
- [x] Query API for the state of an entity
- [x] Reliable publication through a transactional outbox, with atomic writes on transactional stores
- [x] Safe concurrency across instances, a dead-letter channel, and indexes created at startup
- [x] Metrics
- [x] Definitions as code, and auto-configuration
- [ ] First release to Maven Central: the pipeline is ready ([RELEASING.md](./RELEASING.md)); the release waits on
  the namespace and signing key
- [ ] Verified bindings for further message brokers
- [ ] A state-store abstraction, with implementations beyond MongoDB

## 12. Further reading

| Document | Contents |
|---|---|
| [Core concepts](./docs/concepts.md) | The model in detail, with examples from banking and air cargo |
| [Getting started](./docs/getting-started.md) | Running the example; building a monitor; the event format |
| [Extending Aktimetrix](./docs/extending.md) | Meters, process and event handlers, pre- and post-processors |
| [Configuration and API reference](./docs/configuration.md) | Properties, channels, storage, metrics and REST endpoints of the reference implementation |
| [Order Monitor](https://github.com/arun406/aktimetrix-reference-project-order-monitor) | A complete, tested example application |

## Contributing

Contributions are welcome, whether they are bug reports, documentation, new bindings or code.

1. Fork the repository and branch from `develop`.
2. Build and verify with `./mvnw clean verify`.
3. Open a pull request against `develop` that describes the problem and the change.

For larger changes, please open an issue first so the design can be discussed.

## License

Aktimetrix is released under the [Apache License 2.0](./LICENSE).

<p align="center"><sub>Authored by <a href="https://github.com/arun406">Arun Kumar Kandakatla</a> and contributors.</sub></p>
