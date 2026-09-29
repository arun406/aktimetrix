package com.aktimetrix.it.support;

import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.store.MeasurementInstanceStore;
import com.aktimetrix.core.store.OutboxStore;
import com.aktimetrix.core.store.ProcessInstanceStore;
import com.aktimetrix.core.store.StepInstanceStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * An Aktimetrix application started on a test broker and store, and what a test needs to observe it.
 */
public class TestMonitor implements AutoCloseable {

    private final ConfigurableApplicationContext context;
    private final String tenant;

    public TestMonitor(Class<?> application, String tenant, TestBroker broker, TestStore store, String... properties) {
        this.tenant = tenant;
        final Map<String, Object> all = new HashMap<>(broker.properties());
        all.putAll(store.properties());
        all.put("aktimetrix.monitor.enabled", "false");
        this.context = new SpringApplicationBuilder(application)
                .web(WebApplicationType.NONE)
                .initializers(ctx -> ctx.getBeanFactory().registerSingleton("meterRegistry", new SimpleMeterRegistry()))
                .properties(all)
                .properties(properties)
                .run();
    }

    public <T> T bean(Class<T> type) {
        return context.getBean(type);
    }

    public MeterRegistry meters() {
        return bean(MeterRegistry.class);
    }

    public Optional<ProcessInstance> process(String processCode, String entityId) {
        return bean(ProcessInstanceStore.class).findByEntityId(tenant, entityId).stream()
                .filter(p -> processCode.equals(p.getProcessCode())).findFirst();
    }

    public List<StepInstance> steps(String entityId) {
        return bean(ProcessInstanceStore.class).findByEntityId(tenant, entityId).stream()
                .flatMap(p -> bean(StepInstanceStore.class).findByProcessInstance(tenant, p.getId()).stream())
                .collect(Collectors.toList());
    }

    public Optional<StepInstance> step(String entityId, String stepCode) {
        return steps(entityId).stream().filter(s -> stepCode.equals(s.getStepCode())).findFirst();
    }

    public List<MeasurementInstance> measurements(String entityId) {
        return bean(ProcessInstanceStore.class).findByEntityId(tenant, entityId).stream()
                .flatMap(p -> bean(MeasurementInstanceStore.class).findByProcessInstance(tenant, p.getId()).stream())
                .collect(Collectors.toList());
    }

    public long pendingOutbox() {
        return bean(OutboxStore.class).countPending();
    }

    /**
     * Waits up to 20 seconds for the value to exist and satisfy the condition.
     */
    public static <T> T await(Supplier<Optional<T>> value, java.util.function.Predicate<T> condition, String what) {
        final long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            final Optional<T> current = value.get();
            if (current.isPresent() && condition.test(current.get())) {
                return current.get();
            }
            sleep();
        }
        throw new AssertionError(what + " did not happen in time");
    }

    public static void awaitTrue(BooleanSupplier condition, String what) {
        await(() -> Optional.of(condition.getAsBoolean()), ok -> ok, what);
    }

    private static void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    @Override
    public void close() {
        context.close();
    }
}
