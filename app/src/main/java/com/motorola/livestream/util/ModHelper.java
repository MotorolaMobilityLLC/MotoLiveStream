package com.motorola.livestream.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class ModHelper {
    public static final int VENDOR_MOTOROLA = 0x128;
    public static final int PRODUCT_HASSELBLAD_TRUE_ZOOM = 1536;
    public static final int PRODUCT_MOTO_360 = 1552;
    public static final String EXTRA_VID = "vid";
    public static final String EXTRA_PID = "pid";
    public static final String EXTRA_UID = "uid";
    public static final Uri CONTENT_URI_MOD = Uri.parse("content://com.motorola.cameramod.provider/mod_info");

    private static final int PRODUCT_ID_HW_REV_MASK = 0xff;

    public static final String ACTION_MOTO_360_ATTACHED = "com.motorola.livestream.action.MOTO_360_ATTACHED";
    public static final String ACTION_MOTO_360_DETACHED = "com.motorola.livestream.action.MOTO_360_DETACHED";

    private static boolean sIsModCameraAttached = false;

    public static int getProduct(int pid) {
        return (pid ^ PRODUCT_ID_HW_REV_MASK) >> 8;
    }

    public static int getHwRev(int pid) {
        return pid & PRODUCT_ID_HW_REV_MASK;
    }

    public static boolean isModCamera(Context context) {
        int productId = getModProductId(context);

        if (isModHasselblad(productId) || isModMoto360(productId)) {
            sIsModCameraAttached = true;
        } else {
            sIsModCameraAttached = false;
        }
        return sIsModCameraAttached;
    }

    public static boolean isModMoto360(Context context) {
        int productId = getModProductId(context);
        if (isModMoto360(productId)) {
            sIsModCameraAttached = true;
            return true;
        } else {
            sIsModCameraAttached = false;
            return false;
        }
    }

    private static int getModProductId(Context context) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(CONTENT_URI_MOD, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                return getProduct(cursor.getInt(0));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    private static boolean isModHasselblad(int pid) {
        return PRODUCT_HASSELBLAD_TRUE_ZOOM == pid;
    }

    private static boolean isModMoto360(int pid) {
        return PRODUCT_MOTO_360 == pid;
    }

    public static boolean isModCameraAttached() {
        return sIsModCameraAttached;
    }

    public static void setModCameraDetached() {
        sIsModCameraAttached = false;
    }

}
