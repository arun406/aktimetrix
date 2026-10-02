package com.aktimetrix.core.notification;

import com.aktimetrix.core.api.Notification;
import com.aktimetrix.core.api.Notifier;
import com.aktimetrix.core.configurations.AktimetrixProperties;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Posts each notification as JSON to {@code aktimetrix.notifications.webhook.url}, with the configured headers. A
 * response other than 2xx, or no response in time, fails the delivery, which is retried.
 */
public class WebhookNotifier implements Notifier {

    private final AktimetrixProperties.Notifications.Webhook webhook;
    private final ObjectMapper objectMapper;
    private final HttpClient http;

    public WebhookNotifier(AktimetrixProperties.Notifications.Webhook webhook, ObjectMapper objectMapper) {
        this.webhook = webhook;
        this.objectMapper = objectMapper;
        this.http = HttpClient.newBuilder().connectTimeout(webhook.getTimeout()).build();
    }

    @Override
    public void notify(Notification notification) throws IOException, InterruptedException {
        final HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(webhook.getUrl()))
                .timeout(webhook.getTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(notification)));
        webhook.getHeaders().forEach(request::header);
        final HttpResponse<Void> response = http.send(request.build(), HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Webhook answered " + response.statusCode() + " to notification " + notification.getId());
        }
    }
}
