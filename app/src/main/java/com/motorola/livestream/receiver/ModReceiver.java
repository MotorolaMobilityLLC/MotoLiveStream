package com.motorola.livestream.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.motorola.livestream.util.ModHelper;

public class ModReceiver extends BroadcastReceiver {
    private static final String TAG = ModReceiver.class.getSimpleName();
    public static final String ACTION_MOD_ATTACH =
            "com.motorola.mod.action.MOD_ATTACH";
    public static final String ACTION_MOD_DETACH =
            "com.motorola.mod.action.MOD_DETACH";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        switch (action) {
            case ACTION_MOD_ATTACH:
                if (ModHelper.isModCamera()) {
                    System.exit(0);
                }
                break;
            case ACTION_MOD_DETACH:
                if (ModHelper.isModCamera()) {
                    System.exit(0);
                }
                break;
            default:
                break;
        }
    }
}
