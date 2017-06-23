package com.motorola.livestream.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

public abstract class AbstractPermissionActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_STORAGE = 1;
    private boolean mIsPermissionGranted = false;

    protected abstract void onGetPermissionsSuccess();
    protected abstract void onGetPermissionsFailure();

    void checkAppPermissionGranted() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
        };
        requestPermission(permissions, PERMISSION_REQUEST_STORAGE);
    }

    private void requestPermission(String[] permissions, int requestCode) {
        boolean needRequest = false;
        ArrayList<String> permissionList = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission);
                needRequest = true;
            }
        }

        if (needRequest) {
            int count = permissionList.size();
            if (count > 0) {
                String[] permissionArray = new String[count];
                for (int i = 0; i < count; i++) {
                    permissionArray[i] = permissionList.get(i);
                }

                ActivityCompat.requestPermissions(this, permissionArray, requestCode);
            }
        }
        mIsPermissionGranted = !needRequest;
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
        mIsPermissionGranted = checkPermissionGrantResults(grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_STORAGE: {
                if (mIsPermissionGranted) {
                    onGetPermissionsSuccess();
                } else {
                    onGetPermissionsFailure();
                }
            }
        }
    }

    boolean isPermissionGranted() {
        return mIsPermissionGranted;
    }
}
