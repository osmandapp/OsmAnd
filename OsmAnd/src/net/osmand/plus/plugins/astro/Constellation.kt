package net.osmand.plus.plugins.astro

import android.graphics.Color

data class Constellation(
	override val name: String,
	override val wid: String,
	val lines: List<Pair<Int, Int>>,
	override var localizedName: String? = null
) : SkyObject(
	id = "const_${name.lowercase().replace(' ', '_')}",
	hip = -1,
	wid = wid,
	type = Type.CONSTELLATION,
	body = null,
	name = name,
	ra = 0.0,
	dec = 0.0,
	magnitude = 100f,
	color = Color.WHITE,
	localizedName = localizedName
)
