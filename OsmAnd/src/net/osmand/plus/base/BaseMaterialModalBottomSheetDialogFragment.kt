package net.osmand.plus.base

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import net.osmand.plus.R

open class BaseMaterialModalBottomSheetDialogFragment : BaseMaterialBottomSheetDialogFragment() {

	protected var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
		private set

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
			setCancelable(isSheetCancelable())
			setCanceledOnTouchOutside(isCanceledOnTouchOutside())
		}
	}

	override fun onStart() {
		super.onStart()
		setupBottomSheetBehavior()
	}

	protected open fun isSheetCancelable(): Boolean = true

	protected open fun isCanceledOnTouchOutside(): Boolean = true

	protected open fun shouldSkipCollapsed(): Boolean = false

	protected open fun initialBottomSheetState(): Int = BottomSheetBehavior.STATE_COLLAPSED

	protected open fun getPeekHeightAnchorView(): View? = null

	protected open fun getPeekHeightExtra(): Int = 0

	protected open fun getScrollableView(): View? = null

	protected open fun onBottomSheetReady(bottomSheet: FrameLayout, behavior: BottomSheetBehavior<FrameLayout>) {
	}

	protected fun setupHeaderCloseButton(root: View, closeButtonId: Int = R.id.closeBtn) {
		root.findViewById<View>(closeButtonId)?.setOnClickListener { dismiss() }
	}

	private fun setupBottomSheetBehavior() {
		val bottomSheetDialog = dialog as? BottomSheetDialog ?: return
		val bottomSheet =
			bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
				?: return
		val behavior = BottomSheetBehavior.from(bottomSheet).apply {
			skipCollapsed = shouldSkipCollapsed()
		}
		bottomSheetBehavior = behavior

		val anchor = getPeekHeightAnchorView()
		if (anchor != null) {
			anchor.doOnPreDraw {
				behavior.peekHeight = anchor.bottom + getPeekHeightExtra() + getSystemBarsBottomInset()
				behavior.state = initialBottomSheetState()
				scrollToTop()
				onBottomSheetReady(bottomSheet, behavior)
			}
		} else {
			behavior.state = initialBottomSheetState()
			scrollToTop()
			onBottomSheetReady(bottomSheet, behavior)
		}
	}

	private fun getSystemBarsBottomInset(): Int {
		return getLastRootInsets()?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
	}

	private fun scrollToTop() {
		getScrollableView()?.scrollTo(0, 0)
	}
}
