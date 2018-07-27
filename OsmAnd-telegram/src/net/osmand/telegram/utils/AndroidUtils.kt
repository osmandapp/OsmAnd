package net.osmand.telegram.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.support.annotation.AttrRes
import android.support.annotation.ColorInt
import android.support.v4.app.ActivityCompat
import android.support.v4.content.FileProvider
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.View
import android.view.inputmethod.InputMethodManager
import java.io.File

object AndroidUtils {
	
	private const val PERMISSION_REQUEST_LOCATION = 1

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


	fun requestLocationPermission(activity: Activity) {
		ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_LOCATION)
	}
	
	fun dpToPx(ctx: Context, dp: Float): Int {
		val r = ctx.resources
		return TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.displayMetrics
		).toInt()
	}

	fun getStatusBarHeight(ctx: Context): Int {
		var result = 0
		val resourceId = ctx.resources.getIdentifier("status_bar_height", "dimen", "android")
		if (resourceId > 0) {
			result = ctx.resources.getDimensionPixelSize(resourceId)
		}
		return result
	}

	fun addStatusBarPadding19v(ctx: Context, view: View) {
		if (Build.VERSION.SDK_INT >= 19) {
			view.apply {
				setPadding(paddingLeft, paddingTop + getStatusBarHeight(ctx), paddingRight, paddingBottom)
			}
		}
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

	fun isGooglePlayInstalled(ctx: Context): Boolean {
		try {
			ctx.packageManager.getPackageInfo("com.android.vending", 0)
		} catch (e: PackageManager.NameNotFoundException) {
			return false
		}

		return true
	}

	fun getPlayMarketLink(ctx: Context, packageName: String): String {
		if (isGooglePlayInstalled(ctx)) {
			return "market://details?id=$packageName"
		}
		return "https://play.google.com/store/apps/details?id=$packageName"
	}

	fun isIntentSafe(ctx: Context, intent: Intent) =
		ctx.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
}
