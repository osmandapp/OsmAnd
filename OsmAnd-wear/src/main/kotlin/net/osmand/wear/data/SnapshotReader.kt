package net.osmand.wear.data

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Wearable

import kotlinx.coroutines.tasks.await

import net.osmand.wear.api.WearCodec
import net.osmand.wear.api.WearProtocol

/**
 * Turns a published data item into something the screens can render.
 *
 * Arrow images arrive as assets, which Play services transfers lazily, so reading one is a
 * suspending call rather than a field access.
 */
class SnapshotReader(context: Context) {

	private val dataClient = Wearable.getDataClient(context)

	suspend fun read(dataMap: DataMap): Snapshot? {
		val bytes = dataMap.getByteArray(WearProtocol.KEY_STATE) ?: return null
		val state = WearCodec.decodeState(bytes) ?: return null

		val icons = mutableMapOf<String, ImageBitmap>()
		for (key in dataMap.keySet()) {
			val asset = dataMap.getAsset(key) ?: continue
			loadImage(asset)?.let { icons[key] = it }
		}
		return Snapshot(state, icons)
	}

	private suspend fun loadImage(asset: Asset): ImageBitmap? = runCatching {
		dataClient.getFdForAsset(asset).await().inputStream.use { stream ->
			BitmapFactory.decodeStream(stream)?.asImageBitmap()
		}
	}.onFailure { Log.d(TAG, "Reading a turn asset failed", it) }.getOrNull()

	companion object {
		private const val TAG = "OsmAndWear"
	}
}
