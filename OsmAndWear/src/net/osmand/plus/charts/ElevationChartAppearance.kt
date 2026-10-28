package net.osmand.plus.charts

import android.content.Context
import android.graphics.drawable.Drawable

class ElevationChartAppearance (
	var context: Context? = null,
	var markerView: GpxMarkerView? = null,
	var markerIcon: Drawable? = null,
	var topOffset: Float = 24f,
	var bottomOffset: Float = 16f,
	var useGesturesAndScale: Boolean = true,
	var labelsColor: Int? = null,
	var yAxisGridColor: Int? = null,
	var xAxisGridColor: Int? = null
)