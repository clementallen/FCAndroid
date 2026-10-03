package com.cloudwalk.web;

import org.teavm.jso.browser.Storage;

import com.cloudwalk.platform.Prefs;

/**
 * {@link Prefs} over localStorage.
 *
 * Settings are written by the TypeScript settings panel, not from here - the
 * engine only reads them. Values are stored as plain strings under the same
 * keys the Android build uses, so a preference means the same thing on both.
 *
 * localStorage can be absent or throw outright in a private window or with
 * site data blocked, so every read falls back to the caller's default.
 */
public final class WebPrefs implements Prefs {

	private static final String PREFIX = "fc.";

	private final Storage storage;

	public WebPrefs() {
		Storage s;
		try {
			s = Storage.getLocalStorage();
		} catch (Exception e) {
			s = null;
		}
		this.storage = s;
	}

	private String raw(String key) {
		if (storage == null) {
			return null;
		}
		try {
			return storage.getItem(PREFIX + key);
		} catch (Exception e) {
			return null;
		}
	}

	public String getString(String key, String defValue) {
		String v = raw(key);
		return v == null ? defValue : v;
	}

	public int getInt(String key, int defValue) {
		String v = raw(key);
		if (v == null) {
			return defValue;
		}
		try {
			return Integer.parseInt(v.trim());
		} catch (NumberFormatException e) {
			return defValue;
		}
	}

	public boolean getBoolean(String key, boolean defValue) {
		String v = raw(key);
		if (v == null) {
			return defValue;
		}
		return "true".equalsIgnoreCase(v) || "1".equals(v);
	}

	public float getFloat(String key, float defValue) {
		String v = raw(key);
		if (v == null) {
			return defValue;
		}
		try {
			return Float.parseFloat(v.trim());
		} catch (NumberFormatException e) {
			return defValue;
		}
	}
}
