package com.mo.rtmp;

import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.SocketException;

/**
 * Created by leo.ma on 2016/11/3.
 */

public class RtmpHandler extends Handler {
    private static final int MSG_ANNEXB_NOT_MATCH_EXCEPTION = 1;

    private WeakReference<RtmpListener> mWeakListener;

    public RtmpHandler(RtmpListener listener) {
        mWeakListener = new WeakReference<>(listener);
    }

    public void annexbNotMatchException(IllegalArgumentException e) {
        obtainMessage(MSG_ANNEXB_NOT_MATCH_EXCEPTION, e).sendToTarget();
    }


    @Override  // runs on UI thread
    public void handleMessage(Message msg) {
        RtmpListener listener = mWeakListener.get();
        if (listener == null) {
            return;
        }

        switch (msg.what) {
            case MSG_ANNEXB_NOT_MATCH_EXCEPTION:
                listener.onAnnexbNotMatchException((IllegalArgumentException) msg.obj);
                break;
            default:
                throw new RuntimeException("other exception");
        }
    }

    public interface RtmpListener {
        void onAnnexbNotMatchException(IllegalArgumentException e);

    }
}
