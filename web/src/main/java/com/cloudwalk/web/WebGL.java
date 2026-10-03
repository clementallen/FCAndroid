package com.cloudwalk.web;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLProgram;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLShader;
import org.teavm.jso.webgl.WebGLUniformLocation;

import com.cloudwalk.gl.GL;

/**
 * {@link GL} over a WebGL context.
 *
 * Thin by design. WebGL 1.0 is OpenGL ES 2.0, so this is almost all
 * delegation; the only real work is wrapping WebGL's object handles in the
 * facade's opaque types. TeaVM's bindings take java.nio buffers and float[]
 * directly, so the engine's existing vertex data goes across with no
 * marshalling.
 */
public final class WebGL implements GL {

	private static final int COMPILE_STATUS = 0x8B81;
	private static final int LINK_STATUS = 0x8B82;

	private final WebGLRenderingContext gl;

	public WebGL(WebGLRenderingContext gl) {
		this.gl = gl;
	}

	// --- handle wrappers ---

	private static final class Buffer implements GL.Buf {
		final WebGLBuffer o;

		Buffer(WebGLBuffer o) {
			this.o = o;
		}
	}

	private static final class Program implements GL.Prog {
		final WebGLProgram o;

		Program(WebGLProgram o) {
			this.o = o;
		}
	}

	private static final class Shader implements GL.Shdr {
		final WebGLShader o;

		Shader(WebGLShader o) {
			this.o = o;
		}
	}

	private static final class Uniform implements GL.Unif {
		final WebGLUniformLocation o;

		Uniform(WebGLUniformLocation o) {
			this.o = o;
		}
	}

	private static WebGLBuffer raw(GL.Buf b) {
		return b == null ? null : ((Buffer) b).o;
	}

	// --- shaders and programs ---

	public GL.Shdr createShader(int type) {
		return new Shader(gl.createShader(type));
	}

	public void shaderSource(GL.Shdr shader, String source) {
		gl.shaderSource(((Shader) shader).o, source);
	}

	public void compileShader(GL.Shdr shader) {
		gl.compileShader(((Shader) shader).o);
	}

	public boolean shaderCompiled(GL.Shdr shader) {
		return gl.getShaderParameterb(((Shader) shader).o, COMPILE_STATUS);
	}

	public String shaderLog(GL.Shdr shader) {
		return gl.getShaderInfoLog(((Shader) shader).o);
	}

	public void deleteShader(GL.Shdr shader) {
		gl.deleteShader(((Shader) shader).o);
	}

	public GL.Prog createProgram() {
		return new Program(gl.createProgram());
	}

	public void attachShader(GL.Prog program, GL.Shdr shader) {
		gl.attachShader(((Program) program).o, ((Shader) shader).o);
	}

	public void bindAttribLocation(GL.Prog program, int index, String name) {
		gl.bindAttribLocation(((Program) program).o, index, name);
	}

	public void linkProgram(GL.Prog program) {
		gl.linkProgram(((Program) program).o);
	}

	public boolean programLinked(GL.Prog program) {
		return gl.getProgramParameterb(((Program) program).o, LINK_STATUS);
	}

	public String programLog(GL.Prog program) {
		return gl.getProgramInfoLog(((Program) program).o);
	}

	public void deleteProgram(GL.Prog program) {
		gl.deleteProgram(((Program) program).o);
	}

	public void useProgram(GL.Prog program) {
		gl.useProgram(((Program) program).o);
	}

	public int getAttribLocation(GL.Prog program, String name) {
		return gl.getAttribLocation(((Program) program).o, name);
	}

	public GL.Unif getUniformLocation(GL.Prog program, String name) {
		return new Uniform(gl.getUniformLocation(((Program) program).o, name));
	}

	// --- buffers ---

	public GL.Buf createBuffer() {
		return new Buffer(gl.createBuffer());
	}

	public void deleteBuffer(GL.Buf buffer) {
		gl.deleteBuffer(raw(buffer));
	}

	public void bindBuffer(int target, GL.Buf buffer) {
		gl.bindBuffer(target, raw(buffer));
	}

	public void bufferData(int target, FloatBuffer data, int usage) {
		gl.bufferData(target, data, usage);
	}

	public void bufferData(int target, ShortBuffer data, int usage) {
		gl.bufferData(target, data, usage);
	}

	public void bufferSubData(int target, int byteOffset, FloatBuffer data) {
		gl.bufferSubData(target, byteOffset, data);
	}

	// --- attributes and uniforms ---

	public void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, int offset) {
		gl.vertexAttribPointer(index, size, type, normalized, stride, offset);
	}

	public void enableVertexAttribArray(int index) {
		gl.enableVertexAttribArray(index);
	}

	public void vertexAttrib1f(int index, float x) {
		gl.vertexAttrib1f(index, x);
	}

	public void uniform3f(GL.Unif location, float x, float y, float z) {
		gl.uniform3f(((Uniform) location).o, x, y, z);
	}

	public void uniformMatrix4fv(GL.Unif location, boolean transpose, float[] value) {
		gl.uniformMatrix4fv(((Uniform) location).o, transpose, value);
	}

	// --- drawing and state ---

	public void drawArrays(int mode, int first, int count) {
		gl.drawArrays(mode, first, count);
	}

	public void drawElements(int mode, int count, int type, int offset) {
		gl.drawElements(mode, count, type, offset);
	}

	public void clearColor(float red, float green, float blue, float alpha) {
		gl.clearColor(red, green, blue, alpha);
	}

	public void clear(int mask) {
		gl.clear(mask);
	}

	public void enable(int cap) {
		gl.enable(cap);
	}

	public void viewport(int x, int y, int width, int height) {
		gl.viewport(x, y, width, height);
	}

	/** WebGL's core profile allows only 1.0, so wider lines are a no-op here. */
	public void lineWidth(float width) {
		gl.lineWidth(width);
	}
}
