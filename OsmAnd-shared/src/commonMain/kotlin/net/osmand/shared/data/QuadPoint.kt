package net.osmand.shared.data

class QuadPoint {
	var x: Float = 0.0f
	var y: Float = 0.0f

	constructor()

	constructor(x: Float, y: Float) {
		this.x = x
		this.y = y
	}

	constructor(a: QuadPoint) : this(a.x, a.y)

	fun set(x: Float, y: Float) {
		this.x = x
		this.y = y
	}

	override fun toString(): String {
		return "x $x y $y"
	}
}
