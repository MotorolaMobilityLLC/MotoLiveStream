/*
 * Copyright (C) 2013-2017 Motorola Mobility LLC,
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 */

package com.motorola.cameramod360;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Size;

import com.motorola.cameramod360.gl.SphereViewRenderer;
import com.motorola.cameramod360.gl.SphereViewRenderer.ViewType;
import com.motorola.cameramod360.gl.SphereViewState;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SphereCameraView extends SphereMediaView implements iCameraOperator {
    private static final String TAG = SphereCameraView.class.getSimpleName();

    private boolean mIsPrepared = false;

    private int mPreviewWidth;
    private int mPreviewHeight;
    private float mInputAspectRatio;

    private Camera mCamera;
    private int mCamId = -1;
    private ByteBuffer mGLPreviewBuffer;
    private int mPreviewOrientation = Configuration.ORIENTATION_PORTRAIT;
    private int mPreviewRotation = 90;

    private boolean mIsEncoding;
    private Thread mEncodingWorker;
    private final Object writeLock = new Object();
    private ConcurrentLinkedQueue<IntBuffer> mGLIntBufferCache = new ConcurrentLinkedQueue<>();
    private iPreviewCallback mPrevCb;

    public SphereCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initSphereView(int camId) {
        if (camId >= 0 && camId < 2) {
            initSphereView(new SphereViewRenderer(), camId, null);
        } else {
            initSphereView(new SphereViewRenderer(ViewType.SPHERICAL), camId, null);
        }
    }

    @Override
    public void initSphereView(SphereViewRenderer renderer, int mediaId, SphereViewState sphereViewState) {
        setCameraId(mediaId);

        super.initSphereView(renderer, mediaId, sphereViewState);
    }

    @Override
    public Size getMediaSize() {
        Size mediaSize;
        switch (mCamId) {
            case 2:
                mediaSize = new Size(2160, 1080);
                setPreviewResolution(2160, 1080);
                break;
            default:
                mediaSize = getScreenSize();
                int[] newSize = setPreviewResolution(mediaSize.getHeight(), mediaSize.getWidth());
                if (mediaSize.getWidth() != newSize[0]
                        || mediaSize.getHeight() != newSize[1]) {
                    mediaSize = new Size(newSize[0], newSize[1]);
                }
                break;
        }
        return mediaSize;
    }

    @Override
    public void prepareMedia() {
        mRenderer.setViewport(getWidth(), getHeight());
    }

    @Override
    public void attachMedia() {
        // For camera preview on activity creation
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(mRenderer.getSurfaceTexture());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    @Override
    public void detachMedia() {
        stopCamera();
    }

    @Override
    public void releaseResources() {
        stopCamera();

        mIsPrepared = false;
        super.releaseResources();
    }

    @Override
    public void onDraw(float[] viewMatrix, float[] projMatrix) {
        if (mIsEncoding) {
            mRenderer.drawToOffScreenBuffer();
            mGLIntBufferCache.add(mRenderer.getOffscreenBuffer());

            synchronized (writeLock) {
                writeLock.notifyAll();
            }
        }
    }

    @Override
    public void setViewType(ViewType viewType, boolean force) {
        if (viewType != mRenderer.getViewType() || force) {
            mRenderer.setViewType(viewType);
        }
    }

    @Override
    public void setCameraId(int camId) {
        mCamId = camId;
        setPreviewOrientation(mPreviewOrientation);
    }

    @Override
    public int getCameraId() {
        return mCamId;
    }

    @Override
    public void startCamera() {
        if (mCamera == null) {
            mCamera = openCamera();
            if (mCamera == null) {
                return;
            }
        }

        Camera.Parameters params = mCamera.getParameters();
        if (mCamId < 2) {
            List<Camera.Size> sizes = params.getSupportedPictureSizes();
            params.setPictureSize(sizes.get(0).width, sizes.get(0).height);
            for (Camera.Size size : sizes) {
                if (size.width == mPreviewWidth && size.height == mPreviewHeight) {
                    params.setPictureSize(mPreviewWidth, mPreviewHeight);
                    break;
                }
            }
            // only set the preview size to what the current camera supports
            Camera.Size rs = adaptPreviewResolution(mCamera.new Size(mPreviewWidth, mPreviewHeight));
            params.setPreviewSize(rs.width, rs.height);

            int[] range = adaptFpsRange(16, params.getSupportedPreviewFpsRange());
            params.setPreviewFpsRange(range[0], range[1]);
            params.setPreviewFormat(ImageFormat.NV21);
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);

            List<String> supportedFocusModes = params.getSupportedFocusModes();
            if (supportedFocusModes != null && !supportedFocusModes.isEmpty()) {
                if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    mCamera.autoFocus(null);
                } else {
                    params.setFocusMode(supportedFocusModes.get(0));
                }
            }
        } else if (mCamId == 2) {
            params.set("mot-app", "true");
        }
        mCamera.setParameters(params);
        if (mCamId < 2) {
            mCamera.setDisplayOrientation(mPreviewRotation);
        }

        try {
            mCamera.setPreviewTexture(mRenderer.getSurfaceTexture());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    @Override
    public void stopCamera() {
        disableEncoding();

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public int[] setPreviewResolution(int width, int height) {
        if (mCamera == null) {
            mCamera = openCamera();
        }
        mPreviewWidth = width;
        mPreviewHeight = height;
        if (mCamId != 2) {
            Camera.Size rs = adaptPreviewResolution(mCamera.new Size(width, height));
            if (rs != null) {
                mPreviewWidth = rs.width;
                mPreviewHeight = rs.height;
            }
        }
        mCamera.getParameters().setPreviewSize(mPreviewWidth, mPreviewHeight);

        mGLPreviewBuffer = ByteBuffer.allocateDirect(mPreviewWidth * mPreviewHeight * 4);
        mInputAspectRatio = mPreviewWidth > mPreviewHeight ?
                (float) mPreviewWidth / mPreviewHeight : (float) mPreviewHeight / mPreviewWidth;

        return new int[] { mPreviewWidth, mPreviewHeight };
    }

    @Override
    public void setPreviewCallback(iPreviewCallback callback) {
        mPrevCb = callback;
    }

    @Override
    public void enableEncoding() {
        mEncodingWorker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    while (!mGLIntBufferCache.isEmpty()) {
                        IntBuffer picture = mGLIntBufferCache.poll();
                        if (picture != null && mGLPreviewBuffer != null) {
                            mGLPreviewBuffer.asIntBuffer().put(picture.array());
                            mPrevCb.onGetRgbaFrame(mGLPreviewBuffer.array(),
                                    mPreviewWidth, mPreviewHeight);
                        }
                    }
                    // Waiting for next frame
                    synchronized (writeLock) {
                        try {
                            // isEmpty() may take some time, so we set timeout to detect next frame
                            writeLock.wait(500);
                        } catch (InterruptedException ie) {
                            mEncodingWorker.interrupt();
                        }
                    }
                }
            }
        });
        mEncodingWorker.start();
        mIsEncoding = true;
    }

    @Override
    public void disableEncoding() {
        mIsEncoding = false;
        mGLIntBufferCache.clear();

        if (mEncodingWorker != null) {
            mEncodingWorker.interrupt();
            try {
                mEncodingWorker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                mEncodingWorker.interrupt();
            }
            mEncodingWorker = null;
        }
    }

    private Camera openCamera() {
        Camera camera;
        int numCameras;
        Camera.CameraInfo info = new Camera.CameraInfo();
        numCameras = Camera.getNumberOfCameras();
        if (mCamId < 0) {
            for (int i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    mCamId = i;
                }
            }

            if (numCameras > 2) {
                mCamId = 2;
                Class[] paramInt = new Class[2];
                paramInt[0] = Integer.TYPE;
                paramInt[1] = Integer.TYPE;

                try {
                    Class cls = Class.forName("android.hardware.Camera");
                    Method method = cls.getDeclaredMethod("openLegacy", paramInt);
                    camera = (Camera) method.invoke(null, mCamId, 0x100);
                } catch (NoSuchMethodException | IllegalAccessException |
                        InvocationTargetException | ClassNotFoundException e) {
                    camera = Camera.open(mCamId);
                }
            } else {
                camera = Camera.open(mCamId);
            }
        } else {
            if (mCamId == 2) {
                Class[] paramInt = new Class[2];
                paramInt[0] = Integer.TYPE;
                paramInt[1] = Integer.TYPE;

                try {
                    Class cls = Class.forName("android.hardware.Camera");
                    Method method = cls.getDeclaredMethod("openLegacy", paramInt);
                    camera = (Camera) method.invoke(null, mCamId, 0x100);
                } catch (NoSuchMethodException | IllegalAccessException |
                        InvocationTargetException | ClassNotFoundException e) {
                    camera = Camera.open(mCamId);
                }
            } else {
                camera = Camera.open(mCamId);
            }
        }
        return camera;
    }

    private int[] adaptFpsRange(int expectedFps, List<int[]> fpsRanges) {
        expectedFps *= 1000;
        int[] closestRange = fpsRanges.get(0);
        int measure = Math.abs(closestRange[0] - expectedFps) + Math.abs(closestRange[1] - expectedFps);
        for (int[] range : fpsRanges) {
            if (range[0] <= expectedFps && range[1] >= expectedFps) {
                int curMeasure = Math.abs(range[0] - expectedFps) + Math.abs(range[1] - expectedFps);
                if (curMeasure < measure) {
                    closestRange = range;
                    measure = curMeasure;
                }
            }
        }
        return closestRange;
    }

    private Camera.Size adaptPreviewResolution(Camera.Size resolution) {
        float diff = 100f;
        float xdy = (float) resolution.width / (float) resolution.height;
        Camera.Size best = null;
        for (Camera.Size size : mCamera.getParameters().getSupportedPreviewSizes()) {
            if (size.equals(resolution)) {
                return size;
            }
            float tmp = Math.abs(((float) size.width / (float) size.height) - xdy);
            if (tmp < diff) {
                diff = tmp;
                best = size;
            }
        }
        return best;
    }

    public void setPreviewOrientation(int orientation) {
        mPreviewOrientation = orientation;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCamId, info);
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mPreviewRotation = info.orientation % 360;
                mPreviewRotation = (360 - mPreviewRotation) % 360;  // compensate the mirror
            } else {
                mPreviewRotation = (info.orientation + 360) % 360;
            }
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mPreviewRotation = (info.orientation + 90) % 360;
                mPreviewRotation = (360 - mPreviewRotation) % 360;  // compensate the mirror
            } else {
                mPreviewRotation = (info.orientation + 270) % 360;
            }
        }
    }

}