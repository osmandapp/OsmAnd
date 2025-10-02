package net.osmand.plus.settings.backend.preferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SettingsAPI;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class CommonPreference<T> extends PreferenceWithListener<T> {

	private static final Log log = PlatformUtil.getLog(CommonPreference.class);

	private final OsmandSettings settings;
	private Object cachedPreference;

	private final String id;

	private T cachedValue;
	private T defaultValue;
	private Map<ApplicationMode, T> defaultValues;

	private boolean cache;
	private boolean global;
	private boolean shared;
	private boolean lastModifiedTimeStored;
	private String pluginId;

	public CommonPreference(@NonNull OsmandSettings settings, @NonNull String id, T defaultValue) {
		this.settings = settings;
		this.id = id;
		this.defaultValue = defaultValue;
		settings.registerInternalPreference(id, this);
	}

	@Override
	public final String getId() {
		return id;
	}

	// Methods to possibly override
	public abstract T getValue(@NonNull Object prefs, T defaultValue);

	protected long getLastModifiedTime(@NonNull Object prefs) {
		if (!lastModifiedTimeStored) {
			throw new IllegalStateException("Setting " + getId() + " is not granted to store last modified time");
		}
		return getSettingsAPI().getLong(prefs, getLastModifiedTimeId(), 0);
	}

	protected void setLastModifiedTime(@NonNull Object prefs, long lastModifiedTime) {
		if (!lastModifiedTimeStored) {
			throw new IllegalStateException("Setting " + getId() + " is not granted to store last modified time");
		}
		getSettingsAPI().edit(prefs).putLong(getLastModifiedTimeId(), lastModifiedTime).commit();
	}

	protected boolean setValue(Object prefs, T val) {
		if (lastModifiedTimeStored) {
			setLastModifiedTime(prefs, System.currentTimeMillis());
		}
		return true;
	}

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

	public final CommonPreference<T> storeLastModifiedTime() {
		lastModifiedTimeStored = true;
		return this;
	}

	public final void setRelatedPlugin(OsmandPlugin plugin) {
		this.pluginId = plugin != null ? plugin.getId() : null;
	}

	protected final Object getPreferences() {
		return settings.getPreferences(global);
	}

	public final void setModeDefaultValue(ApplicationMode mode, T defValue) {
		if (defaultValues == null) {
			defaultValues = new LinkedHashMap<>();
		}
		defaultValues.put(mode, defValue);
	}

	@Override
	public boolean setModeValue(ApplicationMode mode, T obj) {
		if (global) {
			return set(obj);
		} else if (mode == null) {
			return false;
		}

		Object profilePrefs = settings.getProfilePreferences(mode);
		boolean changed = !Algorithms.objectEquals(obj, getModeValue(mode));
		boolean valueSaved = setValue(profilePrefs, obj);
		if (valueSaved) {
			if (changed) {
				updateLastPreferencesEditTime(profilePrefs);
			}
			if (cache && cachedPreference == profilePrefs) {
				cachedValue = obj;
			}
		}
		fireEvent(obj);

		return valueSaved;
	}

	public T getProfileDefaultValue(@Nullable ApplicationMode mode) {
		if (global) {
			return defaultValue;
		}
		if (defaultValues != null && defaultValues.containsKey(mode)) {
			return defaultValues.get(mode);
		}
		ApplicationMode parentMode = mode != null ? mode.getParent() : null;
		if (parentMode != null) {
			return getProfileDefaultValue(parentMode);
		}
		return defaultValue;
	}

	public void setDefaultValue(T defaultValue) {
		this.defaultValue = defaultValue;
	}

	public final boolean hasDefaultValues() {
		return defaultValues != null && !defaultValues.isEmpty();
	}

	public final boolean hasDefaultValueForMode(ApplicationMode mode) {
		return defaultValues != null && defaultValues.containsKey(mode);
	}

	public T getDefaultValue() {
		return getProfileDefaultValue(getApplicationMode());
	}

	@Override
	public final void overrideDefaultValue(T newDefaultValue) {
		this.defaultValue = newDefaultValue;
	}

	@Override
	public T getModeValue(@Nullable ApplicationMode mode) {
		if (global) {
			return get();
		} else if (mode == null) {
			return defaultValue;
		}
		OsmandPlugin plugin = getRelatedPlugin();
		if (plugin != null && plugin.disablePreferences()) {
			return getProfileDefaultValue(mode);
		}
		T defaultV = getProfileDefaultValue(mode);
		return getValue(settings.getProfilePreferences(mode), defaultV);
	}

	@Override
	public T get() {
		OsmandPlugin plugin = getRelatedPlugin();
		if (plugin != null && plugin.disablePreferences()) {
			return getDefaultValue();
		}
		if (cache && cachedValue != null && cachedPreference == getPreferences()) {
			return cachedValue;
		}
		cachedPreference = getPreferences();
		cachedValue = getValue(cachedPreference, getDefaultValue());
		return cachedValue;
	}

	@Override
	public final void resetToDefault() {
		if (global) {
			T oldValue = get();
			Object prefs = getPreferences();
			if (getSettingsAPI().edit(prefs).remove(getId()).commit()) {
				T newValue = get();
				boolean changed = !Algorithms.objectEquals(oldValue, newValue);
				if (changed && lastModifiedTimeStored) {
					setLastModifiedTime(prefs, System.currentTimeMillis());
				}
				if (changed && isShared()) {
					if (PluginsHelper.isDevelopment()) {
						log.debug("RESET_TO_DEFAULT id=" + getId() + " value=" + newValue + " cached=" + cachedValue);
					}
					updateLastPreferencesEditTime(prefs);
				}
				cachedValue = newValue;
				cachedPreference = prefs;
				fireEvent(newValue);
			}
		} else {
			resetModeToDefault(getApplicationMode());
		}
	}

	@Override
	public final void resetModeToDefault(@Nullable ApplicationMode mode) {
		if (global) {
			resetToDefault();
		} else if (mode != null) {
			T oldValue = getModeValue(mode);
			Object prefs = settings.getProfilePreferences(mode);
			if (getSettingsAPI().edit(prefs).remove(getId()).commit()) {
				T newValue = getModeValue(mode);
				boolean changed = !Algorithms.objectEquals(oldValue, newValue);
				if (changed) {
					updateLastPreferencesEditTime(prefs);
				}
				if (cache && cachedPreference == prefs) {
					cachedValue = newValue;
				}
				fireEvent(newValue);
			}
		}
	}

	@Override
	public boolean set(T obj) {
		Object prefs = getPreferences();
		boolean changed = !Algorithms.objectEquals(obj, get());
		if (setValue(prefs, obj)) {
			if (changed && isShared() && isGlobal() && PluginsHelper.isDevelopment()) {
				log.debug("SET GLOBAL id=" + getId() + " value=" + obj + " cached=" + cachedValue);
			}
			cachedValue = obj;
			cachedPreference = prefs;
			if (changed && (isShared() || !global)) {
				updateLastPreferencesEditTime(prefs);
			}
			fireEvent(obj);
			return true;
		}
		return false;
	}

	public long getLastModifiedTimeModeValue(@Nullable ApplicationMode mode) {
		if (global) {
			return getLastModifiedTime();
		}
		return getLastModifiedTime(settings.getProfilePreferences(mode));
	}

	public long getLastModifiedTime() {
		return getLastModifiedTime(getPreferences());
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		setLastModifiedTime(getPreferences(), lastModifiedTime);
	}

	private void updateLastPreferencesEditTime(@NonNull Object preferences) {
		settings.updateLastPreferencesEditTime(preferences);
	}

	public final boolean isSet() {
		return settings.isSet(global, getId());
	}

	public boolean isSetForMode(@Nullable ApplicationMode mode) {
		return settings.isSet(mode, getId());
	}

	public final boolean isGlobal() {
		return global;
	}

	public final boolean isShared() {
		return shared;
	}

	public boolean isLastModifiedTimeStored() {
		return lastModifiedTimeStored;
	}

	@Nullable
	public OsmandPlugin getRelatedPlugin() {
		return pluginId != null ? PluginsHelper.getPlugin(pluginId) : null;
	}

	public boolean isNullSupported() {
		return isNullSupported(getApplicationMode());
	}

	public boolean isNullSupported(@NonNull ApplicationMode mode) {
		return isGlobal() ? getDefaultValue() == null : getProfileDefaultValue(mode) == null;
	}

	@NonNull
	protected String getLastModifiedTimeId() {
		return id + "_last_modified";
	}

	@Override
	public boolean writeToJson(@NonNull JSONObject json,
			@Nullable ApplicationMode appMode) throws JSONException {
		if (appMode != null) {
			if (!global) {
				if (isSetForMode(appMode)) {
					String value = asStringModeValue(appMode);
					if (value != null) {
						json.put(getId(), value);
					}
					return true;
				}
			}
		} else if (global) {
			if (isSet()) {
				String value = asString();
				if (value != null) {
					json.put(getId(), value);
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void readFromJson(@NonNull JSONObject json,
			@Nullable ApplicationMode appMode) throws JSONException {
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
	public final String asStringModeValue(@Nullable ApplicationMode mode) {
		T v = getModeValue(mode);
		return toString(v);
	}

	@NonNull
	@Override
	public String toString() {
		return getId();
	}
}
