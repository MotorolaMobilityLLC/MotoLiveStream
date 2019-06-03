package com.motorola.livestream.ui;

import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;

import com.motorola.livestream.R;

public class LiveDynamicActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_live_dynamic);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.live_main_container, LiveMainFragment.newInstance())
                .commit();
    }

    @Override
    public void onBackPressed() {
        LiveMainFragment liveMainFragment =
                (LiveMainFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.live_main_container);
        if (liveMainFragment != null && liveMainFragment.isDuringLive()) {
            // Begin, Lenovo, guzy2, IKSWN-71983, Stop live when user pressed Back key
            liveMainFragment.stopLiveByBack();
            // End, Lenovo, guzy2, IKSWN-71983
        } else {
            finish();
        }
    }
}
