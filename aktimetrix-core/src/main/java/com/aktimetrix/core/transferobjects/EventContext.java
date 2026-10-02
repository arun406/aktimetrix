package com.aktimetrix.core.transferobjects;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Where a published event belongs, and why it happened: the {@code eventDetails} of every event Aktimetrix publishes.
 * With it, a consumer can relate a step or measurement event to its business entity and process without looking
 * them up, order events by instance {@code revision}, and trace each back to the business event that caused it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventContext {

    /**
     * Version of the event structure; see {@link com.aktimetrix.core.api.PublishedEvents#SCHEMA_VERSION}.
     */
    private String schemaVersion;
    /**
     * The business entity the process follows, e.g. {@code com.ecom.order} {@code 1234}.
     */
    private BusinessEntity businessEntity;
    private String processCode;
    private String processInstanceId;
    /**
     * Revision of the process definition the instance follows.
     */
    private Long definitionRevision;
    /**
     * The step, for step events and step measurements.
     */
    private String stepCode;
    private String stepInstanceId;
    /**
     * Revision of the process or step instance after this change: of two events about the same instance, the one with
     * the higher revision is the more recent.
     */
    private Long revision;
    /**
     * When the change happened in the business, a UTC instant: the time of the business event that caused it, or of
     * the deadline check.
     */
    private Instant occurredAt;
    /**
     * What caused the change.
     */
    private Cause cause;

    /**
     * Builds an {@link EventContext}. Declared so that its name exists in the source, for Javadoc; Lombok generates
     * its methods.
     */
    public static class EventContextBuilder {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessEntity {
        private String entityType;
        private String entityId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Cause {
        /**
         * A business event.
         */
        public static final String EVENT = "EVENT";
        /**
         * A deadline check: the overdue monitors.
         */
        public static final String DEADLINE = "DEADLINE";
        /**
         * A migration of running instances to a newer revision of their definition.
         */
        public static final String MIGRATION = "MIGRATION";

        /**
         * {@value #EVENT}, {@value #DEADLINE} or {@value #MIGRATION}.
         */
        private String type;
        /**
         * For a business event: its {@code eventId} and {@code eventCode}.
         */
        private String eventId;
        private String eventCode;
    }
}
