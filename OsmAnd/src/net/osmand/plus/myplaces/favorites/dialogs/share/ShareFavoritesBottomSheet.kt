package net.osmand.plus.myplaces.favorites.dialogs.share

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.osmand.plus.R
import net.osmand.plus.base.BaseMaterialBottomSheetWithHeader
import net.osmand.plus.base.dialog.interfaces.dialog.IAskDismissDialog
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog
import net.osmand.plus.utils.AndroidUtils

class ShareFavoritesBottomSheet : BaseMaterialBottomSheetWithHeader(), IDialog,
	IAskDismissDialog, IAskRefreshDialogCompletely {

	private var controller: ShareFavoritesController? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		controller = ShareFavoritesController.getExistedInstance(osmandApp)
		if (controller != null) {
			controller?.registerDialog(this)
		} else {
			dismiss()
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		super.onCreateView(inflater, container, savedInstanceState)
		val controller = controller ?: return mainView

		mainView.findViewById<TextView>(R.id.title).text = controller.title
		mainView.findViewById<View>(R.id.closeBtn).setOnClickListener {
			controller.onDismissRequested()
			dismiss()
		}
		val itemsContainer = mainView.findViewById<LinearLayout>(R.id.itemsContainer)
		inflate(R.layout.bottom_sheet_share_favorites_content, itemsContainer, true)
		mainView.findViewById<View>(R.id.points_only_button).setOnClickListener {
			controller.onPointsOnlyClicked()
		}
		mainView.findViewById<View>(R.id.points_and_media_button).setOnClickListener {
			controller.onPointsAndMediaClicked()
		}
		updateContent()
		return mainView
	}

	override fun onResume() {
		super.onResume()
		controller?.onDialogResumed()
	}

	override fun onPause() {
		controller?.onDialogPaused()
		super.onPause()
	}

	override fun onAskRefreshDialogCompletely(processId: String) {
		if (processId == ShareFavoritesController.PROCESS_ID && view != null) {
			updateContent()
		}
	}

	override fun onAskDismissDialog(processId: String) {
		if (processId == ShareFavoritesController.PROCESS_ID) {
			dismissAllowingStateLoss()
		}
	}

	private fun updateContent() {
		val controller = controller ?: return
		val ready = controller.state == ShareFavoritesController.DialogState.READY
		mainView.findViewById<View>(R.id.ready_state_container).visibility =
			if (ready) View.VISIBLE else View.INVISIBLE
		mainView.findViewById<View>(R.id.preparing_state_container).visibility =
			if (ready) View.INVISIBLE else View.VISIBLE
		mainView.findViewById<TextView>(R.id.description).text = controller.description

		val pointsOnly = mainView.findViewById<View>(R.id.points_only_button)
		val pointsAndMedia = mainView.findViewById<View>(R.id.points_and_media_button)
		pointsOnly.isEnabled = ready
		pointsAndMedia.isEnabled = ready && controller.canSharePointsAndMedia

		val pointsOnlyTitle = getString(R.string.share_favorites_points_only)
		val pointsAndMediaTitle = getString(R.string.share_favorites_points_and_media)
		mainView.findViewById<TextView>(R.id.points_only_description).text =
			controller.pointsOnlyDetails
		mainView.findViewById<TextView>(R.id.points_and_media_description).text =
			controller.pointsAndMediaDetails
		pointsOnly.contentDescription = getString(
			R.string.ltr_or_rtl_combine_via_comma,
			pointsOnlyTitle,
			controller.pointsOnlyDetails
		)
		pointsAndMedia.contentDescription = getString(
			R.string.ltr_or_rtl_combine_via_comma,
			pointsAndMediaTitle,
			controller.pointsAndMediaDetails
		)
	}

	override fun initialBottomSheetState(): Int = BottomSheetBehavior.STATE_EXPANDED

	override fun shouldSkipCollapsed(): Boolean = true

	override fun shouldShowDragHandle(): Boolean = false

	override fun onDestroy() {
		super.onDestroy()
		controller?.finishProcessIfNeeded(activity)
	}

	companion object {
		private val TAG = ShareFavoritesBottomSheet::class.java.simpleName

		@JvmStatic
		fun canBeAdded(manager: FragmentManager): Boolean {
			return AndroidUtils.isFragmentCanBeAdded(manager, TAG, true)
		}

		@JvmStatic
		fun showInstance(manager: FragmentManager) {
			ShareFavoritesBottomSheet().show(manager, TAG)
		}
	}
}
