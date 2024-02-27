package net.osmand.plus.routing;

import static net.osmand.plus.routing.ColoringStyleAlgorithms.isAttributeAvailableForDrawing;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum ColoringType {

	// For route only
	DEFAULT("default", R.string.map_widget_renderer, R.drawable.ic_action_map_style),
	CUSTOM_COLOR("custom_color", R.string.shared_string_custom, R.drawable.ic_action_settings),
	// For gpx track only
	TRACK_SOLID("solid", R.string.track_coloring_solid, R.drawable.ic_action_circle),
	SPEED("speed", R.string.shared_string_speed, R.drawable.ic_action_speed),
	// For both route and gpx file
	ALTITUDE("altitude", R.string.altitude, R.drawable.ic_action_hillshade_dark),
	SLOPE("slope", R.string.shared_string_slope, R.drawable.ic_action_altitude_ascent),
	ATTRIBUTE("attribute", R.string.attribute, R.drawable.ic_action_hillshade_dark);

	private static final List<ColoringType> ROUTE_COLORING_TYPES = new ArrayList<>();
	private static final List<ColoringType> TRACK_COLORING_TYPES = new ArrayList<>();

	static {
		ROUTE_COLORING_TYPES.add(DEFAULT);
		ROUTE_COLORING_TYPES.add(CUSTOM_COLOR);
		ROUTE_COLORING_TYPES.add(ALTITUDE);
		ROUTE_COLORING_TYPES.add(SLOPE);
		ROUTE_COLORING_TYPES.add(ATTRIBUTE);

		TRACK_COLORING_TYPES.add(TRACK_SOLID);
		TRACK_COLORING_TYPES.add(SPEED);
		TRACK_COLORING_TYPES.add(ALTITUDE);
		TRACK_COLORING_TYPES.add(SLOPE);
		TRACK_COLORING_TYPES.add(ATTRIBUTE);
	}

	private final String name;
	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;

	ColoringType(@NonNull String name, int titleId, int iconId) {
		this.name = name;
		this.titleId = titleId;
		this.iconId = iconId;
	}

	@Nullable
	public String getName(@Nullable String routeInfoAttribute) {
		if (!isRouteInfoAttribute()) {
			return name;
		} else {
			return Algorithms.isEmpty(routeInfoAttribute) ? null : routeInfoAttribute;
		}
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

	public boolean isTrackSolid() {
		return this == TRACK_SOLID;
	}

	public boolean isSolidSingleColor() {
		return isDefault() || isCustomColor() || isTrackSolid();
	}

	public boolean isGradient() {
		return this == SPEED || this == ALTITUDE || this == SLOPE;
	}

	public boolean isRouteInfoAttribute() {
		return this == ATTRIBUTE;
	}

	public boolean isAvailableForDrawingRoute(@NonNull OsmandApplication app,
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
			return !Algorithms.isEmpty(route.getOriginalRoute())
					&& isAttributeAvailableForDrawing(app, route.getOriginalRoute(), attributeName);
		}

		return true;
	}

	public boolean isAvailableInSubscription(@NonNull OsmandApplication app,
	                                         @Nullable String attributeName, boolean route) {
		if ((isRouteInfoAttribute() && route) || this == SLOPE) {
			return InAppPurchaseUtils.isColoringTypeAvailable(app);
		}
		return true;
	}
	@Nullable
	public GradientScaleType toGradientScaleType() {
		if (this == SPEED) {
			return GradientScaleType.SPEED;
		} else if (this == ALTITUDE) {
			return GradientScaleType.ALTITUDE;
		} else if (this == SLOPE) {
			return GradientScaleType.SLOPE;
		} else {
			return null;
		}
	}

	@Nullable
	public static ColoringType fromGradientScaleType(@Nullable GradientScaleType scaleType) {
		if (scaleType == GradientScaleType.SPEED) {
			return SPEED;
		} else if (scaleType == GradientScaleType.ALTITUDE) {
			return ALTITUDE;
		} else if (scaleType == GradientScaleType.SLOPE) {
			return SLOPE;
		}
		return null;
	}

	@Nullable
	public static String getRouteInfoAttribute(@Nullable String name) {
		return !Algorithms.isEmpty(name) && name.startsWith(RouteStatisticsHelper.ROUTE_INFO_PREFIX) ?
				name : null;
	}

	@NonNull
	public static ColoringType getRouteColoringTypeByName(@Nullable String name) {
		ColoringType defined = getColoringTypeByName(ROUTE_COLORING_TYPES, name);
		return defined == null ? DEFAULT : defined;
	}

	@NonNull
	public static ColoringType getNonNullTrackColoringTypeByName(@Nullable String name) {
		ColoringType defined = getColoringTypeByName(TRACK_COLORING_TYPES, name);
		return defined == null ? TRACK_SOLID : defined;
	}

	@Nullable
	public static ColoringType getNullableTrackColoringTypeByName(@Nullable String name) {
		return getColoringTypeByName(TRACK_COLORING_TYPES, name);
	}

	@Nullable
	private static ColoringType getColoringTypeByName(List<ColoringType> from, @Nullable String name) {
		if (!Algorithms.isEmpty(getRouteInfoAttribute(name))) {
			return ATTRIBUTE;
		}
		for (ColoringType coloringType : from) {
			if (coloringType.name.equalsIgnoreCase(name) && coloringType != ATTRIBUTE) {
				return coloringType;
			}
		}
		return null;
	}

	public static List<ColoringType> getRouteColoringTypes() {
		return Collections.unmodifiableList(ROUTE_COLORING_TYPES);
	}

	public static List<ColoringType> getTrackColoringTypes() {
		return Collections.unmodifiableList(TRACK_COLORING_TYPES);
	}
}