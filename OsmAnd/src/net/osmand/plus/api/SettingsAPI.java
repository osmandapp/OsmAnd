package net.osmand.plus.api;

public interface SettingsAPI {

	// world readable
	public Object getPreferenceObject(String key);
	
	public interface SettingsEditor {
	
		public SettingsEditor putString(String key, String value);
		public SettingsEditor putBoolean(String key, boolean value);
		public SettingsEditor putFloat(String key, float value);
		public SettingsEditor putInt(String key, int value);
		public SettingsEditor putLong(String key, long value);
		public SettingsEditor remove(String key);
		public SettingsEditor clear();
		public boolean commit();
	}
	
	public SettingsEditor edit(Object pref);
	
	public String getString(Object pref, String key, String defValue);
	
	public float getFloat(Object pref, String key, float defValue);
	
	public boolean getBoolean(Object pref, String key, boolean defValue);
	
	public int getInt(Object pref, String key, int defValue);
	
	public long getLong(Object pref, String key, long defValue);
	
	public boolean contains(Object pref, String key);
	
}
