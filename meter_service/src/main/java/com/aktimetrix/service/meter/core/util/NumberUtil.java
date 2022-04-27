package com.aktimetrix.service.meter.core.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 *
 */
public class NumberUtil {
    /**
     * @param d1
     * @param d2
     * @param wtVolIndicator
     * @return
     */
    public static double addTwoDoubleValues(double d1, double d2, int wtVolIndicator) {
        double value = d1 + d2;
        value = (getFormattedValue(value, wtVolIndicator)).doubleValue();
        return value;
    }


    /**
     * @param input
     * @param wtVolIndicator
     * @return
     */
    public static Double getFormattedValue(double input, int wtVolIndicator) {
        int totalNoOfDigits = 0;
        Double maxValue = null;
        String formatString = null;
        //weight is stored up to 3 precision and volume is up to 8 precision
        if (wtVolIndicator == 1) {
            //      totalNoOfDigits = 7 ;
            totalNoOfDigits = 10;
            maxValue = Double.valueOf(9999999);
            formatString = "#######0.0##";
        } else if (wtVolIndicator == 2) {
            //      totalNoOfDigits = 9 ;
            totalNoOfDigits = 12;
            maxValue = Double.valueOf(999999999);
            formatString = "0.########";
        }
        DecimalFormat formatter = new DecimalFormat(formatString);
        BigDecimal b = new BigDecimal(formatter.format(input));
        Double hVolume = Double.valueOf(b.doubleValue());
        String vol = formatter.format(hVolume);
        hVolume = numericFormatter(vol, totalNoOfDigits, maxValue);
        return hVolume;
    }

    /**
     * After calculation of numeric value,if the value exceeds the max value
     * then max value will be taken
     *
     * @param numericVal
     * @param totalDigits
     * @param maxValue
     * @return
     */
    public static Double numericFormatter(String numericVal, int totalDigits, Double maxValue) {
        int totalCount = numericVal.length();
        String actualVal = numericVal;
        /*
         * If the totalCount of value less than maxNoOfDigts specified
         * then remaining digits should be used for displaying decimals (count includes decimal point)
         *
         */
        if (totalCount > totalDigits) {
            String withoutdecimalstring = null;
            int indexOfDecimal = numericVal.indexOf(".");

            if (indexOfDecimal == -1)
                withoutdecimalstring = numericVal;
            else
                withoutdecimalstring = numericVal.substring(0, indexOfDecimal);

            actualVal = withoutdecimalstring;
            if (withoutdecimalstring.length() < totalDigits) {
                String decimalString = numericVal.substring(indexOfDecimal, totalDigits - withoutdecimalstring.length() + indexOfDecimal);
                actualVal = withoutdecimalstring + decimalString;
                if (actualVal.lastIndexOf(".") == actualVal.length() - 1) {
                    actualVal = actualVal.substring(0, actualVal.lastIndexOf("."));
                }
            } else if (withoutdecimalstring.length() > totalDigits) {
                actualVal = maxValue.toString();
            }
        }

        return Double.valueOf(actualVal);
    }

}
