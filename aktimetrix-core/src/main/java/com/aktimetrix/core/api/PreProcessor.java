package com.aktimetrix.core.api;

/**
 * @author arun kumar kandakatla
 */
public interface PreProcessor extends Processor {

    void preProcess(Context context);

    @Override
    default void process(Context context) {
        preProcess(context);
    }
}
