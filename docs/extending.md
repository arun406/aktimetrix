# Extending Aktimetrix

[← Back to README](../README.md)

## Annotations

Every extension is a Spring bean with a stereotype annotation. Aktimetrix discovers it at startup and registers it
in its internal registry.

| Annotation | Implement / extend | Selected by | Purpose |
|---|---|---|---|
| `@EventHandler(eventType)` | `AbstractEventHandler` or `api.EventHandler` | event's `eventCode` | Entry point for a business event. |
| `@ProcessHandler(processType)` | `AbstractProcessor` or `api.Processor` | definition's `processCode` | Creates process and step instances. |
| `@Measurement(code, stepCode)` | `AbstractMeter` or `meter.api.Meter` | step code + measurement code | Computes a planned measurement value. |
| `@PreProcessor(code, processType, priority)` | `api.PreProcessor` | process type | Runs before instances are created: validate, enrich, filter. |
| `@PostProcessor(code, processType, priority)` | `api.PostProcessor` | process type | Runs after instances are created: publish, notify, integrate. |
| `@Loggable` | any bean exposing an interface | n/a | Logs entry, exit, and execution time of its methods. |

### Example: enrich the context before processing

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

> Pre-processors are matched on the definition's `processType` field, so add `"processType": "ORDER_DELIVERY"`
> to the process definition when you use them.

### Example: react to newly computed measurements

Measurement post-processors run in the meter pipeline under the `METERPROCESSOR` process type, next to the
built-in `MI_PUBLISHER`:

```java
@Slf4j
@Component
@PostProcessor(code = "LATE_DELIVERY_ALERT", processType = "METERPROCESSOR")
public class LateDeliveryAlert implements com.aktimetrix.core.api.PostProcessor {

    private static final LocalTime CUT_OFF = LocalTime.of(20, 0);

    @Override
    public void postProcess(Context context) {
        context.getMeasurementInstances().stream()
                .filter(m -> "DELIVER".equals(m.getStepCode()) && "TIME".equals(m.getCode()))
                .filter(m -> LocalDateTime.parse(m.getValue()).toLocalTime().isAfter(CUT_OFF))
                .forEach(m -> log.warn("Delivery planned after cut-off: {}", m));
    }
}
```

### Built-in components

| Component | Type | Role |
|---|---|---|
| `ProcessInstancePublisherService` (`PI_PUBLISHER`) | post-processor | Publishes process instances to `process-instance-out-0`. |
| `StepInstancePublisherService` (`SI_PUBLISHER`) | post-processor | Publishes step instances to `step-instance-out-0`. |
| `StepEventHandler` (`STEP_EVENT`) | event handler | Feeds step-instance events into the meter pipeline. |
| `DefaultMeasurementProcessor` (`METERPROCESSOR`) | process handler | Runs the meters for each planned measurement of a step. |
| `MeasurementInstancePublisherService` (`MI_PUBLISHER`) | post-processor | Publishes measurement instances to `measurement-instance-out-0`. |

## Modelling your own process

Monitoring a new domain mostly means writing new reference data. A loan-origination process might look like this:

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

Then add:

1. an `@EventHandler(eventType = "LOAN_APPLICATION_SUBMITTED")`,
2. an `@ProcessHandler(processType = "LOAN_ORIGINATION")` that copies the applicant and product details into
   metadata,
3. one `@Measurement` meter per planned value, for example `@Measurement(code = "TIME", stepCode = "APPROVAL")`
   returning *submitted + 48 business hours*.

---

**Next:** [Configuration & API reference](configuration.md)
