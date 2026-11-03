package com.cloudwalk.android;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES20;

import com.cloudwalk.gl.GL;

/**
 * {@link GL} over Android's GLES20.
 *
 * Almost all delegation. Two things it smooths over: GLES20 names objects with
 * ints where the facade uses opaque handles, and several of its calls want
 * scratch int arrays as out-parameters or byte counts the caller has to work
 * out. Both are hidden here rather than at every call site.
 */
public final class Gles20GL implements GL {

	private static final int BYTES_PER_FLOAT = 4;
	private static final int BYTES_PER_SHORT = 2;

	/** One int handle, wearing whichever hat the facade asked for. */
	private static final class H implements GL.Buf, GL.Prog, GL.Shdr, GL.Unif {
		final int id;

		H(int id) {
			this.id = id;
		}
	}

	private static int id(Object h) {
		return h == null ? 0 : ((H) h).id;
	}

	private final int[] scratch = new int[1];

	// --- shaders and programs ---

	public GL.Shdr createShader(int type) {
		return new H(GLES20.glCreateShader(type));
	}

	public void shaderSource(GL.Shdr shader, String source) {
		GLES20.glShaderSource(id(shader), source);
	}

	public void compileShader(GL.Shdr shader) {
		GLES20.glCompileShader(id(shader));
	}

	public boolean shaderCompiled(GL.Shdr shader) {
		GLES20.glGetShaderiv(id(shader), GLES20.GL_COMPILE_STATUS, scratch, 0);
		return scratch[0] != 0;
	}

	public String shaderLog(GL.Shdr shader) {
		return GLES20.glGetShaderInfoLog(id(shader));
	}

	public void deleteShader(GL.Shdr shader) {
		GLES20.glDeleteShader(id(shader));
	}

	public GL.Prog createProgram() {
		return new H(GLES20.glCreateProgram());
	}

	public void attachShader(GL.Prog program, GL.Shdr shader) {
		GLES20.glAttachShader(id(program), id(shader));
	}

	public void bindAttribLocation(GL.Prog program, int index, String name) {
		GLES20.glBindAttribLocation(id(program), index, name);
	}

	public void linkProgram(GL.Prog program) {
		GLES20.glLinkProgram(id(program));
	}

	public boolean programLinked(GL.Prog program) {
		GLES20.glGetProgramiv(id(program), GLES20.GL_LINK_STATUS, scratch, 0);
		return scratch[0] != 0;
	}

	public String programLog(GL.Prog program) {
		return GLES20.glGetProgramInfoLog(id(program));
	}

	public void deleteProgram(GL.Prog program) {
		GLES20.glDeleteProgram(id(program));
	}

	public void useProgram(GL.Prog program) {
		GLES20.glUseProgram(id(program));
	}

	public int getAttribLocation(GL.Prog program, String name) {
		return GLES20.glGetAttribLocation(id(program), name);
	}

	public GL.Unif getUniformLocation(GL.Prog program, String name) {
		return new H(GLES20.glGetUniformLocation(id(program), name));
	}

	// --- buffers ---

	public GL.Buf createBuffer() {
		GLES20.glGenBuffers(1, scratch, 0);
		return new H(scratch[0]);
	}

	public void deleteBuffer(GL.Buf buffer) {
		scratch[0] = id(buffer);
		GLES20.glDeleteBuffers(1, scratch, 0);
	}

	public void bindBuffer(int target, GL.Buf buffer) {
		GLES20.glBindBuffer(target, id(buffer));
	}

	public void bufferData(int target, FloatBuffer data, int usage) {
		GLES20.glBufferData(target, data.remaining() * BYTES_PER_FLOAT, data, usage);
	}

	public void bufferData(int target, ShortBuffer data, int usage) {
		GLES20.glBufferData(target, data.remaining() * BYTES_PER_SHORT, data, usage);
	}

	public void bufferSubData(int target, int byteOffset, FloatBuffer data) {
		GLES20.glBufferSubData(target, byteOffset, data.remaining() * BYTES_PER_FLOAT, data);
	}

	// --- attributes and uniforms ---

	public void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, int offset) {
		GLES20.glVertexAttribPointer(index, size, type, normalized, stride, offset);
	}

	public void enableVertexAttribArray(int index) {
		GLES20.glEnableVertexAttribArray(index);
	}

	public void vertexAttrib1f(int index, float x) {
		GLES20.glVertexAttrib1f(index, x);
	}

	public void uniform3f(GL.Unif location, float x, float y, float z) {
		GLES20.glUniform3f(id(location), x, y, z);
	}

	public void uniformMatrix4fv(GL.Unif location, boolean transpose, float[] value) {
		GLES20.glUniformMatrix4fv(id(location), 1, transpose, value, 0);
	}

	// --- drawing and state ---

	public void drawArrays(int mode, int first, int count) {
		GLES20.glDrawArrays(mode, first, count);
	}

	public void drawElements(int mode, int count, int type, int offset) {
		GLES20.glDrawElements(mode, count, type, offset);
	}

	public void clearColor(float red, float green, float blue, float alpha) {
		GLES20.glClearColor(red, green, blue, alpha);
	}

	public void clear(int mask) {
		GLES20.glClear(mask);
	}

	public void enable(int cap) {
		GLES20.glEnable(cap);
	}

	public void viewport(int x, int y, int width, int height) {
		GLES20.glViewport(x, y, width, height);
	}

	public void lineWidth(float width) {
		GLES20.glLineWidth(width);
	}
}
