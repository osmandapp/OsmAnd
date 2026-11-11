package net.osmand.plus.plugins.topoprint.compose

import net.osmand.PlatformUtil
import org.apache.commons.logging.Log
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Composes the print-ready map SVG, ported from `topo_map_generator.py`
 * (`draw_frame`/`draw_grid`/`draw_label`/`draw_scale_bar`/`draw_compass`).
 *
 * All coordinates are paper millimetres; the root viewBox spans the paper.
 * The paper is true-north-up. The basemap raster is rendered Mercator-north-up
 * and therefore embedded axis-aligned, centred on the frame, at its true
 * georeferenced size (corner UTM spans x scale). Every grid line is
 * drawn by interpolating its true UTM value along the georeferenced raster
 * edges (top/bottom for easting lines, left/right for northing lines), so the
 * grid inherits the convergence directly from the corner coordinates - no
 * per-sheet rotation is applied to the map content.
 *
 * @param raster basemap source; `pngFileName` is the basemap file placed
 *               next to the SVG (embedded as a data URI by the pipeline)
 */
class SvgMapComposer(
	private val geometry: PrintGeometry,
	private val label: String,
	private val raster: Raster,
	private val createdTimeMillis: Long
) {

	/** Georeferenced basemap raster: true UTM coordinates of its four corners. */
	data class Raster(
		val pngFileName: String,
		val eastingTopLeft: Double,
		val northingTopLeft: Double,
		val eastingTopRight: Double,
		val northingTopRight: Double,
		val eastingBottomLeft: Double,
		val northingBottomLeft: Double,
		val eastingBottomRight: Double,
		val northingBottomRight: Double
	)

	private val params = geometry.params
	private val layout = params.layout
	private val k = geometry.kMm

	// Exact UTM spans of the raster in paper millimetres (top/bottom and
	// left/right edges averaged against the UTM non-linearity). Placed
	// axis-aligned and centred, the raster matches its true paper footprint
	// within the UTM point-scale factor (<=0.1mm at 1:25000).
	private val rasterWidthMm = ((raster.eastingTopRight - raster.eastingTopLeft +
			raster.eastingBottomRight - raster.eastingBottomLeft) / 2.0) * k
	private val rasterHeightMm = ((raster.northingTopLeft - raster.northingBottomLeft +
			raster.northingTopRight - raster.northingBottomRight) / 2.0) * k

	/** Placed basemap top-left corner in frame coordinates (set by [appendBasemapImage]). */
	private var imageTlX = 0.0
	private var imageTlY = 0.0

	/** Builds the complete SVG document text. */
	fun compose(): String {
		val sb = StringBuilder(1 shl 20)
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
		sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" ")
		sb.append("viewBox=\"0 0 ${fmt(layout.paperWidthMm)} ${fmt(layout.paperHeightMm)}\" ")
		sb.append("width=\"${fmt(layout.paperWidthMm)}mm\" height=\"${fmt(layout.paperHeightMm)}mm\">\n")
		appendClipDef(sb)
		appendTitle(sb)
		appendFrame(sb)
		appendScaleBar(sb)
		appendCompass(sb)
		sb.append("</svg>\n")
		return sb.toString()
	}

	private fun appendClipDef(sb: StringBuilder) {
		sb.append("  <defs><clipPath id=\"frame_clip\"><rect x=\"0\" y=\"0\" ")
		sb.append("width=\"${fmt(layout.mapWidthMm)}\" height=\"${fmt(layout.mapHeightMm)}\" fill=\"none\"/>")
		sb.append("</clipPath></defs>\n")
	}

	// ------------------------------------------------------------------- title

	private fun appendTitle(sb: StringBuilder) {
		val fontSize = layout.topMarginMm / 5
		val labelY = layout.topMarginMm - fontSize * 1.5
		val calendar = Calendar.getInstance().apply { timeInMillis = createdTimeMillis }
		val dateText = "${calendar.get(Calendar.YEAR)}.${calendar.get(Calendar.MONTH) + 1}"

		sb.append("  <g id=\"label\">\n")
		sb.append(text("1:${params.scaleDenominator}", layout.leftMarginMm, labelY, fontSize, "start"))
		sb.append(text(escape(label), layout.paperWidthMm / 2, labelY, fontSize, "middle"))
		sb.append(text(dateText, layout.paperWidthMm - layout.rightMarginMm, labelY, fontSize, "end"))
		sb.append("  </g>\n")
	}

	// ------------------------------------------------------------------- frame

	private fun appendFrame(sb: StringBuilder) {
		val labelSize = layout.leftMarginMm / 5
		val prefixSize = labelSize * 0.8
		val topLabelY = -labelSize * 0.8
		val bottomLabelY = layout.mapHeightMm + labelSize * 1.5
		val leftLabelX = -labelSize * 0.8
		val rightLabelX = layout.mapWidthMm + labelSize * 0.8

		sb.append("  <g id=\"frame\" transform=\"translate(${fmt(layout.leftMarginMm)} ${fmt(layout.topMarginMm)})\">\n")
		sb.append("    <rect x=\"0\" y=\"0\" width=\"${fmt(layout.mapWidthMm)}\" height=\"${fmt(layout.mapHeightMm)}\" ")
		sb.append("stroke=\"#000000\" stroke-width=\"1.0\" fill=\"none\"/>\n")

		sb.append("    <g clip-path=\"url(#frame_clip)\">\n")
		appendBasemapImage(sb)
		sb.append("      <g id=\"grid\">\n")
		appendGrid(sb)
		sb.append("      </g>\n")
		val zone2 = secondaryZone()
		val secondary = if (zone2 == null) emptyList() else secondaryPolylines(zone2)
		if (secondary.isNotEmpty()) {
			sb.append("      <g id=\"grid_secondary\">\n")
			for (line in secondary) {
				sb.append(polylinePath(line))
			}
			sb.append("      </g>\n")
		}
		sb.append("    </g>\n")

		appendGridLabels(sb, labelSize, prefixSize, topLabelY, bottomLabelY,
			leftLabelX, rightLabelX, zone2, secondary)
		sb.append("  </g>\n")
	}

	private fun appendBasemapImage(sb: StringBuilder) {
		// Axis-aligned and centred: the raster is rendered Mercator-north-up,
		// the paper is true-north-up, so pixel axes match the paper axes and
		// the georeferenced corners form an axis-aligned rectangle (verified
		// against geodesic corner distances, <=0.1mm). The calibration makes
		// the raster's corner-averaged centre coincide with the requested
		// map centre, hence centring is the correct placement. Rotating the
		// image by the convergence would rotate the whole map content.
		imageTlX = (layout.mapWidthMm - rasterWidthMm) / 2.0
		imageTlY = (layout.mapHeightMm - rasterHeightMm) / 2.0
		LOG.info("placement: gamma=${geometry.convergenceDeg} k=$k imageTl=($imageTlX,$imageTlY) " +
				"imageSize=($rasterWidthMm,$rasterHeightMm) frame=(${layout.mapWidthMm}x${layout.mapHeightMm})")
		val href = escape(raster.pngFileName)
		sb.append("      <image xlink:href=\"$href\" href=\"$href\" ")
		sb.append("x=\"${fmt(imageTlX)}\" y=\"${fmt(imageTlY)}\" width=\"${fmt(rasterWidthMm)}\" height=\"${fmt(rasterHeightMm)}\" ")
		sb.append("preserveAspectRatio=\"none\"/>\n")
	}

	// -------------------------------------------------------------------- grid

	private data class GridLine(val metres: Long, val startX: Double, val startY: Double,
	                            val endX: Double, val endY: Double)

	/**
	 * Grid spacing in ground metres: the smallest 1/2/5×10ⁿ unit whose
	 * on-paper spacing reaches 3 cm at this scale. Neighbouring 1/2/5
	 * candidates are at most 2.5× apart, so the spacing always stays under
	 * the 8 cm upper bound; smaller paper at the same scale naturally picks
	 * smaller units through the same rule.
	 */
	private fun gridStepMeters(): Long {
		val steps = longArrayOf(100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000,
				100000, 200000, 500000, 1000000, 2000000, 5000000)
		for (step in steps) {
			if (step * 1000.0 / params.scaleDenominator >= 30.0) {
				return step
			}
		}
		return steps.last()
	}

	/**
	 * Vertical easting grid lines, interpolated along the georeferenced
	 * top and bottom raster edges. The line positions inherit the UTM
	 * convergence from the corner coordinates: on the true-north-up paper the
	 * lines tilt by the local grid convergence without any explicit rotation.
	 */
	private fun eastingLines(): List<GridLine> {
		val result = mutableListOf<GridLine>()
		val step = gridStepMeters()
		val from = Math.ceil(maxOf(raster.eastingTopLeft, raster.eastingBottomLeft).toDouble() / step).toLong()
		val to = Math.floor(minOf(raster.eastingTopRight, raster.eastingBottomRight).toDouble() / step).toLong()
		val spanTop = raster.eastingTopRight - raster.eastingTopLeft
		val spanBottom = raster.eastingBottomRight - raster.eastingBottomLeft
		for (m in from..to) {
			val easting = m * step
			val fxTop = (easting - raster.eastingTopLeft) / spanTop
			val fxBottom = (easting - raster.eastingBottomLeft) / spanBottom
			result.add(GridLine(easting,
				imageTlX + fxTop * rasterWidthMm, imageTlY,
				imageTlX + fxBottom * rasterWidthMm, imageTlY + rasterHeightMm))
		}
		return result
	}

	/** Horizontal northing grid lines, interpolated along the left and right raster edges. */
	private fun northingLines(): List<GridLine> {
		val result = mutableListOf<GridLine>()
		val step = gridStepMeters()
		val from = Math.ceil(raster.northingBottomRight.toDouble() / step).toLong()
		val to = Math.floor(raster.northingTopLeft.toDouble() / step).toLong()
		val spanLeft = raster.northingTopLeft - raster.northingBottomLeft
		val spanRight = raster.northingTopRight - raster.northingBottomRight
		for (m in from..to) {
			val northing = m * step
			val fyLeft = (raster.northingTopLeft - northing) / spanLeft
			val fyRight = (raster.northingTopRight - northing) / spanRight
			result.add(GridLine(northing,
				imageTlX, imageTlY + fyLeft * rasterHeightMm,
				imageTlX + rasterWidthMm, imageTlY + fyRight * rasterHeightMm))
		}
		return result
	}

	/** Grid lines at true grid positions, straight between the raster edge crossings. */
	private fun appendGrid(sb: StringBuilder) {
		for (line in eastingLines()) {
			sb.append(line(line.startX, line.startY, line.endX, line.endY))
		}
		for (line in northingLines()) {
			sb.append(line(line.startX, line.startY, line.endX, line.endY))
		}
	}

	/**
	 * Diagnostic: for whole-kilometre UTM intersections, the paper-mm position
	 * where the vector grid is drawn versus where the placed raster puts the
	 * same ground point (linear interpolation between the georeferenced
	 * corners). With the corner-interpolated grid the two models coincide by
	 * construction; a non-zero residual indicates a regression. Reported for
	 * pipeline observability.
	 */
	fun gridVsRasterResiduals(points: List<Pair<Double, Double>>): String {
		val spanTop = raster.eastingTopRight - raster.eastingTopLeft
		val spanBottom = raster.eastingBottomRight - raster.eastingBottomLeft
		val spanLeft = raster.northingTopLeft - raster.northingBottomLeft
		val spanRight = raster.northingTopRight - raster.northingBottomRight
		val sb = StringBuilder()
		for ((easting, northing) in points) {
			val fxTop = (easting - raster.eastingTopLeft) / spanTop
			val fxBottom = (easting - raster.eastingBottomLeft) / spanBottom
			val fyLeft = (raster.northingTopLeft - northing) / spanLeft
			val fyRight = (raster.northingTopRight - northing) / spanRight
			// grid line position at the point's northing (bilinear of the two edge crossings)
			val fN = (raster.northingTopLeft - northing) / spanLeft
			val gridX = imageTlX + (fxTop + (fxBottom - fxTop) * fN) * rasterWidthMm
			val fE = (easting - raster.eastingTopLeft) / spanTop
			val gridY = imageTlY + (fyLeft + (fyRight - fyLeft) * fE) * rasterHeightMm
			val rasterX = imageTlX + (easting - raster.eastingTopLeft) / spanTop * rasterWidthMm
			val rasterY = imageTlY + (raster.northingTopLeft - northing) / spanLeft * rasterHeightMm
			sb.append(String.format(Locale.US,
				"E%.0f/N%.0f grid=(%.3f,%.3f)mm raster=(%.3f,%.3f)mm d=%.3fmm; ",
				easting, northing, gridX, gridY, rasterX, rasterY,
				hypot(gridX - rasterX, gridY - rasterY)))
		}
		return sb.toString()
	}

	private fun appendGridLabels(sb: StringBuilder, labelSize: Double, prefixSize: Double,
	                             topLabelY: Double, bottomLabelY: Double,
	                             leftLabelX: Double, rightLabelX: Double,
	                             zone2: Int?, secondary: List<SecondaryLine>) {
		val labels = StringBuilder()
		val topIntervals = EdgeIntervals()
		val bottomIntervals = EdgeIntervals()
		val leftIntervals = EdgeIntervals()
		val rightIntervals = EdgeIntervals()
		for (line in eastingLines()) {
			val gradX = line.endX - line.startX
			val gradY = line.endY - line.startY
			if (gradY == 0.0) continue
			val t0 = -line.startY / gradY
			if (t0 >= 0) {
				val x0 = line.startX + t0 * gradX
				if (x0 in 0.0..layout.mapWidthMm) {
					val (prefix, standard) = gridLabelParts(line.metres)
					labels.append(text("$prefix", x0, topLabelY, prefixSize, "end"))
					labels.append(text(standard, x0, topLabelY, labelSize, "start"))
					topIntervals.add(labelInterval(x0, prefix, prefixSize, standard, labelSize))
				}
			}
			val t1 = (layout.mapHeightMm - line.startY) / gradY
			if (t1 <= 1.0) {
				val x1 = line.startX + t1 * gradX
				if (x1 in 0.0..layout.mapWidthMm) {
					val (prefix, standard) = gridLabelParts(line.metres)
					labels.append(text("$prefix", x1, bottomLabelY, prefixSize, "end"))
					labels.append(text(standard, x1, bottomLabelY, labelSize, "start"))
					bottomIntervals.add(labelInterval(x1, prefix, prefixSize, standard, labelSize))
				}
			}
		}

		val left = StringBuilder()
		val right = StringBuilder()
		for (line in northingLines()) {
			val gradX = line.endX - line.startX
			val gradY = line.endY - line.startY
			if (gradX == 0.0) continue
			val t0 = -line.startX / gradX
			if (t0 >= 0) {
				val y0 = line.startY + t0 * gradY
				if (y0 in 0.0..layout.mapHeightMm) {
					val (prefix, standard) = gridLabelParts(line.metres)
					left.append(text("$prefix", layout.mapHeightMm - y0, 0.0, prefixSize, "end"))
					left.append(text(standard, layout.mapHeightMm - y0, 0.0, labelSize, "start"))
					// Rotated group: use a symmetric conservative span so the
					// anchor-direction subtleties can never cause overlap.
					val half = maxOf(prefix.toString().length * prefixSize, standard.length * labelSize) * 0.62
					leftIntervals.add(y0 - half to y0 + half)
				}
			}
			val t1 = (layout.mapWidthMm - line.startX) / gradX
			if (t1 <= 1.0) {
				val y1 = line.startY + t1 * gradY
				if (y1 in 0.0..layout.mapHeightMm) {
					val (prefix, standard) = gridLabelParts(line.metres)
					right.append(text("$prefix", y1, 0.0, prefixSize, "end"))
					right.append(text(standard, y1, 0.0, labelSize, "start"))
					// Rotated group: symmetric conservative span.
					val half = maxOf(prefix.toString().length * prefixSize, standard.length * labelSize) * 0.62
					rightIntervals.add(y1 - half to y1 + half)
				}
			}
		}

		// Secondary-zone labels: pale red, one font size below the primary
		// labels. When the crossing position is taken the label slides along
		// the edge (left/right on the top and bottom edges, up/down on the
		// side edges) in whole-label steps until a free slot fits; it is
		// dropped only when no slot fits at all.
		if (zone2 != null && secondary.isNotEmpty()) {
			val size2 = labelSize * 0.8
			val prefix2 = size2 * 0.8
			for (line in secondary) {
				val (prefixValue, standard) = gridLabelParts(line.metres)
				val prefix = prefixValue.toString()
				val hw = prefix.length * prefix2 * 0.62
				val sw = standard.length * size2 * 0.62
				if (line.isEasting) {
					for (x in crossingsY(line, 0.0)) {
						if (x !in 0.0..layout.mapWidthMm) continue
						val anchor = freePosition(topIntervals, x, hw, sw, 0.0, layout.mapWidthMm)
							?: continue
						labels.append(text(prefix, anchor, topLabelY, prefix2, "end", SECONDARY_COLOR))
						labels.append(text(standard, anchor, topLabelY, size2, "start", SECONDARY_COLOR))
						topIntervals.add(anchor - hw to anchor + sw)
					}
					for (x in crossingsY(line, layout.mapHeightMm)) {
						if (x !in 0.0..layout.mapWidthMm) continue
						val anchor = freePosition(bottomIntervals, x, hw, sw, 0.0, layout.mapWidthMm)
							?: continue
						labels.append(text(prefix, anchor, bottomLabelY, prefix2, "end", SECONDARY_COLOR))
						labels.append(text(standard, anchor, bottomLabelY, size2, "start", SECONDARY_COLOR))
						bottomIntervals.add(anchor - hw to anchor + sw)
					}
				} else {
					// Rotated groups: symmetric conservative span, same as
					// the primary labels, so overlap is impossible.
					val half = maxOf(prefix.length * prefix2, standard.length * size2) * 0.62
					for (y in crossingsX(line, 0.0)) {
						if (y !in 0.0..layout.mapHeightMm) continue
						val anchor = freePosition(leftIntervals, y, half, half, 0.0, layout.mapHeightMm)
							?: continue
						left.append(text(prefix, layout.mapHeightMm - anchor, 0.0, prefix2, "end", SECONDARY_COLOR))
						left.append(text(standard, layout.mapHeightMm - anchor, 0.0, size2, "start", SECONDARY_COLOR))
						leftIntervals.add(anchor - half to anchor + half)
					}
					for (y in crossingsX(line, layout.mapWidthMm)) {
						if (y !in 0.0..layout.mapHeightMm) continue
						val anchor = freePosition(rightIntervals, y, half, half, 0.0, layout.mapHeightMm)
							?: continue
						right.append(text(prefix, anchor, 0.0, prefix2, "end", SECONDARY_COLOR))
						right.append(text(standard, anchor, 0.0, size2, "start", SECONDARY_COLOR))
						rightIntervals.add(anchor - half to anchor + half)
					}
				}
			}
		}

		sb.append("    <!-- grid labels -->\n")
		sb.append(labels)
		sb.append("    <g transform=\"translate(${fmt(leftLabelX)} ${fmt(layout.mapHeightMm)}) rotate(-90)\">")
		sb.append(left).append("</g>\n")
		sb.append("    <g transform=\"translate(${fmt(rightLabelX)} 0) rotate(90)\">")
		sb.append(right).append("</g>\n")
	}

	/** Occupied coordinate intervals along one frame edge, for label collision checks. */
	private class EdgeIntervals {
		private val intervals = mutableListOf<Pair<Double, Double>>()

		fun add(interval: Pair<Double, Double>) {
			intervals.add(interval)
		}

		fun isFree(from: Double, to: Double): Boolean =
			intervals.none { from < it.second && to > it.first }
	}

	/** Occupied span of a two-part grid label centred on `position`. */
	private fun labelInterval(position: Double, prefix: Any, prefixSize: Double,
	                          standard: String, standardSize: Double): Pair<Double, Double> =
		Pair(position - prefix.toString().length * prefixSize * 0.62,
			position + standard.length * standardSize * 0.62)

	/**
	 * Anchor for a label whose preferred position is `preferred`: slides
	 * along the edge in whole-label steps (right, left, then further out)
	 * until the [hw, sw] span fits into a free slot between `min` and `max`.
	 * Returns null when no slot fits within the search range.
	 */
	private fun freePosition(intervals: EdgeIntervals, preferred: Double,
	                         hw: Double, sw: Double, min: Double, max: Double): Double? {
		val step = hw + sw
		for (k in 0..8) {
			val sign = if (k == 0) 0.0 else if (k % 2 == 1) 1.0 else -1.0
			val candidate = preferred + sign * ((k + 1) / 2) * step
			if (candidate - hw >= min && candidate + sw <= max &&
					intervals.isFree(candidate - hw, candidate + sw)) {
				return candidate
			}
		}
		return null
	}

	// ---------------------------------------------------------- secondary grid

	/** A zone-2 grid line mapped onto the sheet: its grid value in zone-2
	 *  metres and the sampled polyline in frame millimetres. */
	private class SecondaryLine(
		val metres: Long,
		val isEasting: Boolean,
		val points: List<Pair<Double, Double>>
	)

	/**
	 * Paper position (frame mm) of a centre-zone UTM point: bilinear
	 * interpolation between the georeferenced raster edges — the same model
	 * the primary grid uses (see [gridVsRasterResiduals]).
	 */
	private fun paperPos(easting: Double, northing: Double): Pair<Double, Double> {
		val spanTop = raster.eastingTopRight - raster.eastingTopLeft
		val spanBottom = raster.eastingBottomRight - raster.eastingBottomLeft
		val spanLeft = raster.northingTopLeft - raster.northingBottomLeft
		val spanRight = raster.northingTopRight - raster.northingBottomRight
		val fxTop = (easting - raster.eastingTopLeft) / spanTop
		val fxBottom = (easting - raster.eastingBottomLeft) / spanBottom
		val fyLeft = (raster.northingTopLeft - northing) / spanLeft
		val fyRight = (raster.northingTopRight - northing) / spanRight
		val x = imageTlX + (fxTop + (fxBottom - fxTop) * fyLeft) * rasterWidthMm
		val y = imageTlY + (fyLeft + (fyRight - fyLeft) * fxTop) * rasterHeightMm
		return x to y
	}

	/**
	 * The adjacent UTM zone whose grid appears on this sheet — a raster
	 * corner falls into it — or null when the whole sheet lies inside the
	 * centre zone.
	 */
	private fun secondaryZone(): Int? {
		val zone1 = geometry.centerUtm.zoneNumber
		val letter = geometry.centerUtm.zoneLetter
		val corners = listOf(
			UtmCoordinate(zone1, letter, raster.eastingTopLeft, raster.northingTopLeft),
			UtmCoordinate(zone1, letter, raster.eastingTopRight, raster.northingTopRight),
			UtmCoordinate(zone1, letter, raster.eastingBottomLeft, raster.northingBottomLeft),
			UtmCoordinate(zone1, letter, raster.eastingBottomRight, raster.northingBottomRight))
		for (corner in corners) {
			val (lat, lon) = UtmMath.toLatLon(corner)
			val natural = UtmMath.fromLatLon(lat, lon).zoneNumber
			if (natural != zone1) {
				return natural
			}
		}
		return null
	}

	/**
	 * Zone-2 grid lines mapped onto the sheet. Each line is sampled in
	 * zone-2 coordinates (constant easting or northing), converted back to
	 * lat/lon and re-projected into the centre zone, so the drawn polyline
	 * inherits the relative rotation between the two grids.
	 */
	private fun secondaryPolylines(zone2: Int): List<SecondaryLine> {
		val zone1 = geometry.centerUtm.zoneNumber
		val letter = geometry.centerUtm.zoneLetter
		val isNorthern = geometry.centerUtm.isNorthern
		val step = gridStepMeters().toDouble()
		val samples = 24

		fun toPaper(easting2: Double, northing2: Double): Pair<Double, Double> {
			val (lat, lon) = UtmMath.toLatLon(UtmCoordinate(zone2, letter, easting2, northing2))
			// Centre-hemisphere conversion: keeps northings continuous for
			// sheets that cross the equator.
			val centre = UtmMath.fromLatLon(lat, lon, zone1, isNorthern)
			return paperPos(centre.easting, centre.northing)
		}

		// Zone-2 extents of the raster corners (forced into zone 2).
		val cornerCoords = listOf(
			raster.eastingTopLeft to raster.northingTopLeft,
			raster.eastingTopRight to raster.northingTopRight,
			raster.eastingBottomLeft to raster.northingBottomLeft,
			raster.eastingBottomRight to raster.northingBottomRight)
		val eastings2 = mutableListOf<Double>()
		val northings2 = mutableListOf<Double>()
		for ((easting1, northing1) in cornerCoords) {
			val (lat, lon) = UtmMath.toLatLon(UtmCoordinate(zone1, letter, easting1, northing1))
			val inZone2 = UtmMath.fromLatLon(lat, lon, zone2, isNorthern)
			eastings2.add(inZone2.easting)
			northings2.add(inZone2.northing)
		}
		val minE2 = eastings2.minOrNull() ?: return emptyList()
		val maxE2 = eastings2.maxOrNull() ?: return emptyList()
		val minN2 = northings2.minOrNull() ?: return emptyList()
		val maxN2 = northings2.maxOrNull() ?: return emptyList()

		val lines = mutableListOf<SecondaryLine>()
		var e2 = Math.ceil(minE2 / step) * step
		while (e2 <= maxE2) {
			val points = (0..samples).map {
				toPaper(e2, minN2 + (maxN2 - minN2) * it / samples)
			}
			lines.add(SecondaryLine(Math.round(e2), true, points))
			e2 += step
		}
		var n2 = Math.ceil(minN2 / step) * step
		while (n2 <= maxN2) {
			val points = (0..samples).map {
				toPaper(minE2 + (maxE2 - minE2) * it / samples, n2)
			}
			lines.add(SecondaryLine(Math.round(n2), false, points))
			n2 += step
		}
		return lines
	}

	/** Pale red dashed SVG path of a secondary grid line. */
	private fun polylinePath(line: SecondaryLine): String {
		val start = line.points.first()
		val rest = line.points.drop(1).joinToString(" ") {
			"L ${fmt(it.first)} ${fmt(it.second)}"
		}
		return "          <path d=\"M ${fmt(start.first)} ${fmt(start.second)} $rest\" " +
				"stroke=\"$SECONDARY_COLOR\" stroke-width=\"0.2\" " +
				"stroke-dasharray=\"1.2 0.8\" fill=\"none\"/>\n"
	}

	/** X positions where the polyline crosses the horizontal line `y`. */
	private fun crossingsY(line: SecondaryLine, y: Double): List<Double> {
		val result = mutableListOf<Double>()
		var prev = line.points.first()
		for (point in line.points.drop(1)) {
			if ((prev.second - y) * (point.second - y) <= 0.0 && prev.second != point.second) {
				val t = (y - prev.second) / (point.second - prev.second)
				result.add(prev.first + t * (point.first - prev.first))
			}
			prev = point
		}
		return result.distinctBy { Math.round(it * 100.0) }
	}

	/** Y positions where the polyline crosses the vertical line `x`. */
	private fun crossingsX(line: SecondaryLine, x: Double): List<Double> {
		val result = mutableListOf<Double>()
		var prev = line.points.first()
		for (point in line.points.drop(1)) {
			if ((prev.first - x) * (point.first - x) <= 0.0 && prev.first != point.first) {
				val t = (x - prev.first) / (point.first - prev.first)
				result.add(prev.second + t * (point.second - prev.second))
			}
			prev = point
		}
		return result.distinctBy { Math.round(it * 100.0) }
	}

	// --------------------------------------------------------------- scale bar

	/** One scale-bar row: base unit, fine extension step and label formatting. */
	private class BarSpec(
		val unitMeters: Double,
		val fineMeters: Double,
		/** Value label for a run of k base units, e.g. "2" (km) or "2000" (ft). */
		val unitLabel: (Int) -> String,
		/** Label at the middle of the fine extension (half a base unit). */
		val halfLabel: String,
		/** Unit caption at the right bar end. */
		val caption: String
	)

	/**
	 * Survey-style bar pair on the sheet bottom: the metric bar on the first
	 * row band, its imperial mirror one band below (as on USGS sheets). The
	 * gray annotation block sits below both bars.
	 */
	private fun appendScaleBar(sb: StringBuilder) {
		val fontSize = layout.bottomMarginMm * 0.6 / 15
		val barHeight = fontSize / 2
		val minBarMm = layout.mapWidthMm / 3.0
		val halfPaperMm = layout.paperWidthMm / 2.0

		sb.append("  <g id=\"scale_bar\" transform=\"translate(${fmt(layout.leftMarginMm)} ")
		sb.append("${fmt(layout.paperHeightMm - layout.bottomMarginMm)})\">\n")
		appendScaleBarRow(sb, metricBarSpec(minBarMm, halfPaperMm),
			fontSize * 6, fontSize, barHeight, minBarMm, halfPaperMm)
		appendScaleBarRow(sb, imperialBarSpec(minBarMm, halfPaperMm),
			fontSize * 10.5, fontSize, barHeight, minBarMm, halfPaperMm)

		// Gray annotations, rebased below the imperial bar's caption row.
		val unitY = fontSize * 12
		val gray = GRAY_COLOR
		val centerUtm = geometry.centerUtm
		val zoneLine = "WGS84 UTM Zone ${geometry.zoneLabel}"
		sb.append(text(zoneLine, 0.0,
			unitY + fontSize * 1.5, fontSize, "start"))
		// A sheet spanning two zones lists the secondary zone after the
		// centre zone, in the secondary grid's pale red.
		val zone2 = secondaryZone()
		if (zone2 != null) {
			val offset = (zoneLine.length + 2) * fontSize * 0.62
			sb.append(text("$zone2${geometry.zoneBand}", offset,
				unitY + fontSize * 1.5, fontSize, "start", SECONDARY_COLOR))
		}
		sb.append(text("Parameters: ${geometry.zoneLabel} ${centerUtm.easting.toInt()} " +
				"${centerUtm.northing.toInt()} 1:${params.scaleDenominator}",
			0.0, unitY + fontSize * 3, fontSize, "start", color = gray))
		sb.append(text("Map is provided by OsmAnd Maps &amp; Navigation https://osmand.net/",
			0.0, unitY + fontSize * 4.5, fontSize, "start", color = gray))
		// Single-line data-credits pointer: the authoritative full list of
		// data sources lives in the OsmAnd repository license file.
		sb.append(text("Full data credits: OsmAnd License — github.com/osmandapp/OsmAnd/blob/master/LICENSE",
			0.0, unitY + fontSize * 6, fontSize, "start", color = gray))
		sb.append("  </g>\n")
	}

	/**
	 * Draws one survey-style bar row: a left extension of one base unit in
	 * fine grey/white steps, then the main section counting up from 0 at
	 * its start. Labels sit above the bar, the unit caption below its right
	 * end. The division scheme follows the ground length of a third of the
	 * map width; the whole bar (extension + main section) takes at most
	 * half of the paper width. Labels: the fine extension reads unit /
	 * unit/2 / 0 (left to right) and the main section counts up from 0 at
	 * its start: every unit when it is under 5 units, otherwise only 1 and
	 * 5 (and 10 for a 10-unit main section).
	 */
	private fun appendScaleBarRow(sb: StringBuilder, spec: BarSpec, topMm: Double,
	                              fontSize: Double, barHeight: Double,
	                              minBarMm: Double, halfPaperMm: Double) {
		val textY = topMm - fontSize
		val unitY = topMm + fontSize * 1.5
		val unitMm = spec.unitMeters * 1000.0 / params.scaleDenominator
		val fineMm = spec.fineMeters * 1000.0 / params.scaleDenominator
		val needUnits = Math.ceil(minBarMm / unitMm).toInt().coerceAtLeast(1)
		val pageCapUnits = (halfPaperMm / unitMm).toInt() - 1
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
		sb.append(text(spec.caption, barWidth, unitY, fontSize, "end"))
	}

	/**
	 * Metric bar. The division scheme follows the ground length of half the
	 * paper width: under 2 km → 10 m fine / 100 m unit with all labels in
	 * plain metres ("m"); otherwise → 100 m fine / 1 km unit
	 * ("1 / 0.5 / 0", "km"). A fine step under 2 mm on paper is multiplied
	 * by 5 (10→50 m, 100→500 m).
	 */
	private fun metricBarSpec(minBarMm: Double, halfPaperMm: Double): BarSpec {
		val halfGroundM = halfPaperMm * params.scaleDenominator / 1000.0
		val metricMode = halfGroundM < 2000.0
		val unitM = if (metricMode) 100L else 1000L
		var fineM = if (metricMode) 10L else 100L
		if (fineM * 1000.0 / params.scaleDenominator < 2.0) {
			fineM *= 5L
		}
		return BarSpec(
			unitMeters = unitM.toDouble(),
			fineMeters = fineM.toDouble(),
			unitLabel = { k -> if (metricMode) (k * unitM).toString() else trimNumber(k.toDouble()) },
			halfLabel = if (metricMode) (unitM / 2L).toString() else "0.5",
			caption = if (metricMode) "m" else "km")
	}

	/**
	 * Imperial mirror of the metric bar (USGS-style). Mile bars use whole
	 * base units (1/2/5×10ⁿ miles): the fine extension subdivides the unit
	 * into ten 0.1-unit steps and reads unit / 0.5 / 0 (so a 1-mile bar
	 * reads 1 → 0.5 → 0 with 0.1-mile steps). Larger-scale maps use decimal
	 * feet base units (100 ft, 1000 ft) with the same unit/10 subdivision.
	 * As in the metric bar, a fine step under 2 mm on paper is multiplied
	 * by 5.
	 */
	private fun imperialBarSpec(minBarMm: Double, halfPaperMm: Double): BarSpec {
		val ftM = 0.3048
		val miM = 1609.344
		val ladder = doubleArrayOf(100 * ftM, 1000 * ftM, miM, 2 * miM,
				5 * miM, 10 * miM, 20 * miM, 50 * miM)
		var unitMeters = ladder.last()
		for (candidate in ladder) {
			if (Math.ceil(minBarMm / (candidate * 1000.0 / params.scaleDenominator)) <= 5.0) {
				unitMeters = candidate
				break
			}
		}
		val unitFt = unitMeters / ftM
		val milesMode = unitFt >= 5279.5
		var fineMeters = unitMeters / 10.0
		if (fineMeters * 1000.0 / params.scaleDenominator < 2.0) {
			fineMeters *= 5.0
		}
		return BarSpec(
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

	// ----------------------------------------------------------------- compass

	private fun appendCompass(sb: StringBuilder) {
		val compassSize = layout.bottomMarginMm * 0.6
		val lineLength = compassSize * 0.85
		val gamma = geometry.convergenceDeg
		val gammaRad = Math.toRadians(gamma)
		val decl = geometry.declinationDeg
		val declRad = Math.toRadians(decl)

		val starRadius = (compassSize - lineLength) * 0.2
		val starTransform = "translate(0 ${fmt(compassSize - lineLength)}) rotate(-90)"

		val labelSize = compassSize / 15
		val labelMargin = labelSize / 2
		val angleSize = compassSize / 20
		val angleMargin = angleSize / 2

		sb.append("  <g id=\"compass\" transform=\"translate(")
		sb.append("${fmt(layout.paperWidthMm - layout.rightMarginMm - compassSize * 0.5)} ")
		sb.append("${fmt(layout.paperHeightMm - layout.bottomMarginMm + (layout.bottomMarginMm - compassSize) / 4)})\">\n")

		// True north line + label.
		sb.append("    <path d=\"M 0 ${fmt(compassSize)} L 0 ${fmt(compassSize - lineLength)}\" ")
		sb.append("stroke=\"#000000\" stroke-width=\"0.2\" fill=\"none\"/>\n")
		sb.append(text("TN", labelMargin, compassSize - lineLength + labelMargin, labelSize, "start"))

		// Magnetic north.
		val mnX = lineLength * sin(declRad)
		val mnY = compassSize - lineLength * cos(declRad)
		sb.append("    <path d=\"M 0 ${fmt(compassSize)} L ${fmt(mnX)} ${fmt(mnY)}\" ")
		sb.append("stroke=\"#000000\" stroke-width=\"0.1\" fill=\"none\"/>\n")
		val mnAnchor = if (mnX < 0) "end" else "start"
		val mnTextX = if (mnX < 0) mnX - labelMargin else mnX + labelMargin
		sb.append(text("MN", mnTextX, mnY + labelSize * 1.5, labelSize, mnAnchor))
		val mnAngleAnchor = if (decl < 0) "end" else "start"
		val mnAngleX = lineLength * 0.5 * sin(declRad) + (if (decl < 0) -angleMargin else angleMargin)
		sb.append(text("${fmt2(decl)}°", mnAngleX,
			compassSize - lineLength * 0.5 * cos(declRad) - angleMargin, angleSize, mnAngleAnchor))
		sb.append("    <path id=\"tn_mn_arc\" d=\"M 0 ${fmt(compassSize - lineLength * 0.85)} ")
		sb.append("A ${fmt(lineLength * 0.85)} ${fmt(lineLength * 0.85)} 0 0 0 ")
		sb.append("${fmt(lineLength * 0.85 * sin(declRad))} ${fmt(compassSize - lineLength * 0.85 * cos(declRad))}\" ")
		sb.append("fill=\"none\" stroke=\"#000000\" stroke-width=\"0.1\" stroke-dasharray=\"0.1 0.8\"/>\n")

		// Grid north.
		val gnX = lineLength * 0.75 * sin(gammaRad)
		val gnY = compassSize - lineLength * 0.75 * cos(gammaRad)
		sb.append("    <path d=\"M 0 ${fmt(compassSize)} L ${fmt(gnX)} ${fmt(gnY)}\" ")
		sb.append("stroke=\"#000000\" stroke-width=\"0.1\" fill=\"none\"/>\n")
		val gnAnchor = if (gnX < 0) "end" else "start"
		val gnTextX = if (gnX < 0) gnX - labelMargin else gnX + labelMargin
		sb.append(text("GN", gnTextX, gnY + labelMargin, labelSize, gnAnchor))
		val gnAngleAnchor = if (gamma < 0) "end" else "start"
		val gnAngleX = lineLength * 0.375 * sin(gammaRad) + (if (gamma < 0) -angleMargin else angleMargin)
		sb.append(text("${fmt2(gamma)}°", gnAngleX,
			compassSize - lineLength * 0.375 * cos(gammaRad) - angleMargin, angleSize, gnAngleAnchor))
		sb.append("    <path id=\"tn_gn_arc\" d=\"M 0 ${fmt(compassSize - lineLength * 0.75)} ")
		sb.append("A ${fmt(lineLength * 0.75)} ${fmt(lineLength * 0.75)} 0 0 0 ${fmt(gnX)} ${fmt(gnY)}\" ")
		sb.append("fill=\"none\" stroke=\"#000000\" stroke-width=\"0.1\" stroke-dasharray=\"0.1 0.8\"/>\n")

		// Compass star.
		sb.append("    <path d=\"${starPath(starRadius)}\" fill=\"#000000\" transform=\"$starTransform\"/>\n")
		sb.append("  </g>\n")
	}

	/** Five-pointed compass star, ported from `draw_star_path`. */
	private fun starPath(radius: Double): String {
		val innerRadius = radius * (3 - sqrt(5.0)) / 2
		val sb = StringBuilder()
		sb.append("M ${fmt(radius)} 0")
		var angle = 36
		while (angle < 360) {
			val rad = Math.toRadians(angle.toDouble())
			val nextRad = Math.toRadians((angle + 36).toDouble())
			sb.append(" L ${fmt(innerRadius * cos(rad))} ${fmt(innerRadius * sin(rad))}")
			sb.append(" L ${fmt(radius * cos(nextRad))} ${fmt(radius * sin(nextRad))}")
			angle += 72
		}
		sb.append(" Z")
		return sb.toString()
	}

	// ----------------------------------------------------------------- helpers

	private fun line(x1: Double, y1: Double, x2: Double, y2: Double): String =
		"          <path d=\"M ${fmt(x1)} ${fmt(y1)} L ${fmt(x2)} ${fmt(y2)}\" " +
				"stroke=\"#000000\" stroke-width=\"0.1\" fill=\"none\"/>\n"

	private fun text(content: String, x: Double, y: Double, fontSize: Double, anchor: String,
	                 color: String = "#000000"): String =
		"      <text x=\"${fmt(x)}\" y=\"${fmt(y)}\" fill=\"$color\" stroke=\"$color\" stroke-width=\"0.1\" " +
				"font-family=\"$FONT_FAMILY\" font-size=\"${fmt(fontSize)}\" text-anchor=\"$anchor\">$content</text>\n"

	/**
	 * Splits a grid line's UTM value (metres) into the two-part edge label:
	 * the downsized prefix (millions + hundred-thousands digits) and the
	 * normal-size standard digits — thousands+hectometres when the grid unit
	 * is finer than 1 km (e.g. 730500 m on a 500 m grid → "7" + "305"),
	 * thousands only at km-or-coarser units (731000 m on a 1 km grid →
	 * "7" + "31").
	 */
	private fun gridLabelParts(rawMetres: Long): Pair<Long, String> {
		// Standard display convention for northings: drop the 10M false
		// northing above the equator (southern centre convention) and take
		// the 10M complement below it (northern centre convention dipping
		// across the equator). Eastings never trigger either branch.
		val metres = when {
			rawMetres >= 10_000_000L -> rawMetres - 10_000_000L
			rawMetres < 0L -> rawMetres + 10_000_000L
			else -> rawMetres
		}
		val subKmGrid = gridStepMeters() < 1000L
		val prefix = metres / 100000L
		val standardDigits = if (subKmGrid) 3 else 2
		val divisor = if (subKmGrid) 100L else 1000L
		val modulus = if (subKmGrid) 1000L else 100L
		val standard = String.format(Locale.US, "%0${standardDigits}d", metres / divisor % modulus)
		return Pair(prefix, standard)
	}

	private fun escape(value: String): String = value
		.replace("&", "&amp;")
		.replace("<", "&lt;")
		.replace(">", "&gt;")
		.replace("\"", "&quot;")

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

	private fun fmt(value: Double): String {
		var formatted = String.format(Locale.US, "%.4f", value)
		while (formatted.contains(".") && formatted.endsWith("0")) {
			formatted = formatted.substring(0, formatted.length - 1)
		}
		if (formatted.endsWith(".")) {
			formatted = formatted.substring(0, formatted.length - 1)
		}
		return if (formatted == "-0") "0" else formatted
	}

	private fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)

	companion object {
		private val LOG: Log = PlatformUtil.getLog(SvgMapComposer::class.java)
		private const val FONT_FAMILY = "Noto Sans CJK SC, Noto Sans, sans-serif"
		private const val GRAY_COLOR = "#808080"
		private const val SECONDARY_COLOR = "#cc5555"
	}
}
