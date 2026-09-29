package com.aktimetrix.it.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The JSON Schemas of the published events, shipped in {@code aktimetrix-core}.
 */
public final class EventSchemas {

    private static final ObjectMapper JSON = new ObjectMapper();

    private EventSchemas() {
    }

    /**
     * Asserts that the message is a valid event of its type, and returns it.
     */
    public static JsonNode assertValid(String message) {
        try {
            final JsonNode event = JSON.readTree(message);
            final String schema;
            switch (event.path("eventType").asText()) {
                case "Process_Event":
                    schema = "process-event";
                    break;
                case "Step_Event":
                    schema = "step-event";
                    break;
                case "Measurement_Event":
                    schema = "measurement-event";
                    break;
                default:
                    throw new AssertionError("Unknown event type: " + message);
            }
            final Set<ValidationMessage> errors = load(schema).validate(event);
            assertThat(errors).as("%s is valid against %s.schema.json", message, schema).isEmpty();
            return event;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JsonSchema load(String name) throws IOException {
        try (InputStream in = EventSchemas.class.getResourceAsStream("/META-INF/aktimetrix/schemas/" + name + ".schema.json")) {
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(in);
        }
    }
}
