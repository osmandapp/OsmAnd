package net.osmand.plus.mapcontextmenu.builders.rows

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.osmand.plus.R
import net.osmand.plus.base.BaseMaterialSimpleListBottomSheet
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog
import net.osmand.plus.utils.AndroidUtils

class PoiAdditionalActionsBottomSheet : BaseMaterialSimpleListBottomSheet(), IDialog {

	private var controller: PoiAdditionalMultiValueDialogController? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		controller = PoiAdditionalMultiValueDialogController.getExistedInstance(osmandApp)
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
		val content = mainView.findViewById<LinearLayout>(R.id.bottomSheetContent)
		content.minimumHeight = getDimensionPixelSize(R.dimen.bottom_sheet_menu_peek_height)
		content.setPadding(
			content.paddingLeft,
			content.paddingTop,
			content.paddingRight,
			getDimensionPixelSize(R.dimen.bottom_sheet_content_margin)
		)
		val controller = controller ?: return mainView
		mainView.findViewById<TextView>(R.id.title).text = controller.title
		val itemsContainer = mainView.findViewById<LinearLayout>(R.id.itemsContainer)
		controller.values.forEachIndexed { index, value ->
			itemsContainer.addView(createRow(itemsContainer, value, index < controller.values.lastIndex))
		}
		return mainView
	}

	override fun initialBottomSheetState(): Int = BottomSheetBehavior.STATE_EXPANDED

	override fun shouldSkipCollapsed(): Boolean = true

	override fun onBottomSheetReady(
		bottomSheet: FrameLayout,
		behavior: BottomSheetBehavior<FrameLayout>
	) {
		super.onBottomSheetReady(bottomSheet, behavior)
		bottomSheet.doOnPreDraw {
			val contentHeight = mainView.findViewById<View>(R.id.bottomSheetContent).height
			behavior.peekHeight = contentHeight
			behavior.state = BottomSheetBehavior.STATE_EXPANDED
		}
	}

	private fun createRow(parent: ViewGroup, value: String, showDivider: Boolean): View {
		return layoutInflater.inflate(R.layout.bottom_sheet_item_active_color_text, parent, false).apply {
			findViewById<TextView>(R.id.itemText).text = value
			findViewById<View>(R.id.divider).visibility = if (showDivider) View.VISIBLE else View.GONE
			setOnClickListener {
				activity?.let { controller?.onItemClick(it, value) }
				dismiss()
			}
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		controller?.finishProcessIfNeeded(activity)
	}

	companion object {
		private val TAG = PoiAdditionalActionsBottomSheet::class.java.simpleName

		@JvmStatic
		fun showInstance(manager: FragmentManager): Boolean {
			if (!AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				return false
			}
			PoiAdditionalActionsBottomSheet().show(manager, TAG)
			return true
		}
	}
}
