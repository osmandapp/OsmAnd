package net.osmand.shared.palette.data

import net.osmand.shared.ColorPalette
import net.osmand.shared.palette.domain.GradientPoint

fun ColorPalette.toGradientPoints(): List<GradientPoint> {
	return this.colors.sortedBy { it.value }.map {
		GradientPoint(
			value = it.value.toFloat(),
			color = it.clr
		)
	}
}

fun List<GradientPoint>.toColorPalette(): ColorPalette {
	val palette = ColorPalette()
	this.forEach { point ->
		val r = (point.color shr 16) and 0xFF
		val g = (point.color shr 8) and 0xFF
		val b = point.color and 0xFF
		val a = (point.color shr 24) and 0xFF

		palette.colors.add(
			ColorPalette.ColorValue(point.value.toDouble(), r, g, b, a)
		)
	}
	return palette
}