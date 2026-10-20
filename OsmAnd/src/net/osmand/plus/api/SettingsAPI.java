package net.osmand.plus.api;

import net.osmand.util.Algorithms;

public interface SettingsAPI {

	// world readable
	Object getPreferenceObject(String key);
	
	interface SettingsEditor {
	
		SettingsEditor putString(String key, String value);
		SettingsEditor putBoolean(String key, boolean value);
		SettingsEditor putFloat(String key, float value);
		default SettingsEditor putDouble(String key, double value) {
			return putString(key, Double.toString(value));
		}
		SettingsEditor putInt(String key, int value);
		SettingsEditor putLong(String key, long value);
		SettingsEditor remove(String key);
		SettingsEditor clear();
		boolean commit();
	}
	
	SettingsEditor edit(Object pref);
	
	String getString(Object pref, String key, String defValue);
	
	float getFloat(Object pref, String key, float defValue);

	default double getDouble(Object pref, String key, double defValue) {
		try {
			return Algorithms.parseDoubleSilently(getString(pref, key, null), defValue);
		} catch (ClassCastException e) {
			// Read floats saved by earlier versions.
			return getFloat(pref, key, (float) defValue);
		}
	}
	
	boolean getBoolean(Object pref, String key, boolean defValue);
	
	int getInt(Object pref, String key, int defValue);
	
	long getLong(Object pref, String key, long defValue);
	
	boolean contains(Object pref, String key);
	
}
