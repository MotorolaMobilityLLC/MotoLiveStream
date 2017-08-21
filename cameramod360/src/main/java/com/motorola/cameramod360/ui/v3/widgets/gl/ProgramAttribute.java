/*
 * Copyright (C) 2013-2016 Motorola Mobility LLC,
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 */

package com.motorola.cameramod360.ui.v3.widgets.gl;

import android.opengl.GLES20;

import com.motorola.cameramod360.Util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

public class ProgramAttribute {

    public enum AttributeKey {
        POSITION("a_Position"),
        TEXTURE_COORD("a_TextureCoord"),
        COLOR("a_Color"),
        Y_TEXCOORD("a_Y_texcoord"),
        VU_TEXCOORD("a_VU_texcoord"),
        PIXCOORD("a_Pixcoord");

        private final String mName;
        private static Map<String, AttributeKey> mLookupMap = new HashMap<>();

        static {
            for (AttributeKey key : AttributeKey.values()) mLookupMap.put(key.getName(), key);
        }

        AttributeKey(String name) {
            mName = name;
        }

        public String getName() {
            return mName;
        }

        public static AttributeKey getKey(String name) {
            return mLookupMap.get(name);
        }
    }

    private final static int FLOAT_SIZE = 4;

    private String mName;
    private int mIndex;
    private boolean mShouldNormalize;
    private int mOffset;
    private int mStride;
    private int mComponents;
    private int mType;
    private int mVbo;
    private int mLength;
    private FloatBuffer mValues;

    public ProgramAttribute(String name, int index) {
        mName = name;
        mIndex = index;
        mLength = -1;
    }

    public void set(int components, int type, boolean normalize, int stride, float[] values) {
        mShouldNormalize = normalize;
        mStride = stride;
        mComponents = components;
        mType = type;
        mVbo = 0;
        if (mLength != values.length) {
            initBuffer(values);
            mLength = values.length;
        }
        copyValues(values);
    }

    public void set(int components, int type, boolean normalize, int stride, int offset, int vbo) {
        mShouldNormalize = normalize;
        mOffset = offset;
        mStride = stride;
        mComponents = components;
        mType = type;
        mVbo = vbo;
        mValues = null;
        mLength = 0;
    }

    public void set(int components, int stride, float[] values) {
        set(components, GLES20.GL_FLOAT, false, stride, values);
    }

    public void set(int components, int stride, int offset, int vbo) {
        set(components, GLES20.GL_FLOAT, false, stride, offset, vbo);
    }

    /*package*/ boolean push() {
        if (mValues != null) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            GLES20.glVertexAttribPointer(mIndex,
                    mComponents,
                    mType,
                    mShouldNormalize,
                    mStride,
                    mValues);
        } else {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
            GLES20.glVertexAttribPointer(mIndex,
                    mComponents,
                    mType,
                    mShouldNormalize,
                    mStride,
                    mOffset);
        }
        GLES20.glEnableVertexAttribArray(mIndex);
        if (Util.DEBUG) GlToolBox.checkGlError("Set vertex-attribute " + mName + " values");
        return true;
    }

    @Override
    public String toString() {
        return mName;
    }

    private void initBuffer(float[] values) {
        mValues = ByteBuffer.allocateDirect(values.length * FLOAT_SIZE)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    private void copyValues(float[] values) {
        mValues.put(values).position(0);
    }
}
