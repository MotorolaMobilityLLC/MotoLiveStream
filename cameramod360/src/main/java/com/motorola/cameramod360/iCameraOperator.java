package com.motorola.cameramod360;

public interface iCameraOperator {

    void setCameraId(int camId);
    int getCameraId();

    void startCamera();
    void stopCamera();
    void releaseCamera();

    int[] setPreviewResolution(int width, int height);

    void setPreviewCallback(iPreviewCallback iPreviewCallback);

    void setPreviewOrientation(int orientation);

    void enableEncoding();
    void disableEncoding();

}
