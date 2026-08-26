package net.osmand.plus.settings.coordinates

import net.osmand.LocationConvert
import net.osmand.data.PointDescription
import net.osmand.plus.OsmandApplication

enum class BuiltInCoordinateFormat(val id: String, val legacyFormat: Int) {
	DDD(CoordinateFormatIds.BUILTIN_DDD, LocationConvert.FORMAT_DEGREES),
	DDM(CoordinateFormatIds.BUILTIN_DDM, LocationConvert.FORMAT_MINUTES),
	DMS(CoordinateFormatIds.BUILTIN_DMS, LocationConvert.FORMAT_SECONDS),
	UTM(CoordinateFormatIds.BUILTIN_UTM, LocationConvert.UTM_FORMAT),
	OLC(CoordinateFormatIds.BUILTIN_OLC, LocationConvert.OLC_FORMAT),
	MGRS(CoordinateFormatIds.BUILTIN_MGRS, LocationConvert.MGRS_FORMAT),
	SWISS_GRID(CoordinateFormatIds.BUILTIN_SWISS_GRID, LocationConvert.SWISS_GRID_FORMAT),
	SWISS_GRID_PLUS(CoordinateFormatIds.BUILTIN_SWISS_GRID_PLUS, LocationConvert.SWISS_GRID_PLUS_FORMAT),
	MAIDENHEAD(CoordinateFormatIds.BUILTIN_MAIDENHEAD, LocationConvert.MAIDENHEAD_FORMAT);

	fun toCoordinateFormat(app: OsmandApplication): CoordinateFormat {
		return CoordinateFormat.builtIn(id, PointDescription.formatToHumanString(app, legacyFormat), legacyFormat)
	}

	companion object {
		@JvmStatic
		fun getAll(app: OsmandApplication): List<CoordinateFormat> {
			return entries.map { it.toCoordinateFormat(app) }
		}

		@JvmStatic
		fun fromId(id: String?): BuiltInCoordinateFormat? {
			val normalized = CoordinateFormatIds.normalize(id) ?: return null
			return entries.firstOrNull { it.id == normalized }
		}

		@JvmStatic
		fun resolve(app: OsmandApplication, id: String?): CoordinateFormat? {
			return fromId(id)?.toCoordinateFormat(app)
		}
	}
}
