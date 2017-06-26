package com.mo.rtmp;

import android.util.Log;

public class RtmpPublisher {
    private static final String TAG = "RtmpPublisher";
    public final char TYPE_AUDIO = 8;
    public final char TYPE_VIDEO = 9;
    public final char TYPE_SCRIPT = 18;
    
    public int startRtmp(String url) {
        Log.d(TAG, "startRtmp");
        int moRtmpCreate_value = moRtmpCreate(url);
        Log.d(TAG, "moRtmpCreate_value = " + moRtmpCreate_value);
        return moRtmpCreate_value;
    }

    public void sendRtmpData(char type, int timestamp, byte[] data, int size) {
        Log.d(TAG, "sendRtmpData");
        int moRtmpWritePackage_value = moRtmpWritePackage(type,timestamp,data,size);
        Log.d(TAG, "moRtmpWritePackage_value = " + moRtmpWritePackage_value);
    }

    private native int moRtmpCreate(String url);
    private native int moRtmpWritePackage(char type, int timestamp, byte[] data, int size);

    static {
        System.loadLibrary("mortmp");
    }
}
