package com.aktimetrix.service.meter.core.impl;

import com.aktimetrix.service.meter.core.api.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * @author arun kumar kandakatla
 */
public class RegistryEntry {

    private Map attributes;
    private Object instance;
    private Class<?> type;

    /**
     * Constructor
     *
     * @param inst
     * @param attributes
     */
    public RegistryEntry(Object inst, Map attributes) {
        if (inst instanceof Class) {
            type = (Class) inst;
        } else {
            this.instance = inst;
        }
        this.attributes = attributes;
        if (this.attributes == null) this.attributes = new HashMap<>();
    }

    private boolean hasType() {
        return type != null;
    }

    public Map getAttributes() {
        return attributes;
    }

    private boolean isSingleton() {
        boolean retVal = false;
        if (attributes.containsKey(Constants.ATT_SCOPE)) {
            if (attribute(Constants.ATT_SCOPE).equals(Constants.VAL_SCOPE_SINGLETON)) {
                retVal = true;
            }
        }
        return retVal;
    }

    public Object getInstance() throws IllegalAccessException, InstantiationException {
        Object retVal = instance;
        if (isSingleton()) {
            if (hasType()) {
                if (null == instance) {
                    instance = type.newInstance();
                    retVal = instance;
                }
            }
        } else {
            if (hasType()) {
                retVal = type.newInstance();
            }
        }
        return retVal;

    }

    public boolean hasAttribute(String att) {
        return attributes.containsKey(att);
    }

    public Object attribute(String att) {
        return attributes.get(att);
    }

    @Override
    public String toString() {
        return "RegistryEntry{" +
                "type=" + type +
                ", instance=" + instance +
                ", attributes=" + attributes +
                '}';
    }
}
