package net.osmand.shared.gpx

import co.touchlab.stately.collections.ConcurrentMutableList
import co.touchlab.stately.collections.ConcurrentMutableMap
import kotlinx.coroutines.runBlocking
import net.osmand.shared.data.StringIntPair
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.gpx.GpxReader.GpxDbReaderCallback
import net.osmand.shared.io.KFile
import net.osmand.shared.util.LoggerFactory

object GpxDbHelper : GpxDbReaderCallback {
	val log = LoggerFactory.getLogger("GpxDbHelper")

	private val database: GpxDatabase by lazy { GpxDatabase() }

	private val dirItems = ConcurrentMutableMap<KFile, GpxDirItem>()
	private val dataItems = ConcurrentMutableMap<KFile, GpxDataItem>()

	private val readingItems = ConcurrentMutableList<KFile>()
	private val readingItemsMap = ConcurrentMutableMap<KFile, GpxDataItem>()
	private val readingItemsCallbacks = ConcurrentMutableMap<KFile, GpxDataItemCallback>()

	private var readerTask: GpxReader? = null
	var readTrackItemCount: Long = 0

	private var initialized: Boolean = false

	fun interface GpxDataItemCallback {
		fun isCancelled(): Boolean = false
		fun onGpxDataItemReady(item: GpxDataItem)
	}

	fun loadItemsBlocking() = runBlocking { loadItems() }
	suspend fun loadItems() {
		loadGpxItems()
		loadGpxDirItems()
		initialized = true
	}

	private suspend fun loadGpxItems() {
		val start = currentTimeMillis()
		val items = getItems()
		val fileExistenceMap = items.associate { it.file to it.file.exists() }

		items.forEach { item ->
			val file = item.file
			if (fileExistenceMap[file] == true) {
				dataItems[file] = item
			} else {
				remove(file)
			}
		}
		log.info("Time to loadGpxItems ${currentTimeMillis() - start} ms, ${items.size} items")
	}

	private fun loadGpxDirItems() {
		val start = currentTimeMillis()
		val items = getDirItems()
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

	private fun putToCache(item: DataItem) {
		val file = item.file
		when (item) {
			is GpxDataItem -> dataItems[file] = item
			is GpxDirItem -> dirItems[file] = item
		}
	}

	private fun removeFromCache(file: KFile) {
		if (GpxDbUtils.isGpxFile(file)) {
			dataItems.remove(file)
		} else {
			dirItems.remove(file)
		}
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

	fun getItemsBlocking(): List<GpxDataItem> = database.getGpxDataItemsBlocking()
	suspend fun getItems(): List<GpxDataItem> = database.getGpxDataItems()

	fun getDirItems(): List<GpxDirItem> = database.getGpxDirItems()

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
		if (file.path().isEmpty()) {
			return null
		}
		val item = dataItems[file]
		if (GpxDbUtils.isAnalyseNeeded(item) && !isGpxReading(file)) {
			readTrackItemCount++
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
		return getItems().filter {
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

	fun isRead(): Boolean {
		return readerTask == null || !readerTask!!.isReading()
	}

	private fun isGpxReading(file: KFile): Boolean {
		val analyser = readerTask
		return readingItems.contains(file) || (analyser != null && file == analyser.file)
	}

	private fun readGpxItem(file: KFile, item: GpxDataItem?, callback: GpxDataItemCallback?) {
		readingItemsMap[file] = item ?: GpxDataItem(file)
		if (callback != null) {
			readingItemsCallbacks[file] = callback
		}
		readingItems.add(file)
		if (readerTask == null) {
			startReading()
		}
	}

	private fun startReading() {
		readerTask = GpxReader(readingItems, readingItemsMap, this).apply { execute() }
	}

	private fun stopReading() {
		readerTask?.cancel()
		readerTask = null
	}

	fun getGPXDatabase(): GpxDatabase = database

	override fun onGpxDataItemRead(item: GpxDataItem) {
		putToCache(item)
		putGpxDataItemToSmartFolder(item)
	}

	private fun putGpxDataItemToSmartFolder(item: GpxDataItem) {
		val trackItem = TrackItem(item.file).apply { dataItem = item }
		SmartFolderHelper.addTrackItemToSmartFolder(trackItem)
	}

	override fun onProgressUpdate(vararg dataItems: GpxDataItem) {
		dataItems.forEach { item ->
			val callback = readingItemsCallbacks.remove(item.file)
			if (callback != null) {
				if (callback.isCancelled()) {
					stopReading()
				} else {
					callback.onGpxDataItemReady(item)
				}
			}
		}
	}

	override fun onReadingCancelled() {
		readingItems.clear()
		readingItemsMap.clear()
		readingItemsCallbacks.clear()
	}

	override fun onReadingFinished(cancelled: Boolean) {
		if (readingItems.isNotEmpty() && !cancelled) {
			startReading()
		} else {
			readerTask = null
		}
	}
}