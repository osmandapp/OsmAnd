package net.osmand.plus.views.controls

import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings
import net.osmand.plus.views.mapwidgets.appearance.PanelAppearanceConsumer

interface WidgetsContainer : PanelAppearanceConsumer {
	fun update(drawSettings: DrawSettings?)
}