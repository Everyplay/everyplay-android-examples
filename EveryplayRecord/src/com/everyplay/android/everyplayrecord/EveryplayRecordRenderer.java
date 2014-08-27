/*
 * Copyright 2014 Unity Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.everyplay.android.everyplayrecord;

import javax.microedition.khronos.egl.*;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;
import android.opengl.GLES20;
import android.opengl.GLES10;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.everyplay.Everyplay.Everyplay;

/**
 * Provides drawing instructions for a GLSurfaceView object. This class
 * must override the OpenGL ES drawing lifecycle methods:
 * <ul>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}</li>
 *   <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
public class EveryplayRecordRenderer implements GLSurfaceView.Renderer {
	private static final String TAG = "EveryplayRecord";

	public static Boolean hudEnabled = false;

	private static final String vertexShaderCode =
			"attribute vec4 position;" +
					"attribute vec4 color;" +
					"varying vec4 colorVarying;" +
					"uniform float translate;" +
					"void main() {" +
					"    gl_Position = position;" +
					"    gl_Position.y += sin(translate) / 2.0;" +
					"    gl_Position.x += cos(translate) * 0.5;" +
					"    colorVarying = color;" +
					"}";

	private static final String fragmentShaderCode =
			"varying lowp vec4 colorVarying;" +
					"void main() {" +
					"    gl_FragColor = colorVarying;" +
					"}";

	private static float[] squareVerticesArray = {
		-0.5f, -0.33f,
		0.5f,  -0.33f,
		-0.5f, 0.33f,
		0.5f,  0.33f,
	};
	private FloatBuffer squareVertices;

	private static byte maxColor = (byte)255;
	private static byte[] squareColorsArray = {
		maxColor, maxColor, 0,        maxColor,
		0,        maxColor, maxColor, maxColor,
		0,        0,        0,        maxColor,
		maxColor, 0,        maxColor, maxColor
	};
	private ByteBuffer squareColors;

	private int program;

	private int attribVertex = 1;
	private int attribColor = 2;
	private int uniformTranslate = 1;

	private float transY = 0.0f;

	private int glesVersion = 2;

	static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

	private int queryESVersion() {
		EGL10 egl = (EGL10)EGLContext.getEGL();

		EGLDisplay display = egl.eglGetCurrentDisplay();
		EGLContext context = egl.eglGetCurrentContext();

		int query[] = { 0 };

		egl.eglQueryContext(display, context, EGL_CONTEXT_CLIENT_VERSION, query);

		return query[0];
	}

	@Override
	public void onSurfaceCreated(GL10 gl10, EGLConfig config) {
		glesVersion = queryESVersion();
		Log.d(TAG, "glesVersion: " + glesVersion);

		ByteBuffer squareVerticesBuf = ByteBuffer
				.allocateDirect(squareVerticesArray.length * 4);
		squareVerticesBuf.order(ByteOrder.nativeOrder());
		squareVertices = squareVerticesBuf.asFloatBuffer();
		squareVertices.put(squareVerticesArray);
		squareVertices.position(0);

		squareColors = ByteBuffer.allocateDirect(squareColorsArray.length);
		squareColors.order(ByteOrder.nativeOrder());
		squareColors.put(squareColorsArray);
		squareColors.position(0);

		GLES20.glClearColor(0.45f, 0.45f, 0.45f, 1.0f);

		if (glesVersion >= 2) {
			// prepare shaders and OpenGL program
			int vertexShader = EveryplayRecordRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
			int fragmentShader = EveryplayRecordRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

			program = GLES20.glCreateProgram();
			GLES20.glAttachShader(program, vertexShader);
			GLES20.glAttachShader(program, fragmentShader);

			GLES20.glBindAttribLocation(program, attribVertex, "position");
			GLES20.glBindAttribLocation(program, attribColor, "color");

			GLES20.glLinkProgram(program);

			// glBindAttribLocation should be enough, but workaround
			attribVertex = GLES20.glGetAttribLocation(program, "position");
			attribColor = GLES20.glGetAttribLocation(program, "color");

			uniformTranslate = GLES20.glGetUniformLocation(program, "translate");
		}
	}

	@Override
	public void onDrawFrame(GL10 gl10) {
		GLES20.glClearColor(0.45f, 0.45f, 0.45f, 1.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		if (glesVersion >= 2) {
			GLES20.glUseProgram(program);

			GLES20.glUniform1f(uniformTranslate, transY);
			transY += 0.075f;

			GLES20.glVertexAttribPointer(attribVertex, 2, GLES20.GL_FLOAT, false, 0, squareVertices);
			GLES20.glEnableVertexAttribArray(attribVertex);
			GLES20.glVertexAttribPointer(attribColor, 4, GLES20.GL_UNSIGNED_BYTE, true, 0, squareColors);
			GLES20.glEnableVertexAttribArray(attribColor);

			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

			if (hudEnabled == true) {
				Everyplay.snapshotRenderbuffer();
			}

			GLES20.glUniform1f(uniformTranslate, transY + 3.1415926536f);
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		} else {
			GLES10.glMatrixMode(GLES10.GL_PROJECTION);
			GLES10.glLoadIdentity();
			GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
			GLES10.glLoadIdentity();
			GLES10.glTranslatef((float)(Math.cos(transY) * 0.5f), (float)(Math.sin(transY) / 2.0f), 0.0f);
			transY += 0.075f;

			GLES10.glVertexPointer(2, GLES10.GL_FLOAT, 0, squareVertices);
			GLES10.glEnableClientState(GLES10.GL_VERTEX_ARRAY);
			GLES10.glColorPointer(4, GLES10.GL_UNSIGNED_BYTE, 0, squareColors);
			GLES10.glEnableClientState(GLES10.GL_COLOR_ARRAY);

			GLES10.glDrawArrays(GLES10.GL_TRIANGLE_STRIP, 0, 4);

			if (hudEnabled == true) {
				Everyplay.snapshotRenderbuffer();
			}

			float transY2 = transY + 3.1415926536f;

			GLES10.glLoadIdentity();
			GLES10.glTranslatef((float)(Math.cos(transY2) * 0.5f), (float)(Math.sin(transY2) / 2.0f), 0.0f);

			GLES10.glDrawArrays(GLES10.GL_TRIANGLE_STRIP, 0, 4);
		}
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		// Adjust the viewport based on geometry changes,
		// such as screen rotation
		GLES20.glViewport(0, 0, width, height);
	}

	/**
	 * Utility method for compiling a OpenGL shader.
	 *
	 * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
	 * method to debug shader coding errors.</p>
	 *
	 * @param type - Vertex or fragment shader type.
	 * @param shaderCode - String containing the shader code.
	 * @return - Returns an id for the shader.
	 */
	public static int loadShader(int type, String shaderCode) {
		// create a vertex shader type (GLES20.GL_VERTEX_SHADER)
		// or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
		int shader = GLES20.glCreateShader(type);

		// add the source code to the shader and compile it
		GLES20.glShaderSource(shader, shaderCode);
		GLES20.glCompileShader(shader);

		return shader;
	}

	/**
	 * Utility method for debugging OpenGL calls. Provide the name of the call
	 * just after making it:
	 *
	 * <pre>
	 * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
	 * ERGLRenderer.checkGlError("glGetUniformLocation");</pre>
	 *
	 * If the operation is not successful, the check throws an error.
	 *
	 * @param glOperation - Name of the OpenGL call to check.
	 */
	public static void checkGlError(String glOperation) {
		int error;

		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e(TAG, glOperation + ": glError " + error);
			throw new RuntimeException(glOperation + ": glError " + error);
		}
	}
}
