package com.motorola.gl.viewfinder;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.motorola.gl.viewfinder.ViewfinderFactory.ViewfinderType;
import com.motorola.gl.utils.OpenGLUtils;

import net.ossrs.yasea.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;

public class DefaultViewfinder {

    private static final int NO_TEXTURE = -1;
    private static final int NOT_INIT = -1;

    protected static final int FLOAT_SIZE_BYTES = 4;

    protected static final int VERTEX_POSITION_SIZE = 3;
    protected static final int VERTEX_TEXCOORD_SIZE = 2;
    protected static final int VERTEX_POSITION_STRIDE_BYTES =
            VERTEX_POSITION_SIZE * FLOAT_SIZE_BYTES;
    protected static final int VERTEX_TEXCOORD_STRIDE_BYTES =
            VERTEX_TEXCOORD_SIZE * FLOAT_SIZE_BYTES;
    protected static final int VERTEX_LENGTH_PER_SEGMENT = 4;

    private boolean mIsInitialized;
    protected Context mContext;
    private ViewfinderFactory.ViewfinderType mType = ViewfinderFactory.ViewfinderType.NONE;
    private final LinkedList<Runnable> mRunOnDraw;
    private final int mVertexShaderId;
    private final int mFragmentShaderId;

    private int mProgramHandle;
    private int mPositionIndex;
    private int mTextureCoordIndex;
    private int mSTMatrixIndex;
    private int mTextureIndex;

    protected int mInputWidth;
    protected int mInputHeight;
    protected int mOutputWidth;
    protected int mOutputHeight;
    protected FloatBuffer mGLCubeBuffer;
    protected FloatBuffer mGLTextureBuffer;

    private int[] mGLCubeId;
    private int[] mGLTextureCoordinateId;
    private float[] mGLTextureTransformMatrix;
    private int mVboSegmentCount = 0;

    private int[] mGLFboId;
    private int[] mGLFboTexId;
    private IntBuffer mGLFboBuffer;

    public DefaultViewfinder() {
        this(ViewfinderFactory.ViewfinderType.NONE);
    }

    public DefaultViewfinder(ViewfinderType type) {
        this(type, R.raw.vertex, R.raw.fragment);
    }

    public DefaultViewfinder(ViewfinderType type, int vertexShaderId, int fragmentShaderId) {
        mType = type;
        mRunOnDraw = new LinkedList<>();
        mVertexShaderId = vertexShaderId;
        mFragmentShaderId = fragmentShaderId;
    }

    public void init(Context context) {
        mContext = context;
        onInit();
        onInitialized();
    }

    protected void onInit() {
        initVbo();
        loadSamplerShader();
    }

    protected void onInitialized() {
        mIsInitialized = true;
    }

    public final void destroy() {
        mIsInitialized = false;
        destroyFboTexture();
        destoryVbo();
        GLES20.glDeleteProgram(mProgramHandle);
        onDestroy();
    }

    protected void onDestroy() {
    }

    public void onInputSizeChanged(final int width, final int height) {
        mInputWidth = width;
        mInputHeight = height;
        initFboTexture(width, height);
    }

    public void onDisplaySizeChanged(final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;
    }

    protected void loadSamplerShader() {
        mProgramHandle = OpenGLUtils.createProgram(getContext(),
                mVertexShaderId, mFragmentShaderId);
        mPositionIndex = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mTextureCoordIndex = GLES20.glGetAttribLocation(mProgramHandle,"a_TextureCoord");
        mSTMatrixIndex = GLES20.glGetUniformLocation(mProgramHandle, "u_STMatrix");
        mTextureIndex = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
    }

    private void initVbo() {
        final float VEX_CUBE[] = getVertexData();
        final float TEX_COORD[] = getTextureCoordData();

        mVboSegmentCount = VEX_CUBE.length / VERTEX_LENGTH_PER_SEGMENT;

        mGLCubeBuffer = ByteBuffer.allocateDirect(VEX_CUBE.length * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLCubeBuffer.put(VEX_CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEX_COORD.length * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mGLTextureBuffer.put(TEX_COORD).position(0);

        mGLCubeId = new int[1];
        mGLTextureCoordinateId = new int[1];

        GLES20.glGenBuffers(1, mGLCubeId, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGLCubeId[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mGLCubeBuffer.capacity() * FLOAT_SIZE_BYTES,
                mGLCubeBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glGenBuffers(1, mGLTextureCoordinateId, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGLTextureCoordinateId[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mGLTextureBuffer.capacity() * FLOAT_SIZE_BYTES,
                mGLTextureBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    private void destoryVbo() {
        if (mGLCubeId != null) {
            GLES20.glDeleteBuffers(1, mGLCubeId, 0);
            mGLCubeId = null;
        }
        if (mGLTextureCoordinateId != null) {
            GLES20.glDeleteBuffers(1, mGLTextureCoordinateId, 0);
            mGLTextureCoordinateId = null;
        }
    }

    private void initFboTexture(int width, int height) {
        if (mGLFboId != null && (mInputWidth != width || mInputHeight != height)) {
            destroyFboTexture();
        }

        mGLFboId = new int[1];
        mGLFboTexId = new int[1];
        mGLFboBuffer = IntBuffer.allocate(width * height);

        GLES20.glGenFramebuffers(1, mGLFboId, 0);
        GLES20.glGenTextures(1, mGLFboTexId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mGLFboTexId[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mGLFboId[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mGLFboTexId[0], 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void destroyFboTexture() {
        if (mGLFboTexId != null) {
            GLES20.glDeleteTextures(1, mGLFboTexId, 0);
            mGLFboTexId = null;
        }
        if (mGLFboId != null) {
            GLES20.glDeleteFramebuffers(1, mGLFboId, 0);
            mGLFboId = null;
        }
    }

    public int onDrawFrame(int cameraTextureId) {
        if (!mIsInitialized) {
            return NOT_INIT;
        }

        if (mGLFboId == null) {
            return NO_TEXTURE;
        }

        GLES20.glUseProgram(mProgramHandle);
        runPendingOnDrawTasks();

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGLCubeId[0]);
        GLES20.glEnableVertexAttribArray(mPositionIndex);
        GLES20.glVertexAttribPointer(mPositionIndex, VERTEX_POSITION_SIZE, GLES20.GL_FLOAT,
                false, VERTEX_POSITION_STRIDE_BYTES, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mGLTextureCoordinateId[0]);
        GLES20.glEnableVertexAttribArray(mTextureCoordIndex);
        GLES20.glVertexAttribPointer(mTextureCoordIndex, VERTEX_TEXCOORD_SIZE, GLES20.GL_FLOAT,
                false, VERTEX_TEXCOORD_STRIDE_BYTES, 0);

        GLES20.glUniformMatrix4fv(mSTMatrixIndex, 1, false, mGLTextureTransformMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);
        GLES20.glUniform1i(mTextureIndex, 0);

        onDrawArraysPre();

        GLES20.glViewport(0, 0, mInputWidth, mInputHeight);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mGLFboId[0]);
        for (int i = 0; i < mVboSegmentCount; i++) {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, i * VERTEX_LENGTH_PER_SEGMENT, VERTEX_LENGTH_PER_SEGMENT);
        }
        GLES20.glReadPixels(0, 0, mInputWidth, mInputHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mGLFboBuffer);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);

        for (int i = 0; i < mVboSegmentCount; i++) {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, i * VERTEX_LENGTH_PER_SEGMENT, VERTEX_LENGTH_PER_SEGMENT);
        }

        onDrawArraysAfter();

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        GLES20.glDisableVertexAttribArray(mPositionIndex);
        GLES20.glDisableVertexAttribArray(mTextureCoordIndex);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        return mGLFboTexId[0];
    }

    protected void onDrawArraysPre() {}

    protected void onDrawArraysAfter() {}

    private void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }

    public IntBuffer getGLFboBuffer() {
        return mGLFboBuffer;
    }

    protected Context getContext() {
        return mContext;
    }

    protected ViewfinderType getFilterType() {
        return mType;
    }

    public void setTextureTransformMatrix(float[] mtx){
        mGLTextureTransformMatrix = mtx;
    }

    protected float[] getVertexData() {
        return new float[] {
                -1f, -1f, 0f, // Bottom left.
                1f, -1f, 0f, // Bottom right.
                -1f, 1f, 0f, // Top left.
                1f, 1f, 0f, // Top right.
        };
    }

    protected float[] getTextureCoordData() {
        return new float[] {
                0f, 0f, // Bottom left.
                1f, 0f, // Bottom right.
                0f, 1f, // Top left.
                1f, 1f, // Top right.
        };
    }
}

