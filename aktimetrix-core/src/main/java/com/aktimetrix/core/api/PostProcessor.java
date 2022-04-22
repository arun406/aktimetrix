package com.aktimetrix.core.api;

/**
 * @author arun kumar kandakatla
 */
public interface PostProcessor extends Processor {

    void postProcess(ProcessContext context);

    @Override
    default void process(ProcessContext context) {
        postProcess(context);
    }
}
