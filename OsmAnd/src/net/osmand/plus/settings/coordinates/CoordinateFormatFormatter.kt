package net.osmand.plus.settings.coordinates

import net.osmand.plus.OsmandApplication
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.util.TextDirectionUtil
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CoordinateFormatFormatter @JvmOverloads constructor(
	private val app: OsmandApplication,
	private val epsgTransformer: EpsgCoordinateTransformer = app.coordinateFormatHelper.transformer
) {

	@JvmOverloads
	fun format(format: CoordinateFormat, lat: Double, lon: Double, forceLTR: Boolean = true): String {
		if (format.type == CoordinateFormatType.BUILT_IN && format.legacyFormat != null) {
			return OsmAndFormatter.getFormattedCoordinates(lat, lon, format.legacyFormat, forceLTR)
		}
		val epsgCode = format.epsgCode
		if (epsgCode != null) {
			val result = epsgTransformer.fromLonLat(epsgCode, lon, lat)
			val point = result.value
			if (point != null) {
				return formatEpsgPoint(point, forceLTR)
			}
		}
		return if (forceLTR) TextDirectionUtil.markAsLTR(UNAVAILABLE_PLACEHOLDER) else UNAVAILABLE_PLACEHOLDER
	}

	fun formatExample(format: CoordinateFormat, lat: Double, lon: Double): String {
		return format(format, lat, lon)
	}

	companion object {
		const val UNAVAILABLE_PLACEHOLDER = "—"

		private val EPSG_DECIMAL_FORMAT = DecimalFormat(
			"#,##0.00",
			DecimalFormatSymbols(Locale.US).apply {
				decimalSeparator = '.'
				groupingSeparator = ' '
			}
		).apply {
			minimumFractionDigits = 2
			maximumFractionDigits = 2
		}

		@JvmStatic
		@JvmOverloads
		fun formatEpsgPoint(point: EpsgPoint, forceLTR: Boolean = true): String {
			val formatted = "${formatEpsgValue(point.easting)}, ${formatEpsgValue(point.northing)}"
			return if (forceLTR) TextDirectionUtil.markAsLTR(formatted) else formatted
		}

		@JvmStatic
		fun formatEpsgValue(value: Double): String {
			return synchronized(EPSG_DECIMAL_FORMAT) {
				EPSG_DECIMAL_FORMAT.format(value)
			}
		}

		@JvmStatic
		fun resolve(app: OsmandApplication, id: String?): CoordinateFormat {
			if (id == null) {
				return getPrimaryFormat(app)
			}
			BuiltInCoordinateFormat.resolve(app, id)?.let { return it }
			return app.coordinateFormatHelper.repository.resolveFormat(id)
		}

		@JvmStatic
		fun getPrimaryFormat(app: OsmandApplication): CoordinateFormat {
			return getPrimaryFormat(app, app.settings.applicationMode)
		}

		@JvmStatic
		fun getPrimaryFormat(app: OsmandApplication, mode: ApplicationMode): CoordinateFormat {
			val primaryId = app.settings.coordinateFormatSettingsStorage.getPrimaryId(mode)
			return resolve(app, primaryId)
		}

		@JvmStatic
		fun getPrimaryTitle(app: OsmandApplication, mode: ApplicationMode): String {
			return getPrimaryFormat(app, mode).title
		}

		@JvmStatic
		@JvmOverloads
		fun formatPrimary(app: OsmandApplication, lat: Double, lon: Double, forceLTR: Boolean = true): String {
			return app.coordinateFormatHelper.formatter.format(getPrimaryFormat(app), lat, lon, forceLTR)
		}

		@JvmStatic
		fun getPreferredFormats(app: OsmandApplication): List<CoordinateFormat> {
			val repository = app.coordinateFormatHelper.repository
			return app.settings.coordinateFormatSettingsStorage.getPreferredIds()
				.map { id ->
					BuiltInCoordinateFormat.resolve(app, id) ?: repository.resolveFormat(id)
				}
		}

		@JvmStatic
		fun formatPreferred(app: OsmandApplication, lat: Double, lon: Double): List<FormattedCoordinate> {
			val formatter = app.coordinateFormatHelper.formatter
			return getPreferredFormats(app).mapIndexed { index, format ->
				FormattedCoordinate(format, formatter.format(format, lat, lon), index == 0)
			}
		}
	}
}

data class FormattedCoordinate(
	val format: CoordinateFormat,
	val text: String,
	val primary: Boolean
) {
	companion object {
		@JvmStatic
		fun plain(text: String): FormattedCoordinate {
			return FormattedCoordinate(CoordinateFormat.unknown(""), text, false)
		}
	}
}
