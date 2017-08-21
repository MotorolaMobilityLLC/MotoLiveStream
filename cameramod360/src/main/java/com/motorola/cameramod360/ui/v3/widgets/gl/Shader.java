/*
 * Copyright (C) 2013-2016 Motorola Mobility LLC,
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 */

package com.motorola.cameramod360.ui.v3.widgets.gl;

import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;

import com.motorola.cameramod360.CamMod360App;
import com.motorola.cameramod360.Util;
import com.motorola.cameramod360.ui.v3.widgets.gl.ProgramAttribute.AttributeKey;
import com.motorola.cameramod360.ui.v3.widgets.gl.ProgramUniform.UniformKey;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Shader {
    private static final String TAG = Shader.class.getSimpleName();
    private static final int NO_HANDLE = -1;
    private int mProgramHandle;
    private ProgramUniform[] mUniforms = new ProgramUniform[UniformKey.values().length];
    private ProgramAttribute[] mAttributes = new ProgramAttribute[AttributeKey.values().length];
    private static int mLastProgram = NO_HANDLE;

    /*package*/ Shader(String vertexSource, String fragSource) {
        mProgramHandle = createProgram(vertexSource, fragSource);
        scanUniforms();
    }

    /*package*/ Shader(int vertexSourceId, int fragSourceId) {
        mProgramHandle = createProgram(getShaderSource(vertexSourceId), getShaderSource(fragSourceId));
        scanUniforms();
    }

    public void use() {
        if (mLastProgram == -1 && mLastProgram == mProgramHandle) return;
        GLES20.glUseProgram(mProgramHandle);
        if (Util.DEBUG) GlToolBox.checkGlError("glUseProgram");
        mLastProgram = mProgramHandle;
    }

    public static void clearLastHandle() {
        mLastProgram = NO_HANDLE;
    }

    public void setUniformValue(UniformKey key, int value) {
        getProgramUniform(key).setUniformValue(value);
    }

    public void setUniformValue(UniformKey key, float value) {
        getProgramUniform(key).setUniformValue(value);
    }

    public void setUniformValue(UniformKey key, int[] values) {
        getProgramUniform(key).setUniformValue(values);
    }

    public void setUniformValue(UniformKey key, float[] values) {
        getProgramUniform(key).setUniformValue(values);
    }

    public ProgramAttribute getProgramAttribute(AttributeKey key) {
        ProgramAttribute result = mAttributes[key.ordinal()];
        if (result == null) {
            int handle = GLES20.glGetAttribLocation(mProgramHandle, key.getName());
            if (handle >= 0) {
                result = new ProgramAttribute(key.getName(), handle);
                mAttributes[key.ordinal()] = result;
            } else {
                throw new IllegalArgumentException("Unknown attribute '" + key.getName() + "'!");
            }
        }
        return result;
    }

    private ProgramUniform getProgramUniform(UniformKey key) {
        ProgramUniform programUniform = mUniforms[key.ordinal()];
        if (programUniform == null) {
            throw new RuntimeException("Could not get uniform location for " + key);
        }
        return programUniform;
    }

    private static String getShaderSource(int sourceId) {
        StringBuilder sb = new StringBuilder();
        Resources res = CamMod360App.getInstance().getResources();

        InputStream inputStream = res.openRawResource(sourceId);
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

        try {
            String read = in.readLine();
            while (read != null) {
                sb.append(read).append("\n");
                read = in.readLine();
            }
            sb.deleteCharAt(sb.length() - 1);
        } catch (IOException ioe) {
            Log.e(TAG, "Error reading shader", ioe);
        }

        Util.closeSilently(in);

        return sb.toString();
    }

    private static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            if (Util.DEBUG) GlToolBox.checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            if (Util.DEBUG) GlToolBox.checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link mProgramHandle: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private void scanUniforms() {
        int uniformCount[] = new int[1];
        GLES20.glGetProgramiv(mProgramHandle, GLES20.GL_ACTIVE_UNIFORMS, uniformCount, 0);
        if (uniformCount[0] > 0) {
            UniformKey key;
            for (int i = 0; i < uniformCount[0]; ++i) {
                ProgramUniform uniform = new ProgramUniform(mProgramHandle, i);
                key = UniformKey.getKey(uniform.getName());
                if (key != null) mUniforms[key.ordinal()] = uniform;
                else {
                    throw new RuntimeException(
                            "Unable to locate uniform value '" + uniform.getName() + "'!");
                }
            }
        }
    }

    public void pushAttributes() {
        for (ProgramAttribute attr : mAttributes) {
            if (attr != null && !attr.push()) {
                throw new RuntimeException("Unable to assign attribute value '" + attr + "'!");
            }
        }
    }
}
