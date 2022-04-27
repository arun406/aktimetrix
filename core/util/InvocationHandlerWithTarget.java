package com.aktimetrix.core.util;

import java.lang.reflect.InvocationHandler;

/**
 *
 */
public abstract class InvocationHandlerWithTarget implements InvocationHandler {

    /*Original Object*/
    protected final Object target;

    public InvocationHandlerWithTarget(Object target) {
        this.target = target;
    }

    public Object getTarget() {
        return target;
    }
}
