package net.osmand.shared.vehicle.specification.domain

import kotlin.jvm.JvmStatic

enum class SpecificationType(val key: String) {

	WIDTH("width"),
	HEIGHT("height"),
	LENGTH("length"),
	WEIGHT("weight"),
	AXLE_LOAD("maxaxleload"),
	WEIGHT_FULL_LOAD("weightrating");

	companion object {

		@JvmStatic
		fun getByKey(key: String): SpecificationType? {
			return entries.find { it.key == key }
		}
	}

	fun isWeightRelated(): Boolean {
		return when (this) {
			WEIGHT, AXLE_LOAD, WEIGHT_FULL_LOAD -> true
			else -> false
		}
	}
}