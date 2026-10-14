package com.cloudwalk.platform;

/**
 * Logging, with the platform behind a sink.
 *
 * Signature-compatible with android.util.Log so call sites port by changing
 * the import. Android installs a sink that forwards to logcat; the web build
 * installs one that forwards to the browser console. Without a sink, output
 * goes to stdout/stderr, which is what the headless harness wants.
 */
public final class Log {

	public static final int DEBUG = 3;
	public static final int INFO = 4;
	public static final int WARN = 5;
	public static final int ERROR = 6;

	public interface Sink {
		void write(int level, String tag, String msg, Throwable t);
	}

	private static final Sink DEFAULT = new Sink() {
		public void write(int level, String tag, String msg, Throwable t) {
			String line = levelChar(level) + "/" + tag + ": " + msg;
			if (level >= WARN) {
				System.err.println(line);
			} else {
				System.out.println(line);
			}
			if (t != null) {
				t.printStackTrace();
			}
		}
	};

	private static Sink sink = DEFAULT;

	public static void setSink(Sink s) {
		sink = (s == null) ? DEFAULT : s;
	}

	public static int d(String tag, String msg) { return write(DEBUG, tag, msg, null); }
	public static int i(String tag, String msg) { return write(INFO, tag, msg, null); }
	public static int w(String tag, String msg) { return write(WARN, tag, msg, null); }
	public static int e(String tag, String msg) { return write(ERROR, tag, msg, null); }

	public static int d(String tag, String msg, Throwable t) { return write(DEBUG, tag, msg, t); }
	public static int i(String tag, String msg, Throwable t) { return write(INFO, tag, msg, t); }
	public static int w(String tag, String msg, Throwable t) { return write(WARN, tag, msg, t); }
	public static int e(String tag, String msg, Throwable t) { return write(ERROR, tag, msg, t); }

	private static int write(int level, String tag, String msg, Throwable t) {
		// Log.e("FC", e.getMessage(), e) is a live pattern here and getMessage()
		// is null for plenty of exceptions - NPEs especially.
		sink.write(level, tag, msg == null ? "null" : msg, t);
		return 0;
	}

	private static char levelChar(int level) {
		switch (level) {
		case DEBUG: return 'D';
		case WARN:  return 'W';
		case ERROR: return 'E';
		default:    return 'I';
		}
	}

	private Log() {
	}
}
