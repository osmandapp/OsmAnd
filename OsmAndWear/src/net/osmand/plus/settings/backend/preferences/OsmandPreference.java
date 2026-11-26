package net.osmand.plus.settings.backend.preferences;

import net.osmand.StateChangedListener;
import net.osmand.plus.settings.backend.ApplicationMode;

import org.json.JSONException;
import org.json.JSONObject;

public interface OsmandPreference<T> {
	T get();

	boolean set(T obj);

	boolean setModeValue(ApplicationMode m, T obj);

	T getModeValue(ApplicationMode m);

	String getId();

	void resetToDefault();

	void resetModeToDefault(ApplicationMode m);

	void overrideDefaultValue(T newDefaultValue);

	void addListener(StateChangedListener<T> listener);

	void removeListener(StateChangedListener<T> listener);

	boolean isSet();

	boolean isSetForMode(ApplicationMode m);

	boolean writeToJson(JSONObject json, ApplicationMode appMode) throws JSONException;

	void readFromJson(JSONObject json, ApplicationMode appMode) throws JSONException;

	String asString();

	String asStringModeValue(ApplicationMode m);

	T parseString(String s);
}