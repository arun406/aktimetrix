package com.aktimetrix.core.referencedata.transferobjects;

public enum ReferenceDataType {
    PROCESS("process-definitions"), STEP("step-definitions"),
    METRIC("metric-definitions"), EVENT("event-type-definitions"),
    MEASUREMENT_TYPE("measurement-type-definitions"),
    MEASUREMENT_UNIT("measurement-unit-definitions");

    public final String name;

    ReferenceDataType(String name) {
        this.name = name;
    }

    public static ReferenceDataType valueOfName(String name) {
        for (ReferenceDataType e : values()) {
            if (e.name.equals(name)) {
                return e;
            }
        }
        return null;
    }
}
