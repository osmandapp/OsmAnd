package net.osmand.plus.settings.backend;


import static net.osmand.IndexConstants.SQLITE_EXT;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONFIGURE_MAP_ITEM_ID_SCHEME;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_ITEM_ID_SCHEME;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_ACTIONS;
import static net.osmand.plus.AppVersionUpgradeOnInit.updateExistingWidgetIds;
import static net.osmand.plus.download.DownloadOsmandIndexesHelper.downloadTtsWithoutInternet;
import static net.osmand.plus.download.DownloadOsmandIndexesHelper.getSupportedTtsByLanguages;
import static net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin.HIDE_WATER_POLYGONS_ATTR;
import static net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin.NO_POLYGONS_ATTR;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.CONTOUR_LINES_ATTR;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.ELEVATION_UNITS_ATTR;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.ELEVATION_UNITS_FEET_VALUE;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.ELEVATION_UNITS_METERS_VALUE;
import static net.osmand.plus.routing.GpxApproximator.DEFAULT_POINT_APPROXIMATION;
import static net.osmand.plus.routing.TransportRoutingHelper.PUBLIC_TRANSPORT_KEY;
import static net.osmand.plus.settings.backend.storages.IntermediatePointsStorage.INTERMEDIATE_POINTS;
import static net.osmand.plus.settings.backend.storages.IntermediatePointsStorage.INTERMEDIATE_POINTS_DESCRIPTION;
import static net.osmand.plus.settings.enums.ApproximationType.APPROX_GEO_CPP;
import static net.osmand.plus.settings.enums.LocationSource.ANDROID_API;
import static net.osmand.plus.settings.enums.LocationSource.GOOGLE_PLAY_SERVICES;
import static net.osmand.plus.settings.enums.RoutingType.HH_CPP;
import static net.osmand.plus.settings.enums.SpeedLimitWarningState.WHEN_EXCEEDED;
import static net.osmand.plus.settings.enums.WidgetSize.MEDIUM;
import static net.osmand.plus.settings.enums.WidgetSize.SMALL;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.PAGE_SEPARATOR;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.WIDGET_SEPARATOR;
import static net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState.DEFAULT_BUTTON_ID;
import static net.osmand.render.RenderingRuleStorageProperties.A_APP_MODE;
import static net.osmand.render.RenderingRuleStorageProperties.A_BASE_APP_MODE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.util.LocalePreferences;

import net.osmand.IndexConstants;
import net.osmand.Period;
import net.osmand.Period.PeriodUnit;
import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.data.DataSourceType;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.ValueHolder;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.api.SettingsAPI;
import net.osmand.plus.api.SettingsAPI.SettingsEditor;
import net.osmand.plus.api.SettingsAPIImpl;
import net.osmand.plus.avoidroads.AvoidRoadInfo;
import net.osmand.plus.card.color.palette.gradient.PaletteGradientColor;
import net.osmand.plus.card.color.palette.main.data.DefaultColors;
import net.osmand.plus.charts.GPXDataSetAxisType;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.configmap.routes.MtbClassification;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.feedback.RateUsState;
import net.osmand.plus.helpers.OsmandBackupAgent;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase.PurchaseOrigin;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription.SubscriptionState;
import net.osmand.plus.keyevent.devices.KeyboardDeviceProfile;
import net.osmand.plus.mapmarkers.CoordinateInputFormats.Format;
import net.osmand.plus.plugins.accessibility.AccessibilityMode;
import net.osmand.plus.plugins.accessibility.RelativeDirectionStyle;
import net.osmand.plus.plugins.rastermaps.LayerTransparencySeekbarMode;
import net.osmand.plus.plugins.weather.units.TemperatureUnit;
import net.osmand.plus.profiles.LocationIcon;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.settings.backend.menuitems.ContextMenuItemsSettings;
import net.osmand.plus.settings.backend.menuitems.DrawerMenuItemsSettings;
import net.osmand.plus.settings.backend.menuitems.MainContextMenuItemsSettings;
import net.osmand.plus.settings.backend.preferences.*;
import net.osmand.plus.settings.backend.storages.ImpassableRoadsStorage;
import net.osmand.plus.settings.backend.storages.IntermediatePointsStorage;
import net.osmand.plus.settings.enums.*;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.views.layers.RadiusRulerControlLayer.RadiusRulerMode;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.wikipedia.WikiArticleShowImages;
import net.osmand.render.RenderingClass;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.GeneralRouter;
import net.osmand.shared.gpx.ColoringPurpose;
import net.osmand.shared.obd.OBDDataComputer;
import net.osmand.shared.routing.ColoringType;
import net.osmand.shared.settings.enums.AltitudeMetrics;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.shared.settings.enums.SpeedConstants;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class OsmandSettings {

	private static final Log LOG = PlatformUtil.getLog(OsmandSettings.class.getName());

	public static final int VERSION = 1;

	// These settings are stored in SharedPreferences
	public static final String CUSTOM_SHARED_PREFERENCES_PREFIX = "net.osmand.customsettings.";
	public static final String SHARED_PREFERENCES_NAME = "net.osmand.settings";
	private static String CUSTOM_SHARED_PREFERENCES_NAME;

	public static final String RENDERER_PREFERENCE_PREFIX = "nrenderer_";
	public static final String ROUTING_PREFERENCE_PREFIX = "prouting_";

	public static final float SIM_MIN_SPEED = 5 / 3.6f;
	/// Settings variables
	private final OsmandApplication ctx;
	private SettingsAPI settingsAPI;
	private Object globalPreferences;
	private Object profilePreferences;
	private ApplicationMode currentMode;
	private final Map<String, OsmandPreference<?>> registeredPreferences = new LinkedHashMap<>();

	// cache variables
	private final long lastTimeInternetConnectionChecked = 0;
	private boolean internetConnectionAvailable = true;

	// TODO variable
	private final Map<String, CommonPreference<String>> customRoutingProps = new LinkedHashMap<>();
	private final Map<String, CommonPreference<String>> customRendersProps = new LinkedHashMap<>();
	private final Map<String, CommonPreference<Boolean>> customBooleanRoutingProps = new LinkedHashMap<>();
	private final Map<String, CommonPreference<Boolean>> customBooleanRendersProps = new LinkedHashMap<>();
	private final Map<String, CommonPreference<Boolean>> customBooleanRenderClassProps = new LinkedHashMap<>();

	private final ImpassableRoadsStorage impassableRoadsStorage = new ImpassableRoadsStorage(this);
	private final IntermediatePointsStorage intermediatePointsStorage = new IntermediatePointsStorage(this);

	private StateChangedListener<ApplicationMode> appModeListener;

	private Object objectToShow;
	private boolean editObjectToShow;
	private String searchRequestToShow;

	protected OsmandSettings(OsmandApplication clientContext, SettingsAPI settinsAPI) {
		ctx = clientContext;
		this.settingsAPI = settinsAPI;
		initPrefs();
	}

	protected OsmandSettings(OsmandApplication clientContext, SettingsAPI settinsAPI, String sharedPreferencesName) {
		ctx = clientContext;
		this.settingsAPI = settinsAPI;
		CUSTOM_SHARED_PREFERENCES_NAME = CUSTOM_SHARED_PREFERENCES_PREFIX + sharedPreferencesName;
		initPrefs();
		setCustomized();
	}

	private void initPrefs() {
		globalPreferences = settingsAPI.getPreferenceObject(getSharedPreferencesName(null));
		currentMode = readApplicationMode();
		profilePreferences = getProfilePreferences(currentMode);
		registeredPreferences.put(APPLICATION_MODE.getId(), APPLICATION_MODE);
		initBaseAppMode();
	}

	private void initBaseAppMode() {
		setAppModeCustomProperties();
		appModeListener = applicationMode -> setAppModeCustomProperties();
		APPLICATION_MODE.addListener(appModeListener);
	}

	public void setAppModeCustomProperties() {
		ApplicationMode appMode = APPLICATION_MODE.get();
		ApplicationMode parentAppMode = APPLICATION_MODE.get().getParent();

		executePreservingPrefTimestamp(appMode, () -> {
			CommonPreference<String> appModePref = getCustomRenderProperty(A_APP_MODE);
			CommonPreference<String> baseAppModePref = getCustomRenderProperty(A_BASE_APP_MODE);

			appModePref.setModeValue(appMode, appMode.getStringKey());
			baseAppModePref.setModeValue(appMode, parentAppMode != null
					? parentAppMode.getStringKey() : appMode.getStringKey());
		});
	}

	public void executePreservingPrefTimestamp(@NonNull Runnable action) {
		executePreservingPrefTimestamp(getApplicationMode(), action);
	}

	public void executePreservingPrefTimestamp(@NonNull ApplicationMode mode, @NonNull Runnable action) {
		long time = getLastModePreferencesEditTime(mode);
		try {
			action.run();
		} finally {
			setLastModePreferencesEditTime(mode, time);
		}
	}

	@NonNull
	public Map<String, OsmandPreference<?>> getRegisteredPreferences() {
		return Collections.unmodifiableMap(registeredPreferences);
	}

	@NonNull
	public Map<String, OsmandPreference<?>> getSavedPreferences(@Nullable ApplicationMode mode) {
		Map<String, OsmandPreference<?>> map = new HashMap<>(registeredPreferences.size());
		for (Map.Entry<String, OsmandPreference<?>> entry : registeredPreferences.entrySet()) {
			OsmandPreference<?> value = entry.getValue();
			if (entry.getValue() instanceof CommonPreference<?> preference) {
				if ((mode == null && preference.isGlobal() && preference.isSet()) ||
						(mode != null && !preference.isGlobal() && preference.isSetForMode(mode))) {
					map.put(entry.getKey(), preference);
				}
			}
		}
		return map;
	}

	public static boolean isRendererPreference(String key) {
		return key.startsWith(RENDERER_PREFERENCE_PREFIX);
	}

	public static boolean isRoutingPreference(String key) {
		return key.startsWith(ROUTING_PREFERENCE_PREFIX);
	}

	private static final String SETTING_CUSTOMIZED_ID = "settings_customized";

	private void setCustomized() {
		settingsAPI.edit(globalPreferences).putBoolean(SETTING_CUSTOMIZED_ID, true).commit();
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

	public OsmAndPreferencesDataStore getDataStore(@Nullable ApplicationMode appMode) {
		return new OsmAndPreferencesDataStore(this, appMode != null ? appMode : APPLICATION_MODE.get());
	}

	public static String getSharedPreferencesName(@Nullable ApplicationMode mode) {
		String modeKey = mode != null ? mode.getStringKey() : null;
		return getSharedPreferencesNameForKey(modeKey);
	}

	public static String getSharedPreferencesNameForKey(@Nullable String modeKey) {
		String sharedPreferencesName = !Algorithms.isEmpty(CUSTOM_SHARED_PREFERENCES_NAME) ? CUSTOM_SHARED_PREFERENCES_NAME : SHARED_PREFERENCES_NAME;
		if (modeKey == null) {
			return sharedPreferencesName;
		} else {
			return sharedPreferencesName + "." + modeKey.toLowerCase();
		}
	}

	public static boolean areSettingsCustomizedForPreference(String sharedPreferencesName, OsmandApplication app) {
		String customPrefName = CUSTOM_SHARED_PREFERENCES_PREFIX + sharedPreferencesName;
		SettingsAPIImpl settingsAPI = new net.osmand.plus.api.SettingsAPIImpl(app);
		SharedPreferences globalPreferences = (SharedPreferences) settingsAPI.getPreferenceObject(customPrefName);

		return globalPreferences != null && globalPreferences.getBoolean(SETTING_CUSTOMIZED_ID, false);
	}

	// TODO doesn't look correct package visibility
	public Object getProfilePreferences(@Nullable ApplicationMode mode) {
		return settingsAPI.getPreferenceObject(getSharedPreferencesName(mode));
	}

	// TODO doesn't look correct package visibility
	Object getProfilePreferences(@Nullable String modeKey) {
		return settingsAPI.getPreferenceObject(getSharedPreferencesNameForKey(modeKey));
	}

	public OsmandPreference<?> getPreference(@NonNull String key) {
		return registeredPreferences.get(key);
	}

	// TODO doesn't look correct
	public void setPreferenceForAllModes(String key, Object value) {
		for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
			setPreference(key, value, mode);
		}
	}

	// TODO doesn't look correct
	public boolean setPreference(String key, Object value) {
		return setPreference(key, value, APPLICATION_MODE.get());
	}

	// TODO doesn't look correct
	@SuppressWarnings("unchecked")
	public boolean setPreference(String key, Object value, ApplicationMode mode) {
		OsmandPreference<?> preference = registeredPreferences.get(key);
		if (preference == null) {
			return false;
		}
		if (preference == APPLICATION_MODE) {
			if (value instanceof String appModeKey) {
				ApplicationMode appMode = ApplicationMode.valueOfStringKey(appModeKey, null);
				if (appMode != null) {
					return setApplicationMode(appMode);
				}
			}
		} else if (preference == DEFAULT_APPLICATION_MODE) {
			if (value instanceof String appModeKey) {
				ApplicationMode appMode = ApplicationMode.valueOfStringKey(appModeKey, null);
				if (appMode != null) {
					return DEFAULT_APPLICATION_MODE.set(appMode);
				}
			}
		} else if (preference instanceof EnumStringPreference enumPref) {
			if (value instanceof String) {
				Enum<?> enumValue = enumPref.parseString((String) value);
				if (enumValue != null) {
					return enumPref.setModeValue(mode, enumValue);
				}
				return false;
			} else if (value instanceof Enum) {
				return enumPref.setModeValue(mode, value);
			} else if (value instanceof Integer) {
				int newVal = (Integer) value;
				if (newVal >= 0 && newVal < enumPref.getValues().length) {
					Enum<?> enumValue = enumPref.getValues()[newVal];
					return enumPref.setModeValue(mode, enumValue);
				}
				return false;
			}
		} else if (preference instanceof ListStringPreference listStringPreference) {
			if (value instanceof List<?> || (value == null && listStringPreference.isNullSupported(mode))) {
				if (value == null) {
					listStringPreference.setStringsListForProfile(mode, null);
				} else {
					List<?> list = (List<?>) value;
					boolean isListOfString = list.stream().allMatch(element -> element instanceof String);
					if (isListOfString) {
						List<String> listOfString = (List<String>) list;
						listStringPreference.setStringsListForProfile(mode, listOfString);
					}
				}
			}
			if (value instanceof String) {
				return listStringPreference.setModeValue(mode, (String) value);
			}
		} else if (preference instanceof StringPreference stringPref) {
			if (value instanceof String || (value == null && stringPref.isNullSupported(mode))) {
				return stringPref.setModeValue(mode, (String) value);
			}
		} else {
			if (value instanceof String) {
				value = preference.parseString((String) value);
			}
			if (preference instanceof BooleanPreference) {
				if (value instanceof Boolean) {
					return ((BooleanPreference) preference).setModeValue(mode, (Boolean) value);
				}
			} else if (preference instanceof FloatPreference) {
				if (value instanceof Float) {
					return ((FloatPreference) preference).setModeValue(mode, (Float) value);
				}
			} else if (preference instanceof IntPreference) {
				if (value instanceof Integer) {
					return ((IntPreference) preference).setModeValue(mode, (Integer) value);
				}
			} else if (preference instanceof LongPreference) {
				if (value instanceof Long) {
					return ((LongPreference) preference).setModeValue(mode, (Long) value);
				}
			} else if (preference instanceof ContextMenuItemsPreference) {
				if (value instanceof ContextMenuItemsSettings) {
					return ((ContextMenuItemsPreference) preference).setModeValue(mode, (ContextMenuItemsSettings) value);
				}
			}
		}
		return false;
	}

	public void resetPreferenceForAllModes(@NonNull String prefId) {
		OsmandPreference<?> preference = getPreference(prefId);
		if (preference != null) {
			for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
				preference.resetModeToDefault(mode);
			}
		}
	}

	public void resetPreference(@NonNull String prefId, @NonNull ApplicationMode mode) {
		OsmandPreference<?> preference = getPreference(prefId);
		if (preference != null) {
			preference.resetModeToDefault(mode);
		}
	}

	public void copyPreferencesFromProfile(@NonNull ApplicationMode modeFrom,
			@NonNull ApplicationMode modeTo, boolean onlySaved) {
		Map<String, OsmandPreference<?>> preferences = onlySaved ? getSavedPreferences(modeFrom) : registeredPreferences;
		copyProfilePreferences(modeFrom, modeTo, new ArrayList<>(preferences.values()));
	}

	public void copyProfilePreferences(@NonNull ApplicationMode modeFrom,
			@NonNull ApplicationMode modeTo, @NonNull List<OsmandPreference> profilePreferences) {
		for (OsmandPreference pref : profilePreferences) {
			if (prefCanBeCopiedOrReset(pref) && !USER_PROFILE_NAME.getId().equals(pref.getId())) {
				CommonPreference profilePref = (CommonPreference) pref;
				if (PARENT_APP_MODE.getId().equals(pref.getId())) {
					if (modeTo.isCustomProfile()) {
						modeTo.setParentAppMode(modeFrom.isCustomProfile() ? modeFrom.getParent() : modeFrom);
					}
				} else {
					Object copiedValue = profilePref.getModeValue(modeFrom);
					profilePref.setModeValue(modeTo, copiedValue);
				}
			}
		}
	}

	public void resetGlobalPreferences(List<OsmandPreference> preferences) {
		for (OsmandPreference preference : preferences) {
			if (preference instanceof CommonPreference) {
				preference.resetToDefault();
			}
		}
	}

	public void resetPreferencesForProfile(ApplicationMode mode) {
		resetProfilePreferences(mode, new ArrayList<>(registeredPreferences.values()));
		setAppModeCustomProperties();
	}

	public void resetProfilePreferences(ApplicationMode mode, List<OsmandPreference> profilePreferences) {
		for (OsmandPreference pref : profilePreferences) {
			if (prefCanBeCopiedOrReset(pref)) {
				pref.resetModeToDefault(mode);
			}
		}
	}

	private boolean prefCanBeCopiedOrReset(OsmandPreference pref) {
		return pref instanceof CommonPreference && !((CommonPreference) pref).isGlobal()
				&& !APP_MODE_ORDER.getId().equals(pref.getId())
				&& !APP_MODE_VERSION.getId().equals(pref.getId());
	}

	public boolean isExportAvailableForPref(@NonNull OsmandPreference<?> preference) {
		if (APPLICATION_MODE.getId().equals(preference.getId())) {
			return true;
		} else if (preference instanceof CommonPreference) {
			CommonPreference<?> commonPreference = (CommonPreference<?>) preference;
			return !commonPreference.isGlobal() || commonPreference.isShared();
		}
		return false;
	}

	public ApplicationMode LAST_ROUTING_APPLICATION_MODE;

	public boolean switchAppModeToNext() {
		return switchAppMode(true);
	}

	public boolean switchAppModeToPrevious() {
		return switchAppMode(false);
	}

	public boolean switchAppMode(boolean next) {
		ApplicationMode appMode = getApplicationMode();
		ApplicationMode nextAppMode = getSwitchedAppMode(appMode, next);
		if (appMode != nextAppMode && setApplicationMode(nextAppMode)) {
			ctx.showShortToastMessage(R.string.application_profile_changed, nextAppMode.toHumanString());
			return true;
		}
		return false;
	}

	public ApplicationMode getSwitchedAppMode(ApplicationMode selectedMode, boolean next) {
		List<ApplicationMode> enabledModes = ApplicationMode.values(ctx);
		int indexOfCurrent = enabledModes.indexOf(selectedMode);
		int indexOfNext;
		if (next) {
			indexOfNext = indexOfCurrent < enabledModes.size() - 1 ? indexOfCurrent + 1 : 0;
		} else {
			indexOfNext = indexOfCurrent > 0 ? indexOfCurrent - 1 : enabledModes.size() - 1;
		}
		return enabledModes.get(indexOfNext);
	}

	public boolean setApplicationMode(ApplicationMode appMode) {
		return setApplicationMode(appMode, true);
	}

	public boolean setApplicationMode(ApplicationMode appMode, boolean markAsLastUsed) {
		boolean valueSaved = APPLICATION_MODE.set(appMode);
		if (markAsLastUsed && valueSaved) {
			LAST_USED_APPLICATION_MODE.set(appMode.getStringKey());
		}
		return valueSaved;
	}

	public final OsmandPreference<ApplicationMode> APPLICATION_MODE = new PreferenceWithListener<>() {

		@Override
		public String getId() {
			return "application_mode";
		}

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

		@Override
		public void resetModeToDefault(ApplicationMode m) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isSet() {
			return true;
		}

		@Override
		public boolean isSetForMode(ApplicationMode m) {
			return true;
		}

		@Override
		public boolean set(ApplicationMode val) {
			ApplicationMode oldMode = currentMode;
			boolean valueSaved = settingsAPI.edit(globalPreferences).putString(getId(), val.getStringKey()).commit();
			if (valueSaved) {
				currentMode = val;
				profilePreferences = getProfilePreferences(currentMode);

				fireEvent(oldMode);
			}
			return valueSaved;
		}

		@Override
		public ApplicationMode getModeValue(ApplicationMode m) {
			return m;
		}

		@Override
		public boolean setModeValue(ApplicationMode m, ApplicationMode obj) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean writeToJson(JSONObject json, ApplicationMode appMode) throws JSONException {
			if (appMode == null) {
				return writeAppModeToJson(json, this);
			} else {
				return true;
			}
		}

		@Override
		public void readFromJson(JSONObject json, ApplicationMode appMode) throws JSONException {
			if (appMode == null) {
				readAppModeFromJson(json, this);
			}
		}

		@Override
		public String asString() {
			return appModeToString(get());
		}

		@Override
		public String asStringModeValue(ApplicationMode m) {
			return appModeToString(m);
		}

		@Override
		public ApplicationMode parseString(String s) {
			return appModeFromString(s);
		}
	};

	private String appModeToString(ApplicationMode appMode) {
		return appMode.getStringKey();
	}

	private ApplicationMode appModeFromString(String s) {
		return ApplicationMode.valueOfStringKey(s, ApplicationMode.DEFAULT);
	}

	private boolean writeAppModeToJson(JSONObject json, OsmandPreference<ApplicationMode> appModePref) throws JSONException {
		json.put(appModePref.getId(), appModePref.asString());
		return true;
	}

	private void readAppModeFromJson(JSONObject json, OsmandPreference<ApplicationMode> appModePref) throws JSONException {
		String s = json.getString(appModePref.getId());
		appModePref.set(appModePref.parseString(s));
	}

	public ApplicationMode getApplicationMode() {
		return APPLICATION_MODE.get();
	}

	public boolean hasAvailableApplicationMode() {
		int currentModeCount = ApplicationMode.values(ctx).size();
		return currentModeCount != 0 && (currentModeCount != 1 || getApplicationMode() != ApplicationMode.DEFAULT);
	}

	public ApplicationMode readApplicationMode() {
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
		try {
			ConnectivityManager mgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo ni = mgr.getActiveNetworkInfo();
			return ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI;
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isInternetConnected() {
		try {
			ConnectivityManager mgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo active = mgr.getActiveNetworkInfo();
			if (active == null) {
				return false;
			} else {
				NetworkInfo.State state = active.getState();
				return state != NetworkInfo.State.DISCONNECTED && state != NetworkInfo.State.DISCONNECTING;
			}
		} catch (Exception e) {
			return false;
		}
	}

	public <T> void registerInternalPreference(String id, CommonPreference<T> preference) {
		registeredPreferences.put(id, preference);
	}

	public boolean isSet(boolean global, String id) {
		return settingsAPI.contains(getPreferences(global), id);
	}

	public boolean isSet(ApplicationMode mode, String id) {
		return settingsAPI.contains(getProfilePreferences(mode), id);
	}

	public Object getPreferences(boolean global) {
		return global ? globalPreferences : profilePreferences;
	}

	private static final String LAST_PREFERENCES_EDIT_TIME = "last_preferences_edit_time";

	public long getLastModePreferencesEditTime(ApplicationMode mode) {
		Object preferences = getProfilePreferences(mode);
		return getLastPreferencesEditTime(preferences);
	}

	public void setLastModePreferencesEditTime(ApplicationMode mode, long lastModifiedTime) {
		Object preferences = getProfilePreferences(mode);
		updateLastPreferencesEditTime(preferences, lastModifiedTime);
	}

	public long getLastGlobalPreferencesEditTime() {
		return getLastPreferencesEditTime(globalPreferences);
	}

	public void setLastGlobalPreferencesEditTime(long lastModifiedTime) {
		updateLastPreferencesEditTime(globalPreferences, lastModifiedTime);
	}

	private long getLastPreferencesEditTime(@NonNull Object preferences) {
		return settingsAPI.getLong(preferences, LAST_PREFERENCES_EDIT_TIME, 0);
	}

	public void updateLastPreferencesEditTime(@NonNull Object preferences) {
		long time = System.currentTimeMillis();
		updateLastPreferencesEditTime(preferences, time);
	}

	protected void updateLastPreferencesEditTime(@NonNull Object preferences, long time) {
		settingsAPI.edit(preferences).putLong(LAST_PREFERENCES_EDIT_TIME, time).commit();
	}

	public void resetLastGlobalPreferencesEditTime() {
		settingsAPI.edit(globalPreferences).remove(LAST_PREFERENCES_EDIT_TIME).commit();
	}

	public void resetLastPreferencesEditTime(@NonNull ApplicationMode mode) {
		Object profilePrefs = getProfilePreferences(mode);
		settingsAPI.edit(profilePrefs).remove(LAST_PREFERENCES_EDIT_TIME).commit();
	}

	public void removePreferences(@NonNull Collection<CommonPreference<?>> preferences) {
		Set<String> globalIds = new HashSet<>();
		Set<String> profileIds = new HashSet<>();
		for (CommonPreference<?> preference : preferences) {
			String id = preference.getId();
			if (preference.isGlobal()) {
				globalIds.add(id);
			} else {
				profileIds.add(id);
			}
		}
		if (!globalIds.isEmpty()) {
			removeFromGlobalPreferences(globalIds.toArray(new String[] {}));
		}
		if (!profileIds.isEmpty()) {
			removeFromModePreferences(profileIds.toArray(new String[] {}));
		}
	}

	public void removeFromGlobalPreferences(@NonNull String... prefIds) {
		removeFromPreferencesImpl(globalPreferences, prefIds);
	}

	public void removeFromModePreferences(@NonNull String... prefIds) {
		for (ApplicationMode appMode : ApplicationMode.allPossibleValues()) {
			Object preferences = getProfilePreferences(appMode);
			if (preferences != null) {
				removeFromPreferencesImpl(preferences, prefIds);
			}
		}
	}

	private void removeFromPreferencesImpl(@NonNull Object preferences, @NonNull String... prefIds) {
		SettingsEditor editor = settingsAPI.edit(preferences);
		for (String prefId : prefIds) {
			editor.remove(prefId);
		}
		editor.commit();
	}

	public int getSavedGlobalPrefsCount() {
		return ((SharedPreferences) globalPreferences).getAll().size();
	}

	public int getSavedModePrefsCount(@NonNull ApplicationMode mode) {
		return ((SharedPreferences) getProfilePreferences(mode)).getAll().size();
	}

	@SuppressWarnings("unchecked")
	public CommonPreference<Boolean> registerBooleanPreference(String id, boolean defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<Boolean>) registeredPreferences.get(id);
		}
		BooleanPreference p = new BooleanPreference(this, id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	@SuppressWarnings("unchecked")
	public CommonPreference<Boolean> registerBooleanAccessibilityPreference(String id, boolean defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<Boolean>) registeredPreferences.get(id);
		}
		BooleanPreference p = new BooleanAccessibilityPreference(this, id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	@SuppressWarnings("unchecked")
	public CommonPreference<String> registerStringPreference(String id, String defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<String>) registeredPreferences.get(id);
		}
		StringPreference p = new StringPreference(this, id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	public ListStringPreference registerStringListPreference(@NonNull String id, @Nullable String defValue, @NonNull String delimiter) {
		if (registeredPreferences.containsKey(id)) {
			return (ListStringPreference) registeredPreferences.get(id);
		}
		ListStringPreference preference = new ListStringPreference(this, id, defValue, delimiter);
		registeredPreferences.put(id, preference);
		return preference;
	}

	@SuppressWarnings("unchecked")
	public CommonPreference<Integer> registerIntPreference(String id, int defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<Integer>) registeredPreferences.get(id);
		}
		IntPreference p = new IntPreference(this, id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	@SuppressWarnings("unchecked")
	public CommonPreference<Long> registerLongPreference(String id, long defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<Long>) registeredPreferences.get(id);
		}
		LongPreference p = new LongPreference(this, id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	@SuppressWarnings("unchecked")
	public CommonPreference<Float> registerFloatPreference(String id, float defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<Float>) registeredPreferences.get(id);
		}
		FloatPreference p = new FloatPreference(this, id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	@SuppressWarnings("unchecked")
	public <T extends Enum> CommonPreference<T> registerEnumStringPreference(String id, Enum defaultValue, Enum[] values, Class<T> clz) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<T>) registeredPreferences.get(id);
		}
		EnumStringPreference preference = new EnumStringPreference(this, id, defaultValue, values);
		registeredPreferences.put(id, preference);
		return preference;
	}

	///////////////////// PREFERENCES ////////////////

	public static final String NUMBER_OF_FREE_DOWNLOADS_ID = "free_downloads_v3";

	private final OsmandPreference<String> PLUGINS = new StringPreference(this, "enabled_plugins", "").makeGlobal().makeShared();

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

	public boolean enablePlugin(String pluginId, boolean enable) {
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
			return PLUGINS.set(serialization.toString());
		}
		return false;
	}

	public final CommonPreference<Boolean> ENABLE_3D_MAPS = registerBooleanPreference("enable_3d_maps", true).makeProfile().makeShared().cache();
	public final CommonPreference<Float> VERTICAL_EXAGGERATION_SCALE = registerFloatPreference("vertical_exaggeration_scale", 1f).makeProfile();

	public final CommonPreference<Integer> SIMULATE_POSITION_SPEED = new IntPreference(this, "simulate_position_movement_speed", 1).makeGlobal().makeShared();

	public final CommonPreference<DistanceByTapTextSize> DISTANCE_BY_TAP_TEXT_SIZE = new EnumStringPreference<>(this, "distance_by_tap_text_size", DistanceByTapTextSize.NORMAL, DistanceByTapTextSize.values()).makeProfile();

	public final OsmandPreference<RadiusRulerMode> RADIUS_RULER_MODE = new EnumStringPreference<>(this, "ruler_mode", RadiusRulerMode.FIRST, RadiusRulerMode.values()).makeProfile();
	public final OsmandPreference<Boolean> SHOW_COMPASS_ON_RADIUS_RULER = new BooleanPreference(this, "show_compass_ruler", true).makeProfile();

	public final OsmandPreference<Boolean> SHOW_DISTANCE_RULER = new BooleanPreference(this, "show_distance_ruler", false).makeProfile();

	public final CommonPreference<SpeedLimitWarningState> SHOW_SPEED_LIMIT_WARNING = new EnumStringPreference<>(this, "show_speed_limit_warning", WHEN_EXCEEDED, SpeedLimitWarningState.values()).makeProfile();

	public final CommonPreference<Boolean> SHOW_LINES_TO_FIRST_MARKERS = new BooleanPreference(this, "show_lines_to_first_markers", false).makeProfile();
	public final CommonPreference<Boolean> SHOW_ARROWS_TO_FIRST_MARKERS = new BooleanPreference(this, "show_arrows_to_first_markers", false).makeProfile();

	public final CommonPreference<Boolean> WIKI_ARTICLE_SHOW_IMAGES_ASKED = new BooleanPreference(this, "wikivoyage_show_images_asked", false).makeGlobal();
	public final CommonPreference<WikiArticleShowImages> WIKI_ARTICLE_SHOW_IMAGES = new EnumStringPreference<>(this, "wikivoyage_show_imgs", WikiArticleShowImages.OFF, WikiArticleShowImages.values()).makeGlobal().makeShared();

	public final CommonPreference<Boolean> WIKI_SHOW_IMAGE_PREVIEWS = new BooleanPreference(this, "wiki_show_image_previews", true).makeGlobal();
	public final CommonPreference<DataSourceType> WIKI_DATA_SOURCE_TYPE = new EnumStringPreference<>(this, "wiki_data_source_type", DataSourceType.ONLINE, DataSourceType.values()) {
		@Override
		public DataSourceType getProfileDefaultValue(@Nullable ApplicationMode mode) {
			boolean paidVersion = Version.isPaidVersion(getContext());
			return paidVersion ? DataSourceType.OFFLINE : DataSourceType.ONLINE;
		}
	}.makeGlobal().makeShared();

	public final CommonPreference<Boolean> SELECT_MARKER_ON_SINGLE_TAP = new BooleanPreference(this, "select_marker_on_single_tap", false).makeProfile();
	public final CommonPreference<Boolean> KEEP_PASSED_MARKERS_ON_MAP = new BooleanPreference(this, "keep_passed_markers_on_map", true).makeProfile();

	public final CommonPreference<Boolean> COORDS_INPUT_USE_RIGHT_SIDE = new BooleanPreference(this, "coords_input_use_right_side", true).makeGlobal().makeShared();
	public final OsmandPreference<Format> COORDS_INPUT_FORMAT = new EnumStringPreference<>(this, "coords_input_format", Format.DD_MM_MMM, Format.values()).makeGlobal().makeShared();
	public final CommonPreference<Boolean> COORDS_INPUT_USE_OSMAND_KEYBOARD = new BooleanPreference(this, "coords_input_use_osmand_keyboard", Build.VERSION.SDK_INT >= 16).makeGlobal().makeShared();
	public final CommonPreference<Boolean> COORDS_INPUT_TWO_DIGITS_LONGTITUDE = new BooleanPreference(this, "coords_input_two_digits_longitude", false).makeGlobal().makeShared();

	public final CommonPreference<Boolean> FORCE_PRIVATE_ACCESS_ROUTING_ASKED = new BooleanPreference(this, "force_private_access_routing", false).makeProfile().cache();

	public final CommonPreference<Boolean> SHOW_CARD_TO_CHOOSE_DRAWER = new BooleanPreference(this, "show_card_to_choose_drawer", false).makeGlobal().makeShared();
	public final CommonPreference<Boolean> SHOW_DASHBOARD_ON_START = new BooleanPreference(this, "should_show_dashboard_on_start", false).makeGlobal().makeShared();
	public final CommonPreference<Boolean> SHOW_DASHBOARD_ON_MAP_SCREEN = new BooleanPreference(this, "show_dashboard_on_map_screen", false).makeGlobal().makeShared();
	public final CommonPreference<Boolean> SHOW_OSMAND_WELCOME_SCREEN = new BooleanPreference(this, "show_osmand_welcome_screen", true).makeGlobal();

	public final CommonPreference<String> API_NAV_DRAWER_ITEMS_JSON = new StringPreference(this, "api_nav_drawer_items_json", "{}").makeGlobal();
	public final CommonPreference<String> API_CONNECTED_APPS_JSON = new StringPreference(this, "api_connected_apps_json", "[]").makeGlobal();
	public final CommonPreference<String> NAV_DRAWER_LOGO = new StringPreference(this, "drawer_logo", "").makeProfile();
	public final CommonPreference<String> NAV_DRAWER_URL = new StringPreference(this, "drawer_url", "").makeProfile();

	public final CommonPreference<Integer> NUMBER_OF_STARTS_FIRST_XMAS_SHOWN = new IntPreference(this, "number_of_starts_first_xmas_shown", 0).makeGlobal();

	public final OsmandPreference<String> AVAILABLE_APP_MODES = new StringPreference(this, "available_application_modes", "car,bicycle,pedestrian,public_transport,") {

		@Override
		public void readFromJson(JSONObject json, ApplicationMode appMode) throws JSONException {
			Set<String> appModesKeys = Algorithms.decodeStringSet(json.getString(getId()), ",");
			Set<String> nonexistentAppModesKeys = new HashSet<>();
			for (String appModeKey : appModesKeys) {
				if (ApplicationMode.valueOfStringKey(appModeKey, null) == null) {
					nonexistentAppModesKeys.add(appModeKey);
				}
			}
			if (!nonexistentAppModesKeys.isEmpty()) {
				appModesKeys.removeAll(nonexistentAppModesKeys);
			}
			set(parseString(Algorithms.encodeCollection(appModesKeys, ",")));
		}

	}.makeGlobal().makeShared().cache();

	public final OsmandPreference<String> LAST_FAV_CATEGORY_ENTERED = new StringPreference(this, "last_fav_category", "").makeGlobal();

	public final OsmandPreference<Boolean> USE_LAST_APPLICATION_MODE_BY_DEFAULT = new BooleanPreference(this, "use_last_application_mode_by_default", false).makeGlobal().makeShared();

	public final OsmandPreference<String> LAST_USED_APPLICATION_MODE = new StringPreference(this, "last_used_application_mode", ApplicationMode.DEFAULT.getStringKey()).makeGlobal().makeShared();

	public final OsmandPreference<ApplicationMode> DEFAULT_APPLICATION_MODE = new CommonPreference<ApplicationMode>(this, "default_application_mode_string", ApplicationMode.DEFAULT) {

		@Override
		public ApplicationMode getValue(@NonNull Object prefs, ApplicationMode defaultValue) {
			String key;
			if (USE_LAST_APPLICATION_MODE_BY_DEFAULT.get()) {
				key = LAST_USED_APPLICATION_MODE.get();
			} else {
				key = settingsAPI.getString(prefs, getId(), defaultValue.getStringKey());
			}
			return ApplicationMode.valueOfStringKey(key, defaultValue);
		}

		@Override
		protected boolean setValue(Object prefs, ApplicationMode val) {
			boolean valueSaved = super.setValue(prefs, val)
					&& settingsAPI.edit(prefs).putString(getId(), val.getStringKey()).commit();
			if (valueSaved) {
				setApplicationMode(val);
			}

			return valueSaved;
		}

		@Override
		public boolean writeToJson(JSONObject json, ApplicationMode appMode) throws JSONException {
			if (appMode == null) {
				return writeAppModeToJson(json, this);
			} else {
				return true;
			}
		}

		@Override
		public void readFromJson(JSONObject json, ApplicationMode appMode) throws JSONException {
			if (appMode == null) {
				readAppModeFromJson(json, this);
			}
		}

		@Override
		protected String toString(ApplicationMode o) {
			return appModeToString(o);
		}


		@Override
		public ApplicationMode parseString(String s) {
			return appModeFromString(s);
		}
	}.makeGlobal().makeShared();

	public final OsmandPreference<ApplicationMode> LAST_ROUTE_APPLICATION_MODE = new CommonPreference<ApplicationMode>(this, "last_route_application_mode_backup_string", ApplicationMode.DEFAULT) {

		@Override
		public ApplicationMode getValue(@NonNull Object prefs, ApplicationMode defaultValue) {
			String key = settingsAPI.getString(prefs, getId(), defaultValue.getStringKey());
			return ApplicationMode.valueOfStringKey(key, defaultValue);
		}

		@Override
		protected boolean setValue(Object prefs, ApplicationMode val) {
			return super.setValue(prefs, val)
					&& settingsAPI.edit(prefs).putString(getId(), val.getStringKey()).commit();
		}

		@Override
		public boolean writeToJson(JSONObject json, ApplicationMode appMode) throws JSONException {
			if (appMode == null) {
				return writeAppModeToJson(json, this);
			} else {
				return true;
			}
		}

		@Override
		public void readFromJson(JSONObject json, ApplicationMode appMode) throws JSONException {
			if (appMode == null) {
				readAppModeFromJson(json, this);
			}
		}

		@Override
		protected String toString(ApplicationMode o) {
			return appModeToString(o);
		}


		@Override
		public ApplicationMode parseString(String s) {
			return appModeFromString(s);
		}
	}.makeGlobal();

	public final OsmandPreference<Boolean> FIRST_MAP_IS_DOWNLOADED = new BooleanPreference(this, "first_map_is_downloaded", false);

	public final CommonPreference<Boolean> DRIVING_REGION_AUTOMATIC = new BooleanPreference(this, "driving_region_automatic", true).makeProfile().cache();
	public final OsmandPreference<DrivingRegion> DRIVING_REGION = new EnumStringPreference<DrivingRegion>(this,
			"default_driving_region", DrivingRegion.EUROPE_ASIA, DrivingRegion.values()) {

		public DrivingRegion getDefaultValue() {
			return DrivingRegion.getDrivingRegionByLocale();
		}

		@Override
		public DrivingRegion getProfileDefaultValue(ApplicationMode mode) {
			return DrivingRegion.getDrivingRegionByLocale();
		}

	}.makeProfile().cache();

	// cache of metrics constants as they are used very often
	public final EnumStringPreference<MetricsConstants> METRIC_SYSTEM = (EnumStringPreference<MetricsConstants>) new EnumStringPreference<MetricsConstants>(this,
			"default_metric_system", MetricsConstants.KILOMETERS_AND_METERS, MetricsConstants.values()) {

		public MetricsConstants getDefaultValue() {
			return DRIVING_REGION.get().defMetrics;
		}

		@Override
		public MetricsConstants getProfileDefaultValue(ApplicationMode mode) {
			return DRIVING_REGION.getModeValue(mode).defMetrics;
		}
	}.makeProfile();

	public final EnumStringPreference<AltitudeMetrics> ALTITUDE_METRIC = (EnumStringPreference<AltitudeMetrics>) new EnumStringPreference<AltitudeMetrics>(this,
			"altitude_metrics", AltitudeMetrics.METERS, AltitudeMetrics.values()) {

		@Override
		public AltitudeMetrics getProfileDefaultValue(ApplicationMode mode) {
			MetricsConstants mc = METRIC_SYSTEM.getModeValue(mode);
			return AltitudeMetrics.Companion.fromMetricsConstant(mc);
		}
	}.makeProfile();

	//public final OsmandPreference<Integer> COORDINATES_FORMAT = new IntPreference("coordinates_format", PointDescription.FORMAT_DEGREES).makeGlobal();

	public final OsmandPreference<AngularConstants> ANGULAR_UNITS = new EnumStringPreference<AngularConstants>(this,
			"angular_measurement", AngularConstants.DEGREES, AngularConstants.values()).makeProfile();

	public static final String LAST_START_LAT = "last_searched_lat";
	public static final String LAST_START_LON = "last_searched_lon";

	public LatLon getLastStartPoint() {
		if (settingsAPI.contains(globalPreferences, LAST_START_LAT) && settingsAPI.contains(globalPreferences, LAST_START_LON)) {
			return new LatLon(settingsAPI.getFloat(globalPreferences, LAST_START_LAT, 0),
					settingsAPI.getFloat(globalPreferences, LAST_START_LON, 0));
		}
		return null;
	}

	public boolean setLastStartPoint(LatLon l) {
		if (l == null) {
			return settingsAPI.edit(globalPreferences).remove(LAST_START_LAT).remove(LAST_START_LON).commit();
		} else {
			return setLastStartPoint(l.getLatitude(), l.getLongitude());
		}
	}

	public boolean setLastStartPoint(double lat, double lon) {
		return settingsAPI.edit(globalPreferences).putFloat(LAST_START_LAT, (float) lat).
				putFloat(LAST_START_LON, (float) lon).commit();
	}

	public final OsmandPreference<SpeedConstants> SPEED_SYSTEM = new EnumStringPreference<SpeedConstants>(this,
			"default_speed_system", SpeedConstants.KILOMETERS_PER_HOUR, SpeedConstants.values()) {

		@Override
		public SpeedConstants getProfileDefaultValue(ApplicationMode mode) {
			MetricsConstants mc = METRIC_SYSTEM.getModeValue(mode);
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
			if (mc == MetricsConstants.NAUTICAL_MILES_AND_METERS || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
				return SpeedConstants.NAUTICALMILES_PER_HOUR;
			} else if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
				return SpeedConstants.KILOMETERS_PER_HOUR;
			} else {
				return SpeedConstants.MILES_PER_HOUR;
			}
		}
	}.makeProfile();

	public final OsmandPreference<VolumeUnit> UNIT_OF_VOLUME = new EnumStringPreference<>(this,
			"unit_of_volume", VolumeUnit.LITRES, VolumeUnit.values()) {

		@Override
		public VolumeUnit getDefaultValue() {
			return DRIVING_REGION.get().volumeUnit;
		}

		@Override
		public VolumeUnit getProfileDefaultValue(ApplicationMode mode) {
			return DRIVING_REGION.getModeValue(mode).volumeUnit;
		}

	}.makeProfile();

	public final OsmandPreference<TemperatureUnitsMode> UNIT_OF_TEMPERATURE = new EnumStringPreference<>(this,
			"unit_of_temperature", TemperatureUnitsMode.SYSTEM_DEFAULT, TemperatureUnitsMode.values()).makeProfile();

	@NonNull
	public TemperatureUnit getTemperatureUnit() {
		return getTemperatureUnit(getApplicationMode());
	}

	@NonNull
	public TemperatureUnit getTemperatureUnit(@NonNull ApplicationMode appMode) {
		TemperatureUnitsMode unitsMode = UNIT_OF_TEMPERATURE.getModeValue(appMode);
		if (unitsMode == TemperatureUnitsMode.SYSTEM_DEFAULT) {
			try {
				String unit = LocalePreferences.getTemperatureUnit();
				boolean fahrenheit = Algorithms.stringsEqual(unit, LocalePreferences.TemperatureUnit.FAHRENHEIT);
				return fahrenheit ? TemperatureUnit.FAHRENHEIT : TemperatureUnit.CELSIUS;
			} catch (IllegalArgumentException e) {
				LOG.error(e);
				return TemperatureUnit.CELSIUS;
			}
		}
		return unitsMode.getTemperatureUnit();
	}

	// fuel tank capacity stored in litres
	public final OsmandPreference<Float> FUEL_TANK_CAPACITY = new FloatPreference(this,
			"fuel_tank_capacity", OBDDataComputer.DEFAULT_FUEL_TANK_CAPACITY).makeProfile();


	// cache of metrics constants as they are used very often
	public final OsmandPreference<RelativeDirectionStyle> DIRECTION_STYLE = new EnumStringPreference<RelativeDirectionStyle>(this,
			"direction_style", RelativeDirectionStyle.SIDEWISE, RelativeDirectionStyle.values()).makeProfile().cache();

	// cache of metrics constants as they are used very often
	public final OsmandPreference<AccessibilityMode> ACCESSIBILITY_MODE = new EnumStringPreference<AccessibilityMode>(this,
			"accessibility_mode", AccessibilityMode.DEFAULT, AccessibilityMode.values()).makeProfile().cache();

	public final OsmandPreference<Float> SPEECH_RATE =
			new FloatPreference(this, "speech_rate", 1f).makeProfile();

	public final OsmandPreference<Float> ARRIVAL_DISTANCE_FACTOR =
			new FloatPreference(this, "arrival_distance_factor", 1f).makeProfile();

	public final OsmandPreference<Float> SPEED_LIMIT_EXCEED_KMH =
			new FloatPreference(this, "speed_limit_exceed", 5f).makeProfile();

	public final CommonPreference<Float> DEFAULT_SPEED = new FloatPreference(this, "default_speed", 10f).makeProfile().cache();

	{
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.DEFAULT, 1.5f);
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.CAR, 12.5f);
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.BICYCLE, 2.77f);
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 1.11f);
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.BOAT, 1.38f);
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.AIRCRAFT, 200f);
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.SKI, 1.38f);
		DEFAULT_SPEED.setModeDefaultValue(ApplicationMode.HORSE, 1.66f);
	}

	public final OsmandPreference<Float> MIN_SPEED = new FloatPreference(this,
			"min_speed", 0f).makeProfile().cache();

	public final OsmandPreference<Float> MAX_SPEED = new FloatPreference(this,
			"max_speed", 0f).makeProfile().cache();

	public final CommonPreference<String> ICON_RES_NAME = new StringPreference(this, "app_mode_icon_res_name", "ic_world_globe_dark") {
		@Override
		public String getModeValue(ApplicationMode mode) {
			String iconResName = super.getModeValue(mode);
			if (AndroidUtils.getDrawableId(getContext(), iconResName) != 0) {
				return iconResName;
			}
			return getProfileDefaultValue(mode);
		}
	}.makeProfile().cache();

	{
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.DEFAULT, "ic_world_globe_dark");
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.CAR, "ic_action_car_dark");
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.BICYCLE, "ic_action_bicycle_dark");
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.PEDESTRIAN, "ic_action_pedestrian_dark");
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.PUBLIC_TRANSPORT, "ic_action_bus_dark");
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.MOPED, "ic_action_motor_scooter");
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.BOAT, "ic_action_sail_boat_dark");
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.AIRCRAFT, "ic_action_aircraft");
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.SKI, "ic_action_skiing");
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.TRUCK, "ic_action_truck_dark");
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.MOTORCYCLE, "ic_action_motorcycle_dark");
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.HORSE, "ic_action_horse");
		ICON_RES_NAME.setModeDefaultValue(ApplicationMode.TRAIN, "ic_action_train");
	}

	public final CommonPreference<ProfileIconColors> ICON_COLOR = new EnumStringPreference<>(this,
			"app_mode_icon_color", ProfileIconColors.DEFAULT, ProfileIconColors.values()).makeProfile().cache();

	public final CommonPreference<String> CUSTOM_ICON_COLOR = new StringPreference(this, "custom_icon_color", null).makeProfile().cache();

	public final CommonPreference<String> USER_PROFILE_NAME = new StringPreference(this, "user_profile_name", "").makeProfile().cache();

	public final CommonPreference<String> PARENT_APP_MODE = new StringPreference(this, "parent_app_mode", null).makeProfile().cache();

	public final CommonPreference<String> DERIVED_PROFILE = new StringPreference(this, "derived_profile", "default").makeProfile().cache();

	{
		DERIVED_PROFILE.setModeDefaultValue(ApplicationMode.MOTORCYCLE, "motorcycle");
		DERIVED_PROFILE.setModeDefaultValue(ApplicationMode.TRUCK, "truck");
	}

	public final CommonPreference<String> ROUTING_PROFILE = new StringPreference(this, "routing_profile", "").makeProfile().cache();

	{
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.CAR, "car");
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.BICYCLE, "bicycle");
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, "pedestrian");
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.PUBLIC_TRANSPORT, PUBLIC_TRANSPORT_KEY);
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.BOAT, "boat");
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.AIRCRAFT, "STRAIGHT_LINE_MODE");
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.SKI, "ski");
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.HORSE, "horsebackriding");
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.MOPED, "moped");
		ROUTING_PROFILE.setModeDefaultValue(ApplicationMode.TRAIN, "train");
	}

	public final CommonPreference<RouteService> ROUTE_SERVICE = new EnumStringPreference<RouteService>(this, "route_service", RouteService.OSMAND, RouteService.values()) {
		@Override
		public RouteService getModeValue(ApplicationMode mode) {
			if (mode == ApplicationMode.DEFAULT) {
				return RouteService.STRAIGHT;
			} else {
				return super.getModeValue(mode);
			}
		}
	}.makeProfile().cache();

	{
		ROUTE_SERVICE.setModeDefaultValue(ApplicationMode.DEFAULT, RouteService.STRAIGHT);
		ROUTE_SERVICE.setModeDefaultValue(ApplicationMode.AIRCRAFT, RouteService.STRAIGHT);
	}

	public final CommonPreference<String> ONLINE_ROUTING_ENGINES = new StringPreference(this, "online_routing_engines", null).makeGlobal().makeShared().storeLastModifiedTime();

	public final CommonPreference<String> NAVIGATION_ICON = new StringPreference(this, "navigation_icon", LocationIcon.MOVEMENT_DEFAULT.name()).makeProfile().cache();

	{
		NAVIGATION_ICON.setModeDefaultValue(ApplicationMode.DEFAULT, LocationIcon.MOVEMENT_DEFAULT.name());
		NAVIGATION_ICON.setModeDefaultValue(ApplicationMode.CAR, LocationIcon.MOVEMENT_DEFAULT.name());
		NAVIGATION_ICON.setModeDefaultValue(ApplicationMode.BICYCLE, LocationIcon.MOVEMENT_DEFAULT.name());
		NAVIGATION_ICON.setModeDefaultValue(ApplicationMode.BOAT, LocationIcon.MOVEMENT_NAUTICAL.name());
		NAVIGATION_ICON.setModeDefaultValue(ApplicationMode.AIRCRAFT, LocationIcon.MOVEMENT_DEFAULT.name());
		NAVIGATION_ICON.setModeDefaultValue(ApplicationMode.SKI, LocationIcon.MOVEMENT_DEFAULT.name());
		NAVIGATION_ICON.setModeDefaultValue(ApplicationMode.HORSE, LocationIcon.MOVEMENT_DEFAULT.name());
	}

	public final CommonPreference<String> LOCATION_ICON = new StringPreference(this, "location_icon", LocationIcon.STATIC_DEFAULT.name()).makeProfile().cache();

	{
		LOCATION_ICON.setModeDefaultValue(ApplicationMode.DEFAULT, LocationIcon.STATIC_DEFAULT.name());
		LOCATION_ICON.setModeDefaultValue(ApplicationMode.CAR, LocationIcon.STATIC_CAR.name());
		LOCATION_ICON.setModeDefaultValue(ApplicationMode.BICYCLE, LocationIcon.STATIC_BICYCLE.name());
		LOCATION_ICON.setModeDefaultValue(ApplicationMode.BOAT, LocationIcon.STATIC_DEFAULT.name());
		LOCATION_ICON.setModeDefaultValue(ApplicationMode.AIRCRAFT, LocationIcon.STATIC_CAR.name());
		LOCATION_ICON.setModeDefaultValue(ApplicationMode.SKI, LocationIcon.STATIC_BICYCLE.name());
		LOCATION_ICON.setModeDefaultValue(ApplicationMode.HORSE, LocationIcon.STATIC_BICYCLE.name());
	}

	public final CommonPreference<Integer> APP_MODE_ORDER = new IntPreference(this, "app_mode_order", 0).makeProfile().cache();

	{
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.DEFAULT, 0);
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.CAR, 1);
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.BICYCLE, 2);
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 3);
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.TRUCK, 4);
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.MOTORCYCLE, 5);
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.MOPED, 6);
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.PUBLIC_TRANSPORT, 7);
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.TRAIN, 8);
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.BOAT, 9);
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.AIRCRAFT, 10);
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.SKI, 11);
		APP_MODE_ORDER.setModeDefaultValue(ApplicationMode.HORSE, 12);
	}

	public final CommonPreference<Integer> APP_MODE_VERSION = new IntPreference(this, "app_mode_version", 0).makeProfile().cache();

	public final OsmandPreference<Boolean> USE_TRACKBALL_FOR_MOVEMENTS =
			new BooleanPreference(this, "use_trackball_for_movements", true).makeProfile();

	public final OsmandPreference<Boolean> ACCESSIBILITY_SMART_AUTOANNOUNCE =
			new BooleanAccessibilityPreference(this, "accessibility_smart_autoannounce", true).makeProfile();

	public final OsmandPreference<Boolean> ACCESSIBILITY_PINCH_ZOOM_MAGNIFICATION =
			new BooleanPreference(this, "accessibility_pinch_zoom_magnification", false).makeProfile();

	// cache of metrics constants as they are used very often
	public final OsmandPreference<Integer> ACCESSIBILITY_AUTOANNOUNCE_PERIOD = new IntPreference(this, "accessibility_autoannounce_period", 10000).makeProfile().cache();

	public final OsmandPreference<Boolean> DISABLE_OFFROUTE_RECALC =
			new BooleanPreference(this, "disable_offroute_recalc", false).makeProfile();

	public final OsmandPreference<Boolean> DISABLE_WRONG_DIRECTION_RECALC =
			new BooleanPreference(this, "disable_wrong_direction_recalc", false).makeProfile();

	public final OsmandPreference<Boolean> HAZMAT_TRANSPORTING_ENABLED =
			new BooleanPreference(this, "hazmat_transporting_enabled", false).makeProfile();

	public final OsmandPreference<Boolean> DIRECTION_AUDIO_FEEDBACK =
			new BooleanAccessibilityPreference(this, "direction_audio_feedback", false).makeProfile();

	public final OsmandPreference<Boolean> DIRECTION_HAPTIC_FEEDBACK =
			new BooleanAccessibilityPreference(this, "direction_haptic_feedback", false).makeProfile();

	// magnetic field doesn'torkmost of the time on some phones
	public final OsmandPreference<Boolean> USE_MAGNETIC_FIELD_SENSOR_COMPASS = new BooleanPreference(this, "use_magnetic_field_sensor_compass", false).makeProfile().cache();
	public final OsmandPreference<Boolean> USE_KALMAN_FILTER_FOR_COMPASS = new BooleanPreference(this, "use_kalman_filter_compass", true).makeProfile().cache();
	public final OsmandPreference<Boolean> USE_VOLUME_BUTTONS_AS_ZOOM = new BooleanPreference(this, "use_volume_buttons_as_zoom", false).makeProfile().cache();

	public final CommonPreference<Boolean> PRECISE_DISTANCE_NUMBERS = new BooleanPreference(this, "precise_distance_numbers", true).makeProfile().cache();

	{
		PRECISE_DISTANCE_NUMBERS.setModeDefaultValue(ApplicationMode.CAR, false);
	}

	public final OsmandPreference<Boolean> DO_NOT_SHOW_STARTUP_MESSAGES = new BooleanPreference(this, "do_not_show_startup_messages", false).makeGlobal().makeShared().cache();
	public final OsmandPreference<Boolean> SHOW_SUGGEST_MAP_DIALOG = new BooleanPreference(this, "show_download_map_dialog", true).makeGlobal().makeShared().cache();
	public final OsmandPreference<Boolean> DO_NOT_USE_ANIMATIONS = new BooleanPreference(this, "do_not_use_animations", false).makeProfile().cache();

	public final OsmandPreference<Boolean> SEND_ANONYMOUS_MAP_DOWNLOADS_DATA = new BooleanPreference(this, "send_anonymous_map_downloads_data", false).makeGlobal().makeShared().cache();
	public final OsmandPreference<Boolean> SEND_ANONYMOUS_APP_USAGE_DATA = new BooleanPreference(this, "send_anonymous_app_usage_data", false).makeGlobal().makeShared().cache();
	public final OsmandPreference<Boolean> SEND_ANONYMOUS_DATA_REQUEST_PROCESSED = new BooleanPreference(this, "send_anonymous_data_request_processed", false).makeGlobal().makeShared().cache();
	public final OsmandPreference<Integer> SEND_ANONYMOUS_DATA_REQUESTS_COUNT = new IntPreference(this, "send_anonymous_data_requests_count", 0).makeGlobal().cache();
	public final OsmandPreference<Integer> SEND_ANONYMOUS_DATA_LAST_REQUEST_NS = new IntPreference(this, "send_anonymous_data_last_request_ns", -1).makeGlobal().cache();

	public final OsmandPreference<Boolean> SEND_UNIQUE_USER_IDENTIFIER = new BooleanPreference(this, "send_unique_user_identifier", true).makeGlobal().cache();

	public final CommonPreference<LocationSource> LOCATION_SOURCE = new EnumStringPreference<>(this, "location_source",
			Version.isGooglePlayEnabled() ? GOOGLE_PLAY_SERVICES : ANDROID_API, LocationSource.values()).makeGlobal().makeShared();

	public final OsmandPreference<Boolean> MAP_EMPTY_STATE_ALLOWED = new BooleanPreference(this, "map_empty_state_allowed", false).makeProfile().cache();

	public final OsmandPreference<Boolean> FIXED_NORTH_MAP = new BooleanPreference(this, "fix_north_map", false).makeProfile().cache();


	public final CommonPreference<Float> TEXT_SCALE = new FloatPreference(this, "text_scale", 1f).makeProfile().cache();

	{
		TEXT_SCALE.setModeDefaultValue(ApplicationMode.CAR, 1.25f);
	}

	public final CommonPreference<Float> MAP_DENSITY = new FloatPreference(this, "map_density_n", 1f).makeProfile().cache();

	public final OsmandPreference<Boolean> SHOW_POI_LABEL = new BooleanPreference(this, "show_poi_label", false).makeProfile();

	public final OsmandPreference<Boolean> ONLINE_PHOTOS_ROW_COLLAPSED = new BooleanPreference(this, "online_photos_menu_collapsed", true).makeGlobal().makeShared();
	public final OsmandPreference<Boolean> EXPLORE_NEARBY_ITEMS_ROW_COLLAPSED = new BooleanPreference(this, "online_photos_menu_collapsed", true).makeGlobal().makeShared();
	public final OsmandPreference<Boolean> WEBGL_SUPPORTED = new BooleanPreference(this, "webgl_supported", true).makeGlobal();

	public final OsmandPreference<String> PREFERRED_LOCALE = new StringPreference(this, "preferred_locale", "").makeGlobal().makeShared();

	public final OsmandPreference<String> MAP_PREFERRED_LOCALE = new StringPreference(this, "map_preferred_locale", "").makeProfile().cache();
	public final OsmandPreference<Boolean> MAP_TRANSLITERATE_NAMES = new BooleanPreference(this, "map_transliterate_names", false) {

		public Boolean getDefaultValue() {
			return usingEnglishNames();
		}

	}.makeGlobal().makeShared().cache();

	public final OsmandPreference<Boolean> MAP_SHOW_LOCAL_NAMES = new BooleanPreference(this, "map_show_local_names", false).makeGlobal().makeShared().cache();

	public boolean usingEnglishNames() {
		return MAP_PREFERRED_LOCALE.get().equals("en");
	}

	public static final String BILLING_USER_DONATION_WORLD_PARAMETER = "";
	public static final String BILLING_USER_DONATION_NONE_PARAMETER = "none";

	public final OsmandPreference<Boolean> INAPPS_READ = new BooleanPreference(this, "inapps_read", false).makeGlobal();

	public final OsmandPreference<String> BILLING_USER_ID = new StringPreference(this, "billing_user_id", "").makeGlobal();
	public final OsmandPreference<String> BILLING_USER_TOKEN = new StringPreference(this, "billing_user_token", "").makeGlobal();
	public final OsmandPreference<String> BILLING_USER_NAME = new StringPreference(this, "billing_user_name", "").makeGlobal();
	public final OsmandPreference<String> BILLING_USER_EMAIL = new StringPreference(this, "billing_user_email", "").makeGlobal();
	public final OsmandPreference<String> BILLING_USER_COUNTRY = new StringPreference(this, "billing_user_country", "").makeGlobal();
	public final OsmandPreference<String> BILLING_USER_COUNTRY_DOWNLOAD_NAME = new StringPreference(this, "billing_user_country_download_name", BILLING_USER_DONATION_NONE_PARAMETER).makeGlobal();
	public final OsmandPreference<Boolean> BILLING_HIDE_USER_NAME = new BooleanPreference(this, "billing_hide_user_name", false).makeGlobal();
	public final OsmandPreference<Boolean> BILLING_PURCHASE_TOKEN_SENT = new BooleanPreference(this, "billing_purchase_token_sent", false).makeGlobal();
	public final OsmandPreference<String> BILLING_PURCHASE_TOKENS_SENT = new StringPreference(this, "billing_purchase_tokens_sent", "").makeGlobal();
	public final OsmandPreference<Boolean> LIVE_UPDATES_PURCHASED = new BooleanPreference(this, "billing_live_updates_purchased", false).makeGlobal();
	public final OsmandPreference<Long> LIVE_UPDATES_EXPIRED_FIRST_DLG_SHOWN_TIME = new LongPreference(this, "live_updates_expired_first_dlg_shown_time", 0).makeGlobal();
	public final OsmandPreference<Long> LIVE_UPDATES_EXPIRED_SECOND_DLG_SHOWN_TIME = new LongPreference(this, "live_updates_expired_second_dlg_shown_time", 0).makeGlobal();
	public final OsmandPreference<Boolean> FULL_VERSION_PURCHASED = new BooleanPreference(this, "billing_full_version_purchased", false).makeGlobal();
	public final OsmandPreference<Boolean> DEPTH_CONTOURS_PURCHASED = new BooleanPreference(this, "billing_sea_depth_purchased", false).makeGlobal();
	public final OsmandPreference<Boolean> CONTOUR_LINES_PURCHASED = new BooleanPreference(this, "billing_srtm_purchased", false).makeGlobal();
	public final OsmandPreference<Boolean> EMAIL_SUBSCRIBED = new BooleanPreference(this, "email_subscribed", false).makeGlobal();
	public final OsmandPreference<Boolean> OSMAND_PRO_PURCHASED = new BooleanPreference(this, "billing_osmand_pro_purchased", false).makeGlobal();
	public final OsmandPreference<Boolean> OSMAND_MAPS_PURCHASED = new BooleanPreference(this, "billing_osmand_maps_purchased", false).makeGlobal();
	public final OsmandPreference<Long> MAPPER_LIVE_UPDATES_EXPIRE_TIME = new LongPreference(this, "mapper_live_updates_expire_time", 0L).makeGlobal();

	public final OsmandPreference<Integer> DISCOUNT_ID = new IntPreference(this, "discount_id", 0).makeGlobal();
	public final OsmandPreference<Integer> DISCOUNT_SHOW_NUMBER_OF_STARTS = new IntPreference(this, "number_of_starts_on_discount_show", 0).makeGlobal();
	public final OsmandPreference<Integer> DISCOUNT_TOTAL_SHOW = new IntPreference(this, "discount_total_show", 0).makeGlobal();
	public final OsmandPreference<Long> DISCOUNT_SHOW_DATETIME_MS = new LongPreference(this, "show_discount_datetime_ms", 0).makeGlobal();

	public final OsmandPreference<String> BACKUP_USER_EMAIL = new StringPreference(this, "backup_user_email", "").makeGlobal();
	public final OsmandPreference<String> BACKUP_USER_ID = new StringPreference(this, "backup_user_id", "").makeGlobal();
	public final OsmandPreference<String> BACKUP_DEVICE_ID = new StringPreference(this, "backup_device_id", "").makeGlobal();
	public final OsmandPreference<String> BACKUP_NATIVE_DEVICE_ID = new StringPreference(this, "backup_native_device_id", "").makeGlobal();
	public final OsmandPreference<String> BACKUP_ACCESS_TOKEN = new StringPreference(this, "backup_access_token", "").makeGlobal();
	public final OsmandPreference<String> BACKUP_ACCESS_TOKEN_UPDATE_TIME = new StringPreference(this, "backup_access_token_update_time", "").makeGlobal();

	public final OsmandPreference<String> BACKUP_PROMOCODE = new StringPreference(this, "backup_promocode", "").makeGlobal();
	public final OsmandPreference<Boolean> BACKUP_PURCHASE_ACTIVE = new BooleanPreference(this, "backup_promocode_active", false).makeGlobal();
	public final OsmandPreference<Long> BACKUP_PURCHASE_START_TIME = new LongPreference(this, "promo_website_start_time", 0L).makeGlobal();
	public final OsmandPreference<Long> BACKUP_PURCHASE_EXPIRE_TIME = new LongPreference(this, "promo_website_expire_time", 0L).makeGlobal();
	public final CommonPreference<SubscriptionState> BACKUP_PURCHASE_STATE = new EnumStringPreference<>(this, "promo_website_state", SubscriptionState.UNDEFINED, SubscriptionState.values()).makeGlobal();
	public final CommonPreference<PurchaseOrigin> BACKUP_SUBSCRIPTION_ORIGIN = new EnumStringPreference<>(this, "backup_subscription_origin", PurchaseOrigin.UNDEFINED, PurchaseOrigin.values()).makeGlobal();
	public final CommonPreference<String> BACKUP_SUBSCRIPTION_SKU = new StringPreference(this, "backup_subscription_sku", null).makeGlobal();
	public final OsmandPreference<Period.PeriodUnit> BACKUP_PURCHASE_PERIOD = new EnumStringPreference<>(this, "backup_purchase_period", null, PeriodUnit.values()).makeGlobal();

	public final OsmandPreference<Long> FAVORITES_LAST_UPLOADED_TIME = new LongPreference(this, "favorites_last_uploaded_time", 0L).makeGlobal();
	public final OsmandPreference<Long> BACKUP_LAST_UPLOADED_TIME = new LongPreference(this, "backup_last_uploaded_time", 0L).makeGlobal();
	public final OsmandPreference<Long> BACKUP_LAST_DOWNLOADED_TIME = new LongPreference(this, "backup_last_downloaded_time", 0L).makeGlobal();
	public final OsmandPreference<String> ITINERARY_LAST_CALCULATED_MD5 = new StringPreference(this, "itinerary_last_calculated_md5", "").makeGlobal();

	public final OsmandPreference<Boolean> AUTO_BACKUP_ENABLED = new BooleanPreference(this, OsmandBackupAgent.AUTO_BACKUP_ENABLED, true).makeGlobal().makeShared();

	public final CommonPreference<DayNightMode> DAYNIGHT_MODE = new EnumStringPreference<>(this, "daynight_mode", DayNightMode.DAY, DayNightMode.values());

	{
		DAYNIGHT_MODE.makeProfile().cache();
		DAYNIGHT_MODE.setModeDefaultValue(ApplicationMode.CAR, DayNightMode.AUTO);
		DAYNIGHT_MODE.setModeDefaultValue(ApplicationMode.BICYCLE, DayNightMode.AUTO);
		DAYNIGHT_MODE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, DayNightMode.DAY);
	}

	public final CommonPreference<Boolean> AUTO_ZOOM_MAP = new BooleanPreference(this, "auto_zoom_map_on_off", false).makeProfile().cache();

	{
		AUTO_ZOOM_MAP.setModeDefaultValue(ApplicationMode.CAR, true);
		AUTO_ZOOM_MAP.setModeDefaultValue(ApplicationMode.BICYCLE, false);
		AUTO_ZOOM_MAP.setModeDefaultValue(ApplicationMode.PEDESTRIAN, false);
	}

	public final CommonPreference<AutoZoomMap> AUTO_ZOOM_MAP_SCALE =
			new EnumStringPreference<AutoZoomMap>(this, "auto_zoom_map_scale", AutoZoomMap.FAR,
					AutoZoomMap.values()).makeProfile().cache();

	{
		AUTO_ZOOM_MAP_SCALE.setModeDefaultValue(ApplicationMode.CAR, AutoZoomMap.FAR);
		AUTO_ZOOM_MAP_SCALE.setModeDefaultValue(ApplicationMode.BICYCLE, AutoZoomMap.CLOSE);
		AUTO_ZOOM_MAP_SCALE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, AutoZoomMap.CLOSE);
	}

	public final CommonPreference<Integer> AUTO_ZOOM_3D_ANGLE = new IntPreference(this, "auto_zoom_3d_angle", 25).makeProfile().cache();

	public final CommonPreference<Integer> DELAY_TO_START_NAVIGATION = new IntPreference(this, "delay_to_start_navigation", -1) {

		public Integer getDefaultValue() {
			if (DEFAULT_APPLICATION_MODE.get().isDerivedRoutingFrom(ApplicationMode.CAR)) {
				return 10;
			}
			return -1;
		}
	}.makeGlobal().makeShared().cache();

	public final CommonPreference<Boolean> SNAP_TO_ROAD = new BooleanPreference(this, "snap_to_road", false).makeProfile().cache();

	{
		SNAP_TO_ROAD.setModeDefaultValue(ApplicationMode.CAR, true);
		SNAP_TO_ROAD.setModeDefaultValue(ApplicationMode.BICYCLE, true);
		SNAP_TO_ROAD.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
	}

	public final CommonPreference<Boolean> PREVIEW_NEXT_TURN = new BooleanPreference(this, "preview_next_turn", true).makeProfile().cache();
	public final CommonPreference<MarkerDisplayOption> VIEW_ANGLE_VISIBILITY = new EnumStringPreference<>(this, "view_angle_visibility", MarkerDisplayOption.RESTING, MarkerDisplayOption.values()).makeProfile().makeShared();
	public final CommonPreference<MarkerDisplayOption> LOCATION_RADIUS_VISIBILITY = new EnumStringPreference<>(this, "location_radius_visibility", MarkerDisplayOption.RESTING_NAVIGATION, MarkerDisplayOption.values()).makeProfile().makeShared();

	public final CommonPreference<Boolean> INTERRUPT_MUSIC = new BooleanPreference(this, "interrupt_music", false).makeProfile();

	public final CommonPreference<Boolean> ENABLE_PROXY = new BooleanPreference(this, "enable_proxy", false) {
		@Override
		protected boolean setValue(Object prefs, Boolean val) {
			boolean valueSaved = super.setValue(prefs, val);
			if (valueSaved) {
				NetworkUtils.setProxy(val ? PROXY_HOST.get() : null, val ? PROXY_PORT.get() : 0);
			}
			return valueSaved;
		}
	}.makeGlobal().makeShared();

	public final CommonPreference<String> PROXY_HOST = new StringPreference(this, "proxy_host", null).makeGlobal().makeShared();
	public final CommonPreference<Integer> PROXY_PORT = new IntPreference(this, "proxy_port", 0).makeGlobal().makeShared();

	public boolean isProxyEnabled() {
		return PROXY_HOST.get() != null && PROXY_PORT.get() > 0 && ENABLE_PROXY.get();
	}

	public final CommonPreference<String> USER_ANDROID_ID = new StringPreference(this, "user_android_id", "").makeGlobal();
	public final CommonPreference<Long> USER_ANDROID_ID_EXPIRED_TIME = new LongPreference(this, "user_android_id_expired_time", 0).makeGlobal();


	public final CommonPreference<Boolean> SAVE_GLOBAL_TRACK_TO_GPX = new BooleanPreference(this, "save_global_track_to_gpx", false).makeGlobal().cache();
	public final CommonPreference<Integer> SAVE_GLOBAL_TRACK_INTERVAL = new IntPreference(this, "save_global_track_interval", 5000).makeProfile().cache();
	public final CommonPreference<Boolean> SAVE_GLOBAL_TRACK_REMEMBER = new BooleanPreference(this, "save_global_track_remember", false).makeProfile().cache();
	public final CommonPreference<Boolean> SHOW_TRIP_REC_START_DIALOG = new BooleanPreference(this, "show_trip_recording_start_dialog", true).makeGlobal().makeShared();
	public final CommonPreference<Boolean> SHOW_BATTERY_OPTIMIZATION_DIALOG = new BooleanPreference(this, "show_battery_optimization_dialog", true).makeGlobal().makeShared();
	public final CommonPreference<Boolean> SAVE_TRACK_TO_GPX = new BooleanPreference(this, "save_track_to_gpx", false).makeProfile().cache();

	{
		SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.CAR, false);
		SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.BICYCLE, false);
		SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.PEDESTRIAN, false);
	}

	public static final Integer REC_DIRECTORY = 0;
	public static final Integer MONTHLY_DIRECTORY = 1;
//	public static final Integer DAILY_DIRECTORY = 2;

	public final CommonPreference<Boolean> DISABLE_RECORDING_ONCE_APP_KILLED = new BooleanPreference(this, "disable_recording_once_app_killed", false).makeProfile();

	public final CommonPreference<Integer> TRACK_STORAGE_DIRECTORY = new IntPreference(this, "track_storage_directory", 0).makeProfile();

	public final OsmandPreference<Boolean> FAST_ROUTE_MODE = new BooleanPreference(this, "fast_route_mode", true).makeProfile();

	public static boolean IGNORE_MISSING_MAPS = false;
	public final CommonPreference<RoutingType> ROUTING_TYPE = new EnumStringPreference<>(this, "routing_method", HH_CPP, RoutingType.values()).makeProfile().cache();
	public final CommonPreference<ApproximationType> APPROXIMATION_TYPE = new EnumStringPreference<>(this, "approximation_method_r49_default", APPROX_GEO_CPP, ApproximationType.values()).makeProfile().cache();

	public final CommonPreference<Boolean> ENABLE_TIME_CONDITIONAL_ROUTING = new BooleanPreference(this, "enable_time_conditional_routing", true).makeProfile();

	public final CommonPreference<Boolean> SHOW_MINOR_TURNS = new BooleanPreference(this, "show_minor_turns", true).makeProfile();

	public boolean simulateNavigation;
	public boolean simulateNavigationStartedFromAdb;
	public String simulateNavigationMode = SimulationMode.PREVIEW.getKey();
	public float simulateNavigationSpeed = SIM_MIN_SPEED;

	public final CommonPreference<Boolean> SHOW_ROUTING_ALARMS = new BooleanPreference(this, "show_routing_alarms", true).makeProfile().cache();

	public final CommonPreference<Boolean> SHOW_TRAFFIC_WARNINGS = new BooleanPreference(this, "show_traffic_warnings", false).makeProfile().cache();

	{
		SHOW_TRAFFIC_WARNINGS.setModeDefaultValue(ApplicationMode.CAR, true);
	}

	public final CommonPreference<Boolean> SHOW_SPEEDOMETER = new BooleanPreference(this, "show_speedometer", false).makeProfile().cache();

	{
		SHOW_SPEEDOMETER.setModeDefaultValue(ApplicationMode.CAR, true);
		SHOW_SPEEDOMETER.setModeDefaultValue(ApplicationMode.TRUCK, true);
		SHOW_SPEEDOMETER.setModeDefaultValue(ApplicationMode.MOTORCYCLE, true);
		SHOW_SPEEDOMETER.setModeDefaultValue(ApplicationMode.MOPED, true);
	}

	public final CommonPreference<WidgetSize> SPEEDOMETER_SIZE = new EnumStringPreference<>(this, "speedometer_size", MEDIUM, WidgetSize.values()).makeProfile();

	{
		SPEEDOMETER_SIZE.setModeDefaultValue(ApplicationMode.CAR, SMALL);
	}

	public final CommonPreference<Boolean> SHOW_SPEED_LIMIT_WARNINGS = new BooleanPreference(this, "show_speed_limit_warnings", false).makeProfile().cache();

	public final CommonPreference<Boolean> SHOW_PEDESTRIAN = new BooleanPreference(this, "show_pedestrian", false).makeProfile().cache();

	{
		SHOW_PEDESTRIAN.setModeDefaultValue(ApplicationMode.CAR, true);
	}

	public final CommonPreference<Boolean> SHOW_TUNNELS = new BooleanPreference(this, "show_tunnels", false).makeProfile().cache();

	{
		SHOW_TUNNELS.setModeDefaultValue(ApplicationMode.CAR, true);
	}

	public final OsmandPreference<Boolean> SHOW_CAMERAS = new BooleanPreference(this, "show_cameras", false).makeProfile().cache();

	public final OsmandPreference<Boolean> SHOW_WPT = new BooleanPreference(this, "show_gpx_wpt", true).makeGlobal().makeShared().cache();
	public final OsmandPreference<Boolean> SHOW_NEARBY_FAVORITES = new BooleanPreference(this, "show_nearby_favorites", false).makeProfile().cache();
	public final OsmandPreference<Boolean> SHOW_NEARBY_POI = new BooleanPreference(this, "show_nearby_poi", false).makeProfile().cache();

	public final OsmandPreference<Boolean> TURN_BY_TURN_DIRECTIONS = new BooleanPreference(this, "turn_by_turn_directions", true).makeProfile().cache();
	public final OsmandPreference<Boolean> SPEAK_STREET_NAMES = new BooleanPreference(this, "speak_street_names", true).makeProfile().cache();
	public final CommonPreference<Boolean> SPEAK_TRAFFIC_WARNINGS = new BooleanPreference(this, "speak_traffic_warnings", true).makeProfile().cache();
	public final CommonPreference<Boolean> SPEAK_PEDESTRIAN = new BooleanPreference(this, "speak_pedestrian", false).makeProfile().cache();

	{
		SPEAK_PEDESTRIAN.setModeDefaultValue(ApplicationMode.CAR, true);
	}

	public final OsmandPreference<Boolean> SPEAK_SPEED_LIMIT = new BooleanPreference(this, "speak_speed_limit", false).makeProfile().cache();
	public final OsmandPreference<Boolean> SPEAK_SPEED_CAMERA = new BooleanPreference(this, "speak_cameras", false).makeProfile().cache();
	public final OsmandPreference<Boolean> SPEAK_TUNNELS = new BooleanPreference(this, "speak_tunnels", false).makeProfile().cache();
	public final OsmandPreference<Boolean> SPEAK_EXIT_NUMBER_NAMES = new BooleanPreference(this, "exit_number_names", true).makeProfile().cache();
	public final OsmandPreference<Boolean> SPEAK_ROUTE_RECALCULATION = new BooleanPreference(this, "speak_route_recalculation", true).makeProfile().cache();
	public final OsmandPreference<Boolean> SPEAK_GPS_SIGNAL_STATUS = new BooleanPreference(this, "speak_gps_signal_status", true).makeProfile().cache();
	public final OsmandPreference<Boolean> SPEAK_ROUTE_DEVIATION = new BooleanPreference(this, "speak_route_deviation", true).makeProfile().cache();

	public final OsmandPreference<Boolean> SPEED_CAMERAS_UNINSTALLED = new BooleanPreference(this, "speed_cameras_uninstalled", false).makeGlobal().makeShared();
	public final OsmandPreference<Boolean> SPEED_CAMERAS_ALERT_SHOWED = new BooleanPreference(this, "speed_cameras_alert_showed", false).makeGlobal().makeShared();

	public Set<String> getForbiddenTypes() {
		Set<String> typeNames = new HashSet<>();
		if (SPEED_CAMERAS_UNINSTALLED.get()) {
			typeNames.add(MapPoiTypes.SPEED_CAMERA);
		}
		return typeNames;
	}

	public final OsmandPreference<Boolean> ANNOUNCE_WPT = new BooleanPreference(this, "announce_wpt", true).makeProfile().cache();

	public final OsmandPreference<Boolean> ANNOUNCE_NEARBY_FAVORITES = new BooleanPreference(this, "announce_nearby_favorites", false).makeProfile().cache();

	public final OsmandPreference<Boolean> ANNOUNCE_NEARBY_POI = new BooleanPreference(this, "announce_nearby_poi", true).makeProfile().cache();

	public final OsmandPreference<Boolean> GPX_ROUTE_CALC_OSMAND_PARTS = new BooleanPreference(this, "gpx_routing_calculate_osmand_route", true).makeGlobal().makeShared().cache();
	public final OsmandPreference<Boolean> GPX_CALCULATE_RTEPT = new BooleanPreference(this, "gpx_routing_calculate_rtept", true).makeGlobal().makeShared().cache();
	public final OsmandPreference<Boolean> GPX_ROUTE_CALC = new BooleanPreference(this, "calc_gpx_route", false).makeGlobal().makeShared().cache();
	public final OsmandPreference<Integer> GPX_SEGMENT_INDEX = new IntPreference(this, "gpx_route_segment", -1).makeGlobal().makeShared().cache();
	public final OsmandPreference<Integer> GPX_ROUTE_INDEX = new IntPreference(this, "gpx_route_index", -1).makeGlobal().makeShared().cache();
	public final OsmandPreference<Boolean> GPX_PASS_WHOLE_ROUTE = new BooleanPreference(this, "gpx_pass_whole_route", false).makeGlobal().makeShared().cache();
	public final OsmandPreference<ReverseTrackStrategy> GPX_REVERSE_STRATEGY =
			new EnumStringPreference<>(this, "gpx_reverse_strategy", ReverseTrackStrategy.RECALCULATE_ALL_ROUTE_POINTS, ReverseTrackStrategy.values()).makeGlobal().makeShared().cache();

	public final OsmandPreference<Boolean> AVOID_TOLL_ROADS = new BooleanPreference(this, "avoid_toll_roads", false).makeProfile().cache();
	public final OsmandPreference<Boolean> AVOID_MOTORWAY = new BooleanPreference(this, "avoid_motorway", false).makeProfile().cache();
	public final OsmandPreference<Boolean> AVOID_UNPAVED_ROADS = new BooleanPreference(this, "avoid_unpaved_roads", false).makeProfile().cache();
	public final OsmandPreference<Boolean> AVOID_FERRIES = new BooleanPreference(this, "avoid_ferries", false).makeProfile().cache();

	public final OsmandPreference<Boolean> PREFER_MOTORWAYS = new BooleanPreference(this, "prefer_motorways", false).makeProfile().cache();

	public final OsmandPreference<Long> LAST_UPDATES_CARD_REFRESH = new LongPreference(this, "last_updates_card_refresh", 0).makeGlobal();
	public final CommonPreference<Integer> CURRENT_TRACK_COLOR = new IntPreference(this, "current_track_color", 0).makeGlobal().makeShared().cache();
	public final CommonPreference<ColoringType> CURRENT_TRACK_COLORING_TYPE = new EnumStringPreference<>(this,
			"current_track_coloring_type", ColoringType.TRACK_SOLID, ColoringType.Companion.valuesOf(ColoringPurpose.TRACK)).makeGlobal().makeShared().cache();
	public final CommonPreference<String> CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE = new StringPreference(this,
			"current_track_route_info_attribute", null);
	public final CommonPreference<String> CURRENT_TRACK_WIDTH = new StringPreference(this, "current_track_width", "").makeGlobal().makeShared().cache();
	public final CommonPreference<Boolean> CURRENT_TRACK_SHOW_ARROWS = new BooleanPreference(this, "current_track_show_arrows", false).makeGlobal().makeShared().cache();
	public final CommonPreference<Boolean> CURRENT_TRACK_SHOW_START_FINISH = new BooleanPreference(this, "current_track_show_start_finish", true).makeGlobal().makeShared().cache();
	public final CommonPreference<String> CURRENT_TRACK_3D_VISUALIZATION_TYPE = new StringPreference(this, "currentTrackVisualization3dByType", "none").makeGlobal().makeShared().cache();
	public final CommonPreference<String> CURRENT_TRACK_3D_WALL_COLORING_TYPE = new StringPreference(this, "currentTrackVisualization3dWallColorType", "none").makeGlobal().makeShared().cache();
	public final CommonPreference<String> CURRENT_TRACK_3D_LINE_POSITION_TYPE = new StringPreference(this, "currentTrackVisualization3dPositionType", "none").makeGlobal().makeShared().cache();
	public final CommonPreference<Float> CURRENT_TRACK_ADDITIONAL_EXAGGERATION = new FloatPreference(this, "currentTrackVerticalExaggerationScale", 1f).makeGlobal().makeShared().cache();
	public final CommonPreference<Float> CURRENT_TRACK_ELEVATION_METERS = new FloatPreference(this, "current_track_elevation_meters", 1000f).makeGlobal().makeShared().cache();
	public final CommonPreference<String> CURRENT_GRADIENT_PALETTE = new StringPreference(this, "current_track_gradient_palette", PaletteGradientColor.DEFAULT_NAME).makeGlobal().makeShared().cache();
	public final CommonPreference<String> CURRENT_TRACK_ROUTE_ACTIVITY = new StringPreference(this, "current_track_route_activity", "").makeProfile().cache();

	{
		CURRENT_TRACK_ROUTE_ACTIVITY.setModeDefaultValue(ApplicationMode.BICYCLE, "road_cycling");
		CURRENT_TRACK_ROUTE_ACTIVITY.setModeDefaultValue(ApplicationMode.BOAT, "motorboat");
		CURRENT_TRACK_ROUTE_ACTIVITY.setModeDefaultValue(ApplicationMode.CAR, "car");
		CURRENT_TRACK_ROUTE_ACTIVITY.setModeDefaultValue(ApplicationMode.HORSE, "horse_riding");
		CURRENT_TRACK_ROUTE_ACTIVITY.setModeDefaultValue(ApplicationMode.MOPED, "motor_scooter");
		CURRENT_TRACK_ROUTE_ACTIVITY.setModeDefaultValue(ApplicationMode.PEDESTRIAN, "walking");
		CURRENT_TRACK_ROUTE_ACTIVITY.setModeDefaultValue(ApplicationMode.SKI, "skiing");
		CURRENT_TRACK_ROUTE_ACTIVITY.setModeDefaultValue(ApplicationMode.TRAIN, "train_riding");
		CURRENT_TRACK_ROUTE_ACTIVITY.setModeDefaultValue(ApplicationMode.TRUCK, "truck_hgv");
	}

	public final CommonPreference<String> GRADIENT_PALETTES = new StringPreference(this, "gradient_color_palettes", null).makeGlobal().makeShared();
	public final ListStringPreference LAST_USED_FAV_ICONS = (ListStringPreference) new ListStringPreference(this, "last_used_favorite_icons", null, ",").makeShared().makeGlobal();

	public final CommonPreference<Integer> SAVE_TRACK_INTERVAL = new IntPreference(this, "save_track_interval", 5000).makeProfile();

	{
		SAVE_TRACK_INTERVAL.setModeDefaultValue(ApplicationMode.CAR, 3000);
		SAVE_TRACK_INTERVAL.setModeDefaultValue(ApplicationMode.BICYCLE, 5000);
		SAVE_TRACK_INTERVAL.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 10000);
	}

	// Please note that SAVE_TRACK_MIN_DISTANCE, SAVE_TRACK_PRECISION, SAVE_TRACK_MIN_SPEED should all be "0" for the default profile, as we have no interface to change them
	public final CommonPreference<Float> SAVE_TRACK_MIN_DISTANCE = new FloatPreference(this, "save_track_min_distance", 0).makeProfile();
	//{
	//	SAVE_TRACK_MIN_DISTANCE.setModeDefaultValue(ApplicationMode.CAR, 5.f);
	//	SAVE_TRACK_MIN_DISTANCE.setModeDefaultValue(ApplicationMode.BICYCLE, 5.f);
	//	SAVE_TRACK_MIN_DISTANCE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 5.f);
	//}
	public final CommonPreference<Float> SAVE_TRACK_PRECISION = new FloatPreference(this, "save_track_precision", 50.0f).makeProfile();
	//{
//		SAVE_TRACK_PRECISION.setModeDefaultValue(ApplicationMode.CAR, 50.f);
//		SAVE_TRACK_PRECISION.setModeDefaultValue(ApplicationMode.BICYCLE, 50.f);
//		SAVE_TRACK_PRECISION.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 50.f);
	//}
	public final CommonPreference<Float> SAVE_TRACK_MIN_SPEED = new FloatPreference(this, "save_track_min_speed", 0.f).makeProfile();
	//{
	//	SAVE_TRACK_MIN_SPEED.setModeDefaultValue(ApplicationMode.CAR, 2.f);
	//	SAVE_TRACK_MIN_SPEED.setModeDefaultValue(ApplicationMode.BICYCLE, 1.f);
//		SAVE_TRACK_MIN_SPEED.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 0.f);
	//}
	public final CommonPreference<Boolean> AUTO_SPLIT_RECORDING = new BooleanPreference(this, "auto_split_recording", true).makeProfile();

	public final ListStringPreference TRIP_RECORDING_X_AXIS = (ListStringPreference) new ListStringPreference(this, "trip_recording_x_axis", GPXDataSetType.ALTITUDE.name(), ";").makeShared().makeGlobal();

	public final CommonPreference<GPXDataSetAxisType> TRIP_RECORDING_Y_AXIS = new EnumStringPreference<>(this, "trip_recording_Y_axis", GPXDataSetAxisType.DISTANCE, GPXDataSetAxisType.values());

	public final CommonPreference<Boolean> SHOW_TRIP_REC_NOTIFICATION = new BooleanPreference(this, "show_trip_recording_notification", false).makeProfile();


	public final CommonPreference<Boolean> LIVE_MONITORING = new BooleanPreference(this, "live_monitoring", false).makeProfile();

	public final CommonPreference<Integer> LIVE_MONITORING_INTERVAL = new IntPreference(this, "live_monitoring_interval", 5000).makeProfile();

	public final CommonPreference<Integer> LIVE_MONITORING_MAX_INTERVAL_TO_SEND = new IntPreference(this, "live_monitoring_maximum_interval_to_send", 900000).makeProfile();

	public final CommonPreference<String> LIVE_MONITORING_URL = new StringPreference(this, "live_monitoring_url",
			"https://example.com?lat={0}&lon={1}&timestamp={2}&hdop={3}&altitude={4}&speed={5}").makeProfile();

	public final CommonPreference<String> GPS_STATUS_APP = new StringPreference(this, "gps_status_app", "").makeGlobal().makeShared();

	public final CommonPreference<String> MAP_INFO_CONTROLS = new StringPreference(this, "map_info_controls", "").makeProfile();

	{
		for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
			MAP_INFO_CONTROLS.setModeDefaultValue(mode, "");
		}
	}

	public final OsmandPreference<Boolean> BATTERY_SAVING_MODE = new BooleanPreference(this, "battery_saving", false).makeGlobal().makeShared();

	public final OsmandPreference<Boolean> SIMULATE_OBD_DATA = new BooleanPreference(this, "simulate_obd_data", false).makeGlobal().makeShared();

	public final OsmandPreference<Boolean> DEBUG_RENDERING_INFO = new BooleanPreference(this, "debug_rendering", false).makeGlobal().makeShared();

	public final OsmandPreference<Boolean> DISABLE_MAP_LAYERS = new BooleanPreference(this, "disable_map_layers", false).makeGlobal().makeShared();

	public final OsmandPreference<Boolean> SHOW_FAVORITES = new BooleanPreference(this, "show_favorites", true).makeProfile().cache();

	public final CommonPreference<Boolean> SHOW_ZOOM_BUTTONS_NAVIGATION = new BooleanPreference(this, "show_zoom_buttons_navigation", false).makeProfile().cache();

	{
		SHOW_ZOOM_BUTTONS_NAVIGATION.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
	}

	public final CommonPreference<Integer> MAX_RENDERING_THREADS = new IntPreference(this, "max_rendering_threads", 0).makeGlobal();

	// Json
	public final OsmandPreference<String> SELECTED_GPX = new StringPreference(this, "selected_gpx", "").makeGlobal().makeShared();

	public final OsmandPreference<Integer> MAP_SCREEN_ORIENTATION =
			new IntPreference(this, "map_screen_orientation", -1/*ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED*/).makeProfile();

//	public final CommonPreference<Boolean> SHOW_VIEW_ANGLE = new BooleanPreference("show_view_angle", false).makeProfile().cache();
//	{
//		SHOW_VIEW_ANGLE.setModeDefaultValue(ApplicationMode.CAR, false);
//		SHOW_VIEW_ANGLE.setModeDefaultValue(ApplicationMode.BICYCLE, true);
//		SHOW_VIEW_ANGLE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
//	}

	// seconds to auto_follow
	public final CommonPreference<Integer> AUTO_FOLLOW_ROUTE = new IntPreference(this, "auto_follow_route", 0).makeProfile();

	{
		AUTO_FOLLOW_ROUTE.setModeDefaultValue(ApplicationMode.CAR, 15);
		AUTO_FOLLOW_ROUTE.setModeDefaultValue(ApplicationMode.BICYCLE, 15);
		AUTO_FOLLOW_ROUTE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 0);
	}

	// seconds to auto_follow
	public final CommonPreference<Integer> KEEP_INFORMING = new IntPreference(this, "keep_informing", 0).makeProfile();

	{
		KEEP_INFORMING.setModeDefaultValue(ApplicationMode.CAR, 0);
		KEEP_INFORMING.setModeDefaultValue(ApplicationMode.BICYCLE, 0);
		KEEP_INFORMING.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 0);
	}

	public final CommonPreference<Boolean> USE_SYSTEM_SCREEN_TIMEOUT = new BooleanPreference(this, "use_system_screen_timeout", false).makeProfile();

	public final CommonPreference<Integer> TURN_SCREEN_ON_TIME_INT = new IntPreference(this, "turn_screen_on_time_int", 0).makeProfile();

	{
		TURN_SCREEN_ON_TIME_INT.setModeDefaultValue(ApplicationMode.CAR, 0);
		TURN_SCREEN_ON_TIME_INT.setModeDefaultValue(ApplicationMode.BICYCLE, 0);
		TURN_SCREEN_ON_TIME_INT.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 0);
	}

	public final CommonPreference<Boolean> TURN_SCREEN_ON_SENSOR = new BooleanPreference(this, "turn_screen_on_sensor", false).makeProfile();

	{
		TURN_SCREEN_ON_SENSOR.setModeDefaultValue(ApplicationMode.CAR, false);
		TURN_SCREEN_ON_SENSOR.setModeDefaultValue(ApplicationMode.BICYCLE, false);
		TURN_SCREEN_ON_SENSOR.setModeDefaultValue(ApplicationMode.PEDESTRIAN, false);
	}

	public final CommonPreference<Boolean> TURN_SCREEN_ON_NAVIGATION_INSTRUCTIONS = new BooleanPreference(this, "turn_screen_on_navigation_instructions", false).makeProfile();

	public final CommonPreference<Boolean> TURN_SCREEN_ON_POWER_BUTTON = new BooleanPreference(this, "turn_screen_on_power_button", false).makeProfile();

	// try without AUTO_FOLLOW_ROUTE_NAV (see forum discussion 'Simplify our navigation preference menu')
	//public final CommonPreference<Boolean> AUTO_FOLLOW_ROUTE_NAV = new BooleanPreference("auto_follow_route_navigation", true, false);

	public static final int ROTATE_MAP_NONE = 0;
	public static final int ROTATE_MAP_BEARING = 1;
	public static final int ROTATE_MAP_COMPASS = 2;
	public static final int ROTATE_MAP_MANUAL = 3;

	public final CommonPreference<Integer> ROTATE_MAP = new IntPreference(this, "rotate_map", ROTATE_MAP_MANUAL).makeProfile().cache();

	{
		ROTATE_MAP.setModeDefaultValue(ApplicationMode.CAR, ROTATE_MAP_BEARING);
		ROTATE_MAP.setModeDefaultValue(ApplicationMode.BICYCLE, ROTATE_MAP_BEARING);
		ROTATE_MAP.setModeDefaultValue(ApplicationMode.PEDESTRIAN, ROTATE_MAP_BEARING);
	}

	public boolean isCompassMode(@NonNull CompassMode compassMode) {
		return ROTATE_MAP.get() == compassMode.getValue();
	}

	@NonNull
	public CompassMode getCompassMode() {
		return getCompassMode(getApplicationMode());
	}

	@NonNull
	public CompassMode getCompassMode(@NonNull ApplicationMode appMode) {
		return CompassMode.getByValue(ROTATE_MAP.getModeValue(appMode));
	}

	public void setCompassMode(@NonNull CompassMode compassMode) {
		ROTATE_MAP.set(compassMode.getValue());
	}

	public void setCompassMode(@NonNull CompassMode compassMode, @NonNull ApplicationMode appMode) {
		ROTATE_MAP.setModeValue(appMode, compassMode.getValue());
	}

	public static final int POSITION_PLACEMENT_AUTOMATIC = 0;
	public static final int POSITION_PLACEMENT_CENTER = 1;
	public static final int POSITION_PLACEMENT_BOTTOM = 2;
	public final CommonPreference<Integer> POSITION_PLACEMENT_ON_MAP = new IntPreference(this, "position_placement_on_map", 0) {

		@Override
		public Integer getProfileDefaultValue(ApplicationMode mode) {
			// By default display position shifts to the bottom part of the screen
			// only if the "Map orientation" was set to "Movement direction".
			return 0;
		}
	}.makeProfile();

	public final CommonPreference<Long> LAST_MAP_ACTIVITY_PAUSED_TIME = new LongPreference(this, "last_map_activity_paused_time", 0).makeGlobal().cache();
	public final CommonPreference<Boolean> MAP_LINKED_TO_LOCATION = new BooleanPreference(this, "map_linked_to_location", true).makeGlobal().cache();

	public final OsmandPreference<Integer> MAX_LEVEL_TO_DOWNLOAD_TILE = new IntPreference(this, "max_level_download_tile", 20).makeProfile().cache();

	public final OsmandPreference<Integer> LEVEL_TO_SWITCH_VECTOR_RASTER = new IntPreference(this, "level_to_switch_vector_raster", 1).makeGlobal().cache();

	public final OsmandPreference<Integer> AUDIO_MANAGER_STREAM = new IntPreference(this, "audio_stream", 3/*AudioManager.STREAM_MUSIC*/).makeProfile();

	// Corresponding USAGE value for AudioAttributes
	public final OsmandPreference<Integer>[] AUDIO_USAGE = new IntPreference[10];

	{
		AUDIO_USAGE[0] = new IntPreference(this, "audio_usage_0", 2).makeGlobal().makeShared().cache(); /*AudioManager.STREAM_VOICE_CALL -> AudioAttributes.USAGE_VOICE_COMMUNICATION*/
		AUDIO_USAGE[3] = new IntPreference(this, "audio_usage_3", 12).makeGlobal().makeShared().cache(); /*AudioManager.STREAM_MUSIC -> AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE*/
		AUDIO_USAGE[5] = new IntPreference(this, "audio_usage_5", 5).makeGlobal().makeShared().cache(); /*AudioManager.STREAM_NOTIFICATION -> AudioAttributes.USAGE_NOTIFICATION*/
	}

	// For now this can be changed only in TestVoiceActivity
	public final OsmandPreference<Integer>[] VOICE_PROMPT_DELAY = new IntPreference[10];

	{
		// 1500 ms delay works for most configurations to establish a BT SCO link
		VOICE_PROMPT_DELAY[0] = new IntPreference(this, "voice_prompt_delay_0", 1500).makeGlobal().makeShared().cache(); /*AudioManager.STREAM_VOICE_CALL*/
		// On most devices sound output works pomptly so usually no voice prompt delay needed
		VOICE_PROMPT_DELAY[3] = new IntPreference(this, "voice_prompt_delay_3", 0).makeGlobal().makeShared().cache();    /*AudioManager.STREAM_MUSIC*/
		VOICE_PROMPT_DELAY[5] = new IntPreference(this, "voice_prompt_delay_5", 0).makeGlobal().makeShared().cache();    /*AudioManager.STREAM_NOTIFICATION*/
	}

	public final OsmandPreference<Boolean> DISPLAY_TTS_UTTERANCE = new BooleanPreference(this, "display_tts_utterance", false).makeGlobal().makeShared();

	public final CommonPreference<Boolean> MAP_ONLINE_DATA = new BooleanPreference(this, "map_online_data", false).makeProfile();

	public final CommonPreference<String> MAP_OVERLAY = new StringPreference(this, "map_overlay", null).makeProfile().cache();

	public final CommonPreference<String> MAP_UNDERLAY = new StringPreference(this, "map_underlay", null).makeProfile().cache();

	public final CommonPreference<Integer> MAP_OVERLAY_TRANSPARENCY = new IntPreference(this, "overlay_transparency", 100).makeProfile().cache();

	public final CommonPreference<Integer> MAP_TRANSPARENCY = new IntPreference(this, "map_transparency", 255).makeProfile().cache();

	public final CommonPreference<Boolean> SHOW_MAP_LAYER_PARAMETER = new BooleanPreference(this, "show_map_layer_parameter", false).makeProfile().cache();

	public final CommonPreference<Boolean> KEEP_MAP_LABELS_VISIBLE = new BooleanPreference(this, "keep_map_labels_visible", false).makeProfile().cache();

	public final CommonPreference<Boolean> SHOW_POLYGONS_WHEN_UNDERLAY_IS_ON = new BooleanPreference(this, "show_polygons_when_underlay_is_on", false).makeProfile().cache();

	public boolean shouldHidePolygons(boolean groundPolygons) {
		String attrName = groundPolygons ? NO_POLYGONS_ATTR : HIDE_WATER_POLYGONS_ATTR;
		CommonPreference<Boolean> hidePreference = getCustomRenderBooleanProperty(attrName);
		return hidePreference.get() || (MAP_UNDERLAY.get() != null && !SHOW_POLYGONS_WHEN_UNDERLAY_IS_ON.get());
	}

	public final CommonPreference<String> MAP_TILE_SOURCES = new StringPreference(this, "map_tile_sources",
			TileSourceManager.getMapnikSource().getName()).makeProfile();

	public final CommonPreference<LayerTransparencySeekbarMode> LAYER_TRANSPARENCY_SEEKBAR_MODE =
			new EnumStringPreference<>(this, "layer_transparency_seekbar_mode", LayerTransparencySeekbarMode.UNDEFINED, LayerTransparencySeekbarMode.values());

	public final CommonPreference<String> MAP_OVERLAY_PREVIOUS = new StringPreference(this, "map_overlay_previous", null).makeGlobal().cache();

	public final CommonPreference<String> MAP_UNDERLAY_PREVIOUS = new StringPreference(this, "map_underlay_previous", null).makeGlobal().cache();

	public CommonPreference<String> PREVIOUS_INSTALLED_VERSION = new StringPreference(this, "previous_installed_version", "").makeGlobal();

	public final OsmandPreference<Boolean> SHOULD_SHOW_FREE_VERSION_BANNER = new BooleanPreference(this, "should_show_free_version_banner", false).makeGlobal().makeShared().cache();

	public final OsmandPreference<Boolean> USE_DISCRETE_AUTO_ZOOM = new BooleanPreference(this, "use_v1_auto_zoom", false).makeGlobal().makeShared().cache();

	public final OsmandPreference<Boolean> USE_LEFT_DISTANCE_TO_INTERMEDIATE = new BooleanPreference(this, "use_left_distance_to_intermediate", false).makeProfile().makeShared().cache();

	public final OsmandPreference<Boolean> TRANSPARENT_STATUS_BAR = new BooleanPreference(this, "transparent_status_bar", true).makeGlobal().makeShared();

	public final OsmandPreference<Boolean> SHOW_INFO_ABOUT_PRESSED_KEY = new BooleanPreference(this, "show_info_about_pressed_key", false).makeGlobal().makeShared();

	public final ListStringPreference TOP_WIDGET_PANEL_ORDER = (ListStringPreference) new ListStringPreference(this,
			"top_widget_panel_order", TextUtils.join(WIDGET_SEPARATOR, WidgetsPanel.TOP.getOriginalOrder()), PAGE_SEPARATOR) {
		@Override
		public String getModeValue(ApplicationMode mode) {
			String value = super.getModeValue(mode);
			if (!Algorithms.isEmpty(value)) {
				return getPagedWidgetIds(Arrays.asList(value.split(getDelimiter())));
			}
			return value;
		}
	}.makeProfile();

	public final ListStringPreference BOTTOM_WIDGET_PANEL_ORDER = (ListStringPreference) new ListStringPreference(this,
			"bottom_widget_panel_order", TextUtils.join(WIDGET_SEPARATOR, WidgetsPanel.BOTTOM.getOriginalOrder()), PAGE_SEPARATOR) {
		@Override
		public String getModeValue(ApplicationMode mode) {
			String value = super.getModeValue(mode);
			if (!Algorithms.isEmpty(value)) {
				return getPagedWidgetIds(Arrays.asList(value.split(getDelimiter())));
			}
			return value;
		}
	}.makeProfile();

	public final ListStringPreference LEFT_WIDGET_PANEL_ORDER = (ListStringPreference) new ListStringPreference(this,
			"left_widget_panel_order", TextUtils.join(WIDGET_SEPARATOR, WidgetsPanel.LEFT.getOriginalOrder()), PAGE_SEPARATOR).makeProfile();

	@Deprecated
	public final ListStringPreference WIDGET_TOP_PANEL_ORDER = (ListStringPreference) new ListStringPreference(this,
			"widget_top_panel_order", TextUtils.join(WIDGET_SEPARATOR, WidgetsPanel.TOP.getOriginalOrder()), PAGE_SEPARATOR) {
		@Override
		public void readFromJson(JSONObject json, ApplicationMode appMode) throws JSONException {
			if (appMode != null) {
				String value = json.getString(getId());
				TOP_WIDGET_PANEL_ORDER.setModeValue(appMode, parseString(value));
				updateExistingWidgetIds(OsmandSettings.this, appMode, TOP_WIDGET_PANEL_ORDER, LEFT_WIDGET_PANEL_ORDER);
				updateExistingWidgetIds(OsmandSettings.this, appMode, TOP_WIDGET_PANEL_ORDER, RIGHT_WIDGET_PANEL_ORDER);
			}
		}

		@Override
		public boolean writeToJson(JSONObject json, ApplicationMode appMode) {
			return false;
		}
	}.makeProfile();

	@Deprecated
	public final ListStringPreference WIDGET_BOTTOM_PANEL_ORDER = (ListStringPreference) new ListStringPreference(this,
			"widget_bottom_panel_order", TextUtils.join(WIDGET_SEPARATOR, WidgetsPanel.BOTTOM.getOriginalOrder()), PAGE_SEPARATOR) {
		@Override
		public void readFromJson(JSONObject json, ApplicationMode appMode) throws JSONException {
			if (appMode != null) {
				String value = json.getString(getId());
				BOTTOM_WIDGET_PANEL_ORDER.setModeValue(appMode, parseString(value));
				updateExistingWidgetIds(OsmandSettings.this, appMode, BOTTOM_WIDGET_PANEL_ORDER, LEFT_WIDGET_PANEL_ORDER);
				updateExistingWidgetIds(OsmandSettings.this, appMode, BOTTOM_WIDGET_PANEL_ORDER, RIGHT_WIDGET_PANEL_ORDER);
			}
		}

		@Override
		public boolean writeToJson(JSONObject json, ApplicationMode appMode) {
			return false;
		}
	}.makeProfile();

	public final ListStringPreference RIGHT_WIDGET_PANEL_ORDER = (ListStringPreference) new ListStringPreference(this,
			"right_widget_panel_order", TextUtils.join(WIDGET_SEPARATOR, WidgetsPanel.RIGHT.getOriginalOrder()), PAGE_SEPARATOR).makeProfile();

	@NonNull
	private String getPagedWidgetIds(@NonNull List<String> pages) {
		StringBuilder builder = new StringBuilder();

		Iterator<String> iterator = pages.iterator();
		while (iterator.hasNext()) {
			boolean pageSeparatorAdded = false;
			String page = iterator.next();
			for (String id : page.split(WIDGET_SEPARATOR)) {
				if (WidgetType.isComplexWidget(id)) {
					pageSeparatorAdded = true;
					builder.append(id).append(PAGE_SEPARATOR);
				} else {
					pageSeparatorAdded = false;
					builder.append(id).append(WIDGET_SEPARATOR);
				}
			}
			if (iterator.hasNext() && !pageSeparatorAdded) {
				builder.append(PAGE_SEPARATOR);
			}
		}
		return builder.toString();
	}

	public final ListStringPreference CUSTOM_WIDGETS_KEYS = (ListStringPreference) new ListStringPreference(this, "custom_widgets_keys", null, WIDGET_SEPARATOR).makeProfile();

	public final OsmandPreference<Integer> DISPLAYED_MARKERS_WIDGETS_COUNT = new IntPreference(this, "displayed_markers_widgets_count", 1).makeProfile();

	public final OsmandPreference<Boolean> SHOW_MAP_MARKERS = new BooleanPreference(this, "show_map_markers", true).makeProfile();

	public final CommonPreference<TracksSortMode> SEARCH_TRACKS_SORT_MODE = new EnumStringPreference<>(this, "search_tracks_sort_mode", TracksSortMode.getDefaultSortMode(null), TracksSortMode.values());
	public final ListStringPreference TRACKS_TABS_SORT_MODES = (ListStringPreference) new ListStringPreference(this, "tracks_tabs_sort_modes", null, ";;").makeGlobal().makeShared().cache();

	public final OsmandPreference<Boolean> ANIMATE_MY_LOCATION = new BooleanPreference(this, "animate_my_location", true).makeProfile().cache();

	public final OsmandPreference<String> EXTERNAL_INPUT_DEVICE = new StringPreference(this, "selected_external_input_device", KeyboardDeviceProfile.ID).makeProfile();
	public final CommonPreference<String> CUSTOM_EXTERNAL_INPUT_DEVICES = new StringPreference(this, "custom_external_input_devices", "").makeProfile();
	public final OsmandPreference<Boolean> EXTERNAL_INPUT_DEVICE_ENABLED = new BooleanPreference(this, "external_input_device_enabled", true).makeProfile();

	public final OsmandPreference<Boolean> ROUTE_MAP_MARKERS_START_MY_LOC = new BooleanPreference(this, "route_map_markers_start_my_loc", false).makeGlobal().makeShared().cache();
	public final OsmandPreference<Boolean> ROUTE_MAP_MARKERS_ROUND_TRIP = new BooleanPreference(this, "route_map_markers_round_trip", false).makeGlobal().makeShared().cache();

	public final OsmandPreference<Boolean> SEARCH_HISTORY = new BooleanPreference(this, "search_history", true).makeGlobal().makeShared();
	public final OsmandPreference<Boolean> NAVIGATION_HISTORY = new BooleanPreference(this, "navigation_history", true).makeGlobal().makeShared();
	public final OsmandPreference<Boolean> MAP_MARKERS_HISTORY = new BooleanPreference(this, "map_markers_history", true).makeGlobal().makeShared();

	@NonNull
	public ITileSource getMapTileSource(boolean warnWhenSelected) {
		ITileSource tileSource = getLayerTileSource(MAP_TILE_SOURCES, warnWhenSelected);
		return tileSource != null ? tileSource : TileSourceManager.getMapnikSource();
	}

	@Nullable
	public ITileSource getLayerTileSource(CommonPreference<String> layerSetting, boolean warnWhenSelected) {
		String tileName = layerSetting.get();
		if (tileName != null) {
			ITileSource tileSource = getTileSourceByName(tileName, warnWhenSelected);
			if (tileSource != null) {
				return tileSource;
			}
		}
		return null;
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

	@NonNull
	public String getSelectedMapSourceTitle() {
		return MAP_ONLINE_DATA.get() ? getTileSourceTitle(MAP_TILE_SOURCES.get()) : ctx.getString(R.string.vector_data);
	}

	@NonNull
	public String getTileSourceTitle(@NonNull String fileName) {
		if (fileName.endsWith(SQLITE_EXT)) {
			ITileSource tileSource = getTileSourceByName(fileName, false);
			return getTileSourceTitle(tileSource, fileName);
		}
		return fileName;
	}

	@NonNull
	public String getTileSourceTitle(@Nullable ITileSource tileSource, @NonNull String fileName) {
		if (tileSource instanceof SQLiteTileSource) {
			return ((SQLiteTileSource) tileSource).getTitle();
		}
		return fileName.replace(SQLITE_EXT, "");
	}

	@Nullable
	public ITileSource getTileSourceByName(String tileName, boolean warnWhenSelected) {
		if (tileName == null || tileName.length() == 0) {
			return null;
		}
		List<TileSourceTemplate> knownTemplates = TileSourceManager.getKnownSourceTemplates();
		File tPath = ctx.getAppPath(IndexConstants.TILES_INDEX_DIR);
		File dir = new File(tPath, tileName);
		if (!dir.exists()) {
			return checkAmongAvailableTileSources(dir, knownTemplates);
		} else if (tileName.endsWith(SQLITE_EXT)) {
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
			if (files != null) {
				Arrays.sort(files, (f1, f2) -> {
					if (f1.lastModified() > f2.lastModified()) {
						return -1;
					} else if (f1.lastModified() == f2.lastModified()) {
						return 0;
					}
					return 1;
				});
				for (File f : files) {
					String fileName = f.getName();
					if (fileName.endsWith(SQLITE_EXT)) {
						if (sqlite) {
							map.put(fileName, getTileSourceTitle(fileName));
						}
					} else if (f.isDirectory() && !fileName.equals(IndexConstants.TEMP_SOURCE_TO_LOAD)
							&& !fileName.startsWith(".")) {
						map.put(fileName, fileName);
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

	public final OsmandPreference<Boolean> SHARED_STORAGE_MIGRATION_FINISHED = new BooleanPreference(this,
			"shared_storage_migration_finished", false).makeGlobal();

	public static final String EXTERNAL_STORAGE_DIR = "external_storage_dir";

	public static final String EXTERNAL_STORAGE_DIR_V19 = "external_storage_dir_V19";
	public static final String EXTERNAL_STORAGE_DIR_TYPE_V19 = "external_storage_dir_type_V19";
	public static final int EXTERNAL_STORAGE_TYPE_DEFAULT = 0; // Environment.getExternalStorageDirectory()
	public static final int EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE = 1; // ctx.getExternalFilesDirs(null)
	public static final int EXTERNAL_STORAGE_TYPE_INTERNAL_FILE = 2; // ctx.getFilesDir()
	public static final int EXTERNAL_STORAGE_TYPE_OBB = 3; // ctx.getObbDirs
	public static final int EXTERNAL_STORAGE_TYPE_SPECIFIED = 4;

	public final OsmandPreference<Long> OSMAND_USAGE_SPACE = new LongPreference(this, "osmand_usage_space", 0).makeGlobal();

	public void freezeExternalStorageDirectory() {
		int type = settingsAPI.getInt(globalPreferences, EXTERNAL_STORAGE_DIR_TYPE_V19, -1);
		if (type == -1) {
			ValueHolder<Integer> vh = new ValueHolder<>();
			File f = getExternalStorageDirectoryV19(vh);
			setExternalStorageDirectoryV19(vh.value, f.getAbsolutePath());
		}
	}

	public void initExternalStorageDirectory() {
		File externalStorage = getExternal1AppPath();
		if (externalStorage != null && FileUtils.isWritable(externalStorage)) {
			setExternalStorageDirectoryV19(EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE,
					getExternal1AppPath().getAbsolutePath());
		} else {
			setExternalStorageDirectoryV19(EXTERNAL_STORAGE_TYPE_INTERNAL_FILE,
					getInternalAppPath().getAbsolutePath());
		}
	}

	public File getExternalStorageDirectory() {
		return getExternalStorageDirectory(null);
	}

	public File getExternalStorageDirectory(ValueHolder<Integer> type) {
		return getExternalStorageDirectoryV19(type);
	}

	public File getInternalAppPath() {
		File fl = getNoBackupPath();
		if (fl != null) {
			return fl;
		}
		return ctx.getFilesDir();
	}

	public File getExternal1AppPath() {
		File[] externals = ctx.getExternalFilesDirs(null);
		if (externals != null && externals.length > 0) {
			return externals[0];
		} else {
			return null;
		}
	}

	public File getNoBackupPath() {
		return ctx.getNoBackupFilesDir();
	}

	public File getExternalStorageDirectoryV19(ValueHolder<Integer> tp) {
		int type = settingsAPI.getInt(globalPreferences, EXTERNAL_STORAGE_DIR_TYPE_V19, -1);
		File location = getDefaultLocationV19();
		if (type == -1) {
			if (FileUtils.isWritable(location)) {
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

	public boolean isExternalStorageDirectoryTypeSpecifiedV19() {
		return settingsAPI.contains(globalPreferences, EXTERNAL_STORAGE_DIR_TYPE_V19);
	}

	public int getExternalStorageDirectoryTypeV19() {
		return settingsAPI.getInt(globalPreferences, EXTERNAL_STORAGE_DIR_TYPE_V19, -1);
	}

	public boolean isExternalStorageDirectorySpecifiedV19() {
		return settingsAPI.contains(globalPreferences, EXTERNAL_STORAGE_DIR_V19);
	}

	public String getExternalStorageDirectoryV19() {
		return settingsAPI.getString(globalPreferences, EXTERNAL_STORAGE_DIR_V19, null);
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
		File[] externals = ctx.getExternalFilesDirs(null);
		for (File file : externals) {
			if (file != null && !file.getAbsolutePath().contains("emulated")) {
				return file;
			}
		}
		return null;
	}

	public void setExternalStorageDirectory(int type, String directory) {
		setExternalStorageDirectoryV19(type, directory);

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
	public static final String LAST_KNOWN_MAP_LAT = "last_known_map_lat";
	public static final String LAST_KNOWN_MAP_LON = "last_known_map_lon";
	public static final String LAST_KNOWN_MAP_ZOOM = "last_known_map_zoom";
	public static final String LAST_KNOWN_MAP_ZOOM_FLOAT_PART = "last_known_map_zoom_float_part";
	public static final String LAST_KNOWN_MAP_HEIGHT = "last_known_map_height";
	public static final String MAP_LABEL_TO_SHOW = "map_label_to_show";
	public static final String MAP_LAT_TO_SHOW = "map_lat_to_show";
	public static final String MAP_LON_TO_SHOW = "map_lon_to_show";
	public static final String MAP_ZOOM_TO_SHOW = "map_zoom_to_show";

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

	public boolean hasMapZoomToShow() {
		return settingsAPI.contains(globalPreferences, MAP_ZOOM_TO_SHOW);
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
		if (addToHistory && pointDescription != null) {
			ctx.getSearchHistoryHelper().addNewItemToHistory(latitude, longitude, pointDescription, HistorySource.SEARCH);
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
	public void setLastKnownMapLocation(LatLon mapLocation) {
		SettingsEditor edit = settingsAPI.edit(globalPreferences);
		edit.putFloat(LAST_KNOWN_MAP_LAT, (float) mapLocation.getLatitude());
		edit.putFloat(LAST_KNOWN_MAP_LON, (float) mapLocation.getLongitude());
		edit.commit();
	}

	public int getLastKnownMapZoom() {
		return settingsAPI.getInt(globalPreferences, LAST_KNOWN_MAP_ZOOM, 5);
	}

	public void setLastKnownMapZoom(int zoom) {
		settingsAPI.edit(globalPreferences).putInt(LAST_KNOWN_MAP_ZOOM, zoom).commit();
	}

	public float getLastKnownMapZoomFloatPart() {
		return settingsAPI.getFloat(globalPreferences, LAST_KNOWN_MAP_ZOOM_FLOAT_PART, 0.0f);
	}

	public void setLastKnownMapZoomFloatPart(float zoomFloatPart) {
		settingsAPI.edit(globalPreferences).putFloat(LAST_KNOWN_MAP_ZOOM_FLOAT_PART, zoomFloatPart).commit();
	}

	public float getLastKnownMapHeight() {
		return settingsAPI.getFloat(globalPreferences, LAST_KNOWN_MAP_HEIGHT, 0.0f);
	}

	public void setLastKnownMapHeight(float height) {
		settingsAPI.edit(globalPreferences).putFloat(LAST_KNOWN_MAP_HEIGHT, height).commit();
	}

	private final CommonPreference<Float> LAST_KNOWN_MAP_ROTATION = new FloatPreference(this, "last_known_map_rotation", 0).makeProfile();
	private final CommonPreference<Float> LAST_KNOWN_MANUALLY_MAP_ROTATION = new FloatPreference(this, "last_known_manually_map_rotation", 0).makeProfile();
	private final CommonPreference<Float> LAST_KNOWN_MAP_ELEVATION = new FloatPreference(this, "last_known_map_elevation", 90).makeProfile();

	public float getLastKnownMapRotation() {
		return getLastKnownMapRotation(getApplicationMode());
	}

	public float getLastKnownMapRotation(@NonNull ApplicationMode appMode) {
		return LAST_KNOWN_MAP_ROTATION.getModeValue(appMode);
	}

	public void setLastKnownMapRotation(float rotation) {
		setLastKnownMapRotation(getApplicationMode(), rotation);
	}

	public void setLastKnownMapRotation(@NonNull ApplicationMode appMode, float rotation) {
		LAST_KNOWN_MAP_ROTATION.setModeValue(appMode, rotation);
	}

	public float getManuallyMapRotation() {
		return getManuallyMapRotation(getApplicationMode());
	}

	public float getManuallyMapRotation(@NonNull ApplicationMode appMode) {
		return LAST_KNOWN_MANUALLY_MAP_ROTATION.getModeValue(appMode);
	}

	public void setManuallyMapRotation(float rotation) {
		setManuallyMapRotation(getApplicationMode(), rotation);
	}

	public void setManuallyMapRotation(@NonNull ApplicationMode appMode, float rotation) {
		LAST_KNOWN_MANUALLY_MAP_ROTATION.setModeValue(appMode, rotation);
	}

	public float getLastKnownMapElevation() {
		return getLastKnownMapElevation(getApplicationMode());
	}

	public float getLastKnownMapElevation(@NonNull ApplicationMode appMode) {
		return LAST_KNOWN_MAP_ELEVATION.getModeValue(appMode);
	}

	public void setLastKnownMapElevation(float elevation) {
		setLastKnownMapElevation(getApplicationMode(), elevation);
	}

	public void setLastKnownMapElevation(@NonNull ApplicationMode appMode, float elevation) {
		LAST_KNOWN_MAP_ELEVATION.setModeValue(appMode, elevation);
	}

	public static final String POINT_NAVIGATE_LAT = "point_navigate_lat";
	public static final String POINT_NAVIGATE_LON = "point_navigate_lon";
	public static final String POINT_NAVIGATE_ROUTE = "point_navigate_route_integer";
	public static final int NAVIGATE = 1;
	public static final String POINT_NAVIGATE_DESCRIPTION = "point_navigate_description";
	public static final String START_POINT_LAT = "start_point_lat";
	public static final String START_POINT_LON = "start_point_lon";
	public static final String START_POINT_DESCRIPTION = "start_point_description";


	public static final String POINT_NAVIGATE_LAT_BACKUP = "point_navigate_lat_backup";
	public static final String POINT_NAVIGATE_LON_BACKUP = "point_navigate_lon_backup";
	public static final String POINT_NAVIGATE_DESCRIPTION_BACKUP = "point_navigate_description_backup";
	public static final String START_POINT_LAT_BACKUP = "start_point_lat_backup";
	public static final String START_POINT_LON_BACKUP = "start_point_lon_backup";
	public static final String START_POINT_DESCRIPTION_BACKUP = "start_point_description_backup";
	public static final String INTERMEDIATE_POINTS_BACKUP = "intermediate_points_backup";
	public static final String INTERMEDIATE_POINTS_DESCRIPTION_BACKUP = "intermediate_points_description_backup";
	public static final String MY_LOC_POINT_LAT = "my_loc_point_lat";
	public static final String MY_LOC_POINT_LON = "my_loc_point_lon";
	public static final String MY_LOC_POINT_DESCRIPTION = "my_loc_point_description";


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
		if (NAVIGATION_HISTORY.get()) {
			backupPointToStart();
			backupPointToNavigate();
			backupIntermediatePoints();
		}
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
		return PointDescription.deserializeFromString(
				settingsAPI.getString(globalPreferences, START_POINT_DESCRIPTION, ""), getPointToStart());
	}

	public PointDescription getPointNavigateDescription() {
		return PointDescription.deserializeFromString(
				settingsAPI.getString(globalPreferences, POINT_NAVIGATE_DESCRIPTION, ""), getPointToNavigate());
	}

	public LatLon getPointToNavigateBackup() {
		float lat = settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LAT_BACKUP, 0);
		float lon = settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LON_BACKUP, 0);
		if (lat == 0 && lon == 0) {
			return null;
		}
		return new LatLon(lat, lon);
	}

	public LatLon getPointToStartBackup() {
		float lat = settingsAPI.getFloat(globalPreferences, START_POINT_LAT_BACKUP, 0);
		float lon = settingsAPI.getFloat(globalPreferences, START_POINT_LON_BACKUP, 0);
		if (lat == 0 && lon == 0) {
			return null;
		}
		return new LatLon(lat, lon);
	}

	public PointDescription getStartPointDescriptionBackup() {
		return PointDescription.deserializeFromString(
				settingsAPI.getString(globalPreferences, START_POINT_DESCRIPTION_BACKUP, ""), getPointToStart());
	}

	public PointDescription getPointNavigateDescriptionBackup() {
		return PointDescription.deserializeFromString(
				settingsAPI.getString(globalPreferences, POINT_NAVIGATE_DESCRIPTION_BACKUP, ""), getPointToNavigate());
	}

	public LatLon getMyLocationToStart() {
		float lat = settingsAPI.getFloat(globalPreferences, MY_LOC_POINT_LAT, 0);
		float lon = settingsAPI.getFloat(globalPreferences, MY_LOC_POINT_LON, 0);
		if (lat == 0 && lon == 0) {
			return null;
		}
		return new LatLon(lat, lon);
	}

	public PointDescription getMyLocationToStartDescription() {
		return PointDescription.deserializeFromString(
				settingsAPI.getString(globalPreferences, MY_LOC_POINT_DESCRIPTION, ""), getMyLocationToStart());
	}

	public void setMyLocationToStart(double latitude, double longitude, PointDescription p) {
		settingsAPI.edit(globalPreferences).putFloat(MY_LOC_POINT_LAT, (float) latitude).putFloat(MY_LOC_POINT_LON, (float) longitude).commit();
		settingsAPI.edit(globalPreferences).putString(MY_LOC_POINT_DESCRIPTION, PointDescription.serializeToString(p)).commit();
	}

	public void clearMyLocationToStart() {
		settingsAPI.edit(globalPreferences).remove(MY_LOC_POINT_LAT).remove(MY_LOC_POINT_LON).
				remove(MY_LOC_POINT_DESCRIPTION).commit();
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

	public final CommonPreference<Boolean> USE_INTERMEDIATE_POINTS_NAVIGATION =
			new BooleanPreference(this, "use_intermediate_points_navigation", false).makeGlobal().cache();


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

	public boolean clearPointToNavigate() {
		return settingsAPI.edit(globalPreferences).remove(POINT_NAVIGATE_LAT).remove(POINT_NAVIGATE_LON).
				remove(POINT_NAVIGATE_DESCRIPTION).commit();
	}

	public boolean clearPointToStart() {
		return settingsAPI.edit(globalPreferences).remove(START_POINT_LAT).remove(START_POINT_LON).
				remove(START_POINT_DESCRIPTION).commit();
	}

	public boolean clearPointToNavigateBackup() {
		return settingsAPI.edit(globalPreferences).remove(POINT_NAVIGATE_LAT_BACKUP).remove(POINT_NAVIGATE_LON_BACKUP).
				remove(POINT_NAVIGATE_DESCRIPTION_BACKUP).commit();
	}

	public boolean clearPointToStartBackup() {
		return settingsAPI.edit(globalPreferences).remove(START_POINT_LAT_BACKUP).remove(START_POINT_LON_BACKUP).
				remove(START_POINT_DESCRIPTION_BACKUP).commit();
	}

	public boolean clearIntermediatePointsBackup() {
		return settingsAPI.edit(globalPreferences).remove(INTERMEDIATE_POINTS_BACKUP).remove(INTERMEDIATE_POINTS_DESCRIPTION_BACKUP).commit();
	}

	public boolean setPointToNavigate(double latitude, double longitude, PointDescription p) {
		boolean add = settingsAPI.edit(globalPreferences).putFloat(POINT_NAVIGATE_LAT, (float) latitude).putFloat(POINT_NAVIGATE_LON, (float) longitude).commit();
		settingsAPI.edit(globalPreferences).putString(POINT_NAVIGATE_DESCRIPTION, PointDescription.serializeToString(p)).commit();
		if (add && NAVIGATION_HISTORY.get()) {
			if (p != null && !p.isSearchingAddress(ctx)) {
				ctx.getSearchHistoryHelper().addNewItemToHistory(latitude, longitude, p, HistorySource.NAVIGATION);
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

	public long getImpassableRoadsLastModifiedTime() {
		return impassableRoadsStorage.getLastModifiedTime();
	}

	public void setImpassableRoadsLastModifiedTime(long lastModifiedTime) {
		impassableRoadsStorage.setLastModifiedTime(lastModifiedTime);
	}

	public List<AvoidRoadInfo> getImpassableRoadPoints() {
		return impassableRoadsStorage.getImpassableRoadsInfo();
	}

	public boolean addImpassableRoad(AvoidRoadInfo avoidRoadInfo) {
		return impassableRoadsStorage.addImpassableRoadInfo(avoidRoadInfo);
	}

	public boolean updateImpassableRoadInfo(AvoidRoadInfo avoidRoadInfo) {
		return impassableRoadsStorage.updateImpassableRoadInfo(avoidRoadInfo);
	}

	public boolean removeImpassableRoad(int index) {
		return impassableRoadsStorage.deletePoint(index);
	}

	public boolean removeImpassableRoad(@NonNull LatLon latLon) {
		return impassableRoadsStorage.deletePoint(latLon);
	}

	public boolean moveImpassableRoad(@NonNull LatLon latLonEx, @NonNull LatLon latLonNew) {
		return impassableRoadsStorage.movePoint(latLonEx, latLonNew);
	}

	public final CommonPreference<Boolean> IS_QUICK_ACTION_TUTORIAL_SHOWN = new BooleanPreference(this, "quick_action_tutorial", false).makeGlobal().makeShared();
	public final ListStringPreference QUICK_ACTION_BUTTONS = (ListStringPreference) new ListStringPreference(this, "quick_action_buttons", DEFAULT_BUTTON_ID + ";", ";").makeGlobal();


	/**
	 * the location of a parked car
	 */

	public static final String LAST_SEARCHED_REGION = "last_searched_region";
	public static final String LAST_SEARCHED_CITY = "last_searched_city";
	public static final String LAST_SEARCHED_CITY_NAME = "last_searched_city_name";
	public static final String LAST_SEARCHED_POSTCODE = "last_searched_postcode";
	public static final String LAST_SEARCHED_STREET = "last_searched_street";
	public static final String LAST_SEARCHED_BUILDING = "last_searched_building";
	public static final String LAST_SEARCHED_INTERSECTED_STREET = "last_searched_intersected_street";
	public static final String LAST_SEARCHED_LAT = "last_searched_lat";
	public static final String LAST_SEARCHED_LON = "last_searched_lon";

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
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_REGION, "");
	}

	public boolean setLastSearchedRegion(String region, LatLon l) {
		SettingsEditor edit = settingsAPI.edit(globalPreferences).putString(LAST_SEARCHED_REGION, region).putLong(LAST_SEARCHED_CITY, -1).
				putString(LAST_SEARCHED_CITY_NAME, "").putString(LAST_SEARCHED_POSTCODE, "").
				putString(LAST_SEARCHED_STREET, "").putString(LAST_SEARCHED_BUILDING, ""); //$NON-NLS-2$
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, "");
		}
		boolean res = edit.commit();
		setLastSearchedPoint(l);
		return res;
	}

	public String getLastSearchedPostcode() {
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_POSTCODE, null);
	}

	public boolean setLastSearchedPostcode(String postcode, LatLon point) {
		SettingsEditor edit = settingsAPI.edit(globalPreferences).putLong(LAST_SEARCHED_CITY, -1).putString(LAST_SEARCHED_STREET, "")
				.putString(LAST_SEARCHED_BUILDING, "").putString(LAST_SEARCHED_POSTCODE, postcode);
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, "");
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
				putString(LAST_SEARCHED_STREET, "").putString(LAST_SEARCHED_BUILDING, "").putString(LAST_SEARCHED_POSTCODE, "");
		//edit.remove(LAST_SEARCHED_POSTCODE);
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, "");
		}
		boolean res = edit.commit();
		setLastSearchedPoint(point);
		return res;
	}

	public String getLastSearchedStreet() {
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_STREET, "");
	}

	public boolean setLastSearchedStreet(String street, LatLon point) {
		SettingsEditor edit = settingsAPI.edit(globalPreferences).putString(LAST_SEARCHED_STREET, street).putString(LAST_SEARCHED_BUILDING, "");
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, "");
		}
		boolean res = edit.commit();
		setLastSearchedPoint(point);
		return res;
	}

	public String getLastSearchedBuilding() {
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_BUILDING, "");
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
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET, "");
	}

	public boolean setLastSearchedIntersectedStreet(String street, LatLon l) {
		setLastSearchedPoint(l);
		return settingsAPI.edit(globalPreferences).putString(LAST_SEARCHED_INTERSECTED_STREET, street).commit();
	}

	public final OsmandPreference<String> LAST_SELECTED_GPX_TRACK_FOR_NEW_POINT = new StringPreference(this, "last_selected_gpx_track_for_new_point", null).makeGlobal().cache();

	// Avoid using this property, probably you need to use PoiFiltersHelper.getSelectedPoiFilters()
	private final ListStringPreference SELECTED_POI_FILTER_FOR_MAP = (ListStringPreference)
			new ListStringPreference(this, "selected_poi_filters_for_map", null, ",,").makeProfile().cache();

	@NonNull
	public Set<String> getSelectedPoiFilters() {
		List<String> result = SELECTED_POI_FILTER_FOR_MAP.getStringsList();
		return result != null ? new LinkedHashSet<>(result) : Collections.emptySet();
	}

	public void setSelectedPoiFilters(@Nullable Set<String> poiFilters) {
		setSelectedPoiFilters(APPLICATION_MODE.get(), poiFilters);
	}

	public void setSelectedPoiFilters(@NonNull ApplicationMode appMode, @Nullable Set<String> poiFilters) {
		List<String> filters = poiFilters != null ? new ArrayList<>(poiFilters) : null;
		SELECTED_POI_FILTER_FOR_MAP.setStringsListForProfile(appMode, filters);
	}

	public final ListStringPreference POI_FILTERS_ORDER = (ListStringPreference)
			new ListStringPreference(this, "poi_filters_order", null, ",,").makeProfile().cache();

	public final ListStringPreference INACTIVE_POI_FILTERS = (ListStringPreference)
			new ListStringPreference(this, "inactive_poi_filters", null, ",,").makeProfile().cache();

	public final ContextMenuItemsPreference DRAWER_ITEMS =
			(ContextMenuItemsPreference) new ContextMenuItemsPreference(this, "drawer_items", DRAWER_ITEM_ID_SCHEME, DrawerMenuItemsSettings.getDrawerDefaultInstance())
					.makeProfile().cache();

	public final ListStringPreference COLLAPSED_CONFIGURE_MAP_CATEGORIES = (ListStringPreference)
			new ListStringPreference(this, "collapsed_configure_map_categories", "", ",,").makeProfile().cache();

	public final ContextMenuItemsPreference CONFIGURE_MAP_ITEMS =
			(ContextMenuItemsPreference) new ContextMenuItemsPreference(this, "configure_map_items", CONFIGURE_MAP_ITEM_ID_SCHEME, new ContextMenuItemsSettings())
					.makeProfile().cache();

	public final ContextMenuItemsPreference CONTEXT_MENU_ACTIONS_ITEMS =
			(ContextMenuItemsPreference) new ContextMenuItemsPreference(this, "context_menu_items", MAP_CONTEXT_MENU_ACTIONS, new MainContextMenuItemsSettings())
					.makeProfile().cache();

	public final List<ContextMenuItemsPreference> CONTEXT_MENU_ITEMS_PREFERENCES = Arrays.asList(DRAWER_ITEMS, CONFIGURE_MAP_ITEMS, CONTEXT_MENU_ACTIONS_ITEMS);

	@Nullable
	public ContextMenuItemsPreference getContextMenuItemsPreference(@NonNull String id) {
		for (ContextMenuItemsPreference preference : CONTEXT_MENU_ITEMS_PREFERENCES) {
			if (id.startsWith(preference.getIdScheme())) {
				return preference;
			}
		}
		return null;
	}

	public static final String VOICE_PROVIDER_NOT_USE = "VOICE_PROVIDER_NOT_USE";

	// this value could localized
	public final OsmandPreference<String> VOICE_PROVIDER = new StringPreference(this, "voice_provider", null) {
		@Override
		public String getProfileDefaultValue(ApplicationMode mode) {
			String language = ctx.getResources().getConfiguration().locale.getLanguage();
			Map<String, IndexItem> supportedTTS = getSupportedTtsByLanguages(ctx);
			IndexItem index = supportedTTS.get(language);
			if (index != null) {
				if (!index.isDownloaded() && (ctx.isApplicationInitializing() || !index.isDownloading(ctx))) {
					downloadTtsWithoutInternet(ctx, index);
				}
				return language + IndexConstants.VOICE_PROVIDER_SUFFIX;
			}
			return VOICE_PROVIDER_NOT_USE;
		}
	}.makeProfile();

	public boolean isVoiceProviderNotSelected(ApplicationMode appMode) {
		String voiceProvider = VOICE_PROVIDER.getModeValue(appMode);
		return Algorithms.isEmpty(voiceProvider) || VOICE_PROVIDER_NOT_USE.equals(voiceProvider);
	}

	public final CommonPreference<String> RENDERER = new StringPreference(this, "renderer", RendererRegistry.DEFAULT_RENDER) {

		@Override
		protected boolean setValue(Object prefs, String val) {
			if (val == null) {
				val = RendererRegistry.DEFAULT_RENDER;
			}
			RenderingRulesStorage loaded = ctx.getRendererRegistry().getRenderer(val);
			if (loaded != null) {
				return super.setValue(prefs, val);
			}
			return false;
		}

	}.makeProfile();

	{
		RENDERER.setModeDefaultValue(ApplicationMode.BOAT, RendererRegistry.NAUTICAL_RENDER);
		RENDERER.setModeDefaultValue(ApplicationMode.SKI, RendererRegistry.WINTER_SKI_RENDER);
	}

	public boolean getRenderBooleanPropertyValue(@NonNull RenderingRuleProperty property) {
		return getRenderBooleanPropertyValue(property.getAttrName());
	}

	public boolean getRenderBooleanPropertyValue(@NonNull String attrName) {
		if (attrName.equals(NO_POLYGONS_ATTR)) {
			return shouldHidePolygons(true);
		} else if (attrName.equals(HIDE_WATER_POLYGONS_ATTR)) {
			return shouldHidePolygons(false);
		} else {
			return getCustomRenderBooleanProperty(attrName).get();
		}
	}

	@NonNull
	public String getRenderPropertyValue(@NonNull RenderingRuleProperty property) {
		String attrName = property.getAttrName();
		if (ELEVATION_UNITS_ATTR.equals(attrName)) {
			boolean useFeet = METRIC_SYSTEM.get().shouldUseFeet();
			return useFeet ? ELEVATION_UNITS_FEET_VALUE : ELEVATION_UNITS_METERS_VALUE;
		}
		CommonPreference<String> preference = getCustomRenderProperty(attrName);
		String value = preference.get();

		if (property.hasPossibleValues()) {
			if (CONTOUR_LINES_ATTR.equals(attrName) && Algorithms.isEmpty(value)) {
				return value;
			}
			return property.containsValue(value) ? value : preference.getDefaultValue();
		}
		return value;
	}

	@NonNull
	public CommonPreference<String> getCustomRenderProperty(@NonNull String attrName) {
		return getCustomRenderProperty(attrName, "");
	}

	@NonNull
	public CommonPreference<String> getCustomRenderProperty(@NonNull String attrName, @Nullable String defaultValue) {
		CommonPreference<String> preference = customRendersProps.get(attrName);
		if (preference == null) {
			preference = registerCustomRenderProperty(attrName, defaultValue);
		}
		return preference;
	}

	@NonNull
	private CommonPreference<String> registerCustomRenderProperty(@NonNull String attrName, @Nullable String defaultValue) {
		String id = attrName.startsWith(RENDERER_PREFERENCE_PREFIX) ? attrName : RENDERER_PREFERENCE_PREFIX + attrName;
		CommonPreference<String> preference = new StringPreference(this, id, defaultValue).makeProfile();
		customRendersProps.put(attrName, preference);
		return preference;
	}

	{
		getCustomRenderProperty("appMode");
		getCustomRenderProperty("defAppMode");
	}

	public boolean isRenderProperty(@NonNull String prefId) {
		return prefId.startsWith(RENDERER_PREFERENCE_PREFIX);
	}

	public boolean isRoutingProperty(@NonNull String prefId) {
		return prefId.startsWith(ROUTING_PREFERENCE_PREFIX);
	}

	@NonNull
	public CommonPreference<Boolean> getCustomRenderBooleanProperty(@NonNull String attrName) {
		return getCustomRenderBooleanProperty(attrName, false);
	}

	@NonNull
	public CommonPreference<Boolean> getCustomRenderBooleanProperty(@NonNull String attrName, boolean defaultValue) {
		CommonPreference<Boolean> preference = customBooleanRendersProps.get(attrName);
		if(preference == null) {
			preference = registerCustomRenderBooleanProperty(attrName, defaultValue);
		}
		return preference;
	}

	@NonNull
	private CommonPreference<Boolean> registerCustomRenderBooleanProperty(@NonNull String attrName, boolean defaultValue) {
		String id = attrName.startsWith(RENDERER_PREFERENCE_PREFIX) ? attrName : RENDERER_PREFERENCE_PREFIX + attrName;
		CommonPreference<Boolean> preference = new BooleanPreference(this, id, defaultValue).makeProfile();
		customBooleanRendersProps.put(attrName, preference);
		return preference;
	}

	@NonNull
	public CommonPreference<String> getCustomRoutingProperty(@NonNull String attrName, String defaultValue) {
		CommonPreference<String> preference = customRoutingProps.get(attrName);
		if (preference == null) {
			String id = attrName.startsWith(ROUTING_PREFERENCE_PREFIX) ? attrName : ROUTING_PREFERENCE_PREFIX + attrName;
			preference = new StringPreference(this, id, defaultValue).makeProfile();
			customRoutingProps.put(attrName, preference);
		}
		return preference;
	}

	@NonNull
	public CommonPreference<Boolean> getCustomRoutingBooleanProperty(@NonNull String attrName, boolean defaultValue) {
		CommonPreference<Boolean> preference = customBooleanRoutingProps.get(attrName);
		if (preference == null) {
			String id = attrName.startsWith(ROUTING_PREFERENCE_PREFIX) ? attrName : ROUTING_PREFERENCE_PREFIX + attrName;
			preference = new BooleanStringPreference(this, id, defaultValue).makeProfile();
			customBooleanRoutingProps.put(attrName, preference);
		}
		return preference;
	}

	@NonNull
	public CommonPreference<Boolean> getBooleanRenderClassProperty(@NonNull RenderingClass renderingClass) {
		return getBooleanRenderClassProperty(renderingClass.getName(), renderingClass.isEnabledByDefault());
	}

	public CommonPreference<Boolean> getBooleanRenderClassProperty(@NonNull String name, boolean defaultValue) {
		CommonPreference<Boolean> preference = customBooleanRenderClassProps.get(name);
		if (preference == null) {
			preference = new BooleanPreference(this, name, defaultValue).makeProfile();
			customBooleanRenderClassProps.put(name, preference);
		}
		return preference;
	}

	public final OsmandPreference<Boolean> SHOW_TRAVEL = new BooleanPreference(this, "show_travel_routes", false).makeProfile().cache();

	public final CommonPreference<Float> ROUTE_RECALCULATION_DISTANCE = new FloatPreference(this, "routing_recalc_distance", 0.f).makeProfile();
	public final CommonPreference<Float> ROUTE_STRAIGHT_ANGLE = new FloatPreference(this, "routing_straight_angle", 30.f).makeProfile();

	public final CommonPreference<Integer> CUSTOM_ROUTE_COLOR_DAY = new IntPreference(this,
			"route_line_color", DefaultColors.values()[0].getColor()).cache().makeProfile();
	public final CommonPreference<Integer> CUSTOM_ROUTE_COLOR_NIGHT = new IntPreference(this,
			"route_line_color_night", DefaultColors.values()[0].getColor()).cache().makeProfile();
	public final CommonPreference<ColoringType> ROUTE_COLORING_TYPE = new EnumStringPreference<>(this,
			"route_line_coloring_type", ColoringType.DEFAULT, ColoringType.Companion.valuesOf(ColoringPurpose.ROUTE_LINE)).cache().makeProfile();

	public final CommonPreference<String> ROUTE_GRADIENT_PALETTE = new StringPreference(this, "route_gradient_palette", PaletteGradientColor.DEFAULT_NAME).makeProfile().cache();
	public final CommonPreference<String> ROUTE_INFO_ATTRIBUTE = new StringPreference(this, "route_info_attribute", null)
			.cache().makeProfile();
	public final CommonPreference<String> ROUTE_LINE_WIDTH = new StringPreference(this, "route_line_width", null).makeProfile();
	public final CommonPreference<Boolean> ROUTE_SHOW_TURN_ARROWS = new BooleanPreference(this, "route_show_turn_arrows", true).makeProfile();

	public final OsmandPreference<Boolean> USE_OSM_LIVE_FOR_ROUTING = new BooleanPreference(this, "enable_osmc_routing", true).makeProfile();

	public final OsmandPreference<Boolean> USE_OSM_LIVE_FOR_PUBLIC_TRANSPORT = new BooleanPreference(this, "enable_osmc_public_transport", false).makeProfile();

	public final OsmandPreference<Boolean> VOICE_MUTE = new BooleanPreference(this, "voice_mute", false).makeProfile().cache();
	public final CommonPreference<TrackApproximationType> DETAILED_TRACK_GUIDANCE = new EnumStringPreference<>(this, "detailed_track_guidance",
			TrackApproximationType.MANUAL, TrackApproximationType.values()).makeProfile().makeShared();
	public final OsmandPreference<Integer> GPX_APPROXIMATION_DISTANCE = new IntPreference(this, "gpx_approximation_distance", DEFAULT_POINT_APPROXIMATION).makeProfile().makeShared();

	// for background service
	public boolean MAP_ACTIVITY_ENABLED = false;

	public final OsmandPreference<Boolean> SAFE_MODE = new BooleanPreference(this, "safe_mode", false).makeGlobal().makeShared();
	public final OsmandPreference<Boolean> PT_SAFE_MODE = new BooleanPreference(this, "pt_safe_mode", false).makeProfile();
	public final OsmandPreference<Boolean> NATIVE_RENDERING_FAILED = new BooleanPreference(this, "native_rendering_failed_init", false).makeGlobal();

	public final CommonPreference<Integer> LOCATION_INTERPOLATION_PERCENT = new IntPreference(this, "location_interpolation_percent", 0).makeProfile().makeShared();

	public final CommonPreference<Boolean> USE_OPENGL_RENDER = new BooleanPreference(this, "use_opengl_render",
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.P).makeGlobal().makeShared().cache();

	public final OsmandPreference<Integer> OPENGL_RENDER_FAILED = new IntPreference(this, "opengl_render_failed_count", 0).makeGlobal().cache();

	public final OsmandPreference<String> CONTRIBUTION_INSTALL_APP_DATE = new StringPreference(this, "CONTRIBUTION_INSTALL_APP_DATE", null).makeGlobal();

	public final OsmandPreference<Integer> COORDINATES_FORMAT = new IntPreference(this, "coordinates_format", PointDescription.FORMAT_DEGREES).makeProfile();

	public final OsmandPreference<Boolean> FOLLOW_THE_ROUTE = new BooleanPreference(this, "follow_to_route", false).makeGlobal();
	public final OsmandPreference<String> FOLLOW_THE_GPX_ROUTE = new StringPreference(this, "follow_gpx", null).makeGlobal();
	public final OsmandPreference<Boolean> SHOW_RESTART_NAVIGATION_DIALOG = new BooleanPreference(this, "show_restart_navigation_dialog", true).makeGlobal().makeShared();

	public final OsmandPreference<String> SELECTED_TRAVEL_BOOK = new StringPreference(this, "selected_travel_book", "").makeGlobal().makeShared();

	public final ListStringPreference DISPLAYED_TRANSPORT_SETTINGS = (ListStringPreference)
			new ListStringPreference(this, "displayed_transport_settings", null, ",").makeProfile();

	public final OsmandPreference<Boolean> SHOW_RELATIVE_BEARING_OTHERWISE_REGULAR_BEARING =
			new BooleanPreference(this, "show_relative_bearing", true).makeProfile();

	public final OsmandPreference<Long> AGPS_DATA_LAST_TIME_DOWNLOADED =
			new LongPreference(this, "agps_data_downloaded", 0).makeGlobal();

	public final CommonPreference<Integer> MEMORY_ALLOCATED_FOR_ROUTING =
			new IntPreference(this, "memory_allocated_for_routing", 256).makeGlobal();

	// Live Updates
	public final OsmandPreference<Boolean> IS_LIVE_UPDATES_ON =
			new BooleanPreference(this, "is_live_updates_on", false).makeGlobal().makeShared();
	public final OsmandPreference<Integer> LIVE_UPDATES_RETRIES =
			new IntPreference(this, "live_updates_retryes", 2).makeGlobal();

	// UI boxes
	public final CommonPreference<Boolean> TRANSPARENT_MAP_THEME =
			new BooleanPreference(this, "transparent_map_theme", false).makeProfile();

	public final CommonPreference<Boolean> SHOW_STREET_NAME = new BooleanPreference(this, "show_street_name", false).makeProfile();

	{
		SHOW_STREET_NAME.setModeDefaultValue(ApplicationMode.DEFAULT, false);
		SHOW_STREET_NAME.setModeDefaultValue(ApplicationMode.CAR, true);
		SHOW_STREET_NAME.setModeDefaultValue(ApplicationMode.BICYCLE, false);
		SHOW_STREET_NAME.setModeDefaultValue(ApplicationMode.PEDESTRIAN, false);
	}

	public final CommonPreference<Integer> SEARCH_TAB =
			new IntPreference(this, "SEARCH_TAB", 0).makeGlobal().cache();

	public final CommonPreference<Integer> FAVORITES_TAB =
			new IntPreference(this, "FAVORITES_TAB", 0).makeGlobal().cache();

	public static final int OSMAND_DARK_THEME = 0;
	public static final int OSMAND_LIGHT_THEME = 1;
	public static final int SYSTEM_DEFAULT_THEME = 2;

	public final CommonPreference<Integer> OSMAND_THEME =
			new IntPreference(this, "osmand_theme", isSupportSystemTheme() ? SYSTEM_DEFAULT_THEME : OSMAND_LIGHT_THEME) {
				@Override
				public void readFromJson(JSONObject json, ApplicationMode appMode) throws JSONException {
					Integer theme = parseString(json.getString(getId()));
					if (theme == SYSTEM_DEFAULT_THEME && !isSupportSystemTheme()) {
						theme = OSMAND_LIGHT_THEME;
					}
					setModeValue(appMode, theme);
				}
			}.makeProfile().cache();

	public final OsmandPreference<Boolean> OPEN_ONLY_HEADER_STATE_ROUTE_CALCULATED =
			new BooleanPreference(this, "open_only_header_route_calculated", false).makeProfile();

	public boolean isLightContent() {
		return isLightContentForMode(APPLICATION_MODE.get());
	}

	public boolean isLightContentForMode(ApplicationMode appMode) {
		if (isSystemThemeUsed(appMode)) {
			return isLightSystemTheme();
		}
		return OSMAND_THEME.getModeValue(appMode) != OSMAND_DARK_THEME;
	}

	public boolean isLightSystemTheme() {
		return !getNightMode(ctx.getResources().getConfiguration());
	}

	private boolean getNightMode(@NonNull Configuration config) {
		int currentNightMode = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;
		switch (currentNightMode) {
			case Configuration.UI_MODE_NIGHT_NO:
				return false;
			case Configuration.UI_MODE_NIGHT_YES:
				return true;
		}
		LOG.info("Undefined night mode" + config);
		return false;
	}

	public boolean isSystemThemeUsed(@NonNull ApplicationMode appMode) {
		return isSupportSystemTheme() && OSMAND_THEME.getModeValue(appMode) == SYSTEM_DEFAULT_THEME;
	}

	public boolean isSupportSystemTheme() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
	}

	public final CommonPreference<Boolean> FLUORESCENT_OVERLAYS =
			new BooleanPreference(this, "fluorescent_overlays", false).makeGlobal().makeShared().cache();


//	public final OsmandPreference<Integer> NUMBER_OF_FREE_DOWNLOADS_V2 = new IntPreference("free_downloads_v2", 0).makeGlobal();

	public final OsmandPreference<Integer> NUMBER_OF_FREE_DOWNLOADS = new IntPreference(this, NUMBER_OF_FREE_DOWNLOADS_ID, 0).makeGlobal();

	// For RateUsDialog
	public final OsmandPreference<Long> LAST_DISPLAY_TIME =
			new LongPreference(this, "last_display_time", 0).makeGlobal().cache();

	public final OsmandPreference<Long> LAST_CHECKED_UPDATES =
			new LongPreference(this, "last_checked_updates", 0).makeGlobal();

	public final OsmandPreference<Integer> NUMBER_OF_APP_STARTS_ON_DISLIKE_MOMENT =
			new IntPreference(this, "number_of_app_starts_on_dislike_moment", 0).makeGlobal().cache();

	public final OsmandPreference<RateUsState> RATE_US_STATE =
			new EnumStringPreference<>(this, "rate_us_state", RateUsState.INITIAL_STATE, RateUsState.values()).makeGlobal();

	public final CommonPreference<String> CUSTOM_APP_MODES_KEYS =
			new StringPreference(this, "custom_app_modes_keys", "").makeGlobal().cache();

	public final CommonPreference<Boolean> SHOW_BORDERS_OF_DOWNLOADED_MAPS =
			new BooleanPreference(this, "show_borders_of_downloaded_maps", true).makeProfile();

	public final CommonPreference<Boolean> SHOW_COORDINATES_GRID =
			new BooleanPreference(this, "show_coordinates_grid", false).makeProfile();

	public final OsmandPreference<GridFormat> COORDINATE_GRID_FORMAT =
			new EnumStringPreference<>(this, "coordinates_grid_format", GridFormat.DMS, GridFormat.values()) {
				@Override
				public GridFormat getProfileDefaultValue(@Nullable ApplicationMode mode) {
					int formatId = COORDINATES_FORMAT.getModeValue(mode);
					return GridFormat.valueOf(formatId);
				}
			}.makeProfile();

	public final CommonPreference<Integer> COORDINATE_GRID_MIN_ZOOM =
			new IntPreference(this, "coordinate_grid_min_zoom", 0).makeProfile();

	public final CommonPreference<Integer> COORDINATE_GRID_MAX_ZOOM =
			new IntPreference(this, "coordinate_grid_max_zoom", 31).makeProfile();

	public final OsmandPreference<GridLabelsPosition> COORDINATES_GRID_LABELS_POSITION =
			new EnumStringPreference<>(this, "coordinates_grid_labels_position", GridLabelsPosition.EDGES, GridLabelsPosition.values()).makeProfile();

	public final OsmandPreference<Integer> COORDINATES_GRID_COLOR_DAY =
			new IntPreference(this, "coordinates_grid_color_day", Color.parseColor("#FF1A00CC")).makeProfile();

	public final OsmandPreference<Integer> COORDINATES_GRID_COLOR_NIGHT =
			new IntPreference(this, "coordinates_grid_color_night", Color.parseColor("#FF1A00CC")).makeProfile();

	public Set<String> getCustomAppModesKeys() {
		String appModesKeys = CUSTOM_APP_MODES_KEYS.get();
		StringTokenizer toks = new StringTokenizer(appModesKeys, ",");
		Set<String> res = new LinkedHashSet<String>();
		while (toks.hasMoreTokens()) {
			res.add(toks.nextToken());
		}
		return res;
	}

	public final CommonPreference<Boolean> LAST_CYCLE_ROUTES_NODE_NETWORK_STATE =
			new BooleanPreference(this, "cycle_routes_last_node_network_state", false).makeProfile();

	public final CommonPreference<String> LAST_MTB_ROUTES_CLASSIFICATION =
			new StringPreference(this, "mtb_routes_last_classification", MtbClassification.SCALE.attrName).makeProfile();

	public final CommonPreference<String> LAST_HIKING_ROUTES_VALUE =
			new StringPreference(this, "hiking_routes_last_selected_value", "").makeProfile();

	public final OsmandPreference<Boolean> FAVORITES_FREE_ACCOUNT_CARD_DISMISSED =
			new BooleanPreference(this, "favorites_free_account_card_dismissed", false).makeGlobal();

	public final OsmandPreference<Boolean> CONFIGURE_PROFILE_FREE_ACCOUNT_CARD_DISMISSED =
			new BooleanPreference(this, "configure_profile_free_account_card_dismissed", false).makeGlobal();

	public final OsmandPreference<Boolean> TRIPLTEK_PROMO_SHOWED = new BooleanPreference(this, "tripltek_promo_showed", false).makeGlobal().makeShared();
	public final OsmandPreference<Boolean> HUGEROCK_PROMO_SHOWED = new BooleanPreference(this, "hugerock_promo_showed", false).makeGlobal().makeShared();
	public final OsmandPreference<Boolean> HMD_PROMO_SHOWED = new BooleanPreference(this, "hmd_promo_showed", false).makeGlobal().makeShared();
	public final CommonPreference<Integer> CONTEXT_GALLERY_SPAN_GRID_COUNT = new IntPreference(this, "context_gallery_span_grid_count", 3).makeProfile();
	public final CommonPreference<Integer> CONTEXT_GALLERY_SPAN_GRID_COUNT_LANDSCAPE = new IntPreference(this, "context_gallery_span_grid_count_landscape", 7).makeProfile();

	public final CommonPreference<Boolean> ENABLE_MSAA = new BooleanPreference(this, "enable_msaa", false).makeGlobal().makeShared().cache();
	public final CommonPreference<Boolean> SPHERICAL_MAP = new BooleanPreference(this, "spherical_map", false).makeGlobal().makeShared().cache();

	@NonNull
	public OsmandPreference<Boolean> getAllowPrivatePreference(@NonNull ApplicationMode appMode) {
		String derivedProfile = appMode.getDerivedProfile();
		CommonPreference<Boolean> allowPrivate =
				getCustomRoutingBooleanProperty(GeneralRouter.ALLOW_PRIVATE, false);
		CommonPreference<Boolean> allowPrivateForTruck =
				getCustomRoutingBooleanProperty(GeneralRouter.ALLOW_PRIVATE_FOR_TRUCK, false);
		return Algorithms.objectEquals(derivedProfile, "truck") ? allowPrivateForTruck : allowPrivate;
	}

	public void setPrivateAccessRoutingAsked() {
		List<ApplicationMode> modes = ApplicationMode.values(ctx);
		for (ApplicationMode mode : modes) {
			if (!getAllowPrivatePreference(mode).getModeValue(mode)) {
				FORCE_PRIVATE_ACCESS_ROUTING_ASKED.setModeValue(mode, true);
			}
		}
	}

	public void setAllowPrivateAccessAllModes(boolean allow) {
		List<ApplicationMode> modes = ApplicationMode.values(ctx);
		for (ApplicationMode mode : modes) {
			OsmandPreference<Boolean> preference = getAllowPrivatePreference(mode);
			if (preference.getModeValue(mode) != allow) {
				preference.setModeValue(mode, allow);
			}
		}
	}
}
