/*
 * Copyright (C) 2013-2017 Motorola Mobility LLC,
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 */

package com.motorola.cameramod360.gl;

import android.animation.ValueAnimator;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Size;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;

import com.motorola.cameramod360.Util;
import com.motorola.cameramod360.ui.v3.widgets.gl.GlToolBox;
import com.motorola.cameramod360.ui.v3.widgets.gl.ProgramAttribute;
import com.motorola.cameramod360.ui.v3.widgets.gl.ProgramUniform;
import com.motorola.cameramod360.ui.v3.widgets.gl.Shader;
import com.motorola.cameramod360.ui.v3.widgets.gl.ShaderFactory;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

public class SphereViewRenderer implements GLSurfaceView.Renderer,
        SurfaceTexture.OnFrameAvailableListener {

    private static final int SPHERE_DEPTH = 5;
    private static final int SPHERE_RADIUS = 1;
    private ValueAnimator flingAnimation;
    private ValueAnimator zoomAnimation;

    public interface SurfaceListener {
        void onSurfaceCreated();

        void onSurfaceChanged(int width, int height);

        void onDraw(float[] viewMatrix, float[] projMatrix);
    }

    public interface RotationListener {
        void onRotationChanged(float yaw, float pitch, float roll);
    }

    public enum ViewType {
        DEFAULT(0),
        SPHERICAL(1),
        SPLITSCREEN(2);

        private final int mId;

        ViewType(int id) {
            mId = id;
        }

        public int getId() {
            return mId;
        }

        public static ViewType getNext(ViewType currentView) {
            if (currentView.equals(DEFAULT)) {
                // DO not switch view type under DEFAULT view
                return currentView;
            } else if (currentView.equals(SPLITSCREEN)) {
                return SPHERICAL;
            } else {
                return SPLITSCREEN;
            }
        }
    }

    private static final float EPSILON = 0.1f;

    private static final float ZOOM_MIN = 1f;
    private static final float ZOOM_CENTER = 2.5f;
    private static final float ZOOM_MAX = 4f;
    private static final float ZOOM_OUT = 1.55f;
    private static final float ZOOM_MIN_Z = 1.9f;
    private static final float ZOOM_MIN_FOV = 100f;
    private static final float ZOOM_MAX_FOV = 50f;

    private static final long ZOOM_ANIM_DURATION = 300L;
    private static final long FLING_ANIM_DURATION = 500L;

    private static final float FLING_FACTOR = 0.025f;

    private static final float VIEW_EYE_Z = 20f;

    private static final float DEFAULT_TILT_ANGLE = 0f;
    private static final float DEFAULT_ROTATION_ANGLE = 90f;
    private static final float DEFAULT_ROLL_ANGLE = 0f;

    private static final int FLOAT_SIZE = 4;
    private static final int FLOATS_PER_VERTEX = 3;
    private static final int FLOATS_PER_TEX = 2;
    private static final int STRIDE = FLOATS_PER_VERTEX + FLOATS_PER_TEX;
    private static final int STRIDE_BYTES = FLOAT_SIZE * STRIDE;
    private static final int VBO_POSITION_OFFSET = 0;

    private static final float[] INITIAL_VERTICES_DATA = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 0f,
            1.0f, -1.0f, 0, 1.f, 0f,
            -1.0f, 1.0f, 0, 0.f, 1.f,
            1.0f, 1.0f, 0, 1.f, 1.f,
    };

    private static final float[] SPLIT_SCREEN_VERTICES_DATA = {
            // X, Y, Z, U, V
            // Center 50% of source drawn to top of preview
            -1f, 0f, 0f, 0.25f, 0f,
            1f, 00f, 0f, 0.75f, 0f,
            -1f, 1f, 0f, 0.25f, 1f,
            1f, 1f, 0f, 0.75f, 1f,
            // Right 25% portion of source drawn to bottom left of preview
            -1f, -1f, 0f, 0.75f, 0f,
            0f, -1f, 0f, 1f, 0f,
            -1f, -0f, 0f, 0.75f, 1f,
            0f, -0f, 0f, 1f, 1f,
            // Left 25% portion of source drawn to bottom right of preview
            0f, -1f, 0f, 0f, 0f,
            1f, -1f, 0f, 0.25f, 0f,
            0f, -0f, 0f, 0f, 1f,
            1f, -0f, 0f, 0.25f, 1f,
    };

    private int mTextureId;
    private SurfaceTexture mSurfaceTexture;

    private int mTotalNumStrips;
    private int mVerticesPerStrip;

    private int mDefaultVbo;
    private int mSphereVbo;
    private int mVbo;
    private Shader mShader;
    private boolean mIsFrameAvailable = false;
    private float mAlpha = 1f;
    private boolean mUpdateVbo = false;

    protected float[] mMvpMatrix = new float[16];
    protected float[] mModelMatrix = new float[16];
    protected float[] mViewMatrix = new float[16];
    protected float[] mProjectionMatrix = new float[16];
    protected float[] mTextureMatrix = new float[16];

    protected float[] mSphereMatrix = new float[16];
    protected float[] mRotationMatrix = new float[16];
    protected float[] mTiltMatrix = new float[16];

    // Default model, view, projection matrices to avoid using the spherical
    protected float[] mDefaultModelMatrix = new float[16];
    protected float[] mDefaultViewMatrix = new float[16];
    protected float[] mDefaultProjectionMatrix = new float[16];

    private OffScreenFrameBuffer mOffScreenFrameBuffer;
    private IntBuffer mOffScreenByteBuffer;

    private float[] mDefaultVerticesData = new float[20];
    private float[] mSplitScreenVerticesData = new float[60];

    private float mAspectRatio;

    private Size mMediaSize = new Size(0, 0);
    private Size mViewportSize = new Size(0, 0);
    private Size mViewSize = new Size(0, 0);

    private SphereViewState mCurViewState = new SphereViewState(
            DEFAULT_ROTATION_ANGLE, DEFAULT_TILT_ANGLE, DEFAULT_ROLL_ANGLE,
            ZOOM_CENTER, ViewType.DEFAULT);

    private boolean mIsCamera = true;

    private RotationListener mRotationListener = null;
    private SurfaceListener mSurfaceListener = null;

    public SphereViewRenderer() {
        this(ViewType.DEFAULT);
    }

    public SphereViewRenderer(ViewType viewType) {
        mCurViewState.setViewType(viewType);
    }

    public void setSurfaceListener(SurfaceListener listener) {
        mSurfaceListener = listener;
    }

    public void setRotationListener(RotationListener listener) {
        mRotationListener = listener;
    }

    public void setViewport(int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void setCameraFlag() {
        mIsCamera = true;
    }

    private void setupView() {
        switch (getViewType()) {
            case SPHERICAL:
                setAngles(mCurViewState.getRotationAngle(), mCurViewState.getTiltAngle());
                setZoom(getZoom());
                break;
//            case DEFAULT:
//                setImageScroll(mCurViewState.getPanningOffset());
//                break;
//            case SPLITSCREEN:
//                //setZoom(ZOOM_MIN);
//                setImageScroll(mCurViewState.getPanningOffset());
//                break;
            default:
                break;
        }
    }

    public void setupViewState(SphereViewState newState) {
        mCurViewState = newState;
        setupView();
    }

    public SphereViewState getCurrentViewState() {
        return mCurViewState;
    }

    public void release() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
    }

    private void cancelRunningAnimations() {
        if (zoomAnimation != null && zoomAnimation.isRunning()) {
            zoomAnimation.cancel();
        }
        if (flingAnimation != null && flingAnimation.isRunning()) {
            flingAnimation.cancel();
        }
    }

    public synchronized void onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
        // d(x,y) = lastP(x,y) - curP(x,y)
        // GL's ordinate is inverted from Android's

        cancelRunningAnimations();
        switch (mCurViewState.getViewType()) {
            case SPHERICAL:
                float rotationDelta = dx / 5f / getZoom();
                float tiltDelta = -dy / 5f / getZoom();
                setAngles(mCurViewState.getRotationAngle() + rotationDelta,
                        mCurViewState.getTiltAngle() + tiltDelta);
                break;
            case DEFAULT:
            case SPLITSCREEN:
                break;
        }
    }

    public void onScale(float scaleFactor) {
        cancelRunningAnimations();
        setZoom(getZoom() * scaleFactor);
    }

    public void setMediaSize(int width, int height) {
        mMediaSize = new Size(width, height);
    }

    public void setViewportSize(int width, int height) {
        mViewportSize = new Size(width, height);
        mAspectRatio = (float) width / (float) height;
    }

    protected synchronized void setAngles(float rotationAngle, float tiltAngle) {
        mCurViewState.setRotationAngle(rotationAngle);

        mCurViewState.setTiltAngle(Math.max(Math.min(60, tiltAngle), -60));
        Matrix.setRotateM(mTiltMatrix, 0, mCurViewState.getTiltAngle(), 1f, 0f, 0f);
        Matrix.setRotateM(mRotationMatrix, 0, mCurViewState.getRotationAngle(), 0f, 1f, 0f);

        Matrix.setIdentityM(mSphereMatrix, 0);
        Matrix.multiplyMM(mSphereMatrix, 0, mRotationMatrix, 0, mSphereMatrix, 0);
        Matrix.multiplyMM(mSphereMatrix, 0, mTiltMatrix, 0, mSphereMatrix, 0);

        // set the angles in ambisonic processor
        if (mRotationListener != null) {
            float yaw = mCurViewState.getRotationAngle();
            float pitch = mCurViewState.getTiltAngle();
            mRotationListener.onRotationChanged(yaw, pitch, 0); // roll is always 0
        }
    }

    public void doubleTapZoom() {
        if (isSplitScreenView()) {
            animateZoom(Math.abs(getZoom() - ZOOM_MIN) <= EPSILON ? ZOOM_CENTER : ZOOM_MIN);
        } else {
            animateZoom(Math.abs(getZoom() - ZOOM_OUT) <= EPSILON ? ZOOM_CENTER : ZOOM_OUT);
        }
    }

    public void animateFling(final float velocityX, final float velocityY) {
        cancelRunningAnimations();
        flingAnimation = ValueAnimator.ofFloat(0f, 1f);
        final float startingRotation = mCurViewState.getRotationAngle();
        final float startingTilt = mCurViewState.getTiltAngle();
        flingAnimation.setDuration(FLING_ANIM_DURATION);
        flingAnimation.setInterpolator(new DecelerateInterpolator());
        flingAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator anim) {
                float value = (float) anim.getAnimatedValue();
                float rotation = startingRotation - value
                        * FLING_FACTOR * velocityX / getZoom();
                float tilt = startingTilt + value
                        * FLING_FACTOR * velocityY / getZoom();
                setAngles(rotation, tilt);
            }
        });
        flingAnimation.start();
    }

    public void animateZoom(float finalZoom) {
        cancelRunningAnimations();
        zoomAnimation = ValueAnimator.ofFloat(getZoom(), finalZoom);
        zoomAnimation.setDuration(ZOOM_ANIM_DURATION);
        zoomAnimation.setInterpolator(new DecelerateInterpolator());
        zoomAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator anim) {
                float animatedZoom = (float) anim.getAnimatedValue();
                setZoom(animatedZoom);
            }
        });
        zoomAnimation.start();
    }

    public synchronized void setZoom(float zoom) {
        if (isSplitScreenView()) {
            mCurViewState.setSplitScreenZoom(Math.max(ZOOM_MIN, Math.min(zoom, ZOOM_MAX)));
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.scaleM(mModelMatrix, 0, getZoom(), getZoom(), 0);
            return;
        }

        mCurViewState.setZoom(Math.max(ZOOM_MIN, Math.min(zoom, ZOOM_MAX)));
        float fieldOfView = ZOOM_MIN_FOV;
        float zOffset = 0f;
        if (getZoom() > ZOOM_CENTER) {
            fieldOfView -= (ZOOM_MIN_FOV - ZOOM_MAX_FOV) *
                    (getZoom() - ZOOM_CENTER) / (ZOOM_MAX - ZOOM_CENTER);
        } else {
            zOffset += ZOOM_MIN_Z - ZOOM_MIN_Z *
                    (getZoom() - ZOOM_MIN) / (ZOOM_CENTER - ZOOM_MIN);
        }
        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, -zOffset, 0f, 0f, 1f, 0f, 1f, 0f);
        Matrix.perspectiveM(mProjectionMatrix, 0, fieldOfView, mAspectRatio, 0.1f, 10f);
    }

    public float getZoom() {
        if (isSplitScreenView()) {
            return mCurViewState.getSplitScreenZoom();
        }
        return mCurViewState.getZoom();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // reset vertices
        System.arraycopy(INITIAL_VERTICES_DATA, 0, mDefaultVerticesData,
                0, INITIAL_VERTICES_DATA.length);
        System.arraycopy(SPLIT_SCREEN_VERTICES_DATA, 0, mSplitScreenVerticesData,
                0, SPLIT_SCREEN_VERTICES_DATA.length);

        ShaderFactory.loadShaders();
        boolean invertX = false;
        boolean invertY = false;
        boolean swapAxis = false;

        Sphere sphere = new Sphere(SPHERE_DEPTH, SPHERE_RADIUS, invertX, invertY, swapAxis);
        mTotalNumStrips = sphere.getStripCount();
        mVerticesPerStrip = sphere.getVerticesPerStrip();
        mSphereVbo = GlToolBox.generateVbo();
        GlToolBox.setVboFloats(mSphereVbo, sphere.getVertices());

        mDefaultVbo = GlToolBox.generateVbo();
        GlToolBox.setVboFloats(mDefaultVbo, mDefaultVerticesData);

        mVbo = GlToolBox.generateVbo();
        GlToolBox.setVboFloats(mVbo, mSplitScreenVerticesData);

        setupView();
        prepareTexture();

        if (mSurfaceListener != null) {
            mSurfaceListener.onSurfaceCreated();
        }

        mOffScreenFrameBuffer = new OffScreenFrameBuffer();
    }

    private void prepareTexture() {
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);

        int targetTexture = mIsCamera ? GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D;
        GLES20.glActiveTexture(Constants.TEXTURE_MEDIA);
        GLES20.glBindTexture(targetTexture, ids[0]);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        mTextureId = ids[0];
        if (mIsCamera) {
            prepareSurface();
        }
    }

    private void prepareSurface() {
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);
    }

    public void setViewSize(int width, int height) {
        mViewSize = new Size(width, height);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        GLES20.glClearColor(0.f, 0.f, 0.f, 0.f);
        GLES20.glClearDepthf(1f);
        setViewportSize(width, height);
        setZoom(getZoom());
        if (mSurfaceListener != null) {
            mSurfaceListener.onSurfaceChanged(width, height);
        }
        prepareOffscreenByteBuffer();
    }

    @Override
    public synchronized void onDrawFrame(GL10 gl) {
        if (mIsCamera && !mIsFrameAvailable) {
            return;
        }

        // draw on screen
        GLES20.glViewport(0, 0, mViewportSize.getWidth(), mViewportSize.getHeight());

        switch (getViewType()) {
            case SPHERICAL:
                GLES20.glFrontFace(GLES20.GL_CCW);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                drawSphere(mSphereMatrix, mViewMatrix, mProjectionMatrix);
                break;
            case DEFAULT:
                prepareDefault();
                break;
            case SPLITSCREEN:
                prepareSplitScreen();
                break;
            default:
                break;
        }

        if (mSurfaceListener != null) {
            mSurfaceListener.onDraw(mTextureMatrix, mProjectionMatrix);
        }
    }

    public void drawToOffScreenBuffer() {
        // make offscreen frame buffer active
        mOffScreenFrameBuffer.resizeBuffer(mMediaSize.getWidth(), mMediaSize.getHeight());
        bindFrameBuffer(mOffScreenFrameBuffer.getFrameBuffer());

        // set the viewport to the correct size and clear it
        GLES20.glViewport(0, 0, mMediaSize.getWidth(), mMediaSize.getHeight());
//        GLES20.glFrontFace(GLES20.GL_CCW);
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Always draw the raw camera frame to OffScreenFrameBuffer
        prepareDefault();

        mOffScreenByteBuffer.rewind();
        GLES20.glReadPixels(0, 0, mMediaSize.getWidth(), mMediaSize.getHeight(),
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mOffScreenByteBuffer);
        bindFrameBuffer(0);
    }

    private void prepareOffscreenByteBuffer() {
        if (mOffScreenByteBuffer != null) {
            int expectedCapacity = mMediaSize.getWidth() * mMediaSize.getHeight();
            if (mOffScreenByteBuffer.capacity() != expectedCapacity) {
                mOffScreenByteBuffer.clear();
            } else {
                return;
            }
        }
        mOffScreenByteBuffer = IntBuffer.allocate(mMediaSize.getWidth() * mMediaSize.getHeight());
    }

    protected void prepareDefault() {
        float[] modelMatrix = mDefaultModelMatrix;
        float[] viewMatrix = mDefaultViewMatrix;
        float[] projectionMatrix = mDefaultProjectionMatrix;

        if (getViewType() == ViewType.DEFAULT) {
            // not writing to offscreen framebuffer
            modelMatrix = mModelMatrix;
            viewMatrix = mViewMatrix;
            projectionMatrix = mProjectionMatrix;
        }

        int viewWidth = mViewSize.getWidth();
        int viewHeight = mViewSize.getHeight();
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.orthoM(projectionMatrix, 0, -viewWidth / 2, viewWidth / 2,
                -viewHeight / 2, viewHeight / 2, -1, 1);
        Matrix.scaleM(modelMatrix, 0, viewWidth / 2f, viewHeight / 2f, 0);
        drawDefault(modelMatrix, viewMatrix, projectionMatrix);
    }

    protected void drawDefault(float[] modelMatrix, float[] viewMatrix, float[] projectionMatrix) {
        if (mIsCamera) {
            mShader = ShaderFactory.getShader(ShaderFactory.Shaders.CAMERA_PREVIEW);
        } else {
            mShader = ShaderFactory.getShader(ShaderFactory.Shaders.BITMAP_SPHERE);
        }
        mShader.use();

        if (mIsCamera) {
            GLES20.glActiveTexture(Constants.TEXTURE_MEDIA);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureId);
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(mTextureMatrix);
        } else {
            Matrix.setIdentityM(mTextureMatrix, 0);
        }

        if (mUpdateVbo) {
            mUpdateVbo = false;
            GlToolBox.setVboFloats(mDefaultVbo, mDefaultVerticesData);
        }

        ProgramAttribute programAttribute =
                mShader.getProgramAttribute(ProgramAttribute.AttributeKey.POSITION);

        programAttribute.set(FLOATS_PER_VERTEX, GLES20.GL_FLOAT, false,
                STRIDE_BYTES, VBO_POSITION_OFFSET, mDefaultVbo);

        programAttribute =
                mShader.getProgramAttribute(ProgramAttribute.AttributeKey.TEXTURE_COORD);

        programAttribute.set(FLOATS_PER_TEX, GLES20.GL_FLOAT, false,
                STRIDE_BYTES, FLOATS_PER_VERTEX * FLOAT_SIZE, mDefaultVbo);

        Matrix.setIdentityM(mMvpMatrix, 0);
        Matrix.multiplyMM(mMvpMatrix, 0, modelMatrix, 0, mMvpMatrix, 0);
        Matrix.multiplyMM(mMvpMatrix, 0, viewMatrix, 0, mMvpMatrix, 0);
        Matrix.multiplyMM(mMvpMatrix, 0, projectionMatrix, 0, mMvpMatrix, 0);

        mShader.setUniformValue(ProgramUniform.UniformKey.MVP_MATRIX, mMvpMatrix);
        mShader.setUniformValue(ProgramUniform.UniformKey.TEXTURE, 0);
        mShader.setUniformValue(ProgramUniform.UniformKey.ST_MATRIX, mTextureMatrix);
        mShader.setUniformValue(ProgramUniform.UniformKey.ALPHA, mAlpha);

        mShader.pushAttributes();

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        if (Util.DEBUG) GlToolBox.checkGlError("glDrawArrays");
    }

    protected void prepareSplitScreen() {
        int viewWidth = mViewSize.getWidth();
        int viewHeight = mViewSize.getHeight();
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.orthoM(mProjectionMatrix, 0, -viewWidth / 2, viewWidth / 2,
                -viewHeight / 2, viewHeight / 2, -1, 1);
        Matrix.scaleM(mModelMatrix, 0, viewWidth / 2f, viewHeight / 2f, 0);

        drawSplitScreen(mModelMatrix, mViewMatrix, mProjectionMatrix);
    }

    protected void drawSplitScreen(float[] modelMatrix, float[] viewMatrix, float[] projectionMatrix) {
        if (mIsCamera) {
            mShader = ShaderFactory.getShader(ShaderFactory.Shaders.CAMERA_PREVIEW);
        } else {
            mShader = ShaderFactory.getShader(ShaderFactory.Shaders.BITMAP_TINY_PLANET);
        }
        mShader.use();

        if (mIsCamera) {
            GLES20.glActiveTexture(Constants.TEXTURE_MEDIA);
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(mTextureMatrix);
        } else {
        }

        if (mUpdateVbo) {
            mUpdateVbo = false;
            GlToolBox.setVboFloats(mVbo, mSplitScreenVerticesData);
        }

        ProgramAttribute programAttribute =
                mShader.getProgramAttribute(ProgramAttribute.AttributeKey.POSITION);

        programAttribute.set(FLOATS_PER_VERTEX, GLES20.GL_FLOAT, false,
                STRIDE_BYTES, VBO_POSITION_OFFSET, mVbo);

        programAttribute =
                mShader.getProgramAttribute(ProgramAttribute.AttributeKey.TEXTURE_COORD);

        programAttribute.set(FLOATS_PER_TEX, GLES20.GL_FLOAT, false,
                STRIDE_BYTES, FLOATS_PER_VERTEX * FLOAT_SIZE, mVbo);

        Matrix.setIdentityM(mMvpMatrix, 0);
        Matrix.multiplyMM(mMvpMatrix, 0, modelMatrix, 0, mMvpMatrix, 0);
        Matrix.multiplyMM(mMvpMatrix, 0, viewMatrix, 0, mMvpMatrix, 0);
        Matrix.multiplyMM(mMvpMatrix, 0, projectionMatrix, 0, mMvpMatrix, 0);

        mShader.setUniformValue(ProgramUniform.UniformKey.MVP_MATRIX, mMvpMatrix);
        mShader.setUniformValue(ProgramUniform.UniformKey.TEXTURE, 0);
        mShader.setUniformValue(ProgramUniform.UniformKey.ST_MATRIX, mTextureMatrix);
        mShader.setUniformValue(ProgramUniform.UniformKey.ALPHA, mAlpha);

        mShader.pushAttributes();

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 4, 4);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 8, 4);

        if (Util.DEBUG) GlToolBox.checkGlError("glDrawArrays");
    }

    protected void drawSphere(float[] modelMatrix, float[] viewMatrix, float[] projectionMatrix) {
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LESS);

        if (mIsCamera) {
            mShader = ShaderFactory.getShader(ShaderFactory.Shaders.CAMERA_PREVIEW_SPHERE);
        } else {
            mShader = ShaderFactory.getShader(ShaderFactory.Shaders.BITMAP_SPHERE);
        }
        if (mShader == null) {
            return;
        }
        mShader.use();

        if (mIsCamera) {
            GLES20.glActiveTexture(Constants.TEXTURE_MEDIA);
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(mTextureMatrix);
        } else {
            Matrix.setIdentityM(mTextureMatrix, 0);
        }

        ProgramAttribute programAttribute =
                mShader.getProgramAttribute(ProgramAttribute.AttributeKey.POSITION);

        programAttribute.set(Sphere.VERTEX_POSITION_SIZE, GLES20.GL_FLOAT, false,
                Sphere.VERTEX_STRIDE_BYTES, Sphere.VERTEX_POSITION_OFFSET_BYTES, mSphereVbo);

        programAttribute =
                mShader.getProgramAttribute(ProgramAttribute.AttributeKey.TEXTURE_COORD);

        programAttribute.set(Sphere.VERTEX_TEXCOORD_SIZE, GLES20.GL_FLOAT, false,
                Sphere.VERTEX_STRIDE_BYTES, Sphere.VERTEX_TEXCOORD_OFFSET_BYTES, mSphereVbo);

        Matrix.setIdentityM(mMvpMatrix, 0);
        Matrix.multiplyMM(mMvpMatrix, 0, modelMatrix, 0, mMvpMatrix, 0);
        Matrix.multiplyMM(mMvpMatrix, 0, viewMatrix, 0, mMvpMatrix, 0);
        Matrix.multiplyMM(mMvpMatrix, 0, projectionMatrix, 0, mMvpMatrix, 0);

        mShader.setUniformValue(ProgramUniform.UniformKey.MVP_MATRIX, mMvpMatrix);
        mShader.setUniformValue(ProgramUniform.UniformKey.TEXTURE, 0);
        mShader.setUniformValue(ProgramUniform.UniformKey.ALPHA, mAlpha);
        mShader.setUniformValue(ProgramUniform.UniformKey.ST_MATRIX, mTextureMatrix);

        mShader.pushAttributes();

        for (int stripNum = 0; stripNum < mTotalNumStrips; stripNum++) {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,
                    mVerticesPerStrip * stripNum, mVerticesPerStrip);
            if (Util.DEBUG) GlToolBox.checkGlError("GlDrawArrays");
        }

        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        setLookAtMatrix();
        mIsFrameAvailable = true;
    }

    public void setLookAtMatrix() {
        if (isDefaultView() || isSplitScreenView()) {
            Matrix.setLookAtM(mViewMatrix, 0, 0, 0, VIEW_EYE_Z, 0f, 0f, 0f, 0f, 1.0f, 0f);
        }
    }

    public ViewType getViewType() {
        return mCurViewState.getViewType();
    }

    public void setViewType(ViewType viewType) {
        mCurViewState.setViewType(viewType);
        setupView();
    }

    public ViewType switchViewType() {
        setViewType(ViewType.getNext(mCurViewState.getViewType()));
        return mCurViewState.getViewType();
    }

    public boolean isDefaultView() {
        return ViewType.DEFAULT.equals(mCurViewState.getViewType());
    }

    public boolean isSphericalView() {
        return ViewType.SPHERICAL.equals(mCurViewState.getViewType());
    }

    public boolean isSplitScreenView() {
        return ViewType.SPLITSCREEN.equals(mCurViewState.getViewType());
    }

    protected void bindFrameBuffer(int fbId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbId);
        if (Util.DEBUG) GlToolBox.checkGlError("Error binding frame buffer in id " + fbId);
    }

    public IntBuffer getOffscreenBuffer() {
        return mOffScreenByteBuffer;
    }
}
