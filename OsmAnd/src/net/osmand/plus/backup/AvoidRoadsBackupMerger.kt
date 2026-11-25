package net.osmand.plus.backup

import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.avoidroads.AvoidRoadInfo
import net.osmand.plus.settings.backend.backup.exporttype.ExportType
import net.osmand.plus.settings.backend.backup.items.AvoidRoadsSettingsItem
import net.osmand.plus.settings.backend.backup.items.SettingsItem
import net.osmand.plus.utils.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Three-way merge of the avoided roads, which OsmAnd Cloud keeps as one file. When the list was
 * changed on this device and on another one since the last sync, the changes are merged against
 * the list of that sync (a snapshot kept on the device): roads added on either side are kept,
 * roads removed on either side are removed. Only when both sides changed the same road
 * differently is it left to the person as a conflict. Same approach as [FavoritesBackupMerger].
 */
class AvoidRoadsBackupMerger(
	private val app: OsmandApplication,
	private val backupHelper: BackupHelper
) {

	// Changes only what is uploaded; the local list is replaced after the upload succeeds.
	fun prepareMergeUploads(info: BackupInfo, autoSync: Boolean) {
		for (conflict in ArrayList(info.filesToMerge)) {
			val localFile = conflict.first
			val remoteFile = conflict.second
			val localItem = localFile.item as? AvoidRoadsSettingsItem ?: continue
			if (localItem is MergeUploadItem) {
				continue
			}
			val exportType = ExportType.findBy(localItem)
			if (exportType == null || !backupHelper.getBackupTypePref(exportType, autoSync).get()) {
				continue
			}
			try {
				val base = loadSnapshot(remoteFile.name, localFile.uploadTime) ?: continue
				val remote = downloadRoads(remoteFile) ?: continue
				val local = currentRoads()
				val merged = mergeRoads(base, local, remote) ?: continue
				localFile.item = MergeUploadItem(localItem, local, merged,
					localFile.localModifiedTime, localFile.uploadTime)
				info.filesToUpload.add(localFile)
				info.filesToMerge.remove(conflict)
			} catch (e: RuntimeException) {
				LOG.warn("Failed to prepare avoid roads merge for " + remoteFile.name, e)
			}
		}
	}

	fun onUploadSuccess(item: SettingsItem, fileName: String, uploadTime: Long) {
		if (uploadTime <= 0 || item !is AvoidRoadsSettingsItem
			|| fileName != BackupUtils.getItemFileName(item)) {
			return
		}
		try {
			if (item is MergeUploadItem) {
				item.finishUpload(fileName, uploadTime)
			} else if (item.localModifiedTime == item.lastModifiedTime) {
				saveSnapshot(item.items, fileName, uploadTime)
			} else {
				// changed while uploading: keep it a local change
				item.localModifiedTime = maxOf(System.currentTimeMillis(), uploadTime + 1)
			}
		} catch (e: RuntimeException) {
			LOG.warn("Failed to update avoid roads sync snapshot for $fileName", e)
		}
	}

	fun onDownloadSuccess(item: SettingsItem, remoteFile: RemoteFile) {
		if (item is AvoidRoadsSettingsItem && sameRoads(item.items, currentRoads())) {
			saveSnapshot(item.items, remoteFile.name, remoteFile.updatetimems)
		}
	}

	private fun currentRoads(): List<AvoidRoadInfo> = ArrayList(app.avoidSpecificRoads.impassableRoads)

	private fun downloadRoads(remoteFile: RemoteFile): List<AvoidRoadInfo>? {
		if (remoteFile.isDeleted) {
			return null
		}
		var tempFile: File? = null
		return try {
			tempFile = File.createTempFile("avoid-roads-merge-", ".json", FileUtils.getTempDir(app))
			val error = backupHelper.downloadFile(tempFile, remoteFile, null, false)
			if (error.isNullOrEmpty()) readRoads(tempFile) else null
		} catch (e: IOException) {
			LOG.warn("Failed to download avoid roads for merge: " + remoteFile.name, e)
			null
		} catch (e: UserNotRegisteredException) {
			LOG.warn("Failed to download avoid roads for merge: " + remoteFile.name, e)
			null
		} finally {
			if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
				LOG.warn("Failed to delete temporary avoid roads file $tempFile")
			}
		}
	}

	private fun loadSnapshot(fileName: String, syncTime: Long): List<AvoidRoadInfo>? {
		val file = snapshotFile(fileName)
		return if (file != null && file.exists() && file.lastModified() == syncTime) readRoads(file) else null
	}

	private fun saveSnapshot(roads: List<AvoidRoadInfo>, fileName: String, syncTime: Long) {
		val file = snapshotFile(fileName) ?: return
		if (syncTime <= 0) {
			return
		}
		val dir = file.parentFile
		if (dir == null || !dir.exists() && !dir.mkdirs()) {
			return
		}
		val saved = try {
			FileOutputStream(file).use { AvoidRoadsSettingsItem(app, ArrayList(roads)).writer?.writeToStream(it, null) }
			file.setLastModified(syncTime)
		} catch (e: IOException) {
			LOG.warn("Failed to save avoid roads sync snapshot for $fileName", e)
			false
		}
		if (!saved && file.exists() && !file.delete()) {
			LOG.warn("Failed to delete invalid avoid roads sync snapshot $file")
		}
	}

	private fun readRoads(file: File): List<AvoidRoadInfo>? {
		return try {
			val item = AvoidRoadsSettingsItem(app, ArrayList<AvoidRoadInfo>())
			FileInputStream(file).use { item.reader?.readFromStream(it, file, file.name) }
			item.items
		} catch (e: IOException) {
			LOG.warn("Failed to read avoid roads from $file", e)
			null
		} catch (e: IllegalArgumentException) {
			LOG.warn("Failed to read avoid roads from $file", e)
			null
		}
	}

	private fun snapshotFile(fileName: String): File? {
		if (File(fileName).name != fileName) {
			return null
		}
		return File(File(app.noBackupFilesDir, SNAPSHOT_DIR), fileName)
	}

	private inner class MergeUploadItem(
		baseItem: AvoidRoadsSettingsItem,
		local: List<AvoidRoadInfo>,
		private val mergedRoads: List<AvoidRoadInfo>,
		private val sourceModifiedTime: Long,
		private val baseSyncTime: Long
	) : AvoidRoadsSettingsItem(this@AvoidRoadsBackupMerger.app, baseItem, ArrayList(mergedRoads)) {

		private val localRoads = ArrayList(local)

		init {
			lastModifiedTime = sourceModifiedTime
		}

		fun finishUpload(fileName: String, uploadTime: Long) {
			var current = currentRoads()
			if (!sameRoads(mergedRoads, current) && sameRoads(localRoads, current)) {
				setReplaceAll(true)
				apply()
				current = currentRoads()
			}
			if (sameRoads(mergedRoads, current)) {
				localModifiedTime = sourceModifiedTime
				saveSnapshot(mergedRoads, fileName, uploadTime)
			} else {
				// changed again while uploading: keep the old common base, so the next sync
				// merges or shows the conflict again
				backupHelper.updateFileUploadTime(type.name, fileName, baseSyncTime)
				localModifiedTime = maxOf(System.currentTimeMillis(), uploadTime + 1)
			}
		}
	}

	companion object {
		private val LOG = PlatformUtil.getLog(AvoidRoadsBackupMerger::class.java)
		private const val SNAPSHOT_DIR = "avoid_roads_sync"

		/**
		 * Roads changed on one side only are taken from that side. Returns null (a conflict) when
		 * both sides changed the same road differently, one side removed a road the other changed,
		 * or the same road was added on both sides with different data.
		 */
		fun mergeRoads(
			base: List<AvoidRoadInfo>,
			local: List<AvoidRoadInfo>,
			remote: List<AvoidRoadInfo>
		): List<AvoidRoadInfo>? {
			if (hasRepeatedRoads(base) || hasRepeatedRoads(local) || hasRepeatedRoads(remote)) {
				return null
			}
			val merged = ArrayList<AvoidRoadInfo>()
			for (baseRoad in base) {
				val localRoad = local.firstOrNull { it.isSameRoad(baseRoad) }
				val remoteRoad = remote.firstOrNull { it.isSameRoad(baseRoad) }
				val localUnchanged = localRoad == baseRoad
				val remoteUnchanged = remoteRoad == baseRoad
				if (!localUnchanged && !remoteUnchanged) {
					// the same change (or removal) on both sides is not a conflict
					if (localRoad != remoteRoad) {
						return null
					}
					if (localRoad != null) {
						merged.add(localRoad)
					}
					continue
				}
				val result = if (localUnchanged) remoteRoad else localRoad
				if (result != null) {
					merged.add(result)
				}
			}
			val localAdded = local.filter { road -> base.none { it.isSameRoad(road) } }
			val remoteAdded = remote.filter { road -> base.none { it.isSameRoad(road) } }
			merged.addAll(localAdded)
			for (road in remoteAdded) {
				val localRoad = localAdded.firstOrNull { it.isSameRoad(road) }
				if (localRoad == null) {
					merged.add(road)
				} else if (localRoad != road) {
					return null
				}
			}
			return merged
		}

		fun sameRoads(first: List<AvoidRoadInfo>, second: List<AvoidRoadInfo>): Boolean {
			return first.size == second.size && first.all { road -> second.any { it == road } }
		}

		private fun hasRepeatedRoads(roads: List<AvoidRoadInfo>): Boolean {
			return roads.indices.any { i -> (i + 1 until roads.size).any { roads[it].isSameRoad(roads[i]) } }
		}
	}
}
