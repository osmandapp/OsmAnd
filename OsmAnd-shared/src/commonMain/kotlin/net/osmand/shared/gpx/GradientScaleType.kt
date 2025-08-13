package net.osmand.shared.gpx

import net.osmand.shared.routing.RouteColorize
import net.osmand.shared.util.Localization

enum class GradientScaleType(
	val typeName: String,
	val colorTypeName: String,
	val resId: String,
	val iconId: String
) {
	SPEED(
		"speed",
		"gradient_speed_color",
		"shared_string_speed",
		"ic_action_speed"
	),
	ALTITUDE(
		"altitude",
		"gradient_altitude_color",
		"altitude",
		"ic_action_altitude_average"
	),
	SLOPE(
		"slope",
		"gradient_slope_color",
		"shared_string_slope",
		"ic_action_altitude_ascent"
	);

	fun getHumanString(): String = Localization.getString(resId)

	fun toColorizationType(): RouteColorize.ColorizationType {
		return when (this) {
			SPEED -> RouteColorize.ColorizationType.SPEED
			ALTITUDE -> RouteColorize.ColorizationType.ELEVATION
			SLOPE -> RouteColorize.ColorizationType.SLOPE
			else -> RouteColorize.ColorizationType.NONE
		}
	}

	companion object {
		fun getGradientTypeByName(name: String?): GradientScaleType? {
			return entries.find { it.name.equals(name, ignoreCase = true) }
		}
	}
}
