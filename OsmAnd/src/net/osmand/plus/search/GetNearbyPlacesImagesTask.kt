package net.osmand.plus.search

import android.net.TrafficStats
import net.osmand.data.QuadRect
import net.osmand.shared.KAsyncTask
import net.osmand.shared.util.LoggerFactory
import net.osmand.wiki.WikiCoreHelper
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData
import java.util.Collections

class GetNearbyPlacesImagesTask(
	val mapRect: QuadRect,
	val zoom: Int,
	val locale: String,
	val listener: GetImageCardsListener) :
	KAsyncTask<Unit, Unit, List<OsmandApiFeatureData>>(true) {
	private val LOG = LoggerFactory.getLogger("GetNearbyImagesTask")

	private val GET_NEARBY_IMAGES_CARD_THREAD_ID = 10105

	override suspend fun doInBackground(vararg params: Unit): List<OsmandApiFeatureData> {
		TrafficStats.setThreadStatsTag(GET_NEARBY_IMAGES_CARD_THREAD_ID)
		var wikimediaImageList = Collections.emptyList<OsmandApiFeatureData>()
		LOG.debug("Start loading nearby places")
		try {
			wikimediaImageList = WikiCoreHelper.getExploreImageList(mapRect, zoom, locale)
		} catch (error: Exception) {
			LOG.debug("Load nearby images error $error")
		}
		LOG.debug("Finish loading nearby places. Found ${wikimediaImageList.size} items")
		return wikimediaImageList
	}

	override fun onPostExecute(result: List<OsmandApiFeatureData>) {
		super.onPostExecute(result)
		listener.onFinish(result)
	}

	interface GetImageCardsListener {
		fun onTaskStarted()
		fun onFinish(result: List<OsmandApiFeatureData>)
	}
}