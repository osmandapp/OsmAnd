package net.osmand.plus.plugins.topoprint.compose

import java.util.Locale

/**
 * Unified scale-bar generator: builds one survey-style bar row — a left
 * extension of one base unit in fine grey/white steps, then the main
 * section counting up from 0 at its start — for the metric and the imperial
 * unit systems ([appendMetricBar]/[appendImperialBar], drawn as the USGS
 * bar pair).
 *
 * The generator depends only on plain numbers (the print scale denominator
 * and the paper geometry), so bars can be generated for any paper/scale
 * combination independently of the map geometry — pure JVM, freely
 * reusable and testable.
 *
 * Shared SVG primitives ([text]/[fmt]/[fmt2]) live in this file as
 * package-level functions; the composer and the generator use the same
 * declarations.
 */
internal class ScaleBarGenerator(
	private val scaleDenominator: Int,
	private val paperWidthMm: Double,
	private val mapWidthMm: Double,
	private val bottomMarginMm: Double
) {
	/** One scale-bar row: base unit, fine extension step and label formatting. */
	private class ScaleBarSpec(
		val unitMeters: Double,
		val fineMeters: Double,
		/** Value label for a run of k base units, e.g. "2" (km) or "2000" (ft). */
		val unitLabel: (Int) -> String,
		/** Label at the middle of the fine extension (half a base unit). */
		val halfLabel: String,
		/** Unit caption at the right bar end. */
		val caption: String
	)

	/** Metric bar: the first row band, capped at half of the paper width. */
	fun appendMetricBar(sb: StringBuilder, topMm: Double, fontSize: Double, barHeight: Double) {
		appendRow(sb, metricSpec(), topMm, fontSize, barHeight,
			mapWidthMm / 3.0, paperWidthMm / 2.0)
	}

	/**
	 * Imperial mirror: the second row band. The bar may run up to the map
	 * width minus the compass (declination indicator) width, keeping a gap
	 * so the two never touch.
	 */
	fun appendImperialBar(sb: StringBuilder, topMm: Double, fontSize: Double, barHeight: Double) {
		appendRow(sb, imperialSpec(), topMm, fontSize, barHeight,
			mapWidthMm / 3.0, mapWidthMm - bottomMarginMm * 0.6 - bottomMarginMm * 0.1)
	}

	/**
	 * Draws one survey-style bar row: a left extension of one base unit in
	 * fine grey/white steps, then the main section counting up from 0 at
	 * its start. Labels sit above the bar, the unit caption below its right
	 * end. The division scheme follows the ground length of a third of the
	 * map width; the whole bar (extension + main section) takes at most
	 * [maxBarMm] — half of the paper width for the metric bar, the full map
	 * width for the imperial one (a mile bar is wide in paper terms and
	 * USGS sheets let it span most of the map). Labels: the fine extension
	 * reads unit / unit/2 / 0 (left to right) and the main section counts
	 * up from 0 at its start: every unit when it is under 5 units,
	 * otherwise only 1 and 5 (and 10 for a 10-unit main section).
	 */
	private fun appendRow(sb: StringBuilder, spec: ScaleBarSpec, topMm: Double,
	                      fontSize: Double, barHeight: Double,
	                      minBarMm: Double, maxBarMm: Double) {
		val textY = topMm - fontSize
		val unitY = topMm + fontSize * 1.5
		val unitMm = spec.unitMeters * 1000.0 / scaleDenominator
		val fineMm = spec.fineMeters * 1000.0 / scaleDenominator
		val needUnits = Math.ceil(minBarMm / unitMm).toInt().coerceAtLeast(1)
		val pageCapUnits = (maxBarMm / unitMm).toInt() - 1
		val mainUnits = when {
			needUnits >= 10 -> 10
			needUnits >= 5 -> 5
			else -> needUnits
		}.coerceIn(0, pageCapUnits.coerceAtLeast(0))
		val barWidth = unitMm * (1 + mainUnits)

		val segments = mutableListOf<Pair<Double, Double>>()
		// 0–1 unit drawn in fine steps (mm on paper, capped at the unit
		// paper length).
		var x = 0.0
		while (x < unitMm - 0.01) {
			val w = minOf(fineMm, unitMm - x)
			segments.add(Pair(x, x + w))
			x += w
		}
		for (k in 1..mainUnits) {
			segments.add(Pair(unitMm * k, unitMm * (k + 1)))
		}

		for ((idx, seg) in segments.withIndex()) {
			sb.append("    <rect x=\"${fmt(seg.first)}\" y=\"${fmt(topMm - barHeight)}\" ")
			sb.append("width=\"${fmt(seg.second - seg.first)}\" height=\"${fmt(barHeight)}\" stroke=\"#000000\" stroke-width=\"0.2\" ")
			sb.append("fill=\"${if (idx % 2 == 0) "#cccccc" else "none"}\"/>\n")
		}
		if (mainUnits == 0) {
			// Extension-only bar: nothing follows the extension, so it reads
			// like a plain ruler — 0 at the left edge, the base unit (the
			// bar's maximum) at its right end.
			sb.append(text("0", 0.0, textY, fontSize, "middle"))
			if (unitMm / 2.0 >= 4.0) {
				sb.append(text(spec.halfLabel, unitMm / 2.0, textY, fontSize, "middle"))
			}
			sb.append(text(spec.unitLabel(1), unitMm, textY, fontSize, "middle"))
		} else {
			sb.append(text(spec.unitLabel(1), 0.0, textY, fontSize, "middle"))
			if (unitMm / 2.0 >= 4.0) {
				sb.append(text(spec.halfLabel, unitMm / 2.0, textY, fontSize, "middle"))
			}
			sb.append(text("0", unitMm, textY, fontSize, "middle"))
			if (mainUnits < 5) {
				for (k in 1..mainUnits) {
					sb.append(text(spec.unitLabel(k), unitMm * (k + 1), textY, fontSize, "middle"))
				}
			} else {
				sb.append(text(spec.unitLabel(1), unitMm * 2, textY, fontSize, "middle"))
				sb.append(text(spec.unitLabel(5), unitMm * 6, textY, fontSize, "middle"))
				if (mainUnits == 10) {
					sb.append(text(spec.unitLabel(10), unitMm * 11, textY, fontSize, "middle"))
				}
			}
		}
		sb.append(text(spec.caption, barWidth, unitY, fontSize, "end"))
	}

	/**
	 * Metric bar. The division scheme follows the ground length of half the
	 * paper width: under 2 km → 10 m fine / 100 m unit with all labels in
	 * plain metres ("m"); otherwise → 100 m fine / 1 km unit
	 * ("1 / 0.5 / 0", "km"). A fine step under 2 mm on paper is multiplied
	 * by 5 (10→50 m, 100→500 m).
	 */
	private fun metricSpec(): ScaleBarSpec {
		val halfPaperMm = paperWidthMm / 2.0
		val halfGroundM = halfPaperMm * scaleDenominator / 1000.0
		val metricMode = halfGroundM < 2000.0
		val unitM = if (metricMode) 100L else 1000L
		var fineM = if (metricMode) 10L else 100L
		if (fineM * 1000.0 / scaleDenominator < 2.0) {
			fineM *= 5L
		}
		return ScaleBarSpec(
			unitMeters = unitM.toDouble(),
			fineMeters = fineM.toDouble(),
			unitLabel = { k -> if (metricMode) (k * unitM).toString() else trimNumber(k.toDouble()) },
			halfLabel = if (metricMode) (unitM / 2L).toString() else "0.5",
			caption = if (metricMode) "m" else "km")
	}

	/**
	 * Imperial mirror of the metric bar (USGS-style). USGS 7.5'-quad scales
	 * (1:24000/25000) prefer miles outright: the 1000-ft rung is skipped so
	 * they get a mile bar reading 1 → 0.5 → 0. Even larger scales reach
	 * miles naturally (the 1000-ft rung no longer fits in five sections),
	 * and the largest scales keep the 100/1000-ft rungs.
	 * Mile bars use whole base units (1/2/5×10ⁿ miles): the fine extension
	 * subdivides the unit into ten 0.1-unit steps. As in the metric bar, a
	 * fine step under 2 mm on paper is multiplied by 5.
	 */
	private fun imperialSpec(): ScaleBarSpec {
		val ftM = 0.3048
		val miM = 1609.344
		// USGS 7.5'-quad scales and smaller: skip the 1000-ft rung so the
		// mile rungs win (a 1000-ft bar would otherwise claim exactly five
		// main sections at 1:25000 on A4 portrait). Even larger scales reach
		// miles naturally once the 1000-ft paper length exceeds the five-
		// section budget; the largest scales keep the 100/1000-ft rungs.
		val ladder = if (scaleDenominator >= 24000)
			doubleArrayOf(100 * ftM, miM, 2 * miM,
					5 * miM, 10 * miM, 20 * miM, 50 * miM)
		else
			doubleArrayOf(100 * ftM, 1000 * ftM, miM, 2 * miM,
					5 * miM, 10 * miM, 20 * miM, 50 * miM)
		var unitMeters = ladder.last()
		for (candidate in ladder) {
			if (Math.ceil(mapWidthMm / 3.0 / (candidate * 1000.0 / scaleDenominator)) <= 5.0) {
				unitMeters = candidate
				break
			}
		}
		val unitFt = unitMeters / ftM
		val milesMode = unitFt >= 5279.5
		var fineMeters = unitMeters / 10.0
		if (fineMeters * 1000.0 / scaleDenominator < 2.0) {
			fineMeters *= 5.0
		}
		return ScaleBarSpec(
			unitMeters = unitMeters,
			fineMeters = fineMeters,
			unitLabel = { k ->
				if (milesMode) trimNumber(k * unitMeters / miM)
				else Math.round(k * unitFt).toString()
			},
			halfLabel = if (milesMode) trimNumber2(unitMeters / miM / 2.0)
				else Math.round(unitFt / 2.0).toString(),
			caption = if (milesMode) "mi" else "ft")
	}

	private fun trimNumber(value: Double): String {
		val formatted = String.format(Locale.US, "%.1f", value)
		return if (formatted.endsWith(".0")) formatted.substring(0, formatted.length - 2) else formatted
	}

	/** Like [trimNumber] but keeps a second decimal (0.25 stays 0.25). */
	private fun trimNumber2(value: Double): String {
		var formatted = String.format(Locale.US, "%.2f", value)
		while (formatted.endsWith("0")) {
			formatted = formatted.substring(0, formatted.length - 1)
		}
		return if (formatted.endsWith(".")) formatted.substring(0, formatted.length - 1) else formatted
	}
}

private const val FONT_FAMILY = "Noto Sans CJK SC, Noto Sans, sans-serif"

internal fun text(content: String, x: Double, y: Double, fontSize: Double, anchor: String,
                  color: String = "#000000"): String =
	"      <text x=\"${fmt(x)}\" y=\"${fmt(y)}\" fill=\"$color\" stroke=\"$color\" stroke-width=\"0.1\" " +
			"font-family=\"$FONT_FAMILY\" font-size=\"${fmt(fontSize)}\" text-anchor=\"$anchor\">$content</text>\n"

internal fun fmt(value: Double): String {
	var formatted = String.format(Locale.US, "%.4f", value)
	while (formatted.contains(".") && formatted.endsWith("0")) {
		formatted = formatted.substring(0, formatted.length - 1)
	}
	if (formatted.endsWith(".")) {
		formatted = formatted.substring(0, formatted.length - 1)
	}
	return if (formatted == "-0") "0" else formatted
}

internal fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)
