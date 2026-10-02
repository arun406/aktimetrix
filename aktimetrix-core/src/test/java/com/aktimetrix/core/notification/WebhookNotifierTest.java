package com.aktimetrix.core.notification;

import com.aktimetrix.core.api.Notification;
import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookNotifierTest {

    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private HttpServer server;

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void postsTheNotificationAsJsonWithTheConfiguredHeaders() throws Exception {
        final AktimetrixProperties.Notifications.Webhook webhook = start(204);
        webhook.getHeaders().put("Authorization", "Bearer token");

        new WebhookNotifier(webhook, JsonMapper.builder().build()).notify(Notification.builder().id("e1")
                .condition("OVERDUE").entityId("1234").build());

        assertThat(body.get()).contains("\"id\":\"e1\"").contains("\"condition\":\"OVERDUE\"").contains("\"entityId\":\"1234\"");
        assertThat(authorization.get()).isEqualTo("Bearer token");
    }

    @Test
    void anAnswerOtherThan2xxFailsTheDelivery() throws Exception {
        final AktimetrixProperties.Notifications.Webhook webhook = start(503);

        assertThatThrownBy(() -> new WebhookNotifier(webhook, JsonMapper.builder().build())
                .notify(Notification.builder().id("e1").build()))
                .isInstanceOf(IOException.class).hasMessageContaining("503");
    }

    private AktimetrixProperties.Notifications.Webhook start(int status) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        final AktimetrixProperties.Notifications.Webhook webhook = new AktimetrixProperties().getNotifications().getWebhook();
        webhook.setUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/hook");
        return webhook;
    }
}
