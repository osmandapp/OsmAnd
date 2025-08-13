package net.osmand.plus.routing;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.shared.gpx.ColoringPurpose;
import net.osmand.shared.gpx.GradientScaleType;
import net.osmand.shared.routing.Gpx3DWallColorType;
import net.osmand.shared.routing.RouteColorize.ColorizationType;
import net.osmand.util.Algorithms;

import java.util.Arrays;

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

	private static final ColoringType[] ROUTE_TYPES = new ColoringType[] {
			DEFAULT, CUSTOM_COLOR, ALTITUDE, SLOPE, ATTRIBUTE
	};
	private static final ColoringType[] TRACK_TYPES = new ColoringType[] {
			TRACK_SOLID, SPEED, ALTITUDE, SLOPE, ATTRIBUTE
	};

	private final String id;
	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;

	ColoringType(@NonNull String id, int titleId, int iconId) {
		this.id = id;
		this.titleId = titleId;
		this.iconId = iconId;
	}

	@NonNull
	public String getId() {
		return id;
	}

	@Nullable
	public String getName(@Nullable String routeInfoAttribute) {
		if (!isRouteInfoAttribute()) {
			return id;
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
	public ColorizationType toColorizationType() {
		if (this == SPEED) {
			return ColorizationType.SPEED;
		} else if (this == ALTITUDE) {
			return ColorizationType.ELEVATION;
		} else if (this == SLOPE) {
			return ColorizationType.SLOPE;
		} else {
			return null;
		}
	}

	@Nullable
	public static String getRouteInfoAttribute(@Nullable String name) {
		return !Algorithms.isEmpty(name) && name.startsWith(RouteStatisticsHelper.ROUTE_INFO_PREFIX) ?
				name : null;
	}

	@Nullable
	public static ColoringType valueOf(@Nullable GradientScaleType type) {
		if (type == GradientScaleType.SPEED) {
			return SPEED;
		} else if (type == GradientScaleType.ALTITUDE) {
			return ALTITUDE;
		} else if (type == GradientScaleType.SLOPE) {
			return SLOPE;
		}
		return null;
	}

	@Nullable
	public static ColoringType valueOf(@Nullable Gpx3DWallColorType type) {
		if (type == Gpx3DWallColorType.SPEED) {
			return SPEED;
		} else if (type == Gpx3DWallColorType.ALTITUDE) {
			return ALTITUDE;
		} else if (type == Gpx3DWallColorType.SLOPE) {
			return SLOPE;
		} else if (type == Gpx3DWallColorType.SOLID) {
			return TRACK_SOLID;
		}
		return null;
	}

	@NonNull
	public static ColoringType requireValueOf(@NonNull ColoringPurpose purpose) {
		return requireValueOf(purpose, null);
	}

	@NonNull
	public static ColoringType requireValueOf(@NonNull ColoringPurpose purpose, @Nullable String name) {
		ColoringType defaultValue = purpose == ColoringPurpose.ROUTE_LINE ? DEFAULT : TRACK_SOLID;
		return valueOf(purpose, name, defaultValue);
	}

	@NonNull
	public static ColoringType valueOf(@NonNull ColoringPurpose purpose, @Nullable String name,
	                                   @NonNull ColoringType defaultValue) {
		ColoringType value = valueOf(purpose, name);
		return value != null ? value : defaultValue;
	}

	@Nullable
	public static ColoringType valueOf(@NonNull ColoringPurpose purpose, @Nullable String name) {
		if (!Algorithms.isEmpty(getRouteInfoAttribute(name))) {
			return ATTRIBUTE;
		}
		for (ColoringType coloringType : valuesOf(purpose)) {
			if (coloringType.id.equalsIgnoreCase(name) && coloringType != ATTRIBUTE) {
				return coloringType;
			}
		}
		return null;
	}

	@NonNull
	public static ColoringType[] valuesOf(@NonNull ColoringPurpose purpose) {
		return purpose == ColoringPurpose.TRACK ? TRACK_TYPES : ROUTE_TYPES;
	}

	public static boolean isColorTypeInPurpose(@NonNull ColoringType type, @NonNull ColoringPurpose purpose) {
		return Arrays.asList(valuesOf(purpose)).contains(type);
	}
}