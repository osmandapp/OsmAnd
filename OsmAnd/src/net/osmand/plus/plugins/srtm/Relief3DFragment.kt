package net.osmand.plus.plugins.srtm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.base.BaseFullScreenFragment
import net.osmand.plus.configmap.MapOptionSliderFragment
import net.osmand.plus.dashboard.DashboardType
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.srtm.building.SunParametersController
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.utils.UiUtilities

/**
 * 3D Relief screen of Configure map: the switch, rows that open the panels of the vertical
 * exaggeration, the Terrain shadows and Sun position, and the maps to download when the relief
 * has no data here.
 */
class Relief3DFragment : BaseFullScreenFragment(), DownloadEvents {

	private lateinit var plugin: SRTMPlugin
	private var relief3DEnabled = false
	private var profileColor = 0

	private lateinit var iconView: ImageView
	private lateinit var stateView: TextView
	private lateinit var switchCompat: SwitchCompat
	private lateinit var titleBottomDivider: View
	private lateinit var contentContainer: View
	private lateinit var exaggerationValueView: TextView
	private lateinit var terrainShadowsButton: View
	private lateinit var terrainShadowsValueView: TextView
	private lateinit var terrainShadowsFooter: View
	private lateinit var sunPositionValueView: TextView
	private lateinit var downloadMapsCard: DownloadMapsCard

	override fun isUsedOnMap() = true

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		plugin = PluginsHelper.requirePlugin(SRTMPlugin::class.java)
		relief3DEnabled = settings.ENABLE_3D_MAPS.get()
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		updateNightMode()
		val root = inflate(R.layout.fragment_relief_3d, container, false)
		profileColor = settings.applicationMode.getProfileColor(nightMode)
		val portrait = AndroidUiHelper.isOrientationPortrait(requireActivity())
		AndroidUiHelper.updateVisibility(root.findViewById(R.id.shadow_on_map), portrait)

		iconView = root.findViewById(R.id.icon_iv)
		stateView = root.findViewById(R.id.state_tv)
		switchCompat = root.findViewById(R.id.switch_compat)
		titleBottomDivider = root.findViewById(R.id.titleBottomDivider)
		contentContainer = root.findViewById(R.id.content_container)
		exaggerationValueView = root.findViewById(R.id.exaggeration_value)
		terrainShadowsButton = root.findViewById(R.id.terrain_shadows_button)
		terrainShadowsValueView = root.findViewById(R.id.terrain_shadows_value)
		terrainShadowsFooter = root.findViewById(R.id.terrain_shadows_footer)
		sunPositionValueView = root.findViewById(R.id.sun_position_value)
		downloadMapsCard = DownloadMapsCard(app, plugin, root.findViewById(R.id.download_maps_card), nightMode)

		root.findViewById<TextView>(R.id.title_tv).setText(R.string.relief_3d)
		root.findViewById<TextView>(R.id.download_description_tv).setText(R.string.relief_3d_download_description)

		switchCompat.isChecked = relief3DEnabled
		switchCompat.setOnClickListener { onSwitchClick() }
		UiUtilities.setupCompoundButton(switchCompat, nightMode, UiUtilities.CompoundButtonType.PROFILE_DEPENDENT)
		val mainToggle = root.findViewById<View>(R.id.main_toggle)
		mainToggle.setOnClickListener { switchCompat.performClick() }
		UiUtilities.setupListItemBackground(app, mainToggle, profileColor)

		root.findViewById<View>(R.id.vertical_exaggeration_button).setOnClickListener {
			showPanel { Relief3DExaggerationFragment.showInstance(it, DashboardType.RELIEF_3D) }
		}
		terrainShadowsButton.setOnClickListener {
			showPanel { TerrainShadowsFragment.showInstance(it) }
		}
		root.findViewById<View>(R.id.sun_position_button).setOnClickListener {
			showPanel { SunParametersController.showDialog(app, it, DashboardType.RELIEF_3D) }
		}

		updateUiMode()
		return root
	}

	private fun showPanel(show: (FragmentManager) -> Unit) {
		callMapActivity { mapActivity ->
			mapActivity.dashboard.hideDashboard()
			show(mapActivity.supportFragmentManager)
		}
	}

	private fun onSwitchClick() {
		relief3DEnabled = !relief3DEnabled
		switchCompat.isChecked = relief3DEnabled
		settings.ENABLE_3D_MAPS.set(relief3DEnabled)
		callMapActivity { mapActivity ->
			app.runInUIThread { app.osmandMap.mapLayers.mapInfoLayer.recreateAllControls(mapActivity) }
		}
		updateUiMode()
	}

	private fun updateUiMode() {
		if (relief3DEnabled) {
			iconView.setImageDrawable(getPaintedIcon(R.drawable.ic_action_3d_relief, profileColor))
			stateView.setText(R.string.shared_string_on)
			callMapActivity { downloadMapsCard.updateDownloadSection(it) }
		} else {
			iconView.setImageDrawable(getIcon(R.drawable.ic_action_3d_relief, ColorUtilities.getSecondaryIconColorId(nightMode)))
			stateView.setText(R.string.shared_string_off)
		}
		titleBottomDivider.visibility = if (relief3DEnabled) View.GONE else View.VISIBLE
		contentContainer.visibility = if (relief3DEnabled) View.VISIBLE else View.GONE
		updateValues()
	}

	/** The Hillshade layer shades the relief itself: Terrain shadows show Off and cannot be changed. */
	private fun updateValues() {
		val shadedByHillshade = plugin.isReliefShadedByHillshade
		exaggerationValueView.text = MapOptionSliderFragment.getFormattedValue(app, plugin.verticalExaggerationScale)
		terrainShadowsValueView.text = if (shadedByHillshade) {
			getString(R.string.shared_string_off)
		} else {
			"${plugin.terrainShadowsIntensity}%"
		}
		terrainShadowsButton.isEnabled = !shadedByHillshade
		terrainShadowsButton.alpha = if (shadedByHillshade) DISABLED_ALPHA else 1f
		AndroidUiHelper.updateVisibility(terrainShadowsFooter, shadedByHillshade)
		sunPositionValueView.text = plugin.sunLight.getDescription()
	}

	override fun onUpdatedIndexesList() {
		callMapActivity { downloadMapsCard.updateDownloadSection(it) }
	}

	override fun downloadInProgress() {
		downloadMapsCard.downloadInProgress()
	}

	override fun downloadHasFinished() {
		callMapActivity { mapActivity ->
			downloadMapsCard.updateDownloadSection(mapActivity)
			if (plugin.isTerrainLayerEnabled) {
				plugin.registerLayers(mapActivity, mapActivity)
			}
		}
	}

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = super.getInsetTargets()
		collection.replace(InsetTarget.createBottomContainer(R.id.main_container).landscapeLeftSided(true))
		collection.removeType(InsetTarget.Type.ROOT_INSET)
		return collection
	}

	override fun getAnalyticsScreen() = "relief_3d"

	companion object {

		private val TAG = Relief3DFragment::class.java.simpleName
		private const val DISABLED_ALPHA = 0.5f

		@JvmStatic
		fun showInstance(fragmentManager: FragmentManager) {
			if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
				fragmentManager.beginTransaction()
					.replace(R.id.content, Relief3DFragment(), TAG)
					.commitAllowingStateLoss()
			}
		}
	}
}
