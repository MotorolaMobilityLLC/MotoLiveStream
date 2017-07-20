package net.ossrs.yasea;

import java.io.IOException;
import java.net.SocketException;

public interface SrsPublishListener {
    void onRtmpConnecting(String msg);

    void onRtmpConnected(String msg);

    void onRtmpVideoStreaming();

    void onRtmpAudioStreaming();

    void onRtmpStopped();

    void onRtmpDisconnected();

    void onRtmpVideoFpsChanged(double fps);

    void onRtmpVideoBitrateChanged(double bitrate);

    void onRtmpAudioBitrateChanged(double bitrate);

    void onRtmpSocketException(SocketException e);

    void onRtmpIOException(IOException e);

    void onRtmpIllegalArgumentException(IllegalArgumentException e);

    void onRtmpIllegalStateException(IllegalStateException e);
}
