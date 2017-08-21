/*
 * Copyright (C) 2013-2016 Motorola Mobility LLC,
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 */

package com.motorola.cameramod360.ui.v3.widgets.gl;

import android.opengl.GLES20;

import com.motorola.cameramod360.Util;

import java.util.HashMap;
import java.util.Map;

public class ProgramUniform {

    public enum UniformKey {
        TEXTURE("u_Texture"),
        OPACITY("u_Opacity"),
        MVP_MATRIX("u_MVPMatrix"),
        ST_MATRIX("u_STMatrix"),
        ALPHA("u_Alpha"),
        ROLL("u_Roll"),
        COLOR("u_Color"),
        Y_TEXTURE("u_Y_texture"),
        UV_TEXTURE("u_UV_texture"),
        DIAGONAL("u_Diagonal"),
        INVERT("u_Invert"),
        FACTOR("u_Factor"),
        COLOR_FG("u_ColorForeground"),
        COLOR_BG("u_ColorBackground"),
        RADIUS("u_Radius"),
        RADII("u_Radii"),
        ANGLES("u_Angles"),
        MASK("u_Mask"),
        COLOR_CHANGE("u_ColorChange");

        private final String mName;
        private static Map<String, UniformKey> mLookupMap = new HashMap<>();

        static {
            for (UniformKey key : UniformKey.values()) mLookupMap.put(key.getName(), key);
        }

        UniformKey(String name) {
            mName = name;
        }

        public String getName() {
            return mName;
        }

        public static UniformKey getKey(String name) {
            return mLookupMap.get(name);
        }
    }

    private static final boolean LOCAL_LOGV = false;

    private String mName;
    private int mLocation;
    private int mType;
    private int mSize;

    public ProgramUniform(int program, int index) {
        int[] len = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_ACTIVE_UNIFORM_MAX_LENGTH, len, 0);

        int[] type = new int[1];
        int[] size = new int[1];
        byte[] name = new byte[len[0]];
        int[] ignore = new int[1];

        GLES20.glGetActiveUniform(program, index, len[0], ignore, 0, size, 0, type, 0, name, 0);
        mName = new String(name, 0, strlen(name));
        mLocation = GLES20.glGetUniformLocation(program, mName);
        mType = type[0];
        mSize = size[0];
        if (Util.DEBUG && LOCAL_LOGV) GlToolBox.checkGlError("Initializing uniform");
    }

    public String getName() {
        return mName;
    }

    public int getType() {
        return mType;
    }

    public int getLocation() {
        return mLocation;
    }

    public int getSize() {
        return mSize;
    }

    public void setUniformValue(int value) {
        GLES20.glUniform1i(mLocation, value);
        if (Util.DEBUG && LOCAL_LOGV) GlToolBox.checkGlError("Set uniform value (" + getName() + ")");
    }

    public void setUniformValue(float value) {
        GLES20.glUniform1f(mLocation, value);
        if (Util.DEBUG && LOCAL_LOGV) GlToolBox.checkGlError("Set uniform value (" + getName() + ")");
    }

    public void setUniformValue(int[] values) {
        int len = values.length;
        switch (getType()) {
            case GLES20.GL_INT:
                checkUniformAssignment(len, 1);
                GLES20.glUniform1iv(getLocation(), len, values, 0);
                break;
            case GLES20.GL_INT_VEC2:
                checkUniformAssignment(len, 2);
                GLES20.glUniform2iv(getLocation(), len / 2, values, 0);
                break;
            case GLES20.GL_INT_VEC3:
                checkUniformAssignment(len, 3);
                GLES20.glUniform2iv(getLocation(), len / 3, values, 0);
                break;
            case GLES20.GL_INT_VEC4:
                checkUniformAssignment(len, 4);
                GLES20.glUniform2iv(getLocation(), len / 4, values, 0);
                break;
            default:
                throw new RuntimeException("Cannot assign int-array to incompatible uniform type "
                        + "for uniform '" + getName() + "'!");
        }
        if (Util.DEBUG && LOCAL_LOGV) GlToolBox.checkGlError("Set uniform value (" + getName() + ")");
    }


    public void setUniformValue(float[] values) {
        int len = values.length;
        switch (getType()) {
            case GLES20.GL_FLOAT:
                checkUniformAssignment(len, 1);
                GLES20.glUniform1fv(getLocation(), len, values, 0);
                break;
            case GLES20.GL_FLOAT_VEC2:
                checkUniformAssignment(len, 2);
                GLES20.glUniform2fv(getLocation(), len / 2, values, 0);
                break;
            case GLES20.GL_FLOAT_VEC3:
                checkUniformAssignment(len, 3);
                GLES20.glUniform3fv(getLocation(), len / 3, values, 0);
                break;
            case GLES20.GL_FLOAT_VEC4:
                checkUniformAssignment(len, 4);
                GLES20.glUniform4fv(getLocation(), len / 4, values, 0);
                break;
            case GLES20.GL_FLOAT_MAT2:
                checkUniformAssignment(len, 4);
                GLES20.glUniformMatrix2fv(getLocation(), len / 4, false, values, 0);
                break;
            case GLES20.GL_FLOAT_MAT3:
                checkUniformAssignment(len, 9);
                GLES20.glUniformMatrix3fv(getLocation(), len / 9, false, values, 0);
                break;
            case GLES20.GL_FLOAT_MAT4:
                checkUniformAssignment(len, 16);
                GLES20.glUniformMatrix4fv(getLocation(), len / 16, false, values, 0);
                break;
            default:
                throw new RuntimeException("Cannot assign float-array to incompatible uniform type "
                        + "for uniform '" + getName() + "'!");
        }
        if (Util.DEBUG && LOCAL_LOGV) GlToolBox.checkGlError("Set uniform value (" + getName() + ")");
    }


    private void checkUniformAssignment(int values, int components) {
        if (values % components != 0) {
            throw new RuntimeException("Size mismatch: Attempting to assign values of size "
                    + values + " to uniform '" + getName() + "' (must be multiple of "
                    + components + ")!");
        } else if (mSize != values / components) {
            throw new RuntimeException("Size mismatch: Cannot assign " + values + " values to "
                    + "uniform '" + getName() + "'!");
        }
    }


    private static int strlen(byte[] strVal) {
        for (int i = 0; i < strVal.length; ++i) {
            if (strVal[i] == '\0') return i;
        }
        return strVal.length;
    }
}
