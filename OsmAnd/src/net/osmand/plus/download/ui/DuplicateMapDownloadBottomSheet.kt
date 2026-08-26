package net.osmand.plus.download.ui

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
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog
import net.osmand.plus.download.AbstractDownloadActivity
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.widgets.dialogbutton.DialogButton
import net.osmand.plus.widgets.dialogbutton.DialogButtonType

class DuplicateMapDownloadBottomSheet : BaseMaterialBottomSheetWithHeader(), IDialog {

	private var controller: DuplicateMapDownloadDialogController? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		controller = DuplicateMapDownloadDialogController.getExistedInstance(osmandApp)
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
		val itemsContainer = mainView.findViewById<LinearLayout>(R.id.itemsContainer)
		val content = inflate(
			R.layout.bottom_sheet_duplicate_map_download_content,
			itemsContainer,
			false
		)
		itemsContainer.addView(content)

		content.findViewById<TextView>(R.id.description).text = controller.description
		setupReplaceButton(content.findViewById(R.id.replaceButton), controller)
		setupKeepBothButton(content.findViewById(R.id.keepBothButton), controller)
		return mainView
	}

	private fun setupReplaceButton(
		button: DialogButton,
		controller: DuplicateMapDownloadDialogController
	) {
		button.setTitleId(controller.replaceButtonTitleId)
		button.setButtonType(controller.replaceButtonType)
		button.setOnClickListener {
			(activity as? AbstractDownloadActivity)?.let(controller::onReplace)
			dismiss()
		}
	}

	private fun setupKeepBothButton(
		button: DialogButton,
		controller: DuplicateMapDownloadDialogController
	) {
		button.setTitleId(R.string.keep_both)
		button.setButtonType(DialogButtonType.SECONDARY)
		button.setOnClickListener {
			(activity as? AbstractDownloadActivity)?.let(controller::onKeepBoth)
			dismiss()
		}
	}

	override fun initialBottomSheetState(): Int = BottomSheetBehavior.STATE_EXPANDED

	override fun shouldSkipCollapsed(): Boolean = true

	override fun shouldShowDragHandle(): Boolean = false

	override fun onDestroy() {
		super.onDestroy()
		controller?.finishProcessIfNeeded(activity)
	}

	companion object {
		private val TAG = DuplicateMapDownloadBottomSheet::class.java.simpleName

		fun canBeAdded(manager: FragmentManager): Boolean {
			return AndroidUtils.isFragmentCanBeAdded(manager, TAG, true)
		}

		fun showInstance(manager: FragmentManager) {
			DuplicateMapDownloadBottomSheet().show(manager, TAG)
		}
	}
}
