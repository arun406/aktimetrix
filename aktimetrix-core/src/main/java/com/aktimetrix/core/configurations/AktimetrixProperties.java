package com.aktimetrix.core.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.ZoneId;

/**
 * Settings of an Aktimetrix application, under the {@code aktimetrix} prefix.
 */
@Data
@ConfigurationProperties(prefix = "aktimetrix")
public class AktimetrixProperties {

    /**
     * Time zone of all planned and actual times. Event times are converted to it, and the overdue monitor compares
     * planned times with the current time in it.
     */
    private ZoneId timeZone = ZoneId.of("UTC");

    private final Events events = new Events();
    private final Definitions definitions = new Definitions();
    private final Monitor monitor = new Monitor();

    @Data
    public static class Events {
        /**
         * Kafka topic of the inbound business events.
         */
        private String topic = "business-events";
        /**
         * Consumer group of the inbound business events.
         */
        private String group = "aktimetrix";
    }

    @Data
    public static class Definitions {
        /**
         * Whether to load process and step definitions from the classpath at startup. Definitions are upserted by
         * tenant and code, so the files can be edited and reloaded by restarting the application.
         */
        private boolean loadOnStartup = true;
        /**
         * Location of the process definitions: a JSON array.
         */
        private String processes = "classpath*:aktimetrix/process-definitions.json";
        /**
         * Location of the step definitions: a JSON array.
         */
        private String steps = "classpath*:aktimetrix/step-definitions.json";
    }

    @Data
    public static class Monitor {
        /**
         * Whether to check periodically for steps whose planned time has passed without completing.
         */
        private boolean enabled = true;
        /**
         * How often to check for overdue steps, as an ISO-8601 duration such as {@code PT1M}.
         */
        private Duration overdueCheckInterval = Duration.ofMinutes(1);
    }
}
