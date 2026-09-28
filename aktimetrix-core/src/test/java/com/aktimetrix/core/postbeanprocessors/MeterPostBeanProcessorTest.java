package com.aktimetrix.core.postbeanprocessors;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.impl.DefaultRegistry;
import com.aktimetrix.core.meter.impl.AbstractMeter;
import com.aktimetrix.core.meter.impl.AbstractProcessMeter;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.stereotypes.Measurement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeterPostBeanProcessorTest {

    private final DefaultRegistry registry = new DefaultRegistry();
    private final MeterPostBeanProcessor postProcessor = new MeterPostBeanProcessor(registry);

    @Test
    void registersStepAndProcessMetersAtTheirOwnLevel() {
        StepTimeMeter stepMeter = new StepTimeMeter();
        RouteDistanceMeter processMeter = new RouteDistanceMeter();
        postProcessor.postProcessAfterInitialization(stepMeter, "stepMeter");
        postProcessor.postProcessAfterInitialization(processMeter, "processMeter");

        RegistryService registryService = new RegistryService();
        ReflectionTestUtils.setField(registryService, "registry", registry);
        assertThat(registryService.getMeter("T1", "SHIP", "TIME")).isSameAs(stepMeter);
        assertThat(registryService.getProcessMeter("T1", "ORDER", "DISTANCE")).isSameAs(processMeter);
        assertThat(registryService.getProcessMeter("T1", "ORDER", "TIME")).isNull();
        assertThat(registryService.getMeter("T1", "ORDER", "DISTANCE")).isNull();
    }

    @Test
    void rejectsAMeterWithoutExactlyOneLevel() {
        assertThatThrownBy(() -> postProcessor.postProcessAfterInitialization(new NoLevelMeter(), "noLevel"))
                .isInstanceOf(BeanInitializationException.class)
                .hasMessageContaining("exactly one of stepCode and processCode");
    }

    @Test
    void rejectsAStepMeterDeclaredForAProcess() {
        assertThatThrownBy(() -> postProcessor.postProcessAfterInitialization(new MisplacedMeter(), "misplaced"))
                .isInstanceOf(BeanInitializationException.class)
                .hasMessageContaining("must implement ProcessMeter");
    }

    // inner (non-static) classes, so that the application's component scan in other tests leaves them out
    @Measurement(code = "TIME", stepCode = "SHIP")
    class StepTimeMeter extends AbstractMeter {
        @Override
        protected String getMeasurementUnit(String tenant, StepInstance step) {
            return Constants.MEASUREMENT_UNIT_TIMESTAMP;
        }

        @Override
        protected String getMeasurementValue(String tenant, StepInstance step) {
            return null;
        }
    }

    @Measurement(code = "DISTANCE", processCode = "ORDER")
    class RouteDistanceMeter extends AbstractProcessMeter {
        @Override
        protected String getMeasurementUnit(String tenant, ProcessInstance process) {
            return "KM";
        }

        @Override
        protected String getMeasurementValue(String tenant, ProcessInstance process) {
            return "12";
        }
    }

    @Measurement(code = "TIME")
    class NoLevelMeter extends StepTimeMeter {
    }

    @Measurement(code = "DISTANCE", processCode = "ORDER")
    class MisplacedMeter extends StepTimeMeter {
    }
}
