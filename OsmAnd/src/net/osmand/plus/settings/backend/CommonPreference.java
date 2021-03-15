package net.osmand.plus.settings.backend;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SettingsAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class CommonPreference<T> extends PreferenceWithListener<T> {

	private OsmandSettings settings;
	private Object cachedPreference;

	private final String id;

	private T cachedValue;
	private T defaultValue;
	private Map<ApplicationMode, T> defaultValues;

	private boolean cache;
	private boolean global;
	private boolean shared;


	public CommonPreference(OsmandSettings settings, String id, T defaultValue) {
		this.settings = settings;
		this.id = id;
		this.defaultValue = defaultValue;
		settings.registerInternalPreference(id, this);
	}

	// Methods to possibly override
	protected abstract T getValue(Object prefs, T defaultValue);

	protected abstract boolean setValue(Object prefs, T val);

	public abstract T parseString(String s);

	protected String toString(T o) {
		return o == null ? null : o.toString();
	}

	protected SettingsAPI getSettingsAPI() {
		return settings.getSettingsAPI();
	}

	protected ApplicationMode getApplicationMode() {
		return settings.getApplicationMode();
	}

	protected OsmandApplication getContext() {
		return settings.getContext();
	}

	// common methods

	public final CommonPreference<T> makeGlobal() {
		global = true;
		return this;
	}

	public final CommonPreference<T> cache() {
		cache = true;
		return this;
	}

	public final CommonPreference<T> makeProfile() {
		global = false;
		return this;
	}

	public final CommonPreference<T> makeShared() {
		shared = true;
		return this;
	}

	protected final Object getPreferences() {
		return settings.getPreferences(global);

	}

	public final void setModeDefaultValue(ApplicationMode mode, T defValue) {
		if (defaultValues == null) {
			defaultValues = new LinkedHashMap<ApplicationMode, T>();
		}
		defaultValues.put(mode, defValue);
	}

	@Override
	public boolean setModeValue(ApplicationMode mode, T obj) {
		if (global) {
			return set(obj);
		}

		Object profilePrefs = settings.getProfilePreferences(mode);
		boolean valueSaved = setValue(profilePrefs, obj);
		if (valueSaved && cache && cachedPreference == profilePrefs) {
			cachedValue = obj;
		}
		fireEvent(obj);

		return valueSaved;
	}

	public T getProfileDefaultValue(ApplicationMode mode) {
		if (global) {
			return defaultValue;
		}
		if (defaultValues != null && defaultValues.containsKey(mode)) {
			return defaultValues.get(mode);
		}
		ApplicationMode pt = mode.getParent();
		if (pt != null) {
			return getProfileDefaultValue(pt);
		}
		return defaultValue;
	}

	public final boolean hasDefaultValues() {
		return defaultValues != null && !defaultValues.isEmpty();
	}

	public final boolean hasDefaultValueForMode(ApplicationMode mode) {
		return defaultValues != null && defaultValues.containsKey(mode);
	}

	protected T getDefaultValue() {
		return getProfileDefaultValue(settings.APPLICATION_MODE.get());
	}

	@Override
	public final void overrideDefaultValue(T newDefaultValue) {
		this.defaultValue = newDefaultValue;
	}

	@Override
	public T getModeValue(ApplicationMode mode) {
		if (global) {
			return get();
		}
		T defaultV = getProfileDefaultValue(mode);
		return getValue(settings.getProfilePreferences(mode), defaultV);
	}

	@Override
	public T get() {
		if (cache && cachedValue != null && cachedPreference == getPreferences()) {
			return cachedValue;
		}
		cachedPreference = getPreferences();
		cachedValue = getValue(cachedPreference, getDefaultValue());
		return cachedValue;
	}

	@Override
	public final String getId() {
		return id;
	}

	@Override
	public final void resetToDefault() {
		T o = getProfileDefaultValue(settings.APPLICATION_MODE.get());
		set(o);
	}

	@Override
	public final void resetModeToDefault(ApplicationMode mode) {
		if (global) {
			resetToDefault();
		} else {
			T o = getProfileDefaultValue(mode);
			setModeValue(mode, o);
		}
	}

	@Override
	public boolean set(T obj) {
		Object prefs = getPreferences();
		if (setValue(prefs, obj)) {
			cachedValue = obj;
			cachedPreference = prefs;
			fireEvent(obj);
			return true;
		}
		return false;
	}

	public final boolean isSet() {
		return settings.isSet(global, getId());
	}

	public boolean isSetForMode(ApplicationMode mode) {
		return settings.isSet(mode, getId());
	}

	public final boolean isGlobal() {
		return global;
	}

	public final boolean isShared() {
		return shared;
	}

	@Override
	public boolean writeToJson(JSONObject json, ApplicationMode appMode) throws JSONException {
		if (appMode != null) {
			if (!global) {
				String value = asStringModeValue(appMode);
				if (value != null) {
					json.put(getId(), value);
				}
				return true;
			}
		} else if (global) {
			String value = asString();
			if (value != null) {
				json.put(getId(), value);
			}
			return true;
		}
		return false;
	}

	@Override
	public void readFromJson(JSONObject json, ApplicationMode appMode) throws JSONException {
		if (appMode != null) {
			if (!global) {
				String modeValue = json.getString(getId());
				setModeValue(appMode, parseString(modeValue));
			}
		} else if (global) {
			String globalValue = json.getString(getId());
			set(parseString(globalValue));
		}
	}

	@Override
	public final String asString() {
		T o = get();
		return toString(o);
	}

	@Override
	public final String asStringModeValue(ApplicationMode m) {
		T v = getModeValue(m);
		return toString(v);
	}
}
