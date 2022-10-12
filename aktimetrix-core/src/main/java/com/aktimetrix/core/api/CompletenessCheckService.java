package com.aktimetrix.core.api;

import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.PostProcessor;

public interface CompletenessCheckService extends PostProcessor {

    boolean isComplete(Context context);

    @Override
    default void postProcess(Context context) {
        boolean complete = isComplete(context);
    }
}
