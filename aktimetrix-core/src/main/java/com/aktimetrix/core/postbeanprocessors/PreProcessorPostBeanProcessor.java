package com.aktimetrix.core.postbeanprocessors;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Registry;
import com.aktimetrix.core.stereotypes.PreProcessor;
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
 *
 */
@Component
public class PreProcessorPostBeanProcessor implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PreProcessorPostBeanProcessor.class);

    private final Registry registry;


    @Autowired
    public PreProcessorPostBeanProcessor(Registry registry) {
        this.registry = registry;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        logger.trace("Called postProcessBeforeInitialization() for : {}", beanName);
        PreProcessor annotation = AnnotationUtils.findAnnotation(bean.getClass(), PreProcessor.class);
        if (annotation == null) {
            return bean;
        }
        logger.info("Event Type: {}, Name : {}, Version {}", annotation.processType(), annotation.name(), annotation.version());
        Map<String, String> attributes = new HashMap<>();
        attributes.put(Constants.ATT_PRE_PROCESSOR_SERVICE, Constants.VAL_YES);
        attributes.put(Constants.ATT_PRE_PROCESSOR_CODE, annotation.code());
        attributes.put(Constants.ATT_PRE_PROCESSOR_PROCESS_TYPE, annotation.processType());
        attributes.put(Constants.ATT_PRE_PROCESSOR_NAME, annotation.name());
        attributes.put(Constants.ATT_PRE_PROCESSOR_PRIORITY, String.valueOf(annotation.priority()));
        attributes.put(Constants.ATT_PRE_PROCESSOR_VERSION, annotation.version());
        attributes.put(Constants.ATT_PRE_PROCESSOR_DEFAULT, annotation.isDefault());

        logger.debug("registering the {} bean with attributes {}", beanName, attributes);
        this.registry.register(beanName, attributes, bean);
        return bean;
    }
}
