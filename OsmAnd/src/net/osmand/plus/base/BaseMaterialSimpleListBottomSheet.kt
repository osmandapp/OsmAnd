package net.osmand.plus.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import net.osmand.plus.R
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.utils.InsetsUtils

abstract class BaseMaterialSimpleListBottomSheet : BaseMaterialModalBottomSheetDialogFragment() {

	protected lateinit var mainView: View

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		mainView = inflater.inflate(getLayoutId(), container, false)
		setupHeaderCloseButton(mainView)
		return mainView
	}

	override fun onStart() {
		super.onStart()
		dialog?.window?.apply {
			addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
			setDimAmount(0.30f)
		}
	}

	protected open fun getLayoutId(): Int = R.layout.bottom_sheet_simple_list_with_header

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = super.getInsetTargets()
		if (::mainView.isInitialized) {
			collection.add(
				InsetTarget.createCustomBuilder(getContentContainerId())
					.portraitSides(InsetsUtils.InsetSide.BOTTOM)
					.landscapeSides(InsetsUtils.InsetSide.BOTTOM)
					.applyPadding(true)
			)
		}
		collection.removeType(InsetTarget.Type.ROOT_INSET)
		return collection
	}

	override fun onBottomSheetReady(
		bottomSheet: FrameLayout,
		behavior: BottomSheetBehavior<FrameLayout>
	) {
		behavior.isFitToContents = true
	}

	override fun getScrollableView(): View? {
		return if (::mainView.isInitialized) {
			mainView.findViewById<NestedScrollView>(R.id.bottomSheetRoot)
		} else {
			null
		}
	}

	protected open fun getContentContainerId(): Int = R.id.bottomSheetContent
}
