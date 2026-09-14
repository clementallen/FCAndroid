package com.cloudwalk.platform;

/**
 * A single touch or mouse event, flattened.
 *
 * Replaces android.view.MotionEvent plus the View the old signatures carried
 * alongside it purely to ask for its size. The engine only ever wanted four
 * numbers and a phase, so that is what this is.
 *
 * Mutable and meant to be reused - one instance per input source, refilled per
 * event, so a busy drag does not allocate.
 */
public final class PointerEvent {

	public static final int DOWN = 0;
	public static final int MOVE = 1;
	public static final int UP = 2;

	public int action;
	public float x;
	public float y;
	public int viewWidth;
	public int viewHeight;

	public void set(int action, float x, float y, int viewWidth, int viewHeight) {
		this.action = action;
		this.x = x;
		this.y = y;
		this.viewWidth = viewWidth;
		this.viewHeight = viewHeight;
	}
}
