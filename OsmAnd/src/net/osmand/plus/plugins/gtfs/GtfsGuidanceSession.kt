package net.osmand.plus.plugins.gtfs

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.data.LatLon
import net.osmand.data.ValueHolder
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.routing.IRouteInformationListener
import net.osmand.plus.utils.AndroidUtils
import net.osmand.router.gtfs.GtfsTransportResultAdapter.GtfsResult
import net.osmand.router.gtfs.guidance.GuidanceController
import net.osmand.router.gtfs.guidance.GuidanceController.Command
import net.osmand.router.gtfs.guidance.GuidanceController.Update
import net.osmand.router.gtfs.guidance.GuidanceNames
import net.osmand.router.gtfs.guidance.GuidanceStatus
import net.osmand.router.gtfs.guidance.ReplanRequest

/**
 * Guidance along one GTFS trip, from the user's tap on Start until they arrive, cancel, replan or the route
 * changes. It owns a [GuidanceController] (the pure logic, tested on the JVM), feeds it locations from the
 * plugin's `updateLocation` hook and a one second tick, and shows the result in the notification of the
 * navigation service, in a panel over the map ([GtfsGuidancePanel]) and as speech.
 *
 * All work happens on the main thread. The stock Start button has already put OsmAnd into navigation mode,
 * which keeps `NavigationService` (and with it location updates) running while the app is in the background;
 * this class piggybacks on that and does not start a service of its own.
 */
class GtfsGuidanceSession(
	private val app: OsmandApplication,
	private val plugin: GtfsTransitPlugin,
	result: GtfsResult,
	names: GuidanceNames,
) : IRouteInformationListener {

	private val log = PlatformUtil.getLog(GtfsGuidanceSession::class.java)

	private val helper = app.transportRoutingHelper
	private val controller = GuidanceController(result.itinerary, names)
	private val texts = GtfsGuidanceTexts(app, result.timeZone)
	private val handler = Handler(Looper.getMainLooper())
	private val arriveSec = result.arriveSec

	private var active = false
	private var replanning = false
	private var finishing = false
	private var wakeLock: PowerManager.WakeLock? = null
	private var lastNotified: String? = null
	private var lastNotifiedMs = 0L

	private val receiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			ACTIONS[intent.action]?.let { command(it) }
		}
	}

	private val tick = object : Runnable {
		override fun run() {
			if (!active || finishing || replanning) {
				return
			}
			apply(controller.onTick(nowSec()))
			handler.postDelayed(this, TICK_MS)
		}
	}

	val isActive: Boolean get() = active

	/** The status for the panel and the notification. */
	fun status(): GuidanceStatus = controller.status(nowSec())

	fun title(): String = if (replanning) {
		app.getString(R.string.gtfs_guide_planning_new)
	} else {
		texts.instruction(status())
	}

	fun detail(): String? = if (replanning) null else texts.detail(status())

	fun start() {
		if (active) {
			return
		}
		active = true
		helper.addListener(this)
		for (action in ACTIONS.keys) {
			AndroidUtils.registerBroadcastReceiver(app, action, receiver, false)
		}
		acquireWakeLock()
		apply(controller.start(nowSec()))
		handler.postDelayed(tick, TICK_MS)
	}

	/** A location from OsmAnd's location pipeline (foreground or the navigation service). */
	fun onLocation(location: Location?) {
		if (location == null) {
			return
		}
		handler.post {
			if (active && !finishing && !replanning) {
				val accuracy = if (location.hasAccuracy()) location.accuracy else UNKNOWN_ACCURACY_M
				apply(controller.onLocation(location.latitude, location.longitude, accuracy, nowSec(), true))
			}
		}
	}

	fun command(command: Command) {
		if (!active) {
			return
		}
		if (finishing || replanning) {
			// only Cancel makes sense while a new trip is being planned or the arrival is shown
			if (command == Command.CANCEL) {
				stop(true)
			}
			return
		}
		apply(controller.onCommand(command, nowSec()))
	}

	/** Ends the session; with [stopNavigation] also leaves navigation mode (stock "Stop"). */
	fun stop(stopNavigation: Boolean) {
		if (!active) {
			return
		}
		active = false
		handler.removeCallbacksAndMessages(null)
		helper.removeListener(this)
		try {
			app.unregisterReceiver(receiver)
		} catch (e: IllegalArgumentException) {
			log.error("Guidance receiver was not registered", e)
		}
		wakeLock?.let { if (it.isHeld) it.release() }
		wakeLock = null
		plugin.onGuidanceEnded(this)
		if (stopNavigation) {
			app.stopNavigation()
		}
	}

	/** Called by the navigation notification: fills the builder it made with the current step. */
	fun buildNotification(builder: NotificationCompat.Builder): NotificationCompat.Builder {
		val title = title()
		val detail = detail()
		builder.setContentTitle(title)
			.setContentText(detail ?: app.getString(R.string.gtfs_guide_title))
			.setCategory(NotificationCompat.CATEGORY_NAVIGATION)
			.setStyle(NotificationCompat.BigTextStyle().bigText(if (detail == null) title else "$title\n$detail"))
		if (!replanning && !finishing) {
			val status = status()
			// a notification shows three actions: the step's main one, Replan and Cancel (Skip is in the panel)
			when {
				status.canAlight -> builder.addAction(
					R.drawable.ic_notification_play, app.getString(R.string.gtfs_guide_got_off), pending(Command.ALIGHTED))
				status.canBoard -> builder.addAction(
					R.drawable.ic_notification_play, app.getString(R.string.gtfs_guide_on_board), pending(Command.BOARDED))
			}
			if (status.canReplan) {
				builder.addAction(
					R.drawable.ic_notification_start_navigation, app.getString(R.string.gtfs_guide_replan), pending(Command.REPLAN))
			}
		}
		builder.addAction(
			R.drawable.ic_notification_remove, app.getString(R.string.gtfs_guide_cancel), pending(Command.CANCEL))
		return builder
	}

	// ------------------------------------------------------------------ route helper callbacks

	// The helper calls these while it walks its listener list, and stop() edits that list: defer.

	override fun newRouteIsCalculated(newRoute: Boolean, showToast: ValueHolder<Boolean>?) {
		app.runInUIThread { onRoutesChanged() }
	}

	override fun routeWasCancelled() {
		app.runInUIThread {
			if (active && !replanning) {
				stop(false)
			}
		}
	}

	override fun routeWasFinished() {
	}

	// ------------------------------------------------------------------ internals

	private fun onRoutesChanged() {
		if (!active) {
			return
		}
		if (!replanning) {
			// someone else planned a route while guiding: this trip is no longer the active one
			stop(false)
			return
		}
		handler.removeCallbacks(replanTimeout)
		if (helper.routes?.firstOrNull() !is GtfsResult) {
			// the map based planner answered: leave that route for the stock UI to show
			stop(false)
			return
		}
		helper.currentRoute = 0
		stop(false)
		plugin.startGuidance()
	}

	private fun apply(update: Update) {
		for (event in update.spoken) {
			texts.sentence(event.textKey, event.args)?.let { speak(it) }
		}
		val replan = update.replan
		when {
			replan != null -> replan(replan)
			update.arrived -> {
				finishing = true
				handler.removeCallbacks(tick)
				handler.postDelayed({ stop(true) }, ARRIVED_LINGER_MS)
			}
			update.finished -> {
				stop(true)
				return
			}
		}
		refresh()
	}

	private fun replan(request: ReplanRequest) {
		replanning = true
		handler.removeCallbacks(tick)
		val here = app.locationProvider.lastKnownLocation
		val fresh = here != null && System.currentTimeMillis() - here.time < FRESH_FIX_MS
		val start = if (fresh) LatLon(here!!.latitude, here.longitude) else request.from ?: helper.startLocation
		val end = helper.endLocation ?: request.to
		if (start == null || end == null) {
			failReplan()
			return
		}
		handler.postDelayed(replanTimeout, REPLAN_TIMEOUT_MS)
		helper.departureTimeSec = null // leave now, not at an earlier "depart at" time
		helper.setFinalAndCurrentLocation(end, start)
	}

	private val replanTimeout = Runnable { failReplan() }

	private fun failReplan() {
		app.showToastMessage(R.string.gtfs_guide_replan_failed)
		stop(true)
	}

	private fun speak(text: String) {
		val player = app.routingHelper.voiceRouter.player
		if (player == null || !player.speak(text)) {
			log.info("Guidance not spoken (no text-to-speech voice, muted or recorded voice): $text")
		}
	}

	/** Updates the panel at once and the notification when its text changed (at most every 2 seconds). */
	private fun refresh() {
		plugin.onGuidanceChanged()
		val text = title() + "|" + detail() + "|" + status().canAlight + status().canBoard
		val now = System.currentTimeMillis()
		if (text != lastNotified && now - lastNotifiedMs >= NOTIFY_MIN_MS) {
			lastNotified = text
			lastNotifiedMs = now
			app.notificationHelper.refreshNotifications()
		}
	}

	private fun acquireWakeLock() {
		val power = app.getSystemService(Context.POWER_SERVICE) as PowerManager
		val remainingMs = (arriveSec - nowSec()) * 1000 + WAKE_EXTRA_MS
		wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "osmand:gtfs-guidance").also {
			it.setReferenceCounted(false)
			it.acquire(remainingMs.coerceIn(WAKE_MIN_MS, WAKE_MAX_MS))
		}
	}

	private fun pending(command: Command): PendingIntent {
		val action = ACTIONS.entries.first { it.value == command }.key
		val intent = Intent(action).setPackage(app.packageName)
		return PendingIntent.getBroadcast(
			app, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)
	}

	private fun nowSec() = System.currentTimeMillis() / 1000

	companion object {
		private const val PREFIX = "net.osmand.plus.gtfs.guidance."
		val ACTIONS = mapOf(
			PREFIX + "BOARDED" to Command.BOARDED,
			PREFIX + "ALIGHTED" to Command.ALIGHTED,
			PREFIX + "SKIP" to Command.SKIP,
			PREFIX + "REPLAN" to Command.REPLAN,
			PREFIX + "CANCEL" to Command.CANCEL,
		)
		private const val TICK_MS = 1000L
		private const val NOTIFY_MIN_MS = 2000L
		private const val ARRIVED_LINGER_MS = 15_000L
		private const val REPLAN_TIMEOUT_MS = 60_000L
		private const val FRESH_FIX_MS = 120_000L
		private const val UNKNOWN_ACCURACY_M = 50f
		private const val WAKE_EXTRA_MS = 30 * 60_000L
		private const val WAKE_MIN_MS = 10 * 60_000L
		private const val WAKE_MAX_MS = 6 * 3_600_000L
	}
}
