package com.aktimetrix.core.util;

import com.aktimetrix.core.stereotypes.Loggable;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

@Slf4j
public class EntryExitDynamicInvocationHandler extends InvocationHandlerWithTarget {

    private final Map<String, Method> methods = new HashMap<>();

    public EntryExitDynamicInvocationHandler(Object target) {
        super(target);
        for (Method method : target.getClass().getDeclaredMethods()) {
            this.methods.put(method.getName(), method);
        }
    }

    @Override
    public Object invoke(final Object bean, final Method method, final Object[] args) throws Throwable {

        final Method originalMethod = this.methods.get(method.getName());
        final boolean annotationPresent = originalMethod.isAnnotationPresent(Loggable.class);
        log.trace("Annotation present : {}", annotationPresent);
        final Loggable annotation = originalMethod.getAnnotation(Loggable.class);
        Object result = null;
        Level level = null;
        boolean showArgs = false;
        boolean showResult = false;
        boolean showExecutionTime = false;
        String methodName = method.getName();
        final Parameter[] parameters = method.getParameters();
        Object[] methodArgs = null;
        ChronoUnit unit = null;
        if (annotation != null) {
            level = annotation.value();
            unit = annotation.unit();
            showArgs = annotation.showArgs();
            showResult = annotation.showResult();
            showExecutionTime = annotation.showExecutionTime();
            methodArgs = args;
            log.trace("Logging the method with params :  [ methodName: {}, methodArgs: {}, parameters: {}, Level: {}, " +
                            "showArgs: {}, showResult: {}, showExecutionTime: {} ] ",
                    methodName, methodArgs, parameters, level, showArgs, showResult, showExecutionTime);
        }
        log(log, level, entry(methodName, showArgs, parameters, methodArgs));
        Instant start = Instant.now();

        result = methods.get(method.getName()).invoke(getTarget(), args);
        Instant end = Instant.now();
        String duration = String.format("%s %s", unit.between(start, end), unit.name().toLowerCase());
        log(log, level, exit(methodName, duration, result, showResult, showExecutionTime));
        return result;
    }


    /**
     * @param methodName
     * @param duration
     * @param result
     * @param showResult
     * @param showExecutionTime
     * @return
     */
    static String exit(String methodName, String duration, Object result, boolean showResult, boolean showExecutionTime) {
        StringJoiner message = new StringJoiner(" ")
                .add("Finished").add(methodName).add("method");
        if (showExecutionTime) {
            message.add("in").add(duration);
        }
        if (showResult && result != null) {
            message.add("with return:").add(result.toString());
        }
        return message.toString();
    }

    /**
     * @param methodName
     * @param showArgs
     * @param params
     * @param args
     * @return
     */
    static String entry(String methodName, boolean showArgs, Parameter[] params, Object[] args) {
        StringJoiner message = new StringJoiner(" ")
                .add("Started").add(methodName).add("method");
        if (showArgs && Objects.nonNull(params) && Objects.nonNull(args) && params.length == args.length) {
            Map<Parameter, Object> values = new HashMap<>(params.length);
            for (int i = 0; i < params.length; i++) {
                values.put(params[i], args[i]);
            }
            message.add("with args:")
                    .add(values.toString());
        }
        return message.toString();
    }

    static void log(Logger logger, Level level, String message) {
        switch (level) {
            case DEBUG:
                logger.debug(message);
                break;
            case TRACE:
                logger.trace(message);
                break;
            case WARN:
                logger.warn(message);
                break;
            case ERROR:
                logger.error(message);
                break;
            default:
                logger.info(message);
        }
    }
}
