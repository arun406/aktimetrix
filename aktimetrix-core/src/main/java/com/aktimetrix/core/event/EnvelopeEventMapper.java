package com.aktimetrix.core.event;

import com.aktimetrix.core.api.EventMapper;
import com.aktimetrix.core.transferobjects.Event;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * The default {@link EventMapper}: reads messages written in the Aktimetrix event envelope.
 */
public class EnvelopeEventMapper implements EventMapper {

    private static final TypeReference<Event<Object, Object>> EVENT = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public EnvelopeEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Event<?, ?> map(String payload, Map<String, Object> headers) throws Exception {
        return objectMapper.readValue(payload, EVENT);
    }
}
