package net.osmand.plus.routing;

import android.content.Context;

import net.osmand.plus.R;
import net.osmand.plus.track.GradientScaleType;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;


public enum RouteColoringType {

	DEFAULT("default", R.string.map_widget_renderer, R.drawable.ic_action_map_style),
	CUSTOM_COLOR("custom_color", R.string.shared_string_custom, R.drawable.ic_action_settings),
	ALTITUDE("altitude", R.string.altitude, R.drawable.ic_action_hillshade_dark),
	SLOPE("slope", R.string.shared_string_slope, R.drawable.ic_action_altitude_ascent),
	ROAD_TYPE("road_type", R.string.routeInfo_roadClass_name, R.drawable.ic_action_hillshade_dark),
	SURFACE("surface", R.string.routeInfo_surface_name, R.drawable.ic_action_hillshade_dark),
	SMOOTHNESS("smoothness", R.string.routeInfo_smoothness_name, R.drawable.ic_action_hillshade_dark),
	STEEPNESS("steepness", R.string.routeInfo_steepness_name, R.drawable.ic_action_hillshade_dark),
	WINTER_ICE_ROAD("winter_ice_road", R.string.routeInfo_winter_ice_road_name, R.drawable.ic_action_hillshade_dark),
	TRACK_TYPE("track_type", R.string.routeInfo_tracktype_name, R.drawable.ic_action_hillshade_dark);

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
	public String getHumanString(@NonNull Context context) {
		return context.getString(titleId);
	}

	public boolean isDefault() {
		return this == DEFAULT;
	}

	public boolean isCustomColor() {
		return this == CUSTOM_COLOR;
	}

	public boolean isGradient() {
		return this == ALTITUDE || this == SLOPE;
	}

	public boolean isPaid() {
		return this == ROAD_TYPE || this == SURFACE || this == SMOOTHNESS || this == STEEPNESS
				|| this == WINTER_ICE_ROAD || this == TRACK_TYPE;
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
				return DEFAULT;
			}
		}
		return DEFAULT;
	}
}