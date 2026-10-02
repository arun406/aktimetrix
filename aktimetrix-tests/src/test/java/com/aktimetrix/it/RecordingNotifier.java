package com.aktimetrix.it;

import com.aktimetrix.core.api.Notification;
import com.aktimetrix.core.api.Notifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps the notifications it receives, for the scenarios to check.
 */
@Component
public class RecordingNotifier implements Notifier {

    private final List<Notification> received = new CopyOnWriteArrayList<>();

    @Override
    public void notify(Notification notification) {
        received.add(notification);
    }

    public List<Notification> received() {
        return received;
    }
}
