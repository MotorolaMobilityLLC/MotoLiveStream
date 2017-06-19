package com.motorola.livestream.util;

public class Util {

    private static final int HOUSAND = 1000;
    private static final int MEGA = 1000000;

    public static String getFormattedNumber(int number) {
        if (number <= 0) {
            // TBD with CXD, shall we display 0 or empty when the number is 0
            // return String.valueOf(0);
            return null;
        } else if (number < HOUSAND) {
            return String.valueOf(number);
        } else if (number < 1000000) {
            return String.format("%.1fK", number / (HOUSAND * 1.0f));
        } else {
            return String.format("%.1fM", number / (MEGA * 1.0f));
        }
    }
}
