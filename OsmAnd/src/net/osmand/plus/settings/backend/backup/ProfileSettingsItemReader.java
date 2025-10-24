package net.osmand.plus.settings.backend.backup;

import static net.osmand.plus.settings.enums.SunPositionMode.SUN_POSITION_MODE;
import static net.osmand.plus.views.mapwidgets.widgetstates.ResizableWidgetState.SIMPLE_WIDGET_SIZE_ID;
import static net.osmand.plus.views.mapwidgets.widgetstates.SunriseSunsetWidgetState.SUN_POSITION_WIDGET_MODE_ID;
import static net.osmand.router.GeneralRouter.USE_SHORTEST_WAY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.AppVersionUpgradeOnInit;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ApplicationModeBean;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.SunPositionMode;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.views.mapwidgets.widgetstates.MapMarkerSideWidgetState.MarkerClickBehaviour;
import net.osmand.plus.views.mapwidgets.widgetstates.MapMarkerSideWidgetState.SideMarkerMode;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ProfileSettingsItemReader<T extends ProfileSettingsItem> extends OsmandSettingsItemReader<T> {

	private final ProfileSettingsItem settingsItem;
	private final Set<String> appModeBeanPrefsIds;

	public ProfileSettingsItemReader(@NonNull T item) {
		super(item);
		this.settingsItem = item;
		this.appModeBeanPrefsIds = ApplicationModeBean.getAppModeBeanPrefsIds(item.getApp());
	}

	@Override
	protected void readPreferenceFromJson(@NonNull OsmandPreference<?> preference,
			@NonNull JSONObject json) throws JSONException {
		if (!appModeBeanPrefsIds.contains(preference.getId())) {
			preference.readFromJson(json, settingsItem.getAppMode());
		}
	}

	@Override
	public void readPreferencesFromJson(@NonNull JSONObject json) {
		AppVersionUpgradeOnInit.migrateRouteRecalculationJsonValues(json);

		OsmandApplication app = settingsItem.getApp();
		app.runInUIThread(() -> {
			OsmandSettings settings = app.getSettings();
			ApplicationMode appMode = settingsItem.getAppMode();
			Map<String, OsmandPreference<?>> prefs = settings.getRegisteredPreferences();
			Iterator<String> iterator = json.keys();
			while (iterator.hasNext()) {
				String key = iterator.next();
				OsmandPreference<?> preference = prefs.get(key);
				if (preference == null) {
					String value = json.optString(key);
					preference = createPreference(settings, key, value);
				}
				if (preference != null) {
					try {
						readPreferenceFromJson(preference, json);
						if (OsmandSettings.isRoutingPreference(preference.getId())
								&& preference.getId().endsWith(USE_SHORTEST_WAY)) {
							settings.FAST_ROUTE_MODE.setModeValue(appMode,
									!settings.getCustomRoutingBooleanProperty(USE_SHORTEST_WAY, false).getModeValue(appMode));
						}
					} catch (Exception e) {
						SettingsHelper.LOG.error("Failed to read preference: " + key, e);
					}
				} else {
					SettingsHelper.LOG.warn("No preference while importing settings: " + key);
				}
			}
			long lastModifiedTime = settingsItem.getLastModifiedTime();
			settings.setLastModePreferencesEditTime(appMode, lastModifiedTime);
		});
	}

	@Nullable
	private OsmandPreference<?> createPreference(@NonNull OsmandSettings settings,
			@NonNull String key, @Nullable String value) {
		if (!Algorithms.isEmpty(value)) {
			boolean boolPref = "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
			if (OsmandSettings.isRoutingPreference(key)) {
				return boolPref ? settings.getCustomRoutingBooleanProperty(key,
						Boolean.parseBoolean(value)) : settings.getCustomRoutingProperty(key, value);
			} else if (OsmandSettings.isRendererPreference(key)) {
				return boolPref ? settings.getCustomRenderBooleanProperty(key) : settings.getCustomRenderProperty(key);
			} else if (boolPref) {
				return settings.registerBooleanPreference(key, Boolean.parseBoolean(value));
			} else if (key.startsWith(SIMPLE_WIDGET_SIZE_ID)) {
				return settings.registerEnumStringPreference(key, WidgetSize.MEDIUM, WidgetSize.values(), WidgetSize.class);
			} else if (key.startsWith(SUN_POSITION_WIDGET_MODE_ID)) {
				return settings.registerEnumStringPreference(key, SUN_POSITION_MODE, SunPositionMode.values(), SunPositionMode.class);
			} else if (key.contains("map_marker_mode")) {
				return settings.registerEnumStringPreference(key, SideMarkerMode.DISTANCE, SideMarkerMode.values(), SideMarkerMode.class);
			} else if (key.contains("map_marker_click_behaviour")) {
				return settings.registerEnumStringPreference(key, MarkerClickBehaviour.SWITCH_MODE, MarkerClickBehaviour.values(), MarkerClickBehaviour.class);
			}
		}
		return null;
	}
}
