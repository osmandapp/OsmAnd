package net.osmand.plus.gallery.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.appbar.AppBarLayout
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseFullScreenFragment
import net.osmand.plus.gallery.contract.IGalleryGridView
import net.osmand.plus.gallery.controller.GalleryGridController
import net.osmand.plus.gallery.model.GalleryToolbarAction
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.helpers.AndroidUiHelper.isOrientationPortrait
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities

class GalleryGridFragment : BaseFullScreenFragment(), IGalleryGridView {

	private lateinit var appBarLayout: AppBarLayout
	private lateinit var toolbar: Toolbar
	private lateinit var toolbarTitle: TextView
	private lateinit var backButton: AppCompatImageView
	private lateinit var actionsContainer: LinearLayout

	private var controller: GalleryGridController? = null
	private var binder: GalleryGridBinder? = null
	private val toolbarRecolor by lazy { GalleryToolbarRecolor(app) }

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		updateNightMode()

		val controllerId = arguments?.getString(CONTROLLER_ID_KEY) ?: return null
		val controller = app.dialogManager.findController(controllerId) as? GalleryGridController ?: return null
		this.controller = controller
		controller.attach(this)

		val view = inflate(R.layout.gallery_grid_fragment, container, false)
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view)

		binder = GalleryGridBinder(view.findViewById(R.id.recycler_view), controller, requireActivity(), nightMode, sectionCards = false)

		appBarLayout = view.findViewById(R.id.app_bar_layout)
		toolbar = view.findViewById(R.id.toolbar)
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title)
		backButton = toolbar.findViewById(R.id.back_button)
		actionsContainer = toolbar.findViewById(R.id.actions_container)
		renderToolbar()
		setupOnBackPressedCallback()

		return view
	}

	override fun updateDisplayMode() {
		binder?.updateDisplayMode()
	}

	override fun updateToolbar() {
		renderToolbar()
		callMapActivity(MapActivity::updateStatusBarColor)
	}

	override fun updateItems() {
		binder?.updateItems()
	}

	override fun updateSelection() {
		binder?.updateSelection()
	}

	private fun renderToolbar() {
		val ctrl = controller ?: return
		val selection = ctrl.isSelectionMode()

		val bgColor = if (selection) {
			ColorUtilities.getToolbarActiveColor(app, nightMode)
		} else {
			ColorUtilities.getColor(app, ColorUtilities.getListBgColorId(nightMode))
		}
		toolbarRecolor.recolor(bgColor) {
			appBarLayout.setBackgroundColor(it)
			toolbar.setBackgroundColor(it)
		}

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

	override fun onDestroyView() {
		controller?.detach()
		toolbarRecolor.cancel()
		binder?.release()
		binder = null
		super.onDestroyView()
	}

	override fun onDestroy() {
		super.onDestroy()
		controller?.onScreenDestroyed(activity)
	}

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
