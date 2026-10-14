package net.osmand.plus.track.share

import net.osmand.PlatformUtil
import net.osmand.aidlapi.map.MapFlyoverParams
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.flyover.FlyoverValues
import net.osmand.plus.flyover.MapFlyoverRecorder
import net.osmand.plus.track.helpers.SelectedGpxFile
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.FileUtils
import net.osmand.shared.gpx.GpxHelper
import net.osmand.shared.gpx.PointAttributes
import net.osmand.util.Algorithms
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * "Share as image" of a track: a picture of the track on the map, north up, with the activity and the region at
 * the top left, the OsmAnd logo at the top right, the totals in a column at the right and the elevation and speed
 * chart at the bottom, in one of the [Format]s; a wide picture has the region alone at the top left with the chart
 * under it and the activity heading the totals under the logo. The map flyover recorder renders it as a single frame off the
 * screen, shows its progress in the map area and puts the map back afterwards; the picture is then shown for a
 * look before it is shared.
 */
class TrackShareImage private constructor(
	private val app: OsmandApplication,
	private val selected: SelectedGpxFile,
	private val format: Format,
) {

	/** The shapes "Share as image" offers. */
	enum class Format(val width: Int, val height: Int, val fileSuffix: String) {
		/** 4:5, as a social post. */
		CARD(1080, 1350, "card"),
		/** Square. */
		SQUARE(1080, 1080, "square"),
		/** 9:16, as a phone screen. */
		SCREENSHOT(1080, 1920, "screenshot"),
		/** 16:9, as a phone screen held sideways: the region at the top left with the chart under it, the logo at
		 * the top right with the activity and the totals under it. */
		SCREENSHOT_WIDE(1920, 1080, "screenshot_wide"),
	}

	/** The picture is wider than high: the sideways layout. */
	private val wide = format.width > format.height

	/** Picture pixels per dp of the overlay: the recorder lays the frame out as 800 dp on its long side. */
	private val pxPerDp = max(format.width, format.height) / 800.0

	/** The chart and the title block as far from the sides as the logo is from the top and the left. */
	private val panelWidth = 1 - 2 * MARGIN_DP * pxPerDp / format.width

	/**
	 * On a wide picture the region stands alone at the top left, its row in line with the logo, and the chart
	 * under it: on one line when the name fits up to a pin's width left of the logo, else on two lines with the
	 * chart lower. Decided in advance with the widget's font.
	 */
	private val wideRegionLines: Int by lazy {
		val region = regionName()
		val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
		paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
		paint.textSize = (28 * pxPerDp).toFloat()
		val available = format.width - (2 * MARGIN_DP + LOGO_WIDTH_DP + PIN_WIDTH_DP + 24) * pxPerDp
		if (paint.measureText(region) <= available) 1 else 2
	}

	/** The chart under the region's one or two lines (48 dp each at the title's top) and a gap. */
	private val wideChartTopDp get() = TITLE_TOP_DP + 48.0 * wideRegionLines + 8

	/** The region as the recorder will show it, for measuring. */
	private fun regionName(): String {
		val points = selected.gpxFile.getAllSegmentsPoints().filter { !it.lat.isNaN() && !it.lon.isNaN() }
		if (points.isEmpty()) return ""
		val mid = points[points.size / 2]
		return FlyoverValues.regionName(app, mid.lat, mid.lon) ?: ""
	}

	private fun record(): MapFlyoverParams {
		val gpxFile = selected.gpxFile
		val points = gpxFile.getAllSegmentsPoints().filter { !it.lat.isNaN() && !it.lon.isNaN() }
		require(points.size >= 2) { app.getString(R.string.share_image_track_too_short) }
		val minLat = points.minOf { it.lat }
		val maxLat = points.maxOf { it.lat }
		val minLon = points.minOf { it.lon }
		val maxLon = points.maxOf { it.lon }
		// the map is drawn as on the phone: its magnifier enlarges the tiles, and is kept for the picture
		val magnifier = app.osmandMap.mapDensity.toDouble()
		val tilePx = 256.0 * max(1.0, max(format.width, format.height) / 640.0) * magnifier
		// the track fits the whole width between the region block and the chart, under the totals too (they are
		// drawn on the map itself); on a wide picture the free field under the chart, left of the totals
		val zoom = fitZoom(minLat, maxLat, minLon, maxLon,
			format.width * (if (wide) WIDE_TRACK_WIDTH else TRACK_WIDTH),
			format.height * (if (wide) WIDE_TRACK_HEIGHT else TRACK_HEIGHT), tilePx)
		val centerLat = (minLat + maxLat) / 2
		val centerLon = (minLon + maxLon) / 2

		// a track opened from another app lives in a temporary file that goes away with its menu: a copy of our own
		val trackFile = if (FileUtils.isTempFile(app, gpxFile.path)) {
			File(app.cacheDir, CACHE_DIR).also { it.mkdirs() }.let { dir -> File(dir, "track.gpx").also { File(gpxFile.path).copyTo(it, overwrite = true) } }
		} else {
			File(gpxFile.path)
		}
		val spec = JSONObject()
		val target = JSONArray().put(if (wide) WIDE_TRACK_CENTER_X else 0.5).put(if (wide) WIDE_TRACK_CENTER_Y else TRACK_CENTER_Y)
		spec.put("keyframes", JSONArray().put(JSONObject().put("target", target)).put(JSONObject().put("target", target)))
		spec.put("gpx", JSONArray().put(JSONObject()
			.put("file", trackFile.absolutePath)
			.put("draw", "full")
			.put("head", "none")
			.put("show_wpts", false)))
		spec.put("map_magnifier", magnifier)
		// the logo opaque at the top right
		spec.put("osmand_widgets", JSONArray().put("osmand_logo"))
		spec.put("logo", JSONObject().put("gravity", "top|right").put("margin", (MARGIN_DP * pxPerDp).roundToInt()))
		spec.put("output", "image")
		val panels = JSONArray()
		panels.put(chartPanel())
		summaryPanels().forEach { panels.put(it) }
		spec.put("panels", panels)
		// the overview camera alone, as one frame: two equal key frames a millisecond apart
		val params = MapFlyoverParams(doubleArrayOf(centerLat, centerLat), doubleArrayOf(centerLon, centerLon),
			floatArrayOf(zoom, zoom), floatArrayOf(0f, 0f), floatArrayOf(90f, 90f),
			longArrayOf(0, 1), format.width, format.height, FPS, fileName())
		params.spec = spec.toString()
		LOG.info("Track picture of ${gpxFile.path}: ${format.width}x${format.height}, zoom $zoom, magnifier $magnifier")
		try {
			MapFlyoverRecorder(app).record(params)
			if (!params.warnings.isNullOrEmpty()) {
				LOG.info("Track picture warnings: ${params.warnings}")
			}
		} finally {
			if (trackFile.path != gpxFile.path) {
				trackFile.delete()
			}
		}
		return params
	}

	/** The elevation and speed chart at the bottom, on a translucent card. */
	private fun chartPanel(): JSONObject = JSONObject()
		.put("gravity", if (wide) "top|left" else "bottom|center")
		.put("margin", (MARGIN_DP * pxPerDp).roundToInt())
		// above the attribution at the bottom right of a tall picture
		.put("margin_bottom", (BOTTOM_MARGIN_DP * pxPerDp).roundToInt())
		.put("margin_top", ((if (wide) wideChartTopDp else MARGIN_DP.toDouble()) * pxPerDp).roundToInt())
		.put("width", if (wide) WIDE_CHART_WIDTH else panelWidth)
		.put("background", CHART_BACKGROUND)
		.put("widgets", JSONArray().put(JSONObject()
			.put("type", "chart")
			.put("cursor", false)
			.put("title", app.getString(R.string.share_image_elevation_speed))
			.put("value", "gpx[1].ele")
			.put("value2", "gpx[1].speed")
			// speed as the second series of the elevation chart is red in the app's track charts
			.put("color2", Algorithms.colorToString(ColorUtilities.getColor(app, R.color.gpx_chart_red)))
			.put("label_color2", Algorithms.colorToString(ColorUtilities.getColor(app, R.color.gpx_chart_red_label)))
			.put("height_dp", CHART_HEIGHT_DP)))

	/**
	 * The activity and the region at the top left and the totals in a column at the right, both on the map itself
	 * (transparent, outlined text).
	 */
	private fun summaryPanels(): List<JSONObject> {
		val analysis = selected.getTrackAnalysis(app)
		val title = JSONArray()
		val activity = selected.gpxFile.metadata.getRouteActivity(app.routeActivityHelper.getActivities())
		if (activity != null) {
			// the icon of the activity as the track menu shows it, by the name of its drawable
			val iconId = AndroidUtils.getActivityIconId(app, activity)
			val iconName = if (iconId != 0) app.resources.getResourceEntryName(iconId) else ""
			title.put(JSONObject().put("type", "text").put("title", "").put("text", activity.label)
				.put("icon", iconName).put("size", "medium").put("span", "row"))
		}
		title.put(JSONObject().put("type", "attribute").put("title", "").put("template", "{gpx[1].region}")
			.put("size", "medium").put("span", "row").put("lines", if (wide) wideRegionLines else REGION_LINES))
		val totals = JSONArray()
		totals.put(JSONObject().put("type", "attribute").put("title", app.getString(R.string.distance)).put("value", "gpx[1].dist"))
		if (analysis.isTimeSpecified()) {
			totals.put(JSONObject().put("type", "attribute").put("title", app.getString(R.string.duration)).put("value", "gpx[1].duration"))
		}
		if (analysis.hasElevationData()) {
			totals.put(JSONObject().put("type", "attribute").put("title", app.getString(R.string.altitude_ascent)).put("value", "gpx[1].climb"))
			totals.put(JSONObject().put("type", "attribute").put("title", app.getString(R.string.altitude_descent)).put("value", "gpx[1].descent"))
		}
		if (analysis.isTimeSpecified()) {
			totals.put(JSONObject().put("type", "attribute").put("title", app.getString(R.string.average_speed)).put("value", "gpx[1].speed_avg"))
		}
		if (analysis.hasData(PointAttributes.SENSOR_TAG_HEART_RATE)) {
			totals.put(JSONObject().put("type", "attribute").put("title", app.getString(R.string.map_widget_ant_heart_rate)).put("value", "gpx[1].hr_avg"))
		}
		val margin = (MARGIN_DP * pxPerDp).roundToInt()
		// on a wide picture the region alone at the top left and the activity heading the totals under the logo
		val block = if (wide) JSONArray().put(title.get(title.length() - 1)) else title
		val column = JSONArray()
		if (wide && title.length() > 1) {
			column.put(JSONObject(title.getJSONObject(0).toString()).put("size", "small"))
		}
		for (i in 0 until totals.length()) {
			column.put(totals.get(i))
		}
		return listOf(
			JSONObject()
				.put("gravity", "top|left")
				.put("margin", margin)
				// the first row centred on the logo at the right
				.put("margin_top", (TITLE_TOP_DP * pxPerDp).roundToInt())
				.put("columns", 1)
				.put("size", "medium")
				.put("background", "transparent")
				// clear of the logo: a long region name wraps, a very long one gets a smaller font
				.put("width", if (wide) 1 - (2 * MARGIN_DP + LOGO_WIDTH_DP + PIN_WIDTH_DP) * pxPerDp / format.width else TITLE_WIDTH)
				.put("widgets", block),
			JSONObject()
				.put("gravity", "top|right")
				.put("margin", margin)
				.put("margin_top", ((if (wide) WIDE_TOTALS_TOP_DP else TOTALS_TOP_DP) * pxPerDp).roundToInt())
				.put("columns", 1)
				.put("size", if (wide) "small" else "medium")
				.put("background", "transparent")
				.put("widgets", column))
	}

	private fun fileName(): String {
		val base = GpxHelper.getGpxTitle(Algorithms.getFileWithoutDirs(selected.gpxFile.path))
			.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
			.trim('_')
			.ifEmpty { "track" }
		return "${base}_${format.fileSuffix}"
	}

	companion object {
		private val LOG = PlatformUtil.getLog(TrackShareImage::class.java)

		/** A still picture: the recorder renders a single frame. */
		private const val FPS = 25
		/** The logo, the title block, the totals and the chart from the frame's edges, dp of its 800-dp long side. */
		private const val MARGIN_DP = 32
		/** The activity and the region take this much of the width, clear of the logo. */
		private const val TITLE_WIDTH = 0.68
		/** The title block from the top: the first row (48 dp) centred on the logo's pin (37 dp at 32 dp). */
		private const val TITLE_TOP_DP = 27
		/** The chart, the totals and the summary end above the attribution at the bottom right. */
		private const val BOTTOM_MARGIN_DP = 40
		/** The logo (pin, gap, wordmark) and the pin alone are about this wide, dp. */
		private const val LOGO_WIDTH_DP = 150
		private const val PIN_WIDTH_DP = 30
		/** A wide picture: the chart's width, the totals' column right under the logo, the track's field. */
		private const val WIDE_CHART_WIDTH = 0.5
		private const val WIDE_TOTALS_TOP_DP = 72
		private const val WIDE_TRACK_WIDTH = 0.72
		private const val WIDE_TRACK_HEIGHT = 0.58
		private const val WIDE_TRACK_CENTER_X = 0.4
		private const val WIDE_TRACK_CENTER_Y = 0.66
		/** A long region name wraps onto this many lines, and the room for them is kept. */
		private const val REGION_LINES = 3
		/** The column of totals starts below the activity and three lines of the region. */
		private const val TOTALS_TOP_DP = 190
		private const val CHART_HEIGHT_DP = 84
		/** Opacity of the chart's card; the title and the totals are drawn on the map itself. */
		private const val CHART_BACKGROUND = 0.6
		/** The track fits this much of the width, and this much of the height centred here: between the region
		 * block and the chart. */
		private const val TRACK_WIDTH = 0.88
		private const val TRACK_HEIGHT = 0.52
		private const val TRACK_CENTER_Y = 0.515
		private const val CACHE_DIR = "track_share"
		private const val HIDE_CONTROLS_DELAY_MS = 600L

		/**
		 * Zoom at which a bounding box fits into a part of a frame, in Web Mercator with square tiles of
		 * {@code tilePx} pixels: the largest zoom at which the box is no wider than {@code availableWidthPx}
		 * and no higher than {@code availableHeightPx}.
		 */
		fun fitZoom(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double,
		            availableWidthPx: Double, availableHeightPx: Double, tilePx: Double): Float {
			val lonSpan = max(1e-6, maxLon - minLon) / 360
			val latSpan = max(1e-9, mercatorY(minLat) - mercatorY(maxLat))
			val byWidth = availableWidthPx / (tilePx * lonSpan)
			val byHeight = availableHeightPx / (tilePx * latSpan)
			return log2(min(byWidth, byHeight)).coerceIn(1.0, 22.0).toFloat()
		}

		/** Web Mercator y of a latitude, 0 at the north edge of the world and 1 at the south edge. */
		private fun mercatorY(lat: Double): Double {
			val rad = Math.toRadians(lat.coerceIn(-85.05, 85.05))
			return (1 - ln(tan(rad) + 1 / cos(rad)) / PI) / 2
		}

		/**
		 * Renders the picture on a background thread and shows it, with a button to share it; the recorder shows
		 * its progress in the map area meanwhile. Errors are reported with a toast.
		 */
		@JvmStatic
		fun start(mapActivity: MapActivity, selected: SelectedGpxFile, format: Format, onFinished: Runnable?) {
			val app = mapActivity.application as OsmandApplication
			if (MapFlyoverRecorder.isRendererHeld()) {
				app.showToastMessage(R.string.share_image_failed, app.getString(R.string.flyover_recording, ""))
				onFinished?.run()
				return
			}
			// the map area shows the frame being rendered; the buttons and widgets over it would only cover it
			// (a little later: the track menu, closing now, shows them once more as it goes)
			app.runInUIThread({ mapActivity.mapLayers.mapControlsLayer.hideMapControls() }, HIDE_CONTROLS_DELAY_MS)
			Thread({
				val result = try {
					TrackShareImage(app, selected, format).record()
				} catch (e: Exception) {
					LOG.error("Track picture failed", e)
					null
				}
				app.runInUIThread {
					mapActivity.mapLayers.mapControlsLayer.showMapControlsIfHidden()
					onFinished?.run()
					val file = result?.filePath?.takeIf { it.isNotEmpty() && result.error.isNullOrEmpty() }?.let { File(it) }
					if (file != null && file.isFile) {
						TrackShareImagePreviewFragment.showInstance(mapActivity.supportFragmentManager, file)
					} else {
						app.showToastMessage(R.string.share_image_failed, result?.error ?: "")
					}
				}
			}, "TrackShareImage").start()
		}
	}
}
