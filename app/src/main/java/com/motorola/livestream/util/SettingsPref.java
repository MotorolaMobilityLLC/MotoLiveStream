package com.motorola.livestream.util;

import android.content.Context;

public class SettingsPref {
    private static final String PREF_FILE = "settings_pref.xml";

    private static final String USER_AVATAR_URL = "USER_AVATAR_URL";

    public static String getUserPhotoUrl(Context context) {
        return context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .getString(USER_AVATAR_URL, null);
    }

    public static void saveUserPhotoUrl(Context context, String url) {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .edit()
                .putString(USER_AVATAR_URL, url)
                .apply();
    }
}
