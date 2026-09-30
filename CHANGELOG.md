# Changelog

All notable changes to Aktimetrix. Versions follow [Semantic Versioning](https://semver.org/); until 1.0, a minor
version may change the public API.

## Unreleased

**Documentation**
- The README is now the product page: what Aktimetrix is and is not, a quick start, how to use it, its modules, the
  air-cargo focus and the roadmap. The white paper moved, unchanged, to [docs/white-paper.md](./docs/white-paper.md).

## 0.1.0

The first release: plan-versus-actual monitoring of long-running business processes, from the events the business
already emits.

**Model**
- Processes and steps declared as definitions; each process instance follows one business entity, such as an order.
- Measurements in any dimension (time, distance, cost, temperature, rating...), each with a planned value, actual
  values, the deviation, and whether it is within tolerance.
- Plans from fixed values, from durations after the process start or an earlier step, or from rules.
- Interim readings while a step is in progress, and metrics derived from several measurements, such as fuel per km.
- Timeliness of every step and process: `ON_TIME`, `LATE`, `AT_RISK` (forecast late) and `OVERDUE`.
- Process lifecycle: implicit or explicit start and end, cancellation, optional steps, and a deadline for the process
  as a whole.
- Versioned definitions: a running instance keeps the definition it started with.

**Runtime**
- Durable deadline alarms: an alarm is set at each deadline when a step or process is planned, and marks it `OVERDUE`
  if its event has not arrived when it fires; alarms are claimed in leased batches by any instance.
- Transactional outbox: state and published events are written in one unit of work and relayed to the broker.
- Optimistic concurrency on every instance, so several runtime instances run side by side without coordination.
- Dead-letter channel for events that cannot be processed.
- Structured published events for processes, steps and measurements, with a context, a code catalogue and JSON Schemas.
- Micrometer metrics for events, timeliness, deviations, alarms and the outbox; a REST query API.

**Definitions**
- A fluent Java DSL, with planning rules as lambdas scoped to their tenant and process.
- YAML and JSON definition files, validated strictly at startup: every problem is reported with where it is.

**Modules**
- `aktimetrix-core`, independent of any broker or store.
- State stores: `aktimetrix-store-mongodb`, `aktimetrix-store-jdbc` (PostgreSQL) and `aktimetrix-store-memory`, all
  passing the same contract tests.
- Message brokers: `aktimetrix-broker-kafka` and `aktimetrix-broker-rabbitmq`.

Tested on JDK 11, 17 and 21, with end-to-end scenarios on every broker and store combination. Artifacts are not yet on
Maven Central: build them with `./mvnw install`.
