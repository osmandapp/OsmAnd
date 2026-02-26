package net.osmand.plus.views.controls

import net.osmand.plus.views.layers.MapInfoLayer.TextState
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings

interface WidgetsContainer {
	fun update(drawSettings: DrawSettings?)
	fun updateColors(textState: TextState)
}