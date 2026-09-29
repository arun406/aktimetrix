package com.aktimetrix.core.store;

import com.aktimetrix.core.model.ProcessInstance;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts model objects to and from JSON for stores that keep them as documents, such as the JDBC and in-memory
 * stores.
 * <p>
 * Unlike the application's {@code ObjectMapper}, it ignores the Jackson annotations that shape published events, so
 * every field is kept, at full precision: the revision, the definition a process started with, ids and times. A
 * process instance's steps are stored separately and are not part of its document.
 */
public final class StoreDocuments {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(MapperFeature.USE_ANNOTATIONS, false)
            .registerModule(new JavaTimeModule())
            .registerModule(new SimpleModule().setSerializerModifier(new BeanSerializerModifier() {
                @Override
                public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription bean,
                                                                 List<BeanPropertyWriter> properties) {
                    if (ProcessInstance.class.isAssignableFrom(bean.getBeanClass())) {
                        return properties.stream().filter(p -> !"steps".equals(p.getName())).collect(Collectors.toList());
                    }
                    return properties;
                }
            }))
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private StoreDocuments() {
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot store " + value.getClass().getSimpleName() + " as JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read a stored " + type.getSimpleName(), e);
        }
    }

    /**
     * A deep copy, as a store would return it.
     */
    @SuppressWarnings("unchecked")
    public static <T> T copy(T value) {
        return value == null ? null : (T) fromJson(toJson(value), value.getClass());
    }
}
