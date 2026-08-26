package net.osmand.plus.plugins.astronomy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import io.github.cosinekitty.astronomy.LunarEclipseMapCoordinate
import io.github.cosinekitty.astronomy.LunarEclipseMapFrame
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

class LunarEclipseMapLayer(context: Context) : OsmandMapLayer(context) {

	private data class LayerState(
		val active: Boolean = false,
		val frame: LunarEclipseMapFrame? = null
	)

	@Volatile
	private var state = LayerState()
	private var nativeGeometryDirty = true
	private var polygonsCollection: PolygonsCollection? = null
	private val nativePolygonIds = mutableListOf<Int>()
	private var boundaryCollection: VectorLinesCollection? = null
	private val nativeBoundaryLines = mutableListOf<VectorLine>()
	private var sublunarMarker: MapMarker? = null
	private var polygonId = 1

	private val visibilityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
	private val boundaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeCap = Paint.Cap.ROUND
		strokeJoin = Paint.Join.ROUND
	}
	private val markerHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
	private val markerOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
	private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

	fun setEclipseData(active: Boolean, frame: LunarEclipseMapFrame?) {
		val previous = state
		if (previous.active == active && previous.frame === frame) return
		state = LayerState(active, frame)
		if (!active || previous.frame !== frame) nativeGeometryDirty = true
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
		val visibilityColor = ContextCompat.getColor(context, R.color.lunar_eclipse_visibility)
		visibilityPaint.color = ColorUtils.setAlphaComponent(visibilityColor, 48)
		boundaryPaint.color = visibilityColor
		boundaryPaint.strokeWidth = 2f * density.coerceAtLeast(1f)
		markerHaloPaint.color = ContextCompat.getColor(context, R.color.lunar_eclipse_marker_halo)
		markerOutlinePaint.color = ContextCompat.getColor(context, R.color.lunar_eclipse_marker_outline)
		markerPaint.color = ContextCompat.getColor(context, R.color.lunar_eclipse_marker)
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
		current.frame?.visibilityPolygons.orEmpty().forEach { polygon ->
			if (polygon.size < 3) return@forEach
			val path = Path()
			polygon.forEachIndexed { index, coordinate ->
				val x = tileBox.getPixXFromLatLon(coordinate.latitude, coordinate.longitude)
				val y = tileBox.getPixYFromLatLon(coordinate.latitude, coordinate.longitude)
				if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
			}
			path.close()
			canvas.drawPath(path, visibilityPaint)
			canvas.drawPath(path, boundaryPaint)
		}
		current.frame?.sublunarPoint?.let { point ->
			val x = tileBox.getPixXFromLatLon(point.latitude, point.longitude)
			val y = tileBox.getPixYFromLatLon(point.latitude, point.longitude)
			val scale = density.coerceAtLeast(1f)
			canvas.drawCircle(x, y, 11f * scale, markerHaloPaint)
			canvas.drawCircle(x, y, 8f * scale, markerOutlinePaint)
			canvas.drawCircle(x, y, 5f * scale, markerPaint)
		}
	}

	private fun updateOpenGl(current: LayerState) {
		val renderer = mapRenderer ?: return
		val frame = current.frame
		if (nativeGeometryDirty) {
			updatePolygonCollection(frame)
			updateBoundaryCollection(frame)
			nativeGeometryDirty = false
		}
		updateMarkerCollection(frame)
		polygonsCollection?.let { if (!renderer.hasSymbolsProvider(it)) renderer.addSymbolsProvider(it) }
		boundaryCollection?.let { if (!renderer.hasSymbolsProvider(it)) renderer.addSymbolsProvider(it) }
		mapMarkersCollection?.let { if (!renderer.hasSymbolsProvider(it)) renderer.addSymbolsProvider(it) }
	}

	private fun updatePolygonCollection(frame: LunarEclipseMapFrame?) {
		val polygons = frame?.visibilityPolygons.orEmpty().filter { it.size >= 3 }
		val collection = polygonsCollection
		if (collection != null && nativePolygonIds.size == polygons.size) {
			val updated = nativePolygonIds.indices.all { index ->
				collection.setPolygonPoints(nativePolygonIds[index], polygons[index].toPoints31())
			}
			if (updated) return
		}
		clearPolygonCollection()
		createPolygonCollection(polygons)
	}

	private fun createPolygonCollection(polygons: List<List<LunarEclipseMapCoordinate>>) {
		if (polygons.isEmpty()) return
		val collection = PolygonsCollection(ZoomLevel.ZoomLevel1, ZoomLevel.ZoomLevel20)
		val color = NativeUtilities.createFColorARGB(visibilityPaint.color)
		polygons.forEach { polygon ->
			val points = polygon.toPoints31()
			val id = polygonId++
			PolygonBuilder()
				.setBaseOrder(baseOrder)
				.setIsHidden(false)
				.setPolygonId(id)
				.setPoints(points)
				.setFillColor(color)
				.buildAndAddToCollection(collection)
			nativePolygonIds.add(id)
		}
		polygonsCollection = collection
	}

	private fun updateBoundaryCollection(frame: LunarEclipseMapFrame?) {
		val boundaries = frame?.visibilityPolygons.orEmpty()
			.flatMap { physicalBoundarySegments(it) }
			.filter { it.size >= 2 }
		if (boundaryCollection != null && nativeBoundaryLines.size == boundaries.size) {
			nativeBoundaryLines.forEachIndexed { index, line ->
				line.setPoints(boundaries[index].toPoints31())
			}
			return
		}
		clearBoundaryCollection()
		createBoundaryCollection(boundaries)
	}

	private fun createBoundaryCollection(boundaries: List<List<LunarEclipseMapCoordinate>>) {
		if (boundaries.isEmpty()) return
		val collection = VectorLinesCollection()
		val lineScale = GeometryWayDrawer.getVectorLineScale(application)
		var lineId = 1
		boundaries.forEach { boundary ->
			val points = boundary.toPoints31()
			VectorLineBuilder()
				.setBaseOrder(baseOrder - 1)
				.setIsHidden(false)
				.setLineId(lineId++)
				.setLineWidth(boundaryPaint.strokeWidth.toDouble() * lineScale.toDouble())
				.setPoints(points)
				.setEndCapStyle(VectorLine.EndCapStyle.BUTT.swigValue())
				.setFillColor(NativeUtilities.createFColorARGB(boundaryPaint.color))
				.setApproximationEnabled(false)
				.buildAndAddToCollection(collection)
				?.let(nativeBoundaryLines::add)
		}
		boundaryCollection = collection
	}

	private fun physicalBoundarySegments(
		polygon: List<LunarEclipseMapCoordinate>
	): List<List<LunarEclipseMapCoordinate>> {
		if (polygon.size < 2) return emptyList()
		fun isProjectionPole(point: LunarEclipseMapCoordinate): Boolean =
			point.latitude <= -89.999 || point.latitude >= 89.999

		if (polygon.none { isProjectionPole(it) }) return listOf(polygon + polygon.first())

		val result = mutableListOf<List<LunarEclipseMapCoordinate>>()
		var segment = mutableListOf<LunarEclipseMapCoordinate>()
		for (index in polygon.indices) {
			val start = polygon[index]
			val end = polygon[(index + 1) % polygon.size]
			if (!isProjectionPole(start) && !isProjectionPole(end)) {
				if (segment.isEmpty()) segment.add(start)
				segment.add(end)
			} else if (segment.size >= 2) {
				result.add(segment)
				segment = mutableListOf()
			}
		}
		if (segment.size >= 2) result.add(segment)
		return result
	}

	private fun updateMarkerCollection(frame: LunarEclipseMapFrame?) {
		val point = frame?.sublunarPoint
		if (point == null) {
			sublunarMarker?.setIsHidden(true)
			return
		}
		val position = PointI(
			MapUtils.get31TileNumberX(point.longitude),
			MapUtils.get31TileNumberY(point.latitude)
		)
		val marker = sublunarMarker
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
		sublunarMarker = builtMarker
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

	private fun List<LunarEclipseMapCoordinate>.toPoints31(): QVectorPointI =
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

	private fun clearPolygonCollection() {
		val renderer = mapRenderer
		polygonsCollection?.let { renderer?.removeSymbolsProvider(it) }
		polygonsCollection = null
		nativePolygonIds.clear()
		polygonId = 1
	}

	private fun clearBoundaryCollection() {
		val renderer = mapRenderer
		boundaryCollection?.let { renderer?.removeSymbolsProvider(it) }
		boundaryCollection = null
		nativeBoundaryLines.clear()
	}

	private fun clearGeometryCollections() {
		clearPolygonCollection()
		clearBoundaryCollection()
		nativeGeometryDirty = true
	}

	private fun clearNativeCollections() {
		clearGeometryCollections()
		clearMapMarkersCollections()
		mapMarkersCollection = null
		sublunarMarker = null
	}

	override fun cleanupResources() {
		super.cleanupResources()
		clearNativeCollections()
	}

	override fun onDraw(canvas: Canvas, tileBox: RotatedTileBox, settings: DrawSettings) = Unit

	override fun drawInScreenPixels(): Boolean = false
}
