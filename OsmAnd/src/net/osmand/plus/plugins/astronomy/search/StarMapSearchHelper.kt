package net.osmand.plus.plugins.astronomy.search

import android.content.Context
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import net.osmand.plus.R
import net.osmand.plus.plugins.astronomy.utils.AstroUtils
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

internal class StarMapSearchHelper(
	private val context: Context,
	private val uiUtilities: UiUtilities,
	private val nightMode: Boolean
) {

	private data class RiseSetCacheEntry(
		val nextRise: ZonedDateTime?,
		val nextSet: ZonedDateTime?
	)

	private val riseSetCache = ConcurrentHashMap<String, RiseSetCacheEntry>()
	private val visibleTonightCache = ConcurrentHashMap<String, Boolean>()
	private var computationContext = StarMapSearchComputationContext(
		observer = Observer(0.0, 0.0, 0.0),
		now = ZonedDateTime.now(),
		dusk = ZonedDateTime.now(),
		dawn = ZonedDateTime.now().plusHours(12)
	)

	fun updateComputationContext(computationContext: StarMapSearchComputationContext) {
		this.computationContext = computationContext
		riseSetCache.clear()
		visibleTonightCache.clear()
	}

	fun getVisibleTonight(entry: StarMapSearchEntry): Boolean {
		if (entry.visibleTonightCalculated) {
			return entry.isVisibleTonight
		}
		visibleTonightCache[entry.objectRef.id]?.let { cachedVisibleTonight ->
			entry.isVisibleTonight = cachedVisibleTonight
			entry.visibleTonightCalculated = true
			return cachedVisibleTonight
		}
		val (nightRise, nightSet) = AstroUtils.nextRiseSet(
			entry.objectRef,
			computationContext.dusk,
			computationContext.observer,
			computationContext.dusk,
			computationContext.dawn
		)
		val visibleAtDusk = AstroUtils.altitude(entry.objectRef, computationContext.dusk, computationContext.observer) > 0
		entry.isVisibleTonight = visibleAtDusk || nightRise != null || nightSet != null
		entry.visibleTonightCalculated = true
		visibleTonightCache[entry.objectRef.id] = entry.isVisibleTonight
		return entry.isVisibleTonight
	}

	fun getRiseSortValue(entry: StarMapSearchEntry): Long {
		ensureRiseSet(entry)
		return entry.nextRise?.toInstant()?.toEpochMilli() ?: Long.MAX_VALUE
	}

	fun getSetSortValue(entry: StarMapSearchEntry): Long {
		ensureRiseSet(entry)
		return entry.nextSet?.toInstant()?.toEpochMilli() ?: Long.MAX_VALUE
	}

	fun resolveEventText(entry: StarMapSearchEntry): CharSequence {
		ensureRiseSet(entry)
		val rise = entry.nextRise
		val set = entry.nextSet
		val eventText = when {
			rise != null && set != null -> {
				if (rise.isBefore(set)) {
					formatEvent(rise, isRise = true)
				} else {
					formatEvent(set, isRise = false)
				}
			}
			rise != null -> formatEvent(rise, isRise = true)
			set != null -> formatEvent(set, isRise = false)
			entry.objectRef.altitude > 0 -> context.getString(R.string.astro_search_always_up)
			else -> context.getString(R.string.astro_search_never_rises)
		}
		return replaceEventArrowWithIcon(eventText)
	}

	private fun ensureRiseSet(entry: StarMapSearchEntry) {
		if (entry.riseSetCalculated) {
			return
		}
		riseSetCache[entry.objectRef.id]?.let { cachedRiseSet ->
			entry.nextRise = cachedRiseSet.nextRise
			entry.nextSet = cachedRiseSet.nextSet
			entry.riseSetCalculated = true
			return
		}
		val (rise, set) = AstroUtils.nextRiseSet(entry.objectRef, computationContext.now, computationContext.observer)
		entry.nextRise = rise
		entry.nextSet = set
		entry.riseSetCalculated = true
		riseSetCache[entry.objectRef.id] = RiseSetCacheEntry(rise, set)
	}

	private fun formatEvent(time: ZonedDateTime, isRise: Boolean): String {
		val formattedTime = AstroUtils.formatLocalTime(
			context,
			Time.fromMillisecondsSince1970(time.toInstant().toEpochMilli())
		)
		val daysBetween = ChronoUnit.DAYS.between(computationContext.now.toLocalDate(), time.toLocalDate())
		return if (daysBetween == 1L) {
			val tomorrow = context.getString(R.string.tomorrow)
			if (isRise) {
				context.getString(R.string.astro_search_rises_tomorrow, tomorrow, formattedTime)
			} else {
				context.getString(R.string.astro_search_sets_tomorrow, tomorrow, formattedTime)
			}
		} else if (isRise) {
			context.getString(R.string.astro_search_rises_at, formattedTime)
		} else {
			context.getString(R.string.astro_search_sets_at, formattedTime)
		}
	}

	private fun replaceEventArrowWithIcon(text: String): CharSequence {
		val (arrow, iconRes) = when {
			text.contains(RISE_ARROW) -> RISE_ARROW to R.drawable.ic_action_arrow_top_right_16
			text.contains(SET_ARROW) -> SET_ARROW to R.drawable.ic_action_arrow_bottom_right_16
			text.contains(UP_ARROW) -> UP_ARROW to R.drawable.ic_action_arrow_up2_16
			text.contains(DOWN_ARROW) -> DOWN_ARROW to R.drawable.ic_action_arrow_down2_16
			else -> return text
		}
		val icon = uiUtilities.getIcon(iconRes, ColorUtilities.getSecondaryIconColorId(nightMode))
		val iconSize = AndroidUtils.dpToPx(context, 16f)
		icon.setBounds(0, 0, iconSize, iconSize)
		return AndroidUtils.replaceCharsWithIcon(text, icon, arrayOf(arrow))
	}

	private companion object {
		const val RISE_ARROW = "↗"
		const val SET_ARROW = "↘"
		const val UP_ARROW = "↑"
		const val DOWN_ARROW = "↓"
	}
}
