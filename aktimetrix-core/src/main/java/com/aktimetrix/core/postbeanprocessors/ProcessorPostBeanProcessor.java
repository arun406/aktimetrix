package com.aktimetrix.core.postbeanprocessors;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.api.Registry;
import com.aktimetrix.core.stereotypes.ProcessHandler;
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
public class ProcessorPostBeanProcessor implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ProcessorPostBeanProcessor.class);

    private final Registry registry;

    @Autowired
    public ProcessorPostBeanProcessor(Registry registry) {
        this.registry = registry;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        if (bean instanceof Processor) {
            logger.trace("Called postProcessBeforeInitialization() for : {}", beanName);

            ProcessHandler annotation =
                    AnnotationUtils.findAnnotation(bean.getClass(), ProcessHandler.class);
            if (annotation == null) {
                return bean;
            }
            logger.info("Process Type: {}, Process Code: {},  Name : {}, Version {}",
                    annotation.processType(), annotation.processCode(), annotation.name(), annotation.version());
            Map<String, String> attributes = new HashMap<>();
            attributes.put(Constants.ATT_PROCESS_HANDLER_SERVICE, Constants.VAL_YES);
            attributes.put(Constants.ATT_PROCESS_TYPE, annotation.processType());
            attributes.put(Constants.ATT_PROCESS_CODE, annotation.processCode());
            attributes.put(Constants.ATT_PROCESS_HANDLER_NAME, annotation.name());
            attributes.put(Constants.ATT_PROCESS_HANDLER_VERSION, annotation.version());
            logger.debug("registering the {} bean with attributes {}", beanName, attributes);
            this.registry.register(beanName, attributes, bean);
        }
        return bean;
    }
}
