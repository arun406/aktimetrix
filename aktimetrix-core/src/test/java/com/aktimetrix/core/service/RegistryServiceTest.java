package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.impl.DefaultRegistry;
import com.aktimetrix.core.postbeanprocessors.PostProcessorPostBeanProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegistryServiceTest {

    private final DefaultRegistry registry = new DefaultRegistry();
    private final RegistryService registryService = new RegistryService();

    private final Publisher publisher = new Publisher();
    private final Audit audit = new Audit();
    private final Notify notify = new Notify();
    private final MeterOnly meterOnly = new MeterOnly();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(registryService, "registry", registry);
        PostProcessorPostBeanProcessor beanProcessor = new PostProcessorPostBeanProcessor(registry);
        beanProcessor.postProcessAfterInitialization(publisher, "publisher");
        beanProcessor.postProcessAfterInitialization(audit, "audit");
        beanProcessor.postProcessAfterInitialization(notify, "notify");
        beanProcessor.postProcessAfterInitialization(meterOnly, "meterOnly");
    }

    @Test
    void postProcessorsOfTheProcessAndOfAllProcessesRunByPriority() {
        List<PostProcessor> processors = registryService.getPostProcessor("ORDER_DELIVERY");

        assertThat(processors).containsExactly(notify, audit, publisher);
    }

    @Test
    void allProcessesWildcardCanBeExcluded() {
        assertThat(registryService.getPostProcessor(Constants.METER_PROCESSOR, false)).containsExactly(meterOnly);
        assertThat(registryService.getPostProcessor("OTHER_PROCESS", false)).isEmpty();
    }

    @com.aktimetrix.core.stereotypes.PostProcessor(code = "PUBLISH", processType = Constants.ALL_PROCESS_TYPES,
            priority = Constants.BUILT_IN_PRIORITY)
    static class Publisher implements PostProcessor {
        @Override
        public void postProcess(Context context) {
        }
    }

    @com.aktimetrix.core.stereotypes.PostProcessor(code = "AUDIT", processType = "ORDER_DELIVERY", priority = 5)
    static class Audit implements PostProcessor {
        @Override
        public void postProcess(Context context) {
        }
    }

    @com.aktimetrix.core.stereotypes.PostProcessor(code = "NOTIFY", processType = "ORDER_DELIVERY")
    static class Notify implements PostProcessor {
        @Override
        public void postProcess(Context context) {
        }
    }

    @com.aktimetrix.core.stereotypes.PostProcessor(code = "METER_ONLY", processType = Constants.METER_PROCESSOR)
    static class MeterOnly implements PostProcessor {
        @Override
        public void postProcess(Context context) {
        }
    }
}
