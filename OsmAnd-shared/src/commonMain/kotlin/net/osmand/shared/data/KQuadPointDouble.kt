package net.osmand.shared.data

/** Mutable double point, port of `net.osmand.data.QuadPointDouble`. */
class KQuadPointDouble {

	var x: Double
	var y: Double

	constructor() : this(0.0, 0.0)

	constructor(x: Double, y: Double) {
		this.x = x
		this.y = y
	}

	constructor(a: KQuadPointDouble) : this(a.x, a.y)

	fun set(x: Double, y: Double) {
		this.x = x
		this.y = y
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is KQuadPointDouble) return false
		return x == other.x && y == other.y
	}

	override fun hashCode(): Int = 31 * x.hashCode() + y.hashCode()

	override fun toString(): String = "x $x y $y"
}
