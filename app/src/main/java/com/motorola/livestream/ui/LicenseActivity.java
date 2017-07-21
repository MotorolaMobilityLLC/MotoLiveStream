/**
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: SetupUtility.java
 * Description: Utility class for Setup.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Apr 16,2013    nmr874       Created file
 **********************************************************
 */

package com.motorola.livestream.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.motorola.livestream.R;

/*
 * This activity is to show the open source license in WebView
 */

public class LicenseActivity extends AppCompatActivity {

    private static final String LICENCES_FILE = "file:///android_res/raw/licenses.html";

    private WebView webview;

    public void onCreate(Bundle savedInstanceState) {

        setTheme(android.R.style.Theme_Material_Light_NoActionBar);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.license_settings);

        setupToolbar(R.string.licenses_title);

        webview = (WebView) findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setSupportZoom(true);
        webview.getSettings().setBuiltInZoomControls(true);
        webview.getSettings().setEnableSmoothTransition(true);
        webview.getSettings().setLoadWithOverviewMode(true);
        webview.getSettings().setAppCacheEnabled(true);

        webview.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            public void onPageFinished(WebView view, String url) {

            }

            public void onReceivedError(WebView view, int errorCode,
                    String description, String failingUrl) {

            }

        });

        webview.loadUrl(LICENCES_FILE);

    }

    /**
     * Add back action on Toolbar navigation
     * @return true
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Sets up the Toolbar (for Moto V2).
     * @param titleResId
     *            id of the string for the header.
     */
    protected final void setupToolbar(final int titleResId) {
        getWindow()
                .addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(titleResId);

            if (titleResId != -1) {
                actionBar.setTitle(titleResId);
            }
        }

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) toolbar
                .getLayoutParams();
        layoutParams.height += getStatusBarHeight();
        toolbar.setLayoutParams(layoutParams);

        toolbar.setPadding(0, getStatusBarHeight(), 0, 0);
        toolbar.setBackground(getDrawable(R.drawable.header));
    }

    /**
     * Gets the height of the status bar reading its property from Android.
     * @return height of status bar in pixels.
     */
    private int getStatusBarHeight() {
        int result = 0;
        final Resources resources = getResources();
        final int resourceId = resources.getIdentifier("status_bar_height",
                "dimen", "android");
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId);
        }
        return result;
    }

}
