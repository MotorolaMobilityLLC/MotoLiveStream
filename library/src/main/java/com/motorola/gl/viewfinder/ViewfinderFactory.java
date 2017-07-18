package com.motorola.gl.viewfinder;

import android.opengl.GLSurfaceView;

import net.ossrs.yasea.SrsCameraView;

public class ViewfinderFactory {

    public enum ViewfinderType {
        NONE,
        SPLITSCREEN,
        OFFSCREEN,
    }

    public static DefaultViewfinder initFilters(ViewfinderType type) {
        switch (type) {
            case NONE:
                return new DefaultViewfinder();
            case SPLITSCREEN:
                return new SplitScreenViewfinder();
            default:
                return null;
        }
    }
}
