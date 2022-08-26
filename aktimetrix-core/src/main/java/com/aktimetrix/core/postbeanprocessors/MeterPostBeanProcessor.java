package com.aktimetrix.core.postbeanprocessors;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Registry;
import com.aktimetrix.core.meter.api.Meter;
import com.aktimetrix.core.stereotypes.Measurement;
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
                logger.info("Measurement Code: {}, Step Code: {}, Type: {} ", annotation.code(), annotation.stepCode(), annotation.type());
                Map<String, String> attributes = new HashMap<>();
                HashMap<String, String> map = new HashMap<>();
                map.put(Constants.ATT_METER_SERVICE, Constants.VAL_YES);
                map.put(Constants.ATT_CODE, annotation.code());
                map.put(Constants.ATT_STEP_CODE, annotation.stepCode());
                map.put(Constants.ATT_MEASUREMENT_TYPE, annotation.type());
                attributes.putAll(map);
                this.registry.register(beanName, attributes, bean);
            }
            logger.debug("Called postProcessBeforeInitialization() for : {}", beanName);
        }
        return bean;
    }
}
