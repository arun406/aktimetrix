# Extending Aktimetrix

[← Back to README](../README.md)

A monitor needs only definitions and meters. Everything else has a default you can replace with a Spring bean that
carries a stereotype annotation; Aktimetrix discovers it at startup.

## Extension points

| Annotation | Extend / implement | Selected by | Default when absent | Purpose |
|---|---|---|---|---|
| a bean of type `EventMapper` | `api.EventMapper` | n/a: one per application | `EnvelopeEventMapper`: messages are in the Aktimetrix event envelope | Reads your own event format; see [Accepting your own event format](#accepting-your-own-event-format). |
| `@Measurement(code, stepCode)` | `AbstractMeter` or `meter.api.Meter` | step code + measurement code | none: the value is skipped | Computes a planned measurement of a step when it is created, and, by overriding `getActualValue`, an actual one when it completes. |
| `@Measurement(code, processCode)` | `AbstractProcessMeter` or `meter.api.ProcessMeter` | process code + measurement code | none: the value is skipped, with a warning | Computes a planned measurement of the process as a whole when it is created, and, by overriding `getActualValue`, an actual one when it completes. |
| `@ProcessHandler(processType)` | `AbstractProcessor` | the process code | `DefaultProcessor`: the event's entity becomes the metadata | Chooses the metadata of the process and its steps. |
| `@EventHandler(eventType)` | `AbstractEventHandler` | the event code | `DefaultEventHandler` | Starts processes and records milestones; override to read the entity id or event time differently. |
| `@EventHandler(eventType)` | `AbstractMilestoneEventHandler` | the event code | `DefaultEventHandler` | Records milestones only, for events that never start a process. |
| `@PreProcessor(code, processType, priority)` | `api.PreProcessor` | the process type, or `*` | none | Runs before a process instance is created: validate, enrich. |
| `@PostProcessor(code, processType, priority)` | `api.PostProcessor` | the process type, or `*` | none | Runs after a process instance is created and planned: notify, integrate. |
| `@Loggable` | any bean with an interface | n/a | n/a | Logs entry, exit, and execution time of its methods. |

**Process type.** Pre- and post-processors are selected by the process definition's `processType`, which defaults
to its `processCode`, so `processType = "ORDER_DELIVERY"` targets that process. Use `processType = "*"` for every
process. Several processors for one process run in ascending `priority` order; the default is `1`, and the built-in
publishers run last with `1000`.

### Example: enrich the order before the process starts

```java
@Component
@PreProcessor(code = "CUSTOMER_TIER", processType = "ORDER_DELIVERY")
public class CustomerTierPreProcessor implements com.aktimetrix.core.api.PreProcessor {

    private final CustomerClient customers;

    public CustomerTierPreProcessor(CustomerClient customers) {
        this.customers = customers;
    }

    @Override
    public void preProcess(Context context) {
        Map<String, Object> order = (Map<String, Object>) context.getProperty(Constants.ENTITY);
        order.put("customerTier", customers.tierOf((String) order.get("customerId")));
    }
}
```

A meter can then plan from `customerTier`, for example giving premium customers a shorter delivery deadline.

### Example: act on a newly planned process

Post-processors see the created process instance and its planned steps:

```java
@Slf4j
@Component
@PostProcessor(code = "LATE_EVENING_ORDERS", processType = "ORDER_DELIVERY")
public class LateEveningDeliveryWarning implements com.aktimetrix.core.api.PostProcessor {

    private static final LocalTime CUT_OFF = LocalTime.of(20, 0);

    @Override
    public void postProcess(Context context) {
        context.getProcessInstance().getSteps().stream()
                .filter(step -> "DELIVERED".equals(step.getStepCode()) && step.getPlannedAt() != null)
                .filter(step -> step.getPlannedAt().toLocalTime().isAfter(CUT_OFF))
                .forEach(step -> log.warn("Order {} is planned for delivery after {}",
                        context.getProperty(Constants.ENTITY_ID), CUT_OFF));
    }
}
```

To react to steps becoming late or overdue, consume `step-instance-out-0` instead: see the
[configuration reference](configuration.md#channels).

## Accepting your own event format

Source systems do not have to adopt the Aktimetrix envelope. Declare an `EventMapper` bean to turn each message on
the inbound topic into an Aktimetrix event:

```java
@Bean
EventMapper shopEvents(ObjectMapper json) {
    return (payload, headers) -> {
        JsonNode order = json.readTree(payload);           // {"id":"1234","status":"DELIVERED","updatedAt":"…"}
        if (!order.has("status")) {
            return null;                                   // not an order event: ignored
        }
        Event<Object, Object> event = Event.of("AA", "ORDER_" + order.get("status").asText() + "_EVENT",
                "com.ecom.order", order.get("id").asText(), ZonedDateTime.parse(order.get("updatedAt").asText()));
        event.setEntity(json.convertValue(order, Map.class));   // becomes metadata
        return event;
    };
}
```

| The mapper… | Aktimetrix… |
|---|---|
| returns an event | processes it; tenant, event code and entity id are required |
| returns `null` | ignores the message, counted with outcome `ignored` |
| throws | sends the message unchanged to the dead-letter topic, counted with outcome `invalid` |

## Public API

These are the types an application uses. Everything else is internal and may change between releases; each
package's `package-info.java` says which it is.

| Package | Types | Use |
|---|---|---|
| `core.stereotypes` | `@Measurement`, `@ProcessHandler`, `@EventHandler`, `@PreProcessor`, `@PostProcessor` | Register your components. |
| `core.meter.impl`, `core.meter.api` | `AbstractMeter`, `AbstractProcessMeter`, `Meter`, `ProcessMeter` | Compute planned and actual measurements. |
| `core.api` | `EventMapper`, `PreProcessor`, `PostProcessor`, `Context`, `Timeliness`, `Constants` | Read your own event format; hook into process creation; read the processing context. |
| `core.impl` | `AbstractProcessor`, `DefaultProcessor` | Choose the metadata of a process and its steps. |
| `core.event.handler` | `AbstractEventHandler`, `AbstractMilestoneEventHandler` | Change how an event code is interpreted. |
| `core.transferobjects` | `Event` | The event envelope, inbound and outbound. |
| `core.model`, `core.referencedata.model` | `ProcessInstance`, `StepInstance`, `MeasurementInstance`, `ProcessDefinition`, `StepDefinition`, `MeasurementDefinition` | Read instances and definitions in your components. |
| `core.store` | `ProcessInstanceStore`, `StepInstanceStore`, `MeasurementInstanceStore`, `DefinitionStore`, `OutboxStore`, `AktimetrixTransactions`, `StoreDocuments` | Implement a state store. |

In a `Context`, read the event's data with the `Constants` context properties: `ENTITY`, `ENTITY_ID`,
`ENTITY_TYPE`, `EVENT`, `EVENT_DATA`, `PROCESS_DEFINITION` and `OCCURRED_AT`.

## Built-in components

| Component | Role |
|---|---|
| `DefaultEventHandler` | Handles every event without its own `@EventHandler`: starts the processes it starts, then records it as a milestone. |
| `DefaultProcessor` | Process handler for processes without their own `@ProcessHandler`. |
| `DefaultMeasurementProcessor` | Runs the meters of each new step and sets its `plannedAt` from the planned `TIME`. |
| `StepPlanner` | Plans steps from the durations in their definitions, sets deadlines, and forecasts the steps at risk. |
| `StepProgressService` | Moves steps through their lifecycle, records actual times, judges `ON_TIME` / `LATE`, and completes, ends or cancels processes. |
| `ActualMeasurementService` | Records the actual measurements of a step or process, and interim readings on progress events, each compared with its plan. |
| `DerivedMetricService` | Computes a process's declared metrics when it completes, from the plan and from the actuals. |
| `OverdueStepMonitor`, `OverdueProcessMonitor` | Mark steps and processes past their deadline as `OVERDUE`; an overdue step puts later steps at risk. |
| `DefinitionLoader` | Loads `aktimetrix/*.json` definitions at startup. |
| `ProcessInstancePublisherService`, `StepInstancePublisherService`, `MeasurementInstancePublisherService` | Queue events for the outbound topics in the outbox. |
| `OutboxRelay` | Publishes queued events to the broker. |
| `AktimetrixMetrics` | Records the Micrometer metrics. |

## Adding a state store

A store module keeps Aktimetrix's state in a database of its own. It implements the interfaces of `core.store` and
declares them in a Spring Boot auto-configuration; the core uses nothing else.

| Interface | Must provide |
|---|---|
| `ProcessInstanceStore`, `StepInstanceStore` | Insert with a new id; update with a version check on `revision`, throwing `OptimisticLockingFailureException` on a stale copy; at most one process instance per tenant, process and entity (`DuplicateKeyException`); the queries of the overdue monitors. |
| `MeasurementInstanceStore` | Insert, and find by process, step, code and type. |
| `DefinitionStore` | Save definitions, replacing the one with the same tenant and code; find them, and the confirmed processes an event starts. |
| `OutboxStore` | An atomic claim of the oldest unsent message, with a lease: the one operation that needs a compare-and-set. |
| `AktimetrixTransactions` | Run a unit of work atomically if the store can, and say whether it does. |

`StoreDocuments` converts model objects to and from JSON, for stores that keep documents; the JDBC and in-memory
stores use it. Follow `aktimetrix-store-jdbc`: its auto-configuration is conditional on
`aktimetrix.storage.type` and on no other store being present, and prepares the database before the stores are used.

A new store is correct when it passes the contract tests: add a subclass of `StoreContractTest` in `aktimetrix-tests`
that starts it, as `JdbcStoreContractTest` does, and a subclass of the end-to-end scenarios that runs on it.

## Adding a message broker

A broker module connects Aktimetrix to a broker through a Spring Cloud Stream binder. It adds the binder dependency
and an `EnvironmentPostProcessor`, registered in `META-INF/spring.factories`, with the lowest-precedence defaults the
binder needs:

- events that still fail after the binder's retries are dead-lettered to `aktimetrix.events.dead-letter.topic`,
  where invalid events are sent through the `dead-letter-out-0` binding;
- the `aktimetrixKey` header of each outbound message becomes the broker's message key, so the events of one instance
  stay in order;
- the inbound events are consumed in order per entity, which the broker contract of the white paper requires.

`aktimetrix-broker-kafka` and `aktimetrix-broker-rabbitmq` show both. Add a `TestBroker` for the new broker in
`aktimetrix-tests`, and subclasses of the end-to-end scenarios that run on it.

## Modelling your own process

Monitoring a new domain is mostly writing new definitions. A loan-origination process might look like this:

```json
{
  "tenant": "BANK01",
  "processCode": "LOAN_ORIGINATION",
  "processName": "Retail loan origination",
  "entityType": "com.bank.loan.application",
  "startEventCodes": ["LOAN_APPLICATION_SUBMITTED"],
  "status": "CONFIRMED",
  "steps": [
    { "stepCode": "KYC" },
    { "stepCode": "CREDIT_CHECK" },
    { "stepCode": "APPROVAL" },
    { "stepCode": "DISBURSEMENT" }
  ]
}
```

Then:

1. define each step with the event that completes it, e.g. `APPROVAL` completed by `LOAN_APPROVED`, and a planned
   `TIME` where there is a deadline;
2. give a step a fixed deadline with `plannedWithin`, or write a `@Measurement(code = "TIME", stepCode = "…")` meter
   for a deadline that follows a rule, for example `APPROVAL` at *submitted + 48 business hours*;
3. optionally, add a `@ProcessHandler(processType = "LOAN_ORIGINATION")` that keeps the applicant and product
   details as metadata.

---

**Next:** [Configuration & API reference](configuration.md)
