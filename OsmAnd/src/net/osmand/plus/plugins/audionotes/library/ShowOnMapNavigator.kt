package net.osmand.plus.plugins.audionotes.library

import androidx.fragment.app.FragmentActivity
import net.osmand.data.FavouritePoint
import net.osmand.data.PointDescription
import net.osmand.data.LatLon
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.gallery.ui.GalleryPhotoPagerFragment
import net.osmand.plus.myplaces.MyPlacesActivity
import net.osmand.plus.plugins.audionotes.library.data.MediaAttachment
import net.osmand.plus.plugins.audionotes.library.data.MediaLibraryEntry
import net.osmand.shared.gpx.primitives.WptPt

object ShowOnMapNavigator {
	fun show(activity: FragmentActivity, entry: MediaLibraryEntry) {
		val lat = entry.lat ?: return
		val lon = entry.lon ?: return
		val target = entry.recording ?: entry.attachments.lastOrNull()?.target
		show(activity, lat, lon, PointDescription(PointDescription.POINT_TYPE_LOCATION, entry.title), target)
	}

	fun show(activity: FragmentActivity, attachment: MediaAttachment) {
		val target = attachment.target
		val description = when (target) {
			is FavouritePoint -> PointDescription(PointDescription.POINT_TYPE_FAVORITE, target.getDisplayName(activity))
			is WptPt -> PointDescription(PointDescription.POINT_TYPE_WPT, target.name ?: attachment.name)
			else -> PointDescription(PointDescription.POINT_TYPE_LOCATION, attachment.name)
		}
		show(activity, attachment.latLon.latitude, attachment.latLon.longitude, description, target)
	}

	fun show(activity: FragmentActivity, lat: Double, lon: Double, description: PointDescription, target: Any?) {
		when (activity) {
			is MyPlacesActivity -> activity.showOnMap(null, lat, lon, 15, description, true, target)
			is MapActivity -> {
				(activity.supportFragmentManager.findFragmentByTag(GalleryPhotoPagerFragment.TAG) as? GalleryPhotoPagerFragment)?.dismiss()
				activity.contextMenu.show(LatLon(lat, lon), description, target)
			}
		}
	}
}
