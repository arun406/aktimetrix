package com.aktimetrix.service.meter;

/**
 * @author arun kumar kandakatla
 */
public class UnknownNameException extends Exception {
    public UnknownNameException() {
    }

    public UnknownNameException(String message) {
        super(message);
    }

    public UnknownNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownNameException(Throwable cause) {
        super(cause);
    }

    public UnknownNameException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
