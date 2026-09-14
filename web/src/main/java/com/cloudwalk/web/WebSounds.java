package com.cloudwalk.web;

import org.teavm.jso.JSBody;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.webaudio.AudioBuffer;
import org.teavm.jso.webaudio.AudioBufferSourceNode;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.webaudio.DecodeSuccessCallback;
import org.teavm.jso.webaudio.GainNode;

import com.cloudwalk.platform.Log;

/**
 * The engine's sounds, on Web Audio.
 *
 * Stands in for Android's SoundPool. The mapping is close: a SoundPool stream
 * is an AudioBufferSourceNode, its rate argument is playbackRate, and loop=-1
 * is loop=true. Rate is used as pitch throughout - the variometer encodes lift
 * strength in it, and the wind tone tracks airspeed - so live sources are kept
 * per index and can be re-rated while playing.
 *
 * Browsers refuse to start an AudioContext without a user gesture, so nothing
 * is created until {@link #resume()} is called from a click.
 */
public final class WebSounds {

	/** Index order matches the Android build's soundIds array. */
	public static final String[] FILES = {
		"beep0", "wind", "hawk", "crow", "landed", "finish", "hawk2", "sink"
	};

	private AudioContext ctx;
	private GainNode master;
	private final AudioBuffer[] buffers = new AudioBuffer[FILES.length];
	private final AudioBufferSourceNode[] live = new AudioBufferSourceNode[FILES.length];

	/**
	 * Plays that arrived before their sound had finished downloading.
	 *
	 * The engine starts the wind on the first frame of the flight, which is
	 * the same moment the mp3s begin fetching - and it only asks once, because
	 * the loop is meant to run until landing. Dropping that request left the
	 * flight silent for its whole duration. So hold the request and honour it
	 * when the buffer lands.
	 */
	private final boolean[] pending = new boolean[FILES.length];
	private final float[] pendingPitch = new float[FILES.length];
	private final int[] pendingLoop = new int[FILES.length];
	private final float[] pendingVolume = new float[FILES.length];
	private final String base;
	private float volume = 1f;
	private boolean muted = false;

	public WebSounds(String base) {
		this.base = base;
	}

	/**
	 * Builds the audio context.
	 *
	 * TeaVM 0.15's AudioContext.create() compiles to `new Context()`, which is
	 * not a thing any browser defines, so it throws on the first call. Doing it
	 * by hand also picks up the webkit-prefixed constructor that older Safari
	 * still needs.
	 */
	@JSBody(script = "var C = window.AudioContext || window.webkitAudioContext;"
			+ " return C ? new C() : null;")
	private static native AudioContext newAudioContext();

	/** Creates the context and starts loading. Must be called from a gesture. */
	public void resume() {
		if (ctx == null) {
			ctx = newAudioContext();
			if (ctx == null) {
				Log.w("FC AUDIO", "no Web Audio in this browser - running silent");
				return;
			}
			master = ctx.createGain();
			master.getGain().setValue(volume);
			master.connect(ctx.getDestination());
			for (int i = 0; i < FILES.length; i++) {
				load(i);
			}
		}
		try {
			if (!"running".equals(ctx.getState())) {
				ctx.resume();
			}
		} catch (Exception e) {
			Log.w("FC AUDIO", "could not resume audio: " + e);
		}
	}

	private void load(final int index) {
		final XMLHttpRequest req = XMLHttpRequest.create();
		req.open("GET", base + FILES[index] + ".mp3");
		req.setResponseType("arraybuffer");
		req.onComplete(new Runnable() {
			public void run() {
				if (req.getStatus() != 200) {
					Log.w("FC AUDIO", "missing sound: " + FILES[index]);
					return;
				}
				ArrayBuffer raw = (ArrayBuffer) req.getResponse();
				ctx.decodeAudioData(raw, new DecodeSuccessCallback() {
					public void onSuccess(AudioBuffer decoded) {
						buffers[index] = decoded;
						if (pending[index]) {
							pending[index] = false;
							play(pendingPitch[index], index, pendingLoop[index], pendingVolume[index]);
						}
					}
				});
			}
		});
		req.send();
	}

	/**
	 * @param pitch  playback rate, as SoundPool used it
	 * @param loop   -1 to repeat forever, as SoundPool used it
	 * @param volume 0..1, per sound - the wind and the birds sit well below
	 *               the rest, so they need their own gain rather than the
	 *               master
	 */
	public void play(float pitch, int index, int loop, float volume) {
		if (ctx == null || muted || index < 0 || index >= buffers.length) {
			return;
		}
		if (buffers[index] == null) {
			// Still downloading - remember it and start when it arrives.
			stop(index);
			pending[index] = true;
			pendingPitch[index] = pitch;
			pendingLoop[index] = loop;
			pendingVolume[index] = volume;
			return;
		}
		stop(index);
		AudioBufferSourceNode src = ctx.createBufferSource();
		src.setBuffer(buffers[index]);
		src.setLoop(loop == -1);
		src.getPlaybackRate().setValue(pitch <= 0 ? 1f : pitch);
		GainNode gain = ctx.createGain();
		gain.getGain().setValue(volume < 0 ? 0 : volume);
		src.connect(gain);
		gain.connect(master);
		src.start();
		live[index] = src;
	}

	/** Re-pitches a sound that is already playing - the wind and sink tones. */
	public void setRate(int index, float rate) {
		if (index < 0 || index >= live.length || rate <= 0) {
			return;
		}
		if (pending[index]) {
			// Not started yet; make sure it starts at the current pitch.
			pendingPitch[index] = rate;
			return;
		}
		AudioBufferSourceNode src = live[index];
		if (src != null) {
			src.getPlaybackRate().setValue(rate);
		}
	}

	public void stop(int index) {
		if (index < 0 || index >= live.length) {
			return;
		}
		pending[index] = false;
		AudioBufferSourceNode src = live[index];
		if (src != null) {
			try {
				src.stop();
			} catch (Exception ignored) {
				// already ended
			}
			live[index] = null;
		}
	}

	public void stopAll() {
		for (int i = 0; i < live.length; i++) {
			stop(i);
		}
	}

	public void setVolume(float v) {
		volume = v;
		if (master != null) {
			master.getGain().setValue(v);
		}
	}

	public void setMuted(boolean m) {
		muted = m;
		if (m) {
			stopAll();
		}
	}

	public boolean isMuted() {
		return muted;
	}
}
