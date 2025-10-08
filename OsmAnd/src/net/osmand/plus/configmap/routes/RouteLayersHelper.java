package net.osmand.plus.configmap.routes;

import static net.osmand.osm.OsmRouteType.ALPINE;
import static net.osmand.osm.OsmRouteType.BICYCLE;
import static net.osmand.osm.OsmRouteType.HIKING;
import static net.osmand.osm.OsmRouteType.MTB;
import static net.osmand.osm.OsmRouteType.SKI_ROUTES;
import static net.osmand.osm.RenderingPropertyAttr.SKI_SLOPES;
import static net.osmand.plus.configmap.ConfigureMapMenu.ALPINE_HIKING_SCALE_SCHEME_ATTR;
import static net.osmand.plus.configmap.routes.AlpineHikingCard.getDifficultyClassificationDescription;
import static net.osmand.plus.configmap.routes.RouteUtils.CYCLE_NODE_NETWORK_ROUTES_ATTR;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.render.RenderingClass;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.util.Objects;

public class RouteLayersHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private final CommonPreference<Boolean> cycleRoutesPreference;
	private final CommonPreference<Boolean> cycleRoutesNodeNetworkPreference;
	private final CommonPreference<Boolean> cycleRoutesLastNodeNetworkState;

	private final CommonPreference<Boolean> mtbRoutesPreference;
	private final CommonPreference<String> mtbRoutesLastClassification;

	private final CommonPreference<String> hikingRoutesPreference;
	private final CommonPreference<String> hikingRoutesLastValue;

	private final CommonPreference<Boolean> alpineHikingPreference;
	private final CommonPreference<String> alpineHikingScaleScheme;

	private final CommonPreference<Boolean> pisteRoutesPreference;
	private final CommonPreference<Boolean> showSkiSlopesPreference;

	@Nullable
	private String selectedAttrName;
	@Nullable
	private RenderingClass selectedRenderingClass;

	public RouteLayersHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();

		cycleRoutesPreference = settings.getCustomRenderBooleanProperty(BICYCLE.getRenderingPropertyAttr());
		cycleRoutesNodeNetworkPreference = settings.getCustomRenderBooleanProperty(CYCLE_NODE_NETWORK_ROUTES_ATTR);
		cycleRoutesLastNodeNetworkState = settings.LAST_CYCLE_ROUTES_NODE_NETWORK_STATE;

		mtbRoutesPreference = settings.getCustomRenderBooleanProperty(MTB.getRenderingPropertyAttr());
		mtbRoutesLastClassification = settings.LAST_MTB_ROUTES_CLASSIFICATION;

		hikingRoutesPreference = settings.getCustomRenderProperty(HIKING.getRenderingPropertyAttr());
		hikingRoutesLastValue = settings.LAST_HIKING_ROUTES_VALUE;

		alpineHikingPreference = settings.getCustomRenderBooleanProperty(ALPINE.getRenderingPropertyAttr());
		alpineHikingScaleScheme = settings.getCustomRenderProperty(ALPINE_HIKING_SCALE_SCHEME_ATTR);

		pisteRoutesPreference = settings.getCustomRenderBooleanProperty(SKI_ROUTES.getRenderingPropertyAttr());
		showSkiSlopesPreference = settings.getCustomRenderBooleanProperty(SKI_SLOPES.getAttrName());
	}

	@Nullable
	public String getSelectedAttrName() {
		return selectedAttrName;
	}

	public void setSelectedAttrName(@Nullable String selectedAttrName) {
		this.selectedAttrName = selectedAttrName;
	}

	@Nullable
	public RenderingClass getSelectedRenderingClass() {
		return selectedRenderingClass;
	}

	public void setSelectedRenderingClass(@Nullable RenderingClass renderingClass) {
		this.selectedRenderingClass = renderingClass;
	}

	public void toggleRoutesType(@NonNull String attrName) {
		if (BICYCLE.getRenderingPropertyAttr().equals(attrName)) {
			toggleCycleRoutes();
		} else if (MTB.getRenderingPropertyAttr().equals(attrName)) {
			toggleMtbRoutes();
		} else if (HIKING.getRenderingPropertyAttr().equals(attrName)) {
			toggleHikingRoutes();
		} else if (ALPINE.getRenderingPropertyAttr().equals(attrName)) {
			toggleAlpineHikingRoutes();
		} else if (SKI_ROUTES.getRenderingPropertyAttr().equals(attrName)) {
			toggleSkiRoutes();
		} else {
			CommonPreference<Boolean> preference = settings.getCustomRenderBooleanProperty(attrName);
			preference.set(!preference.get());
		}
	}

	public boolean isRoutesTypeEnabled(@NonNull String attrName) {
		if (BICYCLE.getRenderingPropertyAttr().equals(attrName)) {
			return isCycleRoutesEnabled();
		} else if (MTB.getRenderingPropertyAttr().equals(attrName)) {
			return isMtbRoutesEnabled();
		} else if (HIKING.getRenderingPropertyAttr().equals(attrName)) {
			return isHikingRoutesEnabled();
		} else if (ALPINE.getRenderingPropertyAttr().equals(attrName)) {
			return isAlpineHikingRoutesEnabled();
		}  else if (SKI_ROUTES.getRenderingPropertyAttr().equals(attrName)) {
			return isSkiRoutesEnabled();
		} else {
			return settings.getCustomRenderBooleanProperty(attrName).get();
		}
	}

	@NonNull
	public String getRoutesTypeName(@NonNull String attrName) {
		if (BICYCLE.getRenderingPropertyAttr().equals(attrName)) {
			return app.getString(R.string.rendering_attr_showCycleRoutes_name);
		} else if (MTB.getRenderingPropertyAttr().equals(attrName)) {
			return app.getString(R.string.app_mode_mountain_bicycle);
		} else if (HIKING.getRenderingPropertyAttr().equals(attrName)) {
			return app.getString(R.string.rendering_attr_hikingRoutesOSMC_name);
		} else if (ALPINE.getRenderingPropertyAttr().equals(attrName)) {
			return app.getString(R.string.rendering_attr_alpineHiking_name);
		} else if (SKI_ROUTES.getRenderingPropertyAttr().equals(attrName)) {
			return app.getString(R.string.help_article_navigation_routing_ski_routing_name);
		}
		return AndroidUtils.getRenderingStringPropertyName(app, attrName, attrName);
	}

	@NonNull
	public String getRoutesTypeDescription(@NonNull String attrName) {
		boolean enabled = isRoutesTypeEnabled(attrName);
		if (MTB.getRenderingPropertyAttr().equals(attrName)) {
			return enabled ? getSelectedMtbClassificationName(app) : app.getString(R.string.shared_string_disabled);
		} else if (ALPINE.getRenderingPropertyAttr().equals(attrName)) {
			return getDifficultyClassificationDescription(app);
		} else {
			return app.getString(enabled ? R.string.shared_string_enabled : R.string.shared_string_disabled);
		}
	}


	// Cycle routes
	public void toggleCycleRoutes() {
		toggleCycleRoutes(!cycleRoutesPreference.get());
	}

	public void toggleCycleRoutes(boolean enabled) {
		if (enabled) {
			cycleRoutesPreference.set(true);
			cycleRoutesNodeNetworkPreference.set(cycleRoutesLastNodeNetworkState.get());
		} else {
			cycleRoutesLastNodeNetworkState.set(cycleRoutesNodeNetworkPreference.get());
			cycleRoutesPreference.set(false);
			cycleRoutesNodeNetworkPreference.set(false);
		}
	}

	public void toggleCycleRoutesNodeNetwork(boolean enabled) {
		cycleRoutesNodeNetworkPreference.set(enabled);
		cycleRoutesLastNodeNetworkState.set(enabled);
	}

	public boolean isCycleRoutesEnabled() {
		return cycleRoutesPreference.get();
	}

	public boolean isCycleRoutesNodeNetworkEnabled() {
		return cycleRoutesNodeNetworkPreference.get();
	}


	// Mountain bike routes
	public void toggleMtbRoutes() {
		toggleMtbRoutes(!mtbRoutesPreference.get());
	}

	public void toggleMtbRoutes(boolean enabled) {
		mtbRoutesPreference.set(enabled);
		updateSelectedMtbClassification(getSelectedMtbClassificationId());
	}

	public boolean isMtbRoutesEnabled() {
		return mtbRoutesPreference.get();
	}

	public void updateSelectedMtbClassification(@Nullable String classificationId) {
		if (classificationId != null) {
			mtbRoutesLastClassification.set(classificationId);
		}
		for (MtbClassification classification : MtbClassification.values()) {
			boolean selected = Objects.equals(classification.attrName, classificationId);
			settings.getCustomRenderBooleanProperty(classification.attrName).set(selected);
		}
	}

	@NonNull
	public String getSelectedMtbClassificationName(@NonNull Context context) {
		MtbClassification classification = getSelectedMtbClassification();
		return classification != null ? context.getString(classification.nameId) : "";
	}

	@NonNull
	public MtbClassification getSelectedMtbClassification() {
		String selectedId = getSelectedMtbClassificationId();
		for (MtbClassification classification : MtbClassification.values()) {
			if (Objects.equals(classification.attrName, selectedId)) {
				return classification;
			}
		}
		return null;
	}

	@Nullable
	public String getSelectedMtbClassificationId() {
		return isMtbRoutesEnabled() ? mtbRoutesLastClassification.get() : null;
	}


	// Hiking routes
	public void toggleHikingRoutes() {
		toggleHikingRoutes(!isHikingRoutesEnabled());
	}

	public void toggleHikingRoutes(boolean enabled) {
		updateHikingRoutesValue(enabled ? getPreviousHikingRoutesValue() : "");
	}

	public void updateHikingRoutesValue(@NonNull String value) {
		if (!Algorithms.isEmpty(value)) {
			hikingRoutesLastValue.set(value);
		}
		hikingRoutesPreference.set(value);
	}

	public boolean isHikingRoutesEnabled() {
		RenderingRuleProperty property = getHikingRenderingRuleProperty();
		return property != null && property.containsValue(hikingRoutesPreference.get());
	}

	@NonNull
	public String getSelectedHikingRoutesValue() {
		return hikingRoutesPreference.get();
	}

	@NonNull
	private String getPreviousHikingRoutesValue() {
		String value = hikingRoutesLastValue.get();
		if (Algorithms.isEmpty(value)) {
			RenderingRuleProperty property = getHikingRenderingRuleProperty();
			value = property != null ? property.getPossibleValues()[0] : null;
		}
		return value != null ? value : "";
	}

	@Nullable
	private RenderingRuleProperty getHikingRenderingRuleProperty() {
		String attrName = HIKING.getRenderingPropertyAttr();
		return ConfigureMapUtils.getPropertyForAttr(app, attrName);
	}

	public void toggleSkiRoutes() {
		toggleSkiRoutes(!isSkiRoutesEnabled());
	}

	public void toggleSkiRoutes(boolean enabled) {
		pisteRoutesPreference.set(enabled);
		showSkiSlopesPreference.set(enabled);
	}

	public boolean isSkiRoutesEnabled() {
		return pisteRoutesPreference.get();
	}

	// Alpine hiking routes
	public void toggleAlpineHikingRoutes() {
		toggleAlpineHikingRoutes(!isAlpineHikingRoutesEnabled());
	}

	public void toggleAlpineHikingRoutes(boolean enabled) {
		alpineHikingPreference.set(enabled);
	}

	public boolean isAlpineHikingRoutesEnabled() {
		return alpineHikingPreference.get();
	}

	public void updateAlpineHikingScaleScheme(@NonNull String value) {
		alpineHikingScaleScheme.set(value);
	}

	@NonNull
	public String getSelectedAlpineHikingScaleScheme() {
		return alpineHikingScaleScheme.get();
	}
}
