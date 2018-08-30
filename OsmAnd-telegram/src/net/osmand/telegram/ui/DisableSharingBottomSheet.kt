package net.osmand.telegram.ui

import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import net.osmand.telegram.R
import net.osmand.telegram.ui.views.BottomSheetDialog

class DisableSharingBottomSheet : DialogFragment() {

	override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(context!!)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val mainView = inflater.inflate(R.layout.bottom_sheet_disable_sharing, container, false)

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

		mainView.findViewById<TextView>(R.id.description).text =
				getString(R.string.disable_all_sharing_desc, arguments?.getInt(CHATS_COUNT_KEY, -1))

		mainView.findViewById<TextView>(R.id.secondary_btn).apply {
			setText(R.string.shared_string_cancel)
			setOnClickListener { dismiss() }
		}

		mainView.findViewById<TextView>(R.id.primary_btn).apply {
			setText(R.string.turn_off_all)
			setOnClickListener {
				targetFragment?.also { target ->
					target.onActivityResult(targetRequestCode, SHARING_DISABLED_REQUEST_CODE, null)
				}
				dismiss()
			}
		}

		return mainView
	}

	companion object {

		const val SHARING_DISABLED_REQUEST_CODE = 1

		private const val TAG = "DisableSharingBottomSheet"
		private const val CHATS_COUNT_KEY = "chats_count"

		fun showInstance(fm: FragmentManager, target: Fragment, chatsCount: Int): Boolean {
			return try {
				DisableSharingBottomSheet().apply {
					arguments = Bundle().apply { putInt(CHATS_COUNT_KEY, chatsCount) }
					setTargetFragment(target, SHARING_DISABLED_REQUEST_CODE)
					show(fm, TAG)
				}
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}
