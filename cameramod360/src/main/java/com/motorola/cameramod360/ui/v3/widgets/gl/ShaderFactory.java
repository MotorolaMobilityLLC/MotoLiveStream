/*
 * Copyright (C) 2013-2016 Motorola Mobility LLC,
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 */

package com.motorola.cameramod360.ui.v3.widgets.gl;

import android.util.Log;
import android.util.SparseArray;

import com.motorola.camera360mod.R;
import com.motorola.cameramod360.Util;

public class ShaderFactory {
    private static final String TAG = ShaderFactory.class.getSimpleName();

    public enum Shaders {
        CAMERA_PREVIEW(R.raw.camera_preview_glvs, R.raw.camera_preview_glfs),
        CAMERA_PREVIEW_SPHERE(R.raw.camera_preview_sphere_glvs, R.raw.camera_preview_sphere_glfs),
        BITMAP(R.raw.vertex, R.raw.fragment),
        BITMAP_SPHERE(R.raw.camera_preview_sphere_glvs, R.raw.bitmap_sphere_glfs),
        BITMAP_TINY_PLANET(R.raw.bitmap_tiny_planet_glvs, R.raw.bitmap_tiny_planet_glfs);

        private int mVshaderSourceId;
        private int mFshaderSourceId;

        Shaders(int vertexSourceId, int fragSourceId) {
            mVshaderSourceId = vertexSourceId;
            mFshaderSourceId = fragSourceId;
        }
    }

    private static SparseArray<Shader> mShaders = new SparseArray<>(Shaders.values().length);

    public static void loadShaders() {
        if (Util.DEBUG) Log.d(TAG, "loadShaders");
        for (Shaders shader : Shaders.values()) {
            mShaders.append(shader.ordinal(),
                    new Shader(shader.mVshaderSourceId, shader.mFshaderSourceId));
        }
    }

    public static void clearShaders() {
        if (Util.DEBUG) Log.d(TAG, "clearShaders");
        mShaders.clear();
        Shader.clearLastHandle();
    }

    /*package*/
    static boolean isLoaded() {
        return mShaders.size() > 0;
    }

    public static Shader getShader(Shaders shader) {
        return mShaders.get(shader.ordinal());
    }
}
