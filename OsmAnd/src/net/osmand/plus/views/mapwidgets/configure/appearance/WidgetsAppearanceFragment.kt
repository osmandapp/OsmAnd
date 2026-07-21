package net.osmand.plus.views.mapwidgets.configure.appearance

import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseFullScreenFragment
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.enums.ScreenLayoutMode
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.utils.InsetsUtils
import net.osmand.plus.views.controls.MapHudLayout
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.configure.dialogs.ConfigureScreenFragment.SCREEN_LAYOUT_MODE
import net.osmand.plus.widgets.TextViewEx
import net.osmand.plus.widgets.popup.PopUpMenu
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem

class WidgetsAppearanceFragment : BaseFullScreenFragment(), CopyAppModePrefsListener,
	InAppPurchaseListener {

	companion object {
		val TAG: String = WidgetsAppearanceFragment::class.java.simpleName

		private const val APP_MODE_KEY = "app_mode_key"
		private const val SELECTED_PANEL_KEY = "selected_panel_key"

		@JvmStatic
		fun showInstance(activity: FragmentActivity, appMode: ApplicationMode,
		                 layoutMode: ScreenLayoutMode?) {
			val fragmentManager = activity.supportFragmentManager
			if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
				val fragment = WidgetsAppearanceFragment()
				val args = Bundle()
				args.putString(APP_MODE_KEY, appMode.stringKey)
				if (layoutMode != null) {
					args.putSerializable(SCREEN_LAYOUT_MODE, layoutMode)
				}
				fragment.arguments = args
				fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss()
			}
		}
	}

	var selectedPanel: WidgetsPanel = WidgetsPanel.LEFT
		private set
	lateinit var selectedAppMode: ApplicationMode
		private set
	var layoutMode: ScreenLayoutMode? = null
		private set

	private lateinit var toolbar: Toolbar
	private lateinit var tabLayout: TabLayout
	private lateinit var viewPager: ViewPager2
	private lateinit var toolbarTitle: TextViewEx
	private var previewActive = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val appModeKey = savedInstanceState?.getString(APP_MODE_KEY)
			?: arguments?.getString(APP_MODE_KEY)
		selectedAppMode = ApplicationMode.valueOfStringKey(appModeKey, settings.applicationMode)
			?: settings.applicationMode

		val savedPanel = savedInstanceState?.getString(SELECTED_PANEL_KEY)
		if (savedPanel != null) {
			selectedPanel = WidgetsPanel.valueOf(savedPanel)
		}
		layoutMode = AndroidUtils.getSerializable(
			savedInstanceState ?: arguments ?: Bundle(), SCREEN_LAYOUT_MODE, ScreenLayoutMode::class.java)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		updateNightMode()
		val view = inflate(R.layout.fragment_widgets_appearance, container, false)
		if (!InsetsUtils.isEdgeToEdgeSupported()) {
			AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view)
		}
		toolbar = view.findViewById(R.id.toolbar)
		tabLayout = view.findViewById(R.id.tab_layout)
		viewPager = view.findViewById(R.id.view_pager)
		toolbarTitle = view.findViewById(R.id.toolbar_title)

		setupToolbar(view)
		setupTabLayout()

		view.findViewById<View>(R.id.map_preview_area).addOnLayoutChangeListener {
			_, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
			if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
				refreshPreview(false)
			}
		}
		return view
	}

	override fun isUsedOnMap(): Boolean = true

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = super.getInsetTargets()
		collection.removeType(InsetTarget.Type.ROOT_INSET)
		collection.replace(InsetTarget.createCollapsingAppBar(R.id.appbar))
		return collection
	}

	override fun onResume() {
		super.onResume()
		val mapActivity = activity as? MapActivity ?: return
		previewActive = true
		mapActivity.disableDrawer()
		mapActivity.hideTopToolbar(TopToolbarControllerType.SUGGEST_MAP)
		view?.findViewById<View>(R.id.map_preview_area)?.post {
			refreshPreview(true)
		}
	}

	override fun onPause() {
		previewActive = false
		super.onPause()
		val mapActivity = activity as? MapActivity ?: return
		mapActivity.enableDrawer()
		mapActivity.findViewById<MapHudLayout>(R.id.map_hud_layout)?.clearExternalVisibleArea()
		refreshMapWidgetPanels()
		refreshMapControlButtons()
		mapActivity.refreshMap()
	}

	private fun updateMapHudVisibleArea(): Boolean {
		val mapActivity = activity as? MapActivity ?: return false
		val previewArea = view?.findViewById<View>(R.id.map_preview_area) ?: return false
		if (previewArea.width <= 0 || previewArea.height <= 0) {
			return false
		}
		val visibleArea = Rect()
		if (!previewArea.getGlobalVisibleRect(visibleArea)) {
			return false
		}
		val mapHudLayout = mapActivity.findViewById<MapHudLayout>(R.id.map_hud_layout) ?: return false
		return mapHudLayout.setExternalVisibleArea(visibleArea)
	}

	private fun refreshPreview(force: Boolean) {
		if (!previewActive) {
			return
		}
		if (updateMapHudVisibleArea() || force) {
			refreshMapWidgetPanels()
			refreshMapControlButtons()
			(activity as? MapActivity)?.refreshMap()
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putString(APP_MODE_KEY, selectedAppMode.stringKey)
		outState.putString(SELECTED_PANEL_KEY, selectedPanel.name)
		layoutMode?.let { outState.putSerializable(SCREEN_LAYOUT_MODE, it) }
	}

	private fun setupToolbar(view: View) {
		val backButton = toolbar.findViewById<ImageButton>(R.id.back_button)
		backButton.setImageDrawable(getContentIcon(AndroidUtils.getNavigationIconResId(app)))
		backButton.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }

		val resetButton = toolbar.findViewById<AppCompatImageButton>(R.id.reset_button)
		resetButton.setOnClickListener { resetSelectedPanel() }

		val actionsButton = view.findViewById<AppCompatImageButton>(R.id.actions_button)
		actionsButton.setOnClickListener { showCopyMenu(actionsButton) }

		updateToolbarTitle()
	}

	private fun updateToolbarTitle() {
		toolbarTitle.setText(selectedPanel.getTitleId(AndroidUtils.isLayoutRtl(app)))
	}

	private fun setupTabLayout() {
		viewPager.adapter = PanelsTabAdapter(this)
		viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				selectedPanel = WidgetsPanel.entries[position]
				updateToolbarTitle()
				if (previewActive) {
					refreshPreview(true)
				} else {
					refreshMapWidgetPanels()
				}
			}
		})
		TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

		val profileColor = selectedAppMode.getProfileColor(nightMode)
		val defaultIconColor = ColorUtilities.getDefaultIconColor(app, nightMode)
		tabLayout.setSelectedTabIndicatorColor(profileColor)
		tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab) = setupTabIconColor(tab, profileColor)
			override fun onTabUnselected(tab: TabLayout.Tab) = setupTabIconColor(tab, defaultIconColor)
			override fun onTabReselected(tab: TabLayout.Tab) = Unit
		})

		val panels = WidgetsPanel.entries
		val rtl = AndroidUtils.isLayoutRtl(app)
		for (i in 0 until tabLayout.tabCount) {
			tabLayout.getTabAt(i)?.apply {
				tag = panels[i]
				setIcon(panels[i].getIconId(rtl, layoutMode))
			}
		}

		val position = panels.indexOf(selectedPanel)
		viewPager.setCurrentItem(position, false)
		if (position == 0) {
			setupTabIconColor(tabLayout.getTabAt(0), profileColor)
		}
	}

	private fun setupTabIconColor(tab: TabLayout.Tab?, color: Int) {
		tab?.icon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
	}

	private fun showCopyMenu(anchorView: View) {
		val items = mutableListOf<PopUpMenuItem>()
		items.add(PopUpMenuItem.Builder(app)
			.setTitle(getString(R.string.copy_from))
			.setTitleBold(true)
			.create())

		items.add(PopUpMenuItem.Builder(app)
			.setTitle(getString(R.string.another_profile))
			.setIcon(getContentIcon(R.drawable.ic_action_copy))
			.setOnClickListener {
				activity?.let {
					SelectCopyAppModeBottomSheet.showInstance(
						it.supportFragmentManager, this, selectedAppMode)
				}
			}
			.create())

		val rtl = AndroidUtils.isLayoutRtl(app)
		var first = true
		for (panel in WidgetsPanel.entries) {
			if (panel == selectedPanel) continue
			val fromPanel = panel
			items.add(PopUpMenuItem.Builder(app)
				.setTitle(getString(panel.getTitleId(rtl)))
				.setIcon(getContentIcon(panel.getIconId(rtl, layoutMode)))
				.showTopDivider(first)
				.setOnClickListener { copyFromPanel(fromPanel) }
				.create())
			first = false
		}

		val displayData = PopUpMenuDisplayData()
		displayData.anchorView = anchorView
		displayData.menuItems = items
		displayData.nightMode = nightMode
		displayData.layoutId = R.layout.popup_menu_item_full_divider
		PopUpMenu.show(displayData)
	}

	override fun copyAppModePrefs(appMode: ApplicationMode) {
		app.panelAppearanceSettingsManager[selectedPanel]
			.copyFromProfile(appMode, selectedAppMode, layoutMode)
		onAppearanceChanged()
	}

	private fun copyFromPanel(fromPanel: WidgetsPanel) {
		app.panelAppearanceSettingsManager[selectedPanel]
			.copyFromPanel(fromPanel, selectedAppMode, layoutMode)
		onAppearanceChanged()
	}

	private fun resetSelectedPanel() {
		app.panelAppearanceSettingsManager[selectedPanel]
			.resetToDefault(selectedAppMode, layoutMode)
		onAppearanceChanged()
	}

	fun onAppearanceChanged() {
		refreshMapWidgetPanels()
		for (fragment in childFragmentManager.fragments) {
			if (fragment is PanelAppearanceFragment) {
				fragment.updateContent()
			}
		}
	}

	override fun onGetItems() {
		onPurchaseStateChanged()
	}

	override fun onItemPurchased(sku: String?, active: Boolean) {
		onPurchaseStateChanged()
	}

	private fun onPurchaseStateChanged() {
		app.panelAppearanceSettingsManager.refreshPurchaseState()
		onAppearanceChanged()
	}

	private fun refreshMapWidgetPanels() {
		app.osmandMap.mapLayers.mapInfoLayer?.refreshWidgetPanels()
	}

	private fun refreshMapControlButtons() {
		val mapLayers = app.osmandMap.mapLayers
		mapLayers.mapQuickActionLayer?.actionButtons?.forEach { it.update() }
		mapLayers.mapControlsLayer?.refreshButtons()
	}

	override fun getStatusBarColorId(): Int {
		return ColorUtilities.getListBgColorId(nightMode)
	}

	override fun getContentStatusBarNightMode(): Boolean = nightMode

	private class PanelsTabAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

		override fun createFragment(position: Int): Fragment {
			return PanelAppearanceFragment.newInstance(WidgetsPanel.entries[position])
		}

		override fun getItemCount(): Int = WidgetsPanel.entries.size
	}
}
