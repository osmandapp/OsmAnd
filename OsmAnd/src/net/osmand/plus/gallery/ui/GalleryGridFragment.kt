package net.osmand.plus.gallery.ui

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.appbar.AppBarLayout
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseFullScreenFragment
import net.osmand.plus.gallery.contract.IGalleryGridView
import net.osmand.plus.gallery.controller.GalleryGridController
import net.osmand.plus.gallery.model.GalleryDisplayMode
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.model.GalleryToolbarAction
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.helpers.AndroidUiHelper.isOrientationPortrait
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection

class GalleryGridFragment : BaseFullScreenFragment(), IGalleryGridView {

	private lateinit var appBarLayout: AppBarLayout
	private lateinit var toolbar: Toolbar
	private lateinit var toolbarTitle: TextView
	private lateinit var backButton: AppCompatImageView
	private lateinit var actionsContainer: LinearLayout
	private lateinit var recyclerView: GalleryGridRecyclerView
	private lateinit var adapter: GalleryGridAdapter
	private lateinit var scaleDetector: ScaleGestureDetector
	private lateinit var itemDecorator: GalleryGridItemDecorator

	private var controller: GalleryGridController? = null
	private var displayModeTransition: GalleryDisplayModeTransition? = null
	private var pendingItemsUpdate = false

	@SuppressLint("ClickableViewAccessibility")
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		updateNightMode()

		val controllerId = arguments?.getString(CONTROLLER_ID_KEY) ?: return null
		controller = app.dialogManager.findController(controllerId) as? GalleryGridController
			?: return null
		controller?.attach(this)

		val view = inflate(R.layout.gallery_grid_fragment, container, false)
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view)

		setupScaleDetector()
		setupRecyclerView(view)

		appBarLayout = view.findViewById(R.id.app_bar_layout)
		toolbar = view.findViewById(R.id.toolbar)
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title)
		backButton = toolbar.findViewById(R.id.back_button)
		actionsContainer = toolbar.findViewById(R.id.actions_container)
		setupToolbar()
		setupOnBackPressedCallback()

		return view
	}

	private fun setupRecyclerView(view: View) {
		recyclerView = view.findViewById(R.id.content_list)
		recyclerView.setGestureFinishedListener { controller?.onPinchGestureFinished() }
		itemDecorator = GalleryGridItemDecorator(app)
		recyclerView.viewTreeObserver.addOnGlobalLayoutListener(
			object : ViewTreeObserver.OnGlobalLayoutListener {
				override fun onGlobalLayout() {
					recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
					val ctrl = controller ?: return
					val activity = mapActivity ?: return

					adapter = ctrl.createAdapter(activity, recyclerView.measuredWidth, nightMode)
					adapter.displayMode = ctrl.getDisplayMode()
					adapter.selectionMode = ctrl.isSelectionMode()
					adapter.setItems(ctrl.getGalleryItems())

					recyclerView.adapter = adapter
					recyclerView.setScaleDetector(scaleDetector)
					recyclerView.addItemDecoration(itemDecorator)
					applyLayoutManager()
				}
			}
		)
	}

	private fun applyLayoutManager() {
		val ctrl = controller ?: return
		val isList = ctrl.getDisplayMode() == GalleryDisplayMode.LIST
		recyclerView.layoutManager = if (isList) {
			LinearLayoutManager(app)
		} else {
			GridLayoutManager(app, ctrl.getSpanCount(isPortrait())).apply {
				spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
					override fun getSpanSize(position: Int): Int =
						if (adapter.getItem(position) is GalleryItem.Media) 1 else spanCount
				}
			}
		}
		val sidePadding = if (isList) {
			0
		} else {
			AndroidUtils.dpToPx(app, GalleryGridItemDecorator.GRID_SIDE_PADDING_DP)
		}
		val bottomPadding = resources.getDimensionPixelSize(R.dimen.content_padding)
		recyclerView.setPadding(sidePadding, 0, sidePadding, bottomPadding)
	}

	@SuppressLint("ClickableViewAccessibility")
	private fun setupScaleDetector() {
		scaleDetector = ScaleGestureDetector(requireMapActivity(),
			object : ScaleGestureDetector.OnScaleGestureListener {
				override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
					controller?.onScaleBegin()
					return true
				}

				override fun onScale(detector: ScaleGestureDetector): Boolean {
					controller?.onScaleChanged(detector.scaleFactor)
					return true
				}

				override fun onScaleEnd(detector: ScaleGestureDetector) {
					controller?.onScaleEnd()
				}
			}
		)
	}

	override fun updateSpan() {
		if (!::adapter.isInitialized) return
		val ctrl = controller ?: return
		if (ctrl.getDisplayMode() != GalleryDisplayMode.GRID) return

		val manager = recyclerView.layoutManager as? GridLayoutManager ?: return
		manager.spanCount = ctrl.getSpanCount(isPortrait())
		for (i in 0 until adapter.itemCount) {
			if (adapter.getItem(i) is GalleryItem.Media) {
				adapter.notifyItemChanged(i)
			}
		}
	}

	override fun updateDisplayMode() {
		if (!::adapter.isInitialized) return
		val ctrl = controller ?: return

		// Morph the visible media items between the two layouts
		displayModeTransition?.cancel()
		val toList = ctrl.getDisplayMode() == GalleryDisplayMode.LIST
		val transition = GalleryDisplayModeTransition(recyclerView)
		transition.captureStart(toList)

		adapter.displayMode = ctrl.getDisplayMode()
		applyLayoutManager()
		adapter.setItems(ctrl.getGalleryItems())

		displayModeTransition = transition
		transition.start {
			displayModeTransition = null
			if (pendingItemsUpdate) {
				pendingItemsUpdate = false
				updateItems()
			}
		}
	}

	override fun updateToolbar() {
		renderToolbar()
		callMapActivity(MapActivity::updateStatusBarColor)
	}

	override fun updateItems() {
		if (!::adapter.isInitialized) return
		val ctrl = controller ?: return
		// Items mustn't change under the display mode morph; apply once the transition finishes.
		if (displayModeTransition != null) {
			pendingItemsUpdate = true
			return
		}
		adapter.selectionMode = ctrl.isSelectionMode()
		adapter.setItems(ctrl.getGalleryItems(), animated = true)
	}

	override fun updateSelection() {
		if (!::adapter.isInitialized) return
		val ctrl = controller ?: return
		adapter.selectionMode = ctrl.isSelectionMode()
		adapter.notifySelectionChanged()
	}

	private fun setupToolbar() {
		renderToolbar()
	}

	private fun renderToolbar() {
		val ctrl = controller ?: return
		val selection = ctrl.isSelectionMode()

		val bgColor = if (selection) {
			ColorUtilities.getToolbarActiveColor(app, nightMode)
		} else {
			ColorUtilities.getColor(app, ColorUtilities.getListBgColorId(nightMode))
		}
		appBarLayout.setBackgroundColor(bgColor)
		toolbar.setBackgroundColor(bgColor)

		val contentColor = if (selection) {
			ContextCompat.getColor(app, R.color.active_buttons_and_links_text_light)
		} else {
			ColorUtilities.getPrimaryTextColor(app, nightMode)
		}
		val iconColor = if (selection) {
			ContextCompat.getColor(app, R.color.active_buttons_and_links_text_light)
		} else {
			ColorUtilities.getDefaultIconColor(app, nightMode)
		}

		toolbarTitle.text = if (selection) {
			ctrl.getSelectedCount().toString()
		} else {
			ctrl.getScreenTitle()
		}
		toolbarTitle.setTextColor(contentColor)

		val backIconId = if (selection) R.drawable.ic_action_close else AndroidUtils.getNavigationIconResId(app)
		backButton.setImageResource(backIconId)
		backButton.imageTintList = ColorStateList.valueOf(iconColor)
		backButton.setOnClickListener { onBackPressed() }

		renderToolbarActions(ctrl.getToolbarActions(), iconColor)
		AndroidUiHelper.updateVisibility(toolbar.findViewById(R.id.toolbar_subtitle), false)
	}

	private fun renderToolbarActions(actions: List<GalleryToolbarAction>, iconColor: Int) {
		actionsContainer.removeAllViews()
		val size = resources.getDimensionPixelSize(R.dimen.toolbar_height)
		val borderlessBg = TypedValue().also {
			requireContext().theme.resolveAttribute(
				android.R.attr.selectableItemBackgroundBorderless, it, true
			)
		}.resourceId
		for (action in actions) {
			val iv = AppCompatImageView(requireContext()).apply {
				layoutParams = LinearLayout.LayoutParams(size, size)
				scaleType = ImageView.ScaleType.CENTER
				setImageResource(action.iconId)
				imageTintList = ColorStateList.valueOf(iconColor)
				contentDescription = getString(action.titleId)
				setBackgroundResource(borderlessBg)
				setOnClickListener { controller?.handleGalleryAction(this, action.action) }
			}
			actionsContainer.addView(iv)
		}
	}

	private fun setupOnBackPressedCallback() {
		requireActivity().onBackPressedDispatcher.addCallback(
			viewLifecycleOwner,
			object : OnBackPressedCallback(true) {
				override fun handleOnBackPressed() = onBackPressed()
			}
		)
	}

	private fun onBackPressed() {
		val ctrl = controller
		if (ctrl != null && ctrl.isSelectionMode()) {
			ctrl.exitSelectionMode()
		} else {
			activity?.supportFragmentManager?.popBackStack()
		}
	}

	override fun getStatusBarColorId(): Int {
		AndroidUiHelper.setStatusBarContentColor(view, nightMode)
		return if (controller?.isSelectionMode() == true) {
			ColorUtilities.getStatusBarActiveColorId(nightMode)
		} else {
			ColorUtilities.getListBgColorId(nightMode)
		}
	}

	override fun getContentStatusBarNightMode() = nightMode

	override fun onResume() {
		super.onResume()
		callMapActivity(MapActivity::disableDrawer)
	}

	override fun onPause() {
		super.onPause()
		callMapActivity(MapActivity::enableDrawer)
	}

	override fun onDestroy() {
		super.onDestroy()
		displayModeTransition?.cancel()
		displayModeTransition = null
		controller?.onScreenDestroyed(activity)
	}

	override fun getInsetTargets(): InsetTargetsCollection {
		return super.getInsetTargets().apply {
			replace(InsetTarget.createScrollable(R.id.content_list))
		}
	}

	// IGalleryGridView
	override fun getMapActivity(): MapActivity? = super.getMapActivity()
	override fun isNightMode(): Boolean = nightMode
	override fun isPortrait(): Boolean = isOrientationPortrait(requireActivity())

	companion object {
		const val TAG = "GalleryGridFragment"
		private const val CONTROLLER_ID_KEY = "controller_id"

		@JvmStatic
		fun showInstance(activity: FragmentActivity, controllerId: String) {
			val manager: FragmentManager = activity.supportFragmentManager
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				manager.beginTransaction()
					.add(R.id.fragmentContainer, newInstance(controllerId), TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss()
			}
		}

		private fun newInstance(controllerId: String) = GalleryGridFragment().apply {
			arguments = Bundle().apply { putString(CONTROLLER_ID_KEY, controllerId) }
		}
	}
}