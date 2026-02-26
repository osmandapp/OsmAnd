package net.osmand.plus.plugins.astro.views.contextmenu

import android.net.TrafficStats
import android.os.AsyncTask
import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.mapcontextmenu.gallery.ImageCardsHolder
import net.osmand.shared.wiki.WikiCoreHelper
import net.osmand.shared.wiki.WikiImage
import org.apache.commons.logging.Log

class GetAstroImagesTask(
	val app: OsmandApplication, val holder: ImageCardsHolder, val wikidataId: String,
	val getImageCardsListener: GetImageCardsListener?,
	val networkResponseListener: WikiCoreHelper.NetworkResponseListener?
) : AsyncTask<Void, Void, List<WikiImage>?>() {

	companion object {
		val LOG: Log? = PlatformUtil.getLog(GetAstroImagesTask::class.java)
		const val GET_IMAGE_CARD_THREAD_ID = 10105
	}


	override fun onPreExecute() {
		getImageCardsListener?.onTaskStarted()
	}

	override fun doInBackground(vararg voids: Void?): List<WikiImage>? {
		TrafficStats.setThreadStatsTag(GET_IMAGE_CARD_THREAD_ID)
		try {
			val list = WikiCoreHelper.getAstroImageList(wikidataId, networkResponseListener)
			return list
		} catch (e: Exception) {
			LOG?.error(e)
		}

		return null
	}

	override fun onPostExecute(holder: List<WikiImage>?) {
		getImageCardsListener?.onFinish(holder)
	}

	interface GetImageCardsListener {
		fun onTaskStarted()

		fun onFinish(images: List<WikiImage>?)
	}
}