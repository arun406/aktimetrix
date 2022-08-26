package com.aktimetrix.core.configurations;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.exception.EventHandlerNotFoundException;
import com.aktimetrix.core.exception.MultipleEventHandlersFoundException;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.StepInstanceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeterConfig {
    final private static Logger log = LoggerFactory.getLogger(MeterConfig.class);

    @Autowired
    private RegistryService registryService;

    /**
     * Consumes the StepEvent and prepares and produce the StepMeasurementEvent
     *
     * @return
     */
    @Bean
    @ConditionalOnProperty(value = "aktimetrix.consumer.step.enabled", havingValue = "true", matchIfMissing = false)
    public java.util.function.Consumer<Event<StepInstanceDTO, Void>> measure() {
        return event -> {
            log.debug("event: {}", event);
            try {
                this.registryService.getEventHandler(Constants.STEP_EVENT, event.getEventCode()).handle(event);
            } catch (EventHandlerNotFoundException | MultipleEventHandlersFoundException e) {
                log.error("Something happened bad. please contact system administrator.", e);
            }
        };
    }
}
