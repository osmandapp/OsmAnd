package net.osmand.shared.routing

import net.osmand.shared.gpx.ColoringPurpose
import net.osmand.shared.gpx.GradientScaleType

enum class ColoringType(val id: String, val titleId: String, val iconId: String) {
	// For route only
	DEFAULT("default", "map_widget_renderer", "ic_action_map_style"),
	CUSTOM_COLOR("custom_color", "shared_string_custom", "ic_action_settings"),
	// For gpx track only
	TRACK_SOLID("solid", "track_coloring_solid", "ic_action_circle"),
	SPEED("speed", "shared_string_speed", "ic_action_speed"),
	// For both route and gpx file
	ALTITUDE("altitude", "altitude", "ic_action_hillshade_dark"),
	SLOPE("slope", "shared_string_slope", "ic_action_altitude_ascent"),
	ATTRIBUTE("attribute", "attribute", "ic_action_hillshade_dark");

	companion object {
		val ROUTE_TYPES = arrayOf(DEFAULT, CUSTOM_COLOR, ALTITUDE, SLOPE, ATTRIBUTE)
		val TRACK_TYPES = arrayOf(TRACK_SOLID, SPEED, ALTITUDE, SLOPE, ATTRIBUTE)

		fun getRouteInfoAttribute(name: String?): String? {
			return if (!name.isNullOrEmpty() && name.startsWith(RouteStatisticsHelper.ROUTE_INFO_PREFIX)) {
				name
			} else {
				null
			}
		}

		fun valueOf(scaleType: GradientScaleType?): ColoringType? {
			return when (scaleType) {
				GradientScaleType.SPEED -> SPEED
				GradientScaleType.ALTITUDE -> ALTITUDE
				GradientScaleType.SLOPE -> SLOPE
				else -> null
			}
		}

		fun requireValueOf(purpose: ColoringPurpose, name: String? = null): ColoringType {
			val defaultValue = if (purpose == ColoringPurpose.ROUTE_LINE) DEFAULT else TRACK_SOLID
			return valueOf(purpose, name, defaultValue)
		}

		fun valueOf(purpose: ColoringPurpose, name: String?, defaultValue: ColoringType): ColoringType {
			return valueOf(purpose, name) ?: defaultValue
		}

		fun valueOf(purpose: ColoringPurpose, name: String?): ColoringType? {
			if (!getRouteInfoAttribute(name).isNullOrEmpty()) {
				return ATTRIBUTE
			}
			for (coloringType in valuesOf(purpose)) {
				if (coloringType.id.equals(name, ignoreCase = true) && coloringType != ATTRIBUTE) {
					return coloringType
				}
			}
			return null
		}

		fun valuesOf(purpose: ColoringPurpose): Array<ColoringType> {
			return if (purpose == ColoringPurpose.TRACK) TRACK_TYPES else ROUTE_TYPES
		}

		fun valueOf(type: Gpx3DWallColorType?): ColoringType? {
			return when (type) {
				Gpx3DWallColorType.SPEED -> SPEED
				Gpx3DWallColorType.ALTITUDE -> return ALTITUDE
				Gpx3DWallColorType.SLOPE -> return SLOPE
				Gpx3DWallColorType.SOLID -> return TRACK_SOLID
				else -> return null
			}
		}

		fun isColorTypeInPurpose(
			type: ColoringType,
			purpose: ColoringPurpose): Boolean {
			return ColoringType.valuesOf(purpose).contains(type)
		}

	}

	fun getName(routeInfoAttribute: String?): String? {
		return if (!isRouteInfoAttribute()) {
			id
		} else {
			if (routeInfoAttribute.isNullOrEmpty()) null else routeInfoAttribute
		}
	}

	fun isDefault(): Boolean = this == DEFAULT

	fun isCustomColor(): Boolean = this == CUSTOM_COLOR

	fun isTrackSolid(): Boolean = this == TRACK_SOLID

	fun isSolidSingleColor(): Boolean = isDefault() || isCustomColor() || isTrackSolid()

	fun isGradient(): Boolean = this == SPEED || this == ALTITUDE || this == SLOPE

	fun isRouteInfoAttribute(): Boolean = this == ATTRIBUTE

	fun toGradientScaleType(): GradientScaleType? {
		return when (this) {
			SPEED -> GradientScaleType.SPEED
			ALTITUDE -> GradientScaleType.ALTITUDE
			SLOPE -> GradientScaleType.SLOPE
			else -> null
		}
	}

	open fun toColorizationType(): RouteColorize.ColorizationType? {
		return when(this){
			SPEED -> RouteColorize.ColorizationType.SPEED
			ALTITUDE -> RouteColorize.ColorizationType.ELEVATION
			SLOPE -> RouteColorize.ColorizationType.SLOPE
			else -> null
		}
	}

}

object RouteStatisticsHelper {
	const val ROUTE_INFO_PREFIX = "route_info_"
}
