package com.aktimetrix.core.api;

/**
 * Process Instance state interface.
 *
 * @author arun.kandakatla
 */
public interface ProcessInstanceState {
    /**
     * Change the current state of the process instance
     *
     * @param context
     */
    void updateState(Context context);

}
