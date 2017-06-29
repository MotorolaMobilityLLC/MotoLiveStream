package com.motorola.livestream.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import com.motorola.livestream.model.fb.User;

import java.util.Locale;

public class Util {

    private static final int THOUSAND = 1000;
    private static final int MEGA = 1000000;

    private static final String FACEBOOK_PACKAGE_NAME = "com.facebook.katana";

    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager manager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo != null) {
                return networkInfo.isConnected();
            }
        }
        return false;
    }

    public static String getFormattedNumber(int number) {
        if (number <= 0) {
            // TBD with CXD, shall we display 0 or empty when the number is 0
            return String.valueOf(0);
            //return null;
        } else if (number < THOUSAND) {
            return String.valueOf(number);
        } else if (number < MEGA) {
            return String.format(Locale.ENGLISH, "%.1fK", number / (THOUSAND * 1.0f));
        } else {
            return String.format(Locale.ENGLISH, "%.1fM", number / (MEGA * 1.0f));
        }
    }

    public static void jumpToFacebook(Context context, User currentUser) {
        if (context == null || currentUser == null) {
            return;
        }

        Intent intent = null;
        try {
            PackageInfo pkgInfo =
                    context.getPackageManager().getPackageInfo(FACEBOOK_PACKAGE_NAME, 0);
            if (pkgInfo.applicationInfo.enabled) {
                intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("facebook://profile/" + currentUser.getId()));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (intent == null) {
            intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://www.facebook.com/"));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }
}
