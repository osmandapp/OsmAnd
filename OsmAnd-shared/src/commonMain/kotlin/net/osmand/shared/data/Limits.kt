package net.osmand.shared.data

class Limits(val min: Number, val max: Number) {

	fun getMidpoint(): Double = (min.toDouble() + max.toDouble()) / 2.0

	override fun toString() = "Limits=[${min};${max}]"

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || this::class != other::class) return false
		other as Limits
		if (min != other.min) return false
		if (max != other.max) return false
		return true
	}

	override fun hashCode(): Int {
		var result = min.hashCode()
		result = 31 * result + max.hashCode()
		return result
	}
}