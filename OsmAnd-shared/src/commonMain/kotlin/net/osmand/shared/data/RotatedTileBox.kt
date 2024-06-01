package net.osmand.shared.data

import net.osmand.shared.extensions.toRadians
import net.osmand.shared.util.Algorithms
import net.osmand.shared.util.MapUtils
import kotlin.math.*

class RotatedTileBox private constructor() {

	private var lat: Double = 0.0
	private var lon: Double = 0.0
	private var height: Float = 0f
	private var rotate: Float = 0f
	private var density: Float = 0f
	private var zoom: Int = 0
	private var mapDensity: Double = 1.0
	private var zoomAnimation: Double = 0.0
	private var zoomFloatPart: Double = 0.0
	private var cx: Int = 0
	private var cy: Int = 0
	private var pixWidth: Int = 0
	private var pixHeight: Int = 0
	private var ratiocx: Float = 0f
	private var ratiocy: Float = 0f

	private var zoomFactor: Double = 0.0
	private var rotateCos: Double = 0.0
	private var rotateSin: Double = 0.0
	private var oxTile: Double = 0.0
	private var oyTile: Double = 0.0
	private var tileBounds: QuadRect? = null
	private var latLonBounds: QuadRect? = null
	private var rotatedLatLonBounds: MutableList<LatLon>? = null
	private var tileLT: QuadPointDouble? = null
	private var tileRT: QuadPointDouble? = null
	private var tileRB: QuadPointDouble? = null
	private var tileLB: QuadPointDouble? = null

	constructor(r: RotatedTileBox) : this() {
		this.pixWidth = r.pixWidth
		this.pixHeight = r.pixHeight
		this.lat = r.lat
		this.lon = r.lon
		this.zoom = r.zoom
		this.mapDensity = r.mapDensity
		this.zoomFloatPart = r.zoomFloatPart
		this.zoomAnimation = r.zoomAnimation
		this.rotate = r.rotate
		this.density = r.density
		this.cx = r.cx
		this.cy = r.cy
		this.ratiocx = r.ratiocx
		this.ratiocy = r.ratiocy
		copyDerivedFields(r)
	}

	private fun copyDerivedFields(r: RotatedTileBox) {
		zoomFactor = r.zoomFactor
		rotateCos = r.rotateCos
		rotateSin = r.rotateSin
		oxTile = r.oxTile
		oyTile = r.oyTile
		if (r.tileBounds != null && r.latLonBounds != null && !r.rotatedLatLonBounds.isNullOrEmpty()) {
			tileBounds = QuadRect(r.tileBounds!!.left, r.tileBounds!!.top, r.tileBounds!!.right, r.tileBounds!!.bottom)
			latLonBounds = QuadRect(r.latLonBounds!!.left, r.latLonBounds!!.top, r.latLonBounds!!.right, r.latLonBounds!!.bottom)
			tileLT = QuadPointDouble(r.tileLT!!.x, r.tileLT!!.y)
			tileRT = QuadPointDouble(r.tileRT!!.x, r.tileRT!!.y)
			tileRB = QuadPointDouble(r.tileRB!!.x, r.tileRB!!.y)
			tileLB = QuadPointDouble(r.tileLB!!.x, r.tileLB!!.y)

			rotatedLatLonBounds = r.rotatedLatLonBounds!!.map { LatLon(it.latitude, it.longitude) }.toMutableList()
		}
	}

	fun calculateDerivedFields() {
		zoomFactor = 2.0.pow(zoomAnimation + zoomFloatPart) * 256 * mapDensity
		val rad = this.rotate.toDouble().toRadians()
		rotateCos = cos(rad)
		rotateSin = sin(rad)
		oxTile = MapUtils.getTileNumberX(zoom.toDouble(), lon)
		oyTile = MapUtils.getTileNumberY(zoom.toDouble(), lat)
		while (rotate < 0) {
			rotate += 360
		}
		while (rotate > 360) {
			rotate -= 360
		}
		tileBounds = null
	}

	fun getLatFromPixel(x: Float, y: Float): Double {
		return MapUtils.getLatitudeFromTile(zoom.toDouble(), getTileYFromPixel(x, y))
	}

	fun getLonFromPixel(x: Float, y: Float): Double {
		return MapUtils.getLongitudeFromTile(zoom.toDouble(), getTileXFromPixel(x, y))
	}

	fun getLatLonFromPixel(x: Float, y: Float): LatLon {
		return LatLon(getLatFromPixel(x, y), getLonFromPixel(x, y))
	}

	fun getCenterLatLon(): LatLon {
		return LatLon(lat, lon)
	}

	fun getCenterPixelPoint(): QuadPoint {
		return QuadPoint(cx.toFloat(), cy.toFloat())
	}

	fun getCenterPixelX(): Int {
		return cx
	}

	fun getCenterPixelY(): Int {
		return cy
	}

	fun setDensity(density: Float) {
		this.density = density
	}

	fun getCenterTileX(): Double {
		return oxTile
	}

	fun getCenter31X(): Int {
		return MapUtils.get31TileNumberX(lon)
	}

	fun getCenter31Y(): Int {
		return MapUtils.get31TileNumberY(lat)
	}

	fun getCenterTileY(): Double {
		return oyTile
	}

	protected fun getTileXFromPixel(x: Float, y: Float): Double {
		val dx = x - cx
		val dy = y - cy
		val dtilex: Double
		if (isMapRotateEnabled()) {
			dtilex = (rotateCos * dx + rotateSin * dy).toDouble()
		} else {
			dtilex = dx.toDouble()
		}
		return dtilex / zoomFactor + oxTile
	}

	protected fun getTileYFromPixel(x: Float, y: Float): Double {
		val dx = x - cx
		val dy = y - cy
		val dtiley: Double
		if (isMapRotateEnabled()) {
			dtiley = (-rotateSin * dx + rotateCos * dy).toDouble()
		} else {
			dtiley = dy.toDouble()
		}
		return dtiley / zoomFactor + oyTile
	}

	fun getTileBounds(): QuadRect {
		checkTileRectangleCalculated()
		return tileBounds!!
	}

	fun calculateTileRectangle() {
		val x1 = getTileXFromPixel(0f, 0f)
		val x2 = getTileXFromPixel(pixWidth.toFloat(), 0f)
		val x3 = getTileXFromPixel(pixWidth.toFloat(), pixHeight.toFloat())
		val x4 = getTileXFromPixel(0f, pixHeight.toFloat())
		val y1 = getTileYFromPixel(0f, 0f)
		val y2 = getTileYFromPixel(pixWidth.toFloat(), 0f)
		val y3 = getTileYFromPixel(pixWidth.toFloat(), pixHeight.toFloat())
		val y4 = getTileYFromPixel(0f, pixHeight.toFloat())
		tileLT = QuadPointDouble(x1, y1)
		tileRT = QuadPointDouble(x2, y2)
		tileRB = QuadPointDouble(x3, y3)
		tileLB = QuadPointDouble(x4, y4)
		val l = minOf(minOf(x1, x2), minOf(x3, x4))
		val r = maxOf(maxOf(x1, x2), maxOf(x3, x4))
		val t = minOf(minOf(y1, y2), minOf(y3, y4))
		val b = maxOf(maxOf(y1, y2), maxOf(y3, y4))
		val bounds = QuadRect(l, t, r, b)
		val zoom = this.zoom.toDouble()
		val top = MapUtils.getLatitudeFromTile(zoom, alignTile(bounds.top))
		val left = MapUtils.getLongitudeFromTile(zoom, alignTile(bounds.left))
		val bottom = MapUtils.getLatitudeFromTile(zoom, alignTile(bounds.bottom))
		val right = MapUtils.getLongitudeFromTile(zoom, alignTile(bounds.right))
		tileBounds = bounds
		latLonBounds = QuadRect(left, top, right, bottom)

		rotatedLatLonBounds = mutableListOf(
			LatLon(MapUtils.getLatitudeFromTile(zoom, alignTile(y1)), MapUtils.getLongitudeFromTile(
				zoom, alignTile(x1))),
			LatLon(MapUtils.getLatitudeFromTile(zoom, alignTile(y2)), MapUtils.getLongitudeFromTile(
				zoom, alignTile(x2))),
			LatLon(MapUtils.getLatitudeFromTile(zoom, alignTile(y3)), MapUtils.getLongitudeFromTile(
				zoom, alignTile(x3))),
			LatLon(MapUtils.getLatitudeFromTile(zoom, alignTile(y4)), MapUtils.getLongitudeFromTile(
				zoom, alignTile(x4)))
		)
	}

	private fun alignTile(tile: Double): Double {
		if (tile < 0) {
			return 0.0
		}
		val zoom = this.zoom.toDouble()
		if (tile >= MapUtils.getPowZoom(zoom)) {
			return MapUtils.getPowZoom(zoom) - 0.000001
		}
		return tile
	}

	fun getPixDensity(): Double {
		val dist = getDistance(0, pixHeight / 2, pixWidth, pixHeight / 2)
		return pixWidth / dist
	}

	fun getPixWidth(): Int {
		return pixWidth
	}

	fun getPixHeight(): Int {
		return pixHeight
	}

	fun getPixXFrom31(x31: Int, y31: Int): Float {
		val zm = MapUtils.getPowZoom(31.0 - zoom)
		val xTile = x31.toDouble() / zm
		val yTile = y31.toDouble() / zm
		return getPixXFromTile(xTile, yTile)
	}

	fun getPixYFrom31(x31: Int, y31: Int): Float {
		val zm = MapUtils.getPowZoom(31.0 - zoom)
		val xTile = x31.toDouble() / zm
		val yTile = y31.toDouble() / zm
		return getPixYFromTile(xTile, yTile)
	}

	fun getPixXFromLatLon(latitude: Double, longitude: Double): Float {
		val xTile = MapUtils.getTileNumberX(zoom.toDouble(), longitude)
		val yTile = MapUtils.getTileNumberY(zoom.toDouble(), latitude)
		return getPixXFromTile(xTile, yTile)
	}

	fun getPixXFromTile(tileX: Double, tileY: Double, zoom: Double): Float {
		val pw = MapUtils.getPowZoom(zoom - this.zoom)
		val xTile = tileX / pw
		val yTile = tileY / pw
		return getPixXFromTile(xTile, yTile)
	}

	protected fun getPixXFromTile(xTile: Double, yTile: Double): Float {
		val rotX: Double
		val dTileX = xTile - oxTile
		val dTileY = yTile - oyTile
		if (isMapRotateEnabled()) {
			rotX = (rotateCos * dTileX - rotateSin * dTileY)
		} else {
			rotX = dTileX
		}
		val dx = rotX * zoomFactor
		return (dx + cx).toFloat()
	}

	fun getPixYFromLatLon(latitude: Double, longitude: Double): Float {
		val xTile = MapUtils.getTileNumberX(zoom.toDouble(), longitude)
		val yTile = MapUtils.getTileNumberY(zoom.toDouble(), latitude)
		return getPixYFromTile(xTile, yTile)
	}

	fun getPixYFromTile(tileX: Double, tileY: Double, zoom: Double): Float {
		val pw = MapUtils.getPowZoom(zoom - this.zoom)
		val xTile = tileX / pw
		val yTile = tileY / pw
		return getPixYFromTile(xTile, yTile)
	}

	protected fun getPixYFromTile(xTile: Double, yTile: Double): Float {
		val dTileX = xTile - oxTile
		val dTileY = yTile - oyTile
		val rotY: Double
		if (isMapRotateEnabled()) {
			rotY = (rotateSin * dTileX + rotateCos * dTileY)
		} else {
			rotY = dTileY
		}
		val dy = rotY * zoomFactor
		return (dy + cy).toFloat()
	}

	fun getPixXFromLonNoRot(longitude: Double): Int {
		val dTilex = MapUtils.getTileNumberX(zoom.toDouble(), longitude) - oxTile
		return (dTilex * zoomFactor + cx).toInt()
	}

	fun getPixXFromTileXNoRot(tileX: Double): Int {
		val dTilex = tileX - oxTile
		return (dTilex * zoomFactor + cx).toInt()
	}

	fun getPixYFromLatNoRot(latitude: Double): Int {
		val dTileY = MapUtils.getTileNumberY(zoom.toDouble(), latitude) - oyTile
		return (dTileY * zoomFactor + cy).toInt()
	}

	fun getPixYFromTileYNoRot(tileY: Double): Int {
		val dTileY = tileY - oyTile
		return (dTileY * zoomFactor + cy).toInt()
	}

	private fun isMapRotateEnabled(): Boolean {
		return rotate != 0f
	}

	fun getLatLonBounds(): QuadRect {
		checkTileRectangleCalculated()
		return latLonBounds!!
	}

	fun getRotateCos(): Double {
		return rotateCos
	}

	fun getRotateSin(): Double {
		return rotateSin
	}

	fun getFullZoom(): Double {
		return getZoom() + getZoomFloatPart() + getZoomAnimation()
	}

	fun getZoom(): Int {
		return zoom
	}

	fun getDefaultRadiusPoi(): Int {
		val radius: Int
		val zoom = getZoom()
		radius = when {
			zoom <= 15 -> 10
			zoom <= 16 -> 14
			zoom <= 17 -> 16
			else -> 18
		}
		return (radius * getDensity()).toInt()
	}

	fun setLatLonCenter(lat: Double, lon: Double) {
		this.lat = lat
		this.lon = lon
		calculateDerivedFields()
	}

	fun setHeight(height: Float) {
		this.height = height
	}

	fun setRotate(rotate: Float) {
		this.rotate = rotate
		calculateDerivedFields()
	}

	fun increasePixelDimensions(dwidth: Int, dheight: Int) {
		this.pixWidth += 2 * dwidth
		this.pixHeight += 2 * dheight
		this.cx += dwidth
		this.cy += dheight
		calculateDerivedFields()
	}

	fun setPixelDimensions(width: Int, height: Int) {
		setPixelDimensions(width, height, 0.5f, 0.5f)
	}

	fun setPixelDimensions(width: Int, height: Int, ratiocx: Float, ratiocy: Float) {
		this.pixHeight = height
		this.pixWidth = width
		this.cx = (pixWidth * ratiocx).toInt()
		this.cy = (pixHeight * ratiocy).toInt()
		this.ratiocx = ratiocx
		this.ratiocy = ratiocy
		calculateDerivedFields()
	}

	fun isZoomAnimated(): Boolean {
		return zoomAnimation != 0.0
	}

	fun getZoomAnimation(): Double {
		return zoomAnimation
	}

	fun getZoomFloatPart(): Double {
		return zoomFloatPart
	}

	fun setZoomAndAnimation(zoom: Int, zoomAnimation: Double, zoomFloatPart: Double) {
		this.zoomAnimation = zoomAnimation
		this.zoomFloatPart = zoomFloatPart
		this.zoom = zoom
		calculateDerivedFields()
	}

	fun setZoomAndAnimation(zoom: Int, zoomAnimation: Double) {
		this.zoomAnimation = zoomAnimation
		this.zoom = zoom
		calculateDerivedFields()
	}

	fun setCenterLocation(ratiocx: Float, ratiocy: Float) {
		this.cx = (pixWidth * ratiocx).toInt()
		this.cy = (pixHeight * ratiocy).toInt()
		this.ratiocx = ratiocx
		this.ratiocy = ratiocy
		calculateDerivedFields()
	}

	fun isCenterShifted(): Boolean {
		return ratiocx != 0.5f || ratiocy != 0.5f
	}

	fun getLeftTopLatLon(): LatLon {
		checkTileRectangleCalculated()
		return LatLon(MapUtils.getLatitudeFromTile(zoom.toDouble(), alignTile(tileLT!!.y)),
			MapUtils.getLongitudeFromTile(zoom.toDouble(), alignTile(tileLT!!.x)))
	}

	fun getLeftTopTile(zoom: Double): QuadPointDouble {
		checkTileRectangleCalculated()
		return QuadPointDouble((tileLT!!.x * MapUtils.getPowZoom(zoom - this.zoom)),
			(tileLT!!.y * MapUtils.getPowZoom(zoom - this.zoom)))
	}

	fun getRightBottomTile(zoom: Double): QuadPointDouble {
		checkTileRectangleCalculated()
		return QuadPointDouble((tileRB!!.x * MapUtils.getPowZoom(zoom - this.zoom)),
			(tileRB!!.y * MapUtils.getPowZoom(zoom - this.zoom)))
	}

	private fun checkTileRectangleCalculated() {
		if (tileBounds == null) {
			calculateTileRectangle()
		}
	}

	fun getRightBottomLatLon(): LatLon {
		checkTileRectangleCalculated()
		return LatLon(MapUtils.getLatitudeFromTile(zoom.toDouble(), alignTile(tileRB!!.y)),
			MapUtils.getLongitudeFromTile(zoom.toDouble(), alignTile(tileRB!!.x)))
	}

	fun setMapDensity(mapDensity: Double) {
		this.mapDensity = mapDensity
		calculateDerivedFields()
	}

	fun getMapDensity(): Double {
		return mapDensity
	}

	fun setZoom(zoom: Int) {
		this.zoom = zoom
		calculateDerivedFields()
	}

	fun getHeight(): Float {
		return height
	}

	fun getRotate(): Float {
		return rotate
	}

	fun getDensity(): Float {
		return density
	}

	fun copy(): RotatedTileBox {
		return RotatedTileBox(this)
	}

	fun containsTileBox(box: RotatedTileBox): Boolean {
		checkTileRectangleCalculated()
		val localBox = box.copy()
		localBox.checkTileRectangleCalculated()
		if (!containsTilePoint(localBox.tileLB!!)) {
			return false
		}
		if (!containsTilePoint(localBox.tileLT!!)) {
			return false
		}
		if (!containsTilePoint(localBox.tileRB!!)) {
			return false
		}
		if (!containsTilePoint(localBox.tileRT!!)) {
			return false
		}
		return true
	}

	fun containsTilePoint(qp: QuadPoint): Boolean {
		val tx = getPixXFromTile(qp.x.toDouble(), qp.y.toDouble())
		val ty = getPixYFromTile(qp.x.toDouble(), qp.y.toDouble())
		return tx >= 0 && tx <= pixWidth && ty >= 0 && ty <= pixHeight
	}

	fun containsTilePoint(qp: QuadPointDouble): Boolean {
		val tx = getPixXFromTile(qp.x, qp.y)
		val ty = getPixYFromTile(qp.x, qp.y)
		return tx >= 0 && tx <= pixWidth && ty >= 0 && ty <= pixHeight
	}

	fun containsRectInRotatedRect(left: Double, top: Double, right: Double, bottom: Double): Boolean {
		val rect = mutableListOf(
			LatLon(top, left),
			LatLon(top, right),
			LatLon(bottom, right),
			LatLon(bottom, left),
			LatLon(top, left)
		)

		checkTileRectangleCalculated()
		val rotatedLatLonRect = rotatedLatLonBounds?.toMutableList()
		rotatedLatLonRect?.add(rotatedLatLonRect[0])
		return if (rotatedLatLonRect != null) {
			Algorithms.isFirstPolygonInsideSecond(rect, rotatedLatLonRect)
		} else {
			false
		}
	}

	fun containsLatLon(lat: Double, lon: Double): Boolean {
		val tx = getPixXFromLatLon(lat, lon)
		val ty = getPixYFromLatLon(lat, lon)
		return tx >= 0 && tx <= pixWidth && ty >= 0 && ty <= pixHeight
	}

	fun containsLatLon(latLon: LatLon): Boolean {
		val tx = getPixXFromLatLon(latLon.latitude, latLon.longitude)
		val ty = getPixYFromLatLon(latLon.latitude, latLon.longitude)
		return tx >= 0 && tx <= pixWidth && ty >= 0 && ty <= pixHeight
	}

	fun containsPoint(tx: Float, ty: Float, outMargin: Float): Boolean {
		return tx >= -outMargin && tx <= pixWidth + outMargin && ty >= -outMargin && ty <= pixHeight + outMargin
	}

	fun getDistance(pixX: Int, pixY: Int, pixX2: Int, pixY2: Int): Double {
		val lat1 = getLatFromPixel(pixX.toFloat(), pixY.toFloat())
		val lon1 = getLonFromPixel(pixX.toFloat(), pixY.toFloat())
		val lat2 = getLatFromPixel(pixX2.toFloat(), pixY2.toFloat())
		val lon2 = getLonFromPixel(pixX2.toFloat(), pixY2.toFloat())
		return MapUtils.getDistance(lat1, lon1, lat2, lon2)
	}

	fun isLatLonNearPixel(latLon: LatLon, centerPixX: Double, centerPixY: Double, radius: Double): Boolean {
		return isLatLonNearPixel(latLon.latitude, latLon.longitude, centerPixX, centerPixY, radius)
	}

	fun isLatLonNearPixel(lat: Double, lon: Double, centerPixX: Double, centerPixY: Double, radius: Double): Boolean {
		val pixelArea = QuadRect(
			centerPixX - radius,
			centerPixY - radius,
			centerPixX + radius,
			centerPixY + radius
		)
		return isLatLonInsidePixelArea(lat, lon, pixelArea)
	}

	fun isLatLonInsidePixelArea(latLon: LatLon, pixelArea: QuadRect): Boolean {
		return isLatLonInsidePixelArea(latLon.latitude, latLon.longitude, pixelArea)
	}

	fun isLatLonInsidePixelArea(lat: Double, lon: Double, pixelArea: QuadRect): Boolean {
		val pixX = getPixXFromLatLon(lat, lon).toDouble()
		val pixY = getPixYFromLatLon(lat, lon).toDouble()
		return pixelArea.contains(pixX, pixY, pixX, pixY)
	}

	class RotatedTileBoxBuilder {
		private val tb = RotatedTileBox()
		private var pixelDimensionsSet = false
		private var locationSet = false
		private var zoomSet = false

		fun density(d: Float): RotatedTileBoxBuilder {
			tb.density = d
			return this
		}

		fun setMapDensity(mapDensity: Double): RotatedTileBoxBuilder {
			tb.mapDensity = mapDensity
			return this
		}

		fun setZoom(zoom: Int): RotatedTileBoxBuilder {
			tb.zoom = zoom
			zoomSet = true
			return this
		}

		fun setZoomFloatPart(zoomFloatPart: Double): RotatedTileBoxBuilder {
			tb.zoomFloatPart = zoomFloatPart
			return this
		}

		fun setLocation(lat: Double, lon: Double): RotatedTileBoxBuilder {
			tb.lat = lat
			tb.lon = lon
			locationSet = true
			return this
		}

		fun setRotate(degrees: Float): RotatedTileBoxBuilder {
			tb.rotate = degrees
			return this
		}

		fun setPixelDimensions(pixWidth: Int, pixHeight: Int, centerX: Float, centerY: Float): RotatedTileBoxBuilder {
			tb.pixWidth = pixWidth
			tb.pixHeight = pixHeight
			tb.cx = (pixWidth * centerX).toInt()
			tb.cy = (pixHeight * centerY).toInt()
			tb.ratiocx = centerX
			tb.ratiocy = centerY
			pixelDimensionsSet = true
			return this
		}

		fun setPixelDimensions(pixWidth: Int, pixHeight: Int): RotatedTileBoxBuilder {
			return setPixelDimensions(pixWidth, pixHeight, 0.5f, 0.5f)
		}

		fun build(): RotatedTileBox {
			if (!pixelDimensionsSet) {
				throw IllegalArgumentException("Please specify pixel dimensions")
			}
			if (!zoomSet) {
				throw IllegalArgumentException("Please specify zoom")
			}
			if (!locationSet) {
				throw IllegalArgumentException("Please specify location")
			}

			tb.calculateDerivedFields()
			return tb
		}
	}

	fun getLongitude(): Double {
		return lon
	}

	fun getLatitude(): Double {
		return lat
	}

	override fun toString(): String {
		return "RotatedTileBox [lat=$lat, lon=$lon, rotate=$rotate, density=$density, zoom=$zoom, mapDensity=$mapDensity, zoomAnimation=$zoomAnimation, zoomFloatPart=$zoomFloatPart, cx=$cx, cy=$cy, pixWidth=$pixWidth, pixHeight=$pixHeight]"
	}
}
