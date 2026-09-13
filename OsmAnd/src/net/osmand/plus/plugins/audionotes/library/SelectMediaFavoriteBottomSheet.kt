package net.osmand.plus.plugins.audionotes.library

import android.os.Bundle
import net.osmand.data.FavouritePoint
import net.osmand.plus.gallery.attached.helpers.AttachedMediaDataHelper
import net.osmand.plus.mapcontextmenu.other.SelectFavouriteBottomSheet
import net.osmand.shared.gpx.primitives.Link

class SelectMediaFavoriteBottomSheet : SelectFavouriteBottomSheet() {
	 override fun onFavouriteSelected(favourite: FavouritePoint) {
		val href = arguments?.getString("href") ?: return
		if (favourite.links.orEmpty().none { it.href == href }) {
			AttachedMediaDataHelper(app).addMediaLinks(favourite,
				listOf(Link(href).apply { text = arguments?.getString("title") }), null)
		}
		app.galleryHelper.mediaLibraryRepository.refresh()
		dismiss()
	}

	companion object {
		fun create(href: String, title: String) = SelectMediaFavoriteBottomSheet().apply {
			arguments = Bundle().apply { putString("href", href); putString("title", title) }
		}
	}
}
