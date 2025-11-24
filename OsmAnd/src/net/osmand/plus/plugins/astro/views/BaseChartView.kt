package net.osmand.plus.plugins.astro.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import net.osmand.plus.utils.AndroidUtils
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

abstract class BaseChartView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

	// Config holding only primitive data types needed for rendering calculations
	protected var config = Config()

	abstract fun rebuildModel()

	/**
	 * Pushes new data to the view. Checks for changes before triggering rebuild.
	 */
	fun updateData(latitude: Double, longitude: Double, date: LocalDate = LocalDate.now()) {
		val newConfig = config.copy(
			latitude = latitude,
			longitude = longitude,
			date = date,
			zoneId = ZoneId.systemDefault()
		)

		if (!config.equalsTo(newConfig)) {
			config = newConfig
			rebuildModel()
			invalidate()
		} else {
			// Even if config is same, ensure we redraw if something external triggered this
			invalidate()
		}
	}

	open class BaseModel(
		open val startLocal: ZonedDateTime,
		open val endLocal: ZonedDateTime
	)

	data class Config(
		val date: LocalDate = LocalDate.now(),
		val zoneId: ZoneId = ZoneId.systemDefault(),
		val latitude: Double = 0.0,
		val longitude: Double = 0.0,
		val elevation: Double = 0.0
	) {
		companion object {
			const val LATLON_EPS = 0.001
			const val ELEVATION_EPS = 10.0
		}

		fun equalsTo(other: Config): Boolean {
			if (zoneId != other.zoneId) return false
			if (abs(latitude - other.latitude) > LATLON_EPS) return false
			if (abs(longitude - other.longitude) > LATLON_EPS) return false
			if (abs(elevation - other.elevation) > ELEVATION_EPS) return false
			return date == other.date
		}
	}

	// Helper for DP conversion common to all charts
	protected fun dp(v: Float) = AndroidUtils.dpToPxF(context, v)
	protected fun sp(v: Float) = AndroidUtils.spToPxF(context, v)
}