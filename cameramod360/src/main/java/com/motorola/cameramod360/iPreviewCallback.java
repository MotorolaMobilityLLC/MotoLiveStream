package com.motorola.cameramod360;

public interface iPreviewCallback {
    void onGetRgbaFrame(byte[] data, int width, int height);
}
