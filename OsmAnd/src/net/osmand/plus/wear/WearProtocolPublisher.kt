package net.osmand.plus.wear

import android.util.Log

import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.wear.api.PhoneState
import net.osmand.wear.api.WearCodec
import net.osmand.wear.api.WearProtocol

/**
 * Phone side of the link: pushes a [PhoneState] snapshot into the Data Layer.
 *
 * Data items are deduplicated by content, so republishing an unchanged snapshot is cheap —
 * but building one is not, hence callers are expected to throttle rather than poll.
 */
class WearProtocolPublisher(private val app: OsmandApplication) {

	private val dataClient = Wearable.getDataClient(app)
	private val stateBuilder = WearStateBuilder(app)

	@Volatile
	private var lastPublished: PhoneState? = null

	fun publish() {
		val state = try {
			stateBuilder.build()
		} catch (e: Exception) {
			LOG.error("Failed to build wear state", e)
			return
		}
		publish(state)
	}

	fun publish(state: PhoneState) {
		// updatedAt changes on every build, so compare everything else to avoid waking the
		// watch radio for an identical payload.
		val previous = lastPublished
		if (previous != null && previous.copy(updatedAt = state.updatedAt) == state) {
			return
		}
		lastPublished = state

		val request = PutDataMapRequest.create(WearProtocol.PATH_STATE).apply {
			dataMap.putByteArray(WearProtocol.KEY_STATE, WearCodec.encodeState(state))
		}.asPutDataRequest().setUrgent()

		dataClient.putDataItem(request)
			.addOnFailureListener { Log.d(TAG, "Publishing state to the watch failed", it) }
	}

	companion object {
		private const val TAG = "OsmAndWear"
		private val LOG = PlatformUtil.getLog(WearProtocolPublisher::class.java)
	}
}
