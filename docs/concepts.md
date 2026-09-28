# Core concepts

[← Back to README](../README.md)

<p align="center">
  <img src="../img/domain-model.svg" alt="Definitions vs. instances in Aktimetrix" width="100%">
</p>

Aktimetrix separates **what a process looks like** (definitions, maintained as reference data) from **what is
happening to one particular business entity** (instances, created at run time).

| Concept | Kind | Description |
|---|---|---|
| **Process** | definition | A named business process, e.g. `ORDER_DELIVERY`. Lists its steps, the event codes that start it, and the entity type it tracks. |
| **Step** | definition | One milestone in the process, e.g. `SHIP`. Lists the measurements to take at that milestone. |
| **Measurement** | definition | A user-defined dimension to measure (`TIME`, `DISTANCE`, `FUEL`, `TEMPERATURE`, `RATING`, …), at the process or at a step, planned (`P`) and actual (`A`), with an optional tolerance. |
| **Business entity** | external | The real-world object being tracked: an order, a loan application, an air waybill. Identified by `entityType` + `entityId`. |
| **Process instance** | runtime | One run of a process for one business entity. *ProcessInstance = Process + identifying metadata.* |
| **Step instance** | runtime | One step of one process instance, carrying its own metadata. |
| **Measurement instance** | runtime | A concrete value for one process or step instance, e.g. *planned DISTANCE = 5 km*; an actual one also carries its plan, its deviation and whether it is within tolerance, e.g. *actual DISTANCE = 12 km, +7 km, outside tolerance*. |
| **Metadata** | runtime | Key/value pairs you attach to process and step instances (order details, customer, location, …) for use by meters and consumers. |

### Step lifecycle: plan and actual

Every step instance moves through a simple lifecycle, driven by the business events named in its step definition:

| Status | When |
|---|---|
| `Created` | The process instance is created. |
| `Started` | An event in the step's `startEventCodes` arrives, and the step also has `endEventCodes`. |
| `Completed` | An event in the step's `endEventCodes` arrives. A step without end codes is a single milestone and completes on its start event. |

### Planning a step

A step gets its planned time in one of two ways:

| How | Step definition | Planned at |
|---|---|---|
| **Duration from the start** | `"plannedWithin": "PT3H"` | the process start + 3 h |
| **Duration from another step** | `"plannedAfter": "SORT", "plannedWithin": "PT5H"` | when `SORT` completes, its actual time + 5 h |
| **Meter** | `"measurements": [{ "measurementCode": "TIME", "type": "P" }]` and a `@Measurement` meter | the process start, from whatever your meter computes |

`"tolerance": "PT15M"` lets a step run 15 minutes past its planned time before it counts as late. The step's
**deadline** is its planned time plus the tolerance.

### Monitoring fields

Alongside its status, each step instance carries:

| Field | Meaning |
|---|---|
| `plannedAt` | When the step should happen. |
| `lateAfter` | Its deadline: `plannedAt` + tolerance. |
| `expectedAt` | Forecast of when it will happen, once an earlier step has run late. |
| `actualAt` | When it did happen: the time of its event (see [the event format](getting-started.md#the-event-format)). |
| `timeliness` | How it compares with its plan (below). |

| Timeliness | When |
|---|---|
| `AT_RISK` | Not completed, and forecast past its deadline because an earlier step is late or overdue by as much. |
| `OVERDUE` | Not completed, and its deadline has passed. |
| `ON_TIME` | Completed by its deadline. |
| `LATE` | Completed after its deadline. |

A step can pass through `AT_RISK` and `OVERDUE` before it completes; it ends as `ON_TIME` or `LATE`, judged by when its
event says it happened. Every change is published to `step-instance-out-0`, including `PLANNED` for steps planned
from another step's completion.

Each completion is also recorded as an **actual** `TIME` measurement (type `A`), compared with the planned time,
together with the step's other actual measurements, such as the distance travelled, each compared with its own plan
(see [Measurement fields](configuration.md#measurement-fields)). When
every non-optional step (`optionalInd` ≠ `Y`) is complete, the process instance is marked complete. Replayed events
are ignored, so a completed step is never recorded twice.

### Example: one order through the process

When `ORDER_PLACED_EVENT` arrives for order `#1234` (placed at `2022-05-22 23:46`), the
[reference project](https://github.com/arun406/aktimetrix-reference-project-order-monitor) creates one
`ORDER_DELIVERY` process instance with three step instances, and its meters compute the plan. The start event
completes `PLACE`. `SHIP` completes at 01:30, 16 minutes ahead of plan, and `DELIVER` at 10:30, 44 minutes late:

<p align="center">
  <img src="../img/order-timeline.svg" alt="Planned timeline for order #1234" width="100%">
</p>

### Same model, any domain

The concepts do not change from one domain to the next. Only the definitions do.

<table>
  <tr>
    <th width="33%">E-commerce: order delivery</th>
    <th width="33%">Banking: loan account processing</th>
    <th width="33%">Air cargo: shipment transportation</th>
  </tr>
  <tr>
    <td><img src="../img/aktimetrix_ecommerce.png" alt="E-commerce order delivery process"></td>
    <td><img src="../img/aktimetrix_banking.jpeg" alt="Banking loan account process"></td>
    <td><img src="../img/aktimetrix_cargo.jpeg" alt="Air cargo transportation process"></td>
  </tr>
</table>

---

**Next:** [Getting started](getting-started.md)
