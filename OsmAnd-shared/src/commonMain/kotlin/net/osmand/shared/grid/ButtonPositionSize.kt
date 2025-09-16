package net.osmand.shared.grid

import net.osmand.shared.data.KQuadRect
import net.osmand.shared.util.LoggerFactory

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ButtonPositionSize {

	val id: String

	var posH: Int = POS_LEFT
	var posV: Int = POS_TOP
	var marginX: Int = 0
	var marginY: Int = 0
	var width: Int = 7
	var height: Int = 7
	var xMove: Boolean = false
	var yMove: Boolean = false
	var moveDescendants: Int = MOVE_DESCENDANTS_ANY

	private val bounds = KQuadRect()

	constructor(id: String) : this(id, 7, true, true)

	constructor(id: String, sz8dp: Int, left: Boolean, top: Boolean) {
		this.id = id
		width = sz8dp
		height = sz8dp
		this.posH = if (left) POS_LEFT else POS_RIGHT
		this.posV = if (top) POS_TOP else POS_BOTTOM
	}

	constructor(id: String, sz8dp: Int, posH: Int, posV: Int) {
		this.id = id
		width = sz8dp
		height = sz8dp
		this.posH = posH
		this.posV = posV
		validate()
	}

	fun setMoveVertical() = apply { this.yMove = true }

	fun setMoveDescendantsAny() = apply { this.moveDescendants = MOVE_DESCENDANTS_ANY }

	fun setMoveDescendantsVertical() = apply { this.moveDescendants = MOVE_DESCENDANTS_VERTICAL }

	fun setMoveDescendantsHorizontal() = apply { this.moveDescendants = MOVE_DESCENDANTS_HORIZONTAL }

	fun setMoveHorizontal() = apply { this.xMove = true }

	fun setSize(width8dp: Int, height8dp: Int) = apply {
		this.width = width8dp
		this.height = height8dp
	}

	fun setPositionHorizontal(posH: Int) = apply {
		this.posH = posH
		validate()
	}

	fun setPositionVertical(posV: Int) = apply {
		this.posV = posV
		validate()
	}

	fun setMargin(marginX: Int, marginY: Int) = apply {
		this.marginX = marginX
		this.marginY = marginY
	}

	val isLeft: Boolean get() = posH == POS_LEFT
	val isRight: Boolean get() = posH == POS_RIGHT
	val isTop: Boolean get() = posV == POS_TOP
	val isBottom: Boolean get() = posV == POS_BOTTOM
	val isFullWidth: Boolean get() = posH == POS_FULL_WIDTH
	val isFullHeight: Boolean get() = posV == POS_FULL_HEIGHT

	fun toLongValue(): Long {
		var vl = 0L
		vl = (vl shl 2) + moveDescendants
		vl = (vl shl 2) + posV
		vl = (vl shl 1) + if (yMove) 1 else 0
		vl = (vl shl MAX_MARGIN_BITS) + min(marginY, MARGIN_MASK)
		vl = (vl shl MAX_SIZE_BITS) + min(height, SIZE_MASK)
		vl = (vl shl 2) + posH
		vl = (vl shl 1) + if (xMove) 1 else 0
		vl = (vl shl MAX_MARGIN_BITS) + min(marginX, MARGIN_MASK)
		vl = (vl shl MAX_SIZE_BITS) + min(width, SIZE_MASK)
		return vl
	}

	fun calcGridPositionFromPixel(
		dpToPix: Float, widthPx: Int, heightPx: Int,
		gravLeft: Boolean, x: Int, gravTop: Boolean, y: Int
	) {
		val calcX: Float
		if (x < widthPx / 2) {
			this.posH = if (gravLeft) POS_LEFT else POS_RIGHT
			calcX = x / dpToPix
		} else {
			this.posH = if (gravLeft) POS_RIGHT else POS_LEFT
			calcX = (widthPx - x) / dpToPix - this.width * CELL_SIZE_DP
		}
		this.marginX = max(0, ((calcX - DEF_MARGIN_DP) / CELL_SIZE_DP).roundToInt())

		val calcY: Float
		if (y < heightPx / 2) {
			this.posV = if (gravTop) POS_TOP else POS_BOTTOM
			calcY = y / dpToPix
		} else {
			this.posV = if (gravTop) POS_BOTTOM else POS_TOP
			calcY = (heightPx - y) / dpToPix - this.height * CELL_SIZE_DP
		}
		this.marginY = max(0, ((calcY - DEF_MARGIN_DP) / CELL_SIZE_DP).roundToInt())
	}

	fun getYStartPix(dpToPix: Float): Int {
		return ((marginY * CELL_SIZE_DP + DEF_MARGIN_DP) * dpToPix).toInt()
	}

	fun getYEndPix(dpToPix: Float): Int {
		return (((marginY + height) * CELL_SIZE_DP + DEF_MARGIN_DP) * dpToPix).toInt()
	}

	fun getXStartPix(dpToPix: Float): Int {
		return ((marginX * CELL_SIZE_DP + DEF_MARGIN_DP) * dpToPix).toInt()
	}

	fun getXEndPix(dpToPix: Float): Int {
		return (((marginX + width) * CELL_SIZE_DP + DEF_MARGIN_DP) * dpToPix).toInt()
	}

	fun getWidthPix(dpToPix: Float): Int {
		return ((width * CELL_SIZE_DP) * dpToPix).toInt()
	}

	fun fromLongValue(v: Long) = apply {
		var value = v
		width = (value and SIZE_MASK.toLong()).toInt()
		value = value shr MAX_SIZE_BITS
		marginX = (value and MARGIN_MASK.toLong()).toInt()
		value = value shr MAX_MARGIN_BITS
		xMove = value % 2 == 1L
		value = value shr 1
		posH = (value % 4).toInt()
		value = value shr 2
		height = (value and SIZE_MASK.toLong()).toInt()
		value = value shr MAX_SIZE_BITS
		marginY = (value and MARGIN_MASK.toLong()).toInt()
		value = value shr MAX_MARGIN_BITS
		yMove = value % 2 == 1L
		value = value shr 1
		posV = (value % 4).toInt()
		value = value shr 2
		moveDescendants = (value % 4).toInt()
		validate()
	}

	private fun validate() {
		if (posH == POS_FULL_WIDTH && posV == POS_FULL_HEIGHT) {
			LOG.error("Error parsing $this as full width + full height")
			posH = POS_RIGHT
		}
	}

	override fun toString(): String {
		val posHStr = when (posH) {
			POS_FULL_WIDTH -> "full_w"
			POS_LEFT -> "left "
			else -> "right"
		}
		val posVStr = when (posV) {
			POS_FULL_HEIGHT -> "full_h"
			POS_TOP -> "top "
			else -> "bott"
		}
		val xMoveIndicator = if (xMove) "+" else " "
		val yMoveIndicator = if (yMove) "+" else " "
		val paddedWidth = width.toString().padStart(2)
		val paddedHeight = height.toString().padStart(2)
		return "Pos ${id.padStart(10)} x=($posHStr->${marginX}$xMoveIndicator), y=($posVStr->${marginY}$yMoveIndicator), w=$paddedWidth, h=$paddedHeight"
	}

	fun overlap(position: ButtonPositionSize): Boolean {
		return KQuadRect.intersects(this.bounds, position.bounds)
	}

	private fun updateBounds(totalWidth: Int, totalHeight: Int) {
		val left: Double
		val right: Double
		val top: Double
		val bottom: Double

		if (posH == POS_FULL_WIDTH) {
			left = 0.0
			right = totalWidth.toDouble()
		} else if (posH == POS_LEFT) {
			left = marginX.toDouble()
			right = left + width
		} else {
			right = (totalWidth - marginX).toDouble()
			left = right - width
		}

		if (posV == POS_FULL_HEIGHT) {
			top = 0.0
			bottom = totalHeight.toDouble()
		} else if (posV == POS_TOP) {
			top = marginY.toDouble()
			bottom = top + height
		} else {
			bottom = (totalHeight - marginY).toDouble()
			top = bottom - height
		}
		bounds.left = left
		bounds.right = right
		bounds.top = top
		bounds.bottom = bottom
	}

	companion object {

		private val LOG = LoggerFactory.getLogger("ButtonPositionSize")

		private const val MAX_ITERATIONS = 1000
		private const val MAX_STUCK_ATTEMPTS = 20

		const val CELL_SIZE_DP = 8
		const val DEF_MARGIN_DP = 4

		const val MOVE_DESCENDANTS_ANY = 0
		const val MOVE_DESCENDANTS_VERTICAL = 1
		const val MOVE_DESCENDANTS_HORIZONTAL = 2

		const val POS_FULL_WIDTH = 0
		const val POS_LEFT = 1
		const val POS_RIGHT = 2

		const val POS_FULL_HEIGHT = 0
		const val POS_TOP = 1
		const val POS_BOTTOM = 2

		private const val MAX_MARGIN_BITS = 10
		private const val MAX_SIZE_BITS = 6
		private const val MARGIN_MASK = (1 shl MAX_MARGIN_BITS) - 1
		private const val SIZE_MASK = (1 shl MAX_SIZE_BITS) - 1

		fun computeNonOverlap(
			space: Int,
			buttons: List<ButtonPositionSize>,
			totalWidth: Int,
			totalHeight: Int
		): Boolean {
			buttons.forEach { it.updateBounds(totalWidth, totalHeight) }

			var iteration = 0
			val moveAttempts = mutableMapOf<ButtonPositionSize, Int>()
			var fixedPos = buttons.size - 1

			while (fixedPos >= 0) {
				if (iteration++ > MAX_ITERATIONS) {
					LOG.error("Relayout is broken")
					return false
				}
				var overlap = false
				val button = buttons[fixedPos]
				for (i in fixedPos + 1 until buttons.size) {
					val check = buttons[i]
					if (button.overlap(check)) {
						val attempts = moveAttempts[check] ?: 0
						if (attempts >= MAX_STUCK_ATTEMPTS) {
							LOG.warn("Skipping move for $check (max attempts reached)")
						} else if (moveButton(space, check, button, totalWidth, totalHeight)) {
							overlap = true
							check.updateBounds(totalWidth, totalHeight)
							moveAttempts[check] = attempts + 1
							fixedPos = i
							break
						}
					}
				}
				if (!overlap) {
					fixedPos--
				}
			}
			return true
		}

		private fun moveButton(
			space: Int, toMove: ButtonPositionSize,
			overlap: ButtonPositionSize, totalWidth: Int, totalHeight: Int
		): Boolean {
			var xMove = false
			var yMove = false
			when (overlap.moveDescendants) {
				MOVE_DESCENDANTS_ANY -> {
					if (overlap.isFullWidth) {
						yMove = true
					} else if (overlap.isFullHeight) {
						xMove = true
					} else {
						if (toMove.xMove) xMove = true
						if (toMove.yMove) yMove = true
					}
				}
				MOVE_DESCENDANTS_VERTICAL -> yMove = true
				MOVE_DESCENDANTS_HORIZONTAL -> xMove = true
			}

			val forceYOnly = overlap.isFullWidth || overlap.moveDescendants == MOVE_DESCENDANTS_VERTICAL
			val forceXOnly = overlap.isFullHeight || overlap.moveDescendants == MOVE_DESCENDANTS_HORIZONTAL
			val overlapHorizontalOnly = overlap.xMove && !overlap.yMove
			val overlapVerticalOnly   = overlap.yMove && !overlap.xMove
			var planX = false
			var planY = false
			when {
				forceYOnly && yMove -> planY = true
				forceXOnly && xMove -> planX = true
				overlapHorizontalOnly && yMove -> planY = true
				overlapVerticalOnly && xMove   -> planX = true
				else -> {
					if (xMove) planX = true
					if (yMove) planY = true
				}
			}

			var newX = if (planX) space + overlap.marginX + overlap.width else toMove.marginX
			var newY = if (planY) space + overlap.marginY + overlap.height else toMove.marginY
			if (newX + toMove.width > totalWidth) {
				newX = max(0, totalWidth - toMove.width)
				if (!planY) {
					val desired = space + overlap.marginY + overlap.height
					newY = max(0, min(desired, totalHeight - toMove.height))
				}
			}
			if (newY + toMove.height > totalHeight) {
				newY = max(0, totalHeight - toMove.height)
				if (!planX) {
					val desired = space + overlap.marginX + overlap.width
					newX = max(0, min(desired, totalWidth - toMove.width))
				}
			}
			val moved = (newX != toMove.marginX) || (newY != toMove.marginY)
			toMove.marginX = newX
			toMove.marginY = newY

			return moved
		}
	}
}