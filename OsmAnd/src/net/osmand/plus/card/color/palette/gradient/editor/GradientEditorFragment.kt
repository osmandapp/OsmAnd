package net.osmand.plus.card.color.palette.gradient.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.base.BaseFullScreenDialogFragment
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.utils.AndroidUtils

class GradientEditorFragment : BaseFullScreenDialogFragment(), IDialog {

	companion object {

		private val TAG = GradientEditorFragment::class.java.simpleName

		fun showInstance(manager: FragmentManager, appMode: ApplicationMode): Boolean {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = GradientEditorFragment()
				val arguments = Bundle()
				arguments.putString(APP_MODE_KEY, appMode.stringKey)
				fragment.arguments = arguments
				fragment.show(manager, TAG)
				return true
			}
			return false
		}
	}

	private var controller: GradientEditorController? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		controller = GradientEditorController.getExistedInstance(app)
		if (controller != null) {
			controller!!.registerDialog(this)
		} else {
			dismiss()
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		updateNightMode()

		val view = inflate(R.layout.fragment_gradient_editor, container, false)

		return view
	}

	override fun getThemeId(): Int {
		return if (nightMode) R.style.OsmandDarkTheme_DarkActionbar else R.style.OsmandLightTheme_DarkActionbar
	}

	override fun onResume() {
		super.onResume()
		callMapActivity {
			it.disableDrawer()
		}
	}

	override fun onPause() {
		super.onPause()
		callMapActivity {
			it.enableDrawer()
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		controller?.finishProcessIfNeeded(activity)
	}
}