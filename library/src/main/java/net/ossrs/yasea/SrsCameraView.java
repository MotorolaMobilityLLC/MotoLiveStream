package net.ossrs.yasea;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;

import com.motorola.gl.viewfinder.DefaultViewfinder;
import com.motorola.gl.viewfinder.OffScreenViewfinder;
import com.motorola.gl.viewfinder.ViewfinderFactory;
import com.motorola.gl.viewfinder.ViewfinderFactory.ViewfinderType;
import com.motorola.gl.utils.OpenGLUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Leo Ma on 2016/2/25.
 */
public class SrsCameraView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private SurfaceTexture surfaceTexture;
    private int mOESTextureId = OpenGLUtils.NO_TEXTURE;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private boolean mIsEncoding;
    private boolean mIsTorchOn = false;
    private float mInputAspectRatio;
    private float mOutputAspectRatio;
    private float[] mProjectionMatrix = new float[16];
    private float[] mSurfaceMatrix = new float[16];
    private float[] mTransformMatrix = new float[16];

    private Camera mCamera;
    private ByteBuffer mGLPreviewBuffer;
    private int mCamId = -1;
    private int mPreviewRotation = 90;
    private int mPreviewOrientation = Configuration.ORIENTATION_PORTRAIT;

    private Thread worker;
    private final Object writeLock = new Object();
    private ConcurrentLinkedQueue<IntBuffer> mGLIntBufferCache = new ConcurrentLinkedQueue<>();
    private PreviewCallback mPrevCb;

    private DefaultViewfinder mPreviewViewfinder;
    private ViewfinderType mViewfinderType = ViewfinderType.NONE;
    private OffScreenViewfinder mOffScreenViewfinder = null;

    public SrsCameraView(Context context) {
        this(context, null);
    }

    public SrsCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glDisable(GL10.GL_DITHER);
        GLES20.glClearColor(0, 0, 0, 0);

        mPreviewViewfinder = ViewfinderFactory.initFilters(mViewfinderType);
        mPreviewViewfinder.init(getContext().getApplicationContext());
        mPreviewViewfinder.onInputSizeChanged(mPreviewWidth, mPreviewHeight);

        mOffScreenViewfinder = new OffScreenViewfinder();
        mOffScreenViewfinder.init(getContext().getApplicationContext());
        mOffScreenViewfinder.onInputSizeChanged(mPreviewWidth, mPreviewHeight);

        mOESTextureId = OpenGLUtils.getExternalOESTextureID();
        surfaceTexture = new SurfaceTexture(mOESTextureId);
        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                requestRender();
            }
        });

        // For camera preview on activity creation
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(surfaceTexture);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        mPreviewViewfinder.onDisplaySizeChanged(width, height);
        mOffScreenViewfinder.onDisplaySizeChanged(width, height);

        mOutputAspectRatio = width > height ? (float) width / height : (float) height / width;
        float aspectRatio = mOutputAspectRatio / mInputAspectRatio;
        if (width > height) {
            Matrix.orthoM(mProjectionMatrix, 0, -1.0f, 1.0f, -aspectRatio, aspectRatio, -1.0f, 1.0f);
        } else {
            Matrix.orthoM(mProjectionMatrix, 0, -aspectRatio, aspectRatio, -1.0f, 1.0f, -1.0f, 1.0f);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(mSurfaceMatrix);

        Matrix.multiplyMM(mTransformMatrix, 0, mSurfaceMatrix, 0, mProjectionMatrix, 0);
        if (mPreviewViewfinder != null) {
            mPreviewViewfinder.setTextureTransformMatrix(mTransformMatrix);
            mPreviewViewfinder.onDrawFrame(mOESTextureId);
        }
        if (mOffScreenViewfinder != null) {
            mOffScreenViewfinder.setTextureTransformMatrix(mTransformMatrix);
            mOffScreenViewfinder.onDrawFrame(mOESTextureId);
        }

        if (mIsEncoding) {
            //mGLIntBufferCache.add(mPreviewViewfinder.getGLFboBuffer());
            mGLIntBufferCache.add(mOffScreenViewfinder.getGLFboBuffer());
            synchronized (writeLock) {
                writeLock.notifyAll();
            }
        }
    }

    public void setPreviewCallback(PreviewCallback cb) {
        mPrevCb = cb;
    }

    public int[] setPreviewResolution(int width, int height) {
        getHolder().setFixedSize(width, height);

        mCamera = openCamera();
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

    public boolean setFilter(final ViewfinderType type) {
        if (mCamera == null) {
            return false;
        }

        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mViewfinderType != type) {
                    mViewfinderType = type;

                    if (mPreviewViewfinder != null) {
                        mPreviewViewfinder.destroy();
                    }

                    mPreviewViewfinder = ViewfinderFactory.initFilters(type);
                    if (mPreviewViewfinder != null) {
                        mPreviewViewfinder.init(getContext().getApplicationContext());
                        mPreviewViewfinder.onInputSizeChanged(mPreviewWidth, mPreviewHeight);
                        mPreviewViewfinder.onDisplaySizeChanged(mSurfaceWidth, mSurfaceHeight);
                    }

                    if (mOffScreenViewfinder != null) {
                        mOffScreenViewfinder.destroy();
                    }
                    mOffScreenViewfinder = new OffScreenViewfinder();
                    if (mOffScreenViewfinder != null) {
                        mOffScreenViewfinder.init(getContext().getApplicationContext());
                        mOffScreenViewfinder.onInputSizeChanged(mPreviewWidth, mPreviewHeight);
                        mOffScreenViewfinder.onDisplaySizeChanged(mSurfaceWidth, mSurfaceHeight);
                    }
                }
            }
        });
        requestRender();
        return true;
    }

    private void deleteTextures() {
        if (mOESTextureId != OpenGLUtils.NO_TEXTURE) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    GLES20.glDeleteTextures(1, new int[]{ mOESTextureId }, 0);
                    mOESTextureId = OpenGLUtils.NO_TEXTURE;
                }
            });
        }
    }

    public void setCameraId(int id) {
        mCamId = id;
        setPreviewOrientation(mPreviewOrientation);
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

    public int getCameraId() {
        return mCamId;
    }

    public void enableEncoding() {
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    while (!mGLIntBufferCache.isEmpty()) {
                        IntBuffer picture = mGLIntBufferCache.poll();
                        mGLPreviewBuffer.asIntBuffer().put(picture.array());
                        mPrevCb.onGetRgbaFrame(mGLPreviewBuffer.array(), mPreviewWidth, mPreviewHeight);
                    }
                    // Waiting for next frame
                    synchronized (writeLock) {
                        try {
                            // isEmpty() may take some time, so we set timeout to detect next frame
                            writeLock.wait(500);
                        } catch (InterruptedException ie) {
                            worker.interrupt();
                        }
                    }
                }
            }
        });
        worker.start();
        mIsEncoding = true;
    }

    public void disableEncoding() {
        mIsEncoding = false;
        mGLIntBufferCache.clear();

        if (worker != null) {
            worker.interrupt();
            try {
                worker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                worker.interrupt();
            }
            worker = null;
        }
    }

    public boolean startCamera() {
        if (mCamera == null) {
            mCamera = openCamera();
            if (mCamera == null) {
                return false;
            }
        }

        Camera.Parameters params = mCamera.getParameters();
        if (mCamId < 2) {
            params.setPictureSize(mPreviewWidth, mPreviewHeight);
            params.setPreviewSize(mPreviewWidth, mPreviewHeight);
            int[] range = adaptFpsRange(SrsEncoder.VFPS, params.getSupportedPreviewFpsRange());
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

            List<String> supportedFlashModes = params.getSupportedFlashModes();
            if (supportedFlashModes != null && !supportedFlashModes.isEmpty()) {
                if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                    if (mIsTorchOn) {
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    }
                } else {
                    params.setFlashMode(supportedFlashModes.get(0));
                }
            }
        } else if (mCamId == 2){
            params.set("mot-app", "true");
        }

        mCamera.setParameters(params);
        if (mCamId < 2) {
            mCamera.setDisplayOrientation(mPreviewRotation);
        }

        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();

        return true;
    }

    public void stopCamera() {
        disableEncoding();

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private Camera openCamera() {
//        Camera camera;
        int numCameras = 0;
        Camera.CameraInfo info = new Camera.CameraInfo();
        numCameras = Camera.getNumberOfCameras();
        if (mCamId < 0) {
            int frontCamId = -1;
            int backCamId = -1;
            int modCamId = -1;
            for (int i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    backCamId = i;
                } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    frontCamId = i;
                    mCamId = i;
//                    break;
                } else {
                    modCamId = i;
                }
            }
//            if (modCamId != -1) {
//                mCamId = modCamId;
//            }else
//            if (frontCamId != -1) {
//                mCamId = frontCamId;
//            } else if (backCamId != -1) {
//                mCamId = backCamId;
//            } else {
//                mCamId = 0;
//            }
        }
//        camera = Camera.open(mCamId);
        Camera camera = null;
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
        return camera;
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

    public interface PreviewCallback {

        void onGetRgbaFrame(byte[] data, int width, int height);
    }
}
