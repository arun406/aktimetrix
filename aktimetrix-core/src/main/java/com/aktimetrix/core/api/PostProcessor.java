package com.aktimetrix.core.api;

/**
 * @author arun kumar kandakatla
 */
public interface PostProcessor extends Processor {

    void postProcess(Context context);

    @Override
    default void process(Context context) {
        postProcess(context);
    }
}
