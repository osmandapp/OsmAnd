package net.osmand.shared.exploreplaces

import net.osmand.shared.KAsyncTask
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.wiki.WikiCoreHelper
import net.osmand.shared.wiki.WikiCoreHelper.OsmandApiFeatureData

class GetExplorePlacesImagesTask(
	val mapRect: KQuadRect,
	val zoom: Int,
	val languages: List<String>,
	val listener: GetImageCardsListener
) : KAsyncTask<Unit, Unit, List<OsmandApiFeatureData>>(true) {

	companion object {
		private val LOG = LoggerFactory.getLogger("GetExplorePlacesImagesTask")
	}

	override fun onPreExecute() {
		super.onPreExecute()
		listener.onTaskStarted()
	}

	override suspend fun doInBackground(vararg params: Unit): List<OsmandApiFeatureData> {
		var list: List<OsmandApiFeatureData> = emptyList()
		LOG.debug("Start loading nearby places")
		try {
			val langs = languages.joinToString(",")
			list = WikiCoreHelper.getExploreImageList(mapRect, zoom, langs)
		} catch (error: Exception) {
			LOG.debug("Load nearby images error $error")
		}
		LOG.debug("Finish loading nearby places. Found ${list.size} items")
		return list
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