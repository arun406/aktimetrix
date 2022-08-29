package com.aktimetrix.core.api;

/**
 * Event Publisher interface
 */
public interface Publisher {

    /**
     * Publish event based on the context information
     *
     * @param context
     */
    public void publish(Context context);
}
