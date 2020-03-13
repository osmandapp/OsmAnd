package net.osmand.telegram.ui

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication

open class BaseDialogFragment : DialogFragment() {

	val app: TelegramApplication
		get() = activity?.application as TelegramApplication

	val telegramHelper get() = app.telegramHelper
	val settings get() = app.settings

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		when {
			Build.VERSION.SDK_INT >= 23 -> setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_FRAME, R.style.AppTheme_NoActionbar_Transparent)
			Build.VERSION.SDK_INT >= 19 -> setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_FRAME, R.style.AppTheme_NoActionbar_Translucent)
			else -> setStyle(androidx.fragment.app.DialogFragment.STYLE_NO_FRAME, R.style.AppTheme_NoActionbar)
		}
	}
}