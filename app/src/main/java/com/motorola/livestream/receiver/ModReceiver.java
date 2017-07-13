package com.motorola.livestream.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

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
                if (ModHelper.isModCamera(context)) {
                    Intent broadcastIntent = new Intent(ModHelper.ACTION_MOTO_360_ATTACHED);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
                }
                break;
            case ACTION_MOD_DETACH:
                if (ModHelper.isModCameraAttached()) {
                    Intent broadcastIntent = new Intent(ModHelper.ACTION_MOTO_360_DETACHED);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
                    ModHelper.setModCameraDetached();
                }
                break;
            default:
                break;
        }
    }
}
