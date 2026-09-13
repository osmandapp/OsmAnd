package net.osmand.plus.plugins.audionotes.library

import androidx.fragment.app.FragmentActivity
import net.osmand.data.PointDescription
import net.osmand.data.LatLon
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.myplaces.MyPlacesActivity
import net.osmand.plus.plugins.audionotes.library.data.MediaLibraryEntry

object ShowOnMapNavigator {
	fun show(activity: FragmentActivity, entry: MediaLibraryEntry) {
		val lat = entry.lat ?: return
		val lon = entry.lon ?: return
		val target = entry.recording ?: entry.attachments.lastOrNull()?.target
		val description = PointDescription(PointDescription.POINT_TYPE_LOCATION, entry.title)
		when (activity) {
			is MyPlacesActivity -> activity.showOnMap(null, lat, lon, 15, description, true, target)
			is MapActivity -> {
				activity.supportFragmentManager.popBackStack()
				activity.contextMenu.show(LatLon(lat, lon), description, target)
			}
		}
	}
}
