package net.osmand.shared.data

/** Mutable float point, port of `net.osmand.data.QuadPoint`. */
class KQuadPoint {

	var x: Float
	var y: Float

	constructor() : this(0f, 0f)

	constructor(x: Float, y: Float) {
		this.x = x
		this.y = y
	}

	constructor(a: KQuadPoint) : this(a.x, a.y)

	fun set(x: Float, y: Float) {
		this.x = x
		this.y = y
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is KQuadPoint) return false
		return x == other.x && y == other.y
	}

	override fun hashCode(): Int = 31 * x.hashCode() + y.hashCode()

	override fun toString(): String = "x $x y $y"
}
