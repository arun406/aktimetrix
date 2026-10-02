package com.aktimetrix.core.api;

/**
 * Acts on what Aktimetrix finds: a step or process going at risk, overdue, or completing late. Implement it to alert
 * people or tools, such as a chat channel, a paging service or a ticket system; every {@code Notifier} bean receives
 * every {@link Notification}. Aktimetrix itself never calls back into the systems it watches.
 * <p>
 * Notifications are queued in the outbox with the change that caused them and delivered after it is saved, at least
 * once: a notifier that throws is called again later, and one that keeps failing is given up after
 * {@code aktimetrix.notifications.max-attempts}. Use {@link Notification#getId()} to recognise a repeat.
 */
public interface Notifier {

    /**
     * @throws Exception when the notification could not be delivered; it is retried
     */
    void notify(Notification notification) throws Exception;
}
