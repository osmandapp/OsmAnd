package net.osmand.plus.card.color.palette.gradient.editor

import androidx.fragment.app.FragmentManager
import net.osmand.OnResultCallback
import net.osmand.plus.OsmandApplication
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.settings.backend.ApplicationMode

class GradientEditorController(
	app: OsmandApplication,
	appMode: ApplicationMode,
	gradientDraft: GradientDraft,
	val callback: OnResultCallback<GradientDraft>
) : BaseDialogController(app) {

	companion object {

		private const val PROCESS_ID = "edit_gradient"

		fun showDialog(
			app: OsmandApplication,
			fragmentManager: FragmentManager,
			appMode: ApplicationMode,
			gradientDraft: GradientDraft,
			callback: OnResultCallback<GradientDraft>
		) {
			val controller = GradientEditorController(app, appMode, gradientDraft, callback)
			app.dialogManager.register(PROCESS_ID, controller)
			if (!GradientEditorFragment.showInstance(fragmentManager, appMode)) {
				app.dialogManager.unregister(PROCESS_ID)
			}
		}

		fun getExistedInstance(app: OsmandApplication): GradientEditorController? {
			return app.dialogManager.findController(PROCESS_ID) as? GradientEditorController
		}
	}

	override fun getProcessId() = PROCESS_ID
}