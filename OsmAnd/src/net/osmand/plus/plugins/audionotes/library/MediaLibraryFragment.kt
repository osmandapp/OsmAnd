package net.osmand.plus.plugins.audionotes.library

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.graphics.drawable.ColorDrawable
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import net.osmand.plus.R
import net.osmand.plus.base.BaseOsmAndFragment
import net.osmand.plus.gallery.contract.IGalleryGridView
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.ui.GalleryGridAdapter
import net.osmand.plus.gallery.ui.GalleryGridBinder
import net.osmand.plus.gallery.ui.GalleryGridRecyclerView
import net.osmand.plus.gallery.ui.motion.GalleryMotion
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.MyPlacesActivity
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection

class MediaLibraryFragment : BaseOsmAndFragment(), IGalleryGridView {
	private lateinit var controller: MediaLibraryController
	private var recyclerView: GalleryGridRecyclerView? = null
	private var adapter: GalleryGridAdapter? = null
	private var binder: GalleryGridBinder? = null
	private var chips: MediaLibraryChips? = null
	private var chipsContainer: View? = null
	private var pendingLayoutState: Parcelable? = null
	private var toolbarSelectionMode = false
	private val toolbarBackground = ColorDrawable()
	private var toolbarColorAnimator: ValueAnimator? = null
	private val backCallback = object : OnBackPressedCallback(false) {
		override fun handleOnBackPressed() = controller.exitSelectionMode()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
		val plugin = requireNotNull(PluginsHelper.getPlugin(AudioVideoNotesPlugin::class.java))
		controller = MediaLibraryController(app, plugin)
		controller.restoreCollapsedGroups(savedInstanceState?.getStringArrayList("collapsed_groups").orEmpty())
		if (savedInstanceState?.getBoolean("selection_mode") == true) {
			controller.restoreSelection(savedInstanceState.getStringArrayList("selected_ids").orEmpty())
		}
		pendingLayoutState = savedInstanceState?.getParcelable("library_layout")
		app.dialogManager.register(controller.processId, controller)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		updateNightMode()
		val root = themedInflater.inflate(R.layout.media_library_fragment, container, false)
		chipsContainer = root.findViewById(R.id.chips_container)
		chips = MediaLibraryChips(root.findViewById(R.id.chips), controller).also { it.update() }
		val recycler = root.findViewById<GalleryGridRecyclerView>(R.id.recycler_view)
		recyclerView = recycler
		recycler.doOnLayout {
			recycler.post {
				if (recyclerView !== recycler) return@post
				val contentWidth = recycler.width - recycler.paddingLeft - recycler.paddingRight
				val mediaAdapter = controller.createAdapter(requireActivity(), contentWidth, nightMode)
				adapter = mediaAdapter
				mediaAdapter.displayMode = controller.getDisplayMode()
				binder = GalleryGridBinder(recycler, mediaAdapter, controller, sectionCards = true, nightMode = nightMode,
					resizableViewWidth = contentWidth).also { it.bind() }
				controller.attach(this)
				updateItems()
			}
		}
		return root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
	}

	override fun onResume() {
		super.onResume()
		updateToolbar()
	}

	override fun onPause() {
		if (activity?.isChangingConfigurations != true) controller.exitSelectionMode()
		super.onPause()
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		menu.clear()
		if (controller.isSelectionMode()) {
			inflater.inflate(R.menu.menu_selection_mode, menu)
			menu.findItem(R.id.select_all).setIcon(if (controller.isAllSelected())
				R.drawable.ic_action_deselect_all else R.drawable.ic_action_select_all)
		}
		(activity as? MyPlacesActivity)?.setToolbarVisibility(false)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (!controller.isSelectionMode()) return false
		return when (item.itemId) {
			android.R.id.home -> { controller.exitSelectionMode(); true }
			R.id.select_all -> { controller.toggleSelectAll(); true }
			R.id.more_button -> {
				requireActivity().findViewById<View>(R.id.more_button)?.let(controller::showSelectionMenu)
				true
			}
			else -> false
		}
	}

	override fun updateItems() {
		val items = controller.getGalleryItems()
		binder?.setItems(items, animated = true)
		if (items.isNotEmpty() && adapter != null) {
			pendingLayoutState?.let { recyclerView?.layoutManager?.onRestoreInstanceState(it) }
			pendingLayoutState = null
		}
		chipsContainer?.isVisible = items.isNotEmpty() && items.none { it is GalleryItem.NoMedia }
		chips?.update()
		recyclerView?.invalidateItemDecorations()
	}

	override fun updateDisplayMode() {
		binder?.morphLayout(controller.getGalleryItems())
		chips?.update()
	}

	override fun updateSections() = updateItems()

	override fun onSaveInstanceState(outState: Bundle) {
		outState.putParcelable("library_layout", recyclerView?.layoutManager?.onSaveInstanceState() ?: pendingLayoutState)
		outState.putStringArrayList("collapsed_groups", ArrayList(controller.getCollapsedGroups()))
		outState.putBoolean("selection_mode", controller.isSelectionMode())
		outState.putStringArrayList("selected_ids", ArrayList(controller.selectedIds()))
		super.onSaveInstanceState(outState)
	}
	override fun updateToolbar() {
		if (!isResumed) return
		val host = activity as? MyPlacesActivity ?: return
		val bar = host.supportActionBar ?: return
		val selected = controller.isSelectionMode()
		backCallback.isEnabled = selected
		val changed = toolbarSelectionMode != selected
		if (changed) {
			host.animateShowHideTabs(selected)
			toolbarSelectionMode = selected
		}
		bar.setHomeButtonEnabled(true)
		bar.setDisplayHomeAsUpEnabled(true)
		val barColor: Int
		if (selected) {
			bar.setHomeAsUpIndicator(R.drawable.ic_action_close)
			barColor = ColorUtilities.getToolbarActiveColor(app, nightMode)
			bar.title = controller.getSelectedCount().toString()
			AndroidUiHelper.setStatusBarColor(host, ColorUtilities.getColor(app, ColorUtilities.getStatusBarActiveColorId(nightMode)))
		} else {
			bar.setHomeAsUpIndicator(app.uiUtilities.getIcon(AndroidUtils.getNavigationIconResId(app),
				ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode)))
			barColor = ColorUtilities.getAppBarColor(app, nightMode)
			bar.setTitle(R.string.shared_string_my_places)
			host.updateStatusBarColor()
		}
		toolbarColorAnimator?.cancel()
		val background = toolbarBackground
		bar.setBackgroundDrawable(background)
		if (changed && GalleryMotion.animationsEnabled(app) && background.color != barColor) {
			toolbarColorAnimator = ValueAnimator.ofArgb(background.color, barColor).apply {
				duration = TOOLBAR_COLOR_DURATION_MS
				interpolator = GalleryMotion.CURVE
				addUpdateListener { background.color = it.animatedValue as Int }
				start()
			}
		} else {
			background.color = barColor
		}
		host.invalidateOptionsMenu()
	}
	override fun getMapActivity(): net.osmand.plus.activities.MapActivity? = activity as? net.osmand.plus.activities.MapActivity
	override fun updateSelection() {
		adapter?.selectionMode = controller.isSelectionMode()
		adapter?.notifySelectionChanged()
	}
	override fun isPortrait(): Boolean = AndroidUiHelper.isOrientationPortrait(requireContext())

	override fun getInsetTargets(): InsetTargetsCollection = InsetTargetsCollection().apply {
		add(InsetTarget.createScrollable(R.id.recycler_view).clipToPadding(false))
		add(InsetTarget.createHorizontalLandscape(R.id.chips_container))
	}

	override fun onDestroyView() {
		controller.detach()
		toolbarColorAnimator?.cancel()
		toolbarColorAnimator = null
		binder?.release()
		recyclerView?.adapter = null
		recyclerView = null
		adapter = null
		binder = null
		chips = null
		chipsContainer = null
		super.onDestroyView()
	}

	override fun onDestroy() {
		controller.onScreenDestroyed(activity)
		super.onDestroy()
	}

	companion object {
		private const val TOOLBAR_COLOR_DURATION_MS = 200L
	}
}
