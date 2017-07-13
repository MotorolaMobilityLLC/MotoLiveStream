package com.motorola.gl.viewfinder;

public class SplitScreenViewfinder extends DefaultViewfinder {

    public SplitScreenViewfinder() {
        super(ViewfinderFactory.ViewfinderType.SPLITSCREEN);
    }

    @Override
    protected float[] getVertexData() {
        return new float[] {
                // drawn to top of preview
                -1f, 0.0032f, 0f,
                1f, 0.0032f, 0f,
                -1f, 1f, 0f,
                1f, 1f, 0f,
                // drawn to bottom left of preview
                -1f, -1f, 0f,
                0f, -1f, 0f,
                -1f, -0.0032f, 0f,
                0f, -0.0032f, 0f,
                // drawn to bottom right of preview
                0f, -1f, 0f,
                1f, -1f, 0f,
                0f, -0.0032f, 0f,
                1f, -0.0032f, 0f
        };
    }

    @Override
    protected float[] getTextureCoordData() {
        return new float[] {
                // Center 50% of source
                0.25f, 0.05556f,
                0.75f, 0.05556f,
                0.25f, 0.9444f,
                0.75f, 0.9444f,
                // Right 25% portion of source
                0.75f, 0.05556f,
                1f, 0.05556f,
                0.75f, 0.9444f,
                1f, 0.9444f,
                // Left 25% portion of source
                0f, 0.05556f,
                0.25f, 0.05556f,
                0f, 0.9444f,
                0.25f, 0.9444f
        };
    }
}
