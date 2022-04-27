package com.aktimetrix.core.api;

/**
 * @author arun kumar kandakatla
 */
public interface PreProcessor extends Processor {

    void preProcess(ProcessContext context);

    @Override
    default void process(ProcessContext context) {
        preProcess(context);
    }
}
