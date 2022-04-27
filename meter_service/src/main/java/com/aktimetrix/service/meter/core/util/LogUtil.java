package com.aktimetrix.service.meter.core.util;

import org.slf4j.Logger;

public class LogUtil {

    /**
     * Logging info messages with format support and its arguments
     *
     * @param log
     * @param message
     * @param args
     */
    public static void info(Logger log, String message, Object... args) {
        if (log.isInfoEnabled()) {
            if (args == null) {
                log.info(message);
            } else {
                log.info(StringUtil.fmt(message, args));
            }
        }
    }

    /**
     * Logging debug messages with format support and its arguments
     *
     * @param log
     * @param message
     * @param args
     */
    public static void debug(Logger log, String message, Object... args) {
        if (log.isDebugEnabled()) {
            if (args == null) {
                log.debug(message);
            } else {
                log.debug(StringUtil.fmt(message, args));
            }
        }
    }


    /**
     * Logging trace messages with format support and its arguments
     *
     * @param log
     * @param message
     * @param args
     */
    public static void trace(Logger log, String message, Object... args) {
        if (log.isTraceEnabled()) {
            if (args == null) {
                log.trace(message);
            } else {
                log.trace(StringUtil.fmt(message, args));
            }
        }
    }


    /**
     * Logging warning for the message with string formatting and its arguments
     *
     * @param log
     * @param message
     * @param args
     */
    public static void warn(Logger log, String message, Object... args) {
        //System.err.println("Warning : "+fmt(message,args));
        warn(log, null, message, args);
    }


    /**
     * Logging warning for the message with string formatting and its arguments with the exception
     *
     * @param log
     * @param err
     * @param message
     * @param args
     */
    public static void warn(Logger log, Throwable err, String message, Object... args) {
        if (log.isWarnEnabled()) {
            if (err != null) {
                if (args == null) {
                    log.warn(message, err);
                } else {
                    log.warn(StringUtil.fmt(message, args), err);
                }
            } else {
                if (args == null) {
                    log.warn(message);
                } else {
                    log.warn(StringUtil.fmt(message, args));
                }
            }
        }
    }


    /**
     * Logging errors for the message with string formatting and its arguments
     *
     * @param log
     * @param message
     * @param args
     */
    public static void error(Logger log, String message, Object... args) {
        error(log, null, message, args);
    }


    /**
     * Logging errors for the message with string formatting and its arguments
     *
     * @param log
     * @param err
     * @param message
     * @param args
     */
    public static void error(Logger log, Throwable err, String message, Object... args) {
        if (log.isErrorEnabled()) {
            if (err != null) {
                if (args == null) {
                    log.error(message, err);
                } else {
                    log.error(StringUtil.fmt(message, args), err);
                }
            } else {
                if (args == null) {
                    log.error(message);
                } else {
                    log.error(StringUtil.fmt(message, args));
                }
            }
        }
    }


}
