package net.osmand.plus.plugins.astronomy.search

import android.content.Context
import io.github.cosinekitty.astronomy.Observer
import net.osmand.plus.plugins.astronomy.AstroDataProvider
import net.osmand.plus.plugins.astronomy.StarMapFragment
import net.osmand.plus.plugins.astronomy.utils.AstroUtils
import net.osmand.plus.utils.ColorUtilities
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar

internal data class StarMapSearchComputationContext(
	val observer: Observer,
	val now: ZonedDateTime,
	val dusk: ZonedDateTime,
	val dawn: ZonedDateTime
)

internal data class StarMapSearchPreparedData(
	val entries: List<StarMapSearchEntry>,
	val catalogEntries: List<StarMapCatalogEntry>,
	val widToDisplayName: Map<String, String>,
	val computationContext: StarMapSearchComputationContext
)

internal class StarMapSearchPreparedDataFactory(
	private val dataProvider: AstroDataProvider,
	private val nightMode: Boolean
) {

	fun create(context: Context, parent: StarMapFragment?): StarMapSearchPreparedData {
		val objects = parent?.getSearchableObjects().orEmpty()
		val observer = parent?.starView?.observer ?: Observer(0.0, 0.0, 0.0)
		val currentCalendar = parent?.viewModel?.currentCalendar?.value ?: Calendar.getInstance()
		val computationContext = createComputationContext(observer, currentCalendar)
		val widToDisplayName = linkedMapOf<String, String>()
		val primaryIconColor = ColorUtilities.getPrimaryIconColor(context, nightMode)

		val entries = objects.map { obj ->
			if (obj.wid.isNotEmpty()) {
				widToDisplayName[obj.wid] = obj.niceName()
			}
			StarMapSearchEntry(
				objectRef = obj,
				displayName = obj.niceName(),
				magnitude = obj.magnitude,
				category = mapStarMapSearchCategory(obj),
				iconRes = AstroUtils.getObjectTypeIcon(obj.type),
				iconColor = if (obj.type.isSunSystem()) obj.color else primaryIconColor,
				catalogWids = obj.catalogs.mapTo(linkedSetOf()) { it.wid }
			)
		}

		return StarMapSearchPreparedData(
			entries = entries,
			catalogEntries = buildCatalogEntries(context, entries),
			widToDisplayName = widToDisplayName,
			computationContext = computationContext
		)
	}

	private fun createComputationContext(observer: Observer, calendar: Calendar): StarMapSearchComputationContext {
		val zoneId = ZoneId.systemDefault()
		val now = calendar.toInstant().atZone(zoneId)
		val dayStart = now.toLocalDate().atStartOfDay(zoneId)
		val dayEnd = dayStart.plusDays(1)
		val twilight = AstroUtils.computeTwilight(dayStart, dayEnd, observer, zoneId)
		val dusk = twilight.civilDusk ?: dayStart.withHour(18)
		val dawnRaw = twilight.civilDawn
		val dawn = when {
			dawnRaw == null -> dusk.plusHours(12)
			dawnRaw.isAfter(dusk) -> dawnRaw
			else -> dawnRaw.plusDays(1)
		}
		return StarMapSearchComputationContext(observer, now, dusk, dawn)
	}

	private fun buildCatalogEntries(
		context: Context,
		preparedEntries: List<StarMapSearchEntry>
	): List<StarMapCatalogEntry> {
		val objectCountByCatalogWid = preparedEntries
			.flatMap { it.catalogWids }
			.groupingBy { it }
			.eachCount()
		return dataProvider.getCatalogs(context).map { catalog ->
			StarMapCatalogEntry(
				catalog = catalog,
				displayName = catalog.name,
				description = dataProvider.getAstroArticle(context, catalog.wid)?.description,
				objectCount = objectCountByCatalogWid[catalog.wid] ?: 0
			)
		}
	}
}
