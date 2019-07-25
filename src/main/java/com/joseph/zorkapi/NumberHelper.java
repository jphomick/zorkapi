package com.joseph.zorkapi;

public class NumberHelper {

    public static boolean isNumber(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
