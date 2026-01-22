package net.osmand.plus.palette.domain

/**
 * Determines how gradient values are interpreted.
 */
enum class GradientRangeType {
	/**
	 * Values are relative (0.0 to 1.0) and displayed as percentage (0% .. 100%).
	 * Colors are distributed proportionally across the min/max range of the data.
	 */
	RELATIVE,

	/**
	 * Values are absolute.
	 * Colors are assigned to specific fixed numbers (e.g. 50 km/h).
	 */
	FIXED_VALUES
}