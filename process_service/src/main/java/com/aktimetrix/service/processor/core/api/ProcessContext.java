package com.aktimetrix.service.processor.core.api;

import com.aktimetrix.service.processor.core.model.ProcessInstance;
import com.aktimetrix.service.processor.core.model.StepInstance;

import java.util.List;

/**
 * @author arun kumar kandakatla
 */
public interface ProcessContext {

    /**
     * Returns the process instance that is currently being executed in this context.
     *
     * @return
     */
    ProcessInstance getProcessInstance();

    /**
     * add the process into the process context
     *
     * @return
     */
    void setProcessInstance(ProcessInstance processInstance);

    /**
     * Returns the value of the property with the given name.
     *
     * @param propertyName
     * @return
     */
    Object getProperty(String propertyName);

    /**
     * Sets the value of the property with the given name.
     *
     * @param propertyName
     * @param value
     */
    void setProperty(String propertyName, Object value);

    /**
     * @return
     */
    String getTenant();


    /**
     * sets the step instances
     *
     * @param stepInstances
     */
    void setStepInstances(List<StepInstance> stepInstances);

    /**
     * returns the step instances
     *
     * @return
     */
    List<StepInstance> getStepInstances();
}
