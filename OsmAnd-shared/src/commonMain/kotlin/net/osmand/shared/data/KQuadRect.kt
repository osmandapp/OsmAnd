package net.osmand.shared.data

class KQuadRect {
	var left: Double
	var right: Double
	var top: Double
	var bottom: Double

	// Primary constructor
	constructor(left: Double, top: Double, right: Double, bottom: Double) {
		this.left = left
		this.right = right
		this.top = top
		this.bottom = bottom
	}

	// Copy constructor
	constructor(a: KQuadRect) : this(a.left, a.top, a.right, a.bottom)

	// Default constructor
	constructor() : this(0.0, 0.0, 0.0, 0.0)

	fun invertedY(): Boolean {
		// For latitude bbox
		return top > bottom
	}

	fun expand(left: Double, top: Double, right: Double, bottom: Double) {
		if (hasInitialState()) {
			this.left = left
			this.right = right
			this.top = top
			this.bottom = bottom
		} else {
			this.left = if (left < right) kotlin.math.min(left, this.left) else kotlin.math.max(left, this.left)
			this.right = if (left < right) kotlin.math.max(right, this.right) else kotlin.math.min(right, this.right)
			this.top = if (top < bottom) kotlin.math.min(top, this.top) else kotlin.math.max(top, this.top)
			this.bottom = if (top < bottom) kotlin.math.max(bottom, this.bottom) else kotlin.math.min(bottom, this.bottom)
		}
	}

	fun width(): Double {
		return kotlin.math.abs(right - left)
	}

	fun height(): Double {
		return kotlin.math.abs(bottom - top)
	}

	fun contains(left: Double, top: Double, right: Double, bottom: Double): Boolean {
		return kotlin.math.min(this.left, this.right) <= kotlin.math.min(left, right)
				&& kotlin.math.max(this.left, this.right) >= kotlin.math.max(left, right)
				&& kotlin.math.min(this.top, this.bottom) <= kotlin.math.min(top, bottom)
				&& kotlin.math.max(this.top, this.bottom) >= kotlin.math.max(top, bottom)
	}

	fun contains(box: KQuadRect): Boolean {
		return contains(box.left, box.top, box.right, box.bottom)
	}

	companion object {
		fun intersects(a: KQuadRect, b: KQuadRect): Boolean {
			return kotlin.math.min(a.left, a.right) <= kotlin.math.max(b.left, b.right)
					&& kotlin.math.max(a.left, a.right) >= kotlin.math.min(b.left, b.right)
					&& kotlin.math.min(a.bottom, a.top) <= kotlin.math.max(b.bottom, b.top)
					&& kotlin.math.max(a.bottom, a.top) >= kotlin.math.min(b.bottom, b.top)
		}

		fun intersectionArea(a: KQuadRect, b: KQuadRect): Double {
			val xleft = kotlin.math.max(kotlin.math.min(a.left, a.right), kotlin.math.min(b.left, b.right))
			val xright = kotlin.math.min(kotlin.math.max(a.left, a.right), kotlin.math.max(b.left, b.right))
			val ytop = kotlin.math.max(kotlin.math.min(a.top, a.bottom), kotlin.math.min(b.top, b.bottom))
			val ybottom = kotlin.math.min(kotlin.math.max(a.top, a.bottom), kotlin.math.max(b.top, b.bottom))
			return if (xright <= xleft || ybottom <= ytop) {
				0.0
			} else {
				(xright - xleft) * (ybottom - ytop)
			}
		}

		fun trivialOverlap(a: KQuadRect, b: KQuadRect): Boolean {
			return intersects(a, b)
		}
	}

	fun centerX(): Double {
		return (left + right) / 2
	}

	fun centerY(): Double {
		return (top + bottom) / 2
	}

	fun offset(dx: Double, dy: Double) {
		left += dx
		top += dy
		right += dx
		bottom += dy
	}

	fun inset(dx: Double, dy: Double) {
		left += dx
		top += dy
		right -= dx
		bottom -= dy
	}

	fun hasInitialState(): Boolean {
		return left == 0.0 && right == 0.0 && top == 0.0 && bottom == 0.0
	}

	override fun toString(): String {
		return "[${left.toFloat()},${top.toFloat()} - ${right.toFloat()},${bottom.toFloat()}]"
	}
}
