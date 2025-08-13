package net.osmand.plus.search

import android.net.TrafficStats
import net.osmand.plus.OsmandApplication
import net.osmand.shared.KAsyncTask
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.util.LoggerFactory
import net.osmand.wiki.WikiCoreHelper
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData

class GetExplorePlacesImagesTask(
	val app: OsmandApplication,
	val mapRect: KQuadRect,
	val zoom: Int,
	val languages: List<String>,
	val listener: GetImageCardsListener
) : KAsyncTask<Unit, Unit, List<OsmandApiFeatureData>?>(true) {

	private val LOG = LoggerFactory.getLogger("GetNearbyImagesTask")

	private val GET_NEARBY_IMAGES_CARD_THREAD_ID = 10105

	override suspend fun doInBackground(vararg params: Unit): List<OsmandApiFeatureData>? {
		TrafficStats.setThreadStatsTag(GET_NEARBY_IMAGES_CARD_THREAD_ID)
		var wikimediaImageList: List<OsmandApiFeatureData>? = null
		LOG.debug("Start loading nearby places")
		try {
			val langs = languages.joinToString(",")
			wikimediaImageList = WikiCoreHelper.getExploreImageList(mapRect, zoom, langs)
		} catch (error: Exception) {
			LOG.debug("Load nearby images error $error")
		}
		LOG.debug("Finish loading nearby places. Found ${wikimediaImageList?.size} items")
		return wikimediaImageList
	}

	override fun onPostExecute(result: List<OsmandApiFeatureData>?) {
		super.onPostExecute(result)
		listener.onFinish(result)
	}

	interface GetImageCardsListener {
		fun onTaskStarted()
		fun onFinish(result: List<OsmandApiFeatureData>?)
	}
}