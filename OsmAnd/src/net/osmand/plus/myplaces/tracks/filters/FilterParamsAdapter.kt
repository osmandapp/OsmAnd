package net.osmand.plus.myplaces.tracks.filters

import android.graphics.drawable.Drawable
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.card.width.WidthMode
import net.osmand.plus.track.fragments.TrackAppearanceFragment
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.gpx.filters.ActivitySingleFieldTrackFilterParams
import net.osmand.shared.gpx.filters.ColorSingleFieldTrackFilterParams
import net.osmand.shared.gpx.filters.FolderSingleFieldTrackFilterParams
import net.osmand.shared.gpx.filters.SingleFieldTrackFilterParams
import net.osmand.shared.gpx.filters.WidthSingleFieldTrackFilterParams
import net.osmand.util.Algorithms

class FilterParamsAdapter(
	private val app: OsmandApplication,
	private val filterParams: SingleFieldTrackFilterParams
) {

	fun getFilterItemIcon(
		itemName: String,
		selected: Boolean,
		nightMode: Boolean
	): Drawable? {
		when (filterParams) {
			is ColorSingleFieldTrackFilterParams -> {
				return if (Algorithms.isEmpty(itemName)) {
					app.uiUtilities.getThemedIcon(R.drawable.ic_action_appearance_disabled)
				} else {
					var color = 0
					try {
						color = if (itemName[0] != '#') Integer.parseInt(itemName) else Algorithms.parseColor(itemName)
					} catch (_: Exception) {
					}
					val colorWithoutAlpha = ColorUtilities.removeAlpha(color)
					val transparencyColor =
						ColorUtilities.getColorWithAlpha(colorWithoutAlpha, 0.8f)
					val transparencyIcon =
						app.uiUtilities.getPaintedIcon(
							R.drawable.ic_bg_transparency,
							transparencyColor
						)
					val colorIcon =
						app.uiUtilities.getPaintedIcon(R.drawable.bg_point_circle, color)
					UiUtilities.getLayeredIcon(transparencyIcon, colorIcon)
				}
			}

			is FolderSingleFieldTrackFilterParams -> {
				return app.uiUtilities.getPaintedIcon(
					R.drawable.ic_action_folder,
					app.getColor(R.color.icon_color_default_light)
				)
			}

			is WidthSingleFieldTrackFilterParams -> {
				return if (Algorithms.isEmpty(itemName)) {
					app.uiUtilities.getThemedIcon(R.drawable.ic_action_appearance_disabled)
				} else {
					val iconColor = when (itemName) {
						WidthMode.THIN.key,
						WidthMode.MEDIUM.key,
						WidthMode.BOLD.key -> R.color.track_filter_width_standard

						else -> {
							R.color.track_filter_width_custom
						}
					}
					TrackAppearanceFragment.getTrackIcon(
						app,
						itemName,
						false,
						app.getColor(iconColor)
					)
				}
			}

			is ActivitySingleFieldTrackFilterParams -> {
				val routeActivity = app.routeActivityHelper.findRouteActivity(itemName)
				val iconColor = if (selected) {
					ColorUtilities.getActiveColor(app, nightMode)
				} else {
					ColorUtilities.getDefaultIconColor(app, nightMode)
				}
				val iconId = if (routeActivity != null) {
					AndroidUtils.getIconId(app, routeActivity.iconName)
				} else {
					R.drawable.ic_action_activity
				}
				return app.uiUtilities.getPaintedIcon(iconId, iconColor)
			}

			else -> {
				return null
			}
		}
	}

	fun getItemText(
		itemName: String
	): String {
		when (filterParams) {
			is ColorSingleFieldTrackFilterParams -> {
				return filterParams.getItemText(itemName)
			}

			is FolderSingleFieldTrackFilterParams -> {
				return filterParams.getItemText(itemName)
			}

			is WidthSingleFieldTrackFilterParams -> {
				return if (Algorithms.isEmpty(itemName)) {
					app.getString(R.string.not_specified)
				} else {
					when (itemName) {
						WidthMode.THIN.key -> app.getString(R.string.rendering_value_thin_name)
						WidthMode.MEDIUM.key -> app.getString(R.string.rendering_value_medium_name)
						WidthMode.BOLD.key -> app.getString(R.string.rendering_value_bold_name)
						else -> {
							"${app.getString(R.string.shared_string_custom)}: $itemName"
						}
					}
				}
			}
			is ActivitySingleFieldTrackFilterParams -> {
				val routeActivity = app.routeActivityHelper.findRouteActivity(itemName)
				return routeActivity?.label ?: app.getString(R.string.shared_string_none)
			}

			else -> {
				return itemName
			}
		}
	}
}