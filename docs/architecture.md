# Architecture

[← Back to README](../README.md)

This page is for contributors, and for anyone adapting Aktimetrix to their own systems. It describes the reference
implementation's internal components: what each does, how an event flows through them, and where to change them.
The [white paper](../README.md) describes the model they implement.

## Components

<p align="center">
  <img src="../img/components.svg" alt="Internal components of Aktimetrix, by layer" width="100%">
</p>

Dashed orange boxes are **extension points**: you implement or replace them. Blue boxes are **built in**. The
[public API](extending.md#public-api) lists which types you may use; everything else is internal and may change.

| Layer | Components | Responsibility |
|---|---|---|
| ① Inbound | `ProcessConfig.processor()`, `EventMapper`, `AktimetrixTransactions` | Consume each message, turn it into an event, reject what cannot be processed, and run the rest as one unit of work. |
| ② Routing | `RegistryService`, `DefaultRegistry`, `*PostBeanProcessor`, event handlers | Find the components registered for a code: event handlers by event code, process handlers by process code, meters by step or process and measurement code. |
| ③ Start and plan | `AbstractProcessor`, pre- and post-processors, `DefaultMeasurementProcessor`, meters | Create a process instance and its steps, and compute their planned measurements. |
| ④ Record | `StepProgressService`, `ActualMeasurementService`, `StepPlanner` | Advance steps, record actual values, judge timeliness, forecast delays, complete or cancel processes. |
| ⑤ Watch | `OverdueStepMonitor`, `OverdueProcessMonitor` | Find steps and processes past their deadline with no event. |
| ⑥ Definitions and state | `DefinitionLoader`, `ProcessDefinitionService`, `StepDefinitionService`, instance services and repositories, `AktimetrixStorageInitializer` | Load and resolve definitions; read and save instances with version checks; prepare the database. |
| ⑦ Outbound | publishers, `Outbox`, `OutboxRelay` | Queue every result with the state it describes, and publish it to the broker. |
| Cross-cutting | `AktimetrixAutoConfiguration`, `AktimetrixDefaultProperties`, `AktimetrixProperties`, `AktimetrixMetrics`, REST resources | Wiring, configuration, metrics and the query API. |

## Flows

### At startup

1. `AktimetrixAutoConfiguration` registers the framework's components in the application, and
   `AktimetrixDefaultProperties` supplies default binding properties, which the application can override.
2. The `*PostBeanProcessor`s find every bean annotated `@EventHandler`, `@ProcessHandler`, `@Measurement`,
   `@PreProcessor` or `@PostProcessor` and register it in `DefaultRegistry` under its codes. A misconfigured meter,
   for example one naming both a step and a process, stops the application here.
3. Before any event is consumed:
   - `AktimetrixStorageInitializer` adds a `revision` to instances saved by earlier builds and creates the indexes;
   - `DefinitionLoader` upserts the process and step definitions from `aktimetrix/*.json`.

### When an event arrives

1. **Consume.** `ProcessConfig.processor()` receives the message and asks the `EventMapper` for an event. A message the
   mapper cannot read, or an event without tenant, event code or entity id, goes to the dead-letter topic through the
   outbox; one it returns `null` for is ignored.
2. **Unit of work.** The rest runs in `AktimetrixTransactions.run(…)`: in one MongoDB transaction when the deployment
   supports it. If anything fails, nothing is kept, and the binder retries the message, then dead-letters it.
3. **Route.** The registry supplies the `@EventHandler` for the event code, or `DefaultEventHandler`.
4. **Start** (`AbstractEventHandler`). For every confirmed process definition the event code starts,
   `ProcessDefinitionService` resolves its steps: the tenant's shared step definitions, overridden by the fields the
   process sets. The registry supplies the process's `@ProcessHandler`, or `DefaultProcessor`, which (`AbstractProcessor`):
   1. runs the pre-processors of the process type;
   2. creates the process instance, with its own deadline if it has one, and its step instances, unless the entity
      already has one;
   3. runs the process-level meters, then, per step, `DefaultMeasurementProcessor` runs the step's meters and turns a
      planned `TIME` into `plannedAt`;
   4. `StepPlanner` plans the steps that have durations, and sets every deadline (`lateAfter`);
   5. runs the post-processors, the built-in publishers last, which queue the `CREATED` process and step events.
5. **Record** (`StepProgressService.recordMilestones`). For each running process of the entity:
   - if the event is one of the process's cancel events, the process and its open steps are cancelled;
   - otherwise each step that lists the event is started or completed. A completed step gets its actual `TIME`, its
     other actual measurements from `ActualMeasurementService`, and its timeliness. `StepPlanner` then plans the steps
     that follow it and forecasts the later ones; those pushed past their deadline become `AT_RISK`;
   - when the last mandatory step completes, the process completes, is judged against its own deadline, and records its
     actual measurements.

   Every change is saved and its event queued in the outbox.
6. **Commit.** The transaction commits: state and queued events together.

### When a deadline passes

Every minute, on every instance, `OverdueStepMonitor` and `OverdueProcessMonitor` query the steps and processes whose
`lateAfter` has passed, that are not completed or cancelled and not yet overdue. Each is marked `OVERDUE` in its own
transaction, with an `OVERDUE` event queued, and, for a step, the later steps are forecast. Saves are version-checked:
if another instance, or the step's event, changed it since it was read, the save fails and the monitor moves on.

### When results are published

`OutboxRelay` runs every second on every instance. It claims the oldest unsent outbox entry with an atomic
find-and-modify that sets a lease, sends it with `StreamBridge` to its binding (`process-instance-out-0`,
`step-instance-out-0`, `measurement-instance-out-0` or `dead-letter-out-0`), and marks it sent. An entry whose send fails
keeps its lease until it expires, then any instance retries it. Sent entries are purged after the retention period.

## Concurrency and consistency

| Concern | Mechanism |
|---|---|
| Events of one entity processed in order | The broker's per-key ordering; producers key events by entity id. |
| One process instance per entity | Unique index on tenant, process code, entity type and entity id. |
| Two changes to the same step or process | `@Version revision` on step and process instances: the second save fails instead of overwriting. |
| State and outbound events | One transaction per event, or per overdue step or process, when MongoDB supports transactions. |
| Outbox shared by several instances | Leases claimed with an atomic find-and-modify. |
| Replayed events | A process that exists is not created again, and a completed step is not completed again. |

## Adapting Aktimetrix

| To… | Change | Where |
|---|---|---|
| Accept another event format | Declare an `EventMapper` bean. | Your application; see [Accepting your own event format](extending.md#accepting-your-own-event-format). |
| Change how an event is interpreted | Add an `@EventHandler` for its event code. | Your application. |
| Choose metadata, validate or enrich | Add a `@ProcessHandler`, `@PreProcessor` or `@PostProcessor`. | Your application. |
| Compute plans or actual values | Add a `@Measurement` meter. | Your application. |
| Use another message broker | Replace the Kafka binder dependency with another Spring Cloud Stream binder, and adapt the binder-specific parts: the dead-letter and key-serializer defaults, and the message-key header the relay sets. | `pom.xml`, `AktimetrixDefaultProperties`, `OutboxRelay` |
| Use another state store | Provide the repositories for definitions and instances, the outbox claim (an atomic conditional update), transactions, and index creation for that store. | `repository`, `referencedata.repository`, `OutboxRelay`, `AktimetrixTransactions`, `AktimetrixStorageInitializer`, and the queries in the definition services |

The last two are not yet pluggable: they mean changing the framework rather than configuring it, and are on the
[roadmap](../README.md#11-status-and-roadmap).

## Source layout

```
aktimetrix-core/src/main/java/com/aktimetrix/
├── autoconfigure/             auto-configuration and default properties
└── core/
    ├── api/                   public interfaces: EventMapper, Pre/PostProcessor, Context, Timeliness …
    ├── stereotypes/           @Measurement, @ProcessHandler, @EventHandler, @PreProcessor, @PostProcessor
    ├── meter/                 Meter, ProcessMeter and their base classes
    ├── event/                 the default event mapper, and event handlers
    ├── impl/                  AbstractProcessor, DefaultProcessor, the registry, event generators
    ├── model/                 process, step and measurement instances
    ├── referencedata/         definitions: model, loading, resolution, repositories, REST
    ├── service/               progress, planning, actual measurements, monitors, publishers, metrics
    ├── outbox/                the outbox and its relay
    ├── storage/               transactions, indexes and data upgrades
    ├── configurations/        aktimetrix.* properties and the inbound consumer
    ├── repository/            repositories of instances
    ├── resource/              the query API
    └── transferobjects/       the event envelope and outbound payloads
```

---

**Next:** [Extending Aktimetrix](extending.md)
