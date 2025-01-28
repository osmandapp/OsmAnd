package net.osmand.plus.download.local

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.osmand.IndexConstants
import net.osmand.plus.OsmandApplication
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController
import net.osmand.util.Algorithms
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class LocalSizeController private constructor(val app: OsmandApplication) : IDialogController {

	companion object {
		private const val PROCESS_ID = "calculate_and_store_local_indexes_size"
		private const val BYTES_IN_MB = 1024 * 1024
		private const val TILES_SIZE_CALCULATION_LIMIT = 50L * BYTES_IN_MB

		@JvmStatic
		fun addCalculationListener(app: OsmandApplication, listener: LocalSizeCalculationListener) {
			requireInstance(app).calculationListeners.add(listener)
		}

		@JvmStatic
		fun removeCalculationListener(app: OsmandApplication, listener: LocalSizeCalculationListener) {
			requireInstance(app).calculationListeners.remove(listener)
		}

		@JvmStatic
		fun calculateFullSize(app: OsmandApplication, item: LocalItem) {
			requireInstance(app).calculateFullSizeImpl(item)
		}

		@JvmStatic
		fun isSizeCalculating(context: Context, item: LocalItem): Boolean {
			if (item.type == LocalItemType.TILES_DATA) {
				val app = context.applicationContext as OsmandApplication
				val controller = requireInstance(app)
				return controller.cachedSize[item.path] == null && controller.isFullSizeMode(item)
			}
			return false
		}

		@JvmStatic
		fun requireInstance(app: OsmandApplication): LocalSizeController {
			val dialogManager = app.dialogManager
			var controller = dialogManager.findController(PROCESS_ID) as? LocalSizeController
			if (controller == null) {
				controller = LocalSizeController(app)
				dialogManager.register(PROCESS_ID, controller)
			}
			return controller
		}
	}

	private val cachedSize = ConcurrentHashMap<String, Long>()
	private val fullSizeMode = Collections.synchronizedSet(mutableSetOf<String>())
	private val calculationListeners = Collections.synchronizedSet(mutableSetOf<LocalSizeCalculationListener>())

	private fun calculateFullSizeImpl(localItem: LocalItem) {
		val key = localItem.path
		fullSizeMode.add(key)
		cachedSize.remove(key)
		notifyOnSizeCalculationEvent(localItem)
		runAsync {
			updateLocalItemSizeIfNeeded(localItem)
			runInUiThread {
				notifyOnSizeCalculationEvent(localItem)
			}
		}
	}

	fun updateLocalItemSizeIfNeeded(localItem: LocalItem) {
		if (localItem.type == LocalItemType.TILES_DATA) {
			val file = localItem.file
			var size = cachedSize[localItem.path]
			val calculationLimit = getSizeCalculationLimit(localItem)
			if (size == null) {
				size = calculateSize(file, calculationLimit)
				cachedSize[localItem.path] = size
			}
			localItem.size = size
			localItem.setSizeCalculationLimit(calculationLimit)
		}
	}

	private fun getSizeCalculationLimit(localItem: LocalItem): Long {
		if (localItem.type == LocalItemType.TILES_DATA && !isFullSizeMode(localItem)) {
			if (!localItem.fileName.endsWith(IndexConstants.SQLITE_EXT)) {
				return TILES_SIZE_CALCULATION_LIMIT
			}
		}
		return -1L
	}

	private fun isFullSizeMode(localItem: LocalItem): Boolean {
		return fullSizeMode.contains(localItem.path)
	}

	private fun calculateSize(file: File, limit: Long): Long {
		return if (file.isDirectory) {
			calculateDirSize(file, 0, limit)
		} else {
			file.length()
		}
	}

	private fun calculateDirSize(file: File, currentSize: Long, limit: Long): Long {
		var size = currentSize
		val files = file.listFiles()
		if (!Algorithms.isEmpty(files)) {
			for (f in files!!) {
				size = if (f.isDirectory) {
					calculateDirSize(f, size, limit)
				} else {
					size + f.length()
				}
				if (limit > 0 && size >= limit) {
					return size
				}
			}
		}
		return size
	}

	private fun notifyOnSizeCalculationEvent(localItem: LocalItem) {
		calculationListeners.forEach { listener -> listener.onSizeCalculationEvent(localItem) }
	}

	private fun runAsync(block: suspend () -> Unit) {
		CoroutineScope(Dispatchers.IO).launch {
			block()
		}
	}

	private fun runInUiThread(runnable: Runnable) {
		app.runInUIThread(runnable)
	}
}