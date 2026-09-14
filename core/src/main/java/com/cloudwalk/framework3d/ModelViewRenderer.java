package com.cloudwalk.framework3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.cloudwalk.gl.GL;
import com.cloudwalk.platform.Color;
import com.cloudwalk.platform.Log;
import com.cloudwalk.platform.Mat4;
import com.cloudwalk.platform.Prefs;

import com.cloudwalk.client.Task;
import com.cloudwalk.client.XCModelViewer;

/**
 * Draws the scene.
 *
 * Talks to a {@link GL} rather than to GLES20 directly, so the same code runs
 * against Android's OpenGL ES 2.0 and against WebGL in a browser. The platform
 * owns the surface and the frame callback; this class owns the program, the
 * matrices and the draw order.
 */
public class ModelViewRenderer {

	/**
	 * Store the model matrix. This matrix is used to move models from object space (where each model can be thought of being located at the center of the
	 * universe) to world space.
	 */
	private float[] mModelMatrix = new float[16];

	/**
	 * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space; it positions things relative to our eye.
	 */
	private float[] mViewMatrix = new float[16];

	/** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
	private float[] mProjectionMatrix = new float[16];

	/** Allocate storage for the final combined matrix. This will be passed into the shader program. */
	private float[] mMVPMatrix = new float[16];

	/** Allocate storage for the model view matrix. This will be passed into the shader program. */
	private float[] mMVMatrix = new float[16];

	/** This will be used to pass in the transformation matrix. */
	private GL.Unif mMVPMatrixHandle;

	/** This will be used to pass in the transformation matrix. */
	private GL.Unif mMVMatrixHandle;

	/** This will be used to pass in the light position. */
	private GL.Unif mLightPosHandle;

	/** This will be used to pass in model position information. */
	int mPositionHandle;

	/** This will be used to pass in model color information. */
	/** Set by the platform when the drawing surface comes up. */
	public GL gl;

	int mColorHandle;

	/** This will be used to pass in model normal information. */
	int mNormalHandle;

	/** This will be used to pass in model normal information. */
	private int mFarDistanceHandle;

	/**
	 * Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when we multiply this by our
	 * transformation matrices.
	 */
	private final float[] mLightPosInModelSpace = new float[] { 500000000.0f, 1000000000.0f, 500000000.0f, 1.0f };

	/** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix) */
	private final float[] mLightPosInEyeSpace = new float[4];

	public XCModelViewer modelViewer;

	public float far = 100f;
	public float lastFar = 0f;
	public int width;
	public int height;
	public int viewAngle;
	boolean no_vbo = false;
	int sky_color = Color.WHITE;
	int ground_color = Color.WHITE;
	Prefs prefs;

	/**
	 * Initialize the model data.
	 */
	public ModelViewRenderer(Prefs prefs) {
		this.prefs = prefs;
		viewAngle = Integer.parseInt(prefs.getString("view_angle", "10"));
		no_vbo = prefs.getBoolean("no_vbo", false);
		sky_color = prefs.getInt("sky_color", Color.WHITE);
		ground_color = prefs.getInt("ground_color", Color.WHITE);
	}

	public void updateCamera() {

		// Position the eye behind the origin.
		float[] eye = modelViewer.cameraMan.getEye();
		// We are looking toward the distance
		float[] focus = modelViewer.cameraMan.getFocus();
		// Set our up vector. This is where our head would be pointing were we holding the camera.
		final float upX = 0.0f;
		final float upY = 1.0f;
		final float upZ = 0.0f;

		// Set the view matrix. This matrix can be said to represent the camera position.
		// NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
		// view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
		Mat4.setLookAtM(mViewMatrix, 0, -eye[1], eye[2], -eye[0], -focus[1], focus[2], -focus[0], upX, upY, upZ);

		// // Position the eye behind the origin.
		// final float eyeX = 0.0f;
		// final float eyeY = 0.0f;
		// final float eyeZ = 2.5f;
		//
		// // We are looking toward the distance
		// final float lookX = 0.0f;
		// final float lookY = 0.0f;
		// final float lookZ = -5.0f;

		mLightPosInModelSpace[0] = Task.sun[1];
		mLightPosInModelSpace[1] = Task.sun[2];
		mLightPosInModelSpace[2] = Task.sun[0];
		//
		// // Set our up vector. This is where our head would be pointing were we holding the camera.
		// final float upX = 0.0f;
		// final float upY = 1.0f;
		// final float upZ = 0.0f;

		// Set the view matrix. This matrix can be said to represent the camera position.
		// NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
		// view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
		// Mat4.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

	}

	/** Called by the platform once the GL context exists, and again if it is lost. */
	public void onSurfaceCreated(GL gl) {
		this.gl = gl;
		if (!no_vbo) {
			Obj3dStatic.static_initialized = false;
			Obj3dStatic.static_initialized_wire = false;
		} 
		gl.clearColor(Color.red(sky_color) / 255f, Color.green(sky_color) / 255f, Color.blue(sky_color) / 255f, 0f);

		updateCamera();
		final String vertexShader = "uniform mat4 u_MVPMatrix;      \n" // A constant representing the combined model/view/projection matrix.
				+ "uniform mat4 u_MVMatrix;       \n" // A constant representing the combined model/view matrix.
				+ "uniform vec3 u_LightPos;       \n" // The position of the light in eye space.

				+ "attribute vec4 a_Position;     \n" // Per-vertex position information we will pass in.
				+ "attribute vec4 a_Color;        \n" // Per-vertex color information we will pass in.
				+ "attribute vec3 a_Normal;       \n" // Per-vertex normal information we will pass in.
				+ "attribute float a_FarDist;       \n" // Per-vertex normal information we will pass in.

				+ "varying vec4 v_Color;          \n" // This will be passed into the fragment shader.
				+ "varying vec3 v_Position;          \n" // This will be passed into the fragment shader.
				+ "varying float v_FarDist;			\n" + "void main()                    \n" // The entry point for our vertex shader.
				+ "{                              \n"
				// Transform the vertex into eye space.
				+ "   v_Position = vec3(u_MVMatrix * a_Position);              \n"
				// Transform the normal's orientation into eye space.
				+ "   vec3 normal = vec3(u_MVMatrix * vec4(normalize(a_Normal), 0.0));     \n"
				// Get a lighting direction vector from the light to the vertex.
				+ "   vec3 lightVector = normalize(u_LightPos - v_Position);             \n"
				// Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
				// pointing in the same direction then it will get max illumination.
				+ "   float diffuse = max(dot(normal, lightVector), 0.1);              \n" + "   v_Color = a_Color * pow(diffuse, 0.25);						\n"
				// + "   vec4 color = a_Color;						\n"

				+ "   v_FarDist = a_FarDist; " // the pipeline.
				+ "   gl_Position = u_MVPMatrix * a_Position;   \n" // gl_Position is a special variable used to store the final position.
				+ "                 \n" // Multiply the vertex by the matrix to get the final point in
				+ "}                              \n"; // normalized screen coordinates.

		final String fragmentShader = "precision mediump float;       \n" // Set the default precision to medium. We don't need as high of a
																			// precision in the fragment shader.

				+ "varying vec4 v_Color;          \n" // This is the color from the vertex shader interpolated across the
														// triangle per fragment.
				+ "varying vec3 v_Position;          \n" // This will be passed into the fragment shader.
				+ "varying float v_FarDist;       \n" // Per-vertex normal information we will pass in.

				+ "void main()                    \n" // The entry point for our fragment shader.
				+ "{                              \n"
				+ "   float distance = length(v_Position);     \n"
				+ "   float factor = 1.0 - (1.0 / (1.0 + (0.06 * (100.0 / v_FarDist) * distance)));"
				+ "   vec4 fog_color = vec4(1.0,1.0,1.0,1.0);	     \n"
				+ "   gl_FragColor = mix(v_Color, fog_color, factor);     \n" // Pass the color directly through
				// + "   gl_FragColor = v_Color;     \n" // Pass the color directly through the pipeline.
				+ "}                              \n";

		// Load in the vertex shader.
		GL.Shdr vertexShaderHandle = gl.createShader(GL.VERTEX_SHADER);
		gl.shaderSource(vertexShaderHandle, vertexShader);
		gl.compileShader(vertexShaderHandle);
		if (!gl.shaderCompiled(vertexShaderHandle)) {
			String log = gl.shaderLog(vertexShaderHandle);
			gl.deleteShader(vertexShaderHandle);
			throw new RuntimeException("Error compiling vertex shader: " + log);
		}

		// Load in the fragment shader.
		GL.Shdr fragmentShaderHandle = gl.createShader(GL.FRAGMENT_SHADER);
		gl.shaderSource(fragmentShaderHandle, fragmentShader);
		gl.compileShader(fragmentShaderHandle);
		if (!gl.shaderCompiled(fragmentShaderHandle)) {
			String log = gl.shaderLog(fragmentShaderHandle);
			gl.deleteShader(fragmentShaderHandle);
			throw new RuntimeException("Error compiling fragment shader: " + log);
		}

		// Create a program object and store the handle to it.
		GL.Prog programHandle = gl.createProgram();
		gl.attachShader(programHandle, vertexShaderHandle);
		gl.attachShader(programHandle, fragmentShaderHandle);

		// Bind attributes
		gl.bindAttribLocation(programHandle, 0, "a_Position");
		gl.bindAttribLocation(programHandle, 1, "a_Color");
		gl.bindAttribLocation(programHandle, 2, "a_Normal");
		gl.bindAttribLocation(programHandle, 3, "a_FarDist");

		gl.linkProgram(programHandle);
		if (!gl.programLinked(programHandle)) {
			String log = gl.programLog(programHandle);
			gl.deleteProgram(programHandle);
			throw new RuntimeException("Error linking program: " + log);
		}

		// Set program handles. These will later be used to pass in values to the program.
		mMVPMatrixHandle = gl.getUniformLocation(programHandle, "u_MVPMatrix");
		mMVMatrixHandle = gl.getUniformLocation(programHandle, "u_MVMatrix");
		mLightPosHandle = gl.getUniformLocation(programHandle, "u_LightPos");
		mPositionHandle = gl.getAttribLocation(programHandle, "a_Position");
		mColorHandle = gl.getAttribLocation(programHandle, "a_Color");
		mNormalHandle = gl.getAttribLocation(programHandle, "a_Normal");
		mFarDistanceHandle = gl.getAttribLocation(programHandle, "a_FarDist");

		// Tell OpenGL to use this program when rendering.
		gl.useProgram(programHandle);
	}

	public void updateProjectionMatrixIfNeeded() {
		this.far = modelViewer.cameraMan.getDepthOfVision();
		if (far != lastFar) {
			final float ratio = (float) width / height;
			final float left = -ratio / viewAngle;
			final float right = ratio / viewAngle;
			final float bottom = -1.0f / viewAngle;
			final float top = 1.0f / viewAngle;
			final float near = 0.2f;
			// Create a new perspective projection matrix. The height will stay the same
			// while the width will vary as per aspect ratio.

			Mat4.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
			lastFar = far;
			// Log.i("FC MVR", "" + far);
		}

	}

	public void onSurfaceChanged(int width, int height) {
		// Set the OpenGL viewport to the same size as the surface.
		gl.viewport(0, 0, width, height);
		gl.enable(GL.DEPTH_TEST);
		gl.enable(GL.CULL_FACE);
		// GL_POLYGON_SMOOTH_HINT used to be hinted here. It is a desktop-GL
		// enum that does not exist in ES 2.0 - the call was raising
		// GL_INVALID_ENUM and doing nothing.
		this.width = width;
		this.height = height;
		updateProjectionMatrixIfNeeded();
	}

	public void drawEverything() {
		Mat4.setIdentityM(mModelMatrix, 0);
		gl.clear(GL.DEPTH_BUFFER_BIT | GL.COLOR_BUFFER_BIT);
		updateCamera();
		updateProjectionMatrixIfNeeded();
		// This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
		// (which currently contains model * view).
		Mat4.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

		gl.uniformMatrix4fv(mMVMatrixHandle, false, mMVMatrix);

		Mat4.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0);

		gl.uniformMatrix4fv(mMVPMatrixHandle, false, mMVPMatrix);

		Mat4.multiplyMV(mLightPosInEyeSpace, 0, mMVMatrix, 0, mLightPosInModelSpace, 0);

		// Pass in the light position in eye space.
		gl.uniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

		gl.vertexAttrib1f(mFarDistanceHandle, far);

		drawWorld();
		Obj3dStatic.draw(gl, mPositionHandle, mColorHandle, mNormalHandle);
		for (int i = 0; i < modelViewer.obj3dManager.size(); i++) {
			try {
				Obj3d o = modelViewer.obj3dManager.obj(i);
				Mat4.setIdentityM(mModelMatrix, 0);
				o.draw(gl, mPositionHandle, mColorHandle, mNormalHandle);
			} catch (Exception e) {
				// This used to swallow the exception whole, which hides a
				// broken object behind a silently missing one. Report it, but
				// only once - it would otherwise fire every frame.
				if (!drawErrorReported) {
					drawErrorReported = true;
					Log.e("FC", "Obj3d draw failed (further reports suppressed)", e);
				}
			}
		}
	}

	private boolean drawErrorReported = false;

	FloatBuffer worldBuffer;
	private GL.Buf worldBufferObject;
	private static final int WORLD_STRIDE = 10 * 4;

	private void drawWorld() {
		float d = 5000f;
		float b = -0.02f;

		if (worldBufferObject == null) {
			final float[] triangle1VerticesData = {
					// X, Y, Z,
					// R, G, B, A
					0, b, d, 0, 1, 0, Color.red(ground_color) / 255f, Color.green(ground_color) / 255f, Color.blue(ground_color) / 255f, 1.0f, d, b, -d, 0, 1,
					0, Color.red(ground_color) / 255f, Color.green(ground_color) / 255f, Color.blue(ground_color) / 255f, 1.0f, -d, b, -d, 0, 1, 0,
					Color.red(ground_color) / 255f, Color.green(ground_color) / 255f, Color.blue(ground_color) / 255f, 1.0f };
			// Initialize the buffers.
			worldBuffer = ByteBuffer.allocateDirect(triangle1VerticesData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
			worldBuffer.put(triangle1VerticesData).position(0);

			// The ground never moves, so this goes up once as a static VBO.
			// It used to be handed to glVertexAttribPointer as a client-side
			// array, which WebGL has no equivalent for.
			worldBufferObject = gl.createBuffer();
			gl.bindBuffer(GL.ARRAY_BUFFER, worldBufferObject);
			gl.bufferData(GL.ARRAY_BUFFER, worldBuffer, GL.STATIC_DRAW);
		}

		// One interleaved buffer: position, normal, colour - 10 floats a vertex.
		gl.bindBuffer(GL.ARRAY_BUFFER, worldBufferObject);
		gl.vertexAttribPointer(mPositionHandle, 3, GL.FLOAT, false, WORLD_STRIDE, 0);
		gl.enableVertexAttribArray(mPositionHandle);
		gl.vertexAttribPointer(mNormalHandle, 3, GL.FLOAT, false, WORLD_STRIDE, 3 * 4);
		gl.enableVertexAttribArray(mNormalHandle);
		gl.vertexAttribPointer(mColorHandle, 4, GL.FLOAT, false, WORLD_STRIDE, 6 * 4);
		gl.enableVertexAttribArray(mColorHandle);
		gl.drawArrays(GL.TRIANGLES, 0, 3);
	}

}
