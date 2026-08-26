package net.osmand.plus.plugins.astronomy

import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.gallery.controller.GalleryGridController
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.ui.GalleryGridFragment

class AstronomyGridController(
	app: OsmandApplication,
	override val key: GalleryKey.Astronomy,
	private val title: String? = null
) : GalleryGridController(app, key) {

	override fun getProcessId(): String = processId(key)

	override fun getScreenTitle(): String? =
		title ?: view?.getMapActivity()?.contextMenu?.titleStr

	companion object {
		private const val PROCESS_ID = "gallery_grid_astronomy"

		fun processId(key: GalleryKey.Astronomy): String =
			"${PROCESS_ID}_${key.wikidataId}"

		fun show(activity: FragmentActivity, key: GalleryKey.Astronomy, title: String? = null) {
			val app = activity.application as OsmandApplication
			val controller = AstronomyGridController(app, key, title)
			app.dialogManager.register(controller.processId, controller)
			GalleryGridFragment.showInstance(activity, controller.processId)
		}
	}
}