package net.osmand.plus.settings.controllers

import android.content.Context
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.base.containers.Limits
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider
import net.osmand.plus.utils.ColorUtilities

interface IModernSliderDialogController : IDisplayDataProvider {

	fun getSliderLimits(): Limits<Int> = Limits(0, 100)

	fun getSelectedValue(): Int = 50

	fun onChangeValue(newValue: Float)

	fun getSliderTitle(): String

	fun getSliderSummary(): String

	fun getSliderColor(context: Context, nightMode: Boolean): Int {
		return ColorUtilities.getActiveColor(context, nightMode)
	}

	fun onApplyChanges()

	fun onDestroy(activity: FragmentActivity?)

	fun formatValue(number: Number): String = number.toString()
}