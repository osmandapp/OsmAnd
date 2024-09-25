package net.osmand.shared.gpx

import co.touchlab.stately.concurrency.Synchronizable
import co.touchlab.stately.concurrency.synchronize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.osmand.shared.KAsyncTask
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.gpx.GpxDbHelper.GpxDataItemCallbackEx
import net.osmand.shared.gpx.data.TrackFolder
import net.osmand.shared.io.KFile
import net.osmand.shared.util.LoggerFactory

class TrackFolderLoaderTask(
	private val folder: TrackFolder,
	private val listener: LoadTracksListener,
	private val forceLoad: Boolean = false
) : KAsyncTask<Unit, TrackItem, TrackFolder>(true) {

	companion object {
		private val log = LoggerFactory.getLogger("TrackFolderLoaderTask")

		private const val LOG_BATCH_SIZE = 100
		private const val PROGRESS_BATCH_SIZE = 70

		private var cachedRootFolder: TrackFolder? = null
		private var cachedDbItemsVersion = -1
	}

	private var loadingTime = 0L
	private var tracksCounter = 0
	private var taskSync = Synchronizable()
	private var progressSync = Synchronizable()

	private fun shouldLoadFolder(cachedRootFolder: TrackFolder?) =
		forceLoad || cachedRootFolder == null || !folder.isRootFolder || cachedDbItemsVersion != GpxDbHelper.getItemsVersion()

	override fun onPreExecute() {
		if (shouldLoadFolder(cachedRootFolder)) listener.loadTracksStarted()
	}

	override fun onProgressUpdate(vararg values: TrackItem) {
		if (shouldLoadFolder(cachedRootFolder)) listener.loadTracksProgress(*values)
	}

	override suspend fun doInBackground(vararg params: Unit): TrackFolder {
		val cachedRootFolder = cachedRootFolder
		if (!shouldLoadFolder(cachedRootFolder)) return cachedRootFolder!!

		val start = currentTimeMillis()
		log.info("Start loading tracks in ${folder.getDirName()}")

		folder.clearData()
		loadingTime = currentTimeMillis()

		val progress = mutableListOf<TrackItem>()
		loadGPXFolder(folder, progress)
		if (folder.isRootFolder) {
			Companion.cachedRootFolder = TrackFolder(folder)
			cachedDbItemsVersion = GpxDbHelper.getItemsVersion()
		}

		listener.tracksLoaded(folder)
		log.info("Finished loading tracks. Took ${currentTimeMillis() - start}ms")

		return folder
	}

	override fun onPostExecute(result: TrackFolder) {
		val cachedRootFolder = cachedRootFolder
		if (result === cachedRootFolder) folder.update(cachedRootFolder)
		resetCachedData(folder)
		listener.loadTracksFinished(folder)
		SmartFolderHelper.notifyUpdateListeners()
	}

	private suspend fun loadGPXFolder(
		rootFolder: TrackFolder,
		progress: MutableList<TrackItem>
	) {
		val trackItems = mutableListOf<TrackItem>()
		withContext(Dispatchers.IO) {
			scanFolder(rootFolder, progress, trackItems)
		}
		SmartFolderHelper.addTrackItemsToSmartFolder(trackItems)

		resetCachedData(rootFolder)
	}

	private fun resetCachedData(rootFolder: TrackFolder) {
		for (subFolder in rootFolder.getFlattenedSubFolders()) subFolder.resetCachedData()
		rootFolder.resetCachedData()
	}

	private suspend fun scanFolder(
		folder: TrackFolder,
		progress: MutableList<TrackItem>,
		trackItems: MutableList<TrackItem>
	): Unit = coroutineScope {
		val dir = folder.getDirFile()
		val files = dir.listFiles()
		if (files.isNullOrEmpty()) return@coroutineScope

		val fileBatches: List<List<KFile>> = files.chunked(PROGRESS_BATCH_SIZE)
		fileBatches.forEach { batch ->
			launch {
				log.info("Loading track file batch = ${batch.size}")
				val batchTrackItems = mutableListOf<TrackItem>()
				for (file in batch) {
					if (isCancelled()) return@launch

					if (file.isDirectory()) {
						val subfolder = TrackFolder(file, folder)
						launch {
							log.info("Loading track subfolder = $subfolder")
							scanFolder(subfolder, progress, trackItems)
							taskSync.synchronize { folder.addSubFolder(subfolder) }
						}
					} else if (GpxHelper.isGpxFile(file)) {
						val item = TrackItem(file)
						item.dataItem = getDataItem(item, file)
						taskSync.synchronize { trackItems.add(item) }
						batchTrackItems.add(item)

						progressSync.synchronize {
							progress.add(item)
							if (progress.size > PROGRESS_BATCH_SIZE) {
								publishProgress(*progress.toTypedArray())
								progress.clear()
							}

							tracksCounter++
							if (tracksCounter % LOG_BATCH_SIZE == 0) {
								val endTime = currentTimeMillis()
								log.info("Loading $LOG_BATCH_SIZE tracks. Took ${endTime - loadingTime}ms")
								loadingTime = endTime
							}
						}
					}
				}
				taskSync.synchronize { folder.addTrackItems(batchTrackItems) }
			}
		}
		if (isCancelled()) return@coroutineScope
	}

	private fun getDataItem(trackItem: TrackItem, file: KFile): GpxDataItem? {
		val callback = object : GpxDataItemCallbackEx {
			override fun isCancelled(): Boolean = this@TrackFolderLoaderTask.isCancelled()

			override fun onGpxDataItemReady(item: GpxDataItem) {}

			override fun onGpxDataItemReady(item: GpxDataItem, lastItem: Boolean) {
				if (file == item.file) {
					trackItem.dataItem = item
				}
				if (lastItem && !isRunning()) {
					resetCachedData(folder)
					listener.deferredLoadTracksFinished(folder)
				}
			}
		}
		return GpxDbHelper.getItem(file, callback)
	}

	fun interface LoadTracksListener {
		fun loadTracksStarted() {}
		fun loadTracksProgress(vararg items: TrackItem) {}
		fun tracksLoaded(folder: TrackFolder) {}
		fun loadTracksFinished(folder: TrackFolder)
		fun deferredLoadTracksFinished(folder: TrackFolder) {}
	}
}