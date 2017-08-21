/*
 *
 *  * Copyright (C) 2013-2016 Motorola Mobility LLC,
 *  * All Rights Reserved.
 *  * Motorola Mobility Confidential Restricted.
 *
 *
 */

package com.motorola.cameramod360;

import android.os.Build;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;

public class Util {
    public static final boolean ENG_BUILD = "eng".equals(Build.TYPE);
    public static final boolean USERDEBUG_BUILD = "userdebug".equals(Build.TYPE);
    public static final boolean USER_BUILD = !ENG_BUILD && !USERDEBUG_BUILD;
    public static final boolean DEBUG = !(USER_BUILD);

    private static final String TAG = Util.class.getSimpleName();

    private Util() {
    }

    public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException t) {
            if (Util.DEBUG) Log.d(TAG, "close fail ", t);
        }
    }
}
