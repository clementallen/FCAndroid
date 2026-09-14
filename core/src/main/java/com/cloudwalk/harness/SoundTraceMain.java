package com.cloudwalk.harness;

import java.io.File;

/**
 * Flies a task and reports what it sounded like.
 *
 * Checks the part of the port that neither a screenshot nor a headless browser
 * can: that the wind starts on launch and stops on landing, that the sink tone
 * comes and goes with the air, and that the variometer still beeps.
 *
 * Usage: SoundTraceMain &lt;assetDir&gt; [task] [pilotType] [frames]
 */
public final class SoundTraceMain {

	public static void main(String[] args) {
		File dir = new File(args.length > 0 ? args[0] : "assets");
		String task = args.length > 1 ? args[1] : "t001";
		int pilotType = args.length > 2 ? Integer.parseInt(args[2]) : 0;
		int frames = args.length > 3 ? Integer.parseInt(args[3]) : 2000;

		AssetSource assets = TraceMain.diskAssets(dir);
		final SoundLog log = new SoundLog(assets, task, pilotType, new int[] { 1, 1, 1, 1 });

		Trace.run(log, task, pilotType, frames, frames + 1, new Trace.Watcher() {
			public void frame(int frame) {
				log.setFrame(frame);
			}
		});

		System.out.println("sound events over " + frames + " frames of " + task + ":");
		String last = "";
		for (SoundLog.Event e : log.events()) {
			// rate changes fire most frames; show transitions, not the stream
			String kind = e.what.split(" ")[0] + " " + e.what.split(" ")[1];
			if (kind.startsWith("rate") && kind.equals(last)) {
				continue;
			}
			last = kind;
			System.out.println("  " + e);
		}

		System.out.println();
		report("wind  play", log.countPlays(1), "stop", log.countStops(1), "rate", log.countRates(1));
		report("sink  play", log.countPlays(7), "stop", log.countStops(7), "rate", log.countRates(7));
		report("beep  play", log.countPlays(0), "stop", log.countStops(0), "rate", log.countRates(0));
		report("landed play", log.countPlays(4), "stop", log.countStops(4), "rate", log.countRates(4));
	}

	private static void report(String a, int an, String b, int bn, String c, int cn) {
		System.out.println("  " + a + "=" + an + "  " + b + "=" + bn + "  " + c + "=" + cn);
	}

	private SoundTraceMain() {
	}
}
