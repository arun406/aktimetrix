package com.aktimetrix.core.postbeanprocessors;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.EventHandler;
import com.aktimetrix.core.api.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author arun kumar kandakatla
 */
@Component
public class EventHandlerPostBeanProcessor implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(EventHandlerPostBeanProcessor.class);

    private final Registry registry;

    @Autowired
    public EventHandlerPostBeanProcessor(Registry registry) {
        this.registry = registry;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        if (bean instanceof EventHandler) {
            logger.trace("Called postProcessBeforeInitialization() for : {}", beanName);

            com.aktimetrix.core.stereotypes.EventHandler annotation =
                    AnnotationUtils.findAnnotation(bean.getClass(), com.aktimetrix.core.stereotypes.EventHandler.class);
            if (annotation != null) {
                logger.info("Event Type: {}, Event Code: {}, Name : {}, Version {}", annotation.eventType(),
                        annotation.eventCode(), annotation.name(), annotation.version());
                Map<String, String> attributes = new HashMap<>();
                attributes.put(Constants.ATT_EVENT_HANDLER_SERVICE, Constants.VAL_YES);
                attributes.put(Constants.ATT_EVENT_TYPE, annotation.eventType());
                attributes.put(Constants.ATT_EVENT_CODE, annotation.eventCode());
                attributes.put(Constants.ATT_EVENT_HANDLER_NAME, annotation.name());
                attributes.put(Constants.ATT_EVENT_HANDLER_VERSION, annotation.version());
                logger.debug("registering the {} bean with attributes {}", beanName, attributes);
                this.registry.register(beanName, attributes, bean);
            }
        }
        return bean;
    }
}
