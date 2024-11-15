package net.osmand.shared.util

import kotlinx.coroutines.runBlocking
import net.osmand.shared.data.KLatLon
import kotlin.math.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.extensions.toDegrees
import net.osmand.shared.extensions.toRadians

/**
 * This utility class includes :
 * 1. distance algorithms
 * 2. finding center for array of nodes
 * 3. tile evaluation algorithms
 */
object KMapUtils {

	const val ROUNDING_ERROR = 3
	private const val EARTH_RADIUS_B = 6356752
	private const val EARTH_RADIUS_A = 6378137
	const val MIN_LATITUDE = -85.0511
	const val MAX_LATITUDE = 85.0511
	const val LATITUDE_TURN = 180.0
	const val MIN_LONGITUDE = -180.0
	const val MAX_LONGITUDE = 180.0
	const val LONGITUDE_TURN = 360.0
	const val DEFAULT_LATLON_PRECISION = 0.00001
	const val HIGH_LATLON_PRECISION = 0.0000001

	private const val BASE_SHORT_OSM_URL = "https://openstreetmap.org/go/"

	private val intToBase64 = charArrayOf(
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
		'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
		'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
		'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_', '~'
	)

	fun calculateFromBaseZoomPrecisionXY(
		baseZoom: Int,
		finalZoom: Int,
		xFinal: Int,
		yFinal: Int
	): Int {
		var px = xFinal
		var py = yFinal
		var precisionNumber = 1
		for (zoom in finalZoom - 1 downTo baseZoom) {
			val x = px / 2
			val y = py / 2
			val deltax = px - x * 2
			val deltay = py - y * 2
			precisionNumber = (precisionNumber shl 2) + (deltax shl 1) + deltay
			px = x
			py = y
		}
		return precisionNumber
	}

	fun calculateFinalXYFromBaseAndPrecisionXY(
		baseZoom: Int,
		finalZoom: Int,
		precisionXY: Int,
		xBase: Int,
		yBase: Int,
		ignoreNotEnoughPrecision: Boolean
	): IntArray {
		var finalX = xBase
		var finalY = yBase
		var tPrecisionXY = precisionXY
		var precisionCalc = tPrecisionXY
		for (zoom in baseZoom until finalZoom) {
			if (precisionCalc <= 1 && precisionCalc > 0 && !ignoreNotEnoughPrecision) {
				throw IllegalArgumentException("Not enough bits to retrieve zoom approximation")
			}
			finalY = finalY * 2 + (tPrecisionXY and 1)
			finalX = finalX * 2 + ((tPrecisionXY and 2) shr 1)
			tPrecisionXY = tPrecisionXY shr 2
		}
		return intArrayOf(finalX, finalY)
	}

	fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
		val R = 6372.8 // for haversine use R = 6372.8 km instead of 6371 km
		val dLat = (lat2 - lat1).toRadians()
		val dLon = (lon2 - lon1).toRadians()
		val sinHalfLat = sin(dLat / 2)
		val sinHalfLon = sin(dLon / 2)
		val a =
			sinHalfLat * sinHalfLat + cos(lat1.toRadians()) * cos(lat2.toRadians()) * sinHalfLon * sinHalfLon
		return 2 * R * 1000 * asin(sqrt(a))
	}

	fun getDistance(l1: KLatLon, l2: KLatLon): Double {
		return getDistance(l1.latitude, l1.longitude, l2.latitude, l2.longitude)
	}

	fun getDistance(l: KLatLon, latitude: Double, longitude: Double): Double {
		return getDistance(l.latitude, l.longitude, latitude, longitude)
	}

	fun scalarMultiplication(
		xA: Double,
		yA: Double,
		xB: Double,
		yB: Double,
		xC: Double,
		yC: Double
	): Double {
		return (xB - xA) * (xC - xA) + (yB - yA) * (yC - yA)
	}

	fun calculateMidPoint(s1: KLatLon, s2: KLatLon): KLatLon {
		val latLon = calculateMidPoint(s1.latitude, s1.longitude, s2.latitude, s2.longitude)
		return KLatLon(latLon[0], latLon[1])
	}

	fun calculateMidPoint(
		firstLat: Double,
		firstLon: Double,
		secondLat: Double,
		secondLon: Double
	): DoubleArray {
		val lat1 = firstLat / 180 * PI
		val lon1 = firstLon / 180 * PI
		val lat2 = secondLat / 180 * PI
		val lon2 = secondLon / 180 * PI
		val Bx = cos(lat2) * cos(lon2 - lon1)
		val By = cos(lat2) * sin(lon2 - lon1)
		val latMid =
			atan2(sin(lat1) + sin(lat2), sqrt((cos(lat1) + Bx) * (cos(lat1) + Bx) + By * By))
		val lonMid = lon1 + atan2(By, cos(lat1) + Bx)
		return doubleArrayOf(checkLatitude(latMid * 180 / PI), checkLongitude(lonMid * 180 / PI))
	}

	fun calculateIntermediatePoint(
		fromLat: Double, fromLon: Double, toLat: Double, toLon: Double, coeff: Double
	): KLatLon {
		val lat1 = fromLat.toRadians()
		val lon1 = fromLon.toRadians()
		val lat2 = toLat.toRadians()
		val lon2 = toLon.toRadians()

		val lat1Cos = cos(lat1)
		val lat2Cos = cos(lat2)

		val d = 2 * asin(
			sqrt(
				sin((lat1 - lat2) / 2).pow(2.0) + lat1Cos * lat2Cos * sin((lon1 - lon2) / 2).pow(2.0)
			)
		)
		val A = sin((1 - coeff) * d) / sin(d)
		val B = sin(coeff * d) / sin(d)
		val x = A * lat1Cos * cos(lon1) + B * lat2Cos * cos(lon2)
		val y = A * lat1Cos * sin(lon1) + B * lat2Cos * sin(lon2)
		val z = A * sin(lat1) + B * sin(lat2)

		val lat = atan2(z, sqrt(x * x + y * y))
		val lon = atan2(y, x)
		return KLatLon(checkLatitude(lat * 180 / PI), checkLongitude(lon * 180 / PI))
	}

	fun getOrthogonalDistance(
		lat: Double, lon: Double, fromLat: Double, fromLon: Double, toLat: Double, toLon: Double
	): Double {
		return getDistance(getProjection(lat, lon, fromLat, fromLon, toLat, toLon), lat, lon)
	}

	fun getProjection(
		lat: Double,
		lon: Double,
		fromLat: Double,
		fromLon: Double,
		toLat: Double,
		toLon: Double
	): KLatLon {
		val mDist = (fromLat - toLat).pow(2.0) + (fromLon - toLon).pow(2.0)
		val projection = scalarMultiplication(fromLat, fromLon, toLat, toLon, lat, lon)
		val prlat: Double
		val prlon: Double
		when {
			projection < 0 -> {
				prlat = fromLat
				prlon = fromLon
			}

			projection >= mDist -> {
				prlat = toLat
				prlon = toLon
			}

			else -> {
				prlat = fromLat + (toLat - fromLat) * (projection / mDist)
				prlon = fromLon + (toLon - fromLon) * (projection / mDist)
			}
		}
		return KLatLon(prlat, prlon)
	}

	fun getProjectionCoeff(
		lat: Double, lon: Double, fromLat: Double, fromLon: Double, toLat: Double, toLon: Double
	): Double {
		val mDist = (fromLat - toLat).pow(2.0) + (fromLon - toLon).pow(2.0)
		val projection = scalarMultiplication(fromLat, fromLon, toLat, toLon, lat, lon)
		return when {
			projection < 0 -> 0.0
			projection >= mDist -> 1.0
			else -> projection / mDist
		}
	}

	fun checkLongitude(longitude: Double): Double {
		if (longitude in MIN_LONGITUDE..MAX_LONGITUDE) {
			return longitude
		}
		var adjustedLongitude = longitude
		while (adjustedLongitude <= MIN_LONGITUDE || adjustedLongitude > MAX_LONGITUDE) {
			adjustedLongitude += if (adjustedLongitude < 0) LONGITUDE_TURN else -LONGITUDE_TURN
		}
		return adjustedLongitude
	}

	fun checkLatitude(latitude: Double): Double {
		if (latitude in MIN_LATITUDE..MAX_LATITUDE) {
			return latitude
		}
		var adjustedLatitude = latitude
		while (adjustedLatitude < -90 || adjustedLatitude > 90) {
			adjustedLatitude += if (adjustedLatitude < 0) LATITUDE_TURN else -LATITUDE_TURN
		}
		return adjustedLatitude.coerceIn(MIN_LATITUDE, MAX_LATITUDE)
	}

	fun get31TileNumberX(longitude: Double): Int {
		val checkedLongitude = checkLongitude(longitude)
		val l = 1L shl 31
		return ((checkedLongitude + 180.0) / 360.0 * l).toInt()
	}

	fun get31TileNumberY(latitude: Double): Int {
		val checkedLatitude = checkLatitude(latitude)
		val eval = ln(tan(checkedLatitude.toRadians()) + 1 / cos(checkedLatitude.toRadians()))
		val l = 1L shl 31
		return ((1 - eval / PI) / 2 * l).toInt()
	}

	fun get31LongitudeX(tileX: Int): Double {
		return getLongitudeFromTile(21.0, tileX / 1024.0)
	}

	fun get31LatitudeY(tileY: Int): Double {
		return getLatitudeFromTile(21.0, tileY / 1024.0)
	}

	fun getTileNumberX(zoom: Double, longitude: Double): Double {
		val checkedLongitude = checkLongitude(longitude)
		val powZoom = getPowZoom(zoom)
		val dz = (checkedLongitude + 180.0) / 360.0 * powZoom
		return if (dz >= powZoom) powZoom - 0.01 else dz
	}

	fun getTileNumberY(zoom: Double, latitude: Double): Double {
		var checkedLatitude = checkLatitude(latitude)
		var eval = ln(tan(checkedLatitude.toRadians()) + 1 / cos(checkedLatitude.toRadians()))
		if (eval.isInfinite() || eval.isNaN()) {
			checkedLatitude = if (checkedLatitude < 0) -89.9 else 89.9
			eval = ln(tan(checkedLatitude.toRadians()) + 1 / cos(checkedLatitude.toRadians()))
		}
		return (1 - eval / PI) / 2 * getPowZoom(zoom)
	}

	fun getTileEllipsoidNumberY(zoom: Double, latitude: Double): Double {
		val E2 = latitude * PI / 180
		val sradiusa = EARTH_RADIUS_A.toDouble()
		val sradiusb = EARTH_RADIUS_B.toDouble()
		val J2 = sqrt(sradiusa * sradiusa - sradiusb * sradiusb) / sradiusa
		val M2 = (ln((1 + sin(E2)) / (1 - sin(E2))) / 2) -
				J2 * ln((1 + J2 * sin(E2)) / (1 - J2 * sin(E2))) / 2
		val B2 = getPowZoom(zoom)
		return B2 / 2 - M2 * B2 / 2 / PI
	}

	fun getTileEllipsoidNumberAndOffsetY(zoom: Int, latitude: Double, tileSize: Int): DoubleArray {
		val E2 = latitude * PI / 180
		val sradiusa = EARTH_RADIUS_A.toDouble()
		val sradiusb = EARTH_RADIUS_B.toDouble()
		val J2 = sqrt(sradiusa * sradiusa - sradiusb * sradiusb) / sradiusa
		val M2 = (ln((1 + sin(E2)) / (1 - sin(E2))) / 2) -
				J2 * ln((1 + J2 * sin(E2)) / (1 - J2 * sin(E2))) / 2
		val B2 = getPowZoom(zoom.toDouble())
		val tileY = B2 / 2 - M2 * B2 / 2 / PI

		val tilesCount = (1 shl zoom).toDouble()
		val yTileNumber = floor(tilesCount * (0.5 - M2 / 2 / PI))
		val offsetY = floor((tilesCount * (0.5 - M2 / 2 / PI) - yTileNumber) * tileSize)
		return doubleArrayOf(tileY, offsetY)
	}

	fun getLatitudeFromEllipsoidTileY(zoom: Double, tileNumberY: Double): Double {
		val MerkElipsK = 0.0000001
		val sradiusa = EARTH_RADIUS_A.toDouble()
		val sradiusb = EARTH_RADIUS_B.toDouble()
		val FExct = sqrt(sradiusa * sradiusa - sradiusb * sradiusb) / sradiusa
		val TilesAtZoom = getPowZoom(zoom)
		var result = (tileNumberY - TilesAtZoom / 2) / -(TilesAtZoom / (2 * PI))
		result = (2 * atan(exp(result)) - PI / 2) * 180 / PI
		var Zu = result / (180 / PI)
		val yy = tileNumberY - TilesAtZoom / 2

		var Zum1 = Zu
		Zu = asin(
			1 - ((1 + sin(Zum1)) * (1 - FExct * sin(Zum1)).pow(FExct)) /
					(exp((2 * yy) / -(TilesAtZoom / (2 * PI))) * (1 + FExct * sin(Zum1)).pow(FExct))
		)
		while (abs(Zum1 - Zu) >= MerkElipsK) {
			Zum1 = Zu
			Zu = asin(
				1 - ((1 + sin(Zum1)) * (1 - FExct * sin(Zum1)).pow(FExct)) /
						(exp((2 * yy) / -(TilesAtZoom / (2 * PI))) * (1 + FExct * sin(Zum1)).pow(
							FExct
						))
			)
		}

		return Zu * 180 / PI
	}

	fun getTileDistanceWidth(zoom: Double): Double {
		return getTileDistanceWidth(30.0, zoom)
	}

	fun getTileDistanceWidth(lat: Double, zoom: Double): Double {
		val ll = KLatLon(lat, getLongitudeFromTile(zoom, 0.0))
		val ll2 = KLatLon(lat, getLongitudeFromTile(zoom, 1.0))
		return getDistance(ll, ll2)
	}

	fun getTileDistanceHeight(lat: Double, zoom: Double): Double {
		val y = getTileNumberY(zoom, lat)
		val ll = KLatLon(getLatitudeFromTile(zoom, floor(y)), 0.0)
		val ll2 = KLatLon(getLatitudeFromTile(zoom, floor(y) + 1), 0.0)
		return getDistance(ll, ll2)
	}

	fun getLongitudeFromTile(zoom: Double, x: Double): Double {
		return x / getPowZoom(zoom) * 360.0 - 180.0
	}

	fun getPowZoom(zoom: Double): Double {
		return if (zoom >= 0 && zoom - floor(zoom) < 0.001f) {
			(1 shl zoom.toInt()).toDouble()
		} else {
			2.0.pow(zoom)
		}
	}

	fun calcDiffPixelX(
		rotateSin: Float,
		rotateCos: Float,
		dTileX: Float,
		dTileY: Float,
		tileSize: Float
	): Float {
		return (rotateCos * dTileX - rotateSin * dTileY) * tileSize
	}

	fun calcDiffPixelY(
		rotateSin: Float,
		rotateCos: Float,
		dTileX: Float,
		dTileY: Float,
		tileSize: Float
	): Float {
		return (rotateSin * dTileX + rotateCos * dTileY) * tileSize
	}

	fun getLatitudeFromTile(zoom: Double, y: Double): Double {
		val sign = if (y < 0) -1 else 1
		return atan(sign * sinh(PI * (1 - 2 * y / getPowZoom(zoom)))) * 180 / PI
	}

	fun getPixelShiftX(zoom: Double, long1: Double, long2: Double, tileSize: Double): Int {
		return ((getTileNumberX(zoom, long1) - getTileNumberX(zoom, long2)) * tileSize).toInt()
	}

	fun getPixelShiftY(zoom: Double, lat1: Double, lat2: Double, tileSize: Double): Int {
		return ((getTileNumberY(zoom, lat1) - getTileNumberY(zoom, lat2)) * tileSize).toInt()
	}

	fun buildGeoUrl(latitude: String, longitude: String, zoom: Int): String {
		return "geo:$latitude,$longitude?z=$zoom"
	}

	fun buildShortOsmUrl(latitude: Double, longitude: Double, zoom: Int): String {
		return "$BASE_SHORT_OSM_URL${createShortLinkString(latitude, longitude, zoom)}?m"
	}

	fun createShortLinkString(latitude: Double, longitude: Double, zoom: Int): String {
		val lat = ((latitude + 90) / 180 * (1L shl 32)).toLong()
		val lon = ((longitude + 180) / 360 * (1L shl 32)).toLong()
		val code = interleaveBits(lon, lat)
		var str = ""
		for (i in 0 until ceil((zoom + 8) / 3.0).toInt()) {
			str += intToBase64[(code shr (58 - 6 * i) and 0x3f).toInt()]
		}
		for (j in 0 until (zoom + 8) % 3) {
			str += '-'
		}
		return str
	}

	fun interleaveBits(x: Long, y: Long): Long {
		var c: Long = 0
		for (b in 31 downTo 0) {
			c = c shl 1 or (x shr b and 1)
			c = c shl 1 or (y shr b and 1)
		}
		return c
	}

	fun unifyRotationDiff(rotate: Float, targetRotate: Float): Float {
		var d = targetRotate - rotate
		while (d >= 180) {
			d -= 360
		}
		while (d < -180) {
			d += 360
		}
		return d
	}

	fun unifyRotationTo360(rotate: Float): Float {
		var rotateVar = rotate
		while (rotateVar < -180) {
			rotateVar += 360
		}
		while (rotateVar > +180) {
			rotateVar -= 360
		}
		return rotateVar
	}

	fun normalizeDegrees360(degrees: Float): Float {
		var degreesVar = degrees
		while (degreesVar < 0.0f) {
			degreesVar += 360.0f
		}
		while (degreesVar >= 360.0f) {
			degreesVar -= 360.0f
		}
		return degreesVar
	}

	fun alignAngleDifference(diff: Double): Double {
		var diffVar = diff
		while (diffVar > PI) {
			diffVar -= 2 * PI
		}
		while (diffVar <= -PI) {
			diffVar += 2 * PI
		}
		return diffVar
	}

	fun degreesDiff(a1: Double, a2: Double): Double {
		var diff = a1 - a2
		while (diff > 180) {
			diff -= 360
		}
		while (diff <= -180) {
			diff += 360
		}
		return diff
	}

	fun squareRootDist31(x1: Int, y1: Int, x2: Int, y2: Int): Double {
		return sqrt(squareDist31TileMetric(x1, y1, x2, y2))
	}

	fun measuredDist31(x1: Int, y1: Int, x2: Int, y2: Int): Double {
		return getDistance(
			get31LatitudeY(y1),
			get31LongitudeX(x1),
			get31LatitudeY(y2),
			get31LongitudeX(x2)
		)
	}

	const val EQUATOR = 1 shl 30
	fun squareDist31TileMetric(x1: Int, y1: Int, x2: Int, y2: Int): Double {
		val top1 = y1 > EQUATOR
		val top2 = y2 > EQUATOR
		if (top1 != top2 && y1 != EQUATOR && y2 != EQUATOR) {
			val mx = x1 / 2 + x2 / 2
			val d1 = sqrt(squareDist31TileMetric(mx, EQUATOR, x2, y2))
			val d2 = sqrt(squareDist31TileMetric(mx, EQUATOR, x1, y1))
			return (d1 + d2).pow(2.0)
		}
		val ymidx = y1 / 2 + y2 / 2
		val tw = runBlocking { getTileWidth(ymidx) }

		val dy = (y1 - y2) * tw
		val dx = (x2 - x1) * tw
		return dx * dx + dy * dy
	}

	private const val PRECISION_ZOOM = 14
	private val DIST_CACHE = mutableMapOf<Int, Double>()
	private val distCacheMutex = Mutex()
	private suspend fun getTileWidth(y31: Int): Double {
		val y = y31 / (1.0 * (1 shl (31 - PRECISION_ZOOM)))
		var tileY = y.toInt()
		val ry = y - tileY
		var d: Double? = null
		var dp: Double? = null
		try {
			d = DIST_CACHE[tileY]
		} catch (e: RuntimeException) {
		}
		if (d == null) {
			distCacheMutex.withLock {
				val td = getTileDistanceWidth(
					get31LatitudeY(tileY shl (31 - PRECISION_ZOOM)),
					PRECISION_ZOOM.toDouble()
				) / (1 shl (31 - PRECISION_ZOOM))
				d = td
				DIST_CACHE[tileY] = td
			}
		}
		tileY += 1
		try {
			dp = DIST_CACHE[tileY]
		} catch (_: RuntimeException) {
		}
		if (dp == null) {
			distCacheMutex.withLock {
				val tdp = getTileDistanceWidth(
					get31LatitudeY(tileY shl (31 - PRECISION_ZOOM)),
					PRECISION_ZOOM.toDouble()
				) / (1 shl (31 - PRECISION_ZOOM))
				dp = tdp
				DIST_CACHE[tileY] = tdp
			}
		}
		return ry * dp!! + (1 - ry) * d!!
	}

	fun rightSide(
		lat: Double, lon: Double, aLat: Double, aLon: Double, bLat: Double, bLon: Double
	): Boolean {
		val ax = aLon - lon
		val ay = aLat - lat
		val bx = bLon - lon
		val by = bLat - lat
		val sa = ax * by - bx * ay
		return sa < 0
	}

	fun deinterleaveY(coord: Long): Long {
		var x: Long = 0
		for (b in 31 downTo 0) {
			x = x shl 1 or (1L and coord shr (b * 2))
		}
		return x
	}

	fun deinterleaveX(coord: Long): Long {
		var x: Long = 0
		for (b in 31 downTo 0) {
			x = x shl 1 or (1L and coord shr (b * 2 + 1))
		}
		return x
	}

	fun calculateLatLonBbox(latitude: Double, longitude: Double, radiusMeters: Int): KQuadRect {
		val zoom = 16.0
		val coeff = radiusMeters / getTileDistanceWidth(zoom)
		val tx = getTileNumberX(zoom, longitude)
		val ty = getTileNumberY(zoom, latitude)
		val topLeftX = max(0.0, tx - coeff)
		val topLeftY = max(0.0, ty - coeff)
		val max = (1 shl zoom.toInt()) - 1
		val bottomRightX = min(max.toDouble(), tx + coeff)
		val bottomRightY = min(max.toDouble(), ty + coeff)
		val pw = getPowZoom(31 - zoom)
		val rect = KQuadRect(topLeftX * pw, topLeftY * pw, bottomRightX * pw, bottomRightY * pw)
		rect.left = get31LongitudeX(rect.left.toInt())
		rect.top = get31LatitudeY(rect.top.toInt())
		rect.right = get31LongitudeX(rect.right.toInt())
		rect.bottom = get31LatitudeY(rect.bottom.toInt())
		return rect
	}

	fun getInterpolatedY(x1: Float, y1: Float, x2: Float, y2: Float, x: Float): Float {
		val a = y1 - y2
		val b = x2 - x1
		val d = -a * b
		return if (d != 0f) {
			val c1 = y2 * x1 - x2 * y1
			val c2 = x * (y2 - y1)
			(a * (c1 - c2)) / d
		} else {
			y1
		}
	}

	fun insetLatLonRect(r: KQuadRect, latitude: Double, longitude: Double) {
		if (r.left == 0.0 && r.right == 0.0) {
			r.left = longitude
			r.right = longitude
			r.top = latitude
			r.bottom = latitude
		} else {
			r.left = min(r.left, longitude)
			r.right = max(r.right, longitude)
			r.top = max(r.top, latitude)
			r.bottom = min(r.bottom, latitude)
		}
	}

	fun areLatLonEqual(l1: KLatLon?, l2: KLatLon?): Boolean {
		return l1 == null && l2 == null || l2 != null && areLatLonEqual(
			l1,
			l2.latitude,
			l2.longitude
		)
	}

	fun areLatLonEqual(l: KLatLon?, lat: Double, lon: Double): Boolean {
		return l != null && areLatLonEqual(l.latitude, l.longitude, lat, lon)
	}

	fun areLatLonEqual(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Boolean {
		return areLatLonEqual(lat1, lon1, lat2, lon2, DEFAULT_LATLON_PRECISION)
	}

	fun areLatLonEqual(l1: KLatLon?, l2: KLatLon?, precision: Double): Boolean {
		return l1 == null && l2 == null || l1 != null && l2 != null && areLatLonEqual(
			l1.latitude,
			l1.longitude,
			l2.latitude,
			l2.longitude,
			precision
		)
	}

	fun areLatLonEqual(
		lat1: Double,
		lon1: Double,
		lat2: Double,
		lon2: Double,
		precision: Double
	): Boolean {
		return abs(lat1 - lat2) < precision && abs(lon1 - lon2) < precision
	}

	fun rhumbDestinationPoint(latLon: KLatLon, distance: Double, bearing: Double): KLatLon {
		return rhumbDestinationPoint(latLon.latitude, latLon.longitude, distance, bearing)
	}

	fun rhumbDestinationPoint(lat: Double, lon: Double, distance: Double, bearing: Double): KLatLon {
		val radius = EARTH_RADIUS_A.toDouble()

		val d = distance / radius // angular distance in radians
		val phi1 = lat.toRadians()
		val lambda1 = lon.toRadians()
		val theta = bearing.toRadians()

		val deltaPhi = d * cos(theta)
		val phi2 = phi1 + deltaPhi

		val deltaPsi = ln(tan(phi2 / 2 + PI / 4) / tan(phi1 / 2 + PI / 4))
		val q = if (abs(deltaPsi) > 10e-12) deltaPhi / deltaPsi else cos(phi1)

		val deltaLambda = d * sin(theta) / q
		val lambda2 = lambda1 + deltaLambda

		return KLatLon(phi2.toDegrees(), lambda2.toDegrees())
	}

	fun getSqrtDistance(startX: Int, startY: Int, endX: Int, endY: Int): Double {
		return sqrt(((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY)).toDouble())
	}

	fun getSqrtDistance(startX: Float, startY: Float, endX: Float, endY: Float): Double {
		return sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY).toDouble())
	}

	fun convertDistToChar(
		dist: Int,
		firstLetter: Char,
		firstDist: Int,
		mult1: Int,
		mult2: Int
	): String {
		var iteration = 0
		var currentDist = firstDist
		while (dist - currentDist > 0) {
			iteration++
			currentDist *= if (iteration % 2 == 1) mult1 else mult2
		}
		return (firstLetter + iteration).toString()
	}

	fun convertCharToDist(
		ch: Char,
		firstLetter: Char,
		firstDist: Int,
		mult1: Int,
		mult2: Int
	): Int {
		var dist = firstDist
		for (iteration in 1 until ch - firstLetter + 1) {
			dist *= if (iteration % 2 == 1) mult1 else mult2
		}
		return dist
	}
}
