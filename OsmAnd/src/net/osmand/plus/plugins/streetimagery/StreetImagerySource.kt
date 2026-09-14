package net.osmand.plus.plugins.streetimagery

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

import net.osmand.plus.R
import net.osmand.plus.views.mapwidgets.WidgetType

/**
 * Presentation metadata of a street-level imagery provider. Immutable configuration only:
 * plugins, preferences and widget instances stay with the provider that owns them.
 */
enum class StreetImagerySource(
	val widgetType: WidgetType,
	@get:DrawableRes val iconId: Int,
	@get:ColorRes val colorId: Int,
	@get:StringRes val titleId: Int,
	@get:StringRes val descriptionId: Int,
	@get:StringRes val widgetTitleId: Int,
	@get:StringRes val widgetDescriptionId: Int
) {
	MAPILLARY(
		WidgetType.MAPILLARY, R.drawable.ic_action_mapillary, R.color.mapillary_color,
		R.string.mapillary, R.string.mapillary_descr,
		R.string.mapillary_widget, R.string.mapillary_widget_descr
	),

	// TODO: Replace with a dedicated Panoramax icon when artwork is available.
	PANORAMAX(
		WidgetType.PANORAMAX, R.drawable.ic_action_photo_street, R.color.panoramax_color,
		R.string.panoramax, R.string.panoramax_descr,
		R.string.panoramax_widget, R.string.panoramax_widget_descr
	)
}
