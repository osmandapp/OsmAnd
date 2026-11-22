package net.osmand.plus.plugins.srtm

import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentManager
import com.google.android.material.slider.Slider
import net.osmand.plus.R
import net.osmand.plus.configmap.ConfigureMapOptionFragment
import net.osmand.plus.dashboard.DashboardType
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.srtm.SRTMPlugin.MAX_TERRAIN_SHADOWS_INTENSITY
import net.osmand.plus.plugins.srtm.SRTMPlugin.MIN_TERRAIN_SHADOWS_INTENSITY
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.UiUtilities
import kotlin.math.roundToInt

/**
 * Panel over the map with the intensity of the Terrain shadows of the 3D relief, opened from the
 * 3D Relief screen. Works like Terrain - Visibility: the map shows the value while the slider
 * moves, Apply keeps it, leaving the panel without Apply brings the previous value back.
 */
class TerrainShadowsFragment : ConfigureMapOptionFragment() {

	private lateinit var plugin: SRTMPlugin
	private lateinit var valueView: TextView
	private lateinit var slider: Slider

	private var originalIntensity = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		plugin = PluginsHelper.requirePlugin(SRTMPlugin::class.java)
		originalIntensity = savedInstanceState?.getInt(ORIGINAL_INTENSITY) ?: plugin.terrainShadowsIntensity

		val activity = requireMapActivity()
		activity.onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				activity.supportFragmentManager.popBackStack()
				activity.dashboard.setDashboardVisibility(true, DashboardType.RELIEF_3D, false)
			}
		})
	}

	override fun onDestroy() {
		if (activity?.isChangingConfigurations != true) {
			plugin.TERRAIN_SHADOWS_INTENSITY.set(originalIntensity)
		}
		super.onDestroy()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putInt(ORIGINAL_INTENSITY, originalIntensity)
	}

	override fun getToolbarTitle(): String = getString(R.string.terrain_shadows)

	override fun setupMainContent(container: ViewGroup) {
		val view = inflate(R.layout.terrain_visibility_fragment, container, false)
		view.findViewById<TextView>(R.id.title).setText(R.string.terrain_shadows)
		valueView = view.findViewById(R.id.transparency_value_tv)
		slider = view.findViewById(R.id.transparency_slider)

		UiUtilities.setupSlider(slider, nightMode, settings.applicationMode.getProfileColor(nightMode))
		slider.valueFrom = MIN_TERRAIN_SHADOWS_INTENSITY.toFloat()
		slider.valueTo = MAX_TERRAIN_SHADOWS_INTENSITY.toFloat()
		slider.stepSize = STEP.toFloat()
		slider.addOnChangeListener { _, value, fromUser ->
			if (fromUser) {
				plugin.TERRAIN_SHADOWS_INTENSITY.set(value.roundToInt())
				updateValue()
				refreshMap()
			}
		}
		updateSlider()
		container.addView(view)
	}

	private fun updateSlider() {
		val stepped = (plugin.terrainShadowsIntensity / STEP.toFloat()).roundToInt() * STEP
		slider.value = stepped.coerceIn(MIN_TERRAIN_SHADOWS_INTENSITY, MAX_TERRAIN_SHADOWS_INTENSITY).toFloat()
		updateValue()
	}

	private fun updateValue() {
		valueView.text = "${plugin.terrainShadowsIntensity}%"
		updateApplyButton(plugin.terrainShadowsIntensity != originalIntensity)
	}

	override fun resetToDefault() {
		plugin.TERRAIN_SHADOWS_INTENSITY.resetToDefault()
		updateSlider()
		refreshMap()
	}

	override fun applyChanges() {
		originalIntensity = plugin.terrainShadowsIntensity
	}

	override fun getAnalyticsScreen(): String = "terrain_shadows"

	companion object {

		private const val ORIGINAL_INTENSITY = "original_intensity"
		private const val STEP = 5

		@JvmStatic
		fun showInstance(manager: FragmentManager) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, ConfigureMapOptionFragment.TAG)) {
				manager.beginTransaction()
					.replace(R.id.fragmentContainer, TerrainShadowsFragment(), ConfigureMapOptionFragment.TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss()
			}
		}
	}
}
