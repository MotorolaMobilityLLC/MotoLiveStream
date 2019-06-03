package com.motorola.livestream.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public abstract class AbstractPermissionActivity extends AppCompatActivity {

    private static final int PERM_REQ_START_UP = 1;

    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };

    private boolean mIsPermissionGranted = false;

    protected abstract void onGetPermissionsSuccess();
    protected abstract void onGetPermissionsFailure();

    boolean checkAppPermissionsGranted() {
        boolean needRequest = false;
        for (String permission : PERMISSIONS) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
            }
        }
        mIsPermissionGranted = !needRequest;

        return mIsPermissionGranted;
    }

    boolean checkAndRequestAppPermissions() {
        boolean needRequest = false;
        ArrayList<String> permissionList = new ArrayList<>();
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission);
                needRequest = true;
            }
        }
        if (needRequest) {
            String[] requiredPermissions =
                    permissionList.toArray(new String[permissionList.size()]);
            requestPermissions(requiredPermissions, PERM_REQ_START_UP);
        }
        mIsPermissionGranted = !needRequest;

        return mIsPermissionGranted;
    }

    private boolean checkPermissionGrantResults(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERM_REQ_START_UP: {
                mIsPermissionGranted = checkPermissionGrantResults(grantResults);
                if (mIsPermissionGranted) {
                    onGetPermissionsSuccess();
                } else {
                    onGetPermissionsFailure();
                }
            }
            default:
                break;
        }
    }

}
