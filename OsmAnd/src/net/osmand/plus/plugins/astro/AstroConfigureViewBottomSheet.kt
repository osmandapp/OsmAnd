package net.osmand.plus.plugins.astro

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import net.osmand.plus.R
import net.osmand.plus.base.BaseMaterialBottomSheetDialogFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.utils.InsetsUtils
import net.osmand.plus.utils.UiUtilities

class AstroConfigureViewBottomSheet :
    BaseMaterialBottomSheetDialogFragment() {

    private lateinit var mainView: View
    private var behavior: BottomSheetBehavior<FrameLayout>? = null


    companion object {
        val TAG: String = AstroConfigureViewBottomSheet::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mainView = inflater.inflate(R.layout.astro_config_view, container, false)

        bindMapActions(mainView)
        bindVisibleObjects(mainView)
        bindSwitchRows(mainView)

        mainView.findViewById<View>(R.id.closeBtn)?.setOnClickListener { dismiss() }

        return mainView
    }

    override fun getInsetTargets(): InsetTargetsCollection? {
        val collection = super.getInsetTargets()
        collection.replace(InsetTarget.createScrollable(mainView).landscapeSides(InsetsUtils.InsetSide.BOTTOM))
        collection.removeType(InsetTarget.Type.ROOT_INSET)
        return collection
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet =
            dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return

        val b = BottomSheetBehavior.from(bottomSheet).apply {
            skipCollapsed = false
        }
        behavior = b

        val grid = mainView.findViewById<View>(R.id.visible_objects_grid_content)
        val extra = resources.getDimensionPixelSize(R.dimen.content_padding)

        grid.doOnPreDraw {
            val bottomInset =
                lastRootInsets?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0

            val peek = grid.bottom + extra + bottomInset

            b.peekHeight = peek
            b.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun bindMapActions(root: View) {
        val activeColor = ColorUtilities.getActiveIconColorId(nightMode)

        bindToggleMapActionCard(
            card = root.findViewById(R.id.button_3d),
            isChecked = { !requireStarMap().starView.is2DMode },
            titleResEnabled = R.string.map_3d,
            titleResDisabled = R.string.map_2d,
            drawableEnabled = uiUtilities.getIcon(R.drawable.ic_action_globe_view, activeColor),
            drawableDisabled = uiUtilities.getIcon(
                R.drawable.ic_action_celestial_path,
                activeColor
            ),
            toggle = { enabled3d ->
				requireStarMap().starView.is2DMode = !enabled3d
            }
        )

        bindToggleMapActionCard(
            card = root.findViewById(R.id.button_map),
            isChecked = { requireStarMap().regularMapVisible },
            titleResEnabled = R.string.shared_string_map,
            drawableEnabled = uiUtilities.getIcon(R.drawable.ic_map, activeColor),
            drawableDisabled = uiUtilities.getIcon(R.drawable.ic_action_map_outlined, activeColor),
            toggle = { regularMap ->
                requireStarMap().setRegularMapVisibility(regularMap)
            }
        )

        val redFilterEnableDrawable = UiUtilities.getLayeredIcon(
            uiUtilities.getIcon(R.drawable.ic_action_red_filter_base_on, activeColor),
            uiUtilities.getIcon(
                R.drawable.ic_action_red_filter_overlay_on,
                ColorUtilities.getWarningColorId(nightMode)
            )
        )
        bindToggleMapActionCard(
            card = root.findViewById(R.id.button_red_filter),
            isChecked = { true },
            titleResEnabled = R.string.red_filter,
            drawableEnabled = redFilterEnableDrawable,
            drawableDisabled = uiUtilities.getIcon(
                R.drawable.ic_action_red_filter_off,
                activeColor
            ),
            toggle = {

            }
        )
    }

    private fun bindVisibleObjects(root: View) {
        bindToggleAstroCard(
            card = root.findViewById(R.id.button_solar_system),
            isChecked = { c -> c.showSun && c.showMoon && c.showPlanets },
            titleRes = R.string.astro_solar_system,
            iconRes = R.drawable.ic_action_planet_outlined,
            toggle = { c ->
                val allOn = c.showSun && c.showMoon && c.showPlanets
                val newValue = !allOn
                c.copy(showSun = newValue, showMoon = newValue, showPlanets = newValue)
            }
        )

        bindToggleAstroCard(
            card = root.findViewById(R.id.button_constellations),
            isChecked = { it.showConstellations },
            titleRes = R.string.astro_constellations,
            iconRes = R.drawable.ic_action_constellations,
            toggle = { it.copy(showConstellations = !it.showConstellations) }
        )

        bindToggleAstroCard(
            card = root.findViewById(R.id.button_stars),
            isChecked = { it.showStars },
            titleRes = R.string.astro_stars,
            iconRes = R.drawable.ic_action_stars,
            toggle = { it.copy(showStars = !it.showStars) }
        )

        bindToggleAstroCard(
            card = root.findViewById(R.id.button_nebulas),
            isChecked = { it.showNebulae },
            titleRes = R.string.astro_nebulas,
            iconRes = R.drawable.ic_action_nebulas,
            toggle = { it.copy(showNebulae = !it.showNebulae) }
        )

        bindToggleAstroCard(
            card = root.findViewById(R.id.button_star_clusters),
            isChecked = { c -> c.showOpenClusters && c.showGlobularClusters },
            titleRes = R.string.astro_star_clusters,
            iconRes = R.drawable.ic_action_star_clusters,
            toggle = { c ->
                val allOn = c.showOpenClusters && c.showGlobularClusters
                val newValue = !allOn
                c.copy(
                    showOpenClusters = newValue,
                    showGlobularClusters = newValue,
                )
            }
        )

        bindToggleAstroCard(
            card = root.findViewById(R.id.button_deep_sky),
            isChecked = { c -> c.showGalaxies && c.showBlackHoles && c.showGalaxyClusters },
            titleRes = R.string.astro_deep_sky,
            iconRes = R.drawable.ic_action_galaxy,
            toggle = { c ->
                val allOn = c.showGalaxies && c.showBlackHoles && c.showGalaxyClusters
                val newValue = !allOn
                c.copy(
                    showGalaxies = newValue,
                    showBlackHoles = newValue,
                    showGalaxyClusters = newValue
                )
            }
        )
    }

    private fun renderToggleCard(
        card: MaterialCardView,
        checked: Boolean,
        drawableEnabled: Drawable,
        drawableDisabled: Drawable = drawableEnabled,
        @StringRes titleResEnabled: Int,
        @StringRes titleResDisabled: Int = titleResEnabled
    ) {
        val icon = card.findViewById<ImageView>(R.id.icon)
        val title = card.findViewById<TextView>(R.id.title)

        card.isChecked = checked

        title.setText(if (checked) titleResEnabled else titleResDisabled)
        icon.setImageDrawable(if (checked) drawableEnabled else drawableDisabled)
    }

    private fun bindToggleMapActionCard(
        card: MaterialCardView,
        drawableEnabled: Drawable,
        drawableDisabled: Drawable = drawableEnabled,
        @StringRes titleResEnabled: Int,
        @StringRes titleResDisabled: Int = titleResEnabled,
        isChecked: () -> Boolean,
        toggle: (Boolean) -> Unit
    ) {
        fun render() {
            renderToggleCard(
                card,
                isChecked(),
                drawableEnabled,
                drawableDisabled,
                titleResEnabled,
                titleResDisabled
            )
        }

        render()

        card.setOnClickListener {
            val newValue = !isChecked()
            toggle(newValue)
            render()
        }
    }

    private fun bindToggleAstroCard(
        card: MaterialCardView,
        @DrawableRes iconRes: Int,
        @StringRes titleRes: Int,
        isChecked: (StarWatcherSettings.StarMapConfig) -> Boolean,
        toggle: (StarWatcherSettings.StarMapConfig) -> StarWatcherSettings.StarMapConfig
    ) {
        fun render(c: StarWatcherSettings.StarMapConfig) {
            val drawable = uiUtilities.getIcon(
                iconRes,
                ColorUtilities.getActiveIconColorId(nightMode)
            )

            renderToggleCard(
                card,
                isChecked(c),
                drawableEnabled = drawable,
                titleResEnabled = titleRes,
            )
        }

        render(requireConfig())

        card.setOnClickListener {
            val newConfig = toggle(requireConfig())
            applyConfigChange(newConfig)
            render(newConfig)
        }
    }

    private fun bindSwitchRows(root: View) {
        val current = requireConfig()

        val personalContainer = root.findViewById<ViewGroup>(R.id.personal_content)
        val renderingContainer = root.findViewById<ViewGroup>(R.id.rendering_content)

        addSwitchRow(
            parent = personalContainer,
            iconRes = R.drawable.ic_action_star_clusters,
            titleRes = R.string.astro_directions,
            checked = false,
            smallItem = false,
        ) {

        }

        addSwitchRow(
            parent = personalContainer,
            iconRes = R.drawable.ic_action_favorite,
            titleRes = R.string.favorites_item,
            checked = current.showFavorites,
            smallItem = false,
        ) { checked ->
            applyConfigChange(requireConfig().copy(showFavorites = checked))
        }

        addSwitchRow(
            parent = personalContainer,
            iconRes = R.drawable.ic_action_star_clusters,
            titleRes = R.string.astro_daily_path,
            checked = false,
            smallItem = false,
            showDivider = false
        ) {

        }

        addSwitchRow(
            parent = renderingContainer,
            iconRes = R.drawable.ic_action_azimuthal_grid,
            titleRes = R.string.azimuthal_grid,
            checked = current.showAzimuthalGrid
        ) { checked ->
            applyConfigChange(requireConfig().copy(showAzimuthalGrid = checked))
        }

        addSwitchRow(
            parent = renderingContainer,
            iconRes = R.drawable.ic_action_meridian_line,
            titleRes = R.string.meridian_line,
            checked = current.showMeridianLine
        ) { checked ->
            applyConfigChange(requireConfig().copy(showMeridianLine = checked))
        }

        addSwitchRow(
            parent = renderingContainer,
            iconRes = R.drawable.ic_action_equatorial_grid,
            titleRes = R.string.equatorial_grid,
            checked = current.showEquatorialGrid
        ) { checked ->
            applyConfigChange(requireConfig().copy(showEquatorialGrid = checked))
        }

        addSwitchRow(
            parent = renderingContainer,
            iconRes = R.drawable.ic_action_eliptical_line,
            titleRes = R.string.ecliptic_line,
            checked = current.showEclipticLine
        ) { checked ->
            applyConfigChange(requireConfig().copy(showEclipticLine = checked))
        }

        addSwitchRow(
            parent = renderingContainer,
            iconRes = R.drawable.ic_action_galaxy_equator,
            titleRes = R.string.equator_line,
            checked = current.showEquatorLine,
            showDivider = false
        ) { checked ->
            applyConfigChange(requireConfig().copy(showEquatorLine = checked))
        }
    }

    private fun addSwitchRow(
        parent: ViewGroup,
        @DrawableRes iconRes: Int,
        @StringRes titleRes: Int,
        checked: Boolean,
        showDivider: Boolean = true,
        smallItem: Boolean = true,
        onToggle: (Boolean) -> Unit
    ) {
        val row = layoutInflater.inflate(R.layout.item_switch_row, parent, false)

        val icon = row.findViewById<ImageView>(R.id.icon)
        val title = row.findViewById<TextView>(R.id.title)
        val switcher = row.findViewById<MaterialSwitch>(R.id.switcher)
        val divider = row.findViewById<View>(R.id.divider)

        setupSwitchItemIcon(icon, iconRes, checked)
        title.setText(titleRes)
        switcher.isChecked = checked

        row.setOnClickListener {
            switcher.toggle()
            setupSwitchItemIcon(icon, iconRes, switcher.isChecked)
        }

        if (!smallItem) {
            row.minimumHeight =
                getDimensionPixelSize(R.dimen.bottom_sheet_selected_item_title_height)
            row.requestLayout()
        }

        switcher.setOnCheckedChangeListener { _, isChecked ->
            onToggle(isChecked)
        }
        AndroidUiHelper.updateVisibility(divider, showDivider)

        parent.addView(row)
    }

    private fun setupSwitchItemIcon(
        imageView: ImageView,
        @DrawableRes iconRes: Int,
        isChecked: Boolean
    ) {
        val imageDrawable =
            uiUtilities.getPaintedIcon(
                iconRes,
                if (isChecked) ColorUtilities.getActiveIconColor(app, nightMode)
                else ColorUtilities.getDefaultIconColor(app, nightMode)
            )
        imageView.setImageDrawable(imageDrawable)
    }

	private fun requireStarMap(): StarMapFragment =
		(parentFragment as? StarMapFragment)
			?: run {
				dismissAllowingStateLoss()
				throw IllegalStateException(
					"Wrong parent fragment for ${this::class.java.simpleName}"
				)
			}

    private fun requireConfig(): StarWatcherSettings.StarMapConfig {
		return requireStarMap().swSettings.getStarMapConfig()
    }

    private fun applyConfigChange(newConfig: StarWatcherSettings.StarMapConfig) {
        requireStarMap().setStarMapSettings(newConfig)
    }
}