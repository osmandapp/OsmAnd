package net.osmand.plus.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.ShapeAppearanceModel
import net.osmand.plus.R
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.utils.InsetsUtils

abstract class BaseMaterialBottomSheetWithHeader : BaseMaterialModalBottomSheetDialogFragment() {

	protected lateinit var mainView: View

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		updateNightMode()
		mainView = inflate(getLayoutId(), container, false)
		mainView.findViewById<View>(R.id.dragHandle)?.visibility =
			if (shouldShowDragHandle()) View.VISIBLE else View.INVISIBLE
		setupHeaderCloseButton(mainView)
		setupRoundedCorners()
		return mainView
	}

	override fun onStart() {
		super.onStart()
		dialog?.window?.apply {
			addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
			setDimAmount(0.30f)
		}
	}

	protected open fun getLayoutId(): Int = R.layout.bottom_sheet_material_with_header

	protected open fun shouldShowDragHandle(): Boolean = true

	private fun setupRoundedCorners() {
		val card = mainView.findViewById<MaterialCardView>(R.id.bottomSheetCard)
		val largeShape = ShapeAppearanceModel.builder(
			card.context,
			R.style.Shape_Card28,
			0
		).build()
		card.shapeAppearanceModel = card.shapeAppearanceModel.toBuilder()
			.setTopLeftCornerSize(largeShape.topLeftCornerSize)
			.setTopRightCornerSize(largeShape.topRightCornerSize)
			.build()
	}

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
