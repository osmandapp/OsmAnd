package net.osmand.telegram.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.osmand.PlatformUtil
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.TelegramSettings.LocationSource
import net.osmand.telegram.ui.views.BottomSheetDialog

class LocationSourceBottomSheet : DialogFragment() {

	private val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	private val log = PlatformUtil.getLog(LocationSourceBottomSheet::class.java)

	override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(requireContext())

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val mainView = inflater.inflate(R.layout.bottom_sheet_location_source, container, false)

		mainView.findViewById<View>(R.id.scroll_view_container).setOnClickListener { dismiss() }

		BottomSheetBehavior.from(mainView.findViewById(R.id.scroll_view))
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

		LocationSource.values().forEach {
			addItemView(mainView.findViewById(R.id.options_container), inflater, it)
		}

		return mainView
	}

	private fun addItemView(container: ViewGroup?, inflater: LayoutInflater, item: LocationSource) {
		val view = inflater.inflate(R.layout.item_left_rb, container, false)
		val radioButton = view.findViewById<RadioButton>(R.id.radio_button)
		val title = view.findViewById<TextView>(R.id.title)
		radioButton.isChecked = item == app.settings.locationSource
		title.text = getText(item.nameId)
		view.setOnClickListener {
			if (app.settings.locationSource != item) {
				app.settings.locationSource = item
				targetFragment?.also { target ->
					target.onActivityResult(
						targetRequestCode,
						LOCATION_SOURCE_PREFERENCES_UPDATED_REQUEST_CODE,
						null
					)
				}
			}
			dismiss()
		}
		container!!.addView(view)
	}

	companion object {

		private const val TAG = "LocationSourceBottomSheet"
		const val LOCATION_SOURCE_PREFERENCES_UPDATED_REQUEST_CODE = 7

		fun showInstance(fm: androidx.fragment.app.FragmentManager, target: androidx.fragment.app.Fragment): Boolean {
			return try {
				val dialog = LocationSourceBottomSheet()
				dialog.setTargetFragment(target, LOCATION_SOURCE_PREFERENCES_UPDATED_REQUEST_CODE)
				dialog.show(fm, TAG)
				true
			} catch (e: RuntimeException) {
				false
			}
		}
	}

}