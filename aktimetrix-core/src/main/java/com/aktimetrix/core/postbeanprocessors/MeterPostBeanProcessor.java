package com.aktimetrix.core.postbeanprocessors;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Registry;
import com.aktimetrix.core.meter.api.Meter;
import com.aktimetrix.core.meter.api.ProcessMeter;
import com.aktimetrix.core.stereotypes.Measurement;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
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
        if (bean instanceof Meter || bean instanceof ProcessMeter) {
            Measurement annotation = AnnotationUtils.getAnnotation(bean.getClass(), Measurement.class);
            if (annotation != null) {
                validate(beanName, bean, annotation);
                logger.info("Measurement Code: {}, Step Code: {}, Process Code: {}", annotation.code(),
                        annotation.stepCode(), annotation.processCode());
                Map<String, String> attributes = new HashMap<>();

                attributes.putAll(Map.of(Constants.ATT_METER_SERVICE, Constants.VAL_YES,
                        Constants.ATT_CODE, annotation.code(),
                        Constants.ATT_STEP_CODE, annotation.stepCode(),
                        Constants.ATT_PROCESS_CODE, annotation.processCode()));
                this.registry.register(beanName, attributes, bean);
            }
            logger.debug("Called postProcessBeforeInitialization() for : {}", beanName);
        }
        return bean;
    }

    private static void validate(String beanName, Object bean, Measurement annotation) {
        final boolean stepLevel = !annotation.stepCode().isEmpty();
        final boolean processLevel = !annotation.processCode().isEmpty();
        if (stepLevel == processLevel) {
            throw new BeanInitializationException("@Measurement on " + beanName
                    + " must set exactly one of stepCode and processCode");
        }
        if (stepLevel && !(bean instanceof Meter)) {
            throw new BeanInitializationException(beanName + " measures step " + annotation.stepCode()
                    + " and must implement Meter, e.g. by extending AbstractMeter");
        }
        if (processLevel && !(bean instanceof ProcessMeter)) {
            throw new BeanInitializationException(beanName + " measures process " + annotation.processCode()
                    + " and must implement ProcessMeter, e.g. by extending AbstractProcessMeter");
        }
    }
}
