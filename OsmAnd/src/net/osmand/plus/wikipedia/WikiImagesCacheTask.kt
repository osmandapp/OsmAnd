package net.osmand.plus.wikipedia

import net.osmand.plus.OsmandApplication
import net.osmand.plus.utils.PicassoUtils
import net.osmand.shared.KAsyncTask
import java.io.IOException

// Reads the size of the Picasso disk cache off the UI thread, clearing the cache first when asked
class WikiImagesCacheTask(
	private val app: OsmandApplication,
	private val clear: Boolean,
	private val listener: CacheSizeListener
) : KAsyncTask<Unit, Unit, Long>(true) {

	override suspend fun doInBackground(vararg params: Unit): Long {
		val picasso = PicassoUtils.getPicasso(app)
		if (clear) {
			picasso.clearAllPicassoCache()
		}
		return try {
			picasso.diskCacheSizeBytes
		} catch (e: IOException) {
			0L
		}
	}

	override fun onPostExecute(result: Long) {
		listener.onCacheSizeCalculated(result)
	}

	fun interface CacheSizeListener {
		fun onCacheSizeCalculated(sizeBytes: Long)
	}
}
