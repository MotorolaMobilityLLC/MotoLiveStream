package com.motorola.livestream.util;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import static com.facebook.FacebookSdk.getApplicationContext;

public class ModHelper {
    public static final int VENDOR_MOTOROLA = 0x128;
    public static final int PRODUCT_HASSELBLAD_TRUE_ZOOM = 1536;
    public static final int PRODUCT_MOTO_360 = 1552;
    public static final String EXTRA_VID = "vid";
    public static final String EXTRA_PID = "pid";
    public static final String EXTRA_UID = "uid";
    public static final Uri CONTENT_URI_MOD = Uri.parse("content://com.motorola.cameramod.provider/mod_info");

    private static final int PRODUCT_ID_HW_REV_MASK = 0xff;

    private ModHelper() {
    }

    public static int getProduct(int pid) {
        return (pid ^ PRODUCT_ID_HW_REV_MASK) >> 8;
    }

    public static int getHwRev(int pid) {
        return pid & PRODUCT_ID_HW_REV_MASK;
    }

    public static boolean isModHasselblad(int pid) {
        return PRODUCT_HASSELBLAD_TRUE_ZOOM == getProduct(pid);
    }

    public static boolean isModMoto360(int pid) {
        return PRODUCT_MOTO_360 == getProduct(pid);
    }

    public static boolean isModCamera() {
        boolean isModCamera = false;
        ContentResolver resolver = getApplicationContext().getContentResolver();
        Cursor cursor = resolver.query(CONTENT_URI_MOD, null, null, null, null);
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            int product = getProduct(cursor.getInt(0));
            isModCamera = (product == PRODUCT_HASSELBLAD_TRUE_ZOOM ||
            product == PRODUCT_MOTO_360);
        }
        if (cursor != null) cursor.close();

        return isModCamera;
    }
}
