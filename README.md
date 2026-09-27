<p align="center">
  <img src="./img/aktimetrix-banner.svg" alt="Aktimetrix — event-driven business process monitoring for the JVM" width="100%">
</p>

<p align="center">
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
6. [Your first monitor in three classes](#your-first-monitor-in-three-classes)
7. [Documentation](#documentation)
8. [Roadmap](#roadmap)
9. [Contributing](#contributing) · [License](#license)

## Why Aktimetrix?

Long-lived business processes such as *order → ship → deliver*, *apply → approve → disburse*, or
*book → accept → fly → deliver* run across many systems. Each system knows its own step; nobody sees the whole
journey, and delays surface only when a customer complains.

**Business process monitoring** fixes this by following each business entity through a defined sequence of
milestones and comparing what *should* happen (the plan) with what *does* happen (the actuals).

Aktimetrix is a lightweight Java framework for building those monitors. You describe the process once as data,
write a few small annotated Spring beans for the domain-specific logic, and Aktimetrix:

- consumes business events from Kafka,
- creates a **process instance** and its **step instances** for every business entity,
- computes **planned measurements**, such as "this order should ship by 01:46",
- records **actual measurements** as milestone events arrive, such as "shipped at 01:30",
- publishes every change as an event, so dashboards, alerts, and planners can react in real time.

## Features

| | |
|---|---|
| **Declarative process model** | Processes, steps, and measurements are reference data in MongoDB. Change a process without redeploying. |
| **Annotation-driven** | `@EventHandler`, `@ProcessHandler`, `@Measurement`, `@PreProcessor`, `@PostProcessor`. Extend a small base class and it is wired in automatically. |
| **Event-driven** | Built on Spring Cloud Stream and Apache Kafka. Every instance created is published to an outbound topic. |
| **Plan and actual** | Meters compute when each step *should* happen; milestone events record when it *did*, and complete the step and the process. |
| **Idempotent** | One process instance per *tenant + process + entity*, and each step completes once, so replayed events never create duplicates. |
| **Multi-tenant** | Every definition and instance carries a tenant key. |
| **Domain agnostic** | E-commerce, banking, air cargo, logistics: if it has milestones, you can monitor it. |

## How it works

<p align="center">
  <img src="./img/architecture.svg" alt="Aktimetrix runtime architecture" width="100%">
</p>

An Aktimetrix application is a Spring Boot service with two pipelines:

1. **Processor.** A business event arrives on the inbound topic. The event handler for its `eventCode` finds the
   process definitions it starts, and the process handler creates the process instance and one step instance per
   step, then publishes them.
2. **Meter.** Aktimetrix consumes its own step events. For each planned measurement defined on the step, it calls
   the matching meter, saves the result, and publishes it to `measurement-instance-out-0`.
3. **Milestones.** Later events about the same entity (e.g. `ORDER_SHIPPED_EVENT`) complete the steps that list
   them, and record an actual `TIME` measurement on the same topic.

Here is the full journey of one `ORDER_PLACED_EVENT`:

<p align="center">
  <img src="./img/sequence.svg" alt="Sequence of one order event through the processor and meter pipelines" width="100%">
</p>

## Core concepts

<p align="center">
  <img src="./img/domain-model.svg" alt="Definitions vs. instances in Aktimetrix" width="100%">
</p>

| Definition (design time) | Instance (run time, one per business entity) |
|---|---|
| **Process**: a named business process, e.g. `ORDER_DELIVERY`, and the events that start it | **Process instance**: that process for one order, e.g. order `#1234` |
| **Step**: one milestone, e.g. `SHIP` | **Step instance**: `SHIP` for order `#1234` |
| **Measurement**: what to measure at a step (`TIME`, `PCS`, …), **P**lanned or **A**ctual | **Measurement instance**: a computed value, e.g. *planned TIME = 2022-05-23T01:46* |

Instances carry **metadata**: key/value pairs from your domain (order id, customer, location) that meters and
downstream consumers use. See **[Core concepts](./docs/concepts.md)** for the full model, a worked example, and
the same model applied to banking and air cargo.

## Quick start

Run the [Order Monitor example](https://github.com/arun406/aktimetrix-reference-project-order-monitor) against
local Kafka and MongoDB. You need **JDK 11+** and **Docker**.

```bash
# 1. Build and install the framework (not yet on Maven Central)
git clone https://github.com/arun406/aktimetrix.git && cd aktimetrix
./mvnw clean install

# 2. Start Kafka and MongoDB, load the example's reference data, and run it
#    (the full commands are in the Getting started guide)

# 3. From the example project directory, publish an order event…
kafka-console-producer.sh --bootstrap-server localhost:9092 --topic order-event-topic < requests/request1.json

# 4. …and read the plan Aktimetrix computed for it
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic measurement-instance-out-0 --from-beginning
```

Each order produces a plan like this:

<p align="center">
  <img src="./img/order-timeline.svg" alt="Planned timeline for order #1234" width="100%">
</p>

➡️ **[Getting started](./docs/getting-started.md)** has every command, including the Docker Compose file and data
loading.

## Your first monitor in three classes

Once the process is defined as reference data, a working plan is three small Spring beans.

**1. An event handler** starts the process when an order is placed:

```java
@Component
@EventHandler(eventType = "ORDER_PLACED_EVENT")
public class OrderPlacedEventHandler extends AbstractEventHandler {
}
```

**2. A process handler** decides which metadata to keep on each instance:

```java
@Component
@ProcessHandler(processType = "ORDER_DELIVERY")
public class OrderProcessor extends AbstractProcessor {

    private static final DateTimeFormatter IN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    protected Map<String, Object> getProcessMetadata(Context context) {
        return (Map<String, Object>) context.getProperty(Constants.ENTITY);
    }

    @Override
    protected Map<String, Object> getStepMetadata(Context context) {
        Map<String, Object> order = (Map<String, Object>) context.getProperty(Constants.ENTITY);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderId", order.get("orderId"));
        metadata.put("orderedOn", LocalDateTime.parse((String) order.get("orderedOn"), IN));
        return metadata;
    }
}
```

**3. A meter** computes a planned value, here "ship within 2 hours of the order":

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
        LocalDateTime orderedOn = LocalDateTime.parse((String) step.getMetadata().get("orderedOn"));
        return String.valueOf(orderedOn.plusHours(2));
    }
}
```

**Then track what actually happens** with a one-line handler per milestone event:

```java
@Component
@EventHandler(eventType = "ORDER_SHIPPED_EVENT")
public class OrderShippedEventHandler extends AbstractMilestoneEventHandler {
}
```

➡️ The **[step-by-step guide](./docs/getting-started.md#build-a-monitor-step-by-step)** walks through the
reference data, the event format, and every class.

## Documentation

| Guide | What's inside |
|---|---|
| 📘 [Core concepts](./docs/concepts.md) | Processes, steps, measurements, instances, metadata; worked example; other domains |
| 🚀 [Getting started](./docs/getting-started.md) | Run the example locally; build a monitor from scratch |
| 🧩 [Extending Aktimetrix](./docs/extending.md) | All annotations, pre- and post-processor examples, built-in components, modelling a new process |
| ⚙️ [Configuration & API reference](./docs/configuration.md) | `application.yml`, Kafka bindings and topics, MongoDB collections, REST endpoints |

**Project layout**

```
aktimetrix/
├── aktimetrix-core/        the framework: api, stereotypes, handlers, meters, reference data, configuration
├── docs/                   the guides above
├── img/                    diagrams
└── pom.xml
```

## Roadmap

Aktimetrix is **alpha** (`0.0.1-SNAPSHOT`). The core pipeline, from events to process and step instances to
planned and actual measurements, works end to end. APIs may still change.

**Next: close the monitoring loop**
- [x] **Actual measurements:** record when each step really happens, using the step definitions' `startEventCodes` / `endEventCodes`
- [ ] **Plan vs. actual status:** mark each step and process *on time*, *at risk*, or *late*
- [ ] **Deadline detection:** raise an alert when an expected event *doesn't* arrive by its planned time
- [ ] **Query API:** answer "where is order #1234?" over REST

**Then: make it easy to adopt**
- [ ] Spring Boot starter with auto-configuration (no `@ComponentScan("com.aktimetrix.core")`)
- [ ] Process definitions as code: YAML loaded at startup, with versioning
- [ ] Remove domain-specific leftovers (air-cargo constants, the hard-coded `A2ATRANSPORT` post-processor type)
- [ ] Publish to Maven Central

**Throughout: production readiness**
- [ ] Reliable delivery: idempotent event handling, and an outbox so MongoDB and Kafka stay consistent
- [ ] Metrics with Micrometer
- [ ] Integration tests with Testcontainers, and CI (unit tests are in place)
- [ ] Fix: `POST /reference-data/process-definitions` ignores the JSON body (use `mongoimport` for now)

## Contributing

Contributions are welcome, whether bug reports, documentation, or code.

1. Fork the repository and branch from `develop`.
2. Build and verify locally with `./mvnw clean verify`.
3. Open a pull request against `develop` describing the problem and your change.

For larger changes, please open an issue first so the design can be discussed.

## License

Aktimetrix is open source software released under the [Apache License 2.0](./LICENSE).

<p align="center"><sub>Built by <a href="https://github.com/arun406">Arun Kumar Kandakatla</a> and contributors.</sub></p>
