# Getting started

[← Back to README](../README.md)

> This guide describes the **reference implementation**. Its examples use MongoDB and Kafka, as the reference project
> does; any store and broker module can be used instead, see [Choosing a store and a broker](configuration.md#choosing-a-store-and-a-broker).
> The model itself is technology-neutral: see the [README](../README.md#52-infrastructure-contract).

## Run the reference project

The [Order Monitor](https://github.com/arun406/aktimetrix-reference-project-order-monitor) is a complete Aktimetrix
application: it monitors the order delivery process of the [worked example](../README.md#11-a-worked-example-order-delivery),
seven steps from order confirmation to the customer's rating, in time, distance, fuel, temperature, cost and rating.
You need **JDK 11+** and **Docker**.

```bash
# 1. Build and install the framework (it is not on Maven Central yet)
git clone https://github.com/arun406/aktimetrix.git
(cd aktimetrix && ./mvnw install -DskipTests)

# 2. Start Kafka and MongoDB, then the monitor
git clone https://github.com/arun406/aktimetrix-reference-project-order-monitor.git
cd aktimetrix-reference-project-order-monitor
docker compose up -d
./mvnw spring-boot:run
```

In a second terminal, send order `1234`'s ten events one at a time, and check on it after each one:

```bash
send() { docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh \
           --bootstrap-server localhost:9092 --topic order-events < "events/$1"; }
where() { curl -s 'http://localhost:8080/process-instances?tenant=AA&entityId=1234'; }

send 01-order-created.json; where      # seven steps planned; DELIVERED by 12:15, the order by tomorrow 09:00
send 02-order-confirmed.json; where    # CONFIRM ON_TIME
# … and so on, up to
send 09-delivered.json; where          # DELIVERED LATE, 40 °C instead of 30; the order Completed, ON_TIME
send 10-rated.json; where              # 4 stars instead of 5, within tolerance
```

The reference project's README explains each result, and `./mvnw test` runs the same story against an embedded
Kafka and an in-memory MongoDB, with no Docker needed.

## Build a monitor step by step

This section builds a smaller version of the same order monitor from scratch: an order is created, confirmed, paid
and delivered, and a priority customer's order must be delivered sooner. The reference project adds the other steps
and measurements the same way.

### 1. Add the dependencies

The core, a store module and a broker module:

```xml
<dependency>
    <groupId>com.aktimetrix</groupId>
    <artifactId>aktimetrix-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.aktimetrix</groupId>
    <artifactId>aktimetrix-store-mongodb</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.aktimetrix</groupId>
    <artifactId>aktimetrix-broker-kafka</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

The core brings Spring Web and Spring Cloud Stream; the modules bring Spring Data MongoDB and the Kafka binder. It all
configures itself through Spring Boot auto-configuration: your application class is a plain `@SpringBootApplication`.
To keep the state in PostgreSQL instead, use `aktimetrix-store-jdbc` and the PostgreSQL driver; to use RabbitMQ,
`aktimetrix-broker-rabbitmq`. To try Aktimetrix without a database, `aktimetrix-store-memory`.

### 2. Point it at the broker and the store

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
    topic: order-events   # the topic your systems publish business events to
```

Everything else has defaults; see the [configuration reference](configuration.md).

### 3. Describe the process

Definitions live with your code under `src/main/resources/aktimetrix/` and are loaded at startup, replacing any
existing definition with the same tenant and code.

`process-definitions.json` names the process, the entity it follows, the events that start it, and its steps in
order:

```json
[
  {
    "tenant": "AA",
    "processCode": "ORDER_DELIVERY",
    "processName": "Order delivery",
    "entityType": "com.ecom.order",
    "startEventCodes": ["ORDER_CREATED_EVENT"],
    "status": "CONFIRMED",
    "steps": [ { "stepCode": "CONFIRM" }, { "stepCode": "PAY" }, { "stepCode": "DELIVERED" } ]
  }
]
```

`step-definitions.json` says which event completes each step, and which steps have a planned time (`P`):

```json
[
  { "tenant": "AA", "stepCode": "CONFIRM", "status": "CONFIRMED",
    "startEventCodes": ["ORDER_CONFIRMED_EVENT"] },
  { "tenant": "AA", "stepCode": "PAY", "status": "CONFIRMED",
    "startEventCodes": ["PAYMENT_CONFIRMED_EVENT"] },
  { "tenant": "AA", "stepCode": "DELIVERED", "status": "CONFIRMED",
    "startEventCodes": ["ORDER_DELIVERED_EVENT"],
    "measurements": [ { "measurementCode": "TIME", "type": "P" } ] }
]
```

A step with only `startEventCodes` completes on that event. Give it `endEventCodes` too when it has a duration: it
is then `Started` by the start event and `Completed` by the end event. Mark a step `"optionalInd": "Y"` if the
process can complete without it.

A step can also be written directly in the process's `steps`, with the same fields. That suits a step only one
process uses, or a shared step one process treats differently, such as a shorter deadline for express orders: see
[step definition fields](configuration.md#step-definition-fields).

### 4. Plan the steps

The simplest plans need no code. Give a step a duration, from the process start or from another step's completion,
and optionally a tolerance:

```json
{ "tenant": "AA", "stepCode": "PAY", "status": "CONFIRMED",
  "startEventCodes": ["PAYMENT_CONFIRMED_EVENT"],
  "plannedAfter": "CONFIRM", "plannedWithin": "PT15M", "tolerance": "PT5M" }
```

`PAY` is then planned 15 minutes after the order is actually confirmed, and counts as late 5 minutes after that.

Time is only one measurement. A fixed plan for any other one needs no code either, for example a planned rating
with a tolerance, compared with the actual rating read from the event that completes the step:

```json
"measurements": [
  { "measurementCode": "RATING", "type": "P", "value": "5", "tolerance": "1" },
  { "measurementCode": "RATING", "type": "A", "valueFrom": "review.stars" }
]
```

For plans computed by rules, such as a shorter delivery for priority customers, write a meter instead, as below.

#### Meters

A meter computes a plan in code, for example from business hours or a customer's service level.
`@Measurement(code, stepCode)` must match a planned measurement in the step's definition. The step's metadata holds
what you need, here when the order was created and whether the customer is a priority customer:

```java
@Component
@Measurement(code = "TIME", stepCode = "DELIVERED")
public class DeliveryPlanMeter extends AbstractMeter {

    @Override
    protected String getMeasurementUnit(String tenant, StepInstance step) {
        return "TIMESTAMP";
    }

    @Override
    protected String getMeasurementValue(String tenant, StepInstance step) {
        // priority customers within 3 h 15 min of the order, others within 2 days
        boolean priority = Boolean.TRUE.equals(step.getMetadata().get("priority"));
        return String.valueOf(priority
                ? metadataTime(step, "createdAt").plusHours(3).plusMinutes(15)
                : metadataTime(step, "createdAt").plusDays(2));
    }
}
```

Write one meter per plan that follows a rule; for the order as a whole, extend `AbstractProcessMeter` and name
`processCode` instead of `stepCode`. A planned `TIME` must be an
ISO-8601 local date-time, which `String.valueOf(LocalDateTime)` produces. `metadataTime` reads a date-time from the
metadata whether it is stored as a `LocalDateTime`, a `Date`, or a string.

That is a working monitor: events start the process, meters plan it, milestone events complete its steps, and the
alarms at the deadlines mark any step whose event does not arrive in time as overdue.

#### The same monitor, in Java or YAML

JSON is one of three equivalent forms. With the **Java DSL**, the definitions and their rules are one bean, and the
meter above becomes a lambda:

```java
import static com.aktimetrix.core.definitions.Planning.metadataTime;

@Configuration
public class OrderDefinitions {

    @Bean
    Definitions orderDelivery() {
        return Definitions.tenant("AA")
                .process("ORDER_DELIVERY", order -> order
                        .name("Order delivery")
                        .entityType("com.ecom.order")
                        .startsOn("ORDER_CREATED_EVENT")
                        .step("CONFIRM", step -> step.on("ORDER_CONFIRMED_EVENT"))
                        .step("PAY", step -> step.on("PAYMENT_CONFIRMED_EVENT")
                                .after("CONFIRM").within("PT15M").tolerance("PT5M"))
                        .step("DELIVERED", step -> step.on("ORDER_DELIVERED_EVENT")
                                .planTime(s -> Boolean.TRUE.equals(s.getMetadata().get("priority"))
                                        ? metadataTime(s, "createdAt").plusHours(3).plusMinutes(15)
                                        : metadataTime(s, "createdAt").plusDays(2)))
                        .step("RATED", step -> step.on("ORDER_RATED_EVENT").optional()
                                .measure("RATING", "review.stars", r -> r.value(5).tolerance("1"))))
                .build();
    }
}
```

| DSL | Definition field |
|---|---|
| `startsOn`, `endsOn`, `cancelledOn` on a process | `startEventCodes`, `endEventCodes`, `cancelEventCodes` |
| `on` on a step (a milestone); `startsOn` and `endsOn` (a step with a duration); `progressOn` | `startEventCodes`; `startEventCodes` and `endEventCodes`; `progressEventCodes` |
| `after`, `within`, `tolerance`, `optional()` | `plannedAfter`, `plannedWithin`, `tolerance`, `"optionalInd": "Y"` |
| `plan(code, m -> ...)`, `actual(code, valueFrom, unit)`, `measure(code, valueFrom, m -> ...)` | a planned (`P`) measurement; an actual (`A`) one; both |
| `planTime(rule)`, `plan(code, unit, rule)` | a planned measurement without a value, and a meter that computes it |
| `metric(code, expression, m -> ...)` | an entry of `metrics` |
| `.step(code)` with no body | a reference to the tenant's shared step, defined with `Definitions.tenant(...).step(...)` |

Definitions built by the DSL default to status `CONFIRMED`. A rule plans one measurement of one step or process, like
a meter, but only for its own tenant and, on a step of a process, only for that process: an express and a standard
process can each plan their `DELIVERED` step by a rule of their own. When several could plan the same measurement, the
most specific wins: a rule on the process's step, then a rule on the tenant's shared step, then an `@Measurement`
meter. Two rules for the same measurement in the same place fail at startup.

As **YAML**, any file `src/main/resources/aktimetrix/*.yaml` (or `*.yml`) holds one tenant's steps and processes,
with the fields of the JSON files:

```yaml
tenant: AA
processes:
  - processCode: ORDER_DELIVERY
    processName: Order delivery
    entityType: com.ecom.order
    status: CONFIRMED
    startEventCodes: [ORDER_CREATED_EVENT]
    steps:
      - { stepCode: CONFIRM, startEventCodes: [ORDER_CONFIRMED_EVENT] }
      - { stepCode: PAY, startEventCodes: [PAYMENT_CONFIRMED_EVENT], plannedAfter: CONFIRM, plannedWithin: PT15M, tolerance: PT5M }
      - stepCode: DELIVERED
        startEventCodes: [ORDER_DELIVERED_EVENT]
        measurements:
          - { measurementCode: TIME, type: P }   # planned by DeliveryPlanMeter
steps: []                                      # the tenant's shared steps, if any
```

The forms can be mixed: a process in YAML may list shared steps defined in JSON, and a rule can be a lambda or a
meter. All are loaded at startup and saved by tenant and code.

#### Mistakes are caught at startup

Definitions are checked as they are loaded, whatever their form, and the application does not start until they are
right. The error lists every problem, each with the file or bean it is in:

```
Invalid Aktimetrix definitions:
  - URL [.../aktimetrix/parcel.yaml]: process PARCEL, step PICKUP: plannedWithin is not an ISO-8601 duration such as PT2H or P1D: 2 hours
  - URL [.../aktimetrix/parcel.yaml]: process PARCEL, step DELIVER: plannedAfter names SORT, which is not a step of the process
```

A misspelt field in a YAML or JSON file, such as `plannedWitin`, is an error too, rather than being silently
ignored. The checks cover the codes and start events a process needs, durations, tolerances, `worseWhen`,
`optionalInd`, and steps that `plannedAfter` names.

### 5. Optional: choose the metadata

By default, the event's entity becomes the metadata of the process and of every step. To keep only what you need,
or to convert it, add a process handler for the process code:

```java
@Component
@ProcessHandler(processType = "ORDER_DELIVERY")
public class OrderProcessor extends AbstractProcessor {

    private final ObjectMapper objectMapper;

    public OrderProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected Map<String, Object> getProcessMetadata(Context context) {
        Order order = objectMapper.convertValue(context.getProperty(Constants.ENTITY), Order.class);
        return Map.of("orderId", order.getOrderId(), "customerId", order.getCustomerId());
    }

    @Override
    protected Map<String, Object> getStepMetadata(Context context) {
        Order order = objectMapper.convertValue(context.getProperty(Constants.ENTITY), Order.class);
        return Map.of("orderId", order.getOrderId(), "priority", order.isPriority(), "createdAt", order.getCreatedAt());
    }
}
```

### 6. Optional: read the event time from the entity

A step's actual time is when its event *happened*. Aktimetrix reads it from the event envelope. If your events carry
it inside the entity instead, for example `"deliveredAt"`, add an event handler for that event code and override
`occurredAt`:

```java
@Component
@EventHandler(eventType = "ORDER_DELIVERED_EVENT")
public class OrderDeliveredEventHandler extends AbstractMilestoneEventHandler {

    @Override
    protected LocalDateTime occurredAt(Event<?, ?> event) {
        Map<?, ?> order = (Map<?, ?>) event.getEntity();
        return LocalDateTime.parse(order.get("deliveredAt").toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
```

Extend `AbstractEventHandler` instead for an event that can also start a process. See
[Extending Aktimetrix](extending.md) for the other extension points.

## The event format

By default, business events use the envelope below, with your domain object in `entity`. If your systems already
publish events in another format, keep it and declare an `EventMapper` instead: see
[Accepting your own event format](extending.md#accepting-your-own-event-format).

```json
{
  "tenantKey": "AA",
  "eventId": "00000000-0000-0000-0000-000012340000",
  "eventType": "ORDER",
  "eventCode": "ORDER_CREATED_EVENT",
  "eventName": "Order created",
  "eventTime": "2024-03-01T09:00:00.000+0000",
  "eventUTCTime": "2024-03-01 09:00:00",
  "source": "shop",
  "entityType": "com.ecom.order",
  "entityId": "1234",
  "entity": { "orderId": "1234", "createdAt": "2024-03-01 09:00:00", "customerId": "C-42", "priority": true },
  "eventDetails": {}
}
```

| Field | Required | Used for |
|---|---|---|
| `tenantKey` | yes | Selects the tenant's definitions and instances. |
| `eventCode` | yes | Which processes the event starts, and which steps it completes. |
| `entityId` | yes | Identifies the business entity: all events of order `1234` carry `"1234"`. |
| `entityType` | yes | Must equal the process definition's `entityType`. |
| `eventTime` | recommended | When it happened (`yyyy-MM-dd'T'HH:mm:ss.SSSZ`), converted to `aktimetrix.time-zone`. |
| `eventUTCTime` | fallback | When it happened, in UTC (`yyyy-MM-dd HH:mm:ss`), if `eventTime` is absent. |
| `entity` | no | Your domain object; becomes metadata. |

Events with a missing `tenantKey`, `eventCode` or `entityId` are sent to the dead-letter channel. Publish all events of
one entity with the entity id as the message key (the Kafka record key, or the RabbitMQ routing key), so they are
processed in order.

---

**Next:** [Extending Aktimetrix](extending.md)
