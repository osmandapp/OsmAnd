package net.osmand.plus.plugins.astro

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.osmand.PlatformUtil
import net.osmand.aidlapi.OsmAndCustomizationConstants
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.OsmandPlugin
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.utils.AndroidNetworkUtils
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem
import java.io.File
import java.net.URL

class StarWatcherPlugin(app: OsmandApplication) : OsmandPlugin(app) {

	companion object {
		private val LOG = PlatformUtil.getLog(StarWatcherPlugin::class.java)
		private const val SETTINGS_PREFERENCE_ID = "star_watcher_settings"
	}

	private val _swSettings by lazy { StarWatcherSettings(getSettingsPref()) }
	val swSettings: StarWatcherSettings get() = _swSettings

	private val _astroDataProvider by lazy { AstroDataDbProvider() }
	val astroDataProvider: AstroDataProvider get() = _astroDataProvider

	override fun getId(): String {
		return OsmAndCustomizationConstants.PLUGIN_STAR_WATCHER
	}

	override fun getName(): String {
		return app.getString(R.string.star_watcher_plugin_name)
	}

	override fun getDescription(linksEnabled: Boolean): CharSequence {
		return app.getString(R.string.star_watcher_plugin_description)
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
