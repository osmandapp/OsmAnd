package net.osmand.plus.track.share

import android.content.res.Configuration
import android.graphics.Bitmap
import net.osmand.PlatformUtil
import net.osmand.aidlapi.map.MapFlyoverParams
import net.osmand.data.LatLon
import net.osmand.gpx.flyover.FlyoverCameraPath
import net.osmand.gpx.flyover.FlyoverCameraPath.Phase
import net.osmand.gpx.flyover.FlyoverTrackGeometry
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.flyover.MapFlyoverRecorder
import net.osmand.plus.track.helpers.SelectedGpxFile
import net.osmand.plus.utils.AndroidUtils
import net.osmand.util.Algorithms
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.FileUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * "Share as animation" of a track: a flyover video rendered by [MapFlyoverRecorder] and shown for a look before it
 * is shared. This class only describes the video: the camera path (an overview of the whole track, a flight along
 * it with the heading held per section, a return to the overview and a hold for the summary), the chart of
 * elevation and speed at the top, photo cards of the user's media and of popular places passed on the way, the
 * summary of the track at the end and the OsmAnd logo. The recorder renders it off the screen, shows its progress
 * and puts the map back afterwards.
 */
class TrackShareAnimation private constructor(
	private val app: OsmandApplication,
	private val selected: SelectedGpxFile,
	private val landscape: Boolean,
) {


	private class Layout(
		val width: Int,
		val height: Int,
		/** Where the camera's point sits on the frame during the flight, fractions of the frame. */
		val flightTarget: DoubleArray,
		/** Where the camera's point sits on the frame for the overview, fractions of the frame. */
		val overviewTarget: DoubleArray,
		/** The part of the frame the overview fits the track into, fractions of the frame. */
		val overviewWidth: Double,
		val overviewHeight: Double,
		val chartGravity: String,
		val chartWidth: Double,
		val cardGravity: String,
		val summaryGravity: String,
		/** Photo cards from the top of the frame (dp), or null for the bottom. */
		val cardTopDp: Double? = null,
	)


	private class PlaceCard(val file: File, val title: String, val subtitle: String, val startSeconds: Double, val endSeconds: Double)

	/** A photo for a card: its place along the track, the picture, the card's title and subtitle. */
	private class Photo(val progress: Double, val bitmap: Bitmap, val title: String, val subtitle: String)

	// the logo is at the top left; the chart below it reaches down to about 25 % of the height, the summary
	// card and the photo cards rise from the bottom to about 58 %; in a video the track fits between, on a
	// picture (north up) it reaches down to the bottom edge of the summary card, at 96 %
	private val layout = when {
		// a landscape video: the chart at the top left (under the region, shown at the end), the logo at the top
		// right with the totals in a column under it at the end, the photo cards under the chart at the right;
		// the current point a third up from the bottom, in the middle between the left edge and the cards
		landscape ->
			Layout(1280, 720, doubleArrayOf(0.37, 0.78), doubleArrayOf(0.37, 0.64), 0.6, 0.5,
				"top|left", 0.52, "top|right", "top|right")
		else ->
			Layout(720, 1280, doubleArrayOf(0.5, 0.43), doubleArrayOf(0.5, 0.43), 0.85, 0.28,
				"top|center", 0.94, "bottom|center", "bottom|center")
	}

	/** Video pixels per dp of the overlay: the recorder lays the frame out as 800 dp on its long side. */
	private val pxPerDp = max(layout.width, layout.height) / 800.0

	/**
	 * The logo from the top and the left: on a picture as far from the top as the summary card is from the
	 * bottom; in a video further, clear of a phone's camera when the video fills its screen.
	 */
	private val logoMarginDp = if (landscape) LANDSCAPE_TOP_DP else VIDEO_LOGO_MARGIN_DP

	/** The chart below the logo: its margin, its height (22 dp, the pin 1.7 times that) and a gap. */
	private val chartTopDp = logoMarginDp + 22 * 1.7 + 10

	/** In a landscape video the region's row is centred on the logo (its pin 37 dp at 10 dp). */
	private val landscapeRegionTopDp get() = LANDSCAPE_TOP_DP + 22 * 1.7 / 2 - 24
	private val landscapeChartTopDp get() = landscapeRegionTopDp + 48.0 * wideRegionLines + 8

	/**
	 * In a landscape video the region comes at the end over the chart, its row in line with the logo: on one line
	 * when the name fits up to a pin's width left of the logo, else on two lines with the chart lower. Decided in
	 * advance with the widget's font.
	 */
	private val wideRegionLines: Int by lazy {
		val region = regionName()
		val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
		paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
		paint.textSize = (28 * pxPerDp).toFloat()
		val available = layout.width - (2 * VIDEO_LOGO_MARGIN_DP + WIDE_LOGO_WIDTH_DP + WIDE_PIN_WIDTH_DP + 24) * pxPerDp
		if (paint.measureText(region) <= available) 1 else 2
	}

	/** The region as the recorder will show it, for measuring. */
	private fun regionName(): String {
		val points = selected.gpxFile.getAllSegmentsPoints().filter { !it.lat.isNaN() && !it.lon.isNaN() }
		if (points.isEmpty()) return ""
		val mid = points[points.size / 2]
		return net.osmand.plus.flyover.FlyoverValues.regionName(app, mid.lat, mid.lon) ?: ""
	}

	private fun record(): MapFlyoverParams {
		val gpxFile = selected.gpxFile
		val points = gpxFile.getAllSegmentsPoints()
			.filter { !it.lat.isNaN() && !it.lon.isNaN() }
			.map { LatLon(it.lat, it.lon) }
		require(points.size >= 2) { app.getString(R.string.share_animation_track_too_short) }
		val geometry = FlyoverTrackGeometry(points)
		// the map is drawn as on the phone: its magnifier enlarges the tiles, and is kept for the video
		val magnifier = app.osmandMap.mapDensity.toDouble()
		val tilePx = 256.0 * max(1.0, max(layout.width, layout.height) / 640.0) * magnifier
		val overviewZoom = FlyoverCameraPath.overviewZoom(
			geometry.minLat, geometry.maxLat, geometry.minLon, geometry.maxLon,
			layout.width * layout.overviewWidth, layout.height * layout.overviewHeight, tilePx)
		val path = FlyoverCameraPath(geometry, FPS, geometry.centerLat, geometry.centerLon, overviewZoom,
			FlyoverCameraPath.DEFAULT_FLIGHT_TILT)

		// a track opened from another app lives in a temporary file that goes away with its menu: a copy of our own
		val trackFile = if (FileUtils.isTempFile(app, gpxFile.path)) {
			File(app.cacheDir, PHOTOS_DIR).also { it.mkdirs() }.let { dir -> File(dir, "track.gpx").also { File(gpxFile.path).copyTo(it, overwrite = true) } }
		} else {
			File(gpxFile.path)
		}
		val places = loadPlaces(geometry, path)
		val flightTarget = if (places.isEmpty() && !landscape) doubleArrayOf(0.5, 0.5) else layout.flightTarget
		val spec = JSONObject()
		val params = cameraParams(path, spec, flightTarget)
		// the current point moves with the camera: evenly along the track over the flight, still during the
		// overviews; the track itself is drawn whole all the time
		spec.put("gpx", JSONArray().put(JSONObject()
			.put("file", trackFile.absolutePath)
			.put("draw", "full")
			.put("head", "dot")
			.put("show_wpts", false)
			.put("time", JSONObject()
				.put("mode", "duration")
				.put("by", "distance")
				.put("offset_s", FlyoverCameraPath.INTRO_SECONDS)
				.put("duration_s", path.flightSeconds)
				.put("ease_in_s", 0)
				.put("ease_out_s", 0))))
		spec.put("ease_in_s", 0)
		spec.put("ease_out_s", 0)
		spec.put("map_magnifier", magnifier)
		// the logo opaque, at the top left of a video and at the top right of a picture
		spec.put("osmand_widgets", JSONArray().put("osmand_logo"))
		spec.put("logo", JSONObject().put("gravity", if (landscape) "top|right" else "top|left")
			.put("margin", (VIDEO_LOGO_MARGIN_DP * pxPerDp).roundToInt())
			.put("margin_top", (logoMarginDp * pxPerDp).roundToInt()))
		val summaryStart = path.flightTimeSeconds(path.introFrames + path.flightFrames + outroFrames(path))
		val panels = JSONArray()
		panels.put(chartPanel())
		for (card in places) {
			panels.put(placePanel(card))
		}
		summaryPanels(summaryStart).forEach { panels.put(it) }
		spec.put("panels", panels)
		params.spec = spec.toString()
		LOG.info("Flyover of ${gpxFile.path}: ${path.totalFrames} frames, ${places.size} photo cards, overview zoom $overviewZoom, flight zoom ${path.flightZoom}, magnifier $magnifier")
		try {
			MapFlyoverRecorder(app).record(params)
			if (!params.warnings.isNullOrEmpty()) {
				LOG.info("Flyover warnings: ${params.warnings}")
			}
		} finally {
			places.forEach { it.file.delete() }
			if (trackFile.path != gpxFile.path) {
				trackFile.delete()
			}
		}
		return params
	}

	/** One key frame per video frame from the camera path, with the camera's point on the frame per phase. */
	private fun cameraParams(path: FlyoverCameraPath, spec: JSONObject, flightTarget: DoubleArray): MapFlyoverParams {
		val n = path.totalFrames
		val lats = DoubleArray(n)
		val lons = DoubleArray(n)
		val zooms = FloatArray(n)
		val rotations = FloatArray(n)
		val tilts = FloatArray(n)
		val times = LongArray(n)
		val keyframes = JSONArray()
		for (i in 0 until n) {
			val frame = path.frameAt(i)
			lats[i] = frame.lat
			lons[i] = frame.lon
			zooms[i] = frame.zoom
			rotations[i] = frame.rotation
			tilts[i] = frame.tilt
			times[i] = i * 1000L / FPS
			val t = FlyoverCameraPath.smooth(frame.phaseProgress)
			val target = when (frame.phase) {
				Phase.INTRO -> blend(layout.overviewTarget, flightTarget, t)
				Phase.FLIGHT -> flightTarget
				Phase.OUTRO -> blend(flightTarget, layout.overviewTarget, t)
				Phase.SUMMARY -> layout.overviewTarget
			}
			keyframes.put(JSONObject().put("target", JSONArray().put(target[0]).put(target[1])))
		}
		spec.put("keyframes", keyframes)
		return MapFlyoverParams(lats, lons, zooms, rotations, tilts, times, layout.width, layout.height, FPS, fileName())
	}

	private fun blend(a: DoubleArray, b: DoubleArray, t: Double) =
		doubleArrayOf(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)

	private fun outroFrames(path: FlyoverCameraPath) = (FlyoverCameraPath.OUTRO_SECONDS * FPS).roundToInt()

	private fun chartPanel(): JSONObject = JSONObject()
		.put("gravity", layout.chartGravity)
		.put("margin", (16 * pxPerDp).roundToInt())
		.put("margin_left", ((if (landscape) VIDEO_LOGO_MARGIN_DP else 16) * pxPerDp).roundToInt())
		// under the logo; in landscape under the region's row shown at the end
		.put("margin_top", ((if (landscape) landscapeChartTopDp else chartTopDp) * pxPerDp).roundToInt())
		.put("width", layout.chartWidth)
		.put("background", CHART_BACKGROUND)
		.put("widgets", JSONArray().put(JSONObject()
			.put("type", "chart")
			.put("title", app.getString(R.string.share_image_elevation_speed))
			.put("value", "gpx[1].ele")
			.put("value2", "gpx[1].speed")
			// speed as the second series of the elevation chart is red in the app's track charts
			.put("color2", Algorithms.colorToString(ColorUtilities.getColor(app, R.color.gpx_chart_red)))
			.put("label_color2", Algorithms.colorToString(ColorUtilities.getColor(app, R.color.gpx_chart_red_label)))
			.put("height_dp", 84)))

	private fun placePanel(card: PlaceCard): JSONObject = JSONObject()
		.put("gravity", layout.cardGravity)
		.put("margin", (CARD_MARGIN_DP * pxPerDp).roundToInt())
		// above the attribution at the bottom right
		.put("margin_bottom", (BOTTOM_MARGIN_DP * pxPerDp).roundToInt())
		// in landscape under the chart
		.put("margin_top", ((if (landscape) landscapeChartTopDp + 134 + 8 else CARD_MARGIN_DP.toDouble()) * pxPerDp).roundToInt())
		.put("margin_right", ((if (landscape) VIDEO_LOGO_MARGIN_DP else CARD_MARGIN_DP) * pxPerDp).roundToInt())
		.put("from_s", card.startSeconds)
		.put("to_s", card.endSeconds)
		.put("fade_s", FADE_SECONDS)
		.put("widgets", JSONArray().put(JSONObject()
			.put("type", "image")
			.put("file", card.file.absolutePath)
			.put("title", card.title)
			.put("subtitle", card.subtitle)
			// in landscape the card ends above the attribution at the bottom right
			.put("width_dp", if (landscape) 240 else 320)
			.put("max_height_dp", if (landscape) LANDSCAPE_CARD_MAX_HEIGHT_DP else 240)))

	/**
	 * The region and activity of the track and its totals: in a video one card of two columns shown with the
	 * final overview; on a picture the activity and the region at the top left and the totals in a column at
	 * the right, both on the map itself (transparent, outlined text).
	 */
	private fun summaryPanels(fromSeconds: Double): List<JSONObject> {
		val analysis = selected.getTrackAnalysis(app)
		val widgets = JSONArray()
		val title = JSONArray()
		// the activity first, then the region, both in the same medium size
		val activity = selected.gpxFile.metadata.getRouteActivity(app.routeActivityHelper.getActivities())
		if (activity != null) {
			// the icon of the activity as the track menu shows it, by the name of its drawable
			val iconId = AndroidUtils.getActivityIconId(app, activity)
			val iconName = if (iconId != 0) app.resources.getResourceEntryName(iconId) else ""
			title.put(JSONObject().put("type", "text").put("title", "").put("text", activity.label)
				.put("icon", iconName).put("size", "medium").put("span", "row"))
		}
		title.put(JSONObject().put("type", "attribute").put("title", "").put("template", "{gpx[1].region}")
			.put("size", "medium").put("span", "row"))
		// three rows of two: distance and time, uphill and downhill, average speed and heart rate
		widgets.put(JSONObject().put("type", "attribute").put("title", app.getString(R.string.distance)).put("value", "gpx[1].dist"))
		if (analysis.isTimeSpecified()) {
			widgets.put(JSONObject().put("type", "attribute").put("title", app.getString(R.string.duration)).put("value", "gpx[1].duration"))
		}
		if (analysis.hasElevationData()) {
			widgets.put(JSONObject().put("type", "attribute").put("title", app.getString(R.string.altitude_ascent)).put("value", "gpx[1].climb"))
			widgets.put(JSONObject().put("type", "attribute").put("title", app.getString(R.string.altitude_descent)).put("value", "gpx[1].descent"))
		}
		if (analysis.isTimeSpecified()) {
			widgets.put(JSONObject().put("type", "attribute").put("title", app.getString(R.string.average_speed)).put("value", "gpx[1].speed_avg"))
		}
		if (analysis.hasData(net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_HEART_RATE)) {
			widgets.put(JSONObject().put("type", "attribute").put("title", app.getString(R.string.map_widget_ant_heart_rate)).put("value", "gpx[1].hr_avg"))
		}
		if (landscape) {
			val margin = (VIDEO_LOGO_MARGIN_DP * pxPerDp).roundToInt()
			// at the end: the activity heading the totals in one column under the logo, on the map itself
			// (transparent, outlined text), its icon in line with theirs; the region over the chart
			val column = JSONArray()
			if (title.length() > 1) {
				column.put(JSONObject(title.getJSONObject(0).toString()).put("size", "small"))
			}
			for (i in 0 until widgets.length()) {
				column.put(widgets.get(i))
			}
			val region = JSONArray().put(JSONObject(title.getJSONObject(title.length() - 1).toString()).put("lines", wideRegionLines))
			return listOf(
				JSONObject()
					.put("gravity", "top|left")
					.put("margin", margin)
					.put("margin_top", (landscapeRegionTopDp * pxPerDp).roundToInt())
					.put("columns", 1)
					.put("size", "medium")
					.put("background", "transparent")
					.put("width", 1 - (2 * VIDEO_LOGO_MARGIN_DP + WIDE_LOGO_WIDTH_DP + WIDE_PIN_WIDTH_DP) * pxPerDp / layout.width)
					.put("from_s", fromSeconds)
					.put("fade_s", FADE_SECONDS)
					.put("widgets", region),
				JSONObject()
					.put("gravity", "top|right")
					.put("margin", margin)
					.put("margin_top", ((LANDSCAPE_TOP_DP + 22 * 1.7 + 12) * pxPerDp).roundToInt())
					.put("columns", 1)
					.put("size", "small")
					.put("background", "transparent")
					.put("from_s", fromSeconds)
					.put("fade_s", FADE_SECONDS)
					.put("widgets", column))
		}
		val all = JSONArray()
		for (i in 0 until title.length()) {
			all.put(title.get(i))
		}
		for (i in 0 until widgets.length()) {
			all.put(widgets.get(i))
		}
		return listOf(JSONObject()
			.put("gravity", layout.summaryGravity)
			.put("margin", (CARD_MARGIN_DP * pxPerDp).roundToInt())
			.put("margin_bottom", (BOTTOM_MARGIN_DP * pxPerDp).roundToInt())
			.put("columns", 2)
			.put("size", "medium")
			.put("background", VIDEO_CARD_BACKGROUND)
			.put("width", if (landscape) 0.42 else 0.92)
			.put("from_s", fromSeconds)
			.put("fade_s", FADE_SECONDS)
			.put("widgets", all))
	}

	/**
	 * The photo cards of the flight: the photos of OsmAnd's media library taken along the track first
	 * (they need no internet), then popular places with their photos for the capacity left, each card with its time
	 * window: it opens when the camera passes the place, or right after the previous card when places
	 * follow each other closely, so all of them are shown one by one.
	 */
	private fun loadPlaces(geometry: FlyoverTrackGeometry, path: FlyoverCameraPath): List<PlaceCard> {
		val available = path.flightSeconds + FlyoverCameraPath.OUTRO_SECONDS - FADE_SECONDS
		val capacity = max(1, (available / (PLACE_CARD_MIN_SECONDS + PLACE_CARD_GAP_SECONDS)).toInt())
		val photos = ArrayList<Photo>()
		try {
			val analysis = selected.getTrackAnalysis(app)
			val mine = FlyoverMediaLoader(app).load(geometry, analysis.startTime, analysis.endTime, capacity) { false }
			val subtitle = app.getString(R.string.shared_string_media)
			mine.mapTo(photos) { Photo(it.selection.progress, it.photo, it.selection.candidate.name, subtitle) }
		} catch (e: Exception) {
			LOG.error("Could not load the media library's photos for the flyover", e)
		}
		if (photos.size < capacity) {
			try {
				val places = FlyoverPlacesLoader(app).load(geometry, capacity - photos.size) { false }
				val subtitle = app.getString(R.string.share_animation_photo_attribution)
				places.mapTo(photos) { Photo(it.selection.progress, it.photo, it.selection.candidate.name, subtitle) }
			} catch (e: Exception) {
				LOG.error("Could not load places for the flyover", e)
			}
		}
		if (photos.isEmpty()) {
			return emptyList()
		}
		photos.sortBy { it.progress }
		val cardSeconds = (available / photos.size - PLACE_CARD_GAP_SECONDS)
			.coerceIn(PLACE_CARD_MIN_SECONDS, PLACE_CARD_MAX_SECONDS)
		val dir = File(app.cacheDir, PHOTOS_DIR).also { it.mkdirs() }
		var previousEnd = Double.NEGATIVE_INFINITY
		val cards = photos.mapIndexedNotNull { index, photo ->
			val file = File(dir, "place_$index.jpg")
			if (!savePhoto(photo.bitmap, file)) {
				return@mapIndexedNotNull null
			}
			val passTime = photo.progress * path.flightSeconds - PLACE_CARD_LEAD_SECONDS
			val start = max(passTime, previousEnd + PLACE_CARD_GAP_SECONDS)
			previousEnd = start + cardSeconds
			PlaceCard(file, photo.title, photo.subtitle, start, previousEnd)
		}
		// cards may run into the return to the overview, but must be gone before the summary appears
		val overflow = previousEnd - available
		return if (overflow <= 0) cards
		else cards.map { PlaceCard(it.file, it.title, it.subtitle, max(0.0, it.startSeconds - overflow), it.endSeconds - overflow) }
	}

	private fun savePhoto(bitmap: Bitmap, file: File): Boolean = try {
		FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
		true
	} catch (e: Exception) {
		LOG.warn("Could not save a flyover photo: ${e.message}")
		false
	}

	private fun fileName(): String {
		val base = net.osmand.shared.gpx.GpxHelper.getGpxTitle(net.osmand.util.Algorithms.getFileWithoutDirs(selected.gpxFile.path))
			.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
			.trim('_')
			.ifEmpty { "track" }
		return "${base}_flyover"
	}

	companion object {
		private val LOG = PlatformUtil.getLog(TrackShareAnimation::class.java)

		private const val FPS = 25
		private const val PLACE_CARD_MAX_SECONDS = 3.0
		private const val PLACE_CARD_MIN_SECONDS = 2.0
		private const val PLACE_CARD_GAP_SECONDS = 0.25
		private const val PLACE_CARD_LEAD_SECONDS = 0.4
		private const val FADE_SECONDS = 0.35
		/** The logo from the top and the left of a video, dp of the frame's 800-dp long side. */
		private const val VIDEO_LOGO_MARGIN_DP = 40
		/** Opacity of the chart's card and of the summary card. */
		private const val CHART_BACKGROUND = 0.7
		private const val VIDEO_CARD_BACKGROUND = 0.9
		private const val CARD_MARGIN_DP = 32
		/** Cards and the summary end above the attribution at the bottom right (12 dp margin, 14 dp text). */
		private const val BOTTOM_MARGIN_DP = 40
		/** The logo (pin, gap, wordmark) and the pin alone are about this wide, dp. */
		private const val WIDE_LOGO_WIDTH_DP = 150
		private const val WIDE_PIN_WIDTH_DP = 30
		/** In a landscape video the photo cards hang under the chart: its top margin, height and a gap. */
		/** A landscape video: the logo as far from the top as the chart is from the logo. */
		private const val LANDSCAPE_TOP_DP = 10
		/** The frame is 450 dp high: the card with its title ends above the attribution. */
		private const val LANDSCAPE_CARD_MAX_HEIGHT_DP = 150
		private const val PHOTOS_DIR = "flyover_photos"
		private const val HIDE_CONTROLS_DELAY_MS = 600L

		/**
		 * Renders the video on a background thread and shows it, with a button to share it; the recorder shows
		 * its progress in the map area meanwhile. Errors are reported with a toast.
		 */
		@JvmStatic
		fun start(mapActivity: MapActivity, selected: SelectedGpxFile, onFinished: Runnable?) {
			val app = mapActivity.application as OsmandApplication
			val landscape = mapActivity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
			if (MapFlyoverRecorder.isRendererHeld()) {
				app.showToastMessage(R.string.share_animation_failed, app.getString(R.string.flyover_recording, ""))
				onFinished?.run()
				return
			}
			// the map area shows the frames being recorded; the buttons and widgets over it would only cover them
			// (a little later: the track menu, closing now, shows them once more as it goes)
			app.runInUIThread({ mapActivity.mapLayers.mapControlsLayer.hideMapControls() }, HIDE_CONTROLS_DELAY_MS)
			Thread({
				val video = TrackShareAnimation(app, selected, landscape)
				val result = try {
					video.record()
				} catch (e: Exception) {
					LOG.error("Flyover failed", e)
					null
				}
				app.runInUIThread {
					mapActivity.mapLayers.mapControlsLayer.showMapControlsIfHidden()
					onFinished?.run()
					val file = result?.filePath?.takeIf { it.isNotEmpty() && result.error.isNullOrEmpty() }?.let { File(it) }
					if (file != null && file.isFile) {
						TrackShareAnimationPreviewFragment.showInstance(mapActivity.supportFragmentManager, file)
					} else {
						app.showToastMessage(R.string.share_animation_failed, result?.error ?: "")
					}
				}
			}, "TrackShareAnimation").start()
		}

	}
}
