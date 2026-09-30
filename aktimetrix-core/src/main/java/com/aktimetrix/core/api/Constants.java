package com.aktimetrix.core.api;

/**
 * Names and values shared by the framework. For applications, the relevant ones are the context properties (at the
 * end), the step and process statuses ({@code STATUS_*}) and {@link #MEASUREMENT_CODE_TIME}; the {@code ATT_*} and
 * {@code VAL_*} constants are registry internals.
 */
public class Constants {
    public static final String FUNCTION_MAP = "function.map";
    public static final String ATTRIBUTE_MAP = "attribute.map";

    public static final String ATT_NAME = "name";
    public static final String ATT_CODE = "code";
    public static final String ATT_STEP_CODE = "step-code";
    public static final String ATT_PROCESS_CODE = "process-code";
    public static final String ATT_VERSION = "version";
    public static final String ATT_CLASS = "class";
    public static final String ATT_SCOPE = "scope";
    public static final String ATT_TYPE = "type";
    public static final String ATT_DOC = "doc";

    public static final String VAL_VERSION_DEFAULT = "1.0.0";
    public static final String VAL_SCOPE_SINGLETON = "singleton";
    public static final String VAL_SCOPE_REQUEST = "request";
    public static final String VAL_SCOPE_DEFAULT = VAL_SCOPE_REQUEST;
    public static final String VAL_YES = "yes";
    public static final String VAL_NO = "no";
    public static final String DEFAULT_VERSION = "1.0.0";
    public static final String ATT_METER_SERVICE = "meter-service";
    /**
     * The tenant a planning rule of the DSL is limited to.
     */
    public static final String ATT_RULE_TENANT = "rule-tenant";
    /**
     * The process a step's planning rule of the DSL is limited to.
     */
    public static final String ATT_RULE_PROCESS = "rule-process";
    public static final String PLAN_MEASUREMENT_TYPE = "P";
    public static final String ACTUAL_MEASUREMENT_TYPE = "A";
    public static final String CREATED = "C";
    public static final String STATUS_CREATED = "Created";
    public static final String STATUS_STARTED = "Started";
    public static final String STATUS_CANCELLED = "Cancelled";
    /**
     * A mandatory step still open when its process was ended by an explicit end event: no longer awaited.
     */
    public static final String STATUS_SKIPPED = "Skipped";
    /**
     * Process type of pre- and post-processors that apply to every process.
     */
    public static final String ALL_PROCESS_TYPES = "*";
    /**
     * Priority of the built-in publishers: they run after post-processors with the default priority.
     */
    public static final int BUILT_IN_PRIORITY = 1000;
    /**
     * Process type of the built-in processor that runs the meters of a step.
     */
    public static final String METER_PROCESSOR = "METERPROCESSOR";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String MEASUREMENT_CODE_TIME = "TIME";
    public static final String MEASUREMENT_UNIT_TIMESTAMP = "TIMESTAMP";

    public static final String ATT_EVENT_HANDLER_SERVICE = "event-handler-service";
    public static final String ATT_EVENT_HANDLER_NAME = "event-handler-name";
    public static final String ATT_EVENT_TYPE = "event-type";
    public static final String ATT_EVENT_HANDLER_VERSION = "event-handler-type";

    public static final String ATT_PROCESS_TYPE = "process-type";
    public static final String ATT_PROCESS_HANDLER_SERVICE = "process-handler-service";
    public static final String ATT_PROCESS_HANDLER_NAME = "process-handler-name";
    public static final String ATT_PROCESS_HANDLER_VERSION = "process-handler-type";

    public static final String ATT_PRE_PROCESSOR_SERVICE = "pre-process-service";
    public static final String ATT_PRE_PROCESSOR_CODE = "code";
    public static final String ATT_PRE_PROCESSOR_PRIORITY = "priority";
    public static final String ATT_PRE_PROCESSOR_PROCESS_TYPE = "process-type";
    public static final String ATT_PRE_PROCESSOR_NAME = "pre-processor-name";
    public static final String ATT_PRE_PROCESSOR_VERSION = "pre-processor-version";

    public static final String ATT_POST_PROCESSOR_SERVICE = "post-process-service";
    public static final String ATT_POST_PROCESSOR_CODE = "code";
    public static final String ATT_POST_PROCESSOR_PRIORITY = "priority";
    public static final String ATT_POST_PROCESSOR_PROCESS_TYPE = "process-type";
    public static final String ATT_POST_PROCESSOR_NAME = "post-processor-name";
    public static final String ATT_POST_PROCESSOR_VERSION = "post-processor-version";

    // ---- Context properties, set for process handlers, meters and pre- and post-processors ----

    /**
     * Context property: the {@code ProcessDefinition} being instantiated, with its steps resolved.
     */
    public final static String PROCESS_DEFINITION = "processDefinition";
    /**
     * Context property: the business entity's id, a {@code String}.
     */
    public final static String ENTITY_ID = "entityId";
    /**
     * Context property: the business entity's type, a {@code String}.
     */
    public final static String ENTITY_TYPE = "entityType";
    /**
     * Context property: the event being processed, an {@code Event}.
     */
    public final static String EVENT = "event";
    /**
     * Context property: the event's {@code eventDetails}.
     */
    public final static String EVENT_DATA = "eventData";
    /**
     * Context property: the event's {@code entity}, the domain object it carries.
     */
    public final static String ENTITY = "entity";
    /**
     * Context property: when the event being processed happened in the business, a {@code LocalDateTime}.
     */
    public final static String OCCURRED_AT = "occurredAt";
}
