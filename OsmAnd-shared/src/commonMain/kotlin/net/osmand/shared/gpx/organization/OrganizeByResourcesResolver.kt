package net.osmand.shared.gpx.organization

import net.osmand.shared.gpx.organization.enums.OrganizeByType

interface OrganizeByResourcesResolver {

	fun resolveName(value: Any, type: OrganizeByType): String

	fun resolveIconName(value: Any, type: OrganizeByType) = type.iconResId
}