package net.osmand.plus.plugins.astronomy

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.View
import net.osmand.PlatformUtil
import net.osmand.aidlapi.OsmAndCustomizationConstants
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.OsmandPlugin
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem

class AstronomyPlugin(app: OsmandApplication) : OsmandPlugin(app) {

	companion object {
		private val LOG = PlatformUtil.getLog(AstronomyPlugin::class.java)
		private const val SETTINGS_PREFERENCE_ID = "astronomy_settings"
	}

	private val astronomySettings by lazy { AstronomyPluginSettings(getSettingsPref()) }
	val astroSettings: AstronomyPluginSettings get() = astronomySettings

	private val astroDataProvider by lazy { AstroDataDbProvider() }
	val dataProvider: AstroDataProvider get() = astroDataProvider

	override fun getId(): String {
		return OsmAndCustomizationConstants.PLUGIN_ASTRONOMY
	}

	override fun getName(): String {
		val name = app.getString(R.string.astronomy_plugin_name)
		return app.getString(R.string.ltr_or_rtl_combine_via_space, name, "(Beta)")
	}

	override fun getDescription(linksEnabled: Boolean): CharSequence {
		return app.getString(R.string.astronomy_plugin_description)
	}

	override fun getLogoResourceId(): Int {
		return R.drawable.ic_action_favorite
	}

	override fun getAssetResourceImage(): Drawable? {
		return app.uiUtilities.getIcon(R.drawable.osmand_development)
	}

	override fun init(app: OsmandApplication, activity: Activity?): Boolean {
		return true
	}

	private fun getSettingsPref(): CommonPreference<String> =
		registerStringPreference(SETTINGS_PREFERENCE_ID, "").makeProfile().makeShared()

	override fun registerOptionsMenuItems(mapActivity: MapActivity, helper: ContextMenuAdapter) {
		if (isActive) {
			helper.addItem(
				ContextMenuItem(OsmAndCustomizationConstants.DRAWER_STAR_MAP_ID)
					.setTitleId(R.string.star_map, mapActivity)
					.setIcon(R.drawable.ic_action_favorite)
					.setOrder(18)
					.setListener { _: OnDataChangeUiAdapter?, _: View?, _: ContextMenuItem?, _: Boolean ->
						app.logEvent("skymapOpen")
						showSkymap(mapActivity)
						true
					}
			)
		}
	}

	fun showSkymap(mapActivity: MapActivity) {
		StarMapFragment.showInstance(mapActivity.supportFragmentManager)
	}
}
