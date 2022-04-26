package net.osmand.plus.transport;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.MapLayers;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

final public class TransportLinesMenu {

	public static final String RENDERING_CATEGORY_TRANSPORT = "transport";

	private final OsmandApplication app;
	private final OsmandSettings settings;

	public TransportLinesMenu(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
	}

	public void toggleTransportLines(@NonNull MapActivity mapActivity, boolean enable) {
		if (enable) {
			List<String> enabledIds = settings.DISPLAYED_TRANSPORT_SETTINGS.getStringsList();
			if (enabledIds != null) {
				showEnabledTransport(mapActivity, enabledIds);
			} else {
				showTransportsDialog(mapActivity);
			}
		} else {
			hideAllTransport(mapActivity);
		}
	}

	public void setTransportEnable(@NonNull MapActivity mapActivity, @NonNull String attrName, boolean enabled) {
		ApplicationMode appMode = getAppMode();
		CommonPreference<Boolean> preference = getTransportPreference(attrName);
		preference.setModeValue(appMode, enabled);
		List<String> idsToSave = new ArrayList<>();
		for (CommonPreference<Boolean> p : getTransportPreferences(app)) {
			if (p.getModeValue(appMode)) {
				idsToSave.add(p.getId());
			}
		}
		settings.DISPLAYED_TRANSPORT_SETTINGS.setModeValues(
				appMode, !Algorithms.isEmpty(idsToSave) ? idsToSave : null);
		refreshMap(mapActivity);
	}

	public boolean isTransportEnabled(@NonNull String attrName) {
		CommonPreference<Boolean> preference = getTransportPreference(attrName);
		return settings.DISPLAYED_TRANSPORT_SETTINGS.containsValue(getAppMode(), preference.getId());
	}

	public void refreshMap(@NonNull MapActivity mapActivity) {
		mapActivity.refreshMapComplete();
		MapLayers mapLayers = mapActivity.getMapLayers();
		mapLayers.updateLayers(mapActivity);
	}

	public boolean isShowAnyTransport() {
		return isShowAnyTransport(getAppMode());
	}

	public boolean isShowAnyTransport(@NonNull ApplicationMode appMode) {
		return isShowAnyTransport(app, appMode);
	}

	public int getTransportIcon(@NonNull String attrName) {
		for (TransportType type : TransportType.values()) {
			if (type.getAttrName().equals(attrName)) {
				return type.getIconId();
			}
		}
		return R.drawable.ic_action_transport_bus;
	}

	public String getTransportName(@NonNull String attrName) {
		return getTransportName(attrName, null);
	}

	public String getTransportName(@NonNull String attrName, @Nullable String defValue) {
		return AndroidUtils.getRenderingStringPropertyName(app, attrName, defValue);
	}

	private void showEnabledTransport(@NonNull MapActivity mapActivity, @NonNull List<String> enabledIds) {
		ApplicationMode appMode = getAppMode();
		for (RenderingRuleProperty p : getTransportRules(app)) {
			CommonPreference<Boolean> preference = getTransportPreference(p.getAttrName());
			String id = preference.getId();
			boolean selected = enabledIds.contains(id);
			preference.setModeValue(appMode, selected);
		}
		refreshMap(mapActivity);
	}

	private void hideAllTransport(@NonNull MapActivity mapActivity) {
		ApplicationMode appMode = getAppMode();
		for (RenderingRuleProperty p : getTransportRules(app)) {
			CommonPreference<Boolean> preference = getTransportPreference(p.getAttrName());
			preference.setModeValue(appMode, false);
		}
		refreshMap(mapActivity);
	}

	private ApplicationMode getAppMode() {
		return settings.getApplicationMode();
	}

	private CommonPreference<Boolean> getTransportPreference(@NonNull String attrName) {
		return settings.getCustomRenderBooleanProperty(attrName);
	}

	public static boolean isShowAnyTransport(@NonNull OsmandApplication app) {
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		return isShowAnyTransport(app, appMode);
	}

	public static boolean isShowAnyTransport(@NonNull OsmandApplication app, @NonNull ApplicationMode appMode) {
		List<CommonPreference<Boolean>> preferences = getTransportPreferences(app);
		for (CommonPreference<Boolean> preference : preferences) {
			if (preference.getModeValue(appMode)) {
				return true;
			}
		}
		return false;
	}

	private static List<CommonPreference<Boolean>> getTransportPreferences(@NonNull OsmandApplication app) {
		List<RenderingRuleProperty> rules = getTransportRules(app);
		List<CommonPreference<Boolean>> preferences = new ArrayList<>();
		for (RenderingRuleProperty property : rules) {
			String attrName = property.getAttrName();
			OsmandSettings settings = app.getSettings();
			CommonPreference<Boolean> pref = settings.getCustomRenderBooleanProperty(attrName);
			preferences.add(pref);
		}
		return preferences;
	}

	public static List<RenderingRuleProperty> getTransportRules(OsmandApplication app) {
		List<RenderingRuleProperty> rules = new ArrayList<>();
		for (RenderingRuleProperty property : ConfigureMapUtils.getCustomRules(app)) {
			if (RENDERING_CATEGORY_TRANSPORT.equals(property.getCategory()) && property.isBoolean()) {
				rules.add(property);
			}
		}
		return rules;
	}

	public static void showTransportsDialog(@NonNull MapActivity mapActivity) {
		DashboardOnMap dashboard = mapActivity.getDashboard();
		dashboard.setDashboardVisibility(true, DashboardType.TRANSPORT_LINES);
	}
}