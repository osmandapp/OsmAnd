package net.osmand.plus.api;

import net.osmand.plus.OsmandApplication;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SettingsAPIImpl implements SettingsAPI {

	private final OsmandApplication app;

	public SettingsAPIImpl(OsmandApplication app) {
		this.app = app;
	}

	@Override
	public Object getPreferenceObject(String key) {
		return app.getSharedPreferences(key, Context.MODE_PRIVATE);
	}

	@Override
	public SettingsEditor edit(Object pref) {
		Editor edit = ((SharedPreferences) pref).edit();
		return new SettingsEditor() {
			
			@Override
			public SettingsEditor remove(String key) {
				edit.remove(key);
				return this;
			}

			@Override
			public SettingsEditor clear() {
				edit.clear();
				return this;
			}

			@Override
			public SettingsEditor putString(String key, String value) {
				edit.putString(key, value);
				return this;
			}
			
			@Override
			public SettingsEditor putLong(String key, long value) {
				edit.putLong(key, value);
				return this;
			}
			
			@Override
			public SettingsEditor putInt(String key, int value) {
				edit.putInt(key, value);
				return this;
			}
			
			@Override
			public SettingsEditor putFloat(String key, float value) {
				edit.putFloat(key, value);
				return this;
			}
			
			@Override
			public SettingsEditor putBoolean(String key, boolean value) {
				edit.putBoolean(key, value);
				return this;
			}
			
			@Override
			public boolean commit() {
				return edit.commit();
			}
		};
	}

	@Override
	public String getString(Object pref, String key, String defValue) {
		return ((SharedPreferences) pref).getString(key, defValue);
	}

	@Override
	public float getFloat(Object pref, String key, float defValue) {
		return ((SharedPreferences) pref).getFloat(key, defValue);
	}

	@Override
	public boolean getBoolean(Object pref, String key, boolean defValue) {
		return ((SharedPreferences) pref).getBoolean(key, defValue);
	}

	@Override
	public int getInt(Object pref, String key, int defValue) {
		return ((SharedPreferences) pref).getInt(key, defValue);
	}

	@Override
	public long getLong(Object pref, String key, long defValue) {
		return ((SharedPreferences) pref).getLong(key, defValue);
	}

	@Override
	public boolean contains(Object pref, String key) {
		return ((SharedPreferences) pref).contains(key);
	}

}
