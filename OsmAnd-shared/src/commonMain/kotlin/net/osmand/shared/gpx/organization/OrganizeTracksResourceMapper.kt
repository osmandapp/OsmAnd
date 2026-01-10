package net.osmand.shared.gpx.organization

import net.osmand.shared.gpx.organization.enums.OrganizeByType

abstract class OrganizeTracksResourceMapper {

	private val nameCache = mutableMapOf<String, String>()
	private val iconNameCache = mutableMapOf<String, String>()

	fun getName(type: OrganizeByType, value: Any): String {
		val key = buildNameKey(type, value)
		return nameCache.getOrPut(key) { resolveName(type, value) }
	}

	fun getIconName(type: OrganizeByType, value: Any): String {
		val key = buildIconKey(type, value)
		return iconNameCache.getOrPut(key) { resolveIconName(type, value) }
	}

	fun clearCache() {
		nameCache.clear()
		iconNameCache.clear()
	}

	protected abstract fun resolveName(type: OrganizeByType, value: Any): String

	protected open fun resolveIconName(type: OrganizeByType, value: Any) = type.iconResId

	private fun buildNameKey(type: OrganizeByType, value: Any) = buildFullKey(type, value)

	private fun buildIconKey(type: OrganizeByType, value: Any): String {
		if (type == OrganizeByType.ACTIVITY) {
			return buildFullKey(type, value)
		}
		return type.name
	}

	private fun buildFullKey(type: OrganizeByType, value: Any) = "${type.name}__${value}"
}