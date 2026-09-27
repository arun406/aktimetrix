<p align="center">
  <img src="./img/aktimetrix-banner.svg" alt="Aktimetrix — event-driven business process monitoring for the JVM" width="100%">
</p>

<p align="center">
  <a href="https://github.com/arun406/aktimetrix/actions/workflows/ci.yml"><img src="https://github.com/arun406/aktimetrix/actions/workflows/ci.yml/badge.svg?branch=develop" alt="CI"></a>
  <a href="#quick-start"><img src="https://img.shields.io/badge/java-11%2B-0F4C5C?logo=openjdk&logoColor=white" alt="Java 11+"></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/spring%20boot-2.7-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot 2.7"></a>
  <a href="https://spring.io/projects/spring-cloud-stream"><img src="https://img.shields.io/badge/spring%20cloud%20stream-2021.0-6DB33F?logo=spring&logoColor=white" alt="Spring Cloud Stream"></a>
  <a href="https://kafka.apache.org/"><img src="https://img.shields.io/badge/apache%20kafka-supported-231F20?logo=apachekafka&logoColor=white" alt="Apache Kafka"></a>
  <a href="https://www.mongodb.com/"><img src="https://img.shields.io/badge/mongodb-supported-47A248?logo=mongodb&logoColor=white" alt="MongoDB"></a>
  <a href="./LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-blue" alt="License: Apache 2.0"></a>
  <a href="#roadmap"><img src="https://img.shields.io/badge/status-alpha-F5A524" alt="Status: alpha"></a>
</p>

<p align="center">
  <b>Aktimetrix</b> turns the business events your systems already emit into a live picture of every running
  business process: which step each order, loan, or shipment is in, and what should happen next and when.
</p>

<p align="center">
  <a href="#quick-start">Quick start</a> ·
  <a href="./docs/concepts.md">Concepts</a> ·
  <a href="./docs/getting-started.md">Guide</a> ·
  <a href="./docs/extending.md">Extending</a> ·
  <a href="./docs/configuration.md">Reference</a> ·
  <a href="https://github.com/arun406/aktimetrix-reference-project-order-monitor">Example project</a>
</p>

---

## Contents

1. [Why Aktimetrix?](#why-aktimetrix)
2. [Features](#features)
3. [How it works](#how-it-works)
4. [Core concepts](#core-concepts)
5. [Quick start](#quick-start)
6. [Your first monitor](#your-first-monitor)
7. [Documentation](#documentation)
8. [Roadmap](#roadmap)
9. [Contributing](#contributing) · [License](#license)

## Why Aktimetrix?

Long-lived business processes such as *order → ship → deliver*, *apply → approve → disburse*, or
*book → accept → fly → deliver* run across many systems. Each system knows its own step; nobody sees the whole
journey, and delays surface only when a customer complains.

**Business process monitoring** fixes this by following each business entity through a defined sequence of
milestones and comparing what *should* happen (the plan) with what *does* happen (the actuals).

Aktimetrix is a lightweight Java framework for building those monitors. You describe the process as JSON, write a
small **meter** for each deadline, and Aktimetrix:

- consumes your business events from Kafka,
- creates a **process instance** with its **steps** for every business entity, such as every order,
- **plans** each step, with a duration from the definition or with your own meter: "this order should ship by 01:46",
- **records** what actually happens as events arrive: "shipped at 01:30", judged **on time** or **late**,
- **forecasts** the steps put **at risk** when an earlier one runs late, and flags missed deadlines as **overdue**,
- answers "where is order 1234?" over REST, and publishes every change to Kafka for dashboards and alerts.

## Features

| | |
|---|---|
| **Plan vs. actual** | Every step gets a planned time and an actual time from its milestone event, and is judged `ON_TIME` or `LATE` against its deadline. |
| **Plans without code** | `"plannedWithin": "PT2H"` in a step definition plans it from the process start or from an earlier step; a meter handles anything more complex. Tolerances set how late is too late. |
| **At-risk forecasting** | When a step runs late, later steps are forecast by the same delay and flagged `AT_RISK` before they miss their deadline. |
| **Overdue detection** | A monitor flags steps whose deadline passes without their event: the delays you most need to know about. |
| **Declarative processes** | Processes and steps are JSON files in your code base, loaded at startup, or managed through the REST API. |
| **Minimal code** | Add the dependency, the JSON and your meters. Event routing and process handling have defaults; override them only when you need to. |
| **Spring Boot auto-configuration** | No `@ComponentScan`, no Kafka binding boilerplate: point it at Kafka and MongoDB. |
| **Event-driven, reliably** | Built on Spring Cloud Stream and Apache Kafka. Process, step, and measurement changes go through a MongoDB outbox, so a Kafka outage delays events instead of losing them. |
| **Observable** | Micrometer metrics for events, processes, and steps by timeliness, ready for Prometheus. |
| **Idempotent** | One process instance per *tenant + process + entity*, and each step completes once, so replayed events are harmless. |
| **Multi-tenant** | Every definition and instance carries a tenant key, and definitions never leak across tenants. |

## How it works

<p align="center">
  <img src="./img/architecture.svg" alt="Aktimetrix runtime architecture" width="100%">
</p>

Every business event goes through the same stages:

1. **Start.** If the event starts a process (it is in the process's `startEventCodes`), Aktimetrix creates the
   process instance and its steps for the entity.
2. **Plan.** Right away, each new step gets its planned time, from a duration in its definition or from your meter,
   and a deadline: the planned time plus any tolerance.
3. **Record.** The event is then applied to every running process of the entity: steps that list it complete
   with the event's time as their actual time, and are judged against their deadline. A late step puts later
   steps **at risk**, and steps planned to follow it get their planned time.
4. **Watch.** Separately, the overdue monitor looks for steps past their deadline with no event yet.

Here is one order being placed, then shipped:

<p align="center">
  <img src="./img/sequence.svg" alt="Sequence of an order being placed and then shipped" width="100%">
</p>

## Core concepts

<p align="center">
  <img src="./img/domain-model.svg" alt="Definitions vs. instances in Aktimetrix" width="100%">
</p>

| Definition (design time) | Instance (run time, one per business entity) |
|---|---|
| **Process**: a named business process, e.g. `ORDER_DELIVERY`, and the events that start it | **Process instance**: that process for one order, e.g. order `#1234` |
| **Step**: one milestone, e.g. `SHIP`, and the events that complete it | **Step instance**: `SHIP` for order `#1234`, with its planned time, actual time, and timeliness |
| **Measurement**: what to measure at a step (`TIME`, …), **P**lanned or **A**ctual | **Measurement instance**: a value, e.g. *planned TIME = 2022-05-23T01:46* |

Instances carry **metadata**: key/value pairs from your domain (order id, customer, order time) that meters use to
plan. See **[Core concepts](./docs/concepts.md)** for the step lifecycle and the same model applied to banking and
air cargo.

## Quick start

The [Order Monitor](https://github.com/arun406/aktimetrix-reference-project-order-monitor) reference project is a
complete application. You need **JDK 11+** and **Docker**.

```bash
git clone https://github.com/arun406/aktimetrix.git
(cd aktimetrix && ./mvnw install -DskipTests)      # not on Maven Central yet

git clone https://github.com/arun406/aktimetrix-reference-project-order-monitor.git
cd aktimetrix-reference-project-order-monitor
docker compose up -d                               # Kafka and MongoDB
./mvnw spring-boot:run
```

Then send an order's events and ask where it is:

```bash
send() { docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh \
           --bootstrap-server localhost:9092 --topic order-events < "events/$1"; }
send order-placed.json; send order-shipped.json; send order-delivered.json

curl -s 'http://localhost:8080/process-instances?tenant=AA&entityId=1234'
```

<p align="center">
  <img src="./img/order-timeline.svg" alt="Planned and actual timeline of order 1234" width="100%">
</p>

## Your first monitor

**1. Add the dependency.** It brings Spring Web, Spring Data MongoDB and Spring Cloud Stream for Kafka, and
configures itself.

```xml
<dependency>
    <groupId>com.aktimetrix</groupId>
    <artifactId>aktimetrix-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

**2. Describe the process** in `src/main/resources/aktimetrix/process-definitions.json`:

```json
[{
  "tenant": "AA", "processCode": "ORDER_DELIVERY", "entityType": "com.ecom.order", "status": "CONFIRMED",
  "startEventCodes": ["ORDER_PLACED_EVENT"],
  "steps": [{ "stepCode": "PLACE" }, { "stepCode": "SHIP" }, { "stepCode": "DELIVER" }]
}]
```

and its steps in `src/main/resources/aktimetrix/step-definitions.json`: the event that completes each step, and
whether it has a planned time.

```json
[
  { "tenant": "AA", "stepCode": "PLACE",   "status": "CONFIRMED", "startEventCodes": ["ORDER_PLACED_EVENT"] },
  { "tenant": "AA", "stepCode": "SHIP",    "status": "CONFIRMED", "startEventCodes": ["ORDER_SHIPPED_EVENT"],
    "measurements": [{ "measurementCode": "TIME", "type": "P" }] },
  { "tenant": "AA", "stepCode": "DELIVER", "status": "CONFIRMED", "startEventCodes": ["ORDER_DELIVERED_EVENT"],
    "measurements": [{ "measurementCode": "TIME", "type": "P" }] }
]
```

**3. Plan the deadlines.** A fixed duration needs no code: give the step `"plannedWithin": "PT2H"` (from the process
start) or add `"plannedAfter": "SHIP"` (from when `SHIP` completes), and optionally `"tolerance": "PT15M"`. For
anything computed, such as business hours or a customer's tier, write a meter instead: "an order should ship within
2 hours".

```java
@Component
@Measurement(code = "TIME", stepCode = "SHIP")
public class OrderShippedPlanTimeMeter extends AbstractMeter {

    @Override
    protected String getMeasurementUnit(String tenant, StepInstance step) {
        return "TIMESTAMP";
    }

    @Override
    protected String getMeasurementValue(String tenant, StepInstance step) {
        return String.valueOf(metadataTime(step, "orderedOn").plusHours(2));
    }
}
```

**4. Point it at your infrastructure** in `application.yml`:

```yaml
spring:
  data.mongodb.uri: mongodb://localhost:27017/order-monitor
  kafka.properties.bootstrap.servers: localhost:9092
aktimetrix:
  events.topic: order-events
```

That's a working monitor. By default the whole event entity becomes the metadata meters read. Add a
`@ProcessHandler` to choose it yourself, as the reference project does. The
**[step-by-step guide](./docs/getting-started.md)** covers the event format and every option.

## Documentation

| Guide | What's inside |
|---|---|
| 📘 [Core concepts](./docs/concepts.md) | Processes, steps, measurements, instances, metadata; step lifecycle and timeliness; other domains |
| 🚀 [Getting started](./docs/getting-started.md) | Run the reference project; build a monitor from scratch; the event format |
| 🧩 [Extending Aktimetrix](./docs/extending.md) | Process handlers, event handlers, pre- and post-processors, built-in components, modelling a new process |
| ⚙️ [Configuration & API reference](./docs/configuration.md) | `aktimetrix.*` properties, Kafka topics, MongoDB collections, REST endpoints |

**Project layout**

```
aktimetrix/
├── aktimetrix-core/        the framework
│   └── src/main/java/com/aktimetrix/
│       ├── autoconfigure/  Spring Boot auto-configuration and default properties
│       └── core/           api, stereotypes, event handlers, processors, meters, monitor, reference data, REST
├── docs/                   the guides above
├── img/                    diagrams
└── pom.xml
```

## Roadmap

Aktimetrix is **alpha** (`0.0.1-SNAPSHOT`): the full loop of plan, actual, at risk and overdue works end to end, as
the tests on an embedded Kafka and an in-memory MongoDB show. APIs may still change.

- [x] Actual measurements and plan-vs-actual timeliness (`ON_TIME`, `LATE`)
- [x] Overdue detection for events that never arrive
- [x] Query API: "where is order #1234?"
- [x] Spring Boot auto-configuration and process definitions as code
- [x] At-risk forecasting from the delays of earlier steps
- [x] Planned durations and tolerances in the step definition, so simple deadlines need no meter
- [x] Reliable delivery through a MongoDB outbox
- [x] Metrics with Micrometer
- [x] Continuous integration on JDK 11, 17 and 21
- [ ] Publish to Maven Central: the release pipeline is ready ([RELEASING.md](./RELEASING.md)); the first release
  waits on the Maven Central namespace and signing key

## Contributing

Contributions are welcome, whether bug reports, documentation, or code.

1. Fork the repository and branch from `develop`.
2. Build and verify locally with `./mvnw clean verify`.
3. Open a pull request against `develop` describing the problem and your change.

For larger changes, please open an issue first so the design can be discussed.

## License

Aktimetrix is open source software released under the [Apache License 2.0](./LICENSE).

<p align="center"><sub>Built by <a href="https://github.com/arun406">Arun Kumar Kandakatla</a> and contributors.</sub></p>
