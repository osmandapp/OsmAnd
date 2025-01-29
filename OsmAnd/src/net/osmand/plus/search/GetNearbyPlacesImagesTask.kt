package net.osmand.plus.search

import android.net.TrafficStats
import net.osmand.ResultMatcher
import net.osmand.binary.BinaryMapIndexReader
import net.osmand.binary.ObfConstants
import net.osmand.data.Amenity
import net.osmand.data.QuadRect
import net.osmand.plus.OsmandApplication
import net.osmand.shared.KAsyncTask
import net.osmand.shared.util.LoggerFactory
import net.osmand.wiki.WikiCoreHelper
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData
import java.util.Collections

class GetNearbyPlacesImagesTask(
	val app: OsmandApplication,
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

		val amenities: List<Amenity> = app.resourceManager.searchAmenities(
			BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER,
			mapRect.top, mapRect.left, mapRect.bottom, mapRect.right,
			-1, true,
			object : ResultMatcher<Amenity?> {
				override fun publish(amenity: Amenity?): Boolean {
					var idFound = false
					val id = ObfConstants.getOsmObjectId(amenity)
					for (data in wikimediaImageList) {
						if(data.properties.osmid == id) {
							data.amenity = amenity
							idFound = true
						}
					}
					return idFound
				}

				override fun isCancelled(): Boolean {
					return false
				}
			})
		LOG.debug("Found ${amenities.size} amenities for nearbyPlaces")
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