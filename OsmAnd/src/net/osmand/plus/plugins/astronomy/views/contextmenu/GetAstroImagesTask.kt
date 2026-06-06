package net.osmand.plus.plugins.astronomy.views.contextmenu

import android.net.TrafficStats
import android.os.AsyncTask
import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.shared.wiki.WikiCoreHelper
import net.osmand.shared.wiki.WikiImage
import org.apache.commons.logging.Log

class GetAstroImagesTask(
	private val app: OsmandApplication,
	private val wikidataId: String,
	private val listener: GetImagesListener?,
	private val networkResponseListener: WikiCoreHelper.NetworkResponseListener?
) : AsyncTask<Void, Void, List<WikiImage>?>() {

	companion object {
		val LOG: Log? = PlatformUtil.getLog(GetAstroImagesTask::class.java)
		const val GET_IMAGE_CARD_THREAD_ID = 10105
	}

	override fun onPreExecute() {
		listener?.onTaskStarted()
	}

	override fun doInBackground(vararg voids: Void?): List<WikiImage>? {
		TrafficStats.setThreadStatsTag(GET_IMAGE_CARD_THREAD_ID)
		return try {
			WikiCoreHelper.getAstroImageList(wikidataId, networkResponseListener)
		} catch (e: Exception) {
			LOG?.error(e)
			null
		}
	}

	override fun onPostExecute(images: List<WikiImage>?) {
		listener?.onFinish(wikidataId, images)
	}

	interface GetImagesListener {
		fun onTaskStarted()
		fun onFinish(wikidataId: String, images: List<WikiImage>?)
	}
}