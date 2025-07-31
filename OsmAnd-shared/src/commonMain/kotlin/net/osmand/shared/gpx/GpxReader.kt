package net.osmand.shared.gpx

import kotlinx.coroutines.delay
import net.osmand.shared.KAsyncTask
import net.osmand.shared.api.SQLiteAPI.SQLiteConnection
import net.osmand.shared.gpx.GpxTrackAnalysis.Companion.ANALYSIS_VERSION
import net.osmand.shared.io.KFile
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.PlatformUtil

class GpxReader(private val adapter: GpxReaderAdapter)
	: KAsyncTask<Unit, GpxDataItem, Unit>(true) {

	companion object {
		private val log = LoggerFactory.getLogger("GpxReader")

		// TODO: Move to GpxAppearanceInfo.kt
		const val MIN_VERTICAL_EXAGGERATION: Float = 1.0f
		const val MAX_VERTICAL_EXAGGERATION: Float = 3.0f
	}

	private val database: GpxDatabase = GpxDbHelper.getGPXDatabase()
	private var analyser = PlatformUtil.getTrackPointsAnalyser()
	private var currentFile: KFile? = null
	private var currentItem: GpxDataItem? = null

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
		var filesCount = 0
		try {
			var file: KFile?
			var item: GpxDataItem?
			pullNextFileItem()
			file = currentFile
			item = currentItem
			while (file != null && !isCancelled()) {
				if (GpxDbUtils.isAnalyseNeeded(item)) {
					item = updateGpxDataItem(item, file)
				}
				if (item != null) {
					adapter.onGpxDataItemRead(item)
					publishProgress(item)
				}

				pullNextFileItem()
				file = currentFile
				item = currentItem
				filesCount++
			}
		} catch (e: Exception) {
			log.error(e.message)
		}
	}

	private fun pullNextFileItem() {
		adapter.pullNextFileItem {
			currentFile = it?.first
			currentItem = it?.second
		}
	}

	override fun onProgressUpdate(vararg values: GpxDataItem) {
		adapter.onProgressUpdate(*values)
	}

	override fun onCancelled() {
		adapter.onReadingCancelled()
	}

	override fun onPostExecute(result: Unit) {
		adapter.onReadingFinished(this, isCancelled())
	}

	private fun updateGpxDataItem(item: GpxDataItem?, file: KFile): GpxDataItem {
		val gpxFile = GpxUtilities.loadGpxFile(file, null, false)
		val updatedItem = item ?: GpxDataItem(file)
		if (gpxFile.error == null) {
			updatedItem.setAnalysis(gpxFile.getAnalysis(file.lastModified(), null, null, analyser))
			if (!updatedItem.isRegularTrack()) {
				return updatedItem
			}
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

			val conn = database.openConnection(false)
			if (conn != null) {
				try {
					if (database.isDataItemExists(file, conn)) {
						GpxDbHelper.updateDataItem(updatedItem)
					} else {
						GpxDbHelper.insertDataItem(updatedItem, conn)
					}
				} catch (e: Exception) {
					log.error(e.message)
				} finally {
					conn.close()
				}
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

	fun isReading(): Boolean = isRunning()

	fun isReading(file: KFile): Boolean = currentFile == file

	interface GpxReaderAdapter {
		fun pullNextFileItem(action: ((Pair<KFile, GpxDataItem>?) -> Unit)? = null): Pair<KFile, GpxDataItem>?

		fun onGpxDataItemRead(item: GpxDataItem) {}
		fun onProgressUpdate(vararg dataItems: GpxDataItem) {}
		fun onReadingCancelled() {}
		fun onReadingFinished(reader: GpxReader, cancelled: Boolean) {}
	}
}