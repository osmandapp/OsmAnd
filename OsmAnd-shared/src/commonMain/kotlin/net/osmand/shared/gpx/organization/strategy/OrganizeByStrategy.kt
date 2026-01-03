package net.osmand.shared.gpx.organization.strategy

import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.SmartFolder
import net.osmand.shared.gpx.organization.OrganizeTracksResourceMapper
import net.osmand.shared.gpx.organization.OrganizeByRules
import net.osmand.shared.gpx.organization.enums.OrganizeByType

interface OrganizeByStrategy<T> {

	fun apply(
		originalGroup: SmartFolder,
		rules: OrganizeByRules,
		resourcesMapper: OrganizeTracksResourceMapper
	): List<OrganizedTracksGroup>?

	fun createId(value: T, originalGroup: SmartFolder, type: OrganizeByType): String {
		val parentId = originalGroup.getId()
		val typeName = type.name.lowercase()
		val valueId = createRepresentedValueId(value)
		return "${parentId}__organized_by_${typeName}__${valueId}"
	}

	fun createRepresentedValueId(value: T): String
}