package net.osmand.shared.gpx

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.concurrency.AtomicBoolean
import co.touchlab.stately.concurrency.AtomicInt
import co.touchlab.stately.concurrency.Synchronizable
import co.touchlab.stately.concurrency.synchronize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.osmand.shared.data.StringIntPair
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.gpx.GpxReader.GpxReaderAdapter
import net.osmand.shared.io.KFile
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.PlatformUtil


object GpxDbHelper : GpxReaderAdapter {
	val log = LoggerFactory.getLogger("GpxDbHelper")

	private val database: GpxDatabase by lazy { GpxDatabase() }

	private val dirItems = ConcurrentMutableMap<KFile, GpxDirItem>()
	private val dataItems = ConcurrentMutableMap<KFile, GpxDataItem>()
	private var itemsVersion = AtomicInt(0)

	// Queued reads must not retain metadata snapshots created before cache hydration.
	private val readingFiles = mutableSetOf<KFile>()
	private val resolvingFiles = mutableSetOf<KFile>()
	private val readingItemsCallbacks = mutableMapOf<KFile, MutableList<GpxDataItemCallback>?>()

	private const val READER_TASKS_LIMIT = 4
	private var readers = mutableListOf<GpxReader>()
	private var readerSync = Synchronizable()

	private val initialized = AtomicBoolean(false)
	private var reconciliationRunning = AtomicBoolean(false)
	private var reconciliationComplete = AtomicBoolean(false)
	private var reconciliationAttempted = AtomicBoolean(false)

	fun interface GpxDataItemCallback {
		fun isCancelled(): Boolean = false
		fun onGpxDataItemReady(item: GpxDataItem)
	}

	interface GpxDataItemCallbackEx : GpxDataItemCallback {
		fun onGpxDataItemReady(item: GpxDataItem, lastItem: Boolean)
	}

	fun loadItemsBlocking() = runBlocking { loadItems() }
	suspend fun loadItems() {
		loadGpxItems()
		loadGpxDirItems()
		initialized.value = true
	}

	private suspend fun loadGpxItems() {
		val start = currentTimeMillis()
		val items = readItems()
		putToCacheBulk(items.associateBy { it.file })
		log.info("Hydrated GPX metadata cache in ${currentTimeMillis() - start} ms, ${items.size} items")
	}

	private fun loadGpxDirItems() {
		val start = currentTimeMillis()
		val items = readDirItems()
		items.forEach { putToCache(it) }
		log.info("Hydrated GPX directory metadata in ${currentTimeMillis() - start} ms, items count ${dirItems.size}")
	}

	fun isInitialized() = initialized.value

	fun isFilesystemReconciliationRunning() = reconciliationRunning.value

	fun isFilesystemReconciliationComplete() = reconciliationComplete.value

	fun startFilesystemReconciliation() {
		if (!initialized.value || reconciliationAttempted.value ||
				!reconciliationRunning.compareAndSet(expected = false, new = true)) {
			return
		}
		reconciliationAttempted.value = true
		CoroutineScope(Dispatchers.IO).launch {
			try {
				reconcileFilesystem()
				reconciliationComplete.value = true
			} catch (error: Throwable) {
				log.error("Failed to reconcile GPX database with filesystem", error)
			} finally {
				reconciliationRunning.value = false
				startQueuedReaders()
			}
		}
	}

	private fun reconcileFilesystem() {
		val start = currentTimeMillis()
		val gpxRoot = normalizePath(PlatformUtil.getOsmAndContext().getGpxDir().path())
		val dataSnapshot = dataItems.entries.associate { it.key to it.value }
		val dirSnapshot = dirItems.entries.associate { it.key to it.value }
		val missingFiles = mutableSetOf<KFile>()
		val modifiedItems = mutableListOf<Pair<KFile, GpxDataItem>>()

		dataSnapshot.forEach { (file, item) ->
			val insideRoot = isInsideGpxRoot(file, gpxRoot)
			if (!insideRoot || !file.exists()) {
				missingFiles.add(file)
			} else {
				val actualModifiedTime = file.lastModified()
				val storedModifiedTime = item.getParameter<Long>(GpxParameter.FILE_LAST_MODIFIED_TIME)
				if (storedModifiedTime != actualModifiedTime
						|| GpxDbUtils.isAnalyseNeeded(item, actualModifiedTime)) {
					modifiedItems.add(file to item)
				}
			}
		}

		val filesToRemove = missingFiles.filterTo(mutableSetOf()) { file ->
			val snapshotItem = dataSnapshot[file]
			dataItems[file] === snapshotItem &&
					(!isInsideGpxRoot(file, gpxRoot) || !file.exists())
		}
		var removedFiles = 0
		if (filesToRemove.isNotEmpty()) {
			if (database.remove(filesToRemove)) {
				removeFromCacheBulk(filesToRemove)
				removedFiles = filesToRemove.size
			} else {
				log.error("Failed to remove missing GPX rows during filesystem reconciliation")
			}
		}

		var removedDirRows = 0
		dirSnapshot.forEach { (file, snapshotItem) ->
			if (dirItems[file] === snapshotItem &&
					(!isInsideGpxRoot(file, gpxRoot) || !file.exists())) {
				if (database.remove(file)) {
					removeFromCache(file)
					removedDirRows++
				}
			}
		}

		var queuedModifiedItems = 0
		modifiedItems.forEach { (file, item) ->
			if (dataItems[file] === item && file.exists()) {
				val actualModifiedTime = file.lastModified()
				if (item.getParameter<Long>(GpxParameter.FILE_LAST_MODIFIED_TIME) != actualModifiedTime
						|| GpxDbUtils.isAnalyseNeeded(item, actualModifiedTime)) {
					readGpxItem(file, null, false)
					queuedModifiedItems++
				}
			}
		}
		log.info(
			"Reconciled GPX filesystem in ${currentTimeMillis() - start} ms: " +
					"checked=${dataSnapshot.size}, removed=$removedFiles, " +
					"removedDirRows=$removedDirRows, queuedModified=$queuedModifiedItems"
		)
	}

	private fun isInsideGpxRoot(file: KFile, normalizedRoot: String): Boolean {
		val path = normalizePath(file.path())
		return hasPathPrefix(path, normalizedRoot)
	}

	internal fun isPathInsideGpxRoot(path: String, root: String): Boolean {
		return hasPathPrefix(normalizePath(path), normalizePath(root))
	}

	private fun hasPathPrefix(path: String, normalizedRoot: String): Boolean {
		return path == normalizedRoot || if (normalizedRoot == "/") {
			path.startsWith(normalizedRoot)
		} else {
			path.startsWith("$normalizedRoot/")
		}
	}

	private fun normalizePath(path: String): String {
		val normalized = path.replace('\\', '/')
		val absolute = normalized.startsWith('/')
		val segments = mutableListOf<String>()
		normalized.split('/').forEach { segment ->
			when {
				segment.isEmpty() || segment == "." -> Unit
				segment == ".." && segments.isNotEmpty() && segments.last() != ".." ->
					segments.removeAt(segments.lastIndex)
				else -> segments.add(segment)
			}
		}
		val prefix = if (absolute) "/" else ""
		return prefix + segments.joinToString("/")
	}

	fun getItemsVersion() = itemsVersion.get()

	private fun putToCache(item: DataItem) {
		val file = item.file
		when (item) {
			is GpxDataItem -> dataItems[file] = item
			is GpxDirItem -> dirItems[file] = item
		}
		itemsVersion.incrementAndGet()
	}

	private fun removeFromCache(file: KFile) {
		if (GpxDbUtils.isGpxFile(file)) {
			dataItems.remove(file)
		} else {
			dirItems.remove(file)
		}
		itemsVersion.incrementAndGet()
	}

	private fun putToCacheBulk(itemsToCache: Map<KFile, GpxDataItem>) {
		dataItems.putAll(itemsToCache)
		itemsVersion.incrementAndGet()
	}

	private fun removeFromCacheBulk(filesToRemove: Set<KFile>) {
		dataItems.keys.removeAll(filesToRemove)
		itemsVersion.incrementAndGet()
	}

	fun rename(currentFile: KFile, newFile: KFile): Boolean {
		val success = database.rename(currentFile, newFile)
		if (success) {
			val newItem = GpxDataItem(newFile)
			val oldItem = dataItems[currentFile]
			if (oldItem != null) {
				newItem.copyData(oldItem)
			}
			putToCache(newItem)
			removeFromCache(currentFile)
			updateDefaultAppearance(newItem, false)
		}
		return success
	}

	fun updateDataItem(item: DataItem): Boolean {
		val res = database.updateDataItem(item)
		putToCache(item)
		return res
	}

	fun persistAnalyzedItem(item: GpxDataItem): GpxDataItem {
		val persistedItem = database.persistAnalyzedItem(item)
		val cachedItem = dataItems[item.file]
		val result = when {
			persistedItem == null -> cachedItem ?: item
			cachedItem != null -> cachedItem.apply { copyAnalysisData(persistedItem) }
			else -> persistedItem
		}
		if (persistedItem != null) {
			putToCache(result)
		}
		return result
	}

	fun updateDataItemParameter(
		item: DataItem,
		parameter: GpxParameter,
		value: Any?
	): Boolean {
		item.setParameter(parameter, value)
		val res = database.updateDataItemParameter(item, parameter, value)
		if (res && parameter.isAnalysisRecalculationNeeded()) {
			// Reset DATA_VERSION to force recalculation of analysis
			item.setParameter(GpxParameter.DATA_VERSION, 0)
			item.increaseAnalysisParametersVersion()
			database.updateDataItemParameter(item, GpxParameter.DATA_VERSION, 0)
		}
		putToCache(item)
		return res
	}

	fun remove(file: KFile): Boolean {
		val res = database.remove(file)
		removeFromCache(file)
		return res
	}

	fun remove(files: Collection<KFile>): Boolean {
		val res = database.remove(files)
		removeFromCacheBulk(files.toSet())
		return res
	}

	fun remove(item: DataItem): Boolean {
		val file = item.file
		val res = database.remove(file)
		removeFromCache(file)
		return res
	}

	fun add(item: GpxDataItem): Boolean {
		val res = database.add(item)
		putToCache(item)
		updateDefaultAppearance(item, true)
		return res
	}

	fun add(item: GpxDirItem): Boolean {
		val res = database.add(item)
		putToCache(item)
		return res
	}

	fun getItems() = dataItems.values.toList()

	fun getRecentlyModifiedItems(limit: Int) = database.getRecentlyModifiedItems(limit)

	fun getDirItems() = dirItems.values.toList()

	private suspend fun readItems(): List<GpxDataItem> = database.getGpxDataItems()

	private fun readDirItems(): List<GpxDirItem> = database.getGpxDirItems()

	fun getStringIntItemsCollection(
		columnName: String,
		includeEmptyValues: Boolean,
		sortByName: Boolean,
		sortDescending: Boolean
	): List<StringIntPair> {
		return database.getStringIntItemsCollection(
			columnName,
			includeEmptyValues,
			sortByName,
			sortDescending
		)
	}

	fun getTracksMinCreateDate(): Long = database.getTracksMinCreateDate()

	fun getMaxParameterValue(parameter: GpxParameter): String {
		return database.getColumnMaxValue(parameter)
	}

	fun getItem(file: KFile): GpxDataItem? {
		return getItem(file, null)
	}

	fun getItem(file: KFile, readIfNeeded: Boolean): GpxDataItem? {
		return getItem(file, null, readIfNeeded)
	}

	fun getGpxDirItem(item: GpxDataItem) = item.file.getParentFile()?.let { getGpxDirItem(it) }

	fun getGpxDirItem(file: KFile): GpxDirItem {
		var item = dirItems[file]
		if (item == null) {
			item = database.getGpxDirItem(file)

			if (item != null) {
				putToCache(item)
			}
		}
		if (item == null) {
			item = GpxDirItem(file)
			add(item)
		}
		return item
	}

	fun getItem(file: KFile, callback: GpxDataItemCallback?): GpxDataItem? {
		return getItem(file, callback, true)
	}

	fun getItem(file: KFile, callback: GpxDataItemCallback?, readIfNeeded: Boolean): GpxDataItem? {
		if (file.isPathEmpty()) {
			return null
		}
		val item = dataItems[file]
		if (item != null && !file.exists()) {
			return null
		}
		if (readIfNeeded && GpxDataItem.isRegularTrack(file)) {
			val shouldCheckAnalysis = callback != null || !isReading(file)
			if (shouldCheckAnalysis && GpxDbUtils.isAnalyseNeeded(item)) {
				val reconciliationFinished = reconciliationAttempted.value && !reconciliationRunning.value
				readGpxItem(file, callback, reconciliationFinished)
			}
		}
		return item
	}

	fun hasGpxDataItem(file: KFile): Boolean {
		return dataItems.containsKey(file)
	}

	fun hasGpxDirItem(file: KFile): Boolean {
		return dirItems.containsKey(file)
	}

	fun getSplitItemsBlocking(): List<GpxDataItem> = runBlocking { getSplitItems() }
	suspend fun getSplitItems(): List<GpxDataItem> {
		return readItems().filter {
			it.getAppearanceParameter<Int>(GpxParameter.SPLIT_TYPE) != 0
		}
	}

	private fun updateDefaultAppearance(item: GpxDataItem, updateExistingValues: Boolean) {
		val file = item.file
		val dir = file.getParentFile()
		if (dir != null) {
			val dirItem = getGpxDirItem(dir)
			for (parameter in GpxParameter.getAppearanceParameters()) {
				val value: Any? = item.getParameter(parameter)
				val defaultValue: Any? = dirItem.getParameter(parameter)
				if (defaultValue != null && (updateExistingValues || value == null)) {
					item.setParameter(parameter, defaultValue)
				}
			}
			updateDataItem(item)
		}
	}

	fun isReading(): Boolean = readerSync.synchronize { readers.isNotEmpty() }

	fun isReading(file: KFile): Boolean =
		readerSync.synchronize {
			readingFiles.contains(file) || resolvingFiles.contains(file) || readers.any { it.isReading(file) }
		}

	private fun readGpxItem(
		file: KFile,
		callback: GpxDataItemCallback?,
		startReader: Boolean = true
	) {
		readerSync.synchronize {
			if (callback != null) {
				readingItemsCallbacks.getOrPut(file) { mutableListOf() }?.apply { add(callback) }
			}
			val alreadyReading = readingFiles.contains(file) || resolvingFiles.contains(file) ||
					readers.any { it.isReading(file) }
			if (!alreadyReading) {
				readingFiles.add(file)
				if (startReader && readers.size < READER_TASKS_LIMIT) {
					startReading()
				}
			}
		}
	}

	private fun startQueuedReaders() {
		readerSync.synchronize {
			while (readingFiles.isNotEmpty() && readers.size < READER_TASKS_LIMIT) {
				startReading()
			}
		}
	}

	private fun startReading() {
		readerSync.synchronize {
			readers.add(GpxReader(this).apply { execute() })
		}
	}

	private fun stopReading() {
		readerSync.synchronize {
			readers.forEach { it.cancel() }
			readers.clear()
		}
	}

	fun getGPXDatabase(): GpxDatabase = database

	override fun pullNextFileItem(
		action: ((Pair<KFile, GpxDataItem?>?) -> Unit)?
	): Pair<KFile, GpxDataItem?>? {
		val file = readerSync.synchronize {
			readingFiles.firstOrNull()?.also {
				readingFiles.remove(it)
				resolvingFiles.add(it)
			}
		}
		var result: Pair<KFile, GpxDataItem?>? = null
		try {
			result = file?.let {
				val item = dataItems[it] ?: try {
					database.getGpxDataItem(it)
				} catch (e: Exception) {
					log.error("Failed to resolve queued GPX item ${it.path()}", e)
					null
				}
				it to item
			}
			return result
		} finally {
			readerSync.synchronize {
				try {
					action?.invoke(result)
				} finally {
					if (file != null) {
						resolvingFiles.remove(file)
					}
				}
			}
		}
	}

	override fun onGpxDataItemRead(item: GpxDataItem) {
		putGpxDataItemToSmartFolder(item)
	}

	private fun putGpxDataItemToSmartFolder(item: GpxDataItem) {
		val trackItem = TrackItem(item.file).apply { dataItem = item }
		PlatformUtil.getOsmAndContext().getSmartFolderHelper().addTrackItemToSmartFolder(trackItem)
	}

	override fun onProgressUpdate(vararg dataItems: GpxDataItem) {
		readerSync.synchronize {
			dataItems.forEach { item ->
				val callbacks = readingItemsCallbacks.remove(item.file)
				callbacks?.forEach { callback ->
					if (callback.isCancelled()) {
						stopReading()
					} else {
						callback.onGpxDataItemReady(item)
						if (callback is GpxDataItemCallbackEx)
							callback.onGpxDataItemReady(item, readingItemsCallbacks.isEmpty())
					}
				}
			}
		}
	}

	override fun onReadingCancelled() {
		readerSync.synchronize {
			readingFiles.clear()
			resolvingFiles.clear()
			readingItemsCallbacks.clear()
		}
	}

	override fun onReadingFinished(reader: GpxReader, cancelled: Boolean) {
		readerSync.synchronize {
			if (readingFiles.isNotEmpty() && readers.size < READER_TASKS_LIMIT && !cancelled) {
				startReading()
			}
			readers.remove(reader)
		}
	}
}
