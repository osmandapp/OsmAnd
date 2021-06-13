package net.osmand.plus.routing;

import android.content.Context;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;


public enum RouteColoringType {

	DEFAULT("default", null, R.string.map_widget_renderer, R.drawable.ic_action_map_style),
	CUSTOM_COLOR("custom_color", null, R.string.shared_string_custom, R.drawable.ic_action_settings),
	ALTITUDE("altitude", null, R.string.altitude, R.drawable.ic_action_hillshade_dark),
	SLOPE("slope", null, R.string.shared_string_slope, R.drawable.ic_action_altitude_ascent),
	ROAD_TYPE("road_type", "routeInfo_roadClass", R.string.routeInfo_roadClass_name, R.drawable.ic_action_hillshade_dark),
	SURFACE("surface", "routeInfo_surface", R.string.routeInfo_surface_name, R.drawable.ic_action_hillshade_dark),
	SMOOTHNESS("smoothness", "routeInfo_smoothness", R.string.routeInfo_smoothness_name, R.drawable.ic_action_hillshade_dark),
	STEEPNESS("steepness", "routeInfo_steepness", R.string.routeInfo_steepness_name, R.drawable.ic_action_hillshade_dark),
	WINTER_ICE_ROAD("winter_ice_road", "routeInfo_winter_ice_road", R.string.routeInfo_winter_ice_road_name, R.drawable.ic_action_hillshade_dark),
	TRACK_TYPE("track_type", "routeInfo_tracktype", R.string.routeInfo_tracktype_name, R.drawable.ic_action_hillshade_dark);

	private final String name;
	private final String attrName;
	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;

	RouteColoringType(String name, String attrName, int titleId, int iconId) {
		this.name = name;
		this.attrName = attrName;
		this.titleId = titleId;
		this.iconId = iconId;
	}

	@NonNull
	public String getName() {
		return name;
	}

	@Nullable
	public String getAttrName() {
		return attrName;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@NonNull
	public String getHumanString(@NonNull Context context) {
		return context.getString(titleId);
	}

	public boolean isDefault() {
		return this == DEFAULT;
	}

	public boolean isCustomColor() {
		return this == CUSTOM_COLOR;
	}

	public boolean isSolidSingleColor() {
		return isDefault() || isCustomColor();
	}

	public boolean isGradient() {
		return this == ALTITUDE || this == SLOPE;
	}

	public boolean isRouteInfoAttribute() {
		return this == ROAD_TYPE || this == SURFACE || this == SMOOTHNESS || this == STEEPNESS
				|| this == WINTER_ICE_ROAD || this == TRACK_TYPE;
	}

	public boolean isAvailableForDrawing(@NonNull OsmandApplication app) {
		RouteCalculationResult route = app.getRoutingHelper().getRoute();
		if (isGradient()) {
			List<Location> locations = route.getImmutableAllLocations();
			for (Location location : locations) {
				if (location.hasAltitude()) {
					return true;
				}
			}
			return false;
		}

		if (isRouteInfoAttribute()) {
			List<RouteSegmentResult> routeSegments = route.getOriginalRoute();
			if (Algorithms.isEmpty(routeSegments)) {
				return false;
			}

			RenderingRulesStorage currentRenderer = app.getRendererRegistry().getCurrentSelectedRenderer();
			RenderingRulesStorage defaultRenderer = app.getRendererRegistry().defaultRender();
			List<String> attrs = RouteStatisticsHelper.getRouteStatisticAttrsNames(currentRenderer, defaultRenderer);
			if (Algorithms.isEmpty(attrs) || !attrs.contains(attrName)) {
				return false;
			}

			boolean night = app.getDaynightHelper().isNightModeForMapControls();
			MapRenderRepositories maps = app.getResourceManager().getRenderer();
			RenderingRuleSearchRequest currentSearchRequest =
					maps.getSearchRequestWithAppliedCustomRules(currentRenderer, night);
			RenderingRuleSearchRequest defaultSearchRequest =
					maps.getSearchRequestWithAppliedCustomRules(defaultRenderer, night);
			List<RouteStatisticsHelper.RouteStatistics> routeStatisticsList =
					RouteStatisticsHelper.calculateRouteStatistic(routeSegments,
							Collections.singletonList(attrName), currentRenderer,
							defaultRenderer, currentSearchRequest, defaultSearchRequest);
			return !Algorithms.isEmpty(routeStatisticsList);
		}

		return true;
	}

	@Nullable
	public GradientScaleType toGradientScaleType() {
		if (this == ALTITUDE) {
			return GradientScaleType.ALTITUDE;
		} else if (this == SLOPE) {
			return GradientScaleType.SLOPE;
		} else {
			return null;
		}
	}

	@NonNull
	public static RouteColoringType getColoringTypeByName(@Nullable String name) {
		for (RouteColoringType coloringType : RouteColoringType.values()) {
			if (coloringType.name.equalsIgnoreCase(name)) {
				return coloringType;
			}
		}
		return DEFAULT;
	}
}