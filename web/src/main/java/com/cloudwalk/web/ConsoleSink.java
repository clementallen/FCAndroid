package com.cloudwalk.web;

import org.teavm.jso.JSBody;

import com.cloudwalk.platform.Log;

/** Routes the engine's logging to the browser console. */
public final class ConsoleSink implements Log.Sink {

	public void write(int level, String tag, String msg, Throwable t) {
		String line = tag + ": " + msg;
		if (t != null) {
			line = line + " (" + t + ")";
		}
		if (level >= Log.ERROR) {
			error(line);
		} else if (level >= Log.WARN) {
			warn(line);
		} else {
			// The engine logs its whole task load at INFO. console.debug keeps
			// it available in devtools without burying everything else.
			debug(line);
		}
	}

	@JSBody(params = "s", script = "console.debug(s);")
	private static native void debug(String s);

	@JSBody(params = "s", script = "console.warn(s);")
	private static native void warn(String s);

	@JSBody(params = "s", script = "console.error(s);")
	private static native void error(String s);
}
