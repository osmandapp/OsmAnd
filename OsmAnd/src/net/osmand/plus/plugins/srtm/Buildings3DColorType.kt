package net.osmand.plus.plugins.srtm

import net.osmand.plus.R

enum class Buildings3DColorType(val labelId: Int, val id: Int) {
	MAP_STYLE(R.string.quick_action_map_style, 1),
	CUSTOM(R.string.shared_string_custom, 2);

	companion object {
		fun getById(id: Int): Buildings3DColorType {
			return when (id) {
				1 -> MAP_STYLE
				2 -> CUSTOM
				else -> throw IllegalArgumentException("Unknown Buildings3DColorType id=$id")
			}
		}
	}
}