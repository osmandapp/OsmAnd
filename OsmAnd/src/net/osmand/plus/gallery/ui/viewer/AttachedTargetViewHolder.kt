package net.osmand.plus.gallery.ui.viewer

import android.text.SpannableStringBuilder
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import net.osmand.data.FavouritePoint
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.favorites.dialogs.FavoriteViewHolder
import net.osmand.plus.plugins.audionotes.library.data.MediaAttachment
import net.osmand.plus.track.GpxAppearanceAdapter
import net.osmand.plus.track.fragments.TrackAppearanceFragment
import net.osmand.plus.track.helpers.GpxAppearanceHelper
import net.osmand.plus.track.helpers.GpxUiHelper
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.UpdateLocationUtils
import net.osmand.plus.views.PointImageUtils
import net.osmand.shared.gpx.GpxDataItem
import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.io.KFile
import java.util.Date

class AttachedTargetViewHolder(view: View) : DetailsHolder(view) {
	private val app = view.context.applicationContext as OsmandApplication
	private val icon: ImageView = view.findViewById(R.id.icon)
	private val title: TextView = view.findViewById(R.id.title)
	private val description: TextView = view.findViewById(R.id.description)
	private val suffix: TextView = view.findViewById(R.id.suffix_description)
	private val directionIcon: ImageView = view.findViewById(R.id.direction_icon)
	private val menuButton: View = view.findViewById(R.id.menu_button)
	private val divider: View = view.findViewById(R.id.divider)
	private val contentContainer: View = view.findViewById(R.id.content_container)
	private val row: View = view.findViewById(R.id.button_container)
	private val locationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(view.context)
	private var bound: MediaAttachment? = null

	init {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.checkbox_container), false)
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.prefix_description), false)
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.full_divider), false)
		title.maxLines = 2
		description.maxLines = 1
	}

	fun bind(attachment: MediaAttachment, showDivider: Boolean, actions: MediaDetailsAdapter.Actions) {
		bound = attachment
		when (attachment.kind) {
			MediaAttachment.Kind.FAVORITE -> bindFavorite(attachment)
			MediaAttachment.Kind.TRACK_POINT -> bindTrack(attachment)
		}
		row.setOnClickListener { actions.onAttachmentMenu(menuButton, attachment) }
		menuButton.setOnClickListener { actions.onAttachmentMenu(menuButton, attachment) }
		AndroidUiHelper.updateVisibility(divider, showDivider)
	}

	private fun bindFavorite(attachment: MediaAttachment) {
		val point = attachment.target as? FavouritePoint
		setIconSize(app.resources.getDimensionPixelSize(R.dimen.favorites_my_places_icon_size), AndroidUtils.dpToPx(app, FAVORITE_CONTENT_MARGIN_DP))
		val color = point?.let { app.favoritesHelper.getColorWithCategory(it, ColorUtilities.getColor(app, R.color.color_favorite)) }
			?: ColorUtilities.getColor(app, R.color.color_favorite)
		icon.setImageDrawable(PointImageUtils.getFromPoint(app, color, false, point))
		title.text = point?.getDisplayName(app) ?: attachment.name
		val info = UpdateLocationUtils.UpdateLocationInfo(app, null, attachment.latLon)
		var text: CharSequence = UpdateLocationUtils.getFormattedDistance(app, info, locationViewCache)
		val address = FavoriteViewHolder.prepareAddress(point?.address)
		if (!address.isNullOrEmpty()) {
			text = app.getString(R.string.ltr_or_rtl_combine_via_bold_point, text, address)
		}
		description.text = text
		UpdateLocationUtils.updateDirectionDrawable(app, directionIcon, info, locationViewCache)
		AndroidUiHelper.updateVisibility(directionIcon, true)
		val time = point?.timestamp ?: 0L
		if (time > 0) {
			suffix.text = " | " + OsmAndFormatter.getDateFormat(app).format(Date(time))
		}
		AndroidUiHelper.updateVisibility(suffix, time > 0)
	}

	private fun bindTrack(attachment: MediaAttachment) {
		setIconSize(app.resources.getDimensionPixelSize(R.dimen.standard_icon_size), app.resources.getDimensionPixelSize(R.dimen.content_padding))
		title.text = attachment.name
		AndroidUiHelper.updateVisibility(directionIcon, false)
		AndroidUiHelper.updateVisibility(suffix, false)
		val file = attachment.trackFile?.let { KFile(it) }
		val item = file?.let { app.gpxDbHelper.getItem(it) { loaded -> if (bound === attachment) bindTrackInfo(loaded) } }
		bindTrackInfo(item)
	}

	private fun bindTrackInfo(item: GpxDataItem?) {
		val helper = GpxAppearanceHelper(app)
		val color = item?.let { helper.getParameter<Int?>(it, GpxParameter.COLOR) } ?: GpxAppearanceAdapter.getTrackColor(app)
		val width = item?.let { helper.getParameter<String?>(it, GpxParameter.WIDTH) }
		val arrows = item?.let { helper.requireParameter<Boolean>(it, GpxParameter.SHOW_ARROWS) } ?: false
		icon.setImageDrawable(TrackAppearanceFragment.getTrackIcon(app, width, arrows, color))
		val analysis = item?.getAnalysis()
		description.text = if (analysis != null) {
			SpannableStringBuilder().also { GpxUiHelper.appendTrackShortDescription(app, it, analysis, null, false) }
		} else {
			""
		}
	}

	private fun setIconSize(size: Int, contentMargin: Int) {
		icon.layoutParams = (icon.layoutParams as LinearLayout.LayoutParams).apply {
			width = size
			height = size
		}
		contentContainer.layoutParams = (contentContainer.layoutParams as LinearLayout.LayoutParams).apply { marginStart = contentMargin }
	}

	companion object {
		private const val FAVORITE_CONTENT_MARGIN_DP = 10f
	}
}
