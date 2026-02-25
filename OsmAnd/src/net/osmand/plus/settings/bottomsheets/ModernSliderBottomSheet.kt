package net.osmand.plus.settings.bottomsheets

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.google.android.material.slider.Slider
import net.osmand.plus.R
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem
import net.osmand.plus.base.dialog.data.DialogExtra
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.controllers.IModernSliderDialogController
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.UiUtilities

class ModernSliderBottomSheet : CustomizableBottomSheet() {

	companion object {
		const val TAG = "ModernSliderBottomSheet"

		@JvmStatic
		fun showInstance(
			manager: FragmentManager,
			appMode: ApplicationMode,
			processId: String
		) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = ModernSliderBottomSheet()
				fragment.setProcessId(processId)
				fragment.setAppMode(appMode)
				fragment.show(manager, TAG)
			}
		}
	}

	private var controller: IModernSliderDialogController? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		controller = manager.findController(processId) as? IModernSliderDialogController
	}

	override fun createMenuItems(savedInstanceState: Bundle?) {
		if (context == null || displayData == null || controller == null) return
		val view = inflate(R.layout.fragment_modern_slider)
		setupCustomView(view)

		items.add(SimpleBottomSheetItem.Builder().setCustomView(view).create())
	}

	private fun setupCustomView(view: View) {
		val tvTitle = view.findViewById<TextView>(R.id.title)
		val tvDesc = view.findViewById<TextView>(R.id.description)

		val tvSliderTitle = view.findViewById<TextView>(R.id.slider_title)
		val tvSliderSummary = view.findViewById<TextView>(R.id.slider_summary)

		val slider = view.findViewById<Slider>(R.id.slider)
		val tvMinValue = view.findViewById<TextView>(R.id.value_min)
		val tvMaxValue = view.findViewById<TextView>(R.id.value_max)

		tvTitle.text = displayData?.getExtra(DialogExtra.TITLE) as? String
		tvDesc.text = displayData?.getExtra(DialogExtra.SUBTITLE) as? String

		tvSliderTitle.text = controller?.getSliderTitle()
		tvSliderSummary.text = controller?.getSliderSummary()

		val limits = controller?.getSliderLimits()
		if (limits != null) {
			slider.valueFrom = limits.min.toFloat()
			slider.valueTo = limits.max.toFloat()

			val currentValue = controller?.getSelectedValue()?.toFloat() ?: limits.min.toFloat()
			// Ensure the value is within bounds to prevent crash from Material Slider
			slider.value = currentValue.coerceIn(limits.min.toFloat(), limits.max.toFloat())

			tvMinValue.text = controller?.formatValue(limits.min)
			tvMaxValue.text = controller?.formatValue(limits.max)
		}

		// Handle value changes dynamically
		slider.addOnChangeListener { _, value, _ ->
			controller?.onChangeValue(value)
			// Update the UI summary text immediately
			tvSliderSummary.text = controller?.formatValue(value)
		}
		val accentColor = controller?.getSliderColor(app, nightMode)
		UiUtilities.setupSlider(slider, nightMode, accentColor, true)
	}

	override fun getDismissButtonTextId(): Int = R.string.shared_string_cancel

	override fun getRightBottomButtonTextId(): Int = R.string.shared_string_apply

	override fun onRightBottomButtonClick() {
		controller?.onApplyChanges()
		dismiss()
	}

	override fun onDestroy() {
		super.onDestroy()
		controller?.onDestroy(activity)
	}
}