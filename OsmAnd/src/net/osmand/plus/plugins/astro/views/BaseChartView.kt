package net.osmand.plus.plugins.astro.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import io.github.cosinekitty.astronomy.Body
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

	protected val visibleBodies = listOf(
		Body.Sun, Body.Moon, Body.Mercury, Body.Venus,
		Body.Mars, Body.Jupiter, Body.Saturn
	)

	// Coroutine scope for background calculations
	protected val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
	private var calculationJob: Job? = null

	// Flag to track if a calculation is running to avoid cancellation starvation
	private var isComputing = false
	// Flag to indicate a pending update is requested
	private var pendingRebuild = false

	// Config holding only primitive data types needed for rendering calculations
	protected var config = Config()

	/**
	 * Implementation must compute the model on a background thread.
	 * DO NOT touch View properties (width/height) inside here unless passed as arguments.
	 */
	protected abstract suspend fun computeModel(config: Config, width: Int, height: Int): Any?

	/**
	 * Called on Main Thread when the model is ready. Update your cached variables and invalidate.
	 */
	protected abstract fun onModelReady(model: Any?)

	fun updateData(latitude: Double, longitude: Double, date: LocalDate = LocalDate.now()) {
		val newConfig = config.copy(
			latitude = latitude,
			longitude = longitude,
			date = date,
			zoneId = ZoneId.systemDefault()
		)

		if (!config.equalsTo(newConfig)) {
			config = newConfig
			triggerAsyncRebuild()
		} else {
			invalidate()
		}
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		if (w > 0 && h > 0) {
			triggerAsyncRebuild()
		}
	}

	protected fun triggerAsyncRebuild() {
		// Don't calculate if we have no size yet
		if (width == 0 || height == 0) return

		// Mark that we need a rebuild
		pendingRebuild = true

		// If already computing, let the current job finish.
		// It will loop back and check pendingRebuild to pick up the latest state.
		if (isComputing) return

		calculationJob = viewScope.launch {
			isComputing = true
			// Keep processing as long as there are pending requests
			while (pendingRebuild && isActive) {
				// Consume the flag immediately
				pendingRebuild = false

				val w = width
				val h = height
				val currentConfig = config

				// Offload heavy math to Default dispatcher
				val result = withContext(Dispatchers.Default) {
					computeModel(currentConfig, w, h)
				}

				// Back on Main Thread
				if (isActive) {
					onModelReady(result)
					invalidate()
				}
			}
			isComputing = false
		}
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		viewScope.cancel()
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