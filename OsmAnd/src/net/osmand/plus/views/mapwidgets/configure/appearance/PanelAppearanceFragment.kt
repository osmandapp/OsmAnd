package net.osmand.plus.views.mapwidgets.configure.appearance

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.ImageViewCompat
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseOsmAndFragment
import net.osmand.plus.inapp.InAppPurchaseUtils
import net.osmand.plus.palette.view.PaletteElements
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.enums.PanelBackgroundMode
import net.osmand.plus.settings.enums.PanelIconMode
import net.osmand.plus.settings.enums.PanelSizeMode
import net.osmand.plus.settings.enums.PanelTextColorMode
import net.osmand.plus.settings.enums.ScreenLayoutMode
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.appearance.PanelColorTarget
import net.osmand.plus.views.mapwidgets.appearance.ResolvedPanelAppearance
import net.osmand.plus.widgets.popup.PopUpMenu
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem

class PanelAppearanceFragment : BaseOsmAndFragment() {

	companion object {
		private const val PANEL_KEY = "panel_key"

		fun newInstance(panel: WidgetsPanel): PanelAppearanceFragment {
			val fragment = PanelAppearanceFragment()
			val args = Bundle()
			args.putString(PANEL_KEY, panel.name)
			fragment.arguments = args
			return fragment
		}
	}

	lateinit var panel: WidgetsPanel
		private set

	private lateinit var appearanceSettings: PanelAppearanceSettings
	private lateinit var resolvedAppearance: ResolvedPanelAppearance
	private var heightButton: View? = null
	private var iconButton: View? = null
	private var textColorButton: View? = null
	private var secondaryTextColorButton: View? = null
	private var backgroundColorButton: View? = null
	private var paletteElements: PaletteElements? = null
	private var paletteElementsNightMode: Boolean? = null

	private val layoutMode: ScreenLayoutMode?
		get() = parentAppearanceFragment?.layoutMode

	private val selectedAppMode: ApplicationMode
		get() = parentAppearanceFragment?.selectedAppMode ?: settings.applicationMode

	private val parentAppearanceFragment: WidgetsAppearanceFragment?
		get() = parentFragment as? WidgetsAppearanceFragment

	override fun isUsedOnMap(): Boolean = true

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val panelName = arguments?.getString(PANEL_KEY) ?: WidgetsPanel.LEFT.name
		panel = WidgetsPanel.valueOf(panelName)
		appearanceSettings = app.panelAppearanceSettingsManager[panel]
	}

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = super.getInsetTargets()
		collection.removeType(InsetTarget.Type.ROOT_INSET)
		return collection
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		updateNightMode()
		val view = inflate(R.layout.fragment_panel_appearance, container, false)
		heightButton = view.findViewById(R.id.height_button)
		iconButton = view.findViewById(R.id.icon_button)
		textColorButton = view.findViewById(R.id.text_color_button)
		secondaryTextColorButton = view.findViewById(R.id.secondary_text_color_button)
		backgroundColorButton = view.findViewById(R.id.background_color_button)
		view.findViewById<TextView>(R.id.parameters_description).text = getString(
			R.string.panel_appearance_original_description,
			getString(R.string.shared_string_original)
		)
		updateContent()
		return view
	}

	override fun onResume() {
		super.onResume()
		if (view != null && ::appearanceSettings.isInitialized) {
			updateContent()
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		heightButton = null
		iconButton = null
		textColorButton = null
		secondaryTextColorButton = null
		backgroundColorButton = null
		paletteElements = null
		paletteElementsNightMode = null
	}

	fun updateContent() {
		resolvedAppearance = app.panelAppearanceSettingsManager.resolveForProfile(
			panel = panel,
			appMode = selectedAppMode,
			layoutMode = layoutMode,
			nightMode = nightMode,
			boldText = false,
			density = resources.displayMetrics.density
		)
		setupHeightButton()
		setupIconButton()
		setupTextColorButton(textColorButton, R.string.text_color, false)
		setupTextColorButton(secondaryTextColorButton, R.string.secondary_text_color, true)
		setupBackgroundColorButton()
	}

	private fun setupHeightButton() {
		val button = heightButton ?: return
		val sizeMode = appearanceSettings.getSizeModePref(layoutMode).getModeValue(selectedAppMode)

		val titleId = if (panel.isPanelVertical) R.string.row_height else R.string.widget_height
		button.findViewById<TextView>(R.id.title).setText(titleId)
		button.findViewById<TextView>(R.id.value).setText(sizeMode.titleId)
		setupRowIcon(button, if (sizeMode.iconId != 0) sizeMode.iconId else R.drawable.ic_action_item_size_m)
		button.setOnClickListener { showHeightMenu(button.findViewById(R.id.selector)) }
	}

	private fun setupIconButton() {
		val button = iconButton ?: return
		val iconMode = appearanceSettings.getIconModePref(layoutMode).getModeValue(selectedAppMode)

		button.findViewById<TextView>(R.id.title).setText(R.string.shared_string_icon)
		button.findViewById<TextView>(R.id.value).setText(iconMode.titleId)
		setupRowIcon(button, if (iconMode.iconId != 0) iconMode.iconId else R.drawable.ic_action_view)
		button.setOnClickListener { showIconMenu(button.findViewById(R.id.selector)) }
	}

	private fun setupTextColorButton(button: View?, @StringRes titleId: Int, secondary: Boolean) {
		button ?: return
		val pref = if (secondary) appearanceSettings.getSecondaryTextColorModePref(layoutMode)
		else appearanceSettings.getTextColorModePref(layoutMode)
		val mode = pref.getModeValue(selectedAppMode)

		button.findViewById<TextView>(R.id.title).setText(titleId)
		button.findViewById<TextView>(R.id.value).setText(mode.titleId)
		setupColorPreview(
			button,
			resolvedAppearance.background.color,
			if (secondary) resolvedAppearance.secondaryTextColor else resolvedAppearance.primaryTextColor
		)
		button.setOnClickListener {
			showTextColorMenu(button.findViewById(R.id.selector), secondary)
		}
	}

	private fun setupBackgroundColorButton() {
		val button = backgroundColorButton ?: return
		val mode = resolvedAppearance.background.mode

		button.findViewById<TextView>(R.id.title).setText(R.string.background_color)
		button.findViewById<TextView>(R.id.value).setText(mode.titleId)
		setupColorPreview(button, resolvedAppearance.background.color, null)
		button.setOnClickListener {
			showBackgroundColorMenu(button.findViewById(R.id.selector))
		}
	}

	private fun setupRowIcon(button: View, @DrawableRes iconId: Int) {
		setLeadingIconMargin(button, R.dimen.text_margin_small)
		val background = button.findViewById<ImageView>(R.id.background)
		val outline = button.findViewById<ImageView>(R.id.outline)
		val icon = button.findViewById<ImageView>(R.id.icon)
		background.visibility = View.INVISIBLE
		outline.visibility = View.INVISIBLE
		setTintedIcon(icon, iconId, ColorUtilities.getDefaultIconColor(app, nightMode))
		icon.visibility = View.VISIBLE
	}

	private fun setupColorPreview(button: View, backgroundColor: Int, textColor: Int?) {
		setLeadingIconMargin(button, R.dimen.content_padding_small)
		val preview = button.findViewById<View>(R.id.appearance_icon)
		getPaletteElements(button).updateColorItemView(preview, backgroundColor, false)

		val background = preview.findViewById<ImageView>(R.id.background)
		val icon = preview.findViewById<ImageView>(R.id.icon)
		background.visibility = View.VISIBLE
		val contourColor = ColorUtilities.getDividerColor(app, nightMode)
		val contour = GradientDrawable().apply {
			shape = GradientDrawable.OVAL
			setColor(Color.TRANSPARENT)
			setStroke(AndroidUtils.dpToPx(app, 1F), contourColor)
		}
		background.setImageDrawable(UiUtilities.getLayeredIcon(background.drawable, contour))
		if (textColor != null) {
			setTintedIcon(icon, R.drawable.ic_action_text_preview, textColor)
			icon.visibility = View.VISIBLE
		} else {
			icon.visibility = View.GONE
		}
	}

	private fun setLeadingIconMargin(button: View, @DimenRes marginRes: Int) {
		val preview = button.findViewById<View>(R.id.appearance_icon)
		val params = preview.layoutParams as? ViewGroup.MarginLayoutParams ?: return
		val marginStart = resources.getDimensionPixelSize(marginRes)
		if (params.marginStart != marginStart) {
			params.marginStart = marginStart
			preview.layoutParams = params
		}
	}

	private fun setTintedIcon(icon: ImageView, @DrawableRes iconId: Int, @ColorInt color: Int) {
		icon.setImageDrawable(AppCompatResources.getDrawable(icon.context, iconId))
		ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(color))
	}

	private fun getPaletteElements(view: View): PaletteElements {
		if (paletteElements == null || paletteElementsNightMode != nightMode) {
			paletteElements = PaletteElements(view.context, nightMode)
			paletteElementsNightMode = nightMode
		}
		return requireNotNull(paletteElements)
	}

	private fun showHeightMenu(anchorView: View) {
		val pref = appearanceSettings.getSizeModePref(layoutMode)
		val iconColor = ColorUtilities.getDefaultIconColor(app, nightMode)
		val items = PanelSizeMode.entries.map { mode ->
			PopUpMenuItem.Builder(app)
				.setTitleId(mode.titleId)
				.setIcon(if (mode.iconId != 0) getPaintedIcon(mode.iconId, iconColor) else null)
				.showTopDivider(mode == PanelSizeMode.SMALL)
				.setOnClickListener {
					pref.setModeValue(selectedAppMode, mode)
					onAppearanceChanged()
				}
				.create()
		}
		showMenu(anchorView, items)
	}

	private fun showIconMenu(anchorView: View) {
		val pref = appearanceSettings.getIconModePref(layoutMode)
		val iconColor = ColorUtilities.getDefaultIconColor(app, nightMode)
		val items = listOf(PanelIconMode.ORIGINAL, PanelIconMode.OFF, PanelIconMode.ON).map { mode ->
			PopUpMenuItem.Builder(app)
				.setTitleId(mode.titleId)
				.setIcon(if (mode.iconId != 0) getPaintedIcon(mode.iconId, iconColor) else null)
				.showTopDivider(mode == PanelIconMode.OFF)
				.setOnClickListener {
					pref.setModeValue(selectedAppMode, mode)
					onAppearanceChanged()
				}
				.create()
		}
		showMenu(anchorView, items)
	}

	private fun showTextColorMenu(anchorView: View, secondary: Boolean) {
		val pref = if (secondary) appearanceSettings.getSecondaryTextColorModePref(layoutMode)
		else appearanceSettings.getTextColorModePref(layoutMode)
		val items = PanelTextColorMode.entries.map { mode ->
			PopUpMenuItem.Builder(app)
				.setTitleId(mode.titleId)
				.showTopDivider(mode == PanelTextColorMode.AUTOMATIC)
				.setOnClickListener {
					if (mode == PanelTextColorMode.CUSTOM) {
						openColorPalette(if (secondary) PanelColorTarget.SECONDARY_TEXT else PanelColorTarget.TEXT)
					} else {
						pref.setModeValue(selectedAppMode, mode)
						onAppearanceChanged()
					}
				}
				.create()
		}
		showMenu(anchorView, items)
	}

	private fun showBackgroundColorMenu(anchorView: View) {
		val pref = appearanceSettings.getBackgroundModePref(layoutMode)
		val customAvailable = InAppPurchaseUtils.isCustomWidgetBackgroundColorAvailable(app)
		val items = PanelBackgroundMode.entries.map { mode ->
			val builder = PopUpMenuItem.Builder(app)
				.setTitleId(mode.titleId)
				.showTopDivider(mode == PanelBackgroundMode.TRANSPARENT)
			if (mode == PanelBackgroundMode.CUSTOM && !customAvailable) {
				builder.setTrailingBadge(
					AppCompatResources.getDrawable(app, R.drawable.ic_action_osmand_maps_plus),
					app.getString(R.string.shared_string_get),
					ColorUtilities.getColor(app, if (nightMode) {
						R.color.maps_plus_badge_text_dark
					} else {
						R.color.maps_plus_badge_text_light
					})
				)
			}
			builder.setOnClickListener {
				if (mode == PanelBackgroundMode.CUSTOM) {
					openColorPalette(PanelColorTarget.BACKGROUND)
				} else {
					pref.setModeValue(selectedAppMode, mode)
					onAppearanceChanged()
				}
			}.create()
		}
		showMenu(anchorView, items)
	}

	private fun openColorPalette(target: PanelColorTarget) {
		val mapActivity = activity as? MapActivity ?: return
		PanelColorController.showDialog(mapActivity, panel, target, selectedAppMode, layoutMode)
	}

	private fun showMenu(anchorView: View, items: List<PopUpMenuItem>) {
		val displayData = PopUpMenuDisplayData()
		displayData.anchorView = anchorView
		displayData.menuItems = items
		displayData.nightMode = nightMode
		PopUpMenu.show(displayData)
	}

	private fun onAppearanceChanged() {
		parentAppearanceFragment?.onAppearanceChanged() ?: updateContent()
	}
}