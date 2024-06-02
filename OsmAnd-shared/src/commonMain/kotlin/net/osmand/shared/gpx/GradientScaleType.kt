package net.osmand.shared.gpx

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

	/* TODO
	fun toColorizationType(): ColorizationType {
		return when (this) {
			SPEED -> ColorizationType.SPEED
			ALTITUDE -> ColorizationType.ELEVATION
			SLOPE -> ColorizationType.SLOPE
		}
	}
	 */

	companion object {
		fun getGradientTypeByName(name: String?): GradientScaleType? {
			return entries.find { it.name.equals(name, ignoreCase = true) }
		}
	}
}
