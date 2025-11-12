package net.osmand.plus.plugins.astro.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import io.github.cosinekitty.astronomy.Observer
import net.osmand.plus.OsmandApplication
import net.osmand.plus.views.OsmandMapTileView
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

open class BaseChartView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

	protected val mapTileView: OsmandMapTileView =
		(context.applicationContext as OsmandApplication).osmandMap.mapView

	sealed class BaseModel(
		open val startLocal: ZonedDateTime,
		open val endLocal: ZonedDateTime
	)

	sealed class BaseConfig(
		open val date: LocalDate,
		open val zoneId: ZoneId,
		open val latitude: Double,
		open val longitude: Double,
		open val elevation: Double
	) {
		companion object {
			const val LATLON_EPS = 0.001
			const val ELEVATION_EPS = 10.0
		}

		fun equalsTo(other: BaseConfig): Boolean {
			if (zoneId != other.zoneId) return false
			if (abs(latitude - other.latitude) > LATLON_EPS) return false
			if (abs(longitude - other.longitude) > LATLON_EPS) return false
			if (abs(elevation - other.elevation) > ELEVATION_EPS) return false
			return date.atStartOfDay(zoneId).toInstant() == other.date.atStartOfDay(other.zoneId)
				.toInstant()
		}
	}

	protected data class Twilight(
		val sunrise: ZonedDateTime?, val sunset: ZonedDateTime?,
		val civilDawn: ZonedDateTime?, val civilDusk: ZonedDateTime?,
		val nauticalDawn: ZonedDateTime?, val nauticalDusk: ZonedDateTime?,
		val astroDawn: ZonedDateTime?, val astroDusk: ZonedDateTime?
	)

}