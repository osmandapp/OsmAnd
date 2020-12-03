package net.osmand.telegram.ui.views

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import net.osmand.telegram.R

class BottomSheetDialog(ctx: Context) : Dialog(ctx, R.style.AppTheme_BottomSheet) {

	init {
		requestWindowFeature(Window.FEATURE_NO_TITLE)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		window?.apply {
			if (Build.VERSION.SDK_INT >= 21) {
				clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
				addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
			}
			setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			attributes?.windowAnimations = R.style.Animations_PopUpMenu_Bottom
		}
	}

	override fun setContentView(layoutResID: Int) {
		super.setContentView(wrapInContainer(layoutResID, null, null))
	}

	override fun setContentView(view: View) {
		super.setContentView(wrapInContainer(0, view, null))
	}

	override fun setContentView(view: View, params: ViewGroup.LayoutParams?) {
		super.setContentView(wrapInContainer(0, view, params))
	}

	private fun wrapInContainer(
		layoutResId: Int,
		view: View?,
		params: ViewGroup.LayoutParams?
	): View {
		val res = View.inflate(context, R.layout.bottom_sheet_dialog, null)
		val container = res.findViewById<ViewGroup>(R.id.content_container)
		var v = view

		if (layoutResId != 0 && v == null) {
			v = layoutInflater.inflate(layoutResId, container, false)
		}
		if (params == null) {
			container.addView(v)
		} else {
			container.addView(v, params)
		}

		res.findViewById<View>(R.id.touch_outside).setOnClickListener {
			cancel()
		}

		// Consume the event and prevent it from falling through
		container.setOnTouchListener { _, _ -> return@setOnTouchListener true }

		return res
	}
}
