package net.osmand.plus.plugins.astro

data class Constellation(
	val name: String,
	val wid: String, // Wikipedia ID
	val lines: List<Pair<Int, Int>>, // Pairs of SkyObject HIP IDs
	var localizedName: String? = null
)