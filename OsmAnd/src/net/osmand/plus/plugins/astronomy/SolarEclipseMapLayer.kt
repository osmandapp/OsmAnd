package net.osmand.plus.plugins.astronomy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import io.github.cosinekitty.astronomy.EclipseKind
import io.github.cosinekitty.astronomy.SolarEclipseMapCoordinate
import io.github.cosinekitty.astronomy.SolarEclipseMapFrame
import io.github.cosinekitty.astronomy.SolarEclipseMapTrack
import net.osmand.core.jni.MapMarker
import net.osmand.core.jni.MapMarkerBuilder
import net.osmand.core.jni.MapMarkersCollection
import net.osmand.core.jni.PointI
import net.osmand.core.jni.PolygonBuilder
import net.osmand.core.jni.PolygonsCollection
import net.osmand.core.jni.QVectorPointI
import net.osmand.core.jni.VectorLine
import net.osmand.core.jni.VectorLineBuilder
import net.osmand.core.jni.VectorLinesCollection
import net.osmand.core.jni.ZoomLevel
import net.osmand.data.RotatedTileBox
import net.osmand.plus.R
import net.osmand.plus.utils.NativeUtilities
import net.osmand.plus.views.layers.base.OsmandMapLayer
import net.osmand.plus.views.layers.geometry.GeometryWayDrawer
import net.osmand.util.MapUtils

class SolarEclipseMapLayer(context: Context) : OsmandMapLayer(context) {

	private data class LayerState(
		val active: Boolean = false,
		val eventKey: Double? = null,
		val eventKind: EclipseKind? = null,
		val track: SolarEclipseMapTrack? = null,
		val frame: SolarEclipseMapFrame? = null
	)

	@Volatile
	private var state = LayerState()
	private var nativeGeometryReady = false
	private var polygonsCollection: PolygonsCollection? = null
	private var vectorLinesCollection: VectorLinesCollection? = null
	private var eclipseMarker: MapMarker? = null
	private var polygonId = 1

	private val corridorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.FILL
	}
	private val centerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeCap = Paint.Cap.ROUND
		strokeJoin = Paint.Join.ROUND
	}
	private val markerHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
	private val markerOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
	private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

	fun setEclipseData(
		active: Boolean,
		eventKey: Double?,
		eventKind: EclipseKind?,
		track: SolarEclipseMapTrack?,
		frame: SolarEclipseMapFrame?
	) {
		val previous = state
		if (previous.active == active && previous.eventKey == eventKey &&
			previous.eventKind == eventKind && previous.track === track && previous.frame === frame
		) {
			return
		}
		state = LayerState(active, eventKey, eventKind, track, frame)
		val staticChanged = previous.active != active || previous.eventKey != eventKey ||
			previous.eventKind != eventKind || previous.track !== track
		if (staticChanged) nativeGeometryReady = false
		if (!active) clearNativeCollections()
		view?.refreshMap()
	}

	override fun initLayer(view: net.osmand.plus.views.OsmandMapTileView) {
		super.initLayer(view)
		setPointsOrder(5.8f)
		updateResources()
	}

	override fun updateResources() {
		clearNativeCollections()
		val pathColor = ContextCompat.getColor(context, R.color.solar_eclipse_path)
		corridorPaint.color = ColorUtils.setAlphaComponent(pathColor, 64)
		centerLinePaint.color = pathColor
		centerLinePaint.strokeWidth = 3f * density.coerceAtLeast(1f)
		markerHaloPaint.color = ContextCompat.getColor(context, R.color.solar_eclipse_marker_halo)
		markerOutlinePaint.color = ContextCompat.getColor(context, R.color.solar_eclipse_marker_outline)
		markerPaint.color = ContextCompat.getColor(context, R.color.solar_eclipse_marker)
	}

	override fun onPrepareBufferImage(
		canvas: Canvas,
		tileBox: RotatedTileBox,
		settings: DrawSettings
	) {
		super.onPrepareBufferImage(canvas, tileBox, settings)
		val current = state
		if (!current.active) return
		if (mapRenderer != null) {
			if (mapRendererChanged) {
				clearNativeCollections()
				mapRendererChanged = false
			}
			updateOpenGl(current)
		} else {
			drawCanvas(canvas, tileBox, current)
		}
	}

	private fun drawCanvas(canvas: Canvas, tileBox: RotatedTileBox, current: LayerState) {
		drawCanvasPolygons(canvas, tileBox, current)
		current.track?.centerLineSegments.orEmpty().forEach { line ->
			if (line.size < 2) return@forEach
			val path = Path()
			line.forEachIndexed { index, coordinate ->
				val x = tileBox.getPixXFromLatLon(coordinate.latitude, coordinate.longitude)
				val y = tileBox.getPixYFromLatLon(coordinate.latitude, coordinate.longitude)
				if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
			}
			canvas.drawPath(path, centerLinePaint)
		}
		current.frame?.shadowPoint?.let { point ->
			val x = tileBox.getPixXFromLatLon(point.latitude, point.longitude)
			val y = tileBox.getPixYFromLatLon(point.latitude, point.longitude)
			val scale = density.coerceAtLeast(1f)
			canvas.drawCircle(x, y, 11f * scale, markerHaloPaint)
			canvas.drawCircle(x, y, 8f * scale, markerOutlinePaint)
			canvas.drawCircle(x, y, 5f * scale, markerPaint)
		}
	}

	private fun drawCanvasPolygons(canvas: Canvas, tileBox: RotatedTileBox, current: LayerState) {
		geometryPolygons(current).forEach { polygon ->
			if (polygon.size < 3) return@forEach
			val path = Path()
			polygon.forEachIndexed { index, coordinate ->
				val x = tileBox.getPixXFromLatLon(coordinate.latitude, coordinate.longitude)
				val y = tileBox.getPixYFromLatLon(coordinate.latitude, coordinate.longitude)
				if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
			}
			path.close()
			canvas.drawPath(path, corridorPaint)
		}
	}

	private fun geometryPolygons(current: LayerState): List<List<SolarEclipseMapCoordinate>> =
		if (current.eventKind == EclipseKind.Partial) {
			current.frame?.penumbralFootprintPolygons.orEmpty()
		} else {
			current.track?.corridorPolygons.orEmpty()
		}

	private fun updateOpenGl(current: LayerState) {
		val renderer = mapRenderer ?: return
		if (!nativeGeometryReady) {
			clearGeometryCollections()
			createPolygonCollection(current)
			createLineCollection(current)
			nativeGeometryReady = true
		}
		updateMarkerCollection(current)
		polygonsCollection?.let { if (!renderer.hasSymbolsProvider(it)) renderer.addSymbolsProvider(it) }
		vectorLinesCollection?.let { if (!renderer.hasSymbolsProvider(it)) renderer.addSymbolsProvider(it) }
		mapMarkersCollection?.let { if (!renderer.hasSymbolsProvider(it)) renderer.addSymbolsProvider(it) }
	}

	private fun createPolygonCollection(current: LayerState) {
		if (current.eventKind == EclipseKind.Partial) return
		val polygons = geometryPolygons(current)
		if (polygons.isEmpty()) return
		val collection = PolygonsCollection(ZoomLevel.ZoomLevel1, ZoomLevel.ZoomLevel20)
		val color = NativeUtilities.createFColorARGB(corridorPaint.color)
		polygons.forEach { polygon ->
			val points = polygon.toPoints31()
			if (points.size() >= 3) {
				PolygonBuilder()
					.setBaseOrder(baseOrder)
					.setIsHidden(false)
					.setPolygonId(polygonId++)
					.setPoints(points)
					.setFillColor(color)
					.buildAndAddToCollection(collection)
			}
		}
		polygonsCollection = collection
	}

	private fun createLineCollection(current: LayerState) {
		val lines = current.track?.centerLineSegments.orEmpty()
		if (lines.isEmpty()) return
		val collection = VectorLinesCollection()
		val lineScale = GeometryWayDrawer.getVectorLineScale(application)
		var lineId = 1
		lines.forEach { line ->
			val points = line.toPoints31()
			if (points.size() >= 2) {
				VectorLineBuilder()
					.setBaseOrder(baseOrder - 1)
					.setIsHidden(false)
					.setLineId(lineId++)
					.setLineWidth(centerLinePaint.strokeWidth.toDouble() * lineScale.toDouble())
					.setPoints(points)
					.setEndCapStyle(VectorLine.EndCapStyle.BUTT.swigValue())
					.setFillColor(NativeUtilities.createFColorARGB(centerLinePaint.color))
					.setApproximationEnabled(false)
					.buildAndAddToCollection(collection)
			}
		}
		vectorLinesCollection = collection
	}

	private fun updateMarkerCollection(current: LayerState) {
		val point = current.frame?.shadowPoint
		if (point == null) {
			eclipseMarker?.setIsHidden(true)
			return
		}
		val position = PointI(
			MapUtils.get31TileNumberX(point.longitude),
			MapUtils.get31TileNumberY(point.latitude)
		)
		val marker = eclipseMarker
		if (marker != null) {
			marker.setPosition(position)
			marker.setIsHidden(false)
			return
		}
		val collection = MapMarkersCollection()
		val builtMarker = MapMarkerBuilder()
			.setPosition(position)
			.setIsHidden(false)
			.setBaseOrder(pointsOrder)
			.setIsAccuracyCircleSupported(false)
			.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal)
			.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.CenterVertical)
			.setPinIcon(NativeUtilities.createSkImageFromBitmap(createMarkerBitmap()))
			.setUpdateAfterCreated(true)
			.buildAndAddToCollection(collection)
		mapMarkersCollection = collection
		eclipseMarker = builtMarker
	}

	private fun createMarkerBitmap(): Bitmap {
		val size = (24f * density.coerceAtLeast(1f)).toInt().coerceAtLeast(24)
		return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
			val canvas = Canvas(bitmap)
			val center = size / 2f
			canvas.drawCircle(center, center, size * 0.46f, markerHaloPaint)
			canvas.drawCircle(center, center, size * 0.34f, markerOutlinePaint)
			canvas.drawCircle(center, center, size * 0.22f, markerPaint)
		}
	}

	private fun List<SolarEclipseMapCoordinate>.toPoints31(): QVectorPointI =
		QVectorPointI().also { result ->
			forEach { coordinate ->
				result.add(
					PointI(
						MapUtils.get31TileNumberX(coordinate.longitude),
						MapUtils.get31TileNumberY(coordinate.latitude)
					)
				)
			}
		}

	private fun clearGeometryCollections() {
		val renderer = mapRenderer
		polygonsCollection?.let { renderer?.removeSymbolsProvider(it) }
		vectorLinesCollection?.let { renderer?.removeSymbolsProvider(it) }
		polygonsCollection = null
		vectorLinesCollection = null
		polygonId = 1
		nativeGeometryReady = false
	}

	private fun clearNativeCollections() {
		clearGeometryCollections()
		clearMapMarkersCollections()
		mapMarkersCollection = null
		eclipseMarker = null
	}

	override fun cleanupResources() {
		super.cleanupResources()
		clearNativeCollections()
	}

	override fun onDraw(canvas: Canvas, tileBox: RotatedTileBox, settings: DrawSettings) {
		val current = state
		if (current.active && current.eventKind == EclipseKind.Partial && mapRenderer != null) {
			drawCanvasPolygons(canvas, tileBox, current)
		}
	}

	override fun drawInScreenPixels(): Boolean = false
}
