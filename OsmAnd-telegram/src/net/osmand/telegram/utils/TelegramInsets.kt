package net.osmand.telegram.utils

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import net.osmand.telegram.R
import kotlin.math.max

private data class InitialPadding(
	val left: Int,
	val top: Int,
	val right: Int,
	val bottom: Int
)

private data class InitialMargins(
	val left: Int,
	val top: Int,
	val right: Int,
	val bottom: Int
)

fun Activity.setupTelegramEdgeToEdge(
	systemStatusBarColor: Int = Color.TRANSPARENT,
	systemNavigationBarColor: Int = Color.TRANSPARENT
) {
	window.setupTelegramEdgeToEdge(systemStatusBarColor, systemNavigationBarColor)
}

fun Window.setupTelegramEdgeToEdge(
	systemStatusBarColor: Int = Color.TRANSPARENT,
	systemNavigationBarColor: Int = Color.TRANSPARENT
) {
	WindowCompat.setDecorFitsSystemWindows(this, false)
	if (Build.VERSION.SDK_INT >= 21) {
		clearFlags(
			WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
		)
		addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
		statusBarColor = systemStatusBarColor
		navigationBarColor = systemNavigationBarColor
	}
	WindowCompat.getInsetsController(this, decorView).apply {
		isAppearanceLightStatusBars = true
		if (Build.VERSION.SDK_INT >= 26) {
			isAppearanceLightNavigationBars = true
		}
	}
	ViewCompat.requestApplyInsets(decorView)
}

fun WindowInsetsCompat.telegramTopInset(): Int {
	val systemBars = getInsets(WindowInsetsCompat.Type.systemBars())
	val cutout = getInsets(WindowInsetsCompat.Type.displayCutout())
	return max(systemBars.top, cutout.top)
}

fun WindowInsetsCompat.telegramLeftInset(): Int {
	val systemBars = getInsets(WindowInsetsCompat.Type.systemBars())
	val cutout = getInsets(WindowInsetsCompat.Type.displayCutout())
	return max(systemBars.left, cutout.left)
}

fun WindowInsetsCompat.telegramRightInset(): Int {
	val systemBars = getInsets(WindowInsetsCompat.Type.systemBars())
	val cutout = getInsets(WindowInsetsCompat.Type.displayCutout())
	return max(systemBars.right, cutout.right)
}

fun WindowInsetsCompat.telegramBottomInset(includeIme: Boolean = false): Int {
	val systemBars = getInsets(WindowInsetsCompat.Type.systemBars())
	val cutout = getInsets(WindowInsetsCompat.Type.displayCutout())
	val ime = if (includeIme) {
		getInsets(WindowInsetsCompat.Type.ime())
	} else {
		Insets.NONE
	}
	return max(max(systemBars.bottom, cutout.bottom), ime.bottom)
}

fun View.doOnTelegramInsets(block: (View, WindowInsetsCompat) -> Unit) {
	ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
		block(view, insets)
		insets
	}
	requestApplyInsetsWhenAttached()
}

fun View.applyTopSystemWindowInsets() {
	val initialPadding = recordInitialPadding()
	doOnTelegramInsets { view, insets ->
		view.setPadding(
			initialPadding.left,
			initialPadding.top + insets.telegramTopInset(),
			initialPadding.right,
			initialPadding.bottom
		)
	}
}

fun View.applyHorizontalSystemWindowInsets() {
	val initialPadding = recordInitialPadding()
	doOnTelegramInsets { view, insets ->
		view.setPadding(
			initialPadding.left + insets.telegramLeftInset(),
			initialPadding.top,
			initialPadding.right + insets.telegramRightInset(),
			initialPadding.bottom
		)
	}
}

fun View.applyBottomSystemWindowInsets(
	includeIme: Boolean = false,
	resizeHeight: Boolean = false
) {
	val initialPadding = recordInitialPadding()
	val initialHeight = layoutParams?.height ?: 0
	doOnTelegramInsets { view, insets ->
		val bottomInset = insets.telegramBottomInset(includeIme)
		if (resizeHeight && initialHeight > 0) {
			val params = view.layoutParams
			if (params != null && params.height != initialHeight + bottomInset) {
				params.height = initialHeight + bottomInset
				view.layoutParams = params
			}
		}
		view.setPadding(
			initialPadding.left,
			initialPadding.top,
			initialPadding.right,
			initialPadding.bottom + bottomInset
		)
	}
}

fun View.applyBottomSystemWindowMargin(
	includeIme: Boolean = false,
	baseBottomMargin: Int? = null
) {
	val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return
	val initialMargins = InitialMargins(
		params.leftMargin,
		params.topMargin,
		params.rightMargin,
		baseBottomMargin ?: params.bottomMargin
	)
	doOnTelegramInsets { view, insets ->
		val marginParams = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return@doOnTelegramInsets
		marginParams.setMargins(
			initialMargins.left,
			initialMargins.top,
			initialMargins.right,
			initialMargins.bottom + insets.telegramBottomInset(includeIme)
		)
		view.layoutParams = marginParams
	}
}

fun View.applyScrollableBottomSystemWindowInsets(includeIme: Boolean = false) {
	val initialPadding = recordInitialPadding()
	if (this is ViewGroup) {
		clipToPadding = false
	}
	doOnTelegramInsets { view, insets ->
		view.setPadding(
			initialPadding.left,
			initialPadding.top,
			initialPadding.right,
			initialPadding.bottom + insets.telegramBottomInset(includeIme)
		)
	}
}

fun View.applyBottomSheetWindowInsets() {
	findViewById<View>(R.id.bottom_sheet_actions)?.applyBottomSystemWindowInsets(
		includeIme = false,
		resizeHeight = true
	)
}

private fun View.recordInitialPadding(): InitialPadding {
	return InitialPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
}

private fun View.requestApplyInsetsWhenAttached() {
	if (ViewCompat.isAttachedToWindow(this)) {
		ViewCompat.requestApplyInsets(this)
	} else {
		addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
			override fun onViewAttachedToWindow(view: View) {
				view.removeOnAttachStateChangeListener(this)
				ViewCompat.requestApplyInsets(view)
			}

			override fun onViewDetachedFromWindow(view: View) = Unit
		})
	}
}
