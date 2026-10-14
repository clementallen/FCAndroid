/*
  @(#)Clock.java (part of 'Flight Club')
	
  This code is covered by the GNU General Public License
  detailed at http://www.gnu.org/copyleft/gpl.html
	
  Flight Club docs located at http://www.danb.dircon.co.uk/hg/hg.htm
  Copyright 2001-2002 Dan Burton <danb@dircon.co.uk>
 */
package com.cloudwalk.framework3d;

import java.util.Vector;

import com.cloudwalk.platform.Log;
import com.cloudwalk.platform.Now;

import com.cloudwalk.client.Trigger;
import com.cloudwalk.client.XCModelViewer;

/**
 * This class implements the clock that manages model time. The clock runs on its own thread and maintains a list of observers. Each time round the run loop it
 * calls the tick method of each of its observers. The frame rate starts out as 25 but it may go up or down depending on how long it takes to execute all the
 * observer's tick methods. The clock keeps track of the *model* time.
 */
public class Clock {

	private boolean running = false;
	int sleepTime;
	Vector<ClockObserver> observers = new Vector<ClockObserver>();

	private long currentTick = 0;
	long tickCount = 0;

	// for tuning the tick rate
	private long busyTime = 0;
	private long nextFrameDueMs = 0;
	private long blockStart;
	private float modelTime;
	private int frameRate;
	private float modelTimePerFrame;

	public boolean paused = false;
	public boolean speedy = false; // Flag true => speed model time up by a
									// factor of 10

	private static final int INIT_RATE = 20;
	public static int MAX_RATE = 30;
	private static final int BLOCK = 2; // how often do we review the frame rate
										// ?
	private static final float MODEL_TIME_PER_SECOND = 1.0f; // units of model
																// time per
																// second
	private static final float MODEL_TIME_PER_TICK = MODEL_TIME_PER_SECOND / 1000f;
	/** Stop trying to catch up on missed frames beyond this far behind. */
	private static final long CATCHUP_LIMIT_MS = 250;
	private static final float IDLE_PERCENT_MIN = 0.05f; // idle for at least 5%
															// of the time - do
															// not
															// thrash the CPU

	/*
	 * Creates the clock. Pass in the current model time. This will be zero if you are not connected to a game server, otherwise it will be the current model
	 * time as defined on the server.
	 */
	public Clock(float modelTime) {
		synchTime(modelTime);
		setFrameRate(INIT_RATE);
	}

	public void addObserver(ClockObserver observer) {
		observers.addElement(observer);
	}

	public void removeObserver(ClockObserver observer) {
		observers.removeElement(observer);
	}

	public void start() {
		running = true;
		blockStart = currentTick = nextFrameDueMs = Now.millis();
		modelTime = getTimeNow();
		// for (int i = 0; i < observers.size(); i++) {
		// ClockObserver observer = (ClockObserver) observers.elementAt(i);
		// if (observer instanceof Trigger) {
		// Trigger t = ((Trigger) observer);
		// if (t.mode == Trigger.SLEEPING)
		// t.wakeUp(modelTime);
		// }
		// }
	}

	public void stop() {
		running = false;
		modelTime = getTimeNow();
		// for (int i = 0; i < observers.size(); i++) {
		// ClockObserver observer = (ClockObserver) observers.elementAt(i);
		// if (observer instanceof Trigger) {
		// ((Trigger) observer).sleep(modelTime);
		// }
		// }
	}

	float t, _t = 0, dt;

	/**
	 * Runs at most one frame, then says how long the caller should wait before
	 * asking again.
	 *
	 * This used to block on Thread.sleep at the end of every frame, which a
	 * browser cannot do - the render loop there is a callback, not a thread we
	 * own. So pacing is a deadline the caller honours however it likes:
	 * requestAnimationFrame simply calls back often and gets 0 returned when a
	 * frame is due, while Android sleeps for the returned interval exactly as
	 * it used to.
	 *
	 * @param renderer what to draw with, or null to simulate without drawing
	 * @param nowMs wall clock, passed in so the caller and the clock agree
	 * @return 0 if a frame ran, otherwise millis until the next one is due
	 */
	public long pump(ModelViewRenderer renderer, long nowMs) {
		if (!running) {
			return sleepTime;
		}

		// Waiting on the server's first TIME message - no model time yet, so
		// there is nothing meaningful to simulate or draw.
		XCModelViewer mv = (XCModelViewer) observers.elementAt(0);
		if (mv.netFlag && !mv.netTimeFlag) {
			return 10;
		}

		long due = nextFrameDueMs - nowMs;
		if (due > 0) {
			return due;
		}

		currentTick = nowMs;
		tickCount++;

		modelTime = t = getTimeNow();

		if (_t == 0) {
			dt = modelTimePerFrame;
		} else {
			dt = t - _t;
		}

		for (int i = 0; i < observers.size(); i++) {
			// when paused still tick the modelviewer so
			// we can change our POV and *un*pause !
			if (i == 0 || !paused) {
				ClockObserver c = (ClockObserver) observers.elementAt(i);
				try {
					if (paused) {
						if (((ModelViewer) c).modelView.dragging) {
							c.tick(modelTime, modelTimePerFrame);
						}
					} else
						c.tick(modelTime, modelTimePerFrame);
				} catch (Exception e) {
					Log.e("FC", e.getMessage(), e);
				}
			}
		}
		if (renderer != null) {
			renderer.drawEverything();
		}

		long after = Now.millis();
		busyTime += after - nowMs;

		// Advance the deadline rather than resetting it, so frame times do not
		// drift. If we have fallen badly behind - a backgrounded tab, a long
		// stall - give up on catching up and restart from now.
		nextFrameDueMs += sleepTime;
		if (nextFrameDueMs < after - CATCHUP_LIMIT_MS) {
			nextFrameDueMs = after;
		}

		// check frame rate every so often
		if (tickCount % BLOCK == 0) {
			reviewRate(after);
		}
		_t = t;
		return 0;
	}

	/**
	 * Re-pegs model time to now without the smoothing synchTime does.
	 *
	 * Model time runs off the wall clock, but the render loop does not run at
	 * all while a browser tab is hidden. Coming back after a minute away would
	 * otherwise jump model time a minute forward while every object sits where
	 * it was, and the world lurches. Call this when the page becomes visible
	 * again.
	 */
	public void reanchor() {
		modelTimeAtSync = modelTime;
		realTimeAtSync = Now.millis();
		nextFrameDueMs = realTimeAtSync;
		_t = 0;
	}

	/* Returns the current model time as defined by the run loop (discrete). */
	public final float getTime() {
		return modelTime;
	}

	/* Returns the current model time right now (continuous). */
	public final float getTimeNow() {
		return (Now.millis() - realTimeAtSync) * MODEL_TIME_PER_TICK + modelTimeAtSync;
	}

	private long realTimeAtSync;
	private float modelTimeAtSync;

	/*
	 * Synchronises this client's copy of the model time with the master time held on the game server.
	 */
	public void synchTime(float t) {
		if (Math.abs(t - getTimeNow()) > 1)
			modelTimeAtSync = t;
		else
			modelTimeAtSync = t * .1f + getTimeNow() * .9f;
		realTimeAtSync = Now.millis();
		Log.w("FC Clock", "Synctime:" + t + " Modeltime diff:" + (getTimeNow() - modelTime));
		modelTime = getTimeNow();
	}

	private void setFrameRate(int r) {
		frameRate = r;
		sleepTime = 1000 / frameRate;
		modelTimePerFrame = MODEL_TIME_PER_SECOND / frameRate;
		if (speedy)
			modelTimePerFrame *= 10;
	}

	/*
	 * Tune the frame rate up or down depending on how long we have been idle over the last N ticks.
	 */
	void reviewRate(long t) {
		long elapsed = t - blockStart;
		float idlePercent = elapsed > 0 ? 1f - (float) busyTime / elapsed : 1f;

		if (idlePercent < IDLE_PERCENT_MIN && frameRate > 2) {
			// working too hard so slow down
			setFrameRate(frameRate - 2);
			// Log.w("FC", "slowing down");
		} else if (frameRate < MAX_RATE) {
			setFrameRate(frameRate + 1);
		}

		// re init vars
		busyTime = 0;
		blockStart = t;
	}

	/** Gets the current frame rate. */
	public final int getFrameRate() {
		return frameRate;
	}
}
