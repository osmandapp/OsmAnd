package net.osmand.plus.card.color.palette.gradient

import com.github.mikephil.charting.formatter.IAxisValueFormatter
import net.osmand.plus.card.color.palette.gradient.editor.data.RelativeConstants
import net.osmand.shared.gpx.GpxParameter.MAX_ELEVATION
import net.osmand.shared.gpx.GpxParameter.MIN_ELEVATION
import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.shared.gpx.filters.MeasureUnitType
import net.osmand.shared.gpx.filters.MeasureUnitType.ALTITUDE
import net.osmand.shared.gpx.filters.MeasureUnitType.SPEED
import net.osmand.shared.palette.domain.GradientRangeType
import net.osmand.shared.palette.domain.category.GradientPaletteCategory
import net.osmand.shared.palette.domain.filetype.GradientFileType
import net.osmand.shared.units.LengthUnits
import net.osmand.shared.units.MeasurementUnit
import net.osmand.shared.units.SpeedUnits
import net.osmand.shared.util.Localization
import java.text.DecimalFormat
import kotlin.math.abs

object GradientFormatter {

	private const val MAX_ALTITUDE_ADDITION = 50f

	private val decimalFormat = DecimalFormat("0.##")

	@JvmStatic
	fun getAxisFormatter(
		paletteCategory: GradientPaletteCategory
	): IAxisValueFormatter {
		return getAxisFormatter(paletteCategory.getFileType(), realDataLimits = null)
	}

	@JvmStatic
	fun getAxisFormatter(
		fileType: GradientFileType,
		analysis: GpxTrackAnalysis?
	): IAxisValueFormatter {
		val unitType = fileType.category.measureUnitType
		val limits = if (analysis != null) calculateRealDataLimits(analysis, unitType) else null
		return getAxisFormatter(fileType, limits)
	}

	@JvmStatic
	fun getAxisFormatter(
		fileType: GradientFileType,
		realDataLimits: RealDataLimits?
	): IAxisValueFormatter {
		return IAxisValueFormatter { value, axis ->
			// Use epsilon comparison to avoid float precision issues
			val isFirstValue =
				axis?.mEntries?.getOrNull(0)?.let { abs(it - value) < 0.001f } ?: false

			formatValue(
				value = value,
				fileType = fileType,
				showUnits = isFirstValue,
				realDataLimits = realDataLimits
			)
		}
	}

	fun formatValue(
		value: Float,
		fileType: GradientFileType,
		showUnits: Boolean,
		realDataLimits: RealDataLimits? = null
	): String {
		var displayUnits = fileType.displayUnits
		var baseUnits = fileType.baseUnits

		val valueStr: String
		val unitsSrt: String

		if (fileType.rangeType == GradientRangeType.RELATIVE) {
			if (realDataLimits != null) {
				// SCENARIO 1: Relative + Real Data (Contextual Preview)
				// We map the 0..1 ratio to real physical values (e.g., speed, altitude).

				// 1. Source: Track analysis data is always in SI Base Units (m/s, meters).
				baseUnits = realDataLimits.units

				// 2. Target: user's preferred units (km/h, mph) instead of "%".
				displayUnits = fileType.category.measureUnitType.getUnit()

				val range = realDataLimits.maxValue - realDataLimits.minValue
				val calculatedBaseValue = realDataLimits.minValue + (value * range)

				val valueInDisplayUnits = displayUnits.from(calculatedBaseValue.toDouble(), baseUnits)
				valueStr = decimalFormat.format(valueInDisplayUnits)
				unitsSrt = displayUnits.getSymbol()

			} else {
				// SCENARIO 2: Relative + No Data (Abstract Preview)
				// Show constants (Min/Max) or percentages.
				val constant = RelativeConstants.valueOfRatio(value)
				if (constant != null) {
					valueStr = constant.getName(useFullName = false)
					unitsSrt = ""
				} else {
					val valueInDisplayUnits = displayUnits.from(value.toDouble(), baseUnits)
					valueStr = decimalFormat.format(valueInDisplayUnits)
					unitsSrt = displayUnits.getSymbol()
				}
			}
		} else {
			// SCENARIO 3: Fixed Values
			// Direct conversion from stored base units to display units.
			val valueInDisplayUnits = displayUnits.from(value.toDouble(), baseUnits)
			valueStr = if (fileType.category.isTerrainRelated()) {
				formatTerrainTypeValues(valueInDisplayUnits.toFloat())
			} else {
				decimalFormat.format(valueInDisplayUnits)
			}
			unitsSrt = displayUnits.getSymbol()
		}

		return if (showUnits && unitsSrt.isNotEmpty()) {
			Localization.getString("ltr_or_rtl_combine_via_space", valueStr, unitsSrt)
		} else {
			valueStr
		}
	}

	fun formatSimpleValue(
		value: Float,
		fileType: GradientFileType
	): String {
		return if (fileType.category.isTerrainRelated()) {
			formatTerrainTypeValues(value)
		} else {
			decimalFormat.format(value)
		}
	}

	private fun formatTerrainTypeValues(value: Float): String {
		val format = DecimalFormat(if (value >= 10) "#" else "#.#")

		var formattedValue = format.format(value.toDouble())
		if (formattedValue.endsWith(".0")) {
			formattedValue = formattedValue.substring(0, formattedValue.length - 2)
		}
		return formattedValue
	}

	private fun calculateRealDataLimits(
		analysis: GpxTrackAnalysis,
		unitType: MeasureUnitType
	): RealDataLimits? {

		var result: RealDataLimits? = null

		when(unitType) {
			SPEED -> {
				if (analysis.maxSpeed > 0f) {
					result = RealDataLimits(
						minValue = analysis.minSpeed,
						maxValue = analysis.maxSpeed,
						units = SpeedUnits.METERS_PER_SECOND
					)
				}
			}

			ALTITUDE -> {
				val min = analysis.minElevation
				val max = analysis.maxElevation + MAX_ALTITUDE_ADDITION
				if (min != MIN_ELEVATION.defaultValue && max != MAX_ELEVATION.defaultValue) {
					result = RealDataLimits(
						minValue = min.toFloat(),
						maxValue = max.toFloat(),
						units = LengthUnits.METERS
					)
				}
			}

			else -> {
				// Other types not supported for real data preview yet
			}
		}
		return result
	}

	data class RealDataLimits(
		val minValue: Float,
		val maxValue: Float,
		val units: MeasurementUnit<*>
	)
}