package com.motorola.livestream.ui.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.motorola.livestream.R;
import com.motorola.livestream.util.ClosableThread;

public class LiveCountingTimer extends LinearLayout {

    private static final long TIME_COUNTER_INTERVAL = 500L;

    private String mTimerPreStr;
    private String mTimeStr;
    private TextView mLiveTime;
    private LiveCountingThread mCountingThread;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            setTime(msg.arg1);
        }
    };

    public LiveCountingTimer(Context context, AttributeSet attrs) {
        super(context, attrs);
        View view = View.inflate(context, R.layout.live_tick_timer, this);
        mLiveTime = (TextView) view.findViewById(R.id.live_states);
        mTimerPreStr = context.getString(R.string.live_label_count_timer);
    }

    private void setTime(int timeInSeconds) {
        mTimeStr = DateUtils.formatElapsedTime(timeInSeconds);
        mLiveTime.setText(String.format(mTimerPreStr, mTimeStr));
    }

    public String getTimeStr() {
        return mTimeStr;
    }

    public void startCounting() {
        if (mCountingThread == null) {
            mCountingThread = new LiveCountingThread();
            mCountingThread.nRecordingTimeMillis = 0L;
            mCountingThread.start();
        } else {
            setTime(0);
            mCountingThread.nRecordingTimeMillis = 0L;
        }
    }

    public void resumeCounting() {
        if (mCountingThread != null) {
            mCountingThread.resumeRecorder();
        }
    }

    public void pauseCounting() {
        if (mCountingThread != null) {
            mCountingThread.pauseRecorder();
        }
    }

    public void stopCounting() {
        if (mCountingThread != null) {
            mCountingThread.close();
            mCountingThread = null;
        }
    }

    private class LiveCountingThread extends ClosableThread {

        private boolean bIsPaused = false;
        private final Object mLock = new Object();
        private long nRecordingTimeMillis;

        public void resumeRecorder() {
            bIsPaused = false;
            synchronized (mLock) {
                try {
                    mLock.notify();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void pauseRecorder() {
            bIsPaused = true;
        }

        @Override
        public void run() {
            while (bIsRunning) {
                synchronized (mLock) {
                    if (bIsPaused) {
                        try {
                            mLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                nRecordingTimeMillis += TIME_COUNTER_INTERVAL;
                mHandler.obtainMessage(0,
                        (int) (nRecordingTimeMillis / 1000L), 0).sendToTarget();
                try {
                    Thread.sleep(TIME_COUNTER_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
