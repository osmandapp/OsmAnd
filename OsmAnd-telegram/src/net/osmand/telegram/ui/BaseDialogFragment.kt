package net.osmand.telegram.ui

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.ui.views.BottomSheetDialog
import net.osmand.telegram.utils.applyHorizontalSystemWindowInsets
import net.osmand.telegram.utils.setupTelegramEdgeToEdge

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

	override fun onStart() {
		super.onStart()
		dialog?.window?.apply {
			val currentStatusBarColor = if (Build.VERSION.SDK_INT >= 21) statusBarColor else Color.TRANSPARENT
			setupTelegramEdgeToEdge(systemStatusBarColor = currentStatusBarColor)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		if (dialog !is BottomSheetDialog && !view.fitsSystemWindows) {
			view.applyHorizontalSystemWindowInsets()
		}
	}
}