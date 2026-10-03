package com.cloudwalk.platform;

/**
 * A line-oriented link to the game server.
 *
 * The multiplayer protocol is newline-delimited ASCII - "TIME: 1234.5",
 * "+HELLO: 7#t001:1", "#x:y:z:vx:vy:iP:turn:t" - which the Android app carries
 * over a TCP socket and a browser would carry over a WebSocket. The protocol
 * is identical either way, so XCNet owns all of it and knows nothing about how
 * the bytes travel.
 *
 * Opening a link is the platform's job: see ModelEnv.openNetLink.
 */
public interface NetLink {

	/** Receives one line, newline already stripped. */
	interface Listener {
		void onLine(String line);
	}

	/** Sends one line. The link appends whatever delimiter it needs. */
	void send(String line);

	/** Closes the link. Safe to call more than once. */
	void close();
}
