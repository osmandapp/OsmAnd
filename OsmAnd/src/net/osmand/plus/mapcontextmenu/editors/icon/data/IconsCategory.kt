package net.osmand.plus.mapcontextmenu.editors.icon.data

class IconsCategory @JvmOverloads constructor(
	val key: String,
	val translation: String,
	val iconKeys: MutableList<String>,
	val isTopCategory: Boolean = false
) {
	fun containsIcon(iconKey: String): Boolean = iconKey in iconKeys

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		return other is IconsCategory && key == other.key
	}

	override fun hashCode(): Int = key.hashCode()

	override fun toString(): String = key
}
