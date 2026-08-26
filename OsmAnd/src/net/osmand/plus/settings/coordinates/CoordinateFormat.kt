package net.osmand.plus.settings.coordinates

import net.osmand.plus.OsmandApplication

enum class CoordinateFormatType {
	BUILT_IN,
	EPSG,
	UNKNOWN
}

data class CoordinateFormat(
	val id: String,
	val type: CoordinateFormatType,
	val title: String,
	val subtitle: String? = null,
	val epsgCode: Int? = null,
	val legacyFormat: Int? = null,
	val isDeprecated: Boolean = false,
	val isResolved: Boolean = true
) {
	companion object {
		@JvmStatic
		fun builtIn(id: String, title: String, legacyFormat: Int): CoordinateFormat {
			return CoordinateFormat(
				id = id,
				type = CoordinateFormatType.BUILT_IN,
				title = title,
				legacyFormat = legacyFormat
			)
		}

		@JvmStatic
		fun epsg(code: Int, title: String?, subtitle: String?, isDeprecated: Boolean): CoordinateFormat {
			val fallbackTitle = "EPSG:$code"
			return CoordinateFormat(
				id = CoordinateFormatIds.epsg(code),
				type = CoordinateFormatType.EPSG,
				title = title?.takeIf { it.isNotEmpty() } ?: fallbackTitle,
				subtitle = subtitle,
				epsgCode = code,
				isDeprecated = isDeprecated
			)
		}

		@JvmStatic
		fun unresolvedEpsg(code: Int): CoordinateFormat {
			return CoordinateFormat(
				id = CoordinateFormatIds.epsg(code),
				type = CoordinateFormatType.EPSG,
				title = "EPSG:$code",
				epsgCode = code,
				isResolved = false
			)
		}

		@JvmStatic
		fun unknown(id: String): CoordinateFormat {
			return CoordinateFormat(
				id = id,
				type = CoordinateFormatType.UNKNOWN,
				title = id,
				isResolved = false
			)
		}

		@JvmStatic
		fun resolve(app: OsmandApplication, id: String, epsgCatalogRepository: EpsgCatalogRepository): CoordinateFormat {
			BuiltInCoordinateFormat.resolve(app, id)?.let { return it }
			return epsgCatalogRepository.resolveFormat(id)
		}
	}
}
