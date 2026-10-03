package com.cloudwalk.platform;

/**
 * The engine's sources of randomness, in one place so they can be pinned.
 *
 * Two things here matter and they are different things.
 *
 * {@link #daySeed()} is the seed behind thermal and scenery layout. It changes
 * once a day and is otherwise fixed, which is deliberate: every player in a
 * network game seeds from it and so gets the same sky. Do not make it vary per
 * run.
 *
 * {@link #random()} is the genuinely unseeded stream, used where the engine
 * wants a throwaway value. By default it is Math.random(), exactly as the call
 * sites used to call directly.
 *
 * {@link #pin} makes both reproducible, which is what lets the headless trace
 * harness compare a run on the JVM against the same run compiled to
 * JavaScript. Production never calls it.
 */
public final class Rnd {

	public interface Source {
		double next();
	}

	private static final Source SYSTEM = new Source() {
		public double next() {
			return Math.random();
		}
	};

	private static long daySeed = System.currentTimeMillis() / 1000 / 60 / 60 / 24;
	private static Source source = SYSTEM;

	/** Days since the epoch: the same for everyone flying together today. */
	public static long daySeed() {
		return daySeed;
	}

	public static double random() {
		return source.next();
	}

	/** Makes both streams reproducible. For harnesses and tests only. */
	public static void pin(long daySeed, final long streamSeed) {
		Rnd.daySeed = daySeed;
		final java.util.Random r = new java.util.Random(streamSeed);
		source = new Source() {
			public double next() {
				return r.nextDouble();
			}
		};
	}

	/** Restores the real clock and the system random stream. */
	public static void unpin() {
		daySeed = System.currentTimeMillis() / 1000 / 60 / 60 / 24;
		source = SYSTEM;
	}

	private Rnd() {
	}
}
