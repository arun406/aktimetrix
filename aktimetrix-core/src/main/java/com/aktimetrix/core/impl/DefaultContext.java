package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.ProcessInstanceState;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.ProcessPlanInstance;
import com.aktimetrix.core.model.StepInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
    private String processType;
    private String processCode;
    private ProcessPlanInstance processPlanInstance;
    private ProcessInstanceState state;

    @Override
    public ProcessInstanceState getCurrentState() {
        return this.state;
    }

    @Override
    public void setCurrentState(ProcessInstanceState state) {
        this.state = state;
    }

    /**
     * default constructor
     */
    public DefaultContext() {
        context = new HashMap<>();
    }

    /**
     * returns the process type
     *
     * @return
     */
    @Override
    public String getProcessType() {
        return this.processType;
    }

    /**
     * returns the process code
     *
     * @return
     */
    @Override
    public String getProcessCode() {
        return this.processCode;
    }

    /**
     * sets the process code
     *
     * @param processCode
     */
    public void setProcessCode(String processCode) {
        this.processCode = processCode;
    }

    /**
     * sets the process type
     *
     * @param processType
     */
    public void setProcessType(String processType) {
        this.processType = processType;
    }

    /**
     * returns the process instance
     *
     * @return
     */
    @Override
    public ProcessInstance getProcessInstance() {
        if (processInstance != null) {
            return processInstance;
        }
        return null;
    }

    /**
     * returns the value from the context map
     *
     * @param propertyName
     * @return
     */
    @Override
    public Object getProperty(String propertyName) {
        return context.get(propertyName);
    }

    /**
     * checks the property existence
     *
     * @param propertyName
     * @return
     */
    public boolean containsProperty(String propertyName) {
        return context.containsKey(propertyName);
    }

    /**
     * add key value pair to context
     *
     * @param propertyName
     * @param value
     */
    @Override
    public void setProperty(String propertyName, Object value) {
        this.context.put(propertyName, value);
    }

    /**
     * sets the process instance
     *
     * @param processInstance
     */
    public void setProcessInstance(ProcessInstance processInstance) {
        this.processInstance = processInstance;
    }

    /**
     * returns the tenant
     *
     * @return
     */
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
        Objects.requireNonNull(stepInstances, "step instances cannot be null");
        return stepInstances;
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
        Objects.requireNonNull(measurementInstances, "measurement instances cannot be null");
        return measurementInstances;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    /**
     * sets the process plan instance into the context
     *
     * @param plan
     */
    @Override
    public void setProcessPlanInstance(ProcessPlanInstance plan) {
        this.processPlanInstance = plan;
    }

    /**
     * returns the process plan instance
     *
     * @return
     */
    @Override
    public ProcessPlanInstance getProcessPlanInstance() {
        return this.processPlanInstance;
    }
}
