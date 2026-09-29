package com.aktimetrix.core.api;

/**
 * The catalogue of events Aktimetrix publishes: their types, their codes, and their schema version. Consumers can
 * rely on these values; a code is only added, never renamed, within a schema version.
 */
public final class PublishedEvents {

    /**
     * Version of the published event structure, in every event's {@code eventDetails.schemaVersion}.
     */
    public static final String SCHEMA_VERSION = "1";
    /**
     * The {@code source} of every published event.
     */
    public static final String SOURCE = "aktimetrix";

    private PublishedEvents() {
    }

    /**
     * {@code Process_Event}: something happened to a process instance.
     */
    public static final class Process {
        public static final String TYPE = "Process_Event";
        public static final String ENTITY_TYPE = "com.aktimetrix.process.instance";
        /** The process instance was created, with its steps, and planned. */
        public static final String CREATED = "CREATED";
        /** The process completed: implicitly with its last mandatory step, or on an end event. */
        public static final String COMPLETED = "COMPLETED";
        /** A cancel event cancelled the process and its open steps. */
        public static final String CANCELLED = "CANCELLED";
        /** The process's own deadline passed before it completed. */
        public static final String OVERDUE = "OVERDUE";

        private Process() {
        }
    }

    /**
     * {@code Step_Event}: something happened to a step instance.
     */
    public static final class Step {
        public static final String TYPE = "Step_Event";
        public static final String ENTITY_TYPE = "com.aktimetrix.step.instance";
        /** The step was created with its process. */
        public static final String CREATED = "CREATED";
        /** The step got its planned time, when the step it is planned after completed. */
        public static final String PLANNED = "PLANNED";
        /** A step with end events was started by one of its start events. */
        public static final String STARTED = "STARTED";
        /** The step completed, and was judged {@code ON_TIME} or {@code LATE}. */
        public static final String COMPLETED = "COMPLETED";
        /** An earlier delay pushed the step's forecast past its deadline; published again when the forecast moves later. */
        public static final String AT_RISK = "AT_RISK";
        /** The step's deadline passed before it completed. */
        public static final String OVERDUE = "OVERDUE";
        /** The process ended on an explicit end event while this mandatory step was open. */
        public static final String SKIPPED = "SKIPPED";
        /** The process was cancelled while this step was open. */
        public static final String CANCELLED = "CANCELLED";

        private Step() {
        }
    }

    /**
     * {@code Measurement_Event}: a measurement was recorded.
     */
    public static final class Measurement {
        public static final String TYPE = "Measurement_Event";
        public static final String ENTITY_TYPE = "com.aktimetrix.measurement.instance";
        /** A planned value, of the process or a step. */
        public static final String PLANNED = "PLANNED";
        /** A final actual value, compared with its plan. */
        public static final String RECORDED = "RECORDED";
        /** An interim reading of a step in progress, compared with its plan. */
        public static final String READING = "READING";
        /** A metric of the process, computed from its measurements when it completed. */
        public static final String METRIC = "METRIC";

        private Measurement() {
        }
    }
}
