package com.cloudwalk.gl;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * The slice of OpenGL ES 2.0 this engine uses.
 *
 * Android backs it with GLES20, the browser with a WebGL context. The two APIs
 * are the same API - WebGL 1.0 is GLES 2.0 - but they disagree on how objects
 * are named: GLES20 hands out int handles, WebGL hands out objects. Rather
 * than pick one and make the other pay for it, handles here are opaque types
 * ({@link Buf} and friends) that each backend defines however suits it.
 *
 * That works because GL handles never leave the three classes that draw -
 * ModelViewRenderer, Obj3d and Obj3dStatic. Vertex attribute locations are the
 * exception: those are plain ints in both APIs, so they stay ints here.
 *
 * Enum values are identical in GLES 2.0 and WebGL 1.0, so they live on the
 * interface rather than being redeclared per backend.
 */
public interface GL {

	/** A buffer object. */
	interface Buf {
	}

	/** A linked program. */
	interface Prog {
	}

	/** A compiled shader. */
	interface Shdr {
	}

	/** The location of a uniform within a program. */
	interface Unif {
	}

	int ARRAY_BUFFER = 0x8892;
	int ELEMENT_ARRAY_BUFFER = 0x8893;
	int STATIC_DRAW = 0x88E4;
	int DYNAMIC_DRAW = 0x88E8;

	int FLOAT = 0x1406;
	int UNSIGNED_SHORT = 0x1403;

	int LINES = 0x0001;
	int TRIANGLES = 0x0004;

	int DEPTH_TEST = 0x0B71;
	int CULL_FACE = 0x0B44;
	int BLEND = 0x0BE2;

	int COLOR_BUFFER_BIT = 0x4000;
	int DEPTH_BUFFER_BIT = 0x0100;

	int VERTEX_SHADER = 0x8B31;
	int FRAGMENT_SHADER = 0x8B30;

	// --- shaders and programs ---

	Shdr createShader(int type);

	void shaderSource(Shdr shader, String source);

	void compileShader(Shdr shader);

	/** Hides the int[1] out-parameter that GLES20 wants for GL_COMPILE_STATUS. */
	boolean shaderCompiled(Shdr shader);

	String shaderLog(Shdr shader);

	void deleteShader(Shdr shader);

	Prog createProgram();

	void attachShader(Prog program, Shdr shader);

	void bindAttribLocation(Prog program, int index, String name);

	void linkProgram(Prog program);

	boolean programLinked(Prog program);

	String programLog(Prog program);

	void deleteProgram(Prog program);

	void useProgram(Prog program);

	int getAttribLocation(Prog program, String name);

	Unif getUniformLocation(Prog program, String name);

	// --- buffers ---

	Buf createBuffer();

	void deleteBuffer(Buf buffer);

	/** Pass null to unbind. */
	void bindBuffer(int target, Buf buffer);

	/** Uploads the buffer's remaining elements, replacing any existing store. */
	void bufferData(int target, FloatBuffer data, int usage);

	void bufferData(int target, ShortBuffer data, int usage);

	/** Overwrites part of an existing store, which must already be big enough. */
	void bufferSubData(int target, int byteOffset, FloatBuffer data);

	// --- attributes and uniforms ---

	/**
	 * Note {@code offset} is a byte offset into the bound buffer. There is no
	 * client-side-array form: WebGL has none, so the engine keeps its vertex
	 * data in buffer objects on both platforms.
	 */
	void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, int offset);

	void enableVertexAttribArray(int index);

	void vertexAttrib1f(int index, float x);

	void uniform3f(Unif location, float x, float y, float z);

	void uniformMatrix4fv(Unif location, boolean transpose, float[] value);

	// --- drawing and state ---

	void drawArrays(int mode, int first, int count);

	void drawElements(int mode, int count, int type, int offset);

	void clearColor(float red, float green, float blue, float alpha);

	void clear(int mask);

	void enable(int cap);

	void viewport(int x, int y, int width, int height);

	/**
	 * Widths above 1 are honoured on Android but silently ignored by WebGL,
	 * where the core profile allows only 1.0.
	 */
	void lineWidth(float width);
}
