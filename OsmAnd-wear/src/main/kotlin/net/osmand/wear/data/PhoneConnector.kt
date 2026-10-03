package net.osmand.wear.data

import android.content.Context
import android.net.Uri
import android.util.Log

import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable

import kotlinx.coroutines.tasks.await

import net.osmand.wear.api.WearCodec
import net.osmand.wear.api.WearCommand
import net.osmand.wear.api.WearProtocol

/** Watch side of the link: finds the phone, pulls the last snapshot, sends commands to it. */
class PhoneConnector(context: Context) {

	private val capabilityClient = Wearable.getCapabilityClient(context)
	private val messageClient = Wearable.getMessageClient(context)
	private val dataClient = Wearable.getDataClient(context)
	private val reader = SnapshotReader(context)

	/** Node id of a reachable phone running OsmAnd, or null. */
	suspend fun findPhoneNode(): String? = runCatching {
		val info = capabilityClient
			.getCapability(WearProtocol.CAPABILITY_PHONE_APP, CapabilityClient.FILTER_REACHABLE)
			.await()
		info.nodes.firstOrNull { it.isNearby }?.id ?: info.nodes.firstOrNull()?.id
	}.onFailure { Log.d(TAG, "Capability lookup failed", it) }.getOrNull()

	/**
	 * Reads whatever snapshot the phone left behind. Data Layer items survive disconnects, so
	 * this is what fills the screen before the first live update arrives.
	 */
	suspend fun loadLastSnapshot(): Snapshot? = runCatching {
		val uri = Uri.Builder()
			.scheme(PutDataRequest.WEAR_URI_SCHEME)
			.path(WearProtocol.PATH_STATE)
			.build()
		val buffer = dataClient.getDataItems(uri).await()
		val dataMap = try {
			buffer.firstOrNull()?.let { DataMapItem.fromDataItem(it).dataMap }
		} finally {
			buffer.release()
		}
		dataMap?.let { reader.read(it) }
	}.onFailure { Log.d(TAG, "Reading last state failed", it) }.getOrNull()

	/** Returns false when the phone could not be reached, so the UI can roll back optimism. */
	suspend fun sendCommand(command: WearCommand): Boolean {
		val nodeId = findPhoneNode() ?: return false
		return runCatching {
			messageClient.sendMessage(nodeId, WearProtocol.PATH_COMMAND, WearCodec.encodeCommand(command)).await()
			true
		}.onFailure { Log.d(TAG, "Sending $command failed", it) }.getOrDefault(false)
	}

	/** Handshake performed whenever a screen becomes visible. */
	suspend fun refresh() {
		PhoneStateRepository.onConnecting()
		loadLastSnapshot()?.let { PhoneStateRepository.onSnapshot(it) }
		if (findPhoneNode() == null) {
			PhoneStateRepository.onPhoneUnreachable()
		} else {
			sendCommand(WearCommand.RequestState)
		}
	}

	companion object {
		private const val TAG = "OsmAndWear"
	}
}
