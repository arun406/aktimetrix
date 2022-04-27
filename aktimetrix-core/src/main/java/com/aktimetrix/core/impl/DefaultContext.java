package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author arun kumar kandakatla
 */
@Component
public class DefaultContext implements Context {

    private static final Logger logger = LoggerFactory.getLogger(Context.class);

    private final Map<String, Object> context;
    private ProcessInstance processInstance;
    private List<StepInstance> stepInstances;
    private List<MeasurementInstance> measurementInstances;
    private String tenant;

    /**
     * default constructor
     */
    public DefaultContext() {
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
        return Objects.requireNonNullElseGet(stepInstances, ArrayList::new);
    }

    /**
     * sets the measurement instances
     *
     * @param measurementInstances
     */
    @Override
    public void setMeasurementInstances(List<MeasurementInstance> measurementInstances) {
        this.measurementInstances = measurementInstances;
    }

    /**
     * returns the step instances
     *
     * @return
     */
    @Override
    public List<MeasurementInstance> getMeasurementInstances() {
        return Objects.requireNonNullElseGet(measurementInstances, ArrayList::new);
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }
}
