package com.aktimetrix.core.store;

import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.util.Times;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;

/**
 * Converts model objects to and from JSON for stores that keep them as documents, such as the JDBC and in-memory
 * stores.
 * <p>
 * Unlike the application's {@code ObjectMapper}, it ignores the Jackson annotations that shape published events, so
 * every field is kept, at full precision: the revision, the definition a process started with, ids and times. A
 * process instance's steps are stored separately and are not part of its document.
 */
public final class StoreDocuments {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(MapperFeature.USE_ANNOTATIONS)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .addModule(new SimpleModule().addDeserializer(Instant.class, new LenientInstant()))
            .build();

    private StoreDocuments() {
    }

    public static String toJson(Object value) {
        try {
            if (value instanceof ProcessInstance) {
                final ObjectNode document = MAPPER.valueToTree(value);
                document.remove("steps");
                return MAPPER.writeValueAsString(document);
            }
            return MAPPER.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Cannot store " + value.getClass().getSimpleName() + " as JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JacksonException e) {
            throw new IllegalStateException("Cannot read a stored " + type.getSimpleName(), e);
        }
    }

    /**
     * Reads an instant, and a date-time without an offset, as stored by earlier versions, as UTC.
     */
    private static final class LenientInstant extends ValueDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonParser parser, DeserializationContext context) {
            if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
                return Instant.ofEpochMilli(parser.getLongValue());
            }
            return Times.parse(parser.getString());
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
