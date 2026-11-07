package net.osmand.plus.backup

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.IndexConstants
import net.osmand.map.TileSourceManager.TileSourceTemplate
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.backup.BackupListeners.OnDownloadFileListener
import net.osmand.plus.settings.backend.backup.SettingsItemReader
import net.osmand.plus.settings.backend.backup.SettingsItemType
import net.osmand.plus.settings.backend.backup.items.SettingsItem
import net.osmand.plus.settings.backend.backup.items.MapSourcesSettingsItem
import net.osmand.plus.settings.backend.backup.exporttype.MapSourcesExportType
import net.osmand.plus.resources.SQLiteTileSource
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.InputStream
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BackupImporterApplyTest {

	private lateinit var app: OsmandApplication
	private lateinit var helper: RecordingBackupHelper
	private lateinit var importer: BackupImporter
	private lateinit var item: TestItem
	private lateinit var remote: RemoteFile

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		helper = RecordingBackupHelper(app)
		importer = BackupImporter(helper, null, false)
		item = TestItem(app)
		remote = RemoteFile(JSONObject().put("type", item.type.name).put("name", item.fileName)
			.put("clienttimems", 200L).put("updatetimems", 300L)).also { it.item = item }
	}

	@After
	fun cleanup() {
		helper.downloadedFiles.forEach { it.delete() }
		helper.executor.shutdownNow()
	}

	@Test
	fun failedPreviouslyReadItemKeepsSyncState() {
		item.applied = false
		runImport(false)
		assertNotAcknowledged()
		assertFalse(item.warnings.isEmpty())
	}

	@Test
	fun successfulPreviouslyReadItemIsAcknowledged() {
		runImport(false)
		assertEquals(listOf(300L), helper.uploadTimes)
		assertEquals(200L, item.localModifiedTime)
		assertEquals(200L, item.lastModifiedTime)
	}

	@Test
	fun failedInstallationKeepsSyncStateAndCanBeRetried() {
		item.applied = false
		runImport(true)
		assertNotAcknowledged()
		item.applied = true
		runImport(true)
		assertEquals(listOf(300L), helper.uploadTimes)
		assertEquals(200L, item.localModifiedTime)
		assertEquals(200L, item.lastModifiedTime)
	}

	@Test
	fun installationExceptionKeepsSyncState() {
		item.failApply = true
		runImport(true)
		assertNotAcknowledged()
	}

	@Test
	fun failedDownloadKeepsSyncStateEvenWhenItemReportsApplied() {
		helper.downloadError = "download failed"
		runImport(true)
		assertNotAcknowledged()
	}

	@Test
	fun failedReadKeepsSyncState() {
		item.failRead = true
		runImport(true)
		assertNotAcknowledged()
	}

	@Test
	fun failedAdditionalParamsKeepSyncState() {
		item.failParams = true
		runImport(true)
		assertNotAcknowledged()
		assertTrue(item.warnings.contains(app.getString(R.string.settings_item_read_error, item.name)))
	}

	@Test
	fun failedAdditionalParamsDoNotInterruptIndependentImport() {
		item.failParams = true
		val successful = TestItem(app)
		val successfulRemote = RemoteFile(JSONObject().put("type", successful.type.name).put("name", successful.fileName)
			.put("clienttimems", 200L).put("updatetimems", 300L)).also { it.item = successful }

		importer.importItems(listOf(item, successful), listOf(remote, successfulRemote), true, false)

		assertEquals(100L, item.localModifiedTime)
		assertEquals(100L, item.lastModifiedTime)
		assertFalse(item.warnings.isEmpty())
		assertEquals(listOf(300L), helper.uploadTimes)
		assertEquals(200L, successful.localModifiedTime)
		assertEquals(200L, successful.lastModifiedTime)
	}

	@Test
	fun metadataOnlyItemWithoutReaderKeepsItsImportContract() {
		item.hasReader = false
		runImport(true)
		assertTrue(helper.uploadTimes.isEmpty())
		assertEquals(200L, item.localModifiedTime)
		assertEquals(200L, item.lastModifiedTime)
	}

	@Test
	fun mapSourcesDownloadFailureAndRetryPreserveLocalAdditions() {
		verifyMapSourcesImport(true)
	}

	@Test
	fun previouslyReadMapSourcesFailureAndRetryPreserveLocalAdditions() {
		verifyMapSourcesImport(false)
	}

	private fun verifyMapSourcesImport(forceReadData: Boolean) {
		val settings = app.settings
		val hashWasSet = settings.MAP_SOURCES_BACKUP_STATE_HASH.isSet
		val timeWasSet = settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.isSet
		val oldHash = settings.MAP_SOURCES_BACKUP_STATE_HASH.get()
		val oldTime = settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.get()
		val name = "backup_import_test_${UUID.randomUUID()}"
		val localDir = app.getAppPath(IndexConstants.TILES_INDEX_DIR + name + "_local")
		val firstDir = app.getAppPath(IndexConstants.TILES_INDEX_DIR + name + "_first")
		val remoteDir = app.getAppPath(IndexConstants.TILES_INDEX_DIR + name + "_remote")
		try {
			val local = TileSourceTemplate(localDir.name, "https://local.example/{0}/{1}/{2}", ".png", 20, 1, 256, 16, 12000)
			assertTrue(settings.installTileSource(local))
			assertTrue(remoteDir.createNewFile())
			settings.MAP_SOURCES_BACKUP_STATE_HASH.set("previous-state")
			settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.set(100L)
			val payload = JSONObject().put("items", JSONArray().put(JSONObject().put("name", firstDir.name)
				.put("url", "https://first.example/{0}/{1}/{2}").put("ext", ".png")
				.put("minZoom", 1).put("maxZoom", 20).put("tileSize", 256).put("bitDensity", 16))
				.put(JSONObject().put("name", remoteDir.name)
				.put("url", "https://remote.example/{0}/{1}/{2}").put("ext", ".png")
				.put("minZoom", 1).put("maxZoom", 20).put("tileSize", 256).put("bitDensity", 16)))
			helper.downloadBody = payload.toString()
			fun importedItem(): MapSourcesSettingsItem {
				val json = if (forceReadData) JSONObject() else JSONObject(payload.toString())
				val imported = MapSourcesSettingsItem(app, json.put("file", "$name.json"))
				imported.setShouldReplace(true)
				if (!forceReadData) {
					imported.processDuplicateItems()
					imported.apply()
				}
				return imported
			}
			fun remoteFile(imported: SettingsItem) = RemoteFile(JSONObject().put("type", "MAP_SOURCES")
				.put("name", "$name.json").put("clienttimems", 200L).put("updatetimems", 300L)).also { it.item = imported }
			val failed = importedItem()
			importer.importItems(listOf(failed), listOf(remoteFile(failed)), forceReadData, false)
			assertFalse(failed.isAppliedLocally)
			assertEquals(listOf(firstDir.name), failed.appliedItems.map { it.name })
			assertTrue(File(firstDir, ".metainfo").isFile)
			assertTrue(helper.uploadTimes.isEmpty())
			assertEquals("previous-state", settings.MAP_SOURCES_BACKUP_STATE_HASH.get())
			assertEquals(100L, settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.get().toLong())
			assertTrue(remoteDir.delete())

			val restored = importedItem()
			val remote = remoteFile(restored)
			importer.importItems(listOf(restored), listOf(remote), forceReadData, false)
			assertTrue(restored.isAppliedLocally)
			assertEquals(setOf(firstDir.name, remoteDir.name), restored.appliedItems.map { it.name }.toSet())
			assertFalse(File(firstDir.parentFile, firstDir.name + "_1").exists())
			assertEquals(listOf(300L), helper.uploadTimes)
			assertEquals(200L, settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.get().toLong())
			val sources = MapSourcesExportType().fetchExportData(app, false)
			try {
				val localItem = MapSourcesSettingsItem(app, sources)
				val localFile = LocalFile().also {
					it.item = localItem
					it.fileName = "$name.json"
					it.uploadTime = 300L
					it.localModifiedTime = localItem.localModifiedTime
				}
				assertTrue(localFile.localModifiedTime > localFile.uploadTime)
				val info = GenerateBackupInfoTask(app, mapOf(remote.typeNamePath to localFile),
					mapOf(remote.typeNamePath to remote), emptyMap(), false, null).doInBackground()
				assertTrue(info.filesToUpload.contains(localFile))
				assertTrue(info.filesToDownload.isEmpty())
			} finally {
				sources.filterIsInstance<SQLiteTileSource>().forEach { it.closeConnection() }
			}
		} finally {
			localDir.deleteRecursively()
			firstDir.deleteRecursively()
			remoteDir.deleteRecursively()
			if (hashWasSet) settings.MAP_SOURCES_BACKUP_STATE_HASH.set(oldHash)
			else settings.MAP_SOURCES_BACKUP_STATE_HASH.resetToDefault()
			if (timeWasSet) settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.set(oldTime)
			else settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.resetToDefault()
		}
	}

	@Test
	fun restoringDeletedItemDoesNotOverwriteModifiedTimes() {
		importer.importItems(listOf(item), listOf(remote), true, true)
		assertEquals(listOf(300L), helper.uploadTimes)
		assertEquals(100L, item.localModifiedTime)
		assertEquals(100L, item.lastModifiedTime)
	}

	private fun runImport(forceReadData: Boolean) {
		importer.importItems(listOf(item), listOf(remote), forceReadData, false)
	}

	private fun assertNotAcknowledged() {
		assertTrue(helper.uploadTimes.isEmpty())
		assertEquals(100L, item.localModifiedTime)
		assertEquals(100L, item.lastModifiedTime)
	}

	private class RecordingBackupHelper(app: OsmandApplication) : BackupHelper(app) {
		val uploadTimes = mutableListOf<Long>()
		val downloadedFiles = mutableListOf<File>()
		var downloadError = ""
		var downloadBody = "{}"

		override fun downloadFile(file: File, remoteFile: RemoteFile, listener: OnDownloadFileListener?, autoSync: Boolean): String {
			if (downloadError.isNotEmpty()) return downloadError
			downloadedFiles.add(file)
			file.parentFile!!.mkdirs()
			file.writeText(downloadBody)
			return ""
		}

		override fun updateFileUploadTime(type: String, fileName: String, updateTime: Long) {
			uploadTimes.add(updateTime)
		}

		override fun getUploadedFileInfo(type: String, fileName: String): UploadedFileInfo? = null
		override fun updateBackupDownloadTime() = Unit
	}

	private class TestItem(app: OsmandApplication) : SettingsItem(app) {
		var applied = true
		var failApply = false
		var failRead = false
		var failParams = false
		var hasReader = true
		private var modifiedTime = 100L

		init {
			fileName = "backup_import_test_${UUID.randomUUID()}.json"
			lastModifiedTime = 100L
		}

		override fun getType() = SettingsItemType.MAP_SOURCES
		override fun getName() = "backup_import_test"
		override fun getPublicName(ctx: Context) = name
		override fun getEstimatedSize() = 0L
		override fun getLocalModifiedTime() = modifiedTime
		override fun setLocalModifiedTime(lastModifiedTime: Long) { modifiedTime = lastModifiedTime }
		override fun isAppliedLocally() = applied
		override fun apply() {
			if (failApply) throw IllegalStateException("installation failed")
		}
		override fun getWriter() = null
		override fun applyAdditionalParams(reader: SettingsItemReader<out SettingsItem>?) {
			if (failParams) throw IllegalStateException("additional params failed")
		}
		override fun getReader() = if (!hasReader) null else object : SettingsItemReader<TestItem>(this) {
			override fun readFromStream(inputStream: InputStream, inputFile: File?, entryName: String?): File? {
				if (failRead) throw IllegalArgumentException("invalid data")
				return null
			}
		}
	}
}
