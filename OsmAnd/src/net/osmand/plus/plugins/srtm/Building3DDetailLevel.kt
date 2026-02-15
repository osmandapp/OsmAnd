package net.osmand.plus.plugins.srtm

import net.osmand.plus.R

enum class Building3DDetailLevel (val labelResId: Int, val value: Boolean) {
	LOW(R.string.shared_string_off, false),
	HIGH(R.string.rendering_value_high_name, true);

	companion object {
		fun fromValue(value: Boolean): Building3DDetailLevel {
			return if(value) HIGH else LOW
		}
	}
}