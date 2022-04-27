package com.aktimetrix.service.processor.core.api;

/**
 * @author Arun Kumar Kandakatla
 */
public interface Processor {
    /**
     * @param context process context
     */
    void process(ProcessContext context);
}
