package com.motorola.livestream.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

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

    private CallbackManager mCallbackManager;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            handleFbLoginProcedure();
        }
    };

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

            handleFbLoginProcedure();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onGetPermissionsSuccess() {
        Log.d(TAG, "Permission granted");
        Intent intent = new Intent(this, LiveDynamicActivity.class);
        intent.putExtras(getIntent());
        startActivity(intent);
        finish();
    }

    @Override
    protected void onGetPermissionsFailure() {
        finish();
    }

    private void handleFbLoginProcedure() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken == null || accessToken.isExpired()
                || !accessToken.getPermissions().contains(FbPermission.READ_CUSTOM_FRIEND_LIST)) {
            loginToFacebook(FbPermission.READ_CUSTOM_FRIEND_LIST);
        } else if (!accessToken.getPermissions().contains(FbPermission.PUBLISH_ACTION)) {
            loginToFacebook(FbPermission.PUBLISH_ACTION);
        } else {
            checkAppPermissionGranted();
            if (isPermissionGranted()) {
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
                showCancelDialog(permission);
            }

            @Override
            public void onError(FacebookException error) {
                Log.w(TAG, "Login failed for: " + permission);
                error.printStackTrace();
                showErrorDialog();
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

}
