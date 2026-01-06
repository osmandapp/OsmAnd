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
		private const val STAR_DB_SIZE_PREFERENCE_ID = "star_db_size"
	}

	private val _swSettings by lazy { StarWatcherSettings(getSettingsPref()) }
	val swSettings: StarWatcherSettings get() = _swSettings

	private val _astroDataProvider by lazy { AstroDataDbProvider() }
	val astroDataProvider: AstroDataProvider get() = _astroDataProvider

	private var starDbUpdated = false

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

	private fun getStarDbSizePref(): CommonPreference<Int> =
		registerIntPreference(STAR_DB_SIZE_PREFERENCE_ID, -1).makeGlobal()

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

	interface DownloadListener {
		fun onProgress(progress: Int)

		fun onComplete(success: Boolean, skipDownload: Boolean)
	}

	fun checkAndDownloadStarsDb(app: OsmandApplication, listener: DownloadListener?) {
		if (starDbUpdated) {
			listener?.onComplete(success = true, skipDownload = true)
			return
		}

		val dbFile = File(app.cacheDir, "stars.db")

		CoroutineScope(Dispatchers.IO).launch {
			try {
				val urlStr = "https://builder.osmand.net/basemap/astro/stars.db.gz"
				val url = URL(urlStr)
				val connection = url.openConnection() as java.net.HttpURLConnection
				connection.requestMethod = "HEAD"
				connection.connect()

				val remoteLength = connection.contentLength
				connection.disconnect()

				val starDbSize = getStarDbSizePref().get()
				val needsDownload = !dbFile.exists() || remoteLength != starDbSize
				if (needsDownload) {
					app.runInUIThread { listener?.onProgress(0) }

					val error = AndroidNetworkUtils.downloadFile(urlStr, dbFile, true, null)
					if (error != null) {
						throw Exception(error)
					}
				}

				starDbUpdated = true
				getStarDbSizePref().set(remoteLength)
				app.runInUIThread { listener?.onComplete(success = true, skipDownload = false) }

			} catch (e: Exception) {
				LOG.error("Error downloading stars.db", e)
				app.runInUIThread { listener?.onComplete(success = false, skipDownload = false) }
			}
		}
	}
}
