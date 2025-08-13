package net.osmand.shared.data

import kotlinx.serialization.Serializable
import net.osmand.shared.util.KMapUtils
import kotlin.math.floor

@Serializable
data class KLatLon(val latitude: Double, val longitude: Double) {

	override fun hashCode(): Int {
		val prime = 31
		var result = 1
		var temp: Int = floor(latitude * 10000).toInt()
		result = prime * result + temp
		temp = floor(longitude * 10000).toInt()
		result = prime * result + temp
		return result
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || this::class != other::class) return false
		other as KLatLon
		return KMapUtils.areLatLonEqual(this, other)
	}

	override fun toString(): String {
		return "Lat ${latitude.toFloat()} Lon ${longitude.toFloat()}"
	}
}
