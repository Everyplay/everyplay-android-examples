package com.everyplay.android.everyplayrecord;

import android.content.Context;
import android.opengl.GLSurfaceView;

/**
 * A view container where OpenGL ES graphics can be drawn on screen. This view can also be used to capture touch events,
 * such as a user interacting with drawn objects.
 */
public class EveryplayRecordSurfaceView extends GLSurfaceView {
    private final EveryplayRecordRenderer mRenderer;

    public EveryplayRecordSurfaceView(Context context) {
        super(context);

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new EveryplayRecordRenderer();
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }
}
