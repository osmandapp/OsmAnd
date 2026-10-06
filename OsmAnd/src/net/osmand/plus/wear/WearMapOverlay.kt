package net.osmand.plus.wear

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

import net.osmand.data.RotatedTileBox

import kotlin.math.abs
import net.osmand.plus.OsmandApplication

/**
 * Draws what the rendered map alone cannot show: the route, where you are, and the markers.
 *
 * The phone's own layers are not reused. They ask the phone's map view for its renderer and
 * behave differently depending on which engine it runs - with OpenGL the route layer feeds
 * geometry to the core instead of painting on a canvas, so handing it ours would draw nothing.
 * These are a few polylines and circles; owning them keeps the watch's map the same whichever
 * engine the phone is on.
 */
class WearMapOverlay(private val app: OsmandApplication) {

	private val routeOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeCap = Paint.Cap.ROUND
		strokeJoin = Paint.Join.ROUND
		color = Color.WHITE
	}

	private val routeLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeCap = Paint.Cap.ROUND
		strokeJoin = Paint.Join.ROUND
	}

	private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
	private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		color = Color.WHITE
	}

	private val path = Path()

	/** Draws the chosen layers and returns how long it took, in milliseconds. */
	fun draw(canvas: Canvas, box: RotatedTileBox, layers: Set<WearMapLayer>): Long {
		val startedAt = android.os.SystemClock.elapsedRealtime()
		val scale = box.density
		if (WearMapLayer.ROUTE in layers) {
			drawRoute(canvas, box, scale)
		}
		if (WearMapLayer.MARKERS in layers) {
			drawMarkers(canvas, box, scale)
		}
		if (WearMapLayer.MY_LOCATION in layers) {
			drawMyLocation(canvas, box, scale)
		}
		return android.os.SystemClock.elapsedRealtime() - startedAt
	}

	private fun drawRoute(canvas: Canvas, box: RotatedTileBox, scale: Float) {
		val route = app.routingHelper.route ?: return
		val points = route.immutableAllLocations
		if (points.size < 2) {
			return
		}
		// Points outside the box are kept rather than filtered: a segment that crosses the
		// screen has both ends outside it, and dropping either would cut the line short exactly
		// where it matters. What is dropped is detail too fine to see - a route across a country
		// carries tens of thousands of points, and at this zoom most of them land on a pixel
		// already occupied.
		path.rewind()
		var started = false
		var lastX = 0f
		var lastY = 0f
		val minStep = THINNING_PX * scale
		for ((index, point) in points.withIndex()) {
			val x = box.getPixXFromLatLon(point.latitude, point.longitude)
			val y = box.getPixYFromLatLon(point.latitude, point.longitude)
			if (!started) {
				path.moveTo(x, y)
				started = true
			} else if (index == points.lastIndex ||
					abs(x - lastX) + abs(y - lastY) >= minStep) {
				path.lineTo(x, y)
			} else {
				continue
			}
			lastX = x
			lastY = y
		}
		routeOutline.strokeWidth = ROUTE_WIDTH_DP * scale + OUTLINE_DP * scale * 2
		routeLine.strokeWidth = ROUTE_WIDTH_DP * scale
		routeLine.color = app.osmandMap.mapLayers.routeLayer.routeLineColor
		canvas.drawPath(path, routeOutline)
		canvas.drawPath(path, routeLine)
	}

	private fun drawMarkers(canvas: Canvas, box: RotatedTileBox, scale: Float) {
		val markers = app.mapMarkersHelper.mapMarkers
		for (marker in markers.take(MAX_MARKERS)) {
			val x = box.getPixXFromLatLon(marker.latitude, marker.longitude)
			val y = box.getPixYFromLatLon(marker.latitude, marker.longitude)
			if (!box.containsPoint(x, y, 0f)) {
				continue
			}
			fill.color = app.getColor(net.osmand.plus.mapmarkers.MapMarker.getColorId(marker.colorIndex))
			outline.strokeWidth = OUTLINE_DP * scale
			canvas.drawCircle(x, y, MARKER_RADIUS_DP * scale, fill)
			canvas.drawCircle(x, y, MARKER_RADIUS_DP * scale, outline)
		}
	}

	private fun drawMyLocation(canvas: Canvas, box: RotatedTileBox, scale: Float) {
		val provider = app.locationProvider
		val location = provider.lastKnownLocation ?: provider.lastStaleKnownLocation ?: return
		val x = box.getPixXFromLatLon(location.latitude, location.longitude)
		val y = box.getPixYFromLatLon(location.latitude, location.longitude)
		if (!box.containsPoint(x, y, 0f)) {
			return
		}
		fill.color = app.getColor(net.osmand.plus.R.color.active_color_primary_light)
		outline.strokeWidth = OUTLINE_DP * scale
		canvas.drawCircle(x, y, LOCATION_RADIUS_DP * scale, fill)
		canvas.drawCircle(x, y, LOCATION_RADIUS_DP * scale, outline)
	}

	private companion object {
		const val ROUTE_WIDTH_DP = 4f
		const val OUTLINE_DP = 1f
		const val MARKER_RADIUS_DP = 4f
		const val LOCATION_RADIUS_DP = 5f
		const val MAX_MARKERS = 10

		/**
		 * How far apart two route points must land before both are drawn. Below this the line
		 * cannot show the difference, and a long route is mostly points that cannot.
		 */
		const val THINNING_PX = 2f
	}
}

/** What the watch may draw over the rendered map. */
enum class WearMapLayer {
	ROUTE,
	MARKERS,
	MY_LOCATION
}
