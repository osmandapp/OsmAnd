package net.osmand.shared.gpx

import kotlinx.coroutines.delay
import net.osmand.shared.KAsyncTask
import net.osmand.shared.api.SQLiteAPI.SQLiteConnection
import net.osmand.shared.gpx.GpxTrackAnalysis.Companion.ANALYSIS_VERSION
import net.osmand.shared.io.KFile
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.PlatformUtil

class GpxReader(
	private val readingItems: MutableList<KFile>,
	private val readingItemsMap: MutableMap<KFile, GpxDataItem>,
	private val listener: GpxDbReaderCallback?
) : KAsyncTask<Unit, GpxDataItem, Unit>(true) {

	companion object {
		private val log = LoggerFactory.getLogger("GpxReader")

		// TODO: Move to GpxAppearanceInfo.kt
		const val MIN_VERTICAL_EXAGGERATION: Float = 1.0f
		const val MAX_VERTICAL_EXAGGERATION: Float = 3.0f
	}

	private val database: GpxDatabase = GpxDbHelper.getGPXDatabase()
	var file: KFile? = null

	override suspend fun doInBackground(vararg params: Unit) {
		waitForInitialization()
		doReading()
	}

	private suspend fun waitForInitialization() {
		while (!GpxDbHelper.isInitialized() && !isCancelled()) {
			delay(100L)
		}
	}

	private fun doReading() {
		log.info(">>>> start GpxReader ===== ")
		var filesCount = 0
		val conn = database.openConnection(false)
		if (conn != null) {
			try {
				file = readingItems.removeFirstOrNull()
				while (file != null && !isCancelled()) {
					var item = readingItemsMap.remove(file)
					if (GpxDbUtils.isAnalyseNeeded(item)) {
						item = updateGpxDataItem(conn, item, file!!)
					}
					if (item != null) {
						listener?.onGpxDataItemRead(item)
						publishProgress(item)
					}
					file = readingItems.removeFirstOrNull()
					filesCount++
				}
			} catch (e: Exception) {
				log.error(e.message)
			} finally {
				conn.close()
				log.info(">>>> done GpxReader ===== filesCount=$filesCount")
			}
		} else {
			cancel()
		}
	}

	override fun onProgressUpdate(vararg values: GpxDataItem) {
		listener?.onProgressUpdate(*values)
	}

	override fun onCancelled() {
		listener?.onReadingCancelled()
	}

	override fun onPostExecute(result: Unit) {
		listener?.onReadingFinished(this, isCancelled())
	}

	private fun updateGpxDataItem(conn: SQLiteConnection, item: GpxDataItem?, file: KFile): GpxDataItem {
		val gpxFile = GpxUtilities.loadGpxFile(file, null, false)
		val updatedItem = item ?: GpxDataItem(file)
		if (gpxFile.error == null) {
			val analyser = PlatformUtil.getOsmAndContext().getTrackPointsAnalyser()
			updatedItem.setAnalysis(gpxFile.getAnalysis(file.lastModified(), null, null, analyser))
			val creationTime: Long = updatedItem.requireParameter(GpxParameter.FILE_CREATION_TIME)
			if (creationTime <= 0) {
				updatedItem.setParameter(GpxParameter.FILE_CREATION_TIME, GpxUtilities.getCreationTime(gpxFile))
			}
			updatedItem.setParameter(GpxParameter.FILE_LAST_MODIFIED_TIME, file.lastModified())

			val metadata = gpxFile.metadata
			val routeActivity = metadata.getRouteActivity(RouteActivityHelper.getActivities())
			val routeActivityId = routeActivity?.id ?: ""
			updatedItem.setParameter(GpxParameter.ACTIVITY_TYPE, routeActivityId)

			setupNearestCityName(updatedItem)

			val additionalExaggeration: Double =
				updatedItem.requireParameter(GpxParameter.ADDITIONAL_EXAGGERATION)
			if (additionalExaggeration < MIN_VERTICAL_EXAGGERATION ||
				additionalExaggeration > MAX_VERTICAL_EXAGGERATION) {
				updatedItem.setParameter(GpxParameter.ADDITIONAL_EXAGGERATION,
					MIN_VERTICAL_EXAGGERATION.toDouble())
			}
			updatedItem.setParameter(
				GpxParameter.DATA_VERSION,
				GpxDbUtils.createDataVersion(ANALYSIS_VERSION)
			)

			if (database.isDataItemExists(file, conn)) {
				GpxDbHelper.updateDataItem(updatedItem)
			} else {
				GpxDbHelper.insertDataItem(updatedItem, conn)
			}
		}
		return updatedItem
	}

	private fun setupNearestCityName(item: GpxDataItem) {
		val analysis = item.getAnalysis()
		val latLon = analysis?.getLatLonStart()
		if (latLon == null) {
			item.setParameter(GpxParameter.NEAREST_CITY_NAME, "")
		} else {
			PlatformUtil.getOsmAndContext().searchNearestCityName(latLon) { cityName ->
				if (cityName.isNotEmpty()) {
					GpxDbHelper.updateDataItemParameter(item, GpxParameter.NEAREST_CITY_NAME, cityName)
				} else {
					item.setParameter(GpxParameter.NEAREST_CITY_NAME, "")
				}
			}
		}
	}

	fun isReading(): Boolean = readingItems.isNotEmpty() || file != null

	interface GpxDbReaderCallback {
		fun onGpxDataItemRead(item: GpxDataItem)
		fun onProgressUpdate(vararg dataItems: GpxDataItem)
		fun onReadingCancelled()
		fun onReadingFinished(reader: GpxReader, cancelled: Boolean)
	}
}