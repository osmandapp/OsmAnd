package net.osmand.plus.plugins.audionotes.library

import android.animation.ValueAnimator
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseOsmAndFragment
import net.osmand.plus.gallery.contract.IGalleryGridView
import net.osmand.plus.gallery.model.GalleryDisplayMode
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.model.GallerySortMode
import net.osmand.plus.gallery.ui.GalleryGridBinder
import net.osmand.plus.gallery.ui.motion.GalleryMotion
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.myplaces.MyPlacesActivity
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin
import net.osmand.plus.search.dialogs.ChipsLayout
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.widgets.popup.OsmAndDropdownMenuSelectionStyle

class MediaLibraryFragment : BaseOsmAndFragment(), IGalleryGridView {
	private lateinit var controller: MediaLibraryController
	private var binder: GalleryGridBinder? = null
	private var chips: ChipsLayout? = null
	private var chipsContainer: View? = null
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
		app.dialogManager.register(controller.processId, controller)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		updateNightMode()
		val root = themedInflater.inflate(R.layout.media_library_fragment, container, false)
		chipsContainer = root.findViewById(R.id.chips_container)
		chips = root.findViewById<ChipsLayout>(R.id.chips).also { setupChips(it) }
		updateChips()
		binder = GalleryGridBinder(root.findViewById(R.id.recycler_view), controller, requireActivity(), nightMode, sectionCards = true)
			.also { it.pendingLayoutState = savedInstanceState?.getParcelable("library_layout") }
		controller.attach(this)
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
		val binder = binder ?: return
		binder.updateItems()
		val items = binder.items
		chipsContainer?.isVisible = items.isNotEmpty() && items.none { it is GalleryItem.NoMedia }
		updateChips()
	}

	override fun updateDisplayMode() {
		binder?.updateDisplayMode()
		updateChips()
	}

	override fun updateSelection() {
		binder?.updateSelection()
	}

	private fun setupChips(view: ChipsLayout) {
		view.setOnChipClickListener {
			when (it) {
				GRID_CHIP -> controller.setDisplayMode(if (controller.getDisplayMode() == GalleryDisplayMode.GRID) GalleryDisplayMode.LIST else GalleryDisplayMode.GRID)
				GROUP_CHIP -> controller.toggleGrouping()
			}
		}
		view.setOnDropdownItemClickListener { _, id -> GallerySortMode.entries.getOrNull(id)?.let(controller::onSortModeSelected) }
	}

	private fun updateChips() {
		val view = chips ?: return
		val mode = controller.sortMode
		fun chip(id: String, icon: Int, title: Int, selected: Boolean) = ChipsLayout.ChipData(id,
			if (selected) 0 else icon, getString(title), selected, true, true, false,
			ChipsLayout.TextColorStyle.PRIMARY, ChipsLayout.IconColorStyle.DEFAULT)
		view.updateContent(listOf(
			ChipsLayout.DropDownChipData(SORT_CHIP, mode.iconId, getString(mode.titleId), false, true, true,
				ChipsLayout.TextColorStyle.PRIMARY, ChipsLayout.IconColorStyle.ACTIVE,
				selectionStyle = OsmAndDropdownMenuSelectionStyle.CHECKMARK,
				dropdownItems = GallerySortMode.entries.mapIndexed { index, item ->
					ChipsLayout.DropdownItem(index, item.iconId, getString(item.titleId), selected = item == mode,
						showDividerBelow = GallerySortMode.entries.getOrNull(index + 1)?.let { it.group != item.group } == true)
				}),
			chip(GRID_CHIP, MediaLibraryIcons.GRID, R.string.shared_string_grid, controller.getDisplayMode() == GalleryDisplayMode.GRID),
			chip(GROUP_CHIP, MediaLibraryIcons.GROUP_BY, R.string.shared_string_group, controller.isGrouped())
		))
	}

	override fun onSaveInstanceState(outState: Bundle) {
		outState.putParcelable("library_layout", binder?.saveLayoutState())
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
		toolbarColorAnimator = GalleryMotion.recolor(app, background.color, barColor, animate = changed && background.color != 0) {
			background.color = it
		}
		host.invalidateOptionsMenu()
	}

	override fun getMapActivity(): MapActivity? = activity as? MapActivity
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
		private const val SORT_CHIP = "sort"
		private const val GRID_CHIP = "grid"
		private const val GROUP_CHIP = "group"
	}
}
