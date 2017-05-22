package net.osmand.plus;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.util.Pair;

import net.osmand.IndexConstants;
import net.osmand.StateChangedListener;
import net.osmand.ValueHolder;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.access.AccessibilityMode;
import net.osmand.plus.access.RelativeDirectionStyle;
import net.osmand.plus.api.SettingsAPI;
import net.osmand.plus.api.SettingsAPI.SettingsEditor;
import net.osmand.plus.dialogs.RateUsBottomSheetDialog;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.mapillary.MapillaryPlugin;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.render.RenderingRulesStorage;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class OsmandSettings {

	public interface OsmandPreference<T> {
		T get();

		boolean set(T obj);

		boolean setModeValue(ApplicationMode m, T obj);

		T getModeValue(ApplicationMode m);

		String getId();

		void resetToDefault();

		void overrideDefaultValue(T newDefaultValue);

		void addListener(StateChangedListener<T> listener);

		void removeListener(StateChangedListener<T> listener);

		boolean isSet();
	}

	private abstract class PreferenceWithListener<T> implements OsmandPreference<T> {
		private List<WeakReference<StateChangedListener<T>>> l = null;

		@Override
		public synchronized void addListener(StateChangedListener<T> listener) {
			if (l == null) {
				l = new LinkedList<WeakReference<StateChangedListener<T>>>();
			}
			if (!l.contains(new WeakReference<StateChangedListener<T>>(listener))) {
				l.add(new WeakReference<StateChangedListener<T>>(listener));
			}
		}

		public synchronized void fireEvent(T value) {
			if (l != null) {
				Iterator<WeakReference<StateChangedListener<T>>> it = l.iterator();
				while (it.hasNext()) {
					StateChangedListener<T> t = it.next().get();
					if (t == null) {
						it.remove();
					} else {
						t.stateChanged(value);
					}
				}
			}
		}

		@Override
		public synchronized void removeListener(StateChangedListener<T> listener) {
			if (l != null) {
				Iterator<WeakReference<StateChangedListener<T>>> it = l.iterator();
				while (it.hasNext()) {
					StateChangedListener<T> t = it.next().get();
					if (t == listener) {
						it.remove();
					}
				}
			}
		}
	}

	// These settings are stored in SharedPreferences
	private static final String SHARED_PREFERENCES_NAME = "net.osmand.settings"; //$NON-NLS-1$


	/// Settings variables
	private final OsmandApplication ctx;
	private SettingsAPI settingsAPI;
	private Object globalPreferences;
	private Object defaultProfilePreferences;
	private Object profilePreferences;
	private ApplicationMode currentMode;
	private Map<String, OsmandPreference<?>> registeredPreferences =
			new LinkedHashMap<String, OsmandSettings.OsmandPreference<?>>();

	// cache variables
	private long lastTimeInternetConnectionChecked = 0;
	private boolean internetConnectionAvailable = true;


	protected OsmandSettings(OsmandApplication clientContext, SettingsAPI settinsAPI) {
		ctx = clientContext;
		this.settingsAPI = settinsAPI;
		initPrefs();
	}

	private void initPrefs() {
		globalPreferences = settingsAPI.getPreferenceObject(SHARED_PREFERENCES_NAME);
		defaultProfilePreferences = getProfilePreferences(ApplicationMode.DEFAULT);
		currentMode = readApplicationMode();
		profilePreferences = getProfilePreferences(currentMode);
	}

	public OsmandApplication getContext() {
		return ctx;
	}

	public void setSettingsAPI(SettingsAPI settingsAPI) {
		this.settingsAPI = settingsAPI;
		initPrefs();
	}

	public SettingsAPI getSettingsAPI() {
		return settingsAPI;
	}

	public static String getSharedPreferencesName(ApplicationMode mode) {
		if (mode == null) {
			return SHARED_PREFERENCES_NAME;
		} else {
			return SHARED_PREFERENCES_NAME + "." + mode.getStringKey().toLowerCase();
		}
	}

	public Object getProfilePreferences(ApplicationMode mode) {
		return settingsAPI.getPreferenceObject(getSharedPreferencesName(mode));
	}

	public ApplicationMode LAST_ROUTING_APPLICATION_MODE = null;

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<ApplicationMode> APPLICATION_MODE = new PreferenceWithListener<ApplicationMode>() {
		@Override
		public String getId() {
			return "application_mode";
		}

		;

		@Override
		public ApplicationMode get() {
			return currentMode;
		}

		@Override
		public void overrideDefaultValue(ApplicationMode newDefaultValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void resetToDefault() {
			set(ApplicationMode.DEFAULT);
		}

		;

		@Override
		public boolean isSet() {
			return true;
		}

		;

		@Override
		public boolean set(ApplicationMode val) {
			ApplicationMode oldMode = currentMode;
			boolean changed = settingsAPI.edit(globalPreferences).putString(getId(), val.getStringKey()).commit();
			if (changed) {
				currentMode = val;
				profilePreferences = getProfilePreferences(currentMode);
				fireEvent(oldMode);
			}
			return changed;
		}

		@Override
		public ApplicationMode getModeValue(ApplicationMode m) {
			return m;
		}

		@Override
		public boolean setModeValue(ApplicationMode m, ApplicationMode obj) {
			throw new UnsupportedOperationException();
		}
	};

	public ApplicationMode getApplicationMode() {
		return APPLICATION_MODE.get();
	}

	protected ApplicationMode readApplicationMode() {
		String s = settingsAPI.getString(globalPreferences, APPLICATION_MODE.getId(), ApplicationMode.DEFAULT.getStringKey());
		return ApplicationMode.valueOfStringKey(s, ApplicationMode.DEFAULT);
	}


	// Check internet connection available every 15 seconds
	public boolean isInternetConnectionAvailable() {
		return isInternetConnectionAvailable(false);
	}

	public boolean isInternetConnectionAvailable(boolean update) {
		long delta = System.currentTimeMillis() - lastTimeInternetConnectionChecked;
		if (delta < 0 || delta > 15000 || update) {
			internetConnectionAvailable = isInternetConnected();
		}
		return internetConnectionAvailable;
	}

	public boolean isWifiConnected() {
		ConnectivityManager mgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = mgr.getActiveNetworkInfo();
		return ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI;
	}

	private boolean isInternetConnected() {
		ConnectivityManager mgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo active = mgr.getActiveNetworkInfo();
		if (active == null) {
			return false;
		} else {
			NetworkInfo.State state = active.getState();
			return state != NetworkInfo.State.DISCONNECTED && state != NetworkInfo.State.DISCONNECTING;
		}
	}


	/////////////// PREFERENCES classes ////////////////

	public abstract class CommonPreference<T> extends PreferenceWithListener<T> {
		private final String id;
		private boolean global;
		private T cachedValue;
		private Object cachedPreference;
		private boolean cache;
		private Map<ApplicationMode, T> defaultValues;
		private T defaultValue;


		public CommonPreference(String id, T defaultValue) {
			this.id = id;
			this.defaultValue = defaultValue;
		}

		public CommonPreference<T> makeGlobal() {
			global = true;
			return this;
		}

		public CommonPreference<T> cache() {
			cache = true;
			return this;
		}

		public CommonPreference<T> makeProfile() {
			global = false;
			return this;
		}

		protected Object getPreferences() {
			return global ? globalPreferences : profilePreferences;
		}

		public void setModeDefaultValue(ApplicationMode mode, T defValue) {
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
			boolean ch = setValue(getProfilePreferences(mode), obj);
			fireEvent(obj);
			return ch;
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
			if (settingsAPI.contains(defaultProfilePreferences, getId())) {
				return getValue(defaultProfilePreferences, defaultValue);
			} else {
				return defaultValue;
			}
		}

		protected T getDefaultValue() {
			return getProfileDefaultValue(currentMode);
		}

		@Override
		public void overrideDefaultValue(T newDefaultValue) {
			this.defaultValue = newDefaultValue;
		}

		protected abstract T getValue(Object prefs, T defaultValue);


		protected abstract boolean setValue(Object prefs, T val);

		@Override
		public T getModeValue(ApplicationMode mode) {
			if (global) {
				return get();
			}
			T defaultV = getProfileDefaultValue(mode);
			return getValue(getProfilePreferences(mode), defaultV);
		}

		@Override
		public T get() {
			if (cache && cachedValue != null && cachedPreference == getPreferences()) {
				return cachedValue;
			}
			cachedPreference = getPreferences();
			cachedValue = getValue(cachedPreference, getProfileDefaultValue(currentMode));
			return cachedValue;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public void resetToDefault() {
			T o = getProfileDefaultValue(currentMode);
			set(o);
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

		public boolean isSet() {
			return settingsAPI.contains(getPreferences(), getId());
		}

	}

	private class BooleanPreference extends CommonPreference<Boolean> {


		private BooleanPreference(String id, boolean defaultValue) {
			super(id, defaultValue);
		}

		@Override
		protected Boolean getValue(Object prefs, Boolean defaultValue) {
			return settingsAPI.getBoolean(prefs, getId(), defaultValue);
		}

		@Override
		protected boolean setValue(Object prefs, Boolean val) {
			return settingsAPI.edit(prefs).putBoolean(getId(), val).commit();
		}
	}

	private class BooleanAccessibilityPreference extends BooleanPreference {

		private BooleanAccessibilityPreference(String id, boolean defaultValue) {
			super(id, defaultValue);
		}

		@Override
		protected Boolean getValue(Object prefs, Boolean defaultValue) {
			return ctx.accessibilityEnabled() ?
					super.getValue(prefs, defaultValue) :
					defaultValue;
		}

		@Override
		protected boolean setValue(Object prefs, Boolean val) {
			return ctx.accessibilityEnabled() ?
					super.setValue(prefs, val) :
					false;
		}
	}

	private class IntPreference extends CommonPreference<Integer> {


		private IntPreference(String id, int defaultValue) {
			super(id, defaultValue);
		}

		@Override
		protected Integer getValue(Object prefs, Integer defaultValue) {
			return settingsAPI.getInt(prefs, getId(), defaultValue);
		}

		@Override
		protected boolean setValue(Object prefs, Integer val) {
			return settingsAPI.edit(prefs).putInt(getId(), val).commit();
		}

	}

	private class LongPreference extends CommonPreference<Long> {


		private LongPreference(String id, long defaultValue) {
			super(id, defaultValue);
		}

		@Override
		protected Long getValue(Object prefs, Long defaultValue) {
			return settingsAPI.getLong(prefs, getId(), defaultValue);
		}

		@Override
		protected boolean setValue(Object prefs, Long val) {
			return settingsAPI.edit(prefs).putLong(getId(), val).commit();
		}

	}

	private class FloatPreference extends CommonPreference<Float> {


		private FloatPreference(String id, float defaultValue) {
			super(id, defaultValue);
		}

		@Override
		protected Float getValue(Object prefs, Float defaultValue) {
			return settingsAPI.getFloat(prefs, getId(), defaultValue);
		}

		@Override
		protected boolean setValue(Object prefs, Float val) {
			return settingsAPI.edit(prefs).putFloat(getId(), val).commit();
		}

	}

	private class StringPreference extends CommonPreference<String> {

		private StringPreference(String id, String defaultValue) {
			super(id, defaultValue);
		}

		@Override
		protected String getValue(Object prefs, String defaultValue) {
			return settingsAPI.getString(prefs, getId(), defaultValue);
		}

		@Override
		protected boolean setValue(Object prefs, String val) {
			return settingsAPI.edit(prefs).putString(getId(), (val != null) ? val.trim() : val).commit();
		}

	}
	
	public class ListStringPreference extends StringPreference {

		private String delimiter;

		private ListStringPreference(String id, String defaultValue, String delimiter) {
			super(id, defaultValue);
			this.delimiter = delimiter;
		}
		
		public boolean addValue(String res) {
			String vl = get();
			if (vl == null || vl.isEmpty()) {
				vl = res + delimiter;
			} else {
				vl = vl + res + delimiter;
			}
			set(vl);
			return true;
		}
		
		public void clearAll() {
			set("");
		}
		
		public boolean containsValue(String res) {
			String vl = get();
			String r = res + delimiter;
			return vl.startsWith(r) || vl.indexOf(delimiter + r) >= 0;
		}
		
		public boolean removeValue(String res) {
			String vl = get();
			String r = res + delimiter;
			if(vl != null) {
				if(vl.startsWith(r)) {
					vl = vl.substring(r.length());
					set(vl);
					return true;
				} else {
					int it = vl.indexOf(delimiter + r);
					if(it >= 0) {
						vl = vl.substring(0, it + delimiter.length()) + vl.substring(it + delimiter.length() + r.length());
					}
					set(vl);
					return true;
				}
			}
			return false;
		}


	}

	private class EnumIntPreference<E extends Enum<E>> extends CommonPreference<E> {

		private final E[] values;

		private EnumIntPreference(String id, E defaultValue, E[] values) {
			super(id, defaultValue);
			this.values = values;
		}


		@Override
		protected E getValue(Object prefs, E defaultValue) {
			try {
				int i = settingsAPI.getInt(prefs, getId(), -1);
				if (i >= 0 && i < values.length) {
					return values[i];
				}
			} catch (ClassCastException ex) {
				setValue(prefs, defaultValue);
			}
			return defaultValue;
		}

		@Override
		protected boolean setValue(Object prefs, E val) {
			return settingsAPI.edit(prefs).putInt(getId(), val.ordinal()).commit();
		}

	}
	///////////// PREFERENCES classes ////////////////

	// this value string is synchronized with settings_pref.xml preference name
	private final OsmandPreference<String> PLUGINS = new StringPreference("enabled_plugins", MapillaryPlugin.ID).makeGlobal();

	public Set<String> getEnabledPlugins() {
		String plugs = PLUGINS.get();
		StringTokenizer toks = new StringTokenizer(plugs, ",");
		Set<String> res = new LinkedHashSet<String>();
		while (toks.hasMoreTokens()) {
			String tok = toks.nextToken();
			if (!tok.startsWith("-")) {
				res.add(tok);
			}
		}
		return res;
	}

	public Set<String> getPlugins() {
		String plugs = PLUGINS.get();
		StringTokenizer toks = new StringTokenizer(plugs, ",");
		Set<String> res = new LinkedHashSet<String>();
		while (toks.hasMoreTokens()) {
			res.add(toks.nextToken());
		}
		return res;
	}

	public void enablePlugin(String pluginId, boolean enable) {
		Set<String> set = getPlugins();
		if (enable) {
			set.remove("-" + pluginId);
			set.add(pluginId);
		} else {
			set.remove(pluginId);
			set.add("-" + pluginId);
		}
		StringBuilder serialization = new StringBuilder();
		Iterator<String> it = set.iterator();
		while (it.hasNext()) {
			serialization.append(it.next());
			if (it.hasNext()) {
				serialization.append(",");
			}
		}
		if (!serialization.toString().equals(PLUGINS.get())) {
			PLUGINS.set(serialization.toString());
		}
	}


	@SuppressWarnings("unchecked")
	public CommonPreference<Boolean> registerBooleanPreference(String id, boolean defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<Boolean>) registeredPreferences.get(id);
		}
		BooleanPreference p = new BooleanPreference(id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	@SuppressWarnings("unchecked")
	public CommonPreference<String> registerStringPreference(String id, String defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<String>) registeredPreferences.get(id);
		}
		StringPreference p = new StringPreference(id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	@SuppressWarnings("unchecked")
	public CommonPreference<Integer> registerIntPreference(String id, int defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<Integer>) registeredPreferences.get(id);
		}
		IntPreference p = new IntPreference(id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	@SuppressWarnings("unchecked")
	public CommonPreference<Long> registerLongPreference(String id, long defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<Long>) registeredPreferences.get(id);
		}
		LongPreference p = new LongPreference(id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	@SuppressWarnings("unchecked")
	public CommonPreference<Float> registerFloatPreference(String id, float defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<Float>) registeredPreferences.get(id);
		}
		FloatPreference p = new FloatPreference(id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	public final CommonPreference<Boolean> USE_FAST_RECALCULATION = new BooleanPreference("use_fast_recalculation", true).makeGlobal().cache();
	public final CommonPreference<Boolean> FORCE_PRIVATE_ACCESS_ROUTING_ASKED = new BooleanPreference("force_private_access_routing", false).makeProfile().cache();

	public final CommonPreference<Boolean> SHOW_CARD_TO_CHOOSE_DRAWER = new BooleanPreference("show_card_to_choose_drawer", false).makeGlobal();
	public final CommonPreference<Boolean> SHOW_DASHBOARD_ON_START = new BooleanPreference("should_show_dashboard_on_start", false).makeGlobal();
	public final CommonPreference<Boolean> SHOW_DASHBOARD_ON_MAP_SCREEN = new BooleanPreference("show_dashboard_on_map_screen", false).makeGlobal();

	public final CommonPreference<Integer> NUMBER_OF_STARTS_FIRST_XMAS_SHOWN = new IntPreference("number_of_starts_first_xmas_shown", 0).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> USE_INTERNET_TO_DOWNLOAD_TILES = new BooleanPreference("use_internet_to_download_tiles", true).makeGlobal().cache();

	public final OsmandPreference<String> AVAILABLE_APP_MODES = new StringPreference("available_application_modes", "car,bicycle,pedestrian,").makeGlobal().cache();

	public final OsmandPreference<String> LAST_FAV_CATEGORY_ENTERED = new StringPreference("last_fav_category", "").makeGlobal();


	public final OsmandPreference<ApplicationMode> DEFAULT_APPLICATION_MODE = new CommonPreference<ApplicationMode>("default_application_mode_string", ApplicationMode.DEFAULT) {
		{
			makeGlobal();
		}

		@Override
		protected ApplicationMode getValue(Object prefs, ApplicationMode defaultValue) {
			String key = settingsAPI.getString(prefs, getId(), defaultValue.getStringKey());
			return ApplicationMode.valueOfStringKey(key, defaultValue);
		}

		@Override
		protected boolean setValue(Object prefs, ApplicationMode val) {
			return settingsAPI.edit(prefs).putString(getId(), val.getStringKey()).commit();
		}
	};

	public final OsmandPreference<Boolean> FIRST_MAP_IS_DOWNLOADED = new BooleanPreference(
			"first_map_is_downloaded", false);

	public final CommonPreference<Boolean> DRIVING_REGION_AUTOMATIC = new BooleanPreference("driving_region_automatic", true).makeGlobal().cache();
	public final OsmandPreference<DrivingRegion> DRIVING_REGION = new EnumIntPreference<DrivingRegion>(
			"default_driving_region", DrivingRegion.EUROPE_ASIA, DrivingRegion.values()) {
		protected boolean setValue(Object prefs, DrivingRegion val) {
			if (val != null) {
				METRIC_SYSTEM.set(val.defMetrics);
			}
			return super.setValue(prefs, val);
		}

		;

		protected DrivingRegion getDefaultValue() {
			Locale df = Locale.getDefault();
			if (df == null) {
				return DrivingRegion.EUROPE_ASIA;
			}
			if (df.getCountry().equalsIgnoreCase(Locale.US.getCountry())) {
				return DrivingRegion.US;
			} else if (df.getCountry().equalsIgnoreCase(Locale.CANADA.getCountry())) {
				return DrivingRegion.CANADA;
			} else if (df.getCountry().equalsIgnoreCase(Locale.JAPAN.getCountry())) {
				return DrivingRegion.JAPAN;
			} else if (df.getCountry().equalsIgnoreCase("au")) {
				return DrivingRegion.AUSTRALIA;
// potentially wrong in Europe
//			} else if(df.getCountry().equalsIgnoreCase(Locale.UK.getCountry())) {
//				return DrivingRegion.UK_AND_OTHERS;
			}
			return DrivingRegion.EUROPE_ASIA;
		}

		;
	}.makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	// cache of metrics constants as they are used very often
	public final OsmandPreference<MetricsConstants> METRIC_SYSTEM = new EnumIntPreference<MetricsConstants>(
			"default_metric_system", MetricsConstants.KILOMETERS_AND_METERS, MetricsConstants.values()) {
		protected MetricsConstants getDefaultValue() {
			return DRIVING_REGION.get().defMetrics;
		}

		;
	}.makeGlobal();


	public final OsmandPreference<SpeedConstants> SPEED_SYSTEM = new EnumIntPreference<SpeedConstants>(
			"default_speed_system", SpeedConstants.KILOMETERS_PER_HOUR, SpeedConstants.values()) {

		@Override
		public SpeedConstants getProfileDefaultValue(ApplicationMode mode) {
			MetricsConstants mc = METRIC_SYSTEM.get();
			if (mode.isDerivedRoutingFrom(ApplicationMode.PEDESTRIAN)) {
				if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
					return SpeedConstants.MINUTES_PER_KILOMETER;
				} else {
					return SpeedConstants.MILES_PER_HOUR;
				}
			}
			if (mode.isDerivedRoutingFrom(ApplicationMode.BOAT)) {
				return SpeedConstants.NAUTICALMILES_PER_HOUR;
			}
			if (mc == MetricsConstants.NAUTICAL_MILES) {
				return SpeedConstants.NAUTICALMILES_PER_HOUR;
			} else if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
				return SpeedConstants.KILOMETERS_PER_HOUR;
			} else {
				return SpeedConstants.MILES_PER_HOUR;
			}
		}

		;

	}.makeProfile();


	// this value string is synchronized with settings_pref.xml preference name
	// cache of metrics constants as they are used very often
	public final OsmandPreference<RelativeDirectionStyle> DIRECTION_STYLE = new EnumIntPreference<RelativeDirectionStyle>(
			"direction_style", RelativeDirectionStyle.SIDEWISE, RelativeDirectionStyle.values()).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	// cache of metrics constants as they are used very often
	public final OsmandPreference<AccessibilityMode> ACCESSIBILITY_MODE = new EnumIntPreference<AccessibilityMode>(
			"accessibility_mode", AccessibilityMode.DEFAULT, AccessibilityMode.values()).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Float> SPEECH_RATE =
			new FloatPreference("speech_rate", 1f).makeGlobal();

	public final OsmandPreference<Float> ARRIVAL_DISTANCE_FACTOR =
			new FloatPreference("arrival_distance_factor", 1f).makeProfile();

	public final OsmandPreference<Float> SPEED_LIMIT_EXCEED =
			new FloatPreference("speed_limit_exceed", 5f).makeProfile();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> USE_TRACKBALL_FOR_MOVEMENTS =
			new BooleanPreference("use_trackball_for_movements", true).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> ACCESSIBILITY_SMART_AUTOANNOUNCE =
		new BooleanAccessibilityPreference("accessibility_smart_autoannounce", true).makeGlobal();
	
	// this value string is synchronized with settings_pref.xml preference name
	// cache of metrics constants as they are used very often
	public final OsmandPreference<Integer> ACCESSIBILITY_AUTOANNOUNCE_PERIOD = new IntPreference("accessibility_autoannounce_period", 10000).makeGlobal().cache();
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> DISABLE_OFFROUTE_RECALC =
		new BooleanAccessibilityPreference("disable_offroute_recalc", false).makeGlobal();
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> DISABLE_WRONG_DIRECTION_RECALC =
		new BooleanAccessibilityPreference("disable_wrong_direction_recalc", false).makeGlobal();
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> DIRECTION_AUDIO_FEEDBACK =
		new BooleanAccessibilityPreference("direction_audio_feedback", false).makeGlobal();
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> DIRECTION_HAPTIC_FEEDBACK =
		new BooleanAccessibilityPreference("direction_haptic_feedback", false).makeGlobal();
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> ZOOM_BY_TRACKBALL =
			new BooleanAccessibilityPreference("zoom_by_trackball", false).makeGlobal();


	// magnetic field doesn'torkmost of the time on some phones
	public final OsmandPreference<Boolean> USE_MAGNETIC_FIELD_SENSOR_COMPASS = new BooleanPreference("use_magnetic_field_sensor_compass", false).makeGlobal().cache();
	public final OsmandPreference<Boolean> USE_KALMAN_FILTER_FOR_COMPASS = new BooleanPreference("use_kalman_filter_compass", true).makeGlobal().cache();

	public final OsmandPreference<Boolean> DO_NOT_SHOW_STARTUP_MESSAGES = new BooleanPreference("do_not_show_startup_messages", false).makeGlobal().cache();
	public final OsmandPreference<Boolean> DO_NOT_SEND_ANONYMOUS_APP_USAGE = new BooleanPreference("do_not_send_anonymous_app_usage", false).makeGlobal().cache();


	public final CommonPreference<Float> TEXT_SCALE = new FloatPreference("text_scale", 1f).makeProfile().cache();

	{
		TEXT_SCALE.setModeDefaultValue(ApplicationMode.CAR, 1.25f);
	}

	public final CommonPreference<Float> MAP_DENSITY = new FloatPreference("map_density_n", 1f).makeProfile().cache();

	{
		MAP_DENSITY.setModeDefaultValue(ApplicationMode.CAR, 1.5f);
	}


	public final OsmandPreference<Boolean> SHOW_POI_LABEL = new BooleanPreference("show_poi_label", false).makeGlobal();

	public final OsmandPreference<Boolean> SHOW_MAPILLARY = new BooleanPreference("show_mapillary", false).makeGlobal();
	public final OsmandPreference<Boolean> MAPILLARY_FIRST_DIALOG_SHOWN = new BooleanPreference("mapillary_first_dialog_shown", false).makeGlobal();
	public final OsmandPreference<Boolean> MAPILLARY_MENU_COLLAPSED = new BooleanPreference("mapillary_menu_collapsed", false).makeGlobal();
	public final OsmandPreference<Boolean> WEBGL_SUPPORTED = new BooleanPreference("webgl_supported", true).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<String> PREFERRED_LOCALE = new StringPreference("preferred_locale", "").makeGlobal();

	public static final String TRANSPORT_STOPS_OVER_MAP = "transportStops";

	public final OsmandPreference<String> MAP_PREFERRED_LOCALE = new StringPreference("map_preferred_locale", "").makeGlobal().cache();
	public final OsmandPreference<Boolean> MAP_TRANSLITERATE_NAMES = new BooleanPreference("map_transliterate_names", false).makeGlobal().cache();

	public boolean usingEnglishNames() {
		return MAP_PREFERRED_LOCALE.get().equals("en");
	}

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<String> USER_NAME = new StringPreference("user_name", "").makeGlobal();

	public static final String BILLING_USER_DONATION_WORLD_PARAMETER = "";
	public static final String BILLING_USER_DONATION_NONE_PARAMETER = "none";

	public final OsmandPreference<String> BILLING_USER_ID = new StringPreference("billing_user_id", "").makeGlobal();
	public final OsmandPreference<String> BILLING_USER_TOKEN = new StringPreference("billing_user_token", "").makeGlobal();
	public final OsmandPreference<String> BILLING_USER_NAME = new StringPreference("billing_user_name", "").makeGlobal();
	public final OsmandPreference<String> BILLING_USER_EMAIL = new StringPreference("billing_user_email", "").makeGlobal();
	public final OsmandPreference<String> BILLING_USER_COUNTRY = new StringPreference("billing_user_country", "").makeGlobal();
	public final OsmandPreference<String> BILLING_USER_COUNTRY_DOWNLOAD_NAME = new StringPreference("billing_user_country_download_name", BILLING_USER_DONATION_NONE_PARAMETER).makeGlobal();
	public final OsmandPreference<Boolean> BILLING_HIDE_USER_NAME = new BooleanPreference("billing_hide_user_name", false).makeGlobal();
	public final OsmandPreference<Boolean> BILLING_PURCHASE_TOKEN_SENT = new BooleanPreference("billing_purchase_token_sent", false).makeGlobal();
	public final OsmandPreference<Boolean> LIVE_UPDATES_PURCHASED = new BooleanPreference("billing_live_updates_purchased", false).makeGlobal();
	public final OsmandPreference<Boolean> FULL_VERSION_PURCHASED = new BooleanPreference("billing_full_version_purchased", false).makeGlobal();
	public final OsmandPreference<Boolean> DEPTH_CONTOURS_PURCHASED = new BooleanPreference("billing_sea_depth_purchased", false).makeGlobal();
	public final OsmandPreference<Boolean> EMAIL_SUBSCRIBED = new BooleanPreference("email_subscribed", false).makeGlobal();

	public final OsmandPreference<Integer> DISCOUNT_ID = new IntPreference("discount_id", 0).makeGlobal();
	public final OsmandPreference<Integer> DISCOUNT_SHOW_NUMBER_OF_STARTS = new IntPreference("number_of_starts_on_discount_show", 0).makeGlobal();
	public final OsmandPreference<Integer> DISCOUNT_TOTAL_SHOW = new IntPreference("discount_total_show", 0).makeGlobal();
	public final OsmandPreference<Long> DISCOUNT_SHOW_DATETIME_MS = new LongPreference("show_discount_datetime_ms", 0).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<String> USER_OSM_BUG_NAME =
			new StringPreference("user_osm_bug_name", "NoName/OsmAnd").makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<String> USER_PASSWORD =
			new StringPreference("user_password", "").makeGlobal();

	// this value boolean is synchronized with settings_pref.xml preference offline POI/Bugs edition
	public final OsmandPreference<Boolean> OFFLINE_EDITION = new BooleanPreference("offline_osm_editing", true).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<DayNightMode> DAYNIGHT_MODE =
			new EnumIntPreference<DayNightMode>("daynight_mode", DayNightMode.DAY, DayNightMode.values());

	{
		DAYNIGHT_MODE.makeProfile().cache();
		DAYNIGHT_MODE.setModeDefaultValue(ApplicationMode.CAR, DayNightMode.AUTO);
		DAYNIGHT_MODE.setModeDefaultValue(ApplicationMode.BICYCLE, DayNightMode.AUTO);
		DAYNIGHT_MODE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, DayNightMode.DAY);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<RouteService> ROUTER_SERVICE =
			new EnumIntPreference<RouteService>("router_service", RouteService.OSMAND,
					RouteService.values()).makeProfile();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> AUTO_ZOOM_MAP = new BooleanPreference("auto_zoom_map_on_off", false).makeProfile().cache();
	{
		AUTO_ZOOM_MAP.setModeDefaultValue(ApplicationMode.CAR, true);
		AUTO_ZOOM_MAP.setModeDefaultValue(ApplicationMode.BICYCLE, false);
		AUTO_ZOOM_MAP.setModeDefaultValue(ApplicationMode.PEDESTRIAN, false);
	}

	public final CommonPreference<AutoZoomMap> AUTO_ZOOM_MAP_SCALE =
			new EnumIntPreference<AutoZoomMap>("auto_zoom_map_scale", AutoZoomMap.FAR,
					AutoZoomMap.values()).makeProfile().cache();
	{
		AUTO_ZOOM_MAP_SCALE.setModeDefaultValue(ApplicationMode.CAR, AutoZoomMap.FAR);
		AUTO_ZOOM_MAP_SCALE.setModeDefaultValue(ApplicationMode.BICYCLE, AutoZoomMap.CLOSE);
		AUTO_ZOOM_MAP_SCALE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, AutoZoomMap.CLOSE);
	}

	public final CommonPreference<Integer> DELAY_TO_START_NAVIGATION = new IntPreference("delay_to_start_navigation", -1) {

		protected Integer getDefaultValue() {
			if (DEFAULT_APPLICATION_MODE.get().isDerivedRoutingFrom(ApplicationMode.CAR)) {
				return 10;
			}
			return -1;
		}

		;
	}.makeGlobal().cache();

	public final CommonPreference<Boolean> SNAP_TO_ROAD = new BooleanPreference("snap_to_road", false).makeProfile().cache();

	{
		SNAP_TO_ROAD.setModeDefaultValue(ApplicationMode.CAR, true);
		SNAP_TO_ROAD.setModeDefaultValue(ApplicationMode.BICYCLE, true);
	}

	public final CommonPreference<Boolean> INTERRUPT_MUSIC = new BooleanPreference("interrupt_music", false).makeProfile();

	public final CommonPreference<String> PROXY_HOST = new StringPreference("proxy_host", "127.0.0.1").makeGlobal();
	public final CommonPreference<Integer> PROXY_PORT = new IntPreference("proxy_port", 8118).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public static final String SAVE_CURRENT_TRACK = "save_current_track"; //$NON-NLS-1$

	public final CommonPreference<Boolean> SAVE_GLOBAL_TRACK_TO_GPX = new BooleanPreference("save_global_track_to_gpx", false).makeGlobal().cache();
	public final CommonPreference<Integer> SAVE_GLOBAL_TRACK_INTERVAL = new IntPreference("save_global_track_interval", 5000).makeGlobal().cache();
	public final CommonPreference<Boolean> SAVE_GLOBAL_TRACK_REMEMBER = new BooleanPreference("save_global_track_remember", false).makeGlobal().cache();
	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> SAVE_TRACK_TO_GPX = new BooleanPreference("save_track_to_gpx", false).makeProfile().cache();

	{
		SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.CAR, true);
		SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.BICYCLE, true);
		SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.PEDESTRIAN, false);
	}

	public final CommonPreference<Boolean> DISABLE_RECORDING_ONCE_APP_KILLED = new BooleanPreference("disable_recording_once_app_killed", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> FAST_ROUTE_MODE = new BooleanPreference("fast_route_mode", true).makeProfile();
	// temporarily for new version
	public final CommonPreference<Boolean> DISABLE_COMPLEX_ROUTING = new BooleanPreference("disable_complex_routing", false).makeGlobal();

	public final CommonPreference<Boolean> SHOW_TRAFFIC_WARNINGS = new BooleanPreference("show_traffic_warnings", false).makeProfile().cache();

	{
		SHOW_TRAFFIC_WARNINGS.setModeDefaultValue(ApplicationMode.CAR, true);
	}

	public final CommonPreference<Boolean> SHOW_PEDESTRIAN = new BooleanPreference("show_pedestrian", false).makeProfile().cache();

	{
		SHOW_PEDESTRIAN.setModeDefaultValue(ApplicationMode.CAR, true);
	}

	public final OsmandPreference<Boolean> SHOW_CAMERAS = new BooleanPreference("show_cameras", false).makeProfile().cache();
	public final CommonPreference<Boolean> SHOW_LANES = new BooleanPreference("show_lanes", false).makeProfile().cache();

	{
		SHOW_LANES.setModeDefaultValue(ApplicationMode.CAR, true);
		SHOW_LANES.setModeDefaultValue(ApplicationMode.BICYCLE, true);
	}

	public final OsmandPreference<Boolean> SHOW_WPT = new BooleanPreference("show_gpx_wpt", true).makeGlobal().cache();
	public final OsmandPreference<Boolean> SHOW_NEARBY_FAVORITES = new BooleanPreference("show_nearby_favorites", false).makeProfile().cache();
	public final OsmandPreference<Boolean> SHOW_NEARBY_POI = new BooleanPreference("show_nearby_poi", false).makeProfile().cache();

	public final OsmandPreference<Boolean> SPEAK_STREET_NAMES = new BooleanPreference("speak_street_names", true).makeProfile().cache();
	public final OsmandPreference<Boolean> SPEAK_TRAFFIC_WARNINGS = new BooleanPreference("speak_traffic_warnings", true).makeProfile().cache();
	public final OsmandPreference<Boolean> SPEAK_PEDESTRIAN = new BooleanPreference("speak_pedestrian", true).makeProfile().cache();
	public final OsmandPreference<Boolean> SPEAK_SPEED_LIMIT = new BooleanPreference("speak_speed_limit", true).makeProfile().cache();
	public final OsmandPreference<Boolean> SPEAK_SPEED_CAMERA = new BooleanPreference("speak_cameras", false).makeProfile().cache();
	public final OsmandPreference<Boolean> ANNOUNCE_WPT = new BooleanPreference("announce_wpt", true).makeGlobal().cache();
	public final OsmandPreference<Boolean> ANNOUNCE_NEARBY_FAVORITES = new BooleanPreference("announce_nearby_favorites", false).makeProfile().cache();
	public final OsmandPreference<Boolean> ANNOUNCE_NEARBY_POI = new BooleanPreference("announce_nearby_poi", false).makeProfile().cache();

	public final OsmandPreference<Boolean> GPX_ROUTE_CALC_OSMAND_PARTS = new BooleanPreference("gpx_routing_calculate_osmand_route", true).makeGlobal().cache();
	public final OsmandPreference<Boolean> GPX_CALCULATE_RTEPT = new BooleanPreference("gpx_routing_calculate_rtept", true).makeGlobal().cache();
	public final OsmandPreference<Boolean> GPX_ROUTE_CALC = new BooleanPreference("calc_gpx_route", false).makeGlobal().cache();

	public final OsmandPreference<Boolean> AVOID_TOLL_ROADS = new BooleanPreference("avoid_toll_roads", false).makeProfile().cache();
	public final OsmandPreference<Boolean> AVOID_MOTORWAY = new BooleanPreference("avoid_motorway", false).makeProfile().cache();
	public final OsmandPreference<Boolean> AVOID_UNPAVED_ROADS = new BooleanPreference("avoid_unpaved_roads", false).makeProfile().cache();
	public final OsmandPreference<Boolean> AVOID_FERRIES = new BooleanPreference("avoid_ferries", false).makeProfile().cache();

	public final OsmandPreference<Boolean> PREFER_MOTORWAYS = new BooleanPreference("prefer_motorways", false).makeProfile().cache();

	public final OsmandPreference<Long> LAST_UPDATES_CARD_REFRESH = new LongPreference("last_updates_card_refresh", 0).makeGlobal();

	public final CommonPreference<Integer> CURRENT_TRACK_COLOR = new IntPreference("current_track_color", 0).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Integer> SAVE_TRACK_INTERVAL = new IntPreference("save_track_interval", 5000).makeProfile();

	{
		SAVE_TRACK_INTERVAL.setModeDefaultValue(ApplicationMode.CAR, 3000);
		SAVE_TRACK_INTERVAL.setModeDefaultValue(ApplicationMode.BICYCLE, 7000);
		SAVE_TRACK_INTERVAL.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 10000);
	}
	
	// Please note that SAVE_TRACK_MIN_DISTANCE, SAVE_TRACK_PRECISION, SAVE_TRACK_MIN_SPEED should all be "0" for the default profile, as we have no interface to change them
	public final CommonPreference<Float> SAVE_TRACK_MIN_DISTANCE = new FloatPreference("save_track_min_distance", 0).makeGlobal();
	//{
	//	SAVE_TRACK_MIN_DISTANCE.setModeDefaultValue(ApplicationMode.CAR, 5.f);
	//	SAVE_TRACK_MIN_DISTANCE.setModeDefaultValue(ApplicationMode.BICYCLE, 5.f);
	//	SAVE_TRACK_MIN_DISTANCE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 5.f);
	//}
	public final CommonPreference<Float> SAVE_TRACK_PRECISION = new FloatPreference("save_track_precision", 0.f).makeGlobal();
	//{
//		SAVE_TRACK_PRECISION.setModeDefaultValue(ApplicationMode.CAR, 50.f);
//		SAVE_TRACK_PRECISION.setModeDefaultValue(ApplicationMode.BICYCLE, 50.f);
//		SAVE_TRACK_PRECISION.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 50.f);
	//}
	public final CommonPreference<Float> SAVE_TRACK_MIN_SPEED = new FloatPreference("save_track_min_speed", 0.f).makeGlobal();
	//{
	//	SAVE_TRACK_MIN_SPEED.setModeDefaultValue(ApplicationMode.CAR, 2.f);
	//	SAVE_TRACK_MIN_SPEED.setModeDefaultValue(ApplicationMode.BICYCLE, 1.f);
//		SAVE_TRACK_MIN_SPEED.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 0.f);
	//}
	public final CommonPreference<Boolean> AUTO_SPLIT_RECORDING = new BooleanPreference("auto_split_recording", true).makeGlobal();

	public final CommonPreference<Boolean> SHOW_TRIP_REC_NOTIFICATION = new BooleanPreference("show_trip_recording_notification", true).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> LIVE_MONITORING = new BooleanPreference("live_monitoring", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Integer> LIVE_MONITORING_INTERVAL = new IntPreference("live_monitoring_interval", 5000).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<String> LIVE_MONITORING_URL = new StringPreference("live_monitoring_url",
			"http://example.com?lat={0}&lon={1}&timestamp={2}&hdop={3}&altitude={4}&speed={5}").makeGlobal();

	public final CommonPreference<String> GPS_STATUS_APP = new StringPreference("gps_status_app", "").makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> SHOW_OSM_BUGS = new BooleanPreference("show_osm_bugs", false).makeGlobal();

	public final CommonPreference<String> MAP_INFO_CONTROLS = new StringPreference("map_info_controls", "").makeProfile();
	{
		for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
			MAP_INFO_CONTROLS.setModeDefaultValue(mode, "");
		}
	}

	public final OsmandPreference<String> OSMO_DEVICE_KEY = new StringPreference("osmo_device_token", "").makeGlobal();

	public final OsmandPreference<String> OSMO_USER_NAME = new StringPreference("osmo_user_name", "").makeGlobal();

	public final OsmandPreference<String> OSMO_USER_PWD = new StringPreference("osmo_user_pwd", null).makeGlobal();

	public final OsmandPreference<Boolean> OSMO_AUTO_CONNECT = new BooleanPreference("osmo_automatically_connect", false).makeGlobal();

	public final OsmandPreference<Long> OSMO_LAST_PING = new LongPreference("osmo_last_ping", 0).makeGlobal().cache();

	public final OsmandPreference<Boolean> OSMO_SEND_LOCATIONS_STATE = new BooleanPreference("osmo_send_locations", false).cache().makeGlobal();

	public final OsmandPreference<Boolean> OSMO_SHOW_GROUP_NOTIFICATIONS = new BooleanPreference("osmo_show_toast_notifications", true).makeGlobal();

	public final CommonPreference<Integer> OSMO_SAVE_TRACK_INTERVAL = new IntPreference("osmo_save_track_interval", 10000).makeGlobal().cache();

	public final OsmandPreference<String> OSMO_GROUPS = new StringPreference("osmo_groups", "{}").makeGlobal();

	public final OsmandPreference<Boolean> NO_DISCOUNT_INFO = new BooleanPreference("no_discount_info", false).makeGlobal();

	public final OsmandPreference<Boolean> SHOW_LEGACY_SEARCH = new BooleanPreference("show_legacy_search", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> DEBUG_RENDERING_INFO = new BooleanPreference("debug_rendering", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> SHOW_FAVORITES = new BooleanPreference("show_favorites", true).makeGlobal().cache();

	public final CommonPreference<Boolean> SHOW_ZOOM_BUTTONS_NAVIGATION = new BooleanPreference("show_zoom_buttons_navigation", false).makeProfile().cache();

	{
		SHOW_ZOOM_BUTTONS_NAVIGATION.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
	}

	// Json
	public final OsmandPreference<String> SELECTED_GPX = new StringPreference("selected_gpx", "").makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> MAP_SCREEN_ORIENTATION =
			new IntPreference("map_screen_orientation", -1/*ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED*/).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
//	public final CommonPreference<Boolean> SHOW_VIEW_ANGLE = new BooleanPreference("show_view_angle", false).makeProfile().cache();
//	{
//		SHOW_VIEW_ANGLE.setModeDefaultValue(ApplicationMode.CAR, false);
//		SHOW_VIEW_ANGLE.setModeDefaultValue(ApplicationMode.BICYCLE, true);
//		SHOW_VIEW_ANGLE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
//	}

	// this value string is synchronized with settings_pref.xml preference name
	// seconds to auto_follow
	public final CommonPreference<Integer> AUTO_FOLLOW_ROUTE = new IntPreference("auto_follow_route", 0).makeProfile();

	{
		AUTO_FOLLOW_ROUTE.setModeDefaultValue(ApplicationMode.CAR, 15);
		AUTO_FOLLOW_ROUTE.setModeDefaultValue(ApplicationMode.BICYCLE, 15);
		AUTO_FOLLOW_ROUTE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 0);
	}

	// this value string is synchronized with settings_pref.xml preference name
	// seconds to auto_follow
	public final CommonPreference<Integer> KEEP_INFORMING = new IntPreference("keep_informing", 0).makeProfile();

	{
		// 0 means never
		KEEP_INFORMING.setModeDefaultValue(ApplicationMode.CAR, 0);
		KEEP_INFORMING.setModeDefaultValue(ApplicationMode.BICYCLE, 0);
		KEEP_INFORMING.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 0);
	}

	// screen power save
	public final CommonPreference<Integer> WAKE_ON_VOICE_INT = new IntPreference("wake_on_voice_int", 0).makeProfile();

	{
		// 0 means never
		WAKE_ON_VOICE_INT.setModeDefaultValue(ApplicationMode.CAR, 0);
		WAKE_ON_VOICE_INT.setModeDefaultValue(ApplicationMode.BICYCLE, 0);
		WAKE_ON_VOICE_INT.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 0);
	}

	// this value string is synchronized with settings_pref.xml preference name
	// try without AUTO_FOLLOW_ROUTE_NAV (see forum discussion 'Simplify our navigation preference menu')
	//public final CommonPreference<Boolean> AUTO_FOLLOW_ROUTE_NAV = new BooleanPreference("auto_follow_route_navigation", true, false);

	// this value string is synchronized with settings_pref.xml preference name
	public static final int ROTATE_MAP_NONE = 0;
	public static final int ROTATE_MAP_BEARING = 1;
	public static final int ROTATE_MAP_COMPASS = 2;
	public final CommonPreference<Integer> ROTATE_MAP =
			new IntPreference("rotate_map", ROTATE_MAP_NONE).makeProfile().cache();

	{
		ROTATE_MAP.setModeDefaultValue(ApplicationMode.CAR, ROTATE_MAP_BEARING);
		ROTATE_MAP.setModeDefaultValue(ApplicationMode.BICYCLE, ROTATE_MAP_BEARING);
		ROTATE_MAP.setModeDefaultValue(ApplicationMode.PEDESTRIAN, ROTATE_MAP_COMPASS);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final int CENTER_CONSTANT = 0;
	public static final int BOTTOM_CONSTANT = 1;
	public static final int MIDDLE_CONSTANT = 2;
	public final CommonPreference<Boolean> CENTER_POSITION_ON_MAP = new BooleanPreference("center_position_on_map", false).makeProfile();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> MAX_LEVEL_TO_DOWNLOAD_TILE = new IntPreference("max_level_download_tile", 20).makeProfile().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> LEVEL_TO_SWITCH_VECTOR_RASTER = new IntPreference("level_to_switch_vector_raster", 1).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> AUDIO_STREAM_GUIDANCE = new IntPreference("audio_stream",
			3/*AudioManager.STREAM_MUSIC*/).makeProfile();

	// For now this can be changed only in TestVoiceActivity
	public final OsmandPreference<Integer> BT_SCO_DELAY = new IntPreference("bt_sco_delay",	1500).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> MAP_ONLINE_DATA = new BooleanPreference("map_online_data", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> SHOW_DESTINATION_ARROW = new BooleanPreference("show_destination_arrow", false).makeProfile();

	{
		SHOW_DESTINATION_ARROW.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
	}

	public final CommonPreference<String> CONTOUR_LINES_ZOOM = new StringPreference("contour_lines_zoom", null).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<String> MAP_OVERLAY = new StringPreference("map_overlay", null).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<String> MAP_UNDERLAY = new StringPreference("map_underlay", null).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Integer> MAP_OVERLAY_TRANSPARENCY = new IntPreference("overlay_transparency",
			100).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Integer> MAP_TRANSPARENCY = new IntPreference("map_transparency",
			255).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<String> MAP_TILE_SOURCES = new StringPreference("map_tile_sources",
			TileSourceManager.getMapnikSource().getName()).makeGlobal();

	public final CommonPreference<LayerTransparencySeekbarMode> LAYER_TRANSPARENCY_SEEKBAR_MODE =
			new EnumIntPreference<>("layer_transparency_seekbar_mode", LayerTransparencySeekbarMode.UNDEFINED, LayerTransparencySeekbarMode.values());

	public final CommonPreference<String> MAP_OVERLAY_PREVIOUS = new StringPreference("map_overlay_previous", null).makeGlobal().cache();

	public final CommonPreference<String> MAP_UNDERLAY_PREVIOUS = new StringPreference("map_underlay_previous", null).makeGlobal().cache();

	public CommonPreference<String> PREVIOUS_INSTALLED_VERSION = new StringPreference("previous_installed_version", "").makeGlobal();

	public final OsmandPreference<Boolean> SHOULD_SHOW_FREE_VERSION_BANNER = new BooleanPreference("should_show_free_version_banner", false).makeGlobal().cache();

	public final OsmandPreference<Boolean> USE_MAP_MARKERS = new BooleanPreference("use_map_markers", true).makeGlobal().cache();

	public final CommonPreference<MapMarkersMode> MAP_MARKERS_MODE =
			new EnumIntPreference<>("map_markers_mode", MapMarkersMode.TOOLBAR, MapMarkersMode.values());

	{
		MAP_MARKERS_MODE.makeProfile().cache();
		MAP_MARKERS_MODE.setModeDefaultValue(ApplicationMode.DEFAULT, MapMarkersMode.TOOLBAR);
		MAP_MARKERS_MODE.setModeDefaultValue(ApplicationMode.CAR, MapMarkersMode.TOOLBAR);
		MAP_MARKERS_MODE.setModeDefaultValue(ApplicationMode.BICYCLE, MapMarkersMode.TOOLBAR);
		MAP_MARKERS_MODE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, MapMarkersMode.TOOLBAR);
	}

	public final OsmandPreference<Boolean> ANIMATE_MY_LOCATION = new BooleanPreference("animate_my_location", true).makeGlobal().cache();

	public final OsmandPreference<Boolean> ROUTE_MAP_MARKERS_START_MY_LOC = new BooleanPreference("route_map_markers_start_my_loc", false).makeGlobal().cache();

	public ITileSource getMapTileSource(boolean warnWhenSelected) {
		String tileName = MAP_TILE_SOURCES.get();
		if (tileName != null) {
			ITileSource ts = getTileSourceByName(tileName, warnWhenSelected);
			if (ts != null) {
				return ts;
			}
		}
		return TileSourceManager.getMapnikSource();
	}

	private TileSourceTemplate checkAmongAvailableTileSources(File dir, List<TileSourceTemplate> list) {
		if (list != null) {
			for (TileSourceTemplate l : list) {
				if (dir.getName().equals(l.getName())) {
					try {
						dir.mkdirs();
						TileSourceManager.createMetaInfoFile(dir, l, true);
					} catch (IOException e) {
					}
					return l;
				}

			}
		}
		return null;
	}


	public ITileSource getTileSourceByName(String tileName, boolean warnWhenSelected) {
		if (tileName == null || tileName.length() == 0) {
			return null;
		}
		List<TileSourceTemplate> knownTemplates = TileSourceManager.getKnownSourceTemplates();
		File tPath = ctx.getAppPath(IndexConstants.TILES_INDEX_DIR);
		File dir = new File(tPath, tileName);
		if (!dir.exists()) {
			return checkAmongAvailableTileSources(dir, knownTemplates);
		} else if (tileName.endsWith(IndexConstants.SQLITE_EXT)) {
			return new SQLiteTileSource(ctx, dir, knownTemplates);
		} else if (dir.isDirectory() && !dir.getName().startsWith(".")) {
			TileSourceTemplate t = TileSourceManager.createTileSourceTemplate(dir);
			if (warnWhenSelected && !t.isRuleAcceptable()) {
				ctx.showToastMessage(R.string.warning_tile_layer_not_downloadable, dir.getName());
			}
			if (!TileSourceManager.isTileSourceMetaInfoExist(dir)) {
				TileSourceTemplate ret = checkAmongAvailableTileSources(dir, knownTemplates);
				if (ret != null) {
					t = ret;
				}
			}
			return t;
		}
		return null;
	}

	public boolean installTileSource(TileSourceTemplate toInstall) {
		File tPath = ctx.getAppPath(IndexConstants.TILES_INDEX_DIR);
		File dir = new File(tPath, toInstall.getName());
		dir.mkdirs();
		if (dir.exists() && dir.isDirectory()) {
			try {
				TileSourceManager.createMetaInfoFile(dir, toInstall, true);
			} catch (IOException e) {
				return false;
			}
		}
		return true;
	}

	public Map<String, String> getTileSourceEntries() {
		return getTileSourceEntries(true);

	}

	public Map<String, String> getTileSourceEntries(boolean sqlite) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		File dir = ctx.getAppPath(IndexConstants.TILES_INDEX_DIR);
		if (dir != null && dir.canRead()) {
			File[] files = dir.listFiles();
			Arrays.sort(files, new Comparator<File>() {
				@Override
				public int compare(File object1, File object2) {
					if (object1.lastModified() > object2.lastModified()) {
						return -1;
					} else if (object1.lastModified() == object2.lastModified()) {
						return 0;
					}
					return 1;
				}

			});
			if (files != null) {
				for (File f : files) {
					if (f.getName().endsWith(IndexConstants.SQLITE_EXT)) {
						if (sqlite) {
							String n = f.getName();
							map.put(f.getName(), n.substring(0, n.lastIndexOf('.')));
						}
					} else if (f.isDirectory() && !f.getName().equals(IndexConstants.TEMP_SOURCE_TO_LOAD)
							&& !f.getName().startsWith(".")) {
						map.put(f.getName(), f.getName());
					}
				}
			}
		}
		for (TileSourceTemplate l : TileSourceManager.getKnownSourceTemplates()) {
			if (!l.isHidden()) {
				map.put(l.getName(), l.getName());
			} else {
				map.remove(l.getName());
			}
		}
		return map;

	}

	public static final String EXTERNAL_STORAGE_DIR = "external_storage_dir"; //$NON-NLS-1$

	public static final String EXTERNAL_STORAGE_DIR_V19 = "external_storage_dir_V19"; //$NON-NLS-1$
	public static final String EXTERNAL_STORAGE_DIR_TYPE_V19 = "external_storage_dir_type_V19"; //$NON-NLS-1$
	public static final int EXTERNAL_STORAGE_TYPE_DEFAULT = 0; // Environment.getExternalStorageDirectory()
	public static final int EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE = 1; // ctx.getExternalFilesDirs(null)
	public static final int EXTERNAL_STORAGE_TYPE_INTERNAL_FILE = 2; // ctx.getFilesDir()
	public static final int EXTERNAL_STORAGE_TYPE_OBB = 3; // ctx.getObbDirs
	public static final int EXTERNAL_STORAGE_TYPE_SPECIFIED = 4;


	public void freezeExternalStorageDirectory() {
		if (Build.VERSION.SDK_INT >= 19) {
			int type = settingsAPI.getInt(globalPreferences, EXTERNAL_STORAGE_DIR_TYPE_V19, -1);
			if (type == -1) {
				ValueHolder<Integer> vh = new ValueHolder<>();
				File f = getExternalStorageDirectoryV19(vh);
				setExternalStorageDirectoryV19(vh.value, f.getAbsolutePath());
			}
		}
	}

	public void initExternalStorageDirectory() {
		if (Build.VERSION.SDK_INT < 19) {
			setExternalStorageDirectoryPre19(getInternalAppPath().getAbsolutePath());
		} else {
			File externalStorage = getExternal1AppPath();
			if (externalStorage != null && OsmandSettings.isWritable(externalStorage)) {
				setExternalStorageDirectoryV19(EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE,
						getExternal1AppPath().getAbsolutePath());
			} else {
				setExternalStorageDirectoryV19(EXTERNAL_STORAGE_TYPE_INTERNAL_FILE,
						getInternalAppPath().getAbsolutePath());
			}
		}
	}

	public File getExternalStorageDirectory() {
		return getExternalStorageDirectory(null);
	}

	public File getExternalStorageDirectory(ValueHolder<Integer> type) {
		if (Build.VERSION.SDK_INT < 19) {
			return getExternalStorageDirectoryPre19();
		} else {
			return getExternalStorageDirectoryV19(type);
		}
	}

	public File getInternalAppPath() {
		if (Build.VERSION.SDK_INT >= 21) {
			File fl = getNoBackupPath();
			if (fl != null) {
				return fl;
			}
		}
		return ctx.getFilesDir();
	}

	@TargetApi(19)
	public File getExternal1AppPath() {
		File[] externals = ctx.getExternalFilesDirs(null);
		if (externals != null && externals.length > 0) {
			return externals[0];
		} else {
			return null;
		}
	}

	@TargetApi(21)
	private File getNoBackupPath() {
		return ctx.getNoBackupFilesDir();
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	public File getExternalStorageDirectoryV19(ValueHolder<Integer> tp) {
		int type = settingsAPI.getInt(globalPreferences, EXTERNAL_STORAGE_DIR_TYPE_V19, -1);
		File location = getDefaultLocationV19();
		if (type == -1) {
			if (isWritable(location)) {
				if (tp != null) {
					tp.value = settingsAPI.contains(globalPreferences, EXTERNAL_STORAGE_DIR_V19) ?
							EXTERNAL_STORAGE_TYPE_SPECIFIED :
							EXTERNAL_STORAGE_TYPE_DEFAULT;
				}
				return location;
			}
			File[] external = ctx.getExternalFilesDirs(null);
			if (external != null && external.length > 0 && external[0] != null) {
				location = external[0];
				if (tp != null) {
					tp.value = EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE;
				}
			} else {
				File[] obbDirs = ctx.getObbDirs();
				if (obbDirs != null && obbDirs.length > 0 && obbDirs[0] != null) {
					location = obbDirs[0];
					if (tp != null) {
						tp.value = EXTERNAL_STORAGE_TYPE_OBB;
					}
				} else {
					location = getInternalAppPath();
					if (tp != null) {
						tp.value = EXTERNAL_STORAGE_TYPE_INTERNAL_FILE;
					}
				}
			}
		}
		return location;
	}

	public File getDefaultLocationV19() {
		String location = settingsAPI.getString(globalPreferences, EXTERNAL_STORAGE_DIR_V19,
				getExternalStorageDirectoryPre19().getAbsolutePath());
		return new File(location);
	}


	public static boolean isWritable(File dirToTest) {
		boolean isWriteable = false;
		try {
			dirToTest.mkdirs();
			File writeTestFile = File.createTempFile("osmand_", ".tmp", dirToTest);
			isWriteable = writeTestFile.exists();
			writeTestFile.delete();
		} catch (IOException e) {
			isWriteable = false;
		}
		return isWriteable;
	}

	public boolean isExternalStorageDirectorySpecifiedV19() {
		return settingsAPI.contains(globalPreferences, EXTERNAL_STORAGE_DIR_TYPE_V19);
	}

	public int getExternalStorageDirectoryTypeV19() {
		return settingsAPI.getInt(globalPreferences, EXTERNAL_STORAGE_DIR_TYPE_V19, -1);
	}

	public File getExternalStorageDirectoryPre19() {
		String defaultLocation = Environment.getExternalStorageDirectory().getAbsolutePath();
		File rootFolder = new File(settingsAPI.getString(globalPreferences, EXTERNAL_STORAGE_DIR,
				defaultLocation));
		return new File(rootFolder, IndexConstants.APP_DIR);
	}

	public File getDefaultInternalStorage() {
		return new File(Environment.getExternalStorageDirectory(), IndexConstants.APP_DIR);
	}

	public boolean setExternalStorageDirectoryV19(int type, String externalStorageDir) {
		return settingsAPI.edit(globalPreferences).
				putInt(EXTERNAL_STORAGE_DIR_TYPE_V19, type).
				putString(EXTERNAL_STORAGE_DIR_V19, externalStorageDir).commit();
	}

	@SuppressLint("NewApi")
	@Nullable
	public File getSecondaryStorage() {
		if (Build.VERSION.SDK_INT < 19) {
			return getExternalStorageDirectoryPre19();
		} else {
			File[] externals = ctx.getExternalFilesDirs(null);
			for (File file : externals) {
				if (file != null && !file.getAbsolutePath().contains("emulated")) {
					return file;
				}
			}
		}
		return null;
	}

	public void setExternalStorageDirectory(int type, String directory) {
		if (Build.VERSION.SDK_INT < 19) {
			setExternalStorageDirectoryPre19(directory);
		} else {
			setExternalStorageDirectoryV19(type, directory);
		}

	}

	public boolean isExternalStorageDirectorySpecifiedPre19() {
		return settingsAPI.contains(globalPreferences, EXTERNAL_STORAGE_DIR);
	}

	public boolean setExternalStorageDirectoryPre19(String externalStorageDir) {
		return settingsAPI.edit(globalPreferences).putString(EXTERNAL_STORAGE_DIR, externalStorageDir).commit();
	}

	public Object getGlobalPreferences() {
		return globalPreferences;
	}


	// This value is a key for saving last known location shown on the map
	public static final String LAST_KNOWN_MAP_LAT = "last_known_map_lat"; //$NON-NLS-1$
	public static final String LAST_KNOWN_MAP_LON = "last_known_map_lon"; //$NON-NLS-1$
	public static final String LAST_KNOWN_MAP_ZOOM = "last_known_map_zoom"; //$NON-NLS-1$

	public static final String MAP_LABEL_TO_SHOW = "map_label_to_show"; //$NON-NLS-1$
	public static final String MAP_LAT_TO_SHOW = "map_lat_to_show"; //$NON-NLS-1$
	public static final String MAP_LON_TO_SHOW = "map_lon_to_show"; //$NON-NLS-1$
	public static final String MAP_ZOOM_TO_SHOW = "map_zoom_to_show"; //$NON-NLS-1$

	public LatLon getLastKnownMapLocation() {
		float lat = settingsAPI.getFloat(globalPreferences, LAST_KNOWN_MAP_LAT, 0);
		float lon = settingsAPI.getFloat(globalPreferences, LAST_KNOWN_MAP_LON, 0);
		return new LatLon(lat, lon);
	}

	public boolean isLastKnownMapLocation() {
		return settingsAPI.contains(globalPreferences, LAST_KNOWN_MAP_LAT);
	}


	public LatLon getAndClearMapLocationToShow() {
		if (!settingsAPI.contains(globalPreferences, MAP_LAT_TO_SHOW)) {
			return null;
		}
		float lat = settingsAPI.getFloat(globalPreferences, MAP_LAT_TO_SHOW, 0);
		float lon = settingsAPI.getFloat(globalPreferences, MAP_LON_TO_SHOW, 0);
		settingsAPI.edit(globalPreferences).remove(MAP_LAT_TO_SHOW).commit();
		return new LatLon(lat, lon);
	}

	public PointDescription getAndClearMapLabelToShow(LatLon l) {
		String label = settingsAPI.getString(globalPreferences, MAP_LABEL_TO_SHOW, null);
		settingsAPI.edit(globalPreferences).remove(MAP_LABEL_TO_SHOW).commit();
		if (label != null) {
			return PointDescription.deserializeFromString(label, l);
		} else {
			return null;
		}
	}

	private Object objectToShow;
	private boolean editObjectToShow;
	private String searchRequestToShow;

	public void setSearchRequestToShow(String request) {
		this.searchRequestToShow = request;
	}

	public String getAndClearSearchRequestToShow() {
		String searchRequestToShow = this.searchRequestToShow;
		this.searchRequestToShow = null;
		return searchRequestToShow;
	}

	public Object getAndClearObjectToShow() {
		Object objectToShow = this.objectToShow;
		this.objectToShow = null;
		return objectToShow;
	}

	public boolean getAndClearEditObjectToShow() {
		boolean res = this.editObjectToShow;
		this.editObjectToShow = false;
		return res;
	}

	public int getMapZoomToShow() {
		return settingsAPI.getInt(globalPreferences, MAP_ZOOM_TO_SHOW, 5);
	}

	public void setMapLocationToShow(double latitude, double longitude, int zoom, PointDescription pointDescription,
									 boolean addToHistory, Object toShow) {
		SettingsEditor edit = settingsAPI.edit(globalPreferences);
		edit.putFloat(MAP_LAT_TO_SHOW, (float) latitude);
		edit.putFloat(MAP_LON_TO_SHOW, (float) longitude);
		if (pointDescription != null) {
			edit.putString(MAP_LABEL_TO_SHOW, PointDescription.serializeToString(pointDescription));
		} else {
			edit.remove(MAP_LABEL_TO_SHOW);
		}
		edit.putInt(MAP_ZOOM_TO_SHOW, zoom);
		edit.commit();
		objectToShow = toShow;
		if (addToHistory) {
			SearchHistoryHelper.getInstance(ctx).addNewItemToHistory(latitude, longitude, pointDescription);
		}
	}

	public void setEditObjectToShow() {
		this.editObjectToShow = true;
	}

	public void setMapLocationToShow(double latitude, double longitude, int zoom) {
		setMapLocationToShow(latitude, longitude, zoom, null, false, null);
	}

	public void setMapLocationToShow(double latitude, double longitude, int zoom, PointDescription historyDescription) {
		setMapLocationToShow(latitude, longitude, zoom, historyDescription, true, null);
	}

	// Do not use that method if you want to show point on map. Use setMapLocationToShow
	public void setLastKnownMapLocation(double latitude, double longitude) {
		SettingsEditor edit = settingsAPI.edit(globalPreferences);
		edit.putFloat(LAST_KNOWN_MAP_LAT, (float) latitude);
		edit.putFloat(LAST_KNOWN_MAP_LON, (float) longitude);
		edit.commit();
	}

	public int getLastKnownMapZoom() {
		return settingsAPI.getInt(globalPreferences, LAST_KNOWN_MAP_ZOOM, 5);
	}

	public void setLastKnownMapZoom(int zoom) {
		settingsAPI.edit(globalPreferences).putInt(LAST_KNOWN_MAP_ZOOM, zoom).commit();
	}

	public final static String POINT_NAVIGATE_LAT = "point_navigate_lat"; //$NON-NLS-1$
	public final static String POINT_NAVIGATE_LON = "point_navigate_lon"; //$NON-NLS-1$
	public final static String POINT_NAVIGATE_ROUTE = "point_navigate_route_integer"; //$NON-NLS-1$
	public final static int NAVIGATE = 1;
	public final static String POINT_NAVIGATE_DESCRIPTION = "point_navigate_description"; //$NON-NLS-1$
	public final static String START_POINT_LAT = "start_point_lat"; //$NON-NLS-1$
	public final static String START_POINT_LON = "start_point_lon"; //$NON-NLS-1$
	public final static String START_POINT_DESCRIPTION = "start_point_description"; //$NON-NLS-1$

	public final static String INTERMEDIATE_POINTS = "intermediate_points"; //$NON-NLS-1$
	public final static String INTERMEDIATE_POINTS_DESCRIPTION = "intermediate_points_description"; //$NON-NLS-1$
	private IntermediatePointsStorage intermediatePointsStorage = new IntermediatePointsStorage();

	public final static String POINT_NAVIGATE_LAT_BACKUP = "point_navigate_lat_backup"; //$NON-NLS-1$
	public final static String POINT_NAVIGATE_LON_BACKUP = "point_navigate_lon_backup"; //$NON-NLS-1$
	public final static String POINT_NAVIGATE_DESCRIPTION_BACKUP = "point_navigate_description_backup"; //$NON-NLS-1$
	public final static String START_POINT_LAT_BACKUP = "start_point_lat_backup"; //$NON-NLS-1$
	public final static String START_POINT_LON_BACKUP = "start_point_lon_backup"; //$NON-NLS-1$
	public final static String START_POINT_DESCRIPTION_BACKUP = "start_point_description_backup"; //$NON-NLS-1$
	public final static String INTERMEDIATE_POINTS_BACKUP = "intermediate_points_backup"; //$NON-NLS-1$
	public final static String INTERMEDIATE_POINTS_DESCRIPTION_BACKUP = "intermediate_points_description_backup"; //$NON-NLS-1$

	public final static String MAP_MARKERS_POINT = "map_markers_point"; //$NON-NLS-1$
	public final static String MAP_MARKERS_COLOR = "map_markers_color"; //$NON-NLS-1$
	public final static String MAP_MARKERS_DESCRIPTION = "map_markers_description"; //$NON-NLS-1$
	public final static String MAP_MARKERS_SELECTION = "map_markers_selection"; //$NON-NLS-1$
	public final static String MAP_MARKERS_HISTORY_POINT = "map_markers_history_point"; //$NON-NLS-1$
	public final static String MAP_MARKERS_HISTORY_DESCRIPTION = "map_markers_history_description"; //$NON-NLS-1$
	public final static int MAP_MARKERS_HISTORY_LIMIT = 30;
	private MapMarkersStorage mapMarkersStorage = new MapMarkersStorage();
	private MapMarkersHistoryStorage mapMarkersHistoryStorage = new MapMarkersHistoryStorage();

	private static final String IMPASSABLE_ROAD_POINTS = "impassable_road_points";
	private static final String IMPASSABLE_ROADS_DESCRIPTIONS = "impassable_roads_descriptions";
	private ImpassableRoadsStorage mImpassableRoadsStorage = new ImpassableRoadsStorage();

	public void backupPointToStart() {
		settingsAPI.edit(globalPreferences)
				.putFloat(START_POINT_LAT_BACKUP, settingsAPI.getFloat(globalPreferences, START_POINT_LAT, 0))
				.putFloat(START_POINT_LON_BACKUP, settingsAPI.getFloat(globalPreferences, START_POINT_LON, 0))
				.putString(START_POINT_DESCRIPTION_BACKUP, settingsAPI.getString(globalPreferences, START_POINT_DESCRIPTION, ""))
				.commit();
	}

	private void backupPointToNavigate() {
		settingsAPI.edit(globalPreferences)
				.putFloat(POINT_NAVIGATE_LAT_BACKUP, settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LAT, 0))
				.putFloat(POINT_NAVIGATE_LON_BACKUP, settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LON, 0))
				.putString(POINT_NAVIGATE_DESCRIPTION_BACKUP, settingsAPI.getString(globalPreferences, POINT_NAVIGATE_DESCRIPTION, ""))
				.commit();
	}

	private void backupIntermediatePoints() {
		settingsAPI.edit(globalPreferences)
				.putString(INTERMEDIATE_POINTS_BACKUP, settingsAPI.getString(globalPreferences, INTERMEDIATE_POINTS, ""))
				.putString(INTERMEDIATE_POINTS_DESCRIPTION_BACKUP, settingsAPI.getString(globalPreferences, INTERMEDIATE_POINTS_DESCRIPTION, ""))
				.commit();
	}

	public void backupTargetPoints() {
		backupPointToStart();
		backupPointToNavigate();
		backupIntermediatePoints();
	}

	public void restoreTargetPoints() {
		settingsAPI.edit(globalPreferences)
				.putFloat(START_POINT_LAT, settingsAPI.getFloat(globalPreferences, START_POINT_LAT_BACKUP, 0))
				.putFloat(START_POINT_LON, settingsAPI.getFloat(globalPreferences, START_POINT_LON_BACKUP, 0))
				.putString(START_POINT_DESCRIPTION, settingsAPI.getString(globalPreferences, START_POINT_DESCRIPTION_BACKUP, ""))
				.putFloat(POINT_NAVIGATE_LAT, settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LAT_BACKUP, 0))
				.putFloat(POINT_NAVIGATE_LON, settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LON_BACKUP, 0))
				.putString(POINT_NAVIGATE_DESCRIPTION, settingsAPI.getString(globalPreferences, POINT_NAVIGATE_DESCRIPTION_BACKUP, ""))
				.putString(INTERMEDIATE_POINTS, settingsAPI.getString(globalPreferences, INTERMEDIATE_POINTS_BACKUP, ""))
				.putString(INTERMEDIATE_POINTS_DESCRIPTION, settingsAPI.getString(globalPreferences, INTERMEDIATE_POINTS_DESCRIPTION_BACKUP, ""))
				.commit();
	}

	public boolean restorePointToStart() {
		if (settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LAT_BACKUP, 0) == 0) {
			settingsAPI.edit(globalPreferences)
					.putFloat(START_POINT_LAT, settingsAPI.getFloat(globalPreferences, START_POINT_LAT_BACKUP, 0))
					.putFloat(START_POINT_LON, settingsAPI.getFloat(globalPreferences, START_POINT_LON_BACKUP, 0))
					.commit();
			return true;
		} else {
			return false;
		}
	}

	public LatLon getPointToNavigate() {
		float lat = settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LAT, 0);
		float lon = settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LON, 0);
		if (lat == 0 && lon == 0) {
			return null;
		}
		return new LatLon(lat, lon);
	}


	public LatLon getPointToStart() {
		float lat = settingsAPI.getFloat(globalPreferences, START_POINT_LAT, 0);
		float lon = settingsAPI.getFloat(globalPreferences, START_POINT_LON, 0);
		if (lat == 0 && lon == 0) {
			return null;
		}
		return new LatLon(lat, lon);
	}

	public PointDescription getStartPointDescription() {
		return
				PointDescription.deserializeFromString(settingsAPI.getString(globalPreferences, START_POINT_DESCRIPTION, ""), getPointToStart());
	}

	public PointDescription getPointNavigateDescription() {
		return
				PointDescription.deserializeFromString(settingsAPI.getString(globalPreferences, POINT_NAVIGATE_DESCRIPTION, ""), getPointToNavigate());
	}


	public int isRouteToPointNavigateAndClear() {
		int vl = settingsAPI.getInt(globalPreferences, POINT_NAVIGATE_ROUTE, 0);
		if (vl != 0) {
			settingsAPI.edit(globalPreferences).remove(POINT_NAVIGATE_ROUTE).commit();
		}
		return vl;
	}


	public boolean clearIntermediatePoints() {
		return settingsAPI.edit(globalPreferences).remove(INTERMEDIATE_POINTS).remove(INTERMEDIATE_POINTS_DESCRIPTION).commit();
	}

	public boolean clearActiveMapMarkers() {
		return settingsAPI.edit(globalPreferences)
				.remove(MAP_MARKERS_POINT)
				.remove(MAP_MARKERS_DESCRIPTION)
				.remove(MAP_MARKERS_COLOR)
				.remove(MAP_MARKERS_SELECTION)
				.commit();
	}

	public boolean clearMapMarkersHistory() {
		return settingsAPI.edit(globalPreferences)
				.remove(MAP_MARKERS_HISTORY_POINT)
				.remove(MAP_MARKERS_HISTORY_DESCRIPTION)
				.commit();
	}

	public final CommonPreference<Boolean> USE_INTERMEDIATE_POINTS_NAVIGATION =
			new BooleanPreference("use_intermediate_points_navigation", false).makeGlobal().cache();


	private class IntermediatePointsStorage extends MapPointsStorage {

		public IntermediatePointsStorage() {
			pointsKey = INTERMEDIATE_POINTS;
			descriptionsKey = INTERMEDIATE_POINTS_DESCRIPTION;
		}

		@Override
		public boolean savePoints(List<LatLon> ps, List<String> ds) {
			boolean res = super.savePoints(ps, ds);
			backupTargetPoints();
			return res;
		}
	}

	private class MapMarkersHistoryStorage extends MapPointsStorage {

		public MapMarkersHistoryStorage() {
			pointsKey = MAP_MARKERS_HISTORY_POINT;
			descriptionsKey = MAP_MARKERS_HISTORY_DESCRIPTION;
		}

		@Override
		public boolean savePoints(List<LatLon> ps, List<String> ds) {
			while (ps.size() > MAP_MARKERS_HISTORY_LIMIT) {
				ps.remove(ps.size() - 1);
				ds.remove(ds.size() - 1);
			}
			return super.savePoints(ps, ds);
		}
	}

	private class MapMarkersStorage extends MapPointsStorage {

		protected String colorsKey;
		protected String selectionKey;

		public MapMarkersStorage() {
			pointsKey = MAP_MARKERS_POINT;
			descriptionsKey = MAP_MARKERS_DESCRIPTION;
			colorsKey = MAP_MARKERS_COLOR;
			selectionKey = MAP_MARKERS_SELECTION;
		}

		public List<Integer> getColors(int sz) {
			List<Integer> list = new ArrayList<>();
			String ip = settingsAPI.getString(globalPreferences, colorsKey, "");
			if (ip.trim().length() > 0) {
				StringTokenizer tok = new StringTokenizer(ip, ",");
				while (tok.hasMoreTokens()) {
					String colorStr = tok.nextToken();
					list.add(Integer.parseInt(colorStr));
				}
			}
			while (list.size() > sz) {
				list.remove(list.size() - 1);
			}
			int i = 0;
			while (list.size() < sz) {
				list.add(i % MapMarkersHelper.MAP_MARKERS_COLORS_COUNT);
				i++;
			}
			return list;
		}

		public List<Boolean> getSelections(int sz) {
			List<Boolean> list = new ArrayList<>();
			String ip = settingsAPI.getString(globalPreferences, selectionKey, "");
			if (ip.trim().length() > 0) {
				StringTokenizer tok = new StringTokenizer(ip, ",");
				while (tok.hasMoreTokens()) {
					String indexStr = tok.nextToken();
					list.add(Boolean.parseBoolean(indexStr));
				}
			}
			while (list.size() > sz) {
				list.remove(list.size() - 1);
			}
			while (list.size() < sz) {
				list.add(false);
			}
			return list;
		}

		public boolean insertPoint(double latitude, double longitude,
								   PointDescription historyDescription, int colorIndex, int pos,
								   boolean selected, int index) {
			List<LatLon> ps = getPoints();
			List<String> ds = getPointDescriptions(ps.size());
			List<Integer> cs = getColors(ps.size());
			List<Boolean> bs = getSelections(ps.size());
			ps.add(index, new LatLon(latitude, longitude));
			ds.add(index, PointDescription.serializeToString(historyDescription));
			cs.add(index, colorIndex);
			bs.add(index, selected);
			if (historyDescription != null && !historyDescription.isSearchingAddress(ctx)) {
				SearchHistoryHelper.getInstance(ctx).addNewItemToHistory(latitude, longitude, historyDescription);
			}
			return savePoints(ps, ds, cs, bs);
		}

		public boolean insertPoints(double[] latitudes, double[] longitudes,
									List<PointDescription> historyDescriptions, int[] colorIndexes,
									int[] positions, boolean[] selections, int[] indexes) {
			List<LatLon> ps = getPoints();
			List<String> ds = getPointDescriptions(ps.size());
			List<Integer> cs = getColors(ps.size());
			List<Boolean> bs = getSelections(ps.size());
			for (int i = 0; i < latitudes.length; i++) {
				double latitude = latitudes[i];
				double longitude = longitudes[i];
				PointDescription historyDescription = historyDescriptions.get(i);
				int colorIndex = colorIndexes[i];
				boolean selected = selections[i];
				int index = indexes[i];
				ps.add(index, new LatLon(latitude, longitude));
				ds.add(index, PointDescription.serializeToString(historyDescription));
				cs.add(index, colorIndex);
				bs.add(index, selected);
				if (historyDescription != null && !historyDescription.isSearchingAddress(ctx)) {
					SearchHistoryHelper.getInstance(ctx).addNewItemToHistory(latitude, longitude, historyDescription);
				}
			}
			return savePoints(ps, ds, cs, bs);
		}

		public boolean updatePoint(double latitude, double longitude,
								   PointDescription historyDescription, int colorIndex,
								   boolean selected) {
			List<LatLon> ps = getPoints();
			List<String> ds = getPointDescriptions(ps.size());
			List<Integer> cs = getColors(ps.size());
			List<Boolean> bs = getSelections(ps.size());
			int index = ps.indexOf(new LatLon(latitude, longitude));
			if (index != -1) {
				ds.set(index, PointDescription.serializeToString(historyDescription));
				if (cs.size() > index) {
					cs.set(index, colorIndex);
				}
				if (bs.size() > index) {
					bs.set(index, selected);
				}
				if (historyDescription != null && !historyDescription.isSearchingAddress(ctx)) {
					SearchHistoryHelper.getInstance(ctx).addNewItemToHistory(latitude, longitude, historyDescription);
				}
				return savePoints(ps, ds, cs, bs);
			} else {
				return false;
			}
		}

		public boolean movePoint(LatLon latLonEx,
								 LatLon latLonNew,
								 PointDescription historyDescription,
								 int colorIndex,
								 boolean selected) {
			List<LatLon> ps = getPoints();
			List<String> ds = getPointDescriptions(ps.size());
			List<Integer> cs = getColors(ps.size());
			List<Boolean> bs = getSelections(ps.size());
			int index = ps.indexOf(latLonEx);
			if (index != -1) {
				if (ps.size() > index) {
					ps.set(index, latLonNew);
				}
				ds.set(index, PointDescription.serializeToString(historyDescription));
				if (cs.size() > index) {
					cs.set(index, colorIndex);
				}
				if (bs.size() > index) {
					bs.set(index, selected);
				}
				if (historyDescription != null && !historyDescription.isSearchingAddress(ctx)) {
					double lat = latLonNew.getLatitude();
					double lon = latLonNew.getLongitude();
					SearchHistoryHelper.getInstance(ctx).addNewItemToHistory(lat, lon, historyDescription);
				}
				return savePoints(ps, ds, cs, bs);
			} else {
				return false;
			}
		}

		@Override
		public boolean deletePoint(int index) {
			List<LatLon> ps = getPoints();
			List<String> ds = getPointDescriptions(ps.size());
			List<Integer> cs = getColors(ps.size());
			List<Boolean> bs = getSelections(ps.size());
			ps.remove(index);
			ds.remove(index);
			if (cs.size() > index) {
				cs.remove(index);
			}
			if (bs.size() > index) {
				bs.remove(index);
			}
			return savePoints(ps, ds, cs, bs);
		}

		public boolean savePoints(List<LatLon> ps, List<String> ds, List<Integer> cs,
								  List<Boolean> bs) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < ps.size(); i++) {
				if (i > 0) {
					sb.append(",");
				}
				sb.append(((float) ps.get(i).getLatitude() + "")).append(",").append(((float) ps.get(i).getLongitude() + ""));
			}
			StringBuilder tb = new StringBuilder();
			for (int i = 0; i < ds.size(); i++) {
				if (i > 0) {
					tb.append("--");
				}
				if (ds.get(i) == null) {
					tb.append("");
				} else {
					tb.append(ds.get(i));
				}
			}
			StringBuilder cb = new StringBuilder();
			for (int i = 0; i < cs.size(); i++) {
				if (i > 0) {
					cb.append(",");
				}
				cb.append(Integer.toString(cs.get(i)));
			}
			StringBuilder bb = new StringBuilder();
			if (bs != null) {
				for (int i = 0; i < bs.size(); i++) {
					if (i > 0) {
						bb.append(",");
					}
					bb.append(Boolean.toString(bs.get(i)));
				}
			}
			return settingsAPI.edit(globalPreferences)
					.putString(pointsKey, sb.toString())
					.putString(descriptionsKey, tb.toString())
					.putString(colorsKey, cb.toString())
					.putString(selectionKey, bb.toString())
					.commit();
		}

		@Override
		public boolean insertPoint(double latitude, double longitude, PointDescription historyDescription, int index) {
			return false;
		}

		@Override
		public boolean updatePoint(double latitude, double longitude, PointDescription historyDescription) {
			return false;
		}

		@Override
		public boolean savePoints(List<LatLon> ps, List<String> ds) {
			return false;
		}
	}

	private class ImpassableRoadsStorage extends MapPointsStorage {
		public ImpassableRoadsStorage() {
			pointsKey = IMPASSABLE_ROAD_POINTS;
			descriptionsKey = IMPASSABLE_ROADS_DESCRIPTIONS;
		}
	}

	private abstract class MapPointsStorage {

		protected String pointsKey;
		protected String descriptionsKey;

		public MapPointsStorage() {
		}

		public List<String> getPointDescriptions(int sz) {
			List<String> list = new ArrayList<>();
			String ip = settingsAPI.getString(globalPreferences, descriptionsKey, "");
			if (ip.trim().length() > 0) {
				list.addAll(Arrays.asList(ip.split("--")));
			}
			while (list.size() > sz) {
				list.remove(list.size() - 1);
			}
			while (list.size() < sz) {
				list.add("");
			}
			return list;
		}

		public List<LatLon> getPoints() {
			List<LatLon> list = new ArrayList<>();
			String ip = settingsAPI.getString(globalPreferences, pointsKey, "");
			if (ip.trim().length() > 0) {
				StringTokenizer tok = new StringTokenizer(ip, ",");
				while (tok.hasMoreTokens()) {
					String lat = tok.nextToken();
					if (!tok.hasMoreTokens()) {
						break;
					}
					String lon = tok.nextToken();
					list.add(new LatLon(Float.parseFloat(lat), Float.parseFloat(lon)));
				}
			}
			return list;
		}

		public boolean insertPoint(double latitude, double longitude, PointDescription historyDescription, int index) {
			List<LatLon> ps = getPoints();
			List<String> ds = getPointDescriptions(ps.size());
			ps.add(index, new LatLon(latitude, longitude));
			ds.add(index, PointDescription.serializeToString(historyDescription));
			if (historyDescription != null && !historyDescription.isSearchingAddress(ctx)) {
				SearchHistoryHelper.getInstance(ctx).addNewItemToHistory(latitude, longitude, historyDescription);
			}
			return savePoints(ps, ds);
		}

		public boolean updatePoint(double latitude, double longitude, PointDescription historyDescription) {
			List<LatLon> ps = getPoints();
			List<String> ds = getPointDescriptions(ps.size());
			int i = ps.indexOf(new LatLon(latitude, longitude));
			if (i != -1) {
				ds.set(i, PointDescription.serializeToString(historyDescription));
				if (historyDescription != null && !historyDescription.isSearchingAddress(ctx)) {
					SearchHistoryHelper.getInstance(ctx).addNewItemToHistory(latitude, longitude, historyDescription);
				}
				return savePoints(ps, ds);
			} else {
				return false;
			}
		}

		public boolean deletePoint(int index) {
			List<LatLon> ps = getPoints();
			List<String> ds = getPointDescriptions(ps.size());
			if (index < ps.size()) {
				ps.remove(index);
				ds.remove(index);
				return savePoints(ps, ds);
			} else {
				return false;
			}
		}

		public boolean deletePoint(LatLon latLon) {
			List<LatLon> ps = getPoints();
			List<String> ds = getPointDescriptions(ps.size());
			int index = ps.indexOf(latLon);
			if (index != -1) {
				ps.remove(index);
				ds.remove(index);
				return savePoints(ps, ds);
			} else {
				return false;
			}
		}

		public boolean savePoints(List<LatLon> ps, List<String> ds) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < ps.size(); i++) {
				if (i > 0) {
					sb.append(",");
				}
				sb.append(((float) ps.get(i).getLatitude() + "")).append(",").append(((float) ps.get(i).getLongitude() + ""));
			}
			StringBuilder tb = new StringBuilder();
			for (int i = 0; i < ds.size(); i++) {
				if (i > 0) {
					tb.append("--");
				}
				if (ds.get(i) == null) {
					tb.append("");
				} else {
					tb.append(ds.get(i));
				}
			}
			return settingsAPI.edit(globalPreferences)
					.putString(pointsKey, sb.toString())
					.putString(descriptionsKey, tb.toString())
					.commit();
		}

		public boolean movePoint(LatLon latLonEx, LatLon latLonNew) {
			List<LatLon> ps = getPoints();
			List<String> ds = getPointDescriptions(ps.size());
			int i = ps.indexOf(latLonEx);
			if (i != -1) {
				ps.set(i, latLonNew);
				return savePoints(ps, ds);
			} else {
				return false;
			}
		}
	}


	public List<String> getIntermediatePointDescriptions(int sz) {
		return intermediatePointsStorage.getPointDescriptions(sz);
	}

	public List<LatLon> getIntermediatePoints() {
		return intermediatePointsStorage.getPoints();
	}

	public boolean insertIntermediatePoint(double latitude, double longitude, PointDescription historyDescription, int index) {
		return intermediatePointsStorage.insertPoint(latitude, longitude, historyDescription, index);
	}

	public boolean updateIntermediatePoint(double latitude, double longitude, PointDescription historyDescription) {
		return intermediatePointsStorage.updatePoint(latitude, longitude, historyDescription);
	}

	public boolean deleteIntermediatePoint(int index) {
		return intermediatePointsStorage.deletePoint(index);
	}

	public boolean saveIntermediatePoints(List<LatLon> ps, List<String> ds) {
		return intermediatePointsStorage.savePoints(ps, ds);
	}


	public List<String> getMapMarkersPointDescriptions(int sz) {
		return mapMarkersStorage.getPointDescriptions(sz);
	}

	public List<Integer> getMapMarkersColors(int sz) {
		return mapMarkersStorage.getColors(sz);
	}

	public List<Boolean> getMapMarkersSelections(int sz) {
		return mapMarkersStorage.getSelections(sz);
	}

	public List<LatLon> getMapMarkersPoints() {
		return mapMarkersStorage.getPoints();
	}

	public boolean insertMapMarker(double latitude, double longitude,
								   PointDescription historyDescription, int colorIndex, int pos,
								   boolean selected, int index) {
		return mapMarkersStorage.insertPoint(latitude, longitude, historyDescription, colorIndex,
				index, selected, pos);
	}

	public boolean insertMapMarkers(double[] latitudes, double[] longitudes,
									List<PointDescription> historyDescriptions, int[] colorIndexes,
									int[] positions, boolean[] selections, int[] indexes) {
		return mapMarkersStorage.insertPoints(latitudes, longitudes, historyDescriptions, colorIndexes,
				positions, selections, indexes);
	}

	public boolean updateMapMarker(double latitude, double longitude,
								   PointDescription historyDescription, int colorIndex, boolean selected) {
		return mapMarkersStorage.updatePoint(latitude, longitude, historyDescription, colorIndex, selected);
	}

	public boolean moveMapMarker(LatLon latLonEx,
								 LatLon latLonNew,
								 PointDescription historyDescription,
								 int colorIndex,
								 boolean selected) {
		return mapMarkersStorage.movePoint(latLonEx, latLonNew, historyDescription, colorIndex, selected);
	}

	public boolean deleteMapMarker(int index) {
		return mapMarkersStorage.deletePoint(index);
	}

	public boolean saveMapMarkers(List<LatLon> ps, List<String> ds, List<Integer> cs, List<Boolean> bs) {
		return mapMarkersStorage.savePoints(ps, ds, cs, bs);
	}


	public List<String> getMapMarkersHistoryPointDescriptions(int sz) {
		return mapMarkersHistoryStorage.getPointDescriptions(sz);
	}

	public List<LatLon> getMapMarkersHistoryPoints() {
		return mapMarkersHistoryStorage.getPoints();
	}

	public boolean insertMapMarkerHistory(double latitude, double longitude,
										  PointDescription historyDescription, int colorIndex, int index) {
		return mapMarkersHistoryStorage.insertPoint(latitude, longitude, historyDescription, index);
	}

	public boolean updateMapMarkerHistory(double latitude, double longitude,
										  PointDescription historyDescription, int colorIndex) {
		return mapMarkersHistoryStorage.updatePoint(latitude, longitude, historyDescription);
	}

	public boolean deleteMapMarkerHistory(int index) {
		return mapMarkersHistoryStorage.deletePoint(index);
	}

	public boolean saveMapMarkersHistory(List<LatLon> ps, List<String> ds, List<Integer> cs) {
		return mapMarkersHistoryStorage.savePoints(ps, ds);
	}


	public boolean clearPointToNavigate() {
		return settingsAPI.edit(globalPreferences).remove(POINT_NAVIGATE_LAT).remove(POINT_NAVIGATE_LON).
				remove(POINT_NAVIGATE_DESCRIPTION).commit();
	}

	public boolean clearPointToStart() {
		return settingsAPI.edit(globalPreferences).remove(START_POINT_LAT).remove(START_POINT_LON).
				remove(START_POINT_DESCRIPTION).commit();
	}

	public boolean setPointToNavigate(double latitude, double longitude, PointDescription p) {
		boolean add = settingsAPI.edit(globalPreferences).putFloat(POINT_NAVIGATE_LAT, (float) latitude).putFloat(POINT_NAVIGATE_LON, (float) longitude).commit();
		settingsAPI.edit(globalPreferences).putString(POINT_NAVIGATE_DESCRIPTION, PointDescription.serializeToString(p)).commit();
		if (add) {
			if (p != null && !p.isSearchingAddress(ctx)) {
				SearchHistoryHelper.getInstance(ctx).addNewItemToHistory(latitude, longitude, p);
			}
		}
		backupTargetPoints();
		return add;
	}

	public boolean setPointToStart(double latitude, double longitude, PointDescription p) {
		boolean add = settingsAPI.edit(globalPreferences).putFloat(START_POINT_LAT, (float) latitude).putFloat(START_POINT_LON, (float) longitude).commit();
		settingsAPI.edit(globalPreferences).putString(START_POINT_DESCRIPTION, PointDescription.serializeToString(p)).commit();
		backupTargetPoints();
		return add;
	}

	public boolean navigateDialog() {
		return settingsAPI.edit(globalPreferences).putInt(POINT_NAVIGATE_ROUTE, NAVIGATE).commit();
	}

	public List<LatLon> getImpassableRoadPoints() {
		return mImpassableRoadsStorage.getPoints();
	}
	public boolean addImpassableRoad(double latitude, double longitude) {
		return mImpassableRoadsStorage.insertPoint(latitude, longitude, null, 0);
	}

	public boolean removeImpassableRoad(int index) {
		return mImpassableRoadsStorage.deletePoint(index);
	}

	public boolean removeImpassableRoad(LatLon latLon) {
		return mImpassableRoadsStorage.deletePoint(latLon);
	}

	public boolean moveImpassableRoad(LatLon latLonEx, LatLon latLonNew) {
		return mImpassableRoadsStorage.movePoint(latLonEx, latLonNew);
	}

	/**
	 * quick actions prefs
	 */

	public static final String QUICK_FAB_MARGIN_X_PORTRAIT_MARGIN = "quick_fab_margin_x_portrait_margin";
	public static final String QUICK_FAB_MARGIN_Y_PORTRAIT_MARGIN = "quick_fab_margin_y_portrait_margin";
	public static final String QUICK_FAB_MARGIN_X_LANDSCAPE_MARGIN = "quick_fab_margin_x_landscape_margin";
	public static final String QUICK_FAB_MARGIN_Y_LANDSCAPE_MARGIN = "quick_fab_margin_y_landscape_margin";

	public final CommonPreference<String> QUICK_ACTION = new StringPreference("quick_action_new", "").makeGlobal();

	public final CommonPreference<String> QUICK_ACTION_LIST = new StringPreference("quick_action_list", "").makeGlobal();

	public final CommonPreference<Boolean> IS_QUICK_ACTION_TUTORIAL_SHOWN = new BooleanPreference("quick_action_tutorial", false).makeGlobal();

	public boolean setPortraitFabMargin(int x, int y) {
		return settingsAPI.edit(globalPreferences).putInt(QUICK_FAB_MARGIN_X_PORTRAIT_MARGIN, x)
				.putInt(QUICK_FAB_MARGIN_Y_PORTRAIT_MARGIN, y).commit();
	}

	public boolean setLandscapeFabMargin(int x, int y) {
		return settingsAPI.edit(globalPreferences).putInt(QUICK_FAB_MARGIN_X_LANDSCAPE_MARGIN, x)
				.putInt(QUICK_FAB_MARGIN_Y_LANDSCAPE_MARGIN, y).commit();
	}

	public Pair<Integer, Integer> getPortraitFabMargin() {
		if (settingsAPI.contains(globalPreferences, QUICK_FAB_MARGIN_X_PORTRAIT_MARGIN) && settingsAPI.contains(globalPreferences, QUICK_FAB_MARGIN_Y_PORTRAIT_MARGIN)) {
			return new Pair<>(settingsAPI.getInt(globalPreferences, QUICK_FAB_MARGIN_X_PORTRAIT_MARGIN, 0),
					settingsAPI.getInt(globalPreferences, QUICK_FAB_MARGIN_Y_PORTRAIT_MARGIN, 0));
		}
		return null;
	}

	public Pair<Integer, Integer> getLandscapeFabMargin() {
		if (settingsAPI.contains(globalPreferences, QUICK_FAB_MARGIN_X_LANDSCAPE_MARGIN) && settingsAPI.contains(globalPreferences, QUICK_FAB_MARGIN_Y_LANDSCAPE_MARGIN)) {
			return new Pair<>(settingsAPI.getInt(globalPreferences, QUICK_FAB_MARGIN_X_LANDSCAPE_MARGIN, 0),
					settingsAPI.getInt(globalPreferences, QUICK_FAB_MARGIN_Y_LANDSCAPE_MARGIN, 0));
		}
		return null;
	}

	/**
	 * the location of a parked car
	 */

	public static final String LAST_SEARCHED_REGION = "last_searched_region"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_CITY = "last_searched_city"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_CITY_NAME = "last_searched_city_name"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_POSTCODE = "last_searched_postcode"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_STREET = "last_searched_street"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_BUILDING = "last_searched_building"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_INTERSECTED_STREET = "last_searched_intersected_street"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_LAT = "last_searched_lat"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_LON = "last_searched_lon"; //$NON-NLS-1$

	public LatLon getLastSearchedPoint() {
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_LAT) && settingsAPI.contains(globalPreferences, LAST_SEARCHED_LON)) {
			return new LatLon(settingsAPI.getFloat(globalPreferences, LAST_SEARCHED_LAT, 0),
					settingsAPI.getFloat(globalPreferences, LAST_SEARCHED_LON, 0));
		}
		return null;
	}

	public boolean setLastSearchedPoint(LatLon l) {
		if (l == null) {
			return settingsAPI.edit(globalPreferences).remove(LAST_SEARCHED_LAT).remove(LAST_SEARCHED_LON).commit();
		} else {
			return setLastSearchedPoint(l.getLatitude(), l.getLongitude());
		}
	}

	public boolean setLastSearchedPoint(double lat, double lon) {
		return settingsAPI.edit(globalPreferences).putFloat(LAST_SEARCHED_LAT, (float) lat).
				putFloat(LAST_SEARCHED_LON, (float) lon).commit();
	}

	public String getLastSearchedRegion() {
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_REGION, ""); //$NON-NLS-1$
	}

	public boolean setLastSearchedRegion(String region, LatLon l) {
		SettingsEditor edit = settingsAPI.edit(globalPreferences).putString(LAST_SEARCHED_REGION, region).putLong(LAST_SEARCHED_CITY, -1).
				putString(LAST_SEARCHED_CITY_NAME, "").putString(LAST_SEARCHED_POSTCODE, "").
				putString(LAST_SEARCHED_STREET, "").putString(LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		boolean res = edit.commit();
		setLastSearchedPoint(l);
		return res;
	}

	public String getLastSearchedPostcode() {
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_POSTCODE, null);
	}

	public boolean setLastSearchedPostcode(String postcode, LatLon point) {
		SettingsEditor edit = settingsAPI.edit(globalPreferences).putLong(LAST_SEARCHED_CITY, -1).putString(LAST_SEARCHED_STREET, "") //$NON-NLS-1$
				.putString(LAST_SEARCHED_BUILDING, "").putString(LAST_SEARCHED_POSTCODE, postcode); //$NON-NLS-1$
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		boolean res = edit.commit();
		setLastSearchedPoint(point);
		return res;
	}

	public Long getLastSearchedCity() {
		return settingsAPI.getLong(globalPreferences, LAST_SEARCHED_CITY, -1);
	}

	public String getLastSearchedCityName() {
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_CITY_NAME, "");
	}

	public boolean setLastSearchedCity(Long cityId, String name, LatLon point) {
		SettingsEditor edit = settingsAPI.edit(globalPreferences).putLong(LAST_SEARCHED_CITY, cityId).putString(LAST_SEARCHED_CITY_NAME, name).
				putString(LAST_SEARCHED_STREET, "").putString(LAST_SEARCHED_BUILDING, "").putString(LAST_SEARCHED_POSTCODE, ""); //$NON-NLS-1$
		//edit.remove(LAST_SEARCHED_POSTCODE);
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		boolean res = edit.commit();
		setLastSearchedPoint(point);
		return res;
	}

	public String getLastSearchedStreet() {
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_STREET, ""); //$NON-NLS-1$
	}

	public boolean setLastSearchedStreet(String street, LatLon point) {
		SettingsEditor edit = settingsAPI.edit(globalPreferences).putString(LAST_SEARCHED_STREET, street).putString(LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		boolean res = edit.commit();
		setLastSearchedPoint(point);
		return res;
	}

	public String getLastSearchedBuilding() {
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$
	}

	public boolean setLastSearchedBuilding(String building, LatLon point) {
		boolean res = settingsAPI.edit(globalPreferences).putString(LAST_SEARCHED_BUILDING, building).remove(LAST_SEARCHED_INTERSECTED_STREET).commit();
		setLastSearchedPoint(point);
		return res;
	}

	public String getLastSearchedIntersectedStreet() {
		if (!settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			return null;
		}
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
	}

	public boolean setLastSearchedIntersectedStreet(String street, LatLon l) {
		setLastSearchedPoint(l);
		return settingsAPI.edit(globalPreferences).putString(LAST_SEARCHED_INTERSECTED_STREET, street).commit();
	}

	public final OsmandPreference<String> LAST_SELECTED_GPX_TRACK_FOR_NEW_POINT = new StringPreference("last_selected_gpx_track_for_new_point", null).makeGlobal().cache();

	// Avoid using this property, probably you need to use PoiFiltersHelper.getSelectedPoiFilters()
	public final OsmandPreference<String> SELECTED_POI_FILTER_FOR_MAP = new StringPreference("selected_poi_filter_for_map", null).makeGlobal().cache();

	public Set<String> getSelectedPoiFilters() {
		Set<String> result = new LinkedHashSet<>();
		String filtersId = SELECTED_POI_FILTER_FOR_MAP.get();
		if (filtersId != null && !filtersId.trim().isEmpty()) {
			Collections.addAll(result, filtersId.split(","));
		}
		return result;
	}

	public void setSelectedPoiFilters(final Set<String> poiFilters) {
		SELECTED_POI_FILTER_FOR_MAP.set(android.text.TextUtils.join(",", poiFilters));
	}

	public static final String VOICE_PROVIDER_NOT_USE = "VOICE_PROVIDER_NOT_USE";

	public static final String[] TTS_AVAILABLE_VOICES = new String[]{
			"de", "en", "es", "fr", "it", "ja", "nl", "pl", "pt", "ru", "zh"
	};
	// this value string is synchronized with settings_pref.xml preference name
	// this value could localized
	public final OsmandPreference<String> VOICE_PROVIDER = new StringPreference("voice_provider", null) {
		protected String getDefaultValue() {
			Configuration config = ctx.getResources().getConfiguration();
			for (String lang : TTS_AVAILABLE_VOICES) {
				if (lang.equals(config.locale.getLanguage())) {
					return lang + "-tts";
				}
			}
			return "en-tts";
		}

		;
	}.makeGlobal();


	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<String> RENDERER = new StringPreference("renderer", RendererRegistry.DEFAULT_RENDER) {
		{
			makeProfile();
		}

		@Override
		protected boolean setValue(Object prefs, String val) {
			if (val == null) {
				val = RendererRegistry.DEFAULT_RENDER;
			}
			RenderingRulesStorage loaded = ctx.getRendererRegistry().getRenderer(val);
			if (loaded != null) {
				super.setValue(prefs, val);
				return true;
			}
			return false;
		}

		;
	};


	Map<String, CommonPreference<String>> customRendersProps = new LinkedHashMap<String, OsmandSettings.CommonPreference<String>>();

	public CommonPreference<String> getCustomRenderProperty(String attrName) {
		if (!customRendersProps.containsKey(attrName)) {
			customRendersProps.put(attrName, new StringPreference("nrenderer_" + attrName, "").makeProfile());
		}
		return customRendersProps.get(attrName);
	}

	{
		CommonPreference<String> pref = getCustomRenderProperty("appMode");
		pref.setModeDefaultValue(ApplicationMode.CAR, "car");
		pref.setModeDefaultValue(ApplicationMode.PEDESTRIAN, "pedestrian");
		pref.setModeDefaultValue(ApplicationMode.BICYCLE, "bicycle");
	}

	Map<String, CommonPreference<Boolean>> customBooleanRendersProps = new LinkedHashMap<String, OsmandSettings.CommonPreference<Boolean>>();

	public CommonPreference<Boolean> getCustomRenderBooleanProperty(String attrName) {
		if (!customBooleanRendersProps.containsKey(attrName)) {
			customBooleanRendersProps.put(attrName, new BooleanPreference("nrenderer_" + attrName, false).makeProfile());
		}
		return customBooleanRendersProps.get(attrName);
	}

	Map<String, CommonPreference<String>> customRoutingProps = new LinkedHashMap<String, OsmandSettings.CommonPreference<String>>();

	public CommonPreference<String> getCustomRoutingProperty(String attrName, String defValue) {
		if (!customRoutingProps.containsKey(attrName)) {
			customRoutingProps.put(attrName, new StringPreference("prouting_" + attrName, defValue).makeProfile());
		}
		return customRoutingProps.get(attrName);
	}

	{
//		CommonPreference<String> pref = getCustomRoutingProperty("appMode");
//		pref.setModeDefaultValue(ApplicationMode.CAR, "car");
	}

	Map<String, CommonPreference<Boolean>> customBooleanRoutingProps = new LinkedHashMap<String, OsmandSettings.CommonPreference<Boolean>>();

	public CommonPreference<Boolean> getCustomRoutingBooleanProperty(String attrName, boolean defaulfValue) {
		if (!customBooleanRoutingProps.containsKey(attrName)) {
			customBooleanRoutingProps.put(attrName, new BooleanPreference("prouting_" + attrName, defaulfValue).makeProfile());
		}
		return customBooleanRoutingProps.get(attrName);
	}

	public final OsmandPreference<Boolean> USE_OSM_LIVE_FOR_ROUTING = new BooleanPreference("enable_osmc_routing", false).makeGlobal();
	
	public final OsmandPreference<Boolean> VOICE_MUTE = new BooleanPreference("voice_mute", false).makeGlobal();

	// for background service
	public final OsmandPreference<Boolean> MAP_ACTIVITY_ENABLED = new BooleanPreference("map_activity_enabled", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> SAFE_MODE = new BooleanPreference("safe_mode", false).makeGlobal();

	public final OsmandPreference<Boolean> NATIVE_RENDERING_FAILED = new BooleanPreference("native_rendering_failed_init", false).makeGlobal();

	public final OsmandPreference<Boolean> USE_OPENGL_RENDER = new BooleanPreference("use_opengl_render",
			false /*Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH*/
	).makeGlobal().cache();

	public final OsmandPreference<Boolean> OPENGL_RENDER_FAILED = new BooleanPreference("opengl_render_failed", false).makeGlobal().cache();


	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<String> CONTRIBUTION_INSTALL_APP_DATE = new StringPreference("CONTRIBUTION_INSTALL_APP_DATE", null).makeGlobal();


	public final OsmandPreference<Integer> COORDINATES_FORMAT = new IntPreference("coordinates_format", PointDescription.FORMAT_DEGREES).makeGlobal();

	public final OsmandPreference<Boolean> FOLLOW_THE_ROUTE = new BooleanPreference("follow_to_route", false).makeGlobal();
	public final OsmandPreference<String> FOLLOW_THE_GPX_ROUTE = new StringPreference("follow_gpx", null).makeGlobal();

	public final ListStringPreference TRANSPORT_DEFAULT_SETTINGS =
			(ListStringPreference) new ListStringPreference("transport_default_settings", "transportStops", ",").makeProfile();
	
	public final OsmandPreference<Boolean> SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME =
			new BooleanPreference("show_arrival_time", true).makeGlobal();

	public final OsmandPreference<Boolean> SHOW_RELATIVE_BEARING_OTHERWISE_REGULAR_BEARING =
			new BooleanPreference("show_relative_bearing", true).makeGlobal();

	public final OsmandPreference<Long> AGPS_DATA_LAST_TIME_DOWNLOADED =
			new LongPreference("agps_data_downloaded", 0).makeGlobal();

	// Live Updates
	public final OsmandPreference<Boolean> IS_LIVE_UPDATES_ON =
			new BooleanPreference("is_live_updates_on", false).makeGlobal();
	public final OsmandPreference<Integer> LIVE_UPDATES_RETRIES =
			new IntPreference("live_updates_retryes", 2).makeGlobal();

	// UI boxes
	public final CommonPreference<Boolean> TRANSPARENT_MAP_THEME =
			new BooleanPreference("transparent_map_theme", true).makeProfile();

	{
		TRANSPARENT_MAP_THEME.setModeDefaultValue(ApplicationMode.CAR, false);
		TRANSPARENT_MAP_THEME.setModeDefaultValue(ApplicationMode.BICYCLE, false);
		TRANSPARENT_MAP_THEME.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
	}

	public final CommonPreference<Boolean> SHOW_STREET_NAME =
			new BooleanPreference("show_street_name", false).makeProfile();

	{
		SHOW_STREET_NAME.setModeDefaultValue(ApplicationMode.DEFAULT, false);
		SHOW_STREET_NAME.setModeDefaultValue(ApplicationMode.CAR, true);
		SHOW_STREET_NAME.setModeDefaultValue(ApplicationMode.BICYCLE, false);
		SHOW_STREET_NAME.setModeDefaultValue(ApplicationMode.PEDESTRIAN, false);
	}

	public static final int OSMAND_DARK_THEME = 0;
	public static final int OSMAND_LIGHT_THEME = 1;


	public final CommonPreference<Integer> SEARCH_TAB =
			new IntPreference("SEARCH_TAB", 0).makeGlobal().cache();

	public final CommonPreference<Integer> FAVORITES_TAB =
			new IntPreference("FAVORITES_TAB", 0).makeGlobal().cache();

	public final CommonPreference<Integer> OSMAND_THEME =
			new IntPreference("osmand_theme", OSMAND_LIGHT_THEME).makeGlobal().cache();

	public boolean isLightActionBar() {
		return true;
	}


	public boolean isLightContent() {
		return OSMAND_THEME.get() != OSMAND_DARK_THEME;
	}


	public final CommonPreference<Boolean> FLUORESCENT_OVERLAYS =
			new BooleanPreference("fluorescent_overlays", false).makeGlobal().cache();


	public final CommonPreference<Boolean> SHOW_RULER =
			new BooleanPreference("show_ruler", true).makeProfile().cache();

//	public final OsmandPreference<Integer> NUMBER_OF_FREE_DOWNLOADS_V2 = new IntPreference("free_downloads_v2", 0).makeGlobal();

	public final OsmandPreference<Integer> NUMBER_OF_FREE_DOWNLOADS = new IntPreference("free_downloads_v3", 0).makeGlobal();

	// For DashRateUsFragment
	public final OsmandPreference<Long> LAST_DISPLAY_TIME =
			new LongPreference("last_display_time", 0).makeGlobal().cache();

	public final OsmandPreference<Long> LAST_CHECKED_UPDATES =
			new LongPreference("last_checked_updates", 0).makeGlobal();

	public final OsmandPreference<Integer> NUMBER_OF_APPLICATION_STARTS =
			new IntPreference("number_of_app_starts", 0).makeGlobal().cache();

	public final OsmandPreference<RateUsBottomSheetDialog.RateUsState> RATE_US_STATE =
			new EnumIntPreference<>("rate_us_state",
					RateUsBottomSheetDialog.RateUsState.INITIAL_STATE, RateUsBottomSheetDialog.RateUsState.values())
					.makeGlobal();


	public enum DayNightMode {
		AUTO(R.string.daynight_mode_auto, R.drawable.ic_action_map_sunst),
		DAY(R.string.daynight_mode_day, R.drawable.ic_action_map_day),
		NIGHT(R.string.daynight_mode_night, R.drawable.ic_action_map_night),
		SENSOR(R.string.daynight_mode_sensor, R.drawable.ic_action_map_light_sensor);

		private final int key;
		@DrawableRes
		private final int drawableRes;

		DayNightMode(@StringRes int key, @DrawableRes int drawableRes) {
			this.key = key;
			this.drawableRes = drawableRes;
		}

		public String toHumanString(Context ctx) {
			return ctx.getString(key);
		}

		@DrawableRes
		public int getIconRes() {
			return drawableRes;
		}

		public boolean isSensor() {
			return this == SENSOR;
		}

		public boolean isAuto() {
			return this == AUTO;
		}

		public boolean isDay() {
			return this == DAY;
		}

		public boolean isNight() {
			return this == NIGHT;
		}

		public static DayNightMode[] possibleValues(Context context) {
			SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
			Sensor mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
			boolean isLightSensorEnabled = mLight != null;
			if (isLightSensorEnabled) {
				return DayNightMode.values();
			} else {
				return new DayNightMode[]{AUTO, DAY, NIGHT};
			}
		}
	}


	public enum LayerTransparencySeekbarMode {
		OVERLAY(R.string.overlay_transparency),
		UNDERLAY(R.string.map_transparency),
		OFF(R.string.shared_string_off),
		UNDEFINED(R.string.shared_string_none);

		private final int key;

		LayerTransparencySeekbarMode(int key) {
			this.key = key;
		}

		public String toHumanString(Context ctx) {
			return ctx.getString(key);
		}
	}

	public enum MapMarkersMode {
		TOOLBAR(R.string.shared_string_topbar),
		WIDGETS(R.string.shared_string_widgets),
		NONE(R.string.shared_string_none);

		private final int key;

		MapMarkersMode(int key) {
			this.key = key;
		}

		public String toHumanString(Context ctx) {
			return ctx.getString(key);
		}

		public boolean isToolbar() {
			return this == TOOLBAR;
		}

		public boolean isWidgets() {
			return this == WIDGETS;
		}

		public boolean isNone() {
			return this == NONE;
		}

		public static MapMarkersMode[] possibleValues(Context context) {
			return new MapMarkersMode[]{TOOLBAR, WIDGETS, NONE};
		}
	}

	public enum SpeedConstants {
		KILOMETERS_PER_HOUR(R.string.km_h, R.string.si_kmh),
		MILES_PER_HOUR(R.string.mile_per_hour, R.string.si_mph),
		METERS_PER_SECOND(R.string.m_s, R.string.si_m_s),
		MINUTES_PER_MILE(R.string.min_mile, R.string.si_min_m),
		MINUTES_PER_KILOMETER(R.string.min_km, R.string.si_min_km),
		NAUTICALMILES_PER_HOUR(R.string.nm_h, R.string.si_nm_h);

		private final int key;
		private int descr;

		SpeedConstants(int key, int descr) {
			this.key = key;
			this.descr = descr;
		}

		public String toHumanString(Context ctx) {
			return ctx.getString(descr);
		}

		public String toShortString(Context ctx) {
			return ctx.getString(key);
		}


	}

	public enum MetricsConstants {
		KILOMETERS_AND_METERS(R.string.si_km_m, "km-m"),
		MILES_AND_FEET(R.string.si_mi_feet, "mi-f"),
		MILES_AND_METERS(R.string.si_mi_meters, "mi-m"),
		MILES_AND_YARDS(R.string.si_mi_yard, "mi-y"),
		NAUTICAL_MILES(R.string.si_nm, "nm");

		private final int key;
		private final String ttsString;

		MetricsConstants(int key, String ttsString) {
			this.key = key;
			this.ttsString = ttsString;
		}

		public String toHumanString(Context ctx) {
			return ctx.getString(key);
		}

		public String toTTSString() {
			return ttsString;
		}

	}

	public enum AutoZoomMap {
		FARTHEST(R.string.auto_zoom_farthest, 1f, 15.5f),
		FAR(R.string.auto_zoom_far, 1.4f, 17f),
		CLOSE(R.string.auto_zoom_close, 2f, 19f);
		public final float coefficient;
		public final int name;
		public final float maxZoom;

		AutoZoomMap(int name, float coefficient, float maxZoom) {
			this.name = name;
			this.coefficient = coefficient;
			this.maxZoom = maxZoom;

		}
	}

	/**
	 * Class represents specific for driving region
	 * Signs, leftHandDriving
	 */
	public enum DrivingRegion {

		EUROPE_ASIA(R.string.driving_region_europe_asia, MetricsConstants.KILOMETERS_AND_METERS, false, false),
		US(R.string.driving_region_us, MetricsConstants.MILES_AND_FEET, false, true),
		CANADA(R.string.driving_region_canada, MetricsConstants.KILOMETERS_AND_METERS, false, true),
		UK_AND_OTHERS(R.string.driving_region_uk, MetricsConstants.MILES_AND_METERS, true, false),
		JAPAN(R.string.driving_region_japan, MetricsConstants.KILOMETERS_AND_METERS, true, false),
		AUSTRALIA(R.string.driving_region_australia, MetricsConstants.KILOMETERS_AND_METERS, true, true);

		public final boolean leftHandDriving;
		public final boolean americanSigns;
		public final MetricsConstants defMetrics;
		public final int name;

		DrivingRegion(int name, MetricsConstants def, boolean leftHandDriving, boolean americanSigns) {
			this.name = name;
			defMetrics = def;
			this.leftHandDriving = leftHandDriving;
			this.americanSigns = americanSigns;
		}

		public String getDescription(Context ctx) {
			return ctx.getString(leftHandDriving ? R.string.left_side_navigation : R.string.right_side_navigation) +
					", " +
					defMetrics.toHumanString(ctx).toLowerCase();
		}
	}

}
