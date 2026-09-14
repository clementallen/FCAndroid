package com.cloudwalk.web;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.webgl.WebGLRenderingContext;

import com.cloudwalk.client.Glider;
import com.cloudwalk.client.XCCameraMan;
import com.cloudwalk.client.XCModelViewer;
import com.cloudwalk.framework3d.ModelView;
import com.cloudwalk.framework3d.ModelViewRenderer;
import com.cloudwalk.framework3d.Obj3dStatic;
import com.cloudwalk.gl.GL;
import com.cloudwalk.platform.Log;
import com.cloudwalk.platform.PointerEvent;

/**
 * What the TypeScript shell talks to.
 *
 * Deliberately narrow. The shell owns the menus, the HUD, the settings panel
 * and the frame callback; the engine owns the simulation and the canvas. This
 * is the whole boundary between them: a handful of commands in, and a HUD
 * snapshot back out.
 */
public class FlightClub {

	/** Called when the engine has something to tell the player. */
	@JSFunctor
	public interface MessageHandler extends JSObject {
		void handle(String text);
	}

	/** Called when the engine wants an acknowledged notice. */
	@JSFunctor
	public interface DialogHandler extends JSObject {
		void handle(String title, String text);
	}

	private XCModelViewer modelViewer;
	private ModelView modelView;
	private ModelViewRenderer renderer;
	private WebSounds sounds;
	private GL gl;
	private HTMLCanvasElement canvas;
	private boolean started = false;
	private boolean surfaceReady = false;

	private final PointerEvent pointer = new PointerEvent();

	private MessageHandler onMessage;
	private DialogHandler onDialog;

	/**
	 * Context attributes with the alpha channel switched off.
	 *
	 * A canvas defaults to alpha:true and premultipliedAlpha:true, so any
	 * fragment the shader emits with alpha below 1 gets composited against the
	 * page behind the canvas - and since the shader writes straight, not
	 * premultiplied, colour, the browser under-weights it and the page shows
	 * through. COLOR_SHADOW is argb(128, 220, 220, 220), the one translucent
	 * colour in the engine, which is exactly why ground shadows washed out to
	 * white here and not on Android: a GLSurfaceView has no alpha channel to
	 * composite with, so it simply discards that alpha. This makes the browser
	 * behave the same way.
	 */
	@JSBody(script = "return { alpha: false };")
	private static native JSObject opaqueContext();

	@JSExport
	public FlightClub() {
	}

	@JSExport
	public void setMessageHandler(MessageHandler h) {
		this.onMessage = h;
	}

	@JSExport
	public void setDialogHandler(DialogHandler h) {
		this.onDialog = h;
	}

	/**
	 * Builds a game on the given canvas and leaves it in demo mode, watching
	 * the AI gliders, exactly as the Android app does before Start is pressed.
	 *
	 * @param soundBase URL prefix the mp3s live under, e.g. "sounds/"
	 */
	@JSExport
	public void start(String canvasId, String task, int pilotType, int aiCount, String soundBase) {
		canvas = (HTMLCanvasElement) HTMLDocument.current().getElementById(canvasId);
		if (canvas == null) {
			throw new RuntimeException("no canvas with id " + canvasId);
		}

		WebGLRenderingContext ctx = (WebGLRenderingContext) canvas.getContext("webgl", opaqueContext());
		if (ctx == null) {
			ctx = (WebGLRenderingContext) canvas.getContext("experimental-webgl", opaqueContext());
		}
		if (ctx == null) {
			throw new RuntimeException("this browser has no WebGL");
		}
		gl = new WebGL(ctx);
		sounds = new WebSounds(soundBase);

		// Static world geometry is global and is rebuilt per game.
		Obj3dStatic.init();

		modelView = new ModelView();
		modelView.setSize(canvas.getWidth(), canvas.getHeight());

		modelViewer = new XCModelViewer(modelView);
		modelView.modelViewer = modelViewer;

		int[] typeNums = new int[] { aiCount, aiCount, aiCount, aiCount };
		WebModelEnv env = new WebModelEnv(sounds, new WebModelEnv.Host() {
			public void message(String msg) {
				if (onMessage != null) {
					onMessage.handle(msg);
				}
			}

			public void dialog(String title, String msg) {
				if (onDialog != null) {
					onDialog.handle(title, msg);
				}
			}
		}, task, pilotType, typeNums);

		renderer = new ModelViewRenderer(env.getPrefs());
		renderer.modelViewer = modelViewer;

		modelViewer.init(env);
		modelViewer.start();

		renderer.onSurfaceCreated(gl);
		renderer.onSurfaceChanged(canvas.getWidth(), canvas.getHeight());
		surfaceReady = true;
		started = true;
	}

	/** The Start button: launches the player's glider. */
	@JSExport
	public void launch(int pilotType) {
		if (started) {
			modelViewer.xcModel.start(pilotType);
		}
	}

	/**
	 * Runs a frame if one is due.
	 *
	 * Called from requestAnimationFrame, which fires far more often than the
	 * game's 20-25Hz, so most calls do nothing and return early. The clock
	 * decides; this just asks.
	 *
	 * Deliberately takes no timestamp. requestAnimationFrame hands out
	 * milliseconds since the page loaded, while model time is derived from the
	 * epoch - mixing the two leaves every frame looking billions of
	 * milliseconds early, and the game simply never starts.
	 */
	@JSExport
	public void pump() {
		if (started && surfaceReady) {
			modelViewer.clock.pump(renderer, System.currentTimeMillis());
		}
	}

	@JSExport
	public void setSize(int width, int height) {
		if (canvas == null) {
			return;
		}
		canvas.setWidth(width);
		canvas.setHeight(height);
		modelView.setSize(width, height);
		if (surfaceReady) {
			renderer.onSurfaceChanged(width, height);
		}
	}

	/**
	 * Model time runs off the wall clock but the render loop stops while a tab
	 * is hidden. Call this when it becomes visible again, or the world lurches
	 * forward by however long the player was away.
	 */
	@JSExport
	public void reanchor() {
		if (started) {
			modelViewer.clock.reanchor();
		}
	}

	// --- input ---

	@JSExport
	public void pointerDown(double x, double y) {
		dispatch(PointerEvent.DOWN, x, y);
	}

	@JSExport
	public void pointerMove(double x, double y) {
		dispatch(PointerEvent.MOVE, x, y);
	}

	@JSExport
	public void pointerUp(double x, double y) {
		dispatch(PointerEvent.UP, x, y);
	}

	private void dispatch(int action, double x, double y) {
		if (!started) {
			return;
		}
		pointer.set(action, (float) x, (float) y, modelView.getWidth(), modelView.getHeight());
		modelViewer.xcModel.gliderManager.gliderUser.handleTouch(pointer);
		modelView.handleTouch(pointer);
	}

	/** -1 left, 0 straight, 1 right. The keyboard's route into the glider. */
	@JSExport
	public void steer(int direction) {
		if (started) {
			modelViewer.xcModel.gliderManager.gliderUser.steer(direction);
		}
	}

	@JSExport
	public void faster() {
		if (started) {
			modelViewer.xcModel.gliderManager.gliderUser.goFaster();
		}
	}

	@JSExport
	public void slower() {
		if (started) {
			modelViewer.xcModel.gliderManager.gliderUser.goSlower();
		}
	}

	// --- camera and clock ---

	@JSExport
	public void setCameraMode(int mode) {
		if (started) {
			modelViewer.xcModel.xcCameraMan.setMode(mode);
		}
	}

	@JSExport
	public void zoomIn() {
		if (started) {
			modelViewer.xcModel.xcCameraMan.pullIn();
		}
	}

	@JSExport
	public void zoomOut() {
		if (started) {
			modelViewer.xcModel.xcCameraMan.pullOut();
		}
	}

	@JSExport
	public void togglePause() {
		setPaused(!isPaused());
	}

	/**
	 * Freezes or resumes the world.
	 *
	 * Silences the looping sounds and re-pegs model time on resume; see
	 * XCModel.setPaused, which Android's pause button goes through too.
	 */
	@JSExport
	public void setPaused(boolean paused) {
		// Not gated on `started`: the engine raises the task briefing from
		// inside start(), before that flag is set, and freezing the world is
		// precisely what has to happen at that moment.
		if (modelViewer != null && modelViewer.xcModel != null) {
			modelViewer.xcModel.setPaused(paused);
		}
	}

	@JSExport
	public boolean isPaused() {
		return started && modelViewer.clock.paused;
	}

	// --- audio ---

	/** Must be called from a user gesture, or the browser refuses to start audio. */
	@JSExport
	public void resumeAudio() {
		if (sounds != null) {
			sounds.resume();
		}
	}

	@JSExport
	public void setMuted(boolean muted) {
		if (sounds != null) {
			sounds.setMuted(muted);
		}
	}

	// --- HUD readback, polled by the shell ---

	@JSExport
	public String getInfoText() {
		return started ? modelView.getInfoText() : "";
	}

	@JSExport
	public double getVario() {
		if (!started) {
			return 0;
		}
		Glider g = modelViewer.xcModel.gliderManager.theGlider();
		return g == null ? 0 : g.getSink() + g.airv;
	}

	/** Compass heading in degrees, 0 = north. */
	@JSExport
	public double getHeading() {
		if (!started) {
			return 0;
		}
		Glider g = modelViewer.xcModel.gliderManager.gliderUser;
		return Math.toDegrees(Math.atan2(g.v[0], g.v[1]));
	}

	@JSExport
	public double getSpeed() {
		if (!started) {
			return 0;
		}
		return modelViewer.xcModel.gliderManager.gliderUser.getSpeed();
	}

	@JSExport
	public double getAltitude() {
		if (!started) {
			return 0;
		}
		return modelViewer.xcModel.gliderManager.gliderUser.getFocus()[2];
	}

	@JSExport
	public double getDistanceFlown() {
		return started ? modelViewer.xcModel.gliderManager.gliderUser.distanceFlown : 0;
	}

	@JSExport
	public boolean isOnGround() {
		return started && modelViewer.xcModel.gliderManager.gliderUser.getOnGround();
	}

	@JSExport
	public boolean isFinished() {
		return started && modelViewer.xcModel.gliderManager.gliderUser.finished;
	}

	@JSExport
	public int getFrameRate() {
		return started ? modelViewer.clock.getFrameRate() : 0;
	}

	@JSExport
	public void stop() {
		if (started) {
			modelViewer.stop();
			if (sounds != null) {
				sounds.stopAll();
			}
			started = false;
		}
	}

	/** Camera mode constants, so the shell need not hard-code them. */
	@JSExport
	public static int cameraUser() {
		return XCCameraMan.USER;
	}

	@JSExport
	public static int cameraGaggle() {
		return XCCameraMan.GAGGLE;
	}

	@JSExport
	public static int cameraPlan() {
		return XCCameraMan.PLAN;
	}

	@JSExport
	public static int cameraTask() {
		return XCCameraMan.TASK;
	}

	@JSExport
	public static int cameraPilot() {
		return XCCameraMan.PILOT;
	}

	static {
		Log.setSink(new ConsoleSink());
	}
}
