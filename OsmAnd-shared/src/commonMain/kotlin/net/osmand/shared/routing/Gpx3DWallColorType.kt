package net.osmand.shared.routing

enum class Gpx3DWallColorType(val typeName: String, val displayNameResId: String) {

	NONE("none", "shared_string_none"),
	SOLID("solid", "track_coloring_solid"),
	DOWNWARD_GRADIENT("downward_gradient", "downward_gradient"),
	UPWARD_GRADIENT("upward_gradient", "upward_gradient"),
	ALTITUDE("altitude", "altitude"),
	SLOPE("slope", "shared_string_slope"),
	SPEED("speed", "shared_string_speed");


	companion object {
		fun get3DWallColorType(typeName: String?): Gpx3DWallColorType {
			for (type in Gpx3DWallColorType.entries) {
				if (type.typeName.equals(typeName, ignoreCase = true)) {
					return type
				}
			}
			return NONE
		}
	}

	open fun isGradient(): Boolean {
		return this == ALTITUDE || this == SLOPE || this == SPEED
	}

	open fun isVerticalGradient(): Boolean {
		return this == UPWARD_GRADIENT || this == DOWNWARD_GRADIENT
	}

}