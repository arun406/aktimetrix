# Getting started

[← Back to README](../README.md)

## Run the reference project

The quickest way to see Aktimetrix run is the
[**Order Monitor reference project**](https://github.com/arun406/aktimetrix-reference-project-order-monitor).

### Prerequisites

- JDK 11 or newer
- Docker (for local Kafka and MongoDB), or your own Kafka and MongoDB instances
- Git

### 1. Build and install the framework

Aktimetrix is not yet published to Maven Central, so install it into your local Maven repository:

```bash
git clone https://github.com/arun406/aktimetrix.git
cd aktimetrix
./mvnw clean install
```

### 2. Start Kafka and MongoDB

Save this as `docker-compose.yml` in the `aktimetrix` directory and run `docker compose up -d`:

```yaml
services:
  kafka:
    image: apache/kafka:3.7.0      # single-node KRaft broker on localhost:9092
    ports: ["9092:9092"]
  mongo:
    image: mongo:6
    ports: ["27017:27017"]
```

### 3. Load the reference data

```bash
cd ..
git clone https://github.com/arun406/aktimetrix-reference-project-order-monitor.git
cd aktimetrix-reference-project-order-monitor

for c in processDefinitions stepDefinitions eventTypeDefinitions measurementTypeDefinitions; do
  docker compose -f ../aktimetrix/docker-compose.yml exec -T mongo \
    mongoimport --db svm --collection "$c" --jsonArray < "src/main/resources/$c.json"
done
```

### 4. Run the monitor

Point the application at your local services (these override `application.yml`):

```bash
export SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/svm
export SPRING_KAFKA_PROPERTIES_BOOTSTRAP_SERVERS=localhost:9092
export SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL=PLAINTEXT
./mvnw spring-boot:run
```

### 5. Send a business event and watch the plan appear

```bash
# publish an ORDER_PLACED_EVENT
docker compose -f ../aktimetrix/docker-compose.yml exec -T kafka \
  /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 \
  --topic order-event-topic < requests/request1.json

# read the planned measurements
docker compose -f ../aktimetrix/docker-compose.yml exec kafka \
  /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic measurement-instance-out-0 --from-beginning
```

Each message on `measurement-instance-out-0` looks like this:

```json
{
  "tenantKey": "AA",
  "eventType": "Measurement_Event",
  "eventCode": "CREATED",
  "eventName": "Measurement Instance Created Event",
  "source": "Meter",
  "entityType": "com.aktimetrix.measurement.instance",
  "entity": {
    "tenant": "AA",
    "stepCode": "SHIP",
    "code": "TIME",
    "value": "2022-05-23T01:46",
    "unit": "TIMESTAMP",
    "type": "P",
    "processInstanceId": "62a1…",
    "stepInstanceId": "62a1…"
  }
}
```

## Build a monitor step by step

This section rebuilds the order monitor from scratch. You need three pieces of code: an **event handler**, a
**process handler**, and one **meter** per planned measurement.

### 1. Add the dependency

```xml
<dependency>
    <groupId>com.aktimetrix</groupId>
    <artifactId>aktimetrix-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2. Scan the Aktimetrix components

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.example.ordermonitor", "com.aktimetrix.core"})
public class OrderMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderMonitorApplication.class, args);
    }
}
```

### 3. Describe the process as reference data

A process definition ties the process to the events that start it and the steps it contains:

```json
{
  "tenant": "AA",
  "processCode": "ORDER_DELIVERY",
  "processName": "Order Delivery process",
  "categoryCode": "ORDER",
  "subCategoryCode": "DELIVERY",
  "entityType": "com.ecom.order",
  "startEventCodes": ["ORDER_PLACED_EVENT"],
  "status": "CONFIRMED",
  "steps": [ { "stepCode": "PLACE" }, { "stepCode": "SHIP" }, { "stepCode": "DELIVER" } ]
}
```

Each step definition declares what should be measured at that step (`P` = planned, `A` = actual):

```json
{
  "tenant": "AA",
  "stepCode": "SHIP",
  "stepName": "Order Shipped Step",
  "status": "CONFIRMED",
  "startEventCodes": ["ORDER_SHIPPED_EVENT"],
  "measurements": [ { "measurementCode": "TIME", "type": "P" } ]
}
```

### 4. Handle the triggering event

An event handler receives every event whose `eventCode` matches `eventType`. The built-in
`AbstractEventHandler` resolves the matching process definitions and dispatches to their process handlers, so
most handlers are one line:

```java
@Component
@EventHandler(eventType = "ORDER_PLACED_EVENT")
public class OrderPlacedEventHandler extends AbstractEventHandler {
}
```

Inbound events use the standard Aktimetrix envelope. Your domain object goes in `entity`:

```json
{
  "tenantKey": "AA",
  "eventId": "51541182-81fa-4727-afd5-114acdf086b1",
  "eventType": "ORDER",
  "eventCode": "ORDER_PLACED_EVENT",
  "eventName": "order placed event",
  "eventTime": "2015-11-18T00:00:00.000+0200",
  "eventUTCTime": "2015-11-18 00:00:00",
  "source": "AA",
  "entityId": "1234",
  "entityType": "com.ecom.order",
  "entity": {
    "orderId": "1234",
    "orderedOn": "2022-05-22 23:46:00",
    "customerId": "1",
    "orderTotal": 100,
    "orderCurrency": "USD"
  },
  "eventDetails": {}
}
```

### 5. Create the process handler

The process handler's `processType` must equal the definition's `processCode`. Extend `AbstractProcessor` and
decide which metadata to keep on the process instance and on each step instance:

```java
@Component
@ProcessHandler(processType = "ORDER_DELIVERY")
public class OrderProcessor extends AbstractProcessor {

    private static final DateTimeFormatter IN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    protected Map<String, Object> getProcessMetadata(Context context) {
        // keep the whole order on the process instance
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

### 6. Write the meters

A meter computes one measurement for one step. `@Measurement(code, stepCode)` must match a
`measurementCode` in that step's definition:

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
        LocalDateTime orderedOn = LocalDateTime.parse((String) step.getMetadata().get("orderedOn"));
        return String.valueOf(orderedOn.plusHours(2));
    }
}
```

```java
@Component
@Measurement(code = "TIME", stepCode = "DELIVER")
public class OrderDeliveredPlanTimeMeter extends AbstractMeter {

    @Override
    protected String getMeasurementUnit(String tenant, StepInstance step) {
        return "TIMESTAMP";
    }

    @Override
    protected String getMeasurementValue(String tenant, StepInstance step) {
        // and be delivered within 10 hours
        LocalDateTime orderedOn = LocalDateTime.parse((String) step.getMetadata().get("orderedOn"));
        return String.valueOf(orderedOn.plusHours(10));
    }
}
```

That's the whole monitor: three small classes and some JSON.

---

**Next:** [Extending Aktimetrix](extending.md)
