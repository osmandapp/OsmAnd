package net.osmand.plus.routing;

import android.content.Context;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;


public enum RouteColoringType {

	DEFAULT("default", R.string.map_widget_renderer, R.drawable.ic_action_map_style),
	CUSTOM_COLOR("custom_color", R.string.shared_string_custom, R.drawable.ic_action_settings),
	ALTITUDE("altitude", R.string.altitude, R.drawable.ic_action_hillshade_dark),
	SLOPE("slope", R.string.shared_string_slope, R.drawable.ic_action_altitude_ascent),
	ATTRIBUTE("attribute", R.string.attribute, R.drawable.ic_action_hillshade_dark);

	private final String name;
	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;

	RouteColoringType(String name, int titleId, int iconId) {
		this.name = name;
		this.titleId = titleId;
		this.iconId = iconId;
	}

	@NonNull
	public String getName() {
		return name;
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
	public String getHumanString(@NonNull Context context, @Nullable String routeInfoAttribute) {
		return isRouteInfoAttribute()
				? getHumanStringRouteInfoAttribute(context, routeInfoAttribute)
				: context.getString(titleId);
	}

	@NonNull
	private String getHumanStringRouteInfoAttribute(@NonNull Context context, @Nullable String routeInfoAttribute) {
		String routeInfoPrefix = RouteStatisticsHelper.ROUTE_INFO_PREFIX;
		if (!isRouteInfoAttribute() || routeInfoAttribute == null
				|| !routeInfoAttribute.startsWith(routeInfoPrefix)) {
			return "";
		}
		return AndroidUtils.getStringRouteInfoPropertyValue(context, routeInfoAttribute.replace(routeInfoPrefix, ""));
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
		return this == ATTRIBUTE;
	}

	public boolean isAvailableForDrawing(@NonNull OsmandApplication app,
	                                     @NonNull RouteCalculationResult route,
	                                     @Nullable String attributeName) {
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
			return isAttributeAvailableForDrawing(app, route, attributeName);
		}

		return true;
	}

	private boolean isAttributeAvailableForDrawing(@NonNull OsmandApplication app,
	                                               @NonNull RouteCalculationResult route,
	                                               @Nullable String attributeName) {
		List<RouteSegmentResult> routeSegments = route.getOriginalRoute();
		if (Algorithms.isEmpty(routeSegments) || Algorithms.isEmpty(attributeName)) {
			return false;
		}

		RenderingRulesStorage currentRenderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		RenderingRulesStorage defaultRenderer = app.getRendererRegistry().defaultRender();
		List<String> rendererAttrs = RouteStatisticsHelper
				.getRouteStatisticAttrsNames(currentRenderer, defaultRenderer, true);
		if (Algorithms.isEmpty(rendererAttrs) || !rendererAttrs.contains(attributeName)) {
			return false;
		}

		boolean night = app.getDaynightHelper().isNightModeForMapControls();
		MapRenderRepositories maps = app.getResourceManager().getRenderer();
		RenderingRuleSearchRequest currentSearchRequest =
				maps.getSearchRequestWithAppliedCustomRules(currentRenderer, night);
		RenderingRuleSearchRequest defaultSearchRequest =
				maps.getSearchRequestWithAppliedCustomRules(defaultRenderer, night);

		List<RouteStatistics> routeStatisticsList =
				RouteStatisticsHelper.calculateRouteStatistic(routeSegments,
						Collections.singletonList(attributeName), currentRenderer,
						defaultRenderer, currentSearchRequest, defaultSearchRequest);
		return !Algorithms.isEmpty(routeStatisticsList);
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
		if (!Algorithms.isEmpty(name) && name.startsWith(RouteStatisticsHelper.ROUTE_INFO_PREFIX)) {
			return ATTRIBUTE;
		}
		for (RouteColoringType coloringType : RouteColoringType.values()) {
			if (coloringType.name.equalsIgnoreCase(name)) {
				return coloringType;
			}
		}
		return DEFAULT;
	}
}