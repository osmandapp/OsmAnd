package net.osmand.telegram.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent

class LockableViewPager : androidx.viewpager.widget.ViewPager {
	var swipeLocked: Boolean = false

	constructor(context: Context) : super(context) {}

	constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		return !swipeLocked && super.onTouchEvent(event)
	}

	override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
		return !swipeLocked && super.onInterceptTouchEvent(event)
	}

	override fun canScrollHorizontally(direction: Int): Boolean {
		return !swipeLocked && super.canScrollHorizontally(direction)
	}
}
