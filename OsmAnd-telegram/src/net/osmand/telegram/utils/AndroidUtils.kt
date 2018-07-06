package net.osmand.telegram.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.support.annotation.AttrRes
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.View
import android.view.inputmethod.InputMethodManager
import java.io.File

object AndroidUtils {

	private fun isHardwareKeyboardAvailable(context: Context): Boolean {
		return context.resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS
	}

	fun softKeyboardDelayed(view: View) {
		view.post {
			view.requestFocus()
			if (!isHardwareKeyboardAvailable(view.context)) {
				val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
				imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
			}
		}
	}

	fun hideSoftKeyboard(activity: Activity, input: View?) {
		val inputMethodManager = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
		if (inputMethodManager != null) {
			if (input != null) {
				val windowToken = input.windowToken
				if (windowToken != null) {
					inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
				}
			}
		}
	}
    
	fun isLocationPermissionAvailable(context: Context): Boolean {
		return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
	}

	fun dpToPx(ctx: Context, dp: Float): Int {
		val r = ctx.resources
		return TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.displayMetrics
		).toInt()
	}

	@ColorInt
	fun getAttrColor(ctx: Context, @AttrRes attrId: Int, @ColorInt defaultColor: Int = 0): Int {
		val ta = ctx.theme.obtainStyledAttributes(intArrayOf(attrId))
		val color = ta.getColor(0, defaultColor)
		ta.recycle()
		return color
	}

	fun getUriForFile(context: Context, file: File): Uri {
		return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			Uri.fromFile(file)
		} else {
			FileProvider.getUriForFile(context,  "net.osmand.telegram.fileprovider", file)
		}
	}
}
