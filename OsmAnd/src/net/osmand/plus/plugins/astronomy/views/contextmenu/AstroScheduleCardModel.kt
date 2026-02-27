package net.osmand.plus.plugins.astronomy.views.contextmenu

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
import java.util.ArrayList
import java.util.Locale

class AstroScheduleCardModel(app: OsmandApplication) : AstroContextCard(app) {

	data class ScheduleDayGraphData(
		val sunAltitudes: DoubleArray,
		val objectAltitudes: DoubleArray
	) {
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as ScheduleDayGraphData

			if (!sunAltitudes.contentEquals(other.sunAltitudes)) return false
			if (!objectAltitudes.contentEquals(other.objectAltitudes)) return false

			return true
		}

		override fun hashCode(): Int {
			var result = sunAltitudes.contentHashCode()
			result = 31 * result + objectAltitudes.contentHashCode()
			return result
		}
	}

	data class ScheduleDayEntry(
		val date: LocalDate,
		val dayLabel: String,
		val riseTime: String?,
		val setTime: String?,
		val graphData: ScheduleDayGraphData
	)

	var skyObject: SkyObject? = null
	var observer: Observer? = null
	var zoneId: ZoneId = ZoneId.systemDefault()
	var periodStart: LocalDate = LocalDate.now()
	var rangeLabel: String = ""
	var days: List<ScheduleDayEntry> = emptyList()
	var onDataChanged: (() -> Unit)? = null

	private var computeScope = createScope()
	private var computeJob: Job? = null

	private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
	private val dayLabelFormatter = DateTimeFormatter.ofPattern("EEE, d", Locale.getDefault())
	private val rangeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

	fun updateCard(
		skyObject: SkyObject?,
		observer: Observer?,
		periodStart: LocalDate,
		zoneId: ZoneId
	) {
		this.skyObject = skyObject
		this.observer = observer
		this.periodStart = periodStart
		this.zoneId = zoneId
		computeJob?.cancel()

		if (skyObject == null || observer == null) {
			rangeLabel = ""
			days = emptyList()
			onDataChanged?.invoke()
			return
		}

		val periodEnd = periodStart.plusDays((PERIOD_DAYS - 1).toLong())
		rangeLabel = "${rangeFormatter.format(periodStart)} - ${rangeFormatter.format(periodEnd)}"
		days = emptyList()

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
			days = entries
			onDataChanged?.invoke()
		}
	}

	fun cancelPendingComputations() {
		computeJob?.cancel()
		computeJob = null
	}

	private fun buildPeriodEntries(
		obj: SkyObject,
		observer: Observer,
		periodStart: LocalDate,
		zoneId: ZoneId
	): List<ScheduleDayEntry> {
		val entries = ArrayList<ScheduleDayEntry>(PERIOD_DAYS)
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
	): ScheduleDayEntry {
		val startLocal = day.atTime(12, 0).atZone(zoneId)
		val endLocal = startLocal.plusDays(1)
		val (rise, set) = AstroUtils.nextRiseSet(obj, startLocal, observer, startLocal, endLocal)
		val samples = AstroChartMath.computeDaySamples(
			objectToRender = obj,
			observer = observer,
			startLocal = startLocal,
			endLocal = endLocal,
			sampleCount = SAMPLE_COUNT,
			includeAzimuth = false
		)
		return ScheduleDayEntry(
			date = day,
			dayLabel = dayLabelFormatter.format(day),
			riseTime = rise?.format(timeFormatter),
			setTime = set?.format(timeFormatter),
			graphData = ScheduleDayGraphData(
				sunAltitudes = samples.sunAltitudes,
				objectAltitudes = samples.objectAltitudes
			)
		)
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
	}
}
