package net.osmand.shared.gpx.organization

import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.SmartFolder
import net.osmand.shared.gpx.filters.FilterType
import net.osmand.shared.gpx.organization.enums.OrganizeByType
import net.osmand.shared.gpx.organization.strategy.OrganizeByRangeStrategy
import net.osmand.shared.gpx.organization.strategy.OrganizeByStrategy
import kotlin.reflect.KClass

class TracksOrganizer {

	private var strategy: OrganizeByStrategy<*>? = null
	private var cachedRules: OrganizeByRules? = null

	fun execute(
		originalGroup: SmartFolder,
		rules: OrganizeByRules,
		resourcesMapper: OrganizeByResourcesMapper
	): List<OrganizedTracksGroup>? {
		val oldRules = cachedRules
		if (strategy == null || oldRules == null || oldRules.type != rules.type) {
			// Update strategy if organization type changed
			strategy = createStrategy(rules)
			cachedRules = rules
		}
		return strategy?.apply(originalGroup, rules, resourcesMapper)
	}

	private fun createStrategy(rules: OrganizeByRules): OrganizeByStrategy<*>? {
		val filterType = rules.type.filterType.filterType

		return when(filterType) {
			FilterType.RANGE -> createRangeStrategy(rules.type)
			else -> null
		}
	}

	private fun createRangeStrategy(type: OrganizeByType): OrganizeByStrategy<*> {
		// TODO throw an exception if the type is not range represented
		val parameter = type.filterType.property
		if (isGpxParameterClass(parameter, Double::class)) {
			return OrganizeByRangeStrategy<Double>()
		} else if (isGpxParameterClass(parameter, Float::class)) {
			return OrganizeByRangeStrategy<Float>()
		} else if (isGpxParameterClass(parameter, Int::class)) {
			return OrganizeByRangeStrategy<Int>()
		} else if (isGpxParameterClass(parameter, Long::class)) {
			return OrganizeByRangeStrategy<Long>()
		}
		throw IllegalArgumentException("Unsupported gpxParameter type class " + parameter?.typeClass)
	}

	private fun isGpxParameterClass(parameter: GpxParameter?, javaClass: KClass<*>): Boolean {
		return parameter?.typeClass == javaClass
	}
}