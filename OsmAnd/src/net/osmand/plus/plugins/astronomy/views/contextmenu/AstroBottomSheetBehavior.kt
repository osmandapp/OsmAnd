package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.IdRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

class AstroBottomSheetBehavior<V : View>(
	context: Context,
	attrs: AttributeSet?
) : BottomSheetBehavior<V>(context, attrs) {

	@IdRes
	private var lockedNestedScrollTargetId: Int = View.NO_ID

	fun setLockedNestedScrollTargetId(@IdRes id: Int) {
		lockedNestedScrollTargetId = id
	}

	override fun onNestedPreScroll(
		coordinatorLayout: CoordinatorLayout,
		child: V,
		target: View,
		dx: Int,
		dy: Int,
		consumed: IntArray,
		type: Int
	) {
		if (shouldIgnoreNestedScrollFrom(target)) {
			return
		}
		super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
	}

	override fun onNestedPreFling(
		coordinatorLayout: CoordinatorLayout,
		child: V,
		target: View,
		velocityX: Float,
		velocityY: Float
	): Boolean {
		if (shouldIgnoreNestedScrollFrom(target)) {
			return false
		}
		return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)
	}

	private fun shouldIgnoreNestedScrollFrom(target: View): Boolean {
		if (lockedNestedScrollTargetId == View.NO_ID) {
			return false
		}
		if (target.id == lockedNestedScrollTargetId) {
			return true
		}
		var parent = target.parent
		while (parent is View) {
			if (parent.id == lockedNestedScrollTargetId) {
				return true
			}
			parent = parent.parent
		}
		return false
	}
}