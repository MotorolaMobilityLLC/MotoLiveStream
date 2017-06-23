package com.motorola.livestream.ui.privacy;

import android.content.Intent;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.motorola.livestream.util.FbPermission;

import java.util.Collections;

public class FriendListActivity extends AppCompatActivity {

    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //fbLogin();
        showFriendListFragment();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fbLogin() {
        mCallbackManager = CallbackManager.Factory.create();

        if (hasPermission()) {
            showFriendListFragment();
            return;
        }

        LoginManager.getInstance().logInWithReadPermissions(this,
                Collections.singletonList(FbPermission.READ_CUSTOM_FRIEND_LIST));
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("Facebook", "Login onSuccess");
                showFriendListFragment();
            }

            @Override
            public void onCancel() {
                Log.d("Facebook", "Login onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d("Facebook", "Login onError: " + error.toString());
            }
        });
    }
    private boolean hasPermission() {
        return AccessToken.getCurrentAccessToken()
                .getPermissions().contains(FbPermission.READ_CUSTOM_FRIEND_LIST);
    }

    private void showFriendListFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(android.R.id.content, FriendListFragment.newInstance())
                .commit();
    }
}
