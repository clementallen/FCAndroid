package com.cloudwalk.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cloudwalk.platform.Prefs;

/** {@link Prefs} over SharedPreferences. */
public final class AndroidPrefs implements Prefs {

	private final SharedPreferences prefs;

	public AndroidPrefs(Context context) {
		this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public String getString(String key, String defValue) {
		return prefs.getString(key, defValue);
	}

	public int getInt(String key, int defValue) {
		return prefs.getInt(key, defValue);
	}

	public boolean getBoolean(String key, boolean defValue) {
		return prefs.getBoolean(key, defValue);
	}

	public float getFloat(String key, float defValue) {
		return prefs.getFloat(key, defValue);
	}
}
