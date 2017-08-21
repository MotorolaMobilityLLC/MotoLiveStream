/*
 *
 *  * Copyright (C) 2013-2016 Motorola Mobility LLC,
 *  * All Rights Reserved.
 *  * Motorola Mobility Confidential Restricted.
 *
 *
 */

package com.motorola.cameramod360;

import android.app.Application;

public class CamMod360App extends Application {

    private static CamMod360App sInstance;

    public static CamMod360App getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

}
