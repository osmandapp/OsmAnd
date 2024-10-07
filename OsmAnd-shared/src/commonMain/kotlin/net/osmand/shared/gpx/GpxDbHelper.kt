package net.osmand.shared.gpx

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.concurrency.AtomicInt
import co.touchlab.stately.concurrency.Synchronizable
import co.touchlab.stately.concurrency.synchronize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import net.osmand.shared.api.SQLiteAPI.SQLiteConnection
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

	private val readingItemsMap = mutableMapOf<KFile, GpxDataItem>()
	private val readingItemsCallbacks = mutableMapOf<KFile, MutableList<GpxDataItemCallback>?>()

	private const val READER_TASKS_LIMIT = 4
	private var readers = mutableListOf<GpxReader>()
	private var readerSync = Synchronizable()

	private var initialized: Boolean = false

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
		initialized = true
	}

	private suspend fun loadGpxItems() {
		val start = currentTimeMillis()
		val items = readItems()
		val startEx = currentTimeMillis()
		val fileExistenceMap = getFileExistenceMap(items)
		log.info("Time to getFileExistenceMap ${currentTimeMillis() - startEx} ms, ${items.size} items")

		val itemsToCache = mutableMapOf<KFile, GpxDataItem>()
		val itemsToRemove = mutableSetOf<KFile>()
		items.forEach { item ->
			val file = item.file
			if (fileExistenceMap[file] == true) {
				itemsToCache[file] = item
			} else {
				itemsToRemove.add(file)
			}
		}
		putToCacheBulk(itemsToCache)
		removeFromCacheBulk(itemsToRemove)
		database.remove(itemsToRemove)
		log.info("Time to loadGpxItems ${currentTimeMillis() - start} ms, ${items.size} items")
	}

	private suspend fun getFileExistenceMap(
		items: List<GpxDataItem>,
		batchSize: Int = 100
	): Map<KFile, Boolean> = coroutineScope {
		val gpxPath = PlatformUtil.getOsmAndContext().getGpxDir().path()
		items.chunked(batchSize).map { batch ->
			async(Dispatchers.IO) { batch.associate {
				it.file to (it.file.exists() && it.file.path().startsWith(gpxPath)) } }
		}.awaitAll().fold(mutableMapOf()) { acc, map -> acc.apply { putAll(map) } }
	}

	private fun loadGpxDirItems() {
		val start = currentTimeMillis()
		val items = readDirItems()
		items.forEach { item ->
			val file = item.file
			if (file.exists()) {
				putToCache(item)
			} else {
				remove(file)
			}
		}
		log.info("Time to loadGpxDirItems ${currentTimeMillis() - start} ms, items count ${dirItems.size}")
	}

	fun isInitialized() = initialized

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

	fun insertDataItem(item: DataItem, conn: SQLiteConnection) {
		database.insertItem(item, conn)
		putToCache(item)
	}

	fun updateDataItemParameter(
		item: DataItem,
		parameter: GpxParameter,
		value: Any?
	): Boolean {
		item.setParameter(parameter, value)
		val res = database.updateDataItemParameter(item, parameter, value)
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

	fun getGpxDirItem(file: KFile): GpxDirItem {
		var item = dirItems[file]
		if (item == null) {
			item = database.getGpxDirItem(file)
		}
		if (item == null) {
			item = GpxDirItem(file)
			add(item)
		}
		return item
	}

	fun getItem(file: KFile, callback: GpxDataItemCallback?): GpxDataItem? {
		if (file.isPathEmpty()) {
			return null
		}
		val item = dataItems[file]
		if (GpxDbUtils.isAnalyseNeeded(item) && GpxDataItem.isRegularTrack(file)) {
			readGpxItem(file, item, callback)
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

	private fun isReading(file: KFile): Boolean =
		readerSync.synchronize { readingItemsMap.contains(file) || readers.any { it.isReading(file) } }

	private fun readGpxItem(file: KFile, item: GpxDataItem?, callback: GpxDataItemCallback?) {
		readerSync.synchronize {
			if (callback != null) {
				readingItemsCallbacks.getOrPut(file) { mutableListOf() }?.apply { add(callback) }
			}
			if (!isReading(file)) {
				readingItemsMap[file] = item ?: GpxDataItem(file)
				if (readers.size < READER_TASKS_LIMIT) {
					startReading()
				}
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

	override fun pullNextFileItem(action: ((Pair<KFile, GpxDataItem>?) -> Unit)?): Pair<KFile, GpxDataItem>? =
		readerSync.synchronize {
			val result = readingItemsMap.entries.firstOrNull()?.toPair()?.apply { readingItemsMap.remove(first) }
			action?.invoke(result)
			result
		}

	override fun onGpxDataItemRead(item: GpxDataItem) {
		putGpxDataItemToSmartFolder(item)
	}

	private fun putGpxDataItemToSmartFolder(item: GpxDataItem) {
		val trackItem = TrackItem(item.file).apply { dataItem = item }
		SmartFolderHelper.addTrackItemToSmartFolder(trackItem)
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
			readingItemsMap.clear()
			readingItemsCallbacks.clear()
		}
	}

	override fun onReadingFinished(reader: GpxReader, cancelled: Boolean) {
		readerSync.synchronize {
			if (readingItemsMap.isNotEmpty() && readers.size < READER_TASKS_LIMIT && !cancelled) {
				startReading()
			}
			readers.remove(reader)
		}
	}
}