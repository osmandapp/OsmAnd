package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.text.format.DateFormat
import io.github.cosinekitty.astronomy.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.astronomy.SkyObject
import net.osmand.plus.plugins.astronomy.utils.AstroUtils
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.ArrayList
import java.util.Locale

class AstroScheduleCardController(
	private val app: OsmandApplication
) {

	var skyObject: SkyObject? = null
		private set
	var observer: Observer? = null
		private set
	var zoneId: ZoneId = ZoneId.systemDefault()
		private set
	var periodStart: LocalDate = LocalDate.now()
		private set
	var rangeLabel: String = ""
		private set
	var days: List<AstroScheduleDayItem> = emptyList()
		private set
	var showResetPeriodButton: Boolean = false
		private set
	var onDataChanged: (() -> Unit)? = null

	private var computeScope = createScope()
	private var computeJob: Job? = null
	private var lastObjectId: String? = null
	private var lastObserverLat = Double.NaN
	private var lastObserverLon = Double.NaN
	private var lastObserverHeight = Double.NaN
	private var lastPeriodStart: LocalDate? = null
	private var lastZoneId: ZoneId? = null
	private var lastShowResetPeriodButton = false

	private val dayLabelFormatter = DateTimeFormatter.ofPattern("EEE, d", Locale.getDefault())
	private val rangeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

	fun update(
		skyObject: SkyObject?,
		observer: Observer?,
		periodStart: LocalDate,
		zoneId: ZoneId,
		showResetPeriodButton: Boolean
	) {
		this.skyObject = skyObject
		this.observer = observer
		this.periodStart = periodStart
		this.zoneId = zoneId
		this.showResetPeriodButton = showResetPeriodButton

		if (skyObject == null || observer == null) {
			computeJob?.cancel()
			rangeLabel = ""
			days = emptyList()
			lastObjectId = null
			lastObserverLat = Double.NaN
			lastObserverLon = Double.NaN
			lastObserverHeight = Double.NaN
			lastPeriodStart = null
			lastZoneId = null
			lastShowResetPeriodButton = false
			onDataChanged?.invoke()
			return
		}

		val periodEnd = periodStart.plusDays((PERIOD_DAYS - 1).toLong())
		rangeLabel = "${rangeFormatter.format(periodStart)} - ${rangeFormatter.format(periodEnd)}"
		val computationMatchesState =
			lastObjectId == skyObject.id &&
				lastObserverLat == observer.latitude &&
				lastObserverLon == observer.longitude &&
				lastObserverHeight == observer.height &&
				lastPeriodStart == periodStart &&
				lastZoneId == zoneId &&
				lastShowResetPeriodButton == showResetPeriodButton &&
				days.isNotEmpty()
		if (computationMatchesState) {
			return
		}

		computeJob?.cancel()
		ensureScope()
		computeJob = computeScope.launch {
			val entries = withContext(Dispatchers.Default) {
				buildPeriodEntries(
					obj = skyObject,
					observer = observer,
					periodStart = periodStart,
					zoneId = zoneId
				)
			}
			if (!isActive) {
				return@launch
			}
			if (
				this@AstroScheduleCardController.skyObject?.id != skyObject.id ||
				this@AstroScheduleCardController.observer?.latitude != observer.latitude ||
				this@AstroScheduleCardController.observer?.longitude != observer.longitude ||
				this@AstroScheduleCardController.observer?.height != observer.height ||
				this@AstroScheduleCardController.periodStart != periodStart ||
				this@AstroScheduleCardController.zoneId != zoneId
			) {
				return@launch
			}
			if (days != entries) {
				days = entries
			}
			lastObjectId = skyObject.id
			lastObserverLat = observer.latitude
			lastObserverLon = observer.longitude
			lastObserverHeight = observer.height
			lastPeriodStart = periodStart
			lastZoneId = zoneId
			lastShowResetPeriodButton = showResetPeriodButton
			onDataChanged?.invoke()
		}
	}

	fun buildItem(): AstroScheduleCardItem? {
		if (skyObject == null || observer == null) {
			return null
		}
		return AstroScheduleCardItem(
			periodStart = periodStart,
			rangeLabel = rangeLabel,
			days = days,
			showResetPeriodButton = showResetPeriodButton
		)
	}

	fun cancelPendingWork() {
		computeJob?.cancel()
		computeJob = null
	}

	private fun buildPeriodEntries(
		obj: SkyObject,
		observer: Observer,
		periodStart: LocalDate,
		zoneId: ZoneId
	): List<AstroScheduleDayItem> {
		val entries = ArrayList<AstroScheduleDayItem>(PERIOD_DAYS)
		for (offset in 0 until PERIOD_DAYS) {
			val day = periodStart.plusDays(offset.toLong())
			entries.add(buildDayEntry(obj, observer, day, zoneId))
		}
		return entries
	}

	private fun buildDayEntry(
		obj: SkyObject,
		observer: Observer,
		day: LocalDate,
		zoneId: ZoneId
	): AstroScheduleDayItem {
		val startLocal = day.atStartOfDay(zoneId)
		val endLocal = startLocal.plusDays(1)
		val dayEndInclusive = endLocal.minusNanos(1)
		val (rise, _) = AstroUtils.nextRiseSet(
			obj = obj,
			startSearch = startLocal,
			obs = observer,
			windowStart = startLocal,
			windowEnd = dayEndInclusive
		)
		val set = rise?.let {
			AstroUtils.nextRiseSet(
				obj = obj,
				startSearch = it,
				obs = observer,
				limitDays = SET_SEARCH_LIMIT_DAYS
			).second
		}
		val setDayOffset = if (set != null) {
			ChronoUnit.DAYS.between(day, set.toLocalDate()).toInt().coerceAtLeast(0)
		} else {
			0
		}
		val samples = AstroChartMath.computeDaySamples(
			objectToRender = obj,
			observer = observer,
			startLocal = startLocal,
			endLocal = endLocal,
			sampleCount = SAMPLE_COUNT,
			includeAzimuth = false
		)
		val timeFormatter = createTimeFormatter()
		return AstroScheduleDayItem(
			date = day,
			dayLabel = dayLabelFormatter.format(day),
			riseTime = rise?.format(timeFormatter),
			setTime = set?.format(timeFormatter),
			setDayOffset = setDayOffset,
			graph = AstroScheduleDayGraphSnapshot(
				sunAltitudes = samples.sunAltitudes,
				objectAltitudes = samples.objectAltitudes
			)
		)
	}

	private fun createTimeFormatter(): DateTimeFormatter {
		return if (DateFormat.is24HourFormat(app)) {
			DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
		} else {
			DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
		}
	}

	private fun createScope(): CoroutineScope {
		return CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
	}

	private fun ensureScope() {
		val scopeJob = computeScope.coroutineContext[Job]
		if (scopeJob == null || !scopeJob.isActive) {
			computeScope = createScope()
		}
	}

	companion object {
		const val PERIOD_DAYS: Int = 7
		private const val SAMPLE_COUNT: Int = AstroChartMath.SCHEDULE_SAMPLE_COUNT
		private const val SET_SEARCH_LIMIT_DAYS = 5.0
	}
}
