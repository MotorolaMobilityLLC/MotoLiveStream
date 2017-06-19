package com.motorola.livestream.ui.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.motorola.livestream.R;
import com.motorola.livestream.util.ClosableThread;

public class LiveCountingTimer extends LinearLayout {

    public static final long TIME_COUNTER_INTERVAL = 500L;

    private String mTimeStr;
    private TextView mLiveTime;
    private LiveCountingThread mCountingThread;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            setTime(msg.arg1);
        }
    };

    public LiveCountingTimer(Context context, AttributeSet attrs) {
        super(context, attrs);
        View view = View.inflate(context, R.layout.live_tick_timer, this);
        mLiveTime = (TextView) view.findViewById(R.id.live_states);
    }

    private String formatTime(int timeInSeconds) {
        timeInSeconds = Math.max(timeInSeconds, 0);
        int minute = timeInSeconds / 60;
        int hour = minute / 60;
        StringBuilder sb = new StringBuilder();
        if (hour > 0) {
            sb.append(formatTimePart(hour)).append(":");
        }
        sb.append(formatTimePart(minute % 60)).append(":");
        sb.append(formatTimePart(timeInSeconds % 60));
        return sb.toString();
    }

    private String formatTimePart(int timePart) {
        String timeStr = String.valueOf(timePart);
        if (timePart < 10) {
            StringBuilder sb = new StringBuilder();
            sb.append("0").append(timeStr);
            return sb.toString();
        }
        return timeStr;
    }

    private void setTime(int timeInSeconds) {
        mTimeStr = formatTime(timeInSeconds);
        mLiveTime.setText("LIVE ");
        mLiveTime.append(mTimeStr);
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
        private Object mLock = new Object();
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

        public boolean isPaused() {
            return bIsPaused;
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
