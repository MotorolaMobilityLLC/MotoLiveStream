package com.motorola.livestream.ui;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.DefaultAudience;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.motorola.livestream.R;
import com.motorola.livestream.util.FbPermission;
import com.motorola.livestream.util.Log;
import com.motorola.livestream.util.Util;

import java.util.Collections;

public class MainActivity extends AbstractPermissionActivity {

    private static final String TAG = "Login";

    private static final int REQUEST_NOTIFICATION_POLICY_ACCESS = 0x101;

    private static final String URI_PACKAGE = "package:";

    private CallbackManager mCallbackManager;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            handleAllPermissions();
        }
    };

    private boolean mIsJumpedToAppInfo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Util.isNetworkConnected(getApplicationContext())) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.label_network_not_available)
                    .setPositiveButton(R.string.btn_ok,
                            (DialogInterface dialog, int which) -> {
                                MainActivity.this.finish();
                            }
                    )
                    .show();
        } else {
            mCallbackManager = CallbackManager.Factory.create();

            handleAllPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mIsJumpedToAppInfo) {
            mIsJumpedToAppInfo = false;
            if (!checkAppPermissionsGranted()) {
                // If still not granted the permissions from AppInfo, show required dialog again
                showPermissionRequiredDialog();
            } else {
                // Since all permissions were granted, continue check other permissions
                handleAllPermissions();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_NOTIFICATION_POLICY_ACCESS:
                // No matter user allow app to access notification policy or not
                // Just enter app this time
                // Begin, Lenovo, guzy2, IKSWN-73799, show dialog of requesting "Do Not Disturb" permission
                startLiveActivity();
                // End, Lenovo, guzy2, IKSWN-73799
                break;
            default:
                mCallbackManager.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    protected void onGetPermissionsSuccess() {
        Log.d(TAG, "Permission granted");
        // Begin, Lenovo, guzy2, IKSWN-73799, show dialog of requesting "Do Not Disturb" permission
        if (checkAudioChangePermission()) {
            startLiveActivity();
        }
        // End, Lenovo, guzy2, IKSWN-73799
    }

    @Override
    protected void onGetPermissionsFailure() {
        showPermissionRequiredDialog();
    }

    private void handleAllPermissions() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken == null || accessToken.isExpired()
                || !accessToken.getPermissions().contains(FbPermission.READ_CUSTOM_FRIEND_LIST)) {
            loginToFacebook(FbPermission.READ_CUSTOM_FRIEND_LIST);
        } else if (!accessToken.getPermissions().contains(FbPermission.PUBLISH_ACTION)) {
            loginToFacebook(FbPermission.PUBLISH_ACTION);
        } else {
            // Begin, Lenovo, guzy2, IKSWN-73799, show dialog of requesting "Do Not Disturb" permission
            if (checkAndRequestAppPermissions()) {
            // End, Lenovo, guzy2, IKSWN-73799
                onGetPermissionsSuccess();
            }
        }
    }

    private void loginToFacebook(String permission) {
        if (FbPermission.PUBLISH_ACTION.equals(permission)) {
            LoginManager.getInstance().
                    logInWithPublishPermissions(this, Collections.singletonList(permission));
            LoginManager.getInstance().setDefaultAudience(DefaultAudience.FRIENDS);
        } else {
            LoginManager.getInstance().
                    logInWithReadPermissions(this, Collections.singletonList(permission));
        }
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "Login onSuccess: " + loginResult.toString());
                mHandler.sendEmptyMessage(0);
            }

            @Override
            public void onCancel() {
                Log.w(TAG, "Login canceled for: " + permission);
                // Begin, Lenovo, guzy2, IKSWN-73849, Do not show cancelled or error dialog
                //showCancelDialog(permission);
                // End, Lenovo, guzy2, IKSWN-73849
                MainActivity.this.finish();
            }

            @Override
            public void onError(FacebookException error) {
                Log.w(TAG, "Login failed for: " + permission);
                error.printStackTrace();
                // Begin, Lenovo, guzy2, IKSWN-73849, Do not show cancelled or error dialog
                //showErrorDialog();
                // End, Lenovo, guzy2, IKSWN-73849
                MainActivity.this.finish();
            }
        });
    }

    private void showCancelDialog(String permission) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (FbPermission.PUBLISH_ACTION.equals(permission)) {
            builder.setMessage(R.string.dlg_publish_not_granted);
        } else {
            builder.setMessage(R.string.dlg_public_profile_not_granted);
        }
        builder.setCancelable(false)
                .setPositiveButton(R.string.btn_retry,
                        (DialogInterface dialog, int which) -> {
                            mHandler.sendEmptyMessage(0);
                    })
                .setNegativeButton(R.string.btn_exit,
                        (DialogInterface dialog, int which) -> {
                            MainActivity.this.finish();
                    })
                .show();
    }

    private void showErrorDialog() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.dlg_fb_login_failed))
                .setCancelable(false)
                .setPositiveButton(R.string.btn_exit,
                        (DialogInterface dialog, int which) -> {
                            MainActivity.this.finish();
                        })
                .show();
    }

    private void showPermissionRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.perm_required_alert_title)
                .setMessage(R.string.perm_required_alert_msg)
                .setPositiveButton(R.string.perm_required_alert_button_app_info,
                        (DialogInterface dialog, int which) -> {
                            jumpToAppInfo();
                        })
                .setOnCancelListener((DialogInterface dialog) -> {
                            MainActivity.this.finish();
                        }
                )
                .show();
    }

    private void jumpToAppInfo() {
        mIsJumpedToAppInfo = true;

        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse(URI_PACKAGE + getPackageName()));
        startActivity(intent);
    }

    private boolean checkAudioChangePermission() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !notificationManager.isNotificationPolicyAccessGranted()) {
            showRequestDialog();
            return false;
        }
        return true;
    }

    private void showRequestDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.dlg_request_notification_policy_access)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_setting,
                        (DialogInterface dialog, int which) -> {
                            Intent intent = new Intent(
                                    Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                            startActivityForResult(intent, REQUEST_NOTIFICATION_POLICY_ACCESS);
                        })
                .setNegativeButton(R.string.btn_cancel,
                        (DialogInterface dialog, int which) -> {
                            // Begin, Lenovo, guzy2, IKSWN-73799, show dialog of requesting "Do Not Disturb" permission
                            startLiveActivity();
                            // End, Lenovo, guzy2, IKSWN-73799
                        })
                .show();
    }

    // Begin, Lenovo, guzy2, IKSWN-73799, show dialog of requesting "Do Not Disturb" permission
    private void startLiveActivity() {
        Intent intent = new Intent(this, LiveDynamicActivity.class);
        intent.putExtras(getIntent());
        startActivity(intent);
        finish();
    }
    // End, Lenovo, guzy2, IKSWN-73799
}
