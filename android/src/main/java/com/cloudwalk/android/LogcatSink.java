package com.cloudwalk.android;

import com.cloudwalk.platform.Log;

/** Routes the engine's logging back to logcat. */
public final class LogcatSink implements Log.Sink {

	public static void install() {
		Log.setSink(new LogcatSink());
	}

	public void write(int level, String tag, String msg, Throwable t) {
		switch (level) {
		case Log.DEBUG:
			if (t != null) {
				android.util.Log.d(tag, msg, t);
			} else {
				android.util.Log.d(tag, msg);
			}
			break;
		case Log.WARN:
			if (t != null) {
				android.util.Log.w(tag, msg, t);
			} else {
				android.util.Log.w(tag, msg);
			}
			break;
		case Log.ERROR:
			if (t != null) {
				android.util.Log.e(tag, msg, t);
			} else {
				android.util.Log.e(tag, msg);
			}
			break;
		default:
			if (t != null) {
				android.util.Log.i(tag, msg, t);
			} else {
				android.util.Log.i(tag, msg);
			}
			break;
		}
	}
}
