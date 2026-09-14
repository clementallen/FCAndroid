package com.cloudwalk.harness;

import com.cloudwalk.client.Glider;
import com.cloudwalk.client.XCModelViewer;
import com.cloudwalk.framework3d.ModelView;
import com.cloudwalk.framework3d.Obj3dStatic;
import com.cloudwalk.platform.Log;
import com.cloudwalk.platform.Now;
import com.cloudwalk.platform.Rnd;

/**
 * Runs the simulation headless and writes down where the glider went.
 *
 * This project has no test suite, and a port moves twelve thousand lines of
 * flight physics across a compiler boundary. A trace turns "does the web build
 * fly the same?" from a judgement call into a diff: run this on the JVM, run
 * the same thing compiled to JavaScript, compare the text.
 *
 * Everything that would otherwise vary run to run is pinned first - both the
 * day seed behind thermal layout and the unseeded stream - so a difference in
 * the output means a difference in the simulation.
 */
public final class Trace {

	/** Frames to run before sampling starts, to get past launch. */
	public static final int WARMUP = 50;

	public static String run(AssetSource assets, String task, int pilotType, int frames, int sampleEvery) {
		Rnd.pin(20000L, 12345L);
		Now.Virtual vclock = new Now.Virtual(0L);
		Now.setSource(vclock);
		// Silence the engine: task descriptions are logged over several lines,
		// so a trace is only cleanly comparable if nothing else is writing.
		Log.setSink(new Log.Sink() {
			public void write(int level, String tag, String msg, Throwable t) {
			}
		});
		try {
			// The static world geometry is global mutable state, reset by the
			// platform before each game. Do the same here so repeated runs in
			// one process do not accumulate.
			Obj3dStatic.init();

			ModelView view = new ModelView();
			view.setSize(1280, 720);

			XCModelViewer mv = new XCModelViewer(view);
			view.modelViewer = mv;
			mv.init(new HeadlessEnv(assets, task, pilotType, new int[] { 1, 1, 1, 1 }));
			mv.start();

			// start() leaves the model in demo mode, watching the AI gliders.
			// This is what the Start button does (StartFlightClub:410): launch
			// the user's glider and point the camera at it.
			mv.xcModel.start(pilotType);

			StringBuilder out = new StringBuilder();
			out.append("task=").append(task).append(" pilot=").append(pilotType)
					.append(" frames=").append(frames).append('\n');

			// Model time is wall-clock derived, so the virtual clock advances
			// exactly one frame per iteration. Otherwise the trace would
			// depend on how fast the machine happened to be.
			int frameMs = 1000 / 20;
			for (int i = 0; i < frames; i++) {
				long now = vclock.advance(frameMs);
				mv.clock.pump(null, now);
				if (i >= WARMUP && (i - WARMUP) % sampleEvery == 0) {
					out.append(sample(i, mv));
				}
			}
			return out.toString();
		} finally {
			Rnd.unpin();
			Now.reset();
			Log.setSink(null);
		}
	}

	private static String sample(int frame, XCModelViewer mv) {
		Glider g = mv.xcModel.gliderManager.gliderUser;
		// getFocus() is the position, and is public where the field is not
		float[] p = g.getFocus();
		StringBuilder b = new StringBuilder();
		b.append(frame).append(' ')
				.append(fmt(p[0])).append(' ').append(fmt(p[1])).append(' ').append(fmt(p[2])).append(' ')
				.append(fmt(g.v[0])).append(' ').append(fmt(g.v[1])).append(' ')
				.append(fmt(g.getSink())).append(' ').append(fmt(g.airv))
				.append('\n');
		return b.toString();
	}

	/**
	 * Fixed four decimals. Java and JavaScript disagree about how to print a
	 * float in the last place or two, and that is not the kind of difference
	 * this is looking for.
	 */
	private static String fmt(float f) {
		long scaled = Math.round((double) f * 10000.0);
		boolean neg = scaled < 0;
		if (neg) {
			scaled = -scaled;
		}
		String digits = Long.toString(scaled);
		while (digits.length() < 5) {
			digits = "0" + digits;
		}
		String s = digits.substring(0, digits.length() - 4) + "." + digits.substring(digits.length() - 4);
		return neg ? "-" + s : s;
	}

	private Trace() {
	}
}
