package com.motorola.livestream.util;

public class Log {

    private static final boolean DEBUG = true;
    private static final String TAG = "Facebook";

    public static void i(String logTag, String message) {
        if (DEBUG) {
            android.util.Log.i(TAG, logTag + " : " + message);
        }
    }

    public static void d(String logTag, String message) {
        if (DEBUG) {
            android.util.Log.d(TAG, logTag + " : " + message);
        }
    }

    public static void w(String logTag, String message) {
        if (DEBUG) {
            android.util.Log.w(TAG, logTag + " : " +message);
        }
    }

    public static void e(String logTag, String message) {
        if (DEBUG) {
            android.util.Log.e(TAG, logTag + " : " +message);
        }
    }
}
