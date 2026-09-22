package net.osmand.wear.data

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

import net.osmand.wear.api.WearProtocol

/**
 * Receives state pushed by the phone. Runs regardless of whether a screen is open, so that
 * tiles and complications (later stages) see fresh data without launching the app.
 */
class WearDataLayerService : WearableListenerService() {

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val reader by lazy { SnapshotReader(applicationContext) }

	override fun onDataChanged(events: DataEventBuffer) {
		// The buffer is released as soon as this method returns, so the data maps are copied out
		// before the arrow assets are fetched on another thread.
		val maps = events
			.filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == WearProtocol.PATH_STATE }
			.map { DataMapItem.fromDataItem(it.dataItem).dataMap }

		for (map in maps) {
			scope.launch { PhoneStateRepository.onSnapshot(reader.read(map)) }
		}
	}

	override fun onDestroy() {
		scope.cancel()
		super.onDestroy()
	}
}
