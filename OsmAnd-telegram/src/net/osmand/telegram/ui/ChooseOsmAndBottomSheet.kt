package net.osmand.telegram.ui

import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import net.osmand.telegram.R
import net.osmand.telegram.ui.views.BottomSheetDialog

class ChooseOsmAndBottomSheet : DialogFragment() {

	override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(context!!)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val mainView = inflater.inflate(R.layout.bottom_sheet_choose_osmand, container, false)

		mainView.findViewById<View>(R.id.scroll_view_container).setOnClickListener { dismiss() }

		BottomSheetBehavior.from(mainView.findViewById<View>(R.id.scroll_view))
			.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

				override fun onStateChanged(bottomSheet: View, newState: Int) {
					if (newState == BottomSheetBehavior.STATE_HIDDEN) {
						dismiss()
					}
				}

				override fun onSlide(bottomSheet: View, slideOffset: Float) {}
			})

		// TODO: inflate AppConn items

		mainView.findViewById<TextView>(R.id.secondary_btn).apply {
			setText(R.string.shared_string_cancel)
			setOnClickListener { dismiss() }
		}

		return mainView
	}

	companion object {

		private const val TAG = "ChooseOsmAndBottomSheet"

		fun showInstance(fm: FragmentManager): Boolean {
			return try {
				ChooseOsmAndBottomSheet().show(fm, TAG)
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}
