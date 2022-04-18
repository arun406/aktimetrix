package com.aktimetrix.service.meter.core.meter;

import com.aktimetrix.service.meter.core.api.Constants;
import com.aktimetrix.service.meter.core.meter.api.Meter;
import com.aktimetrix.service.meter.core.api.Registry;
import com.aktimetrix.service.meter.core.stereotypes.Measurement;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MeterPostBeanProcessor implements BeanPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MeterPostBeanProcessor.class);

    private final Registry registry;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof Meter) {
            Measurement annotation = AnnotationUtils.getAnnotation(bean.getClass(), Measurement.class);
            if (annotation != null) {
                logger.info("Measurement Code: {}, Step Code: {}", annotation.code(), annotation.stepCode());
                Map<String, String> attributes = new HashMap<>();

                attributes.putAll(Map.of(Constants.ATT_METER_SERVICE, Constants.VAL_YES,
                        Constants.ATT_CODE, annotation.code(),
                        Constants.ATT_STEP_CODE, annotation.stepCode()));
                this.registry.register(beanName, attributes, bean);
            }
            logger.debug("Called postProcessBeforeInitialization() for : {}", beanName);
        }
        return bean;
    }
}
