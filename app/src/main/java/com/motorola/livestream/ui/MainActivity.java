package com.motorola.livestream.ui;

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
import com.motorola.livestream.util.FbPermission;
import com.motorola.livestream.util.Log;

import java.util.Arrays;

public class MainActivity extends AbstractPermissionActivity {

    private static final String TAG = "Login";

    private CallbackManager mCallbackManager;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            handleFbLoginProcedure();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCallbackManager = CallbackManager.Factory.create();

        handleFbLoginProcedure();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onGetPermissionsSuccess() {
        Log.d(TAG, "Permission granted");
        startActivity(new Intent(this, LiveDynamicActivity.class));
        finish();
    }

    @Override
    protected void onGetPermissionsFailure() {
        finish();
    }

    private void handleFbLoginProcedure() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken == null || accessToken.isExpired()
                || !accessToken.getPermissions().contains(FbPermission.READ_CUSTOM_FRIENDLIST)) {
            loginToFacebook(FbPermission.READ_CUSTOM_FRIENDLIST);
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
            LoginManager.getInstance().logInWithPublishPermissions(this, Arrays.asList(permission));
            LoginManager.getInstance().setDefaultAudience(DefaultAudience.FRIENDS);
        } else {
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList(permission));
        }
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "Login onSuccess: " + loginResult.toString());
                mHandler.sendEmptyMessage(0);
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "Login onCancel");
                // Toast.makeText(MainActivity.this, "Login canceled", Toast.LENGTH_SHORT).show();
                MainActivity.this.finish();
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "Login onError: " + error.toString());
                // Toast.makeText(MainActivity.this, "Login error", Toast.LENGTH_SHORT).show();
                MainActivity.this.finish();
            }
        });
    }

}
