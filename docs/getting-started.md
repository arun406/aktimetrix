# Getting started

[← Back to README](../README.md)

> This guide describes the **reference implementation**, which binds the message broker to Apache Kafka and the
> state store to MongoDB. The model itself is technology-neutral: see the [README](../README.md#42-infrastructure-contract).

## Run the reference project

The [Order Monitor](https://github.com/arun406/aktimetrix-reference-project-order-monitor) is a complete Aktimetrix
application: it monitors order delivery (placed → shipped within 2 hours → delivered within 10 hours). You need
**JDK 11+** and **Docker**.

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

In a second terminal, send order `1234`'s events and check on it after each one:

```bash
send() { docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh \
           --bootstrap-server localhost:9092 --topic order-events < "events/$1"; }

send order-placed.json
curl -s 'http://localhost:8080/process-instances?tenant=AA&entityId=1234'   # SHIP planned 01:46, DELIVER 09:46

send order-shipped.json
curl -s 'http://localhost:8080/process-instances?tenant=AA&entityId=1234'   # SHIP ON_TIME

send order-delivered.json
curl -s 'http://localhost:8080/process-instances?tenant=AA&entityId=1234'   # DELIVER LATE, process Completed
```

The reference project's README explains each result, and `./mvnw test` runs the same story against an embedded
Kafka and an in-memory MongoDB, with no Docker needed.

## Build a monitor step by step

This section builds the order monitor from scratch.

### 1. Add the dependency

```xml
<dependency>
    <groupId>com.aktimetrix</groupId>
    <artifactId>aktimetrix-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

It brings Spring Web, Spring Data MongoDB and Spring Cloud Stream with the Kafka binder, and configures itself
through Spring Boot auto-configuration. Your application class is a plain `@SpringBootApplication`.

### 2. Point it at Kafka and MongoDB

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
    "startEventCodes": ["ORDER_PLACED_EVENT"],
    "status": "CONFIRMED",
    "steps": [ { "stepCode": "PLACE" }, { "stepCode": "SHIP" }, { "stepCode": "DELIVER" } ]
  }
]
```

`step-definitions.json` says which event completes each step, and which steps have a planned time (`P`):

```json
[
  { "tenant": "AA", "stepCode": "PLACE", "status": "CONFIRMED",
    "startEventCodes": ["ORDER_PLACED_EVENT"] },
  { "tenant": "AA", "stepCode": "SHIP", "status": "CONFIRMED",
    "startEventCodes": ["ORDER_SHIPPED_EVENT"],
    "measurements": [ { "measurementCode": "TIME", "type": "P" } ] },
  { "tenant": "AA", "stepCode": "DELIVER", "status": "CONFIRMED",
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
{ "tenant": "AA", "stepCode": "DELIVER", "status": "CONFIRMED",
  "startEventCodes": ["ORDER_DELIVERED_EVENT"],
  "plannedAfter": "SHIP", "plannedWithin": "PT8H", "tolerance": "PT30M" }
```

`DELIVER` is then planned 8 hours after the order actually ships, and counts as late 30 minutes after that.

For plans you compute, write a meter instead, as below.

#### Meters

A meter computes a step's planned time in code, for example from business hours or a customer's service level.
`@Measurement(code, stepCode)` must match a planned measurement in the step's definition. The step's metadata holds what you need, here the order time:

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
        // orders should ship within 2 hours of being placed
        return String.valueOf(metadataTime(step, "orderedOn").plusHours(2));
    }
}
```

Write one meter per planned step (`OrderDeliveredPlanTimeMeter` returns *ordered + 10 h*). A planned `TIME` must be an
ISO-8601 local date-time, which `String.valueOf(LocalDateTime)` produces. `metadataTime` reads a date-time from the
metadata whether it is stored as a `LocalDateTime`, a `Date`, or a string.

That is a working monitor: events start the process, meters plan it, milestone events complete its steps, and the
overdue monitor watches the deadlines.

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
        return Map.of("orderId", order.getOrderId(), "orderedOn", order.getOrderedOn());
    }
}
```

### 6. Optional: read the event time from the entity

A step's actual time is when its event *happened*. Aktimetrix reads it from the event envelope. If your events carry
it inside the entity instead, for example `"shippedAt"`, add an event handler for that event code and override
`occurredAt`:

```java
@Component
@EventHandler(eventType = "ORDER_SHIPPED_EVENT")
public class OrderShippedEventHandler extends AbstractMilestoneEventHandler {

    @Override
    protected LocalDateTime occurredAt(Event<?, ?> event) {
        Map<?, ?> order = (Map<?, ?>) event.getEntity();
        return LocalDateTime.parse(order.get("shippedAt").toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
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
  "eventId": "51541182-81fa-4727-afd5-114acdf086b1",
  "eventType": "ORDER",
  "eventCode": "ORDER_PLACED_EVENT",
  "eventName": "Order placed",
  "eventTime": "2022-05-22T23:46:00.000+0000",
  "eventUTCTime": "2022-05-22 23:46:00",
  "source": "shop",
  "entityType": "com.ecom.order",
  "entityId": "1234",
  "entity": { "orderId": "1234", "orderedOn": "2022-05-22 23:46:00", "customerId": "1" },
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

Events with a missing `tenantKey`, `eventCode` or `entityId` are logged and skipped. Publish all events of one entity
with the entity id as the Kafka key, so they are processed in order.

---

**Next:** [Extending Aktimetrix](extending.md)
