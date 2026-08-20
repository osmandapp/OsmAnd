package net.osmand.plus.settings.coordinates

import net.osmand.PlatformUtil
import net.osmand.core.jni.CoordinateTransformer
import net.osmand.core.jni.GridConfiguration
import net.osmand.core.jni.GridConfiguration.Format
import net.osmand.core.jni.GridConfiguration.Projection
import net.osmand.core.jni.PointD
import net.osmand.plus.OsmandApplication
import net.osmand.plus.settings.enums.GridFormat
import org.apache.commons.logging.Log
import java.util.concurrent.ConcurrentHashMap

class CoordinateGridFormatProvider(
	private val app: OsmandApplication,
	private val repository: EpsgCatalogRepository
) {

	private val resolvedFormats = ConcurrentHashMap<String, CoordinateGridFormat>()
	private val unsupportedFormats = ConcurrentHashMap.newKeySet<String>()

	@Synchronized
	fun resolve(formatId: String?): CoordinateGridFormat? {
		val normalizedId = normalizeId(formatId) ?: return null
		resolvedFormats[normalizedId]?.let { return it }
		if (normalizedId in unsupportedFormats) {
			return null
		}

		val builtInFormat = GridFormat.fromCoordinateFormatId(normalizedId)
		val epsgCode = builtInFormat?.epsgCode ?: CoordinateFormatIds.getEpsgCode(normalizedId)
		val resolved = if (epsgCode == null) {
			builtInFormat?.let {
				CoordinateGridFormat(
					id = normalizedId,
					projection = it.projection,
					format = it.format,
					needSuffixes = it.needSuffixes()
				)
			}
		} else {
			resolveProjectedFormat(normalizedId, epsgCode, builtInFormat)
		}

		if (resolved == null) {
			unsupportedFormats.add(normalizedId)
		} else {
			resolvedFormats[normalizedId] = resolved
		}
		return resolved
	}

	fun isSupported(formatId: String?): Boolean {
		val normalizedId = normalizeId(formatId) ?: return false
		val builtInFormat = GridFormat.fromCoordinateFormatId(normalizedId)
		val epsgCode = builtInFormat?.epsgCode ?: CoordinateFormatIds.getEpsgCode(normalizedId)
		return (epsgCode == null && builtInFormat != null) ||
			(epsgCode != null && repository.getGridDefinition(epsgCode) != null)
	}

	fun filterSupportedIds(formatIds: List<String>): List<String> {
		return formatIds.mapNotNull(::normalizeId)
			.filter(::isSupported)
			.distinct()
	}

	private fun resolveProjectedFormat(
		formatId: String,
		epsgCode: Int,
		builtInFormat: GridFormat?
	): CoordinateGridFormat? {
		val definition = repository.getGridDefinition(epsgCode) ?: return null
		val projection = getProjection(definition.projectionMethodCode) ?: return null
		val projectionParameters = resolveProjectionParameters(definition, projection) ?: return null
		return CoordinateGridFormat(
			id = formatId,
			projection = projection,
			format = Format.Decimal,
			needSuffixes = builtInFormat?.needSuffixes() ?: false,
			projectionParameters = projectionParameters
		)
	}

	private fun resolveProjectionParameters(
		definition: EpsgGridDefinition,
		projection: Projection
	): CoordinateGridProjectionParameters? {
		val constants = readProjectionConstants(definition.epsgCode, projection) ?: return null
		if (definition.usesWgs84) {
			// Custom projections still need an initialized Helmert scale in GridConfiguration.
			return constants.withEllipsoid(CoordinateGridEllipsoidParameters.IDENTITY, null)
		}

		for (operationCode in definition.transformationCodes) {
			val ellipsoid = readEllipsoidParameters(definition.epsgCode, operationCode)
			if (ellipsoid != null) {
				return constants.withEllipsoid(ellipsoid, operationCode)
			}
		}
		return null
	}

	private fun readProjectionConstants(
		epsgCode: Int,
		projection: Projection
	): CoordinateGridProjectionConstants? {
		return try {
			val resourcesPath = app.getAppPath(null).absolutePath
			val transformer = CoordinateTransformer(resourcesPath, epsgCode)
			try {
				val lonBounds = PointD()
				val latBounds = PointD()
				val semiMajorAxisAndInverseFlattening = PointD()
				val refLonLat = PointD()
				val falseEastingAndNorthing = PointD()
				val scaleFactor = PointD()
				if (!transformer.getConstants(
						projection,
						lonBounds,
						latBounds,
						semiMajorAxisAndInverseFlattening,
						refLonLat,
						falseEastingAndNorthing,
						scaleFactor
					)
				) {
					return null
				}
				if (!hasValidProjectionConstants(
						lonBounds,
						latBounds,
						semiMajorAxisAndInverseFlattening,
						scaleFactor
					)
				) {
					return null
				}

				CoordinateGridProjectionConstants(
					lonBounds = lonBounds.toValue(),
					latBounds = latBounds.toValue(),
					semiMajorAxisAndInverseFlattening = semiMajorAxisAndInverseFlattening.toValue(),
					refLonLat = refLonLat.toValue(),
					falseEastingAndNorthing = falseEastingAndNorthing.toValue(),
					scaleFactor = scaleFactor.toValue()
				)
			} finally {
				transformer.delete()
			}
		} catch (e: Throwable) {
			LOG.error("Failed to read Coordinate Grid constants for EPSG:$epsgCode", e)
			null
		}
	}

	private fun hasValidProjectionConstants(
		lonBounds: PointD,
		latBounds: PointD,
		semiMajorAxisAndInverseFlattening: PointD,
		scaleFactor: PointD
	): Boolean {
		return lonBounds.getX().isFinite() && lonBounds.getY().isFinite() &&
			latBounds.getX().isFinite() && latBounds.getY().isFinite() &&
			lonBounds.getX() < lonBounds.getY() && latBounds.getX() < latBounds.getY() &&
			semiMajorAxisAndInverseFlattening.getX().isFinite() &&
			semiMajorAxisAndInverseFlattening.getX() > 0.0 &&
			scaleFactor.getX().isFinite() && scaleFactor.getX() > 0.0 &&
			scaleFactor.getY().isFinite()
	}

	private fun readEllipsoidParameters(
		epsgCode: Int,
		operationCode: Int
	): CoordinateGridEllipsoidParameters? {
		return try {
			val resourcesPath = app.getAppPath(null).absolutePath
			val transformer = CoordinateTransformer(resourcesPath, epsgCode, operationCode)
			try {
				val translationsXY = PointD()
				val translationsZW = PointD()
				val rotationsXY = PointD()
				val rotationsZScale = PointD()
				if (transformer.getEllipsoidParameters(
						translationsXY,
						translationsZW,
						rotationsXY,
						rotationsZScale
					)
				) {
					CoordinateGridEllipsoidParameters(
						translationsXY.toValue(),
						translationsZW.toValue(),
						rotationsXY.toValue(),
						rotationsZScale.toValue()
					)
				} else {
					null
				}
			} finally {
				transformer.delete()
			}
		} catch (e: Throwable) {
			LOG.error(
				"Failed to read ellipsoid parameters for EPSG:$epsgCode using operation EPSG:$operationCode",
				e
			)
			null
		}
	}

	private fun getProjection(methodCode: Int): Projection? = when (methodCode) {
		TRANSVERSE_MERCATOR_METHOD -> Projection.TM
		OBLIQUE_STEREOGRAPHIC_METHOD -> Projection.OSTEREO
		HOTINE_OBLIQUE_MERCATOR_V2_METHOD -> Projection.HOMV2
		else -> null
	}

	private fun resolveLegacyId(formatId: String?): String? {
		val legacyFormat = formatId?.let { value ->
			GridFormat.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
		}
		return legacyFormat?.coordinateFormatId
	}

	private fun normalizeId(formatId: String?): String? {
		return CoordinateFormatIds.normalize(formatId) ?: resolveLegacyId(formatId)
	}

	private fun PointD.toValue(): CoordinateGridPoint {
		return CoordinateGridPoint(getX(), getY())
	}

	private companion object {
		private val LOG: Log = PlatformUtil.getLog(CoordinateGridFormatProvider::class.java)
		private const val TRANSVERSE_MERCATOR_METHOD = 9807
		private const val OBLIQUE_STEREOGRAPHIC_METHOD = 9809
		private const val HOTINE_OBLIQUE_MERCATOR_V2_METHOD = 9815
	}
}

private data class CoordinateGridProjectionConstants(
	val lonBounds: CoordinateGridPoint,
	val latBounds: CoordinateGridPoint,
	val semiMajorAxisAndInverseFlattening: CoordinateGridPoint,
	val refLonLat: CoordinateGridPoint,
	val falseEastingAndNorthing: CoordinateGridPoint,
	val scaleFactor: CoordinateGridPoint
) {
	fun withEllipsoid(
		ellipsoidParameters: CoordinateGridEllipsoidParameters,
		operationCode: Int?
	): CoordinateGridProjectionParameters {
		return CoordinateGridProjectionParameters(
			lonBounds = lonBounds,
			latBounds = latBounds,
			semiMajorAxisAndInverseFlattening = semiMajorAxisAndInverseFlattening,
			refLonLat = refLonLat,
			falseEastingAndNorthing = falseEastingAndNorthing,
			scaleFactor = scaleFactor,
			ellipsoidParameters = ellipsoidParameters,
			operationCode = operationCode
		)
	}
}

data class CoordinateGridFormat(
	val id: String,
	val projection: Projection,
	val format: Format,
	val needSuffixes: Boolean,
	val projectionParameters: CoordinateGridProjectionParameters? = null
) {

	val granularity: Float?
		get() = when (projection) {
			Projection.OLC -> OLC_GRID_GRANULARITY
			Projection.MLS -> MLS_GRID_GRANULARITY
			else -> null
		}

	val maxZoom: Int?
		get() = when (projection) {
			Projection.OLC -> OLC_GRID_MAX_ZOOM
			else -> null
		}

	fun applyProjectionConfiguration(configuration: GridConfiguration) {
		configuration.setSecondaryProjection(projection)
		configuration.setSecondaryFormat(format)
		projectionParameters?.let {
			configuration.setProjectionParameters()
			it.applyTo(configuration, projection)
		}
	}

	private companion object {
		private const val OLC_GRID_GRANULARITY = 3.0f
		private const val MLS_GRID_GRANULARITY = 6.0f
		private const val OLC_GRID_MAX_ZOOM = 18
	}
}

data class CoordinateGridProjectionParameters(
	val lonBounds: CoordinateGridPoint,
	val latBounds: CoordinateGridPoint,
	val semiMajorAxisAndInverseFlattening: CoordinateGridPoint,
	val refLonLat: CoordinateGridPoint,
	val falseEastingAndNorthing: CoordinateGridPoint,
	val scaleFactor: CoordinateGridPoint,
	val ellipsoidParameters: CoordinateGridEllipsoidParameters,
	val operationCode: Int?
) {
	fun applyTo(configuration: GridConfiguration, projection: Projection) {
		configuration.setSecondaryProjectionConstants(
			projection,
			lonBounds.toNative(),
			latBounds.toNative(),
			semiMajorAxisAndInverseFlattening.toNative(),
			refLonLat.toNative(),
			falseEastingAndNorthing.toNative(),
			scaleFactor.toNative()
		)
		ellipsoidParameters.applyTo(configuration)
	}

	fun contains(latitude: Double, longitude: Double): Boolean {
		val latitudeRadians = Math.toRadians(latitude)
		val longitudeRadians = Math.toRadians(longitude)
		val withinLatitude = latitudeRadians in latBounds.x..latBounds.y
		val withinLongitude = if (lonBounds.x <= lonBounds.y) {
			longitudeRadians in lonBounds.x..lonBounds.y
		} else {
			longitudeRadians >= lonBounds.x || longitudeRadians <= lonBounds.y
		}
		return withinLatitude && withinLongitude
	}
}

data class CoordinateGridEllipsoidParameters(
	val translationsXY: CoordinateGridPoint,
	val translationsZW: CoordinateGridPoint,
	val rotationsXY: CoordinateGridPoint,
	val rotationsZScale: CoordinateGridPoint
) {
	fun applyTo(configuration: GridConfiguration) {
		configuration.setSecondaryEllipsoidParameters(
			translationsXY.toNative(),
			translationsZW.toNative(),
			rotationsXY.toNative(),
			rotationsZScale.toNative()
		)
	}

	companion object {
		val IDENTITY = CoordinateGridEllipsoidParameters(
			translationsXY = CoordinateGridPoint.ZERO,
			translationsZW = CoordinateGridPoint.ZERO,
			rotationsXY = CoordinateGridPoint.ZERO,
			rotationsZScale = CoordinateGridPoint(0.0, 1.0)
		)
	}
}

data class CoordinateGridPoint(val x: Double, val y: Double) {
	fun toNative(): PointD = PointD(x, y)

	companion object {
		val ZERO = CoordinateGridPoint(0.0, 0.0)
	}
}