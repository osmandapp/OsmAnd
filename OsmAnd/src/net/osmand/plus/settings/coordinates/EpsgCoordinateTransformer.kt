package net.osmand.plus.settings.coordinates

import net.osmand.PlatformUtil
import net.osmand.core.jni.CoordinateTransformer
import net.osmand.core.jni.PointD
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import org.apache.commons.logging.Log
import java.util.concurrent.ConcurrentHashMap

class EpsgCoordinateTransformer(private val app: OsmandApplication) {

	private val transformers = ConcurrentHashMap<Int, CoordinateTransformer>()

	fun fromLonLat(epsgCode: Int, lon: Double, lat: Double): EpsgTransformResult<EpsgPoint> {
		val transformer = getTransformer(epsgCode)
			?: return EpsgTransformResult.failure("Coordinate transformer is unavailable")
		return try {
			val point = PointD(lon, lat)
			if (transformer.fromLonLat(point)) {
				EpsgTransformResult.success(EpsgPoint(point.getX(), point.getY()))
			} else {
				EpsgTransformResult.failure("Coordinate transform failed")
			}
		} catch (e: Throwable) {
			LOG.error("Failed to transform lon/lat to EPSG:$epsgCode", e)
			EpsgTransformResult.failure("Coordinate transform failed")
		}
	}

	fun toLonLat(epsgCode: Int, easting: Double, northing: Double): EpsgTransformResult<LatLon> {
		val transformer = getTransformer(epsgCode)
			?: return EpsgTransformResult.failure("Coordinate transformer is unavailable")
		return try {
			val point = PointD(easting, northing)
			if (transformer.toLonLat(point)) {
				EpsgTransformResult.success(LatLon(point.getY(), point.getX()))
			} else {
				EpsgTransformResult.failure("Coordinate transform failed")
			}
		} catch (e: Throwable) {
			LOG.error("Failed to transform EPSG:$epsgCode to lon/lat", e)
			EpsgTransformResult.failure("Coordinate transform failed")
		}
	}

	private fun getTransformer(epsgCode: Int): CoordinateTransformer? {
		if (epsgCode <= 0) {
			return null
		}
		transformers[epsgCode]?.let { return it }
		return try {
			val projResourcesPath = app.getAppPath(null).absolutePath
			CoordinateTransformer(projResourcesPath, epsgCode).also { transformers[epsgCode] = it }
		} catch (e: Throwable) {
			LOG.error("Failed to create EPSG coordinate transformer: $epsgCode", e)
			null
		}
	}

	private companion object {
		private val LOG: Log = PlatformUtil.getLog(EpsgCoordinateTransformer::class.java)
	}
}

data class EpsgPoint(
	val easting: Double,
	val northing: Double
)

class EpsgTransformResult<T> private constructor(
	val value: T?,
	val errorMessage: String?
) {
	val isSuccess: Boolean
		get() = value != null

	companion object {
		@JvmStatic
		fun <T> success(value: T): EpsgTransformResult<T> {
			return EpsgTransformResult(value, null)
		}

		@JvmStatic
		fun <T> failure(errorMessage: String): EpsgTransformResult<T> {
			return EpsgTransformResult(null, errorMessage)
		}
	}
}
