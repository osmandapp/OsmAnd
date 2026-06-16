package net.osmand.plus.gallery.online

import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.gallery.controller.GalleryGridController
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.ui.GalleryGridFragment

class OnlinePhotosGridController(
	app: OsmandApplication,
	override val key: GalleryKey.Location
) : GalleryGridController(app, key) {

	override fun getProcessId(): String = processId(key)

	override fun getScreenTitle() = view?.getMapActivity()?.contextMenu?.titleStr

	companion object {
		private const val PROCESS_ID = "gallery_grid_online"

		fun processId(key: GalleryKey.Location): String =
			"${PROCESS_ID}_${key.latLon.latitude}_${key.latLon.longitude}"

		fun show(activity: FragmentActivity, key: GalleryKey.Location) {
			val app = activity.application as OsmandApplication
			val controller = OnlinePhotosGridController(app, key)
			app.dialogManager.register(controller.processId, controller)
			GalleryGridFragment.showInstance(activity, controller.processId)
		}
	}
}