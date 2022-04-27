package com.aktimetrix.service.meter.core.util;

/**
 * Created by vj on 16/02/17.
 */
public class StringUtil {

    /**
     * Helper for formtting strings
     * Useful if statically imported
     * @param msg
     * @param args
     * @return
     */
    public static String fmt(String msg,Object... args){
        return String.format(msg,args);
    }
}