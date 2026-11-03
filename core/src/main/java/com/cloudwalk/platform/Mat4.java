package com.cloudwalk.platform;

/**
 * The handful of 4x4 matrix operations the renderer needs.
 *
 * Replaces android.opengl.Matrix, of which exactly five functions were ever
 * used. Written from scratch rather than lifted from AOSP: this project is
 * GPL, and borrowing 120 lines of textbook linear algebra is not worth
 * importing a second licence over.
 *
 * Matrices are 16 floats in column-major order - the layout OpenGL expects, so
 * they go straight to glUniformMatrix4fv with transpose=false. Element (row r,
 * column c) lives at index c * 4 + r.
 */
public final class Mat4 {

	public static void setIdentityM(float[] m, int off) {
		for (int i = 0; i < 16; i++) {
			m[off + i] = 0.0f;
		}
		m[off + 0] = m[off + 5] = m[off + 10] = m[off + 15] = 1.0f;
	}

	/**
	 * result = lhs * rhs. Aliasing result with either operand is not supported,
	 * matching android.opengl.Matrix.
	 */
	public static void multiplyMM(float[] result, int resultOff, float[] lhs, int lhsOff, float[] rhs, int rhsOff) {
		for (int c = 0; c < 4; c++) {
			int rc = resultOff + c * 4;
			int cc = rhsOff + c * 4;
			float r0 = rhs[cc], r1 = rhs[cc + 1], r2 = rhs[cc + 2], r3 = rhs[cc + 3];
			for (int r = 0; r < 4; r++) {
				result[rc + r] = lhs[lhsOff + r] * r0
						+ lhs[lhsOff + 4 + r] * r1
						+ lhs[lhsOff + 8 + r] * r2
						+ lhs[lhsOff + 12 + r] * r3;
			}
		}
	}

	/** resultVec = lhsMat * rhsVec, both vectors being 4 floats. */
	public static void multiplyMV(float[] resultVec, int resultOff, float[] lhsMat, int lhsOff, float[] rhsVec, int rhsOff) {
		float x = rhsVec[rhsOff], y = rhsVec[rhsOff + 1], z = rhsVec[rhsOff + 2], w = rhsVec[rhsOff + 3];
		for (int r = 0; r < 4; r++) {
			resultVec[resultOff + r] = lhsMat[lhsOff + r] * x
					+ lhsMat[lhsOff + 4 + r] * y
					+ lhsMat[lhsOff + 8 + r] * z
					+ lhsMat[lhsOff + 12 + r] * w;
		}
	}

	/** The usual off-axis perspective frustum. near and far must both be positive. */
	public static void frustumM(float[] m, int off, float left, float right, float bottom, float top, float near, float far) {
		float rWidth = 1.0f / (right - left);
		float rHeight = 1.0f / (top - bottom);
		float rDepth = 1.0f / (near - far);
		float x = 2.0f * near * rWidth;
		float y = 2.0f * near * rHeight;
		float A = (right + left) * rWidth;
		float B = (top + bottom) * rHeight;
		float C = (far + near) * rDepth;
		float D = 2.0f * far * near * rDepth;

		m[off + 0] = x;    m[off + 4] = 0.0f; m[off + 8]  = A;     m[off + 12] = 0.0f;
		m[off + 1] = 0.0f; m[off + 5] = y;    m[off + 9]  = B;     m[off + 13] = 0.0f;
		m[off + 2] = 0.0f; m[off + 6] = 0.0f; m[off + 10] = C;     m[off + 14] = D;
		m[off + 3] = 0.0f; m[off + 7] = 0.0f; m[off + 11] = -1.0f; m[off + 15] = 0.0f;
	}

	/** A view matrix placing the eye at (eyeX, eyeY, eyeZ) looking at the centre point. */
	public static void setLookAtM(float[] m, int off,
			float eyeX, float eyeY, float eyeZ,
			float centerX, float centerY, float centerZ,
			float upX, float upY, float upZ) {

		// forward = normalise(center - eye)
		float fx = centerX - eyeX, fy = centerY - eyeY, fz = centerZ - eyeZ;
		float rlf = 1.0f / length(fx, fy, fz);
		fx *= rlf; fy *= rlf; fz *= rlf;

		// side = normalise(forward x up)
		float sx = fy * upZ - fz * upY;
		float sy = fz * upX - fx * upZ;
		float sz = fx * upY - fy * upX;
		float rls = 1.0f / length(sx, sy, sz);
		sx *= rls; sy *= rls; sz *= rls;

		// trueUp = side x forward  (re-orthogonalised, so up need not be exact)
		float ux = sy * fz - sz * fy;
		float uy = sz * fx - sx * fz;
		float uz = sx * fy - sy * fx;

		m[off + 0] = sx;  m[off + 4] = sy;  m[off + 8]  = sz;  m[off + 12] = -(sx * eyeX + sy * eyeY + sz * eyeZ);
		m[off + 1] = ux;  m[off + 5] = uy;  m[off + 9]  = uz;  m[off + 13] = -(ux * eyeX + uy * eyeY + uz * eyeZ);
		m[off + 2] = -fx; m[off + 6] = -fy; m[off + 10] = -fz; m[off + 14] = fx * eyeX + fy * eyeY + fz * eyeZ;
		m[off + 3] = 0.0f; m[off + 7] = 0.0f; m[off + 11] = 0.0f; m[off + 15] = 1.0f;
	}

	private static float length(float x, float y, float z) {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	private Mat4() {
	}
}
