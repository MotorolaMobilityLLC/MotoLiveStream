/*
 * Copyright (C) 2013-2017 Motorola Mobility LLC,
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 */

package com.motorola.cameramod360.gl;

import android.opengl.GLES20;
import android.util.Log;

import com.motorola.cameramod360.Util;
import com.motorola.cameramod360.ui.v3.widgets.gl.GlToolBox;

public class OffScreenFrameBuffer {

    private static final String TAG = OffScreenFrameBuffer.class.getSimpleName();

    private int mRenderTex, mDepthRb, mFb;
    private int mWidth, mHeight;
    private boolean mIsLoaded = false;

    public OffScreenFrameBuffer() {
    }

    private synchronized void loadBuffer() {
        if (mWidth == 0 || mHeight == 0) return;
        createTexture();
        createDepthBuffer();
        createFrameBuffer();
        mIsLoaded = true;
    }

    private void releaseBuffer() {
        if (mRenderTex == 0) return;
        GlToolBox.deleteFbo(mFb);
        GlToolBox.deleteTexture(mRenderTex);
        GlToolBox.deleteRenderBuffer(mDepthRb);
        mFb = 0;
        mDepthRb = 0;
        mRenderTex = 0;
        mIsLoaded = false;
    }

    public void resizeBuffer(int width, int height) {
        if (mIsLoaded && width == mWidth && height == mHeight) return;
        if (Util.DEBUG) Log.d(TAG, "resizing FBO " + this);

        mWidth = width;
        mHeight = height;

        if (mIsLoaded) releaseBuffer();
        loadBuffer();
    }

    private void createTexture() {
        GLES20.glActiveTexture(Constants.TEXTURE_OFFSCREEN);
        mRenderTex = GlToolBox.generateTexture(GLES20.GL_TEXTURE_2D);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth, mHeight, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        if (Util.DEBUG) GlToolBox.checkGlError("glTexParameter mRenderTex in " + TAG);
    }

    private void createDepthBuffer() {
        int[] depthRb = new int[1];

        GLES20.glGenRenderbuffers(1, depthRb, 0);
        mDepthRb = depthRb[0];
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthRb);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                mWidth, mHeight);
        if (Util.DEBUG) GlToolBox.checkGlError("Creating depth buffer in " + TAG);
    }

    private void createFrameBuffer() {
        mFb = GlToolBox.generateFbo();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFb);
        if (Util.DEBUG) GlToolBox.checkGlError("Binding frame buffer in " + TAG);

        // attach the texture to FBO color attachment point
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mRenderTex, 0);
        if (Util.DEBUG) GlToolBox.checkGlError("Attaching texture in " + TAG);

        // attach the depth buffer to depth attachment point
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, mDepthRb);
        if (Util.DEBUG) GlToolBox.checkGlError("Attaching depth buffer in " + TAG);

        // check FBO status
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "Failed to create FBO: " + status);
        } else {
            if (Util.DEBUG) Log.d(TAG, "Successfully created FBO {" + hashCode() + "}");
        }

        // switch back to window-system-provided framebuffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public int getFrameBuffer() {
        return mFb;
    }
}
