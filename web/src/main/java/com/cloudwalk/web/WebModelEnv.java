package com.cloudwalk.web;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import com.cloudwalk.harness.AssetSource;
import com.cloudwalk.platform.NetLink;
import com.cloudwalk.platform.Prefs;
import com.cloudwalk.startup.ModelEnv;

/**
 * The browser as a host environment for the engine.
 *
 * Assets come from {@link BakedAssets}, which is generated at build time - so
 * openFile stays synchronous and the StreamTokenizer parsers need no changes.
 * Settings are localStorage. Messages and dialogs go out to the TypeScript
 * shell, which owns everything the player actually sees outside the canvas.
 */
public final class WebModelEnv implements ModelEnv {

	/** What the shell needs to hear about. */
	public interface Host {
		void message(String msg);

		void dialog(String title, String msg);
	}

	private final AssetSource assets = new BakedAssets();
	private final Prefs prefs = new WebPrefs();
	private final WebSounds sounds;
	private final Host host;

	private String task;
	private int pilotType;
	private int[] typeNums;

	public WebModelEnv(WebSounds sounds, Host host, String task, int pilotType, int[] typeNums) {
		this.sounds = sounds;
		this.host = host;
		this.task = task;
		this.pilotType = pilotType;
		this.typeNums = typeNums;
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

	/** Solo only for now. Multiplayer arrives as a WebSocket NetLink. */
	public String getHostPort() {
		return null;
	}

	public NetLink openNetLink(String hostPort, NetLink.Listener listener) {
		return null;
	}

	public int[] getTypeNums() {
		return typeNums;
	}

	public void play(float pitch, int index, int loop) {
		sounds.play(pitch, index, loop);
	}

	public Prefs getPrefs() {
		return prefs;
	}

	public void sendMessage(String msg) {
		host.message(msg);
	}

	public void showDialog(String title, String msg) {
		host.dialog(title, msg);
	}
}
