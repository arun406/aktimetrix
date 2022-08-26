package com.aktimetrix.core.exception;

public class PostProcessorNotFoundException extends Exception {
    public PostProcessorNotFoundException() {
        super();
    }

    public PostProcessorNotFoundException(String message) {
        super(message);
    }

    public PostProcessorNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public PostProcessorNotFoundException(Throwable cause) {
        super(cause);
    }

    protected PostProcessorNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
