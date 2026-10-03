/*
  @(#)ModelCanvas.java (part of 'Flight Club')

  This code is covered by the GNU General Public License
  detailed at http://www.gnu.org/copyleft/gpl.html

  Flight Club docs located at http://www.danb.dircon.co.uk/hg/hg.htm
  Copyright 2001-2003 Dan Burton <danb@dircon.co.uk>
 */
package com.cloudwalk.framework3d;

import com.cloudwalk.platform.PointerEvent;

/**
 * The viewport the 3d model is drawn into: its size, the drag gesture that
 * orbits the camera, and the three lines of status text.
 *
 * This used to extend GLSurfaceView, which put the whole Android view system
 * on the engine's classpath for the sake of getWidth()/getHeight(). It is now
 * a plain object that each platform drives - Android from its GLSurfaceView,
 * the browser from canvas pointer events.
 *
 * @see ModelViewer
 * @see CameraMan
 */
public class ModelView {

	public ModelViewer modelViewer;
	public boolean dragging = false;

	private int width, height;
	private int x0 = 0, y0 = 0;
	private int dx = 0, dy = 0;
	private float rotationStep = 0;

	public String status1 = "", status2 = "", status3 = "";
	private final StringBuffer infoBuffer = new StringBuffer();

	/**
	 * How far the pointer must be dragged before the camera moves at all.
	 */
	protected static int DRAG_MIN = 20;

	/** Called by the platform whenever the drawing surface is sized or resized. */
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
		DRAG_MIN = height / 15;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void init() {
		// 4 seconds to rotate 90 degrees (at 25hz) - slooow !
		rotationStep = (float) Math.PI / (25 * 4);
		DRAG_MIN = height / 15;
	}

	void tick() {
		/*
		 * Change camera angle if dragging mouse and moused has moved more than the minimum amount
		 */
		float dtheta = 0, dz = 0;
		float zStep;

		if (!dragging)
			return;

		/*
		 * Q. How much do we change camera height by ? A. Depends how far camera is from the focus. Take 3 seconds to move up or down by the same distance that
		 * the camera is from the focus.
		 */
		zStep = modelViewer.cameraMan.getDistance() / (25 * 3);

		if (dx > DRAG_MIN)
			dtheta = -rotationStep;
		if (dx < -DRAG_MIN)
			dtheta = +rotationStep;

		if (dy > DRAG_MIN)
			dz = -zStep;
		if (dy < -DRAG_MIN)
			dz = +zStep;

		if (dtheta != 0 || dz != 0) {
			modelViewer.cameraMan.rotate(dtheta, dz);
		}
	}

	/** Displays some text at the bottom of the screen. */
	public void setText(String s, int line) {
		switch (line) {
		case 0:
			status1 = s;
			break;
		case 1:
			status2 = s;
			break;
		case 2:
			status3 = s;
			break;

		default:
			break;
		}
	}

	public String getInfoText() {
		infoBuffer.setLength(0);
		infoBuffer.append(status1).append("<br/>").append(status2).append("<br/>").append(status3);
		return infoBuffer.toString();
	}

	public void handleTouch(PointerEvent event) {
		if (event.action == PointerEvent.DOWN) {
			x0 = (int) event.x;
			y0 = (int) event.y;
			dragging = true;
		} else if (event.action == PointerEvent.UP) {
			dx = 0;
			dy = 0;
			dragging = false;
		} else if (event.action == PointerEvent.MOVE) {
			dx = (int) (event.x - x0);
			dy = (int) (event.y - y0);
		}
	}
}
