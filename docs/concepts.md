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
| **Measurement** | definition | *What* to measure at a step (`TIME`, `PCS`, `WT`, …) and whether it is **P**lanned or **A**ctual. |
| **Business entity** | external | The real-world object being tracked: an order, a loan application, an air waybill. Identified by `entityType` + `entityId`. |
| **Process instance** | runtime | One run of a process for one business entity. *ProcessInstance = Process + identifying metadata.* |
| **Step instance** | runtime | One step of one process instance, carrying its own metadata. |
| **Measurement instance** | runtime | A concrete value computed for one step instance, e.g. *SHIP planned TIME = 2022-05-23T01:46*. |
| **Metadata** | runtime | Key/value pairs you attach to process and step instances (order details, customer, location, …) for use by meters and consumers. |

### Example: one order through the process

When `ORDER_PLACED_EVENT` arrives for order `#1234` (placed at `2022-05-22 23:46`), the
[reference project](https://github.com/arun406/aktimetrix-reference-project-order-monitor) creates one
`ORDER_DELIVERY` process instance with three step instances, and its meters compute the plan:

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
