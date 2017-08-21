/*
 * Copyright (C) 2017 Motorola Mobility LLC,
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 */

package com.motorola.cameramod360.gl;

public class Sphere {
    public static final int FLOAT_SIZE_BYTES = Float.SIZE / 8;
    public static final int VERTEX_POSITION_SIZE = 3;
    public static final int VERTEX_TEXCOORD_SIZE = 2;
    public static final int VERTEX_POSITION_OFFSET_BYTES = 0;
    public static final int VERTEX_TEXCOORD_OFFSET_BYTES = VERTEX_POSITION_SIZE * FLOAT_SIZE_BYTES;
    public static final int VERTEX_STRIDE_BYTES =
            (VERTEX_POSITION_SIZE + VERTEX_TEXCOORD_SIZE) * FLOAT_SIZE_BYTES;

    private static final double NINETY_DEGREES = Math.PI / 2;
    private static final double ONE_EIGHTY_DEGREES = Math.PI;
    private static final double THREE_SIXTY_DEGREES = ONE_EIGHTY_DEGREES * 2;
    private static final double ONE_TWENTY_DEGREES = THREE_SIXTY_DEGREES / 3;

    private static final long POWER_CLAMP = 0x00000000ffffffffL;

    private static final int MAX_DEPTH = 6;
    private static final int STRIP_FACTOR = 5;

    private int mStripCount;
    private int mVerticesPerStrip;
    private float[] mVertices;

    public Sphere(int depth, float radius, boolean invertX, boolean invertY, boolean swapAxis) {
        final int clampedDepth = Math.max(1, Math.min(MAX_DEPTH, depth));

        mStripCount = power(2, clampedDepth) * STRIP_FACTOR;
        mVerticesPerStrip = power(2, clampedDepth) * 3;

        final double altitudeStepAngle = ONE_TWENTY_DEGREES / power(2, clampedDepth);
        final double azimuthStepAngle = THREE_SIXTY_DEGREES / mStripCount;
        double altitude, azimuth, h, x, y, z, s, t;

        final int floatsPerStrip =
                mVerticesPerStrip * (VERTEX_POSITION_SIZE + VERTEX_TEXCOORD_SIZE);
        mVertices = new float[mStripCount * floatsPerStrip];

        for (int stripNum = 0; stripNum < mStripCount; stripNum++) {
            int vertexPos = floatsPerStrip * stripNum;

            // Calculate position of the first vertex in this strip.
            altitude = NINETY_DEGREES;
            azimuth = stripNum * azimuthStepAngle;

            // Draw the rest of this strip.
            for (int vertexNum = 0; vertexNum < mVerticesPerStrip; vertexNum += 2) {
                // First point - Vertex.
                y = radius * Math.sin(altitude);
                h = radius * Math.cos(altitude);
                z = h * Math.sin(azimuth);
                x = h * Math.cos(azimuth);

                mVertices[vertexPos++] = (float) x;
                mVertices[vertexPos++] = (float) y;
                mVertices[vertexPos++] = (float) z;

                // First point - Texture.
                s = azimuth / THREE_SIXTY_DEGREES;
                t = (altitude + NINETY_DEGREES) / ONE_EIGHTY_DEGREES;
                if (invertX) s = 1 - s;
                if (invertY) t = 1 - t;
                if (swapAxis) {
                    mVertices[vertexPos++] = (float) t;
                    mVertices[vertexPos++] = (float) s;
                } else {
                    mVertices[vertexPos++] = (float) s;
                    mVertices[vertexPos++] = (float) t;
                }

                // Second point - Vertex.
                altitude -= altitudeStepAngle;
                azimuth -= azimuthStepAngle / 2.0;
                y = radius * Math.sin(altitude);
                h = radius * Math.cos(altitude);
                z = h * Math.sin(azimuth);
                x = h * Math.cos(azimuth);

                mVertices[vertexPos++] = (float) x;
                mVertices[vertexPos++] = (float) y;
                mVertices[vertexPos++] = (float) z;

                // Second point - Texture.
                s = azimuth / THREE_SIXTY_DEGREES;
                t = (altitude + NINETY_DEGREES) / ONE_EIGHTY_DEGREES;
                if (invertX) s = 1 - s;
                if (invertY) t = 1 - t;
                if (swapAxis) {
                    mVertices[vertexPos++] = (float) t;
                    mVertices[vertexPos++] = (float) s;
                } else {
                    mVertices[vertexPos++] = (float) s;
                    mVertices[vertexPos++] = (float) t;
                }

                azimuth += azimuthStepAngle;
            }
        }
    }

    public int getStripCount() {
        return mStripCount;
    }

    public int getVerticesPerStrip() {
        return mVerticesPerStrip;
    }

    public float[] getVertices() {
        return mVertices;
    }

    public static int power(final int base, final int raise) {
        int p = 1;
        long b = raise & POWER_CLAMP;

        // bits in b correspond to values of powerN
        // so start with p=1, and for each set bit in b, multiply corresponding
        // table entry
        long powerN = base;

        while (b != 0) {
            if ((b & 1) != 0) {
                p *= powerN;
            }
            b >>>= 1;
            powerN = powerN * powerN;
        }

        return p;
    }
}
