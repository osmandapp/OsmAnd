package net.osmand.plus.card.color.palette.gradient.editor.data

import net.osmand.shared.palette.domain.GradientPoint
import net.osmand.shared.palette.domain.filetype.GradientFileType

data class GradientDraft(
	val originalId: String?,
	val fileType: GradientFileType,
	val points: List<GradientPoint>,
	val noDataColor: Int?
) {
	fun withPoints(newPoints: List<GradientPoint>): GradientDraft {
		return this.copy(points = newPoints.sortedBy { it.value })
	}

	fun withPointAdded(point: GradientPoint): GradientDraft {
		val newPoints = points + point
		return this.copy(points = newPoints.sortedBy { it.value })
	}

	fun withPointUpdated(oldPoint: GradientPoint, newPoint: GradientPoint): GradientDraft {
		val newPoints = points.map { if (it == oldPoint) newPoint else it }
		return this.copy(points = newPoints.sortedBy { it.value })
	}

	fun withPointRemoved(point: GradientPoint): GradientDraft {
		val newPoints = points - point
		return this.copy(points = newPoints)
	}
}
