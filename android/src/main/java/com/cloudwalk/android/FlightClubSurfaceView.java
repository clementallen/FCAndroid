package com.cloudwalk.android;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.cloudwalk.framework3d.ModelView;
import com.cloudwalk.framework3d.ModelViewRenderer;
import com.cloudwalk.gl.GL;
import com.cloudwalk.platform.PointerEvent;

/**
 * The Android surface the game draws into.
 *
 * ModelView used to be this class. It is now a plain object holding the
 * viewport's size, the drag gesture and the status lines, and this wraps it -
 * which is what keeps the Android view system out of the engine. The renderer
 * and the clock are driven from here in exactly the order they were before:
 * GLSurfaceView's continuous mode calls onDrawFrame, that runs a frame, and
 * the frame pacing that used to be a Thread.sleep inside the clock is now the
 * interval the clock hands back.
 */
public class FlightClubSurfaceView extends GLSurfaceView {

	public final ModelView modelView = new ModelView();
	private ModelViewRenderer renderer;
	private final PointerEvent pointer = new PointerEvent();

	public FlightClubSurfaceView(Context context) {
		super(context);
	}

	public FlightClubSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/** Installs the renderer. Call after setEGLContextClientVersion(2). */
	public void setModelRenderer(ModelViewRenderer renderer) {
		this.renderer = renderer;
		setKeepScreenOn(true);
		setRenderer(new Callbacks());
	}

	private final class Callbacks implements GLSurfaceView.Renderer {

		private final GL gl = new Gles20GL();

		public void onSurfaceCreated(GL10 unused, EGLConfig config) {
			renderer.onSurfaceCreated(gl);
		}

		public void onSurfaceChanged(GL10 unused, int width, int height) {
			modelView.setSize(width, height);
			renderer.onSurfaceChanged(width, height);
		}

		public void onDrawFrame(GL10 unused) {
			if (renderer.modelViewer == null) {
				return;
			}
			long wait = renderer.modelViewer.clock.pump(renderer, System.currentTimeMillis());
			if (wait > 0) {
				// The clock used to sleep here itself. It cannot any more - a
				// browser's render loop is a callback - so it says how long to
				// wait and we honour it, which keeps the frame rate exactly
				// where it was rather than spinning at vsync.
				try {
					Thread.sleep(wait);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	/** Translates a touch into the engine's platform-free pointer event. */
	public PointerEvent toPointerEvent(MotionEvent event) {
		int action;
		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			action = PointerEvent.DOWN;
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			action = PointerEvent.UP;
			break;
		default:
			action = PointerEvent.MOVE;
			break;
		}
		pointer.set(action, event.getX(), event.getY(), getWidth(), getHeight());
		return pointer;
	}
}
