package com.cloudwalk.platform;

/**
 * Wall-clock time, in one place so it can be replaced.
 *
 * Model time is derived from the real clock, which means the engine's output
 * depends on when it ran and how fast the machine was. That is fine in a game
 * and useless in a trace, so the harness swaps in a virtual clock it advances
 * by hand and gets the same answer every time.
 *
 * Production leaves this alone and reads the real clock.
 */
public final class Now {

	public interface Source {
		long millis();
	}

	private static final Source SYSTEM = new Source() {
		public long millis() {
			return System.currentTimeMillis();
		}
	};

	private static Source source = SYSTEM;

	public static long millis() {
		return source.millis();
	}

	/** For harnesses and tests only. */
	public static void setSource(Source s) {
		source = (s == null) ? SYSTEM : s;
	}

	public static void reset() {
		source = SYSTEM;
	}

	/** A clock that only moves when told to. */
	public static final class Virtual implements Source {
		private long millis;

		public Virtual(long start) {
			this.millis = start;
		}

		public long millis() {
			return millis;
		}

		public long advance(long delta) {
			millis += delta;
			return millis;
		}
	}

	private Now() {
	}
}
