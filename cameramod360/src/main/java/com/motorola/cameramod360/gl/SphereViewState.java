/*
 * Copyright (C) 2013-2017 Motorola Mobility LLC,
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 */

package com.motorola.cameramod360.gl;

import java.io.Serializable;

public class SphereViewState implements Serializable {

    private float mRotationAngle;
    private float mTiltAngle;
    private float mRollAngle;
    private float mZoom;
    private float mSplitScreenZoom = 1.0f;
    private SphereViewRenderer.ViewType mViewType;

    public SphereViewState(float rotationAngle, float tiltAngle, float rollAngle, float zoom,
                           SphereViewRenderer.ViewType viewType) {
        this.mRotationAngle = rotationAngle;
        this.mTiltAngle = tiltAngle;
        this.mRollAngle = rollAngle;
        this.mZoom = zoom;
        this.mViewType = viewType;
    }

    public SphereViewState(SphereViewState copyState) {
        this.mRotationAngle = copyState.getRotationAngle();
        this.mTiltAngle = copyState.getTiltAngle();
        this.mZoom = copyState.getZoom();
        this.mViewType = copyState.getViewType();
    }

    public float getRotationAngle() {
        return mRotationAngle;
    }

    public void setRotationAngle(float rotationAngle) {
        this.mRotationAngle = rotationAngle;
    }

    public float getTiltAngle() {
        return mTiltAngle;
    }

    public void setTiltAngle(float tiltAngle) {
        this.mTiltAngle = tiltAngle;
    }

    public float getRollAngle() {
        return mRollAngle;
    }

    public void setRollAngle(float rollAngle) {
        this.mRollAngle = rollAngle;
    }

    public float getZoom() {
        return mZoom;
    }

    public void setZoom(float zoom) {
        this.mZoom = zoom;
    }

    public float getSplitScreenZoom() {
        return mSplitScreenZoom;
    }

    public void setSplitScreenZoom(float zoom) {
        mSplitScreenZoom = zoom;
    }

    public SphereViewRenderer.ViewType getViewType() {
        return mViewType;
    }

    public void setViewType(SphereViewRenderer.ViewType viewType) {
        this.mViewType = viewType;
    }
}
