package net.osmand.shared.palette.domain

import net.osmand.shared.util.Localization

/**
 * Determines how gradient values are interpreted.
 */
enum class GradientRangeType(
	private val iconResId: String,
	private val titleResId: String,
	private val summaryResId: String
) {
	/**
	 * Values are relative (0.0 to 1.0) and displayed as percentage (0% .. 100%).
	 * Colors are distributed proportionally across the min/max range of the data.
	 */
	RELATIVE(
		iconResId = "ic_action_percent",
		titleResId = "gradient_range_type_relative",
		summaryResId = "gradient_range_type_relative_summary"
	),

	/**
	 * Values are absolute.
	 * Colors are assigned to specific fixed numbers (e.g. 50 km/h).
	 */
	FIXED_VALUES(
		iconResId = "ic_action_123",
		titleResId = "gradient_range_type_fixed",
		summaryResId = "gradient_range_type_fixed_summary"
	);

	fun getIconName() = iconResId

	fun getTitle(): String = Localization.getString(titleResId)

	fun getSummary(): String = Localization.getString(summaryResId)
}