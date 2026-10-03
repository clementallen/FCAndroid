package com.cloudwalk.platform;

/**
 * ARGB colour packing.
 *
 * android.graphics.Color is pure integer arithmetic with no platform behind
 * it, so this is one implementation shared by every target rather than an
 * interface with backends. Signature-compatible with the Android class, so
 * call sites port by changing the import.
 *
 * Colours are 0xAARRGGBB ints throughout the engine.
 */
public final class Color {

	public static final int BLACK   = 0xFF000000;
	public static final int DKGRAY  = 0xFF444444;
	public static final int GRAY    = 0xFF888888;
	public static final int LTGRAY  = 0xFFCCCCCC;
	public static final int WHITE   = 0xFFFFFFFF;
	public static final int RED     = 0xFFFF0000;
	public static final int GREEN   = 0xFF00FF00;
	public static final int BLUE    = 0xFF0000FF;
	public static final int YELLOW  = 0xFFFFFF00;
	public static final int CYAN    = 0xFF00FFFF;
	public static final int MAGENTA = 0xFFFF00FF;
	public static final int TRANSPARENT = 0;

	public static int alpha(int color) { return color >>> 24; }
	public static int red(int color)   { return (color >> 16) & 0xFF; }
	public static int green(int color) { return (color >> 8) & 0xFF; }
	public static int blue(int color)  { return color & 0xFF; }

	public static int rgb(int red, int green, int blue) {
		return 0xFF000000 | (red << 16) | (green << 8) | blue;
	}

	public static int argb(int alpha, int red, int green, int blue) {
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}

	/**
	 * Parses "#RRGGBB" or "#AARRGGBB". The engine feeds this the quoted hex
	 * strings from the .txt model files (Obj3d's parser), which are all the
	 * six-digit form.
	 */
	public static int parseColor(String colorString) {
		if (colorString.charAt(0) == '#') {
			long color = Long.parseLong(colorString.substring(1), 16);
			if (colorString.length() == 7) {
				// six digits carry no alpha - make it opaque
				color |= 0x00000000ff000000L;
			} else if (colorString.length() != 9) {
				throw new IllegalArgumentException("Unknown color: " + colorString);
			}
			return (int) color;
		}
		throw new IllegalArgumentException("Unknown color: " + colorString);
	}

	private Color() {
	}
}
