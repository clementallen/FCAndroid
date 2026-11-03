package com.cloudwalk.harness;

import java.util.ArrayList;
import java.util.List;

import com.cloudwalk.client.SoundDirector;
import com.cloudwalk.platform.NetLink;

/**
 * A {@link HeadlessEnv} that writes down what it was asked to play.
 *
 * Audio is the one part of the port that cannot be checked by looking at the
 * screen or by listening in a headless browser, so it gets checked the same way
 * the physics does: fly a real task and compare what came out.
 */
public class SoundLog extends HeadlessEnv {

	private static final String[] NAMES = {
		"beep", "wind", "hawk", "crow", "landed", "finish", "hawk2", "sink"
	};

	public static final class Event {
		public final int frame;
		public final String what;

		Event(int frame, String what) {
			this.frame = frame;
			this.what = what;
		}

		public String toString() {
			return frame + " " + what;
		}
	}

	private final List<Event> events = new ArrayList<Event>();
	private int frame = 0;

	public SoundLog(AssetSource assets, String task, int pilotType, int[] typeNums) {
		super(assets, task, pilotType, typeNums);
	}

	public void setFrame(int frame) {
		this.frame = frame;
	}

	public List<Event> events() {
		return events;
	}

	private static String name(int index) {
		return index >= 0 && index < NAMES.length ? NAMES[index] : ("#" + index);
	}

	public void play(float pitch, int index, int loop, float volume) {
		events.add(new Event(frame, "play " + name(index)
				+ (loop == -1 ? " loop" : "")
				+ " pitch=" + round(pitch) + " vol=" + round(volume)));
	}

	public void stopSound(int index) {
		events.add(new Event(frame, "stop " + name(index)));
	}

	public void setSoundRate(int index, float rate) {
		// Rates change most frames; recording every one would bury everything
		// else. The transitions are what matter.
		events.add(new Event(frame, "rate " + name(index) + " " + round(rate)));
	}

	public NetLink openNetLink(String hostPort, NetLink.Listener listener) {
		return null;
	}

	/** Counts how many events mention this sound, ignoring rate changes. */
	public int countPlays(int index) {
		int n = 0;
		for (Event e : events) {
			if (e.what.startsWith("play " + name(index))) {
				n++;
			}
		}
		return n;
	}

	public int countStops(int index) {
		int n = 0;
		for (Event e : events) {
			if (e.what.equals("stop " + name(index))) {
				n++;
			}
		}
		return n;
	}

	public int countRates(int index) {
		int n = 0;
		for (Event e : events) {
			if (e.what.startsWith("rate " + name(index))) {
				n++;
			}
		}
		return n;
	}

	public static int wind() {
		return SoundDirector.WIND;
	}

	private static String round(float f) {
		return Long.toString(Math.round(f * 100.0)) ;
	}
}
