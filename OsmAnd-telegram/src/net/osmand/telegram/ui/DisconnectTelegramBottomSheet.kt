package net.osmand.telegram.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.osmand.telegram.R
import net.osmand.telegram.ui.views.BottomSheetDialog

class DisconnectTelegramBottomSheet : DialogFragment() {

	override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(context!!)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val mainView = inflater.inflate(R.layout.bottom_sheet_disconnect_telegram, container, false)

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

		mainView.findViewById<TextView>(R.id.secondary_btn).apply {
			setText(R.string.shared_string_close)
			setOnClickListener { dismiss() }
		}

		return mainView
	}

	companion object {

		private const val TAG = "DisconnectTelegramBottomSheet"

		fun showInstance(fm: androidx.fragment.app.FragmentManager): Boolean {
			return try {
				DisconnectTelegramBottomSheet().show(fm, TAG)
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}
