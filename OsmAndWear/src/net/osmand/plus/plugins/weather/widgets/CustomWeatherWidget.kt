package net.osmand.plus.plugins.weather.widgets

import androidx.annotation.LayoutRes
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.views.layers.MapInfoLayer
import net.osmand.plus.views.mapwidgets.WidgetType

class CustomWeatherWidget(
	mapActivity: MapActivity,
	widgetType: WidgetType,
	customId: String?,
	band: Short) : WeatherWidget(mapActivity, widgetType, customId, band) {

	@LayoutRes
	override fun getContentLayoutId(): Int {
		return R.layout.widget_custom_vertical
	}

	override fun updateColors(textState: MapInfoLayer.TextState) {
		this.textState = textState
		updateVerticalWidgetColors(textState)
	}
}