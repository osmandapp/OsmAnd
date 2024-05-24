package net.osmand.shared.data

class QuadPointDouble {
	var x: Double
	var y: Double

	constructor() {
		x = 0.0
		y = 0.0
	}

	constructor(x: Double, y: Double) {
		this.x = x
		this.y = y
	}

	constructor(a: QuadPointDouble) {
		this.x = a.x
		this.y = a.y
	}

	fun set(x: Double, y: Double) {
		this.x = x
		this.y = y
	}
}
