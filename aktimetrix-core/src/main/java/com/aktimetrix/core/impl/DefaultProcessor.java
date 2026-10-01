package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Context;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Process handler for processes without a {@code @ProcessHandler} of their own: stores the event's entity, as
 * key/value pairs, as the metadata of the process instance and of each step instance.
 */
@Component
@RequiredArgsConstructor
public class DefaultProcessor extends AbstractProcessor {

    private final ObjectMapper objectMapper;

    @Override
    protected Map<String, Object> getStepMetadata(Context context) {
        return entity(context);
    }

    @Override
    protected Map<String, Object> getProcessMetadata(Context context) {
        return entity(context);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> entity(Context context) {
        final Object entity = context.getProperty(Constants.ENTITY);
        if (entity == null) {
            return new HashMap<>();
        }
        return new HashMap<>(objectMapper.convertValue(entity, Map.class));
    }
}
