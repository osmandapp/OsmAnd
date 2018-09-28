package net.osmand.telegram.ui

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.ui.views.BottomSheetDialog


class AddDeviceBottomSheet : DialogFragment() {
	private lateinit var editText: EditText

	override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(context!!)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val mainView = inflater.inflate(R.layout.bottom_sheet_add_device, container, false)

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

		editText = mainView.findViewById<EditText>(R.id.device_id_edit_text)

		mainView.findViewById<TextView>(R.id.secondary_btn).apply {
			setText(R.string.shared_string_cancel)
			setOnClickListener { dismiss() }
		}

		mainView.findViewById<TextView>(R.id.primary_btn).apply {
			setText(R.string.shared_string_save)
			setOnClickListener {
				val app = activity?.application as TelegramApplication
				val settings = app.settings
				val newDeviceId = editText.text.toString()
				settings.addSharingDevice(newDeviceId)
				targetFragment?.also { target ->
					val intent = Intent()
					intent.putExtra(NEW_DEVICE_ID, newDeviceId)
					target.onActivityResult(targetRequestCode, ADD_DEVICE_REQUEST_CODE, intent)
				}
				dismiss()
			}
		}


		return mainView
	}

	companion object {

		const val ADD_DEVICE_REQUEST_CODE = 5
		const val NEW_DEVICE_ID = "NEW_DEVICE_ID"

		private const val TAG = "AddDeviceBottomSheet"

		fun showInstance(fm: FragmentManager, target: Fragment): Boolean {
			return try {
				AddDeviceBottomSheet().apply {
					setTargetFragment(target, ADD_DEVICE_REQUEST_CODE)
					show(fm, TAG)
				}
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}
}