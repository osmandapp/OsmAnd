package net.osmand.plus.plugins.gtfs

import android.net.Uri
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.OsmandPlugin
import net.osmand.plus.settings.fragments.SettingsScreenType
import net.osmand.router.gtfs.GtfsFeedStore
import net.osmand.router.gtfs.GtfsIndexGuidanceNames
import net.osmand.router.gtfs.GtfsIndexSource
import net.osmand.router.gtfs.GtfsIndexSummary
import net.osmand.router.gtfs.GtfsTransitRouteProvider
import net.osmand.router.gtfs.GtfsTransportResultAdapter.GtfsResult

/**
 * Offline public transport planning from imported GTFS timetables.
 *
 * The planner, the index and the feed bookkeeping live in `OsmAnd-java` (`net.osmand.router.gtfs`) and are
 * tested on the JVM. This class owns the feed store, registers the route provider while the plugin is on
 * and an index exists, and runs imports one at a time.
 */
class GtfsTransitPlugin(app: OsmandApplication) : OsmandPlugin(app) {

	private val log = PlatformUtil.getLog(GtfsTransitPlugin::class.java)

	val maxWalkMeters = registerIntPreference(MAX_WALK_PREF, DEFAULT_MAX_WALK_METERS)

	val store = GtfsFeedStore(app.getAppPath(DIR_NAME))
	private val source = GtfsIndexSource(store.indexFile)
	private val provider = GtfsTransitRouteProvider(source) { maxWalkMeters.get() }
	private var providerRegistered = false

	/** The running import, removal or rebuild; changed and read on the UI thread only. */
	var update: GtfsFeedUpdate? = null
		private set

	/** What the last update said, kept for the settings screen; null once it was shown. */
	var lastMessage: String? = null

	/** Called on the UI thread when the running update changes its progress or ends. */
	var updateListener: (() -> Unit)? = null

	override fun getId() = PLUGIN_ID

	override fun getName(): String = app.getString(net.osmand.plus.R.string.gtfs_plugin_name)

	override fun getDescription(linksEnabled: Boolean): CharSequence =
		app.getString(net.osmand.plus.R.string.gtfs_plugin_description)

	override fun getSettingsScreenType() = SettingsScreenType.GTFS_SETTINGS

	override fun setEnabled(enabled: Boolean) {
		super.setEnabled(enabled)
		if (!enabled) {
			guidanceSession?.stop(false)
		}
		refreshProvider()
	}

	override fun mapActivityCreate(activity: MapActivity) {
		// the route helper may not have existed yet when the plugin was enabled at start up
		refreshProvider()
	}

	override fun mapActivityResume(activity: MapActivity) {
		panelActivity = activity
		showPanel()
	}

	override fun mapActivityPause(activity: MapActivity) {
		if (panelActivity === activity) {
			panel?.detach()
			panel = null
			panelActivity = null
		}
	}

	/** Called for every location, in the foreground and from the navigation service in the background. */
	override fun updateLocation(location: Location?) {
		guidanceSession?.onLocation(location)
	}

	/**
	 * Starts step by step guidance when the route the user just started is a timetable route; any other
	 * route is left to the stock behaviour. Returns true if guidance runs.
	 */
	fun startGuidance(): Boolean {
		if (!isEnabled) {
			return false
		}
		val result = app.transportRoutingHelper?.currentRouteResult as? GtfsResult ?: return false
		val loaded = source.get()
		if (loaded == null) {
			app.showToastMessage(net.osmand.plus.R.string.gtfs_guide_no_index)
			return false
		}
		guidanceSession?.stop(false)
		val started = GtfsGuidanceSession(app, this, result, GtfsIndexGuidanceNames(loaded.index))
		guidanceSession = started
		started.start()
		showPanel()
		return true
	}

	/** The running guidance, or null. */
	var guidanceSession: GtfsGuidanceSession? = null
		private set

	private var panel: GtfsGuidancePanel? = null
	private var panelActivity: MapActivity? = null

	fun onGuidanceChanged() {
		panel?.update()
	}

	fun onGuidanceEnded(session: GtfsGuidanceSession) {
		if (guidanceSession === session) {
			guidanceSession = null
			panel?.detach()
			panel = null
			app.notificationHelper.refreshNotifications()
		}
	}

	private fun showPanel() {
		val session = guidanceSession
		val activity = panelActivity
		if (session != null && activity != null && panel == null) {
			panel = GtfsGuidancePanel(activity, session).also { it.attach() }
		}
	}

	/** Registers the provider when the plugin is on and there is an index, and removes it otherwise. */
	fun refreshProvider() {
		val helper = app.transportRoutingHelper ?: return
		val wanted = isEnabled && store.hasIndex()
		if (wanted && !providerRegistered) {
			helper.addRouteProvider(provider)
			providerRegistered = true
		} else if (!wanted && providerRegistered) {
			helper.removeRouteProvider(provider)
			providerRegistered = false
		}
	}

	/** The open index described for the settings screen, or null when there is none. */
	fun summary(): GtfsIndexSummary? {
		val loaded = source.get() ?: return null
		return GtfsIndexSummary.of(loaded.index, store.indexFile)
	}

	/** Why the index file cannot be used, or null. Call after [summary] returned null. */
	fun indexError(): String? = source.lastError?.message

	fun importFeeds(uris: List<Uri>) = start(GtfsFeedUpdate.Change.Add(uris))

	fun removeFeed(name: String) = start(GtfsFeedUpdate.Change.Remove(name))

	fun rebuildIndex() = start(GtfsFeedUpdate.Change.Rebuild)

	fun cancelUpdate() {
		update?.cancel()
	}

	private fun start(change: GtfsFeedUpdate.Change) {
		if (update != null) {
			return
		}
		lastMessage = null
		val job = GtfsFeedUpdate(app, store, change, { fireListener() }) { message ->
			update = null
			lastMessage = message
			source.invalidate()
			refreshProvider()
			app.showToastMessage(message)
			fireListener()
		}
		update = job
		job.start()
	}

	private fun fireListener() {
		try {
			updateListener?.invoke()
		} catch (e: RuntimeException) {
			log.error("GTFS settings listener failed", e)
		}
	}

	companion object {
		const val PLUGIN_ID = "gtfs.transit.plugin"
		private const val MAX_WALK_PREF = "gtfs_max_walk_meters"
		private const val DIR_NAME = "gtfs"
		const val DEFAULT_MAX_WALK_METERS = 800
	}
}
