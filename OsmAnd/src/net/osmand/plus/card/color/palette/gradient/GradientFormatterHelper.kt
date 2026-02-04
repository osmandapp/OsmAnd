package net.osmand.plus.card.color.palette.gradient

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import net.osmand.gpx.GpxParameter
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.gradient.editor.data.RelativeConstants
import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.shared.palette.domain.GradientRangeType
import net.osmand.shared.palette.domain.category.GradientPaletteCategory
import net.osmand.shared.palette.domain.filetype.GradientFileType
import java.text.DecimalFormat

object GradientFormatterHelper {

	private const val MAX_ALTITUDE_ADDITION = 50f
	private val decimalFormat = DecimalFormat("0.##")

	fun getGradientFormatter(
		app: OsmandApplication,
		fileType: GradientFileType,
		analysis: GpxTrackAnalysis?
	): IAxisValueFormatter {
		return IAxisValueFormatter { value, axis ->
			val isFirstValue = axis?.mEntries?.getOrNull(0) == value
			formatValue(
				app = app,
				value = value,
				fileType = fileType,
				showUnits = isFirstValue,
				analysis = analysis
			)
		}
	}

	/**
	 * Formats a single value based on FileType and Units.
	 */
	fun formatValue(
		app: OsmandApplication,
		value: Float,
		fileType: GradientFileType,
		showUnits: Boolean = true,
		analysis: GpxTrackAnalysis? = null
	): String {
		// --- NEW LOGIC: Check for Relative Constants (Min/Avg/Max) ---
		// Ми застосовуємо це тільки якщо немає analysis (режим редактора/прев'ю),
		// бо якщо analysis є, ми зазвичай хочемо бачити реальні значення (наприклад, 50 км/год), а не "Average".
		if (fileType.rangeType == GradientRangeType.RELATIVE && analysis == null) {
			val constant = RelativeConstants.valueOfRatio(value)
			if (constant != null) {
				// Повертаємо локалізовану назву (Min, Max) без юнітів
				return constant.getName(full = false)
			}
		}
		// -------------------------------------------------------------

		// 1. Calculate / Interpolate Real Value
		val valueToFormat = calculateRealValue(value, fileType, analysis)

		// 2. Format Number & Get Unit
		val (valueStr, unitStr) = formatValueAndUnit(valueToFormat, fileType, analysis)

		// 3. Combine
		return if (showUnits && unitStr.isNotEmpty()) {
			app.getString(R.string.ltr_or_rtl_combine_via_space, valueStr, unitStr)
		} else {
			valueStr
		}
	}

	private fun calculateRealValue(
		value: Float,
		fileType: GradientFileType,
		analysis: GpxTrackAnalysis?
	): Float {
//		if (fileType.rangeType == GradientRangeType.FIXED_VALUES) {
//			return value
//		}
//
//		if (analysis != null) {
//			return when (fileType.category) {
//				GradientPaletteCategory.SPEED, GradientPaletteCategory.MAX_SPEED -> {
//					if (analysis.maxSpeed != 0f) value * analysis.maxSpeed else value
//				}
//				GradientPaletteCategory.ALTITUDE -> {
//					val minEle = analysis.minElevation.toFloat()
//					val maxEle = analysis.maxElevation.toFloat() + MAX_ALTITUDE_ADDITION
//					if (minEle != GpxParameter.MIN_ELEVATION.defaultValue.toFloat() &&
//						maxEle != GpxParameter.MAX_ELEVATION.defaultValue.toFloat()
//					) {
//						minEle + (value * (maxEle - minEle))
//					} else {
//						value
//					}
//				}
//				else -> value
//			}
//		}

		return value
	}

	private fun formatValueAndUnit(
		value: Float,
		fileType: GradientFileType,
		analysis: GpxTrackAnalysis?
	): Pair<String, String> {

		// Relative Preview (without analysis) - fallback to simple decimals
		if (fileType.rangeType == GradientRangeType.RELATIVE && analysis == null) {
			// Якщо це не Min/Avg/Max (які ми обробили на початку), то просто форматуємо число (наприклад 33%)
			// Тут ми можемо додати логіку множення на 100 для відсотків, якщо треба
			val displayUnit = fileType.displayUnits
			val converted = displayUnit.from(value.toDouble(), fileType.baseUnits).toFloat()
			return decimalFormat.format(converted) to displayUnit.getSymbol()
		}

		val baseUnit = fileType.baseUnits
		val displayUnit = fileType.displayUnits

		val convertedValue = if (baseUnit != displayUnit) {
			displayUnit.from(value.toDouble(), baseUnit).toFloat()
		} else {
			value
		}

		val formattedValue = decimalFormat.format(convertedValue)
		val symbol = displayUnit.getSymbol()

		return formattedValue to symbol
	}
}