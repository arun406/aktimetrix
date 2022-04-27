package com.aktimetrix.service.meter.core.util;

public class BooleanUtils {

    /**
     * return Y or N
     *
     * @param b
     * @return
     */
    public static String toStringYOrN(boolean b) {
        return b ? "Y" : "N";
    }

    /**
     * return Y or N
     *
     * @param b
     * @return
     */
    public static String toStringYesOrNo(boolean b) {
        return b ? "Yes" : "No";
    }

    /**
     * return true or false
     *
     * @param s
     * @return
     */
    public static Boolean toBoolean(String s) {
        return s.equalsIgnoreCase("Y") ? true : false;
    }
}
