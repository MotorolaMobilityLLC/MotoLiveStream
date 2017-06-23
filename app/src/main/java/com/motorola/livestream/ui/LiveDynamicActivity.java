package com.motorola.livestream.ui;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.motorola.livestream.R;
import com.motorola.livestream.util.FbPermission;

import java.util.Collections;

public class LiveDynamicActivity extends AppCompatActivity {

    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_live_dynamic);

        // response screen rotation event
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.live_main_container, LiveMainFragment.newInstance())
                .commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mCallbackManager != null) {
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    public boolean checkIfNeedToRequestPermission() {
        if (hasPublishPermission()) {
            return true;
        }

        loginToFacebook();
        return false;
    }

    private void loginToFacebook() {
        mCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().logInWithPublishPermissions(this,
                Collections.singletonList(FbPermission.PUBLISH_ACTION));
        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        onFbPermissionGranted();
                    }

                    @Override
                    public void onCancel() {
                        Log.d("Facebook", "Login onCancel");
                        onFbPermissionCanceled();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.d("Facebook", "Login onError: " + error.toString());
                    }
                });
    }

    private boolean hasPublishPermission() {
        return AccessToken.getCurrentAccessToken().getPermissions().contains(FbPermission.PUBLISH_ACTION);
    }

    private void onFbPermissionGranted() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.live_main_container);
        if (fragment != null
                && fragment instanceof LiveMainFragment) {
            ((LiveMainFragment) fragment).startGoLive();
        }
    }

    private void onFbPermissionCanceled() {
        Toast.makeText(this, "You should grant the permission to post a live video",
                Toast.LENGTH_SHORT).show();
    }
}
