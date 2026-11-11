package net.osmand.plus.plugins.gtfs

import android.net.Uri
import android.provider.OpenableColumns
import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.AndroidUtils
import net.osmand.router.gtfs.GtfsFeedImporter
import net.osmand.router.gtfs.GtfsFeedStore
import net.osmand.router.gtfs.GtfsProgressListener
import java.io.File
import java.io.IOException

/**
 * One change of the stored GTFS feeds followed by an index rebuild, on its own thread. The change is
 * all or nothing: when the copy or the rebuild fails or is cancelled, the zips this update added are
 * deleted, a removed zip is put back and the previous index stays active.
 *
 * [onProgress] and [onDone] run on the UI thread.
 */
class GtfsFeedUpdate(
	private val app: OsmandApplication,
	private val store: GtfsFeedStore,
	private val change: Change,
	private val onProgress: () -> Unit,
	private val onDone: (String) -> Unit
) {

	sealed interface Change {
		class Add(val uris: List<Uri>) : Change
		class Remove(val name: String) : Change
		object Rebuild : Change
	}

	private val log = PlatformUtil.getLog(GtfsFeedUpdate::class.java)

	@Volatile
	private var cancelled = false

	/** What is going on, for display. */
	@Volatile
	var stage: String = ""
		private set

	/** 0..100 while the index is built, -1 while copying or removing. */
	@Volatile
	var percent: Int = -1
		private set

	fun cancel() {
		cancelled = true
	}

	fun start() {
		Thread({ run() }, "GtfsFeedUpdate").start()
	}

	private fun setStage(text: String, pct: Int) {
		if (text != stage || pct != percent) {
			stage = text
			percent = pct
			app.runInUIThread { onProgress() }
		}
	}

	private fun run() {
		val added = ArrayList<File>()
		var staged: String? = null
		val message = try {
			when (change) {
				is Change.Add -> copy(change.uris, added)
				is Change.Remove -> {
					setStage(app.getString(R.string.gtfs_stage_removing, change.name), -1)
					if (!store.stageRemoval(change.name)) {
						throw IOException(change.name)
					}
					staged = change.name
				}
				Change.Rebuild -> Unit
			}
			setStage(app.getString(R.string.gtfs_stage_building, ""), 0)
			val report = store.rebuild(object : GtfsProgressListener {
				override fun onProgress(stage: String, percent: Int) {
					setStage(app.getString(R.string.gtfs_stage_building, stage), percent)
				}

				override fun isCancelled() = cancelled
			})
			staged?.let { store.finishRemoval(it, true) }
			success(report)
		} catch (e: IOException) {
			undo(added, staged)
			describe(e)
		} catch (e: OutOfMemoryError) {
			undo(added, staged)
			app.getString(R.string.gtfs_work_out_of_memory)
		} catch (e: RuntimeException) {
			log.error("GTFS update failed", e)
			undo(added, staged)
			app.getString(R.string.gtfs_work_failed, e.toString())
		}
		app.runInUIThread { onDone(message) }
	}

	private fun undo(added: List<File>, staged: String?) {
		added.forEach { it.delete() }
		staged?.let { store.finishRemoval(it, false) }
	}

	private fun copy(uris: List<Uri>, added: MutableList<File>) {
		store.deleteLeftovers()
		for ((i, uri) in uris.withIndex()) {
			val (name, size) = describe(uri)
			setStage(app.getString(R.string.gtfs_stage_copying, name, i + 1, uris.size), -1)
			val input = try {
				app.contentResolver.openInputStream(uri)
			} catch (e: Exception) {
				throw IOException(app.getString(R.string.gtfs_work_cannot_read, name), e)
			} ?: throw IOException(app.getString(R.string.gtfs_work_cannot_read, name))
			input.use { added.add(store.addFeed(name, it, size) { cancelled }) }
		}
	}

	/** Display name and size of a document; the size is -1 when the provider does not say. */
	private fun describe(uri: Uri): Pair<String, Long> {
		var name = uri.lastPathSegment ?: "feed"
		var size = -1L
		try {
			app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
				?.use { c ->
					if (c.moveToFirst()) {
						c.getString(0)?.let { name = it }
						if (!c.isNull(1)) {
							size = c.getLong(1)
						}
					}
				}
		} catch (e: RuntimeException) {
			log.warn("Cannot query $uri", e)
		}
		return name to size
	}

	private fun describe(e: IOException): String {
		val text = e.message ?: e.toString()
		return when {
			e is GtfsFeedStore.CancelledException || (e as? GtfsFeedImporter.ImportException)?.isCancelled == true ->
				app.getString(R.string.gtfs_work_cancelled)
			text.startsWith("Out of memory") -> app.getString(R.string.gtfs_work_out_of_memory)
			text.startsWith("Not enough free space") -> app.getString(R.string.gtfs_work_no_space, text)
			else -> app.getString(R.string.gtfs_work_failed, text)
		}
	}

	private fun success(report: GtfsFeedImporter.Report?): String {
		if (report == null) {
			return app.getString(R.string.gtfs_work_index_removed)
		}
		val sb = StringBuilder(
			app.getString(R.string.gtfs_work_done, report.stops, report.trips, AndroidUtils.formatSize(app, report.fileSize))
		)
		if (report.tripsSkipped > 0) {
			sb.append('\n').append(app.getString(R.string.gtfs_work_done_skipped, report.tripsSkipped))
		}
		return sb.toString()
	}
}
