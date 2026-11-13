package net.osmand.plus.plugins.gtfs

import android.text.format.DateFormat
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.router.TransportClockFormat
import net.osmand.router.gtfs.guidance.GuidanceStatus
import java.time.ZoneId
import java.util.Locale

/**
 * Turns the text keys and arguments of the guidance engine (`guidance.board`, ...) into the sentences of
 * `strings.xml`. Clock times are shown in the feed's time zone, 12 or 24 hour as the device is set.
 */
class GtfsGuidanceTexts(private val app: OsmandApplication, private val zone: ZoneId) {

	private val clock = TransportClockFormat(DateFormat.is24HourFormat(app), Locale.getDefault())

	/** The sentence for a text key, or null if the key is unknown or silent. */
	fun sentence(key: String?, args: List<Any?>): String? = when (key) {
		"guidance.walk" -> if (args.firstOrNull() == null) {
			app.getString(R.string.gtfs_guide_walk_destination)
		} else {
			app.getString(R.string.gtfs_guide_walk, args[0])
		}
		"guidance.board" -> ride(R.string.gtfs_guide_board, R.string.gtfs_guide_board_toward, args)
		"guidance.transfer" -> ride(R.string.gtfs_guide_transfer, R.string.gtfs_guide_transfer_toward, args)
		"guidance.rideStarted" -> app.getString(R.string.gtfs_guide_ride_started, args[0], stops(args[1]))
		"guidance.nextStop" -> app.getString(R.string.gtfs_guide_next_stop, args[0], stops(args[1]))
		"guidance.getOffNext" -> app.getString(R.string.gtfs_guide_get_off_next, args[0])
		"guidance.missed" -> app.getString(R.string.gtfs_guide_missed, args[0], args[1], time(args[2]))
		"guidance.replan" -> app.getString(R.string.gtfs_guide_replan_offer)
		"guidance.offRoute" -> app.getString(R.string.gtfs_guide_off_route)
		"guidance.routeResumed" -> app.getString(R.string.gtfs_guide_route_resumed)
		"guidance.arrived" -> app.getString(R.string.gtfs_guide_arrived)
		"guidance.cancelled" -> if (args.firstOrNull() == "replan") app.getString(R.string.gtfs_guide_planning_new) else null
		else -> null
	}

	/** The instruction shown for a status: its sentence, or the app name of the feature before the first one. */
	fun instruction(status: GuidanceStatus): String =
		sentence(status.textKey, status.args) ?: app.getString(R.string.gtfs_guide_title)

	/** The extra line, for now the distance left on a walk, or null. */
	fun detail(status: GuidanceStatus): String? =
		if (status.state == net.osmand.router.gtfs.guidance.GuidanceState.RIDING && status.rideRoute != null
			&& status.confidence != net.osmand.router.gtfs.guidance.GtfsGuidanceEngine.Confidence.MANUAL) {
			val head = status.rideHeadsign
			if (status.confidence == net.osmand.router.gtfs.guidance.GtfsGuidanceEngine.Confidence.ASSUMED) {
				app.getString(R.string.gtfs_guide_status_assumed, status.rideRoute)
			} else if (head.isNullOrEmpty()) {
				app.getString(R.string.gtfs_guide_status_auto_plain, status.rideRoute)
			} else {
				app.getString(R.string.gtfs_guide_status_auto, status.rideRoute, head)
			}
		} else if (status.needsConfirmation) {
			app.getString(R.string.gtfs_guide_status_assumed_off)
		} else if (status.walkRemainingM >= 0) {
			app.getString(
				R.string.gtfs_guide_walk_left,
				OsmAndFormatter.getFormattedDistance(status.walkRemainingM.toFloat(), app)
			)
		} else {
			null
		}

	private fun ride(plain: Int, toward: Int, args: List<Any?>): String {
		val headsign = args[1] as String?
		return if (headsign.isNullOrEmpty()) {
			app.getString(plain, args[0], args[2], time(args[3]))
		} else {
			app.getString(toward, args[0], headsign, args[2], time(args[3]))
		}
	}

	private fun stops(count: Any?): String {
		val n = (count as Number).toInt()
		return app.resources.getQuantityString(R.plurals.gtfs_guide_stops, n, n)
	}

	private fun time(epochSec: Any?): String = clock.format((epochSec as Number).toLong(), zone)
}
