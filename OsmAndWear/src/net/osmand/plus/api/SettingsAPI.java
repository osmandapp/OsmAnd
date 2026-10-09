package net.osmand.plus.api;

public interface SettingsAPI {

	// world readable
	Object getPreferenceObject(String key);
	
	interface SettingsEditor {
	
		SettingsEditor putString(String key, String value);
		SettingsEditor putBoolean(String key, boolean value);
		SettingsEditor putFloat(String key, float value);
		SettingsEditor putInt(String key, int value);
		SettingsEditor putLong(String key, long value);
		SettingsEditor remove(String key);
		SettingsEditor clear();
		boolean commit();
	}
	
	SettingsEditor edit(Object pref);
	
	String getString(Object pref, String key, String defValue);
	
	float getFloat(Object pref, String key, float defValue);
	
	boolean getBoolean(Object pref, String key, boolean defValue);
	
	int getInt(Object pref, String key, int defValue);
	
	long getLong(Object pref, String key, long defValue);
	
	boolean contains(Object pref, String key);
	
}
