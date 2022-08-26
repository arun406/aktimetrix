package com.aktimetrix.core.api;

/**
 * @author arun.kandakatla
 */
public interface StateChangePublisher {
    /**
     * Publish the State change to the observers
     *
     * @param context
     */
    void publishState(Context context);
}
