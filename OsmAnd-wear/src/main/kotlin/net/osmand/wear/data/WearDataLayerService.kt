package net.osmand.wear.data

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

import net.osmand.wear.api.WearProtocol

/**
 * Receives state pushed by the phone. Runs regardless of whether a screen is open, so that
 * tiles and complications (later stages) see fresh data without launching the app.
 */
class WearDataLayerService : WearableListenerService() {

	override fun onDataChanged(events: DataEventBuffer) {
		for (event in events) {
			if (event.type != DataEvent.TYPE_CHANGED) {
				continue
			}
			val item = event.dataItem
			if (item.uri.path != WearProtocol.PATH_STATE) {
				continue
			}
			val bytes = DataMapItem.fromDataItem(item).dataMap.getByteArray(WearProtocol.KEY_STATE)
			PhoneStateRepository.onStateBytes(bytes)
		}
	}
}
