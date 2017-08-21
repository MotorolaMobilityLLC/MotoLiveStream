/*
 * Copyright (C) 2013-2016 Motorola Mobility LLC,
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 */

package com.motorola.cameramod360.ui.v3.widgets.gl;

import android.opengl.GLES20;
import android.opengl.GLU;
import android.util.Log;

import com.motorola.cameramod360.Util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class GlToolBox {
    private static final String TAG = GlToolBox.class.getSimpleName();
    public static final int FLOAT_SIZE_BYTES = 4;
    public static final int SHORT_SIZE_BYTES = 2;

    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error + " - " + GLU.gluErrorString(error));
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    @SuppressWarnings("unused")
    public static float[] mapTouchToWorldCoords(float rx, float ry, float rz,
                                                float[] modelViewMatrix,
                                                float[] projMatrix,
                                                int[] viewPort) {
        ry = viewPort[3] - ry;
        float[] xyzw = {0, 0, 0, 0};
        android.opengl.GLU.gluUnProject(rx, ry, rz,
                modelViewMatrix, 0,
                projMatrix, 0,
                viewPort, 0,
                xyzw, 0);
        xyzw[0] /= xyzw[3];
        xyzw[1] /= xyzw[3];
        xyzw[2] /= xyzw[3];
        xyzw[3] = 1;
        return xyzw;
    }

    public static int generateTexture(int type) {
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        if (Util.DEBUG) checkGlError("glGenTextures");
        GLES20.glBindTexture(type, ids[0]);
        if (Util.DEBUG) GlToolBox.checkGlError("glBindTexture");
        GlToolBox.setDefaultTexParams();
        return ids[0];
    }

    public static int[] generateTextures(int count, int type) {
        int[] ids = new int[count];
        GLES20.glGenTextures(count, ids, 0);
        if (Util.DEBUG) checkGlError("glGenTextures");
        for (int id : ids) {
            GLES20.glBindTexture(type, id);
            if (Util.DEBUG) GlToolBox.checkGlError("glBindTexture");
            GlToolBox.setDefaultTexParams();
        }
        return ids;
    }


    public static int generateFbo() {
        int[] ids = new int[1];
        GLES20.glGenFramebuffers(1, ids, 0);
        if (Util.DEBUG) checkGlError("generateFbo");
        return ids[0];
    }

    public static int generateVbo() {
        int[] ids = new int[1];
        GLES20.glGenBuffers(1, ids, 0);
        if (Util.DEBUG) checkGlError("generateVbo");
        return ids[0];
    }

    public static void deleteTexture(int id) {
        int[] ids = new int[]{id};
        GLES20.glDeleteTextures(1, ids, 0);
        if (Util.DEBUG) checkGlError("deleteTexture");
    }

    public static void deleteTextures(int[] ids) {
        GLES20.glDeleteTextures(1, ids, 0);
        if (Util.DEBUG) checkGlError("deleteTexture");
    }

    public static void deleteFbo(int id) {
        int[] ids = new int[]{id};
        GLES20.glDeleteFramebuffers(1, ids, 0);
        if (Util.DEBUG) checkGlError("deleteFbo");
    }

    public static void deleteVbo(int id) {
        int[] ids = new int[]{id};
        GLES20.glDeleteBuffers(1, ids, 0);
        if (Util.DEBUG) checkGlError("deleteVbo");
    }

    public static int generateRenderBuffer(int width, int height) {
        int[] depthRb = new int[1];
        GLES20.glGenRenderbuffers(1, depthRb, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRb[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                width, height);
        GlToolBox.checkGlError("generateRenderBuffer");
        return depthRb[0];
    }

    public static void deleteRenderBuffer(int id) {
        int[] ids = new int[]{id};
        GLES20.glDeleteRenderbuffers(1, ids, 0);
        if (Util.DEBUG) checkGlError("deleteRenderBuffer");
    }

    public static void setVboData(int vboId, FloatBuffer data) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
                data.capacity() * FLOAT_SIZE_BYTES, data, GLES20.GL_STATIC_DRAW);
        if (Util.DEBUG) checkGlError("setVboData");
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public static void setVboFloats(int vboId, float[] data) {
        int len = data.length * FLOAT_SIZE_BYTES;
        FloatBuffer buffer = ByteBuffer.allocateDirect(len)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(data).position(0);
        setVboData(vboId, buffer);
    }

    public static void setIboData(int vboId, ShortBuffer data) {
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, vboId);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER,
                data.capacity() * SHORT_SIZE_BYTES, data, GLES20.GL_STATIC_DRAW);
        if (Util.DEBUG) checkGlError("setBoData");
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public static void setIboShorts(int vboId, short[] data) {
        int len = data.length * SHORT_SIZE_BYTES;
        ShortBuffer buffer = ByteBuffer.allocateDirect(len)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        buffer.put(data).position(0);
        setIboData(vboId, buffer);
    }


    public static void setDefaultTexParams() {
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        // Clamp to edge is the only option
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        if (Util.DEBUG) GlToolBox.checkGlError("setDefaultTexParams");
    }
}
