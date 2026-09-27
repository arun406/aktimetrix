package com.aktimetrix.core.event.handler;

import org.springframework.stereotype.Component;

/**
 * Handles every business event that has no {@code @EventHandler} of its own: starts the processes it starts and
 * records it as a milestone on the entity's active processes. See {@link AbstractEventHandler}.
 */
@Component
public class DefaultEventHandler extends AbstractEventHandler {
}
