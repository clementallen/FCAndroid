package com.cloudwalk.harness;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import com.cloudwalk.platform.Prefs;
import com.cloudwalk.startup.ModelEnv;

/**
 * A ModelEnv with no platform behind it: no sound, no dialogs, default
 * settings, and assets from whatever {@link AssetSource} it is given.
 *
 * Used by the trace harness to run the simulation with nothing but the
 * simulation in play, so that a run on the JVM and the same run compiled to
 * JavaScript can be compared directly.
 */
public class HeadlessEnv implements ModelEnv {

	private final AssetSource assets;
	private final Prefs prefs;
	private String task;
	private int pilotType;
	private final int[] typeNums;

	public HeadlessEnv(AssetSource assets, String task, int pilotType, int[] typeNums) {
		this.assets = assets;
		this.task = task;
		this.pilotType = pilotType;
		this.typeNums = typeNums;
		this.prefs = new DefaultPrefs();
	}

	public InputStream openFile(String s) {
		String text = assets.read(s);
		if (text == null) {
			throw new RuntimeException("no such asset: " + s);
		}
		try {
			return new ByteArrayInputStream(text.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public String getTask() {
		return task;
	}

	public void setTask(String task) {
		this.task = task;
	}

	public int getPilotType() {
		return pilotType;
	}

	public void setPilotType(int pilotType) {
		this.pilotType = pilotType;
	}

	/** Null means a solo game, which is the only thing the harness runs. */
	public String getHostPort() {
		return null;
	}

	public int[] getTypeNums() {
		return typeNums;
	}

	public void play(float pitch, int index, int loop) {
	}

	public Prefs getPrefs() {
		return prefs;
	}

	public void sendMessage(String msg) {
	}

	public void showDialog(String title, String msg) {
	}

	/** Every setting at its default, so a trace does not depend on saved state. */
	public static class DefaultPrefs implements Prefs {
		public String getString(String key, String defValue) {
			return defValue;
		}

		public int getInt(String key, int defValue) {
			return defValue;
		}

		public boolean getBoolean(String key, boolean defValue) {
			return defValue;
		}

		public float getFloat(String key, float defValue) {
			return defValue;
		}
	}
}
