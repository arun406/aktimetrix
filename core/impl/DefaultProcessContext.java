package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.ProcessContext;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author arun kumar kandakatla
 */
@Component
public class DefaultProcessContext implements ProcessContext {

    private static final Logger logger = LoggerFactory.getLogger(ProcessContext.class);

    private final Map<String, Object> context;
    private ProcessInstance processInstance;
    private List<StepInstance> stepInstances;
    private String tenant;

    /**
     * default constructor
     */
    public DefaultProcessContext() {
        context = new HashMap<>();
    }

    @Override
    public ProcessInstance getProcessInstance() {
        if (processInstance != null) {
            return processInstance;
        }
        return null;
    }

    @Override
    public Object getProperty(String propertyName) {
        return context.get(propertyName);
    }

    @Override
    public void setProperty(String propertyName, Object value) {
        this.context.put(propertyName, value);
    }

    public void setProcessInstance(ProcessInstance processInstance) {
        this.processInstance = processInstance;
    }

    public String getTenant() {
        return tenant;
    }

    /**
     * sets the step instances
     *
     * @param stepInstances
     */
    @Override
    public void setStepInstances(List<StepInstance> stepInstances) {
        this.stepInstances = stepInstances;
    }

    /**
     * returns the step instances
     *
     * @return
     */
    @Override
    public List<StepInstance> getStepInstances() {
        if (stepInstances != null) {
            return stepInstances;
        }
        return new ArrayList<>();
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }
}
