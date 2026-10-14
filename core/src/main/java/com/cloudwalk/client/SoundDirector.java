package com.cloudwalk.client;

import java.util.Random;

import com.cloudwalk.platform.Prefs;
import com.cloudwalk.startup.ModelEnv;

/**
 * Decides what the game sounds like.
 *
 * All of this used to live inside the Android Activity's Handler, calling
 * SoundPool directly, which meant seven of the game's eight sounds existed
 * only on Android - the browser build was silent apart from the variometer.
 * When to play what is game behaviour, not platform behaviour, so it belongs
 * here with the rest of it; the platform only has to supply
 * {@link ModelEnv#play}, {@link ModelEnv#stopSound} and
 * {@link ModelEnv#setSoundRate}.
 *
 * Driven from {@link GliderUser#tick} at the full frame rate rather than from
 * XCModel, whose 5Hz throttle would make the sink tone lumpy and thin the bird
 * calls out fivefold.
 */
public class SoundDirector {

	/** Indices into the platform's sound bank, in load order. */
	public static final int BEEP = 0;
	public static final int WIND = 1;
	public static final int HAWK = 2;
	public static final int CROW = 3;
	public static final int LANDED = 4;
	public static final int FINISH = 5;
	public static final int HAWK2 = 6;
	public static final int SINK = 7;

	/** One in this many ticks, per bird. */
	private static final int BIRD_ODDS = 3000;

	/** The wind sits behind everything else; the birds are far behind that. */
	private static final float WIND_VOLUME = 0.5f;
	private static final float BIRD_VOLUME = 0.05f;

	private final XCModelViewer xcModelViewer;
	private final GliderUser glider;

	/**
	 * Its own random stream, deliberately not the shared one. Bird calls must
	 * not consume from the sequence the simulation draws on, or turning the
	 * sound on would change where the gliders fly.
	 */
	private final Random random = new Random();

	private boolean flying = false;
	private boolean sinking = false;
	private boolean finished = false;
	private float currentSpeed = Float.NaN;

	public SoundDirector(XCModelViewer xcModelViewer, GliderUser glider) {
		this.xcModelViewer = xcModelViewer;
		this.glider = glider;
	}

	public void tick(float t) {
		ModelEnv env = xcModelViewer.modelEnv;
		Prefs prefs = env.getPrefs();
		boolean ambient = prefs.getBoolean("ambient_sound", true);

		// Landing ends the flight: kill both loops, then the thud.
		if (flying && glider.getLanded()) {
			flying = false;
			sinking = false;
			env.stopSound(WIND);
			env.stopSound(SINK);
			env.play(1f, LANDED, 0, 1f);
		}

		// The chime is for the timed tasks; a distance task has no finish.
		if (glider.finished && !finished) {
			finished = true;
			Task task = xcModelViewer.xcModel.task;
			if (task != null && (task.type == Task.TIME || task.type == Task.TIME_PRECISE)) {
				env.play(1f, FINISH, 0, 1f);
			}
		}

		if (flying && glider.airv < 0 && !sinking && prefs.getBoolean("sink_tone", true)) {
			env.play(sinkRate(), SINK, -1, 1f);
			sinking = true;
		} else if (flying && sinking && glider.airv >= 0) {
			env.stopSound(SINK);
			sinking = false;
		}
		if (flying && sinking) {
			env.setSoundRate(SINK, sinkRate());
		}

		if (flying) {
			if (ambient) {
				// Re-pitch rather than replay - starting the loop again every
				// frame would stutter it.
				float speed = glider.getSpeed();
				if (speed != currentSpeed) {
					env.setSoundRate(WIND, windRate(speed));
					currentSpeed = speed;
				}
				int roll = (int) (random.nextDouble() * BIRD_ODDS);
				if (roll == 0) {
					env.play(1f, HAWK, 0, birdVolume());
				} else if (roll == 1) {
					env.play(1f, CROW, 0, birdVolume());
				} else if (roll == 2) {
					env.play(1f, HAWK2, 0, birdVolume());
				}
			}
		} else if (glider.racing && !glider.getLanded()) {
			// Latch regardless of the preference, so turning ambient sound off
			// silences the flight rather than re-arming this every frame.
			flying = true;
			currentSpeed = glider.getSpeed();
			if (ambient) {
				env.play(windRate(currentSpeed), WIND, -1, WIND_VOLUME);
			}
		}
	}

	/** Stops anything looping. For leaving the game. */
	public void stop() {
		ModelEnv env = xcModelViewer.modelEnv;
		env.stopSound(WIND);
		env.stopSound(SINK);
		flying = false;
		sinking = false;
	}

	private float windRate(float speed) {
		return (float) Math.sqrt(speed / 1.7);
	}

	/** airv is negative in sinking air, so stronger sink means lower pitch. */
	private float sinkRate() {
		return 1f + glider.airv * 3;
	}

	private float birdVolume() {
		return (float) random.nextDouble() * BIRD_VOLUME;
	}
}
