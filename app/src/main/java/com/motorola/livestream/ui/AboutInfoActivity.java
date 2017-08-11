/**
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 */

package com.motorola.livestream.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.motorola.livestream.R;
import com.motorola.livestream.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * This activity is to show the open source license in WebView
 */

public class AboutInfoActivity extends AppCompatActivity {
    private static final String LOG_TAG = "AboutInfoActivity";
    public List<String> licenseArray;
    public void onCreate(Bundle savedInstanceState) {

        setTheme(android.R.style.Theme_Material_Light_NoActionBar);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        setupToolbar(R.string.about_title);

        ListView list = (ListView) findViewById(R.id.InfoList);

        if(getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            Log.d(LOG_TAG, "AboutInfoActivity RTL");
        } else {
            Log.d(LOG_TAG, "AboutInfoActivity LTR");
        }
        SimpleAdapter adapter = new SimpleAdapter(this,
                getAboutInfo(),
                R.layout.about_info_item,
                new String[] {"ItemTitle", "ItemText"},
                new int[] {R.id.title,R.id.detail});

        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView)parent;
                HashMap<String, String> map = (HashMap<String, String>) listView.getItemAtPosition(position);
                String title = map.get("ItemTitle");
                if(title.equals(getResources().getString(R.string.licenses_title))) {
                    Intent intent = new Intent(AboutInfoActivity.this, LicenseActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    private ArrayList<HashMap<String, String>> getAboutInfo(){
        PackageManager pm = this.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(this.getPackageName(), 0);
            String version = pi.versionName;
            ArrayList<HashMap<String, String>> infoList = new ArrayList<HashMap<String, String>>();

            HashMap<String, String> map = new HashMap<String, String>();
            map.put("ItemTitle", getResources().getString(R.string.app_name_title));
            map.put("ItemText", getResources().getString(R.string.app_name));
            infoList.add(map);
            map = new HashMap<String, String>();
            map.put("ItemTitle", getResources().getString(R.string.app_version_title));
            map.put("ItemText", version);
            infoList.add(map);
            map = new HashMap<String, String>();
            map.put("ItemTitle", getResources().getString(R.string.licenses_title));
            map.put("ItemText", getResources().getString(R.string.licenses_detail));
            infoList.add(map);

            return infoList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
