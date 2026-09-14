/*
 * @(#)ModelEnv.java (part of 'Flight Club')
 *
 * This code is covered by the GNU General Public License
 * detailed at http://www.gnu.org/copyleft/gpl.html
 *
 * Flight Club docs located at http://www.danb.dircon.co.uk/hg/hg.htm
 * Copyright 2001-2003 Dan Burton <danb@dircon.co.uk>
 */
package com.cloudwalk.startup;

import java.io.InputStream;

import com.cloudwalk.platform.NetLink;
import com.cloudwalk.platform.Prefs;

/**
 * The host environment a ModelViewer runs inside.
 *
 * Originally written so the engine could run in either an applet or a frame;
 * it since acquired Android types, which put the whole platform on the
 * engine's classpath. Those are gone again - this interface is now pure Java,
 * and the Android app and the browser each implement it.
 */
public interface ModelEnv {

	/** Opens a bundled data file - a .task or a glider .txt. */
	InputStream openFile(String s);

	String getTask();

	int getPilotType();

	void setPilotType(int pilotType);

	void setTask(String task);

	/** "host:port" of the game server, or null for a solo game. */
	String getHostPort();

	/**
	 * Opens a link to the game server, delivering each received line to
	 * {@code listener}. Android carries this over a TCP socket, a browser over
	 * a WebSocket. Returns null if this platform has no multiplayer transport.
	 */
	NetLink openNetLink(String hostPort, NetLink.Listener listener);

	int[] getTypeNums();

	/** Plays sound {@code index} at {@code pitch}; loop -1 repeats forever. */
	void play(float pitch, int index, int loop);

	Prefs getPrefs();

	/** A transient message for the player - a toast, or a line of HUD. */
	void sendMessage(String msg);

	/** A modal notice the player has to acknowledge. */
	void showDialog(String title, String msg);
}
