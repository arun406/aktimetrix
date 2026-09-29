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
    private final Alarms alarms = new Alarms();
    private final Outbox outbox = new Outbox();
    private final Storage storage = new Storage();

    @Data
    public static class Events {
        /**
         * Destination (topic, or exchange) of the inbound business events on the message broker.
         */
        private String topic = "business-events";
        /**
         * Consumer group of the inbound business events.
         */
        private String group = "aktimetrix";

        private final DeadLetter deadLetter = new DeadLetter();

        @Data
        public static class DeadLetter {
            /**
             * Whether events that cannot be processed, after retries, are sent to a dead-letter topic instead of
             * being dropped. Invalid events are sent there without retries.
             */
            private boolean enabled = true;
            /**
             * Dead-letter topic; defaults to the inbound topic followed by {@code .dlq}.
             */
            private String topic;
        }
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
         * Whether to sweep periodically for steps and processes past their deadline. Alarms mark them overdue as their
         * deadlines pass; the sweep is a safety net, for instances without an alarm, such as those created by earlier
         * versions.
         */
        private boolean enabled = true;
        /**
         * How often to sweep for overdue steps and processes, as an ISO-8601 duration such as {@code PT10M}.
         */
        private Duration overdueCheckInterval = Duration.ofMinutes(10);
    }

    @Data
    public static class Alarms {
        /**
         * Whether to fire the alarms set at deadlines: a step or process with a planned time gets an alarm at its
         * deadline, which marks it overdue if its event has not arrived by then.
         */
        private boolean enabled = true;
        /**
         * How often to look for alarms that are due, as an ISO-8601 duration: at most how late an alarm fires.
         */
        private Duration checkInterval = Duration.ofSeconds(5);
        /**
         * Most alarms claimed at once; claiming continues until none are due.
         */
        private int batchSize = 100;
        /**
         * How long an instance holds a claimed alarm before another may fire it, if the first did not finish.
         */
        private Duration lease = Duration.ofSeconds(30);
    }

    @Data
    public static class Outbox {
        /**
         * How often the relay publishes pending events to the broker, as an ISO-8601 duration such as {@code PT1S}.
         */
        private Duration relayInterval = Duration.ofSeconds(1);
        /**
         * Most events published per relay run.
         */
        private int batchSize = 100;
        /**
         * How long a relay holds a claimed event before another may retry it.
         */
        private Duration lease = Duration.ofSeconds(30);
        /**
         * How long sent events are kept before being purged.
         */
        private Duration retention = Duration.ofDays(7);
    }

    @Data
    public static class Storage {
        /**
         * Which store module to use when more than one is on the classpath: {@code mongodb}, {@code jdbc} or
         * {@code memory}. Not needed with a single store module.
         */
        private String type;
        /**
         * Whether each business event, and each overdue step or process, is processed in a transaction, so that state
         * and outbound events are written together: {@code auto} uses transactions when the store supports them (with
         * MongoDB, replica sets and sharded clusters; always with JDBC), {@code always} requires them, {@code never}
         * disables them.
         */
        private TransactionMode transactions = TransactionMode.AUTO;
        /**
         * Whether to create the indexes, and with JDBC the tables, Aktimetrix relies on at startup.
         */
        private boolean createIndexes = true;

        public enum TransactionMode {AUTO, ALWAYS, NEVER}
    }
}
