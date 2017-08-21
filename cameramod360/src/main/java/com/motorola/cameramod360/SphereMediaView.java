/*
 * Copyright (C) 2013-2017 Motorola Mobility LLC,
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 */

/* Copied from https://github.com/google/grafika/blob/master/src/com/android/grafika/gles/EglCore.java */

package com.motorola.cameramod360;

import android.content.Context;
import android.graphics.Point;
import android.opengl.EGL14;
import android.opengl.EGLExt;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;

import com.motorola.cameramod360.gl.SphereViewRenderer;
import com.motorola.cameramod360.gl.SphereViewRenderer.ViewType;
import com.motorola.cameramod360.gl.SphereViewState;
import com.motorola.cameramod360.ui.v3.widgets.gl.ShaderFactory;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

public abstract class SphereMediaView extends GLSurfaceView implements
        SphereViewRenderer.SurfaceListener {

    public interface OnHoldListener {
        boolean isOnHold();

        void onHold();

        void onRelease();
    }

    public interface OnGestureListener {
        void onDown(MotionEvent e);

        boolean onSingleTap(MotionEvent e);
    }

    public interface SphereMediaViewListener {
        void onSphereViewMediaPrepared(SphereViewRenderer renderer);

        void onSphereViewUpdated();
    }

    protected OnHoldListener mOnHoldListener;
    protected SphereMediaViewListener mSphereMediaViewListener;
    private OnGestureListener mOnGestureListener;

    private static final String TAG = SphereMediaView.class.getSimpleName();

    private static final int EGL_CLIENT_CONTEXT_VERSION = 2;
    private boolean mIsDoubleTapZoomEnabled;

    protected int mMediaId;
    protected SphereViewRenderer mRenderer;

    private boolean mInitialized = false;
    private boolean mIsSuspended = false;
    private GestureDetector mGestureDetector;

    private ScaleGestureDetector mScaleDetector;
    private boolean mScaleDelay = false;

    public SphereMediaView(Context context) {
        this(context, null);
    }

    public SphereMediaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIsDoubleTapZoomEnabled = true;
        setEGLContextClientVersion(EGL_CLIENT_CONTEXT_VERSION);
        setEGLConfigChooser(new RecordableConfigChooser(EGL_CLIENT_CONTEXT_VERSION, true));
        setEGLContextFactory(new ContextFactory());
        initGestureListeners();
    }

    abstract Size getMediaSize();

    /**
     * Gets media ready to render
     */
    abstract void prepareMedia();

    /**
     * Called when Surface is created or resumed
     */
    abstract void attachMedia();

    /**
     * Callend when Surface is paused or destroyed
     */
    abstract void detachMedia();

    public void initSphereView(SphereViewRenderer renderer, int mediaId,
                               SphereViewState sphereViewState) {
        setRenderer(renderer);

        mMediaId = mediaId;

        doPrepareMedia(sphereViewState);
    }

    @Override
    public void setRenderer(Renderer renderer) {
        if (!(renderer instanceof SphereViewRenderer)) {
            throw new IllegalArgumentException("Invalid renderer class");
        }
        mRenderer = (SphereViewRenderer) renderer;
        mRenderer.setSurfaceListener(this);
        super.setRenderer(mRenderer);
        mInitialized = true;
        mIsSuspended = false;
    }

    protected void doPrepareMedia(SphereViewState viewState) {
        Size mediaSize = getMediaSize();
        mRenderer.setMediaSize(mediaSize.getWidth(), mediaSize.getHeight());

        prepareMedia();

        if (viewState != null) {
            mRenderer.setupViewState(viewState);
        }

        if (mSphereMediaViewListener != null) {
            mSphereMediaViewListener.onSphereViewMediaPrepared(mRenderer);
        }
    }

    protected void initGestureListeners() {
        mGestureDetector = new GestureDetector(getContext(), gestureListener);
        mScaleDetector = new ScaleGestureDetector(getContext(), scaleListener);
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mOnHoldListener != null && mOnHoldListener.isOnHold() &&
                        event.getAction() == MotionEvent.ACTION_UP) {
                    mOnHoldListener.onRelease();
                    return true;
                }

                if (mRenderer.isSphericalView()) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mScaleDelay = event.getPointerCount() != 1;
                    }
                    if (event.getPointerCount() > 1) {
                        return mScaleDetector.onTouchEvent(event);
                    } else if (!mScaleDelay) {
                        return mGestureDetector.onTouchEvent(event);
                    }
                } else {
                    return mGestureDetector.onTouchEvent(event);
                }
                return true;
            }
        });
    }

    public void setOnHoldListener(OnHoldListener listener) {
        mOnHoldListener = listener;
    }

    public void setOnGestureListener(OnGestureListener l) {
        mOnGestureListener = l;
    }

    public void setSphereMediaViewListener(SphereMediaViewListener listener) {
        mSphereMediaViewListener = listener;
    }

    public void setDoubleTapZoomEnabled(boolean isEnabled) {
        mIsDoubleTapZoomEnabled = isEnabled;
    }

    public SphereViewState getCurrentViewState() {
        return mRenderer.getCurrentViewState();
    }

    public void suspend() {
        if (mInitialized) {
            onPause();
        }
        mIsSuspended = true;
    }

    public void releaseResources() {
        releaseRenderer();
        mInitialized = false;
    }

    private void releaseRenderer() {
        if (mRenderer != null) mRenderer.release();
    }

    public void onScroll(MotionEvent e1, MotionEvent e2, float x, float y) {
        mRenderer.onScroll(e1, e2, x, y);
    }

    public void onFling(float x, float y) {
        mRenderer.animateFling(x, y);
    }

    public void onScale(float scaleFactor) {
        mRenderer.onScale(scaleFactor);
    }

    public void suspendSphereView() {
        if (mInitialized) {
            detachMedia();
            onPause();
            releaseRenderer();
            mIsSuspended = true;
        }
    }

    public void resumeSphereView() {
        if (mInitialized && mIsSuspended) {
            onResume();
            mIsSuspended = false;
        }
    }

    public SphereViewRenderer getRenderer() {
        return mRenderer;
    }

    public ViewType switchViewType() {
        return mRenderer.switchViewType();
    }

    public void setViewType(ViewType viewType, boolean force) {
        mRenderer.setViewType(viewType);
    }

    @Override
    public void onSurfaceCreated() {
        attachMedia();
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        mRenderer.setViewSize(width, height);
        if (mSphereMediaViewListener != null) {
            mSphereMediaViewListener.onSphereViewUpdated();
        }
    }

    protected Size getScreenSize() {
        // Get the real screen size and set as preview resolution
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Point screenSize = new Point();
        wm.getDefaultDisplay().getRealSize(screenSize);
        return new Size(screenSize.x, screenSize.y);
    }

    private class ContextFactory implements EGLContextFactory {
        private final String CONTEXT_FACTORY_TAG = ContextFactory.class.getSimpleName();
        private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        double GLES_30 = 3.0;
        double GLES_20 = 2.0;

        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
            if (Util.DEBUG) Log.d(TAG, "createContext");
            EGLContext context = createContextPriv(egl, display, eglConfig, GLES_30);
            if (context == null) {
                context = createContextPriv(egl, display, eglConfig, GLES_20);
            }
            return context;
        }

        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
            if (Util.DEBUG) Log.d(TAG, "destroyContext");
            ShaderFactory.clearShaders();
            if (!egl.eglDestroyContext(display, context)) {
                Log.e(CONTEXT_FACTORY_TAG, "display:" + display + " mContext: " + context);
                throw new RuntimeException("eglDestroyContex:" + egl.eglGetError());
            }
        }

        private EGLContext createContextPriv(EGL10 egl, EGLDisplay display,
                                             EGLConfig eglConfig, double version) {
            Log.i(CONTEXT_FACTORY_TAG, "Creating OpenGL ES " + version + " mContext");
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, (int) version, EGL10.EGL_NONE};
            EGLContext context = egl.eglCreateContext(display, eglConfig,
                    EGL10.EGL_NO_CONTEXT, attrib_list);

            if (context != null) return context;

            if (Util.DEBUG)
                Log.d(CONTEXT_FACTORY_TAG, "Failed to create OpenGL ES" + version + " mContext");
            return null;
        }
    }

    private class RecordableConfigChooser implements EGLConfigChooser {

        private static final int EGL_RECORDABLE_ANDROID = 0x3142;

        private int[] mValue;
        private int mRedSize;
        private int mGreenSize;
        private int mBlueSize;
        private int mAlphaSize;
        private int mDepthSize;
        private int mStencilSize;

        private int[] mConfigSpec;

        public RecordableConfigChooser(int clientContextVersion, boolean isRecordable) {
            int renderableType;
            if (clientContextVersion == 2) {
                renderableType = EGL14.EGL_OPENGL_ES2_BIT;
            } else {
                renderableType = EGLExt.EGL_OPENGL_ES3_BIT_KHR;
            }

            // The actual surface is generally RGBA or RGBX, so situationally omitting alpha
            // doesn't really help.  It can also lead to a huge performance hit on glReadPixels()
            // when reading into a GL_RGBA buffer.
            mValue = new int[1];
            mRedSize = 8;
            mGreenSize = 8;
            mBlueSize = 8;
            mAlphaSize = 8;
            mDepthSize = 16;
            mStencilSize = 0;

            int[] configSpec = new int[]{
                    EGL14.EGL_RED_SIZE, mRedSize,
                    EGL14.EGL_GREEN_SIZE, mGreenSize,
                    EGL14.EGL_BLUE_SIZE, mBlueSize,
                    EGL14.EGL_ALPHA_SIZE, mAlphaSize,
                    EGL14.EGL_DEPTH_SIZE, mDepthSize,
                    EGL14.EGL_STENCIL_SIZE, mStencilSize,
                    EGL14.EGL_RENDERABLE_TYPE, renderableType,
                    EGL14.EGL_NONE, 0,      // placeholder for recordable [@-3]
                    EGL14.EGL_NONE
            };

            if (isRecordable) {
                configSpec[configSpec.length - 3] = EGL_RECORDABLE_ANDROID;
                configSpec[configSpec.length - 2] = 1;
            }
            mConfigSpec = configSpec;
        }

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[] num_config = new int[1];
            if (!egl.eglChooseConfig(display, mConfigSpec, null, 0,
                    num_config)) {
                throw new IllegalArgumentException("eglChooseConfig failed");
            }

            int numConfigs = num_config[0];

            if (numConfigs <= 0) {
                throw new IllegalArgumentException(
                        "No configs match configSpec");
            }

            EGLConfig[] configs = new EGLConfig[numConfigs];
            if (!egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs,
                    num_config)) {
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }
            EGLConfig config = chooseConfig(egl, display, configs);
            if (config == null) {
                throw new IllegalArgumentException("No config chosen");
            }
            return config;
        }

        private EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
            for (EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config,
                        EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config,
                        EGL10.EGL_STENCIL_SIZE, 0);
                if ((d >= mDepthSize) && (s >= mStencilSize)) {
                    int r = findConfigAttrib(egl, display, config,
                            EGL10.EGL_RED_SIZE, 0);
                    int g = findConfigAttrib(egl, display, config,
                            EGL10.EGL_GREEN_SIZE, 0);
                    int b = findConfigAttrib(egl, display, config,
                            EGL10.EGL_BLUE_SIZE, 0);
                    int a = findConfigAttrib(egl, display, config,
                            EGL10.EGL_ALPHA_SIZE, 0);
                    if ((r == mRedSize) && (g == mGreenSize)
                            && (b == mBlueSize) && (a == mAlphaSize)) {
                        return config;
                    }
                }
            }
            return null;
        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute,
                                     int defaultValue) {

            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                return mValue[0];
            }
            return defaultValue;
        }
    }

    // handle the gestures
    protected GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector
            .SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            if (mOnGestureListener != null) {
                mOnGestureListener.onDown(e);
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return (mOnGestureListener != null) && mOnGestureListener.onSingleTap(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mIsDoubleTapZoomEnabled) {
                mRenderer.doubleTapZoom();
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (mRenderer.isSphericalView()) {
                SphereMediaView.this.onScroll(e1, e2, distanceX, distanceY);
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            SphereMediaView.this.onFling(velocityX, velocityY);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (!mScaleDelay && mOnHoldListener != null) mOnHoldListener.onHold();
        }
    };

    protected ScaleGestureDetector.OnScaleGestureListener scaleListener = new ScaleGestureDetector
            .OnScaleGestureListener() {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (mOnHoldListener == null || !mOnHoldListener.isOnHold()) {
                SphereMediaView.this.onScale(detector.getScaleFactor());
            }
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mScaleDelay = true;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    };

}
