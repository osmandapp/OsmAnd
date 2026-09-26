package net.osmand.plus.wear

import android.util.Log

import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.wear.api.WearCodec
import net.osmand.wear.api.WearProtocol

/**
 * Phone side of the link: pushes a snapshot into the Data Layer.
 *
 * Manoeuvre arrows travel as assets rather than inline bytes: Play services addresses assets by
 * content, so an arrow that has not changed is not pushed over Bluetooth again while the
 * distances next to it tick down.
 */
class WearProtocolPublisher(private val app: OsmandApplication) {

	private val dataClient = Wearable.getDataClient(app)
	private val stateBuilder = WearStateBuilder(app)

	@Volatile
	private var lastPublished: WearStateBuilder.Snapshot? = null

	fun publish() {
		val snapshot = try {
			stateBuilder.build()
		} catch (e: Exception) {
			LOG.error("Failed to build wear state", e)
			return
		}

		// Comparing the state alone is enough: the arrows are a pure function of the turn type
		// and exit number, both of which are fields of the state being compared.
		val previous = lastPublished?.state
		val state = snapshot.state
		if (previous != null && previous.copy(updatedAt = state.updatedAt) == state) {
			return
		}
		lastPublished = snapshot

		val request = PutDataMapRequest.create(WearProtocol.PATH_STATE).apply {
			dataMap.putByteArray(WearProtocol.KEY_STATE, WearCodec.encodeState(state))
			for ((key, png) in snapshot.icons) {
				dataMap.putAsset(key, Asset.createFromBytes(png))
			}
		}.asPutDataRequest().setUrgent()

		dataClient.putDataItem(request)
			.addOnFailureListener { Log.d(TAG, "Publishing state to the watch failed", it) }
	}

	companion object {
		private const val TAG = "OsmAndWear"
		private val LOG = PlatformUtil.getLog(WearProtocolPublisher::class.java)
	}
}
