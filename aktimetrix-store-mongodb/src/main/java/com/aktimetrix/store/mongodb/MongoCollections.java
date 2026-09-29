package com.aktimetrix.store.mongodb;

/**
 * The collections Aktimetrix uses.
 */
public final class MongoCollections {
    public static final String PROCESS_INSTANCES = "processInstances";
    public static final String STEP_INSTANCES = "stepInstances";
    public static final String MEASUREMENT_INSTANCES = "measurement-instance";
    public static final String PROCESS_DEFINITIONS = "processDefinitions";
    public static final String STEP_DEFINITIONS = "stepDefinitions";
    public static final String MEASUREMENT_TYPE_DEFINITIONS = "measurementTypeDefinitions";
    public static final String OUTBOX = "outbox";

    private MongoCollections() {
    }
}
