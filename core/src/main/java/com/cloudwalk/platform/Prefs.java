package com.cloudwalk.platform;

/**
 * Read access to user settings.
 *
 * Method names match android.content.SharedPreferences so call sites port by
 * changing a type. Android backs this with SharedPreferences; the web build
 * backs it with localStorage.
 *
 * Read-only by design: the engine only ever reads settings. Writing them is
 * the settings UI's job, which lives outside the engine on both platforms.
 */
public interface Prefs {

	String getString(String key, String defValue);

	int getInt(String key, int defValue);

	boolean getBoolean(String key, boolean defValue);

	float getFloat(String key, float defValue);
}
