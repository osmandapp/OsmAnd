package net.osmand.plus.settings.backend

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.IndexConstants
import net.osmand.plus.OsmandApplication
import net.osmand.plus.api.FileSettingsAPIImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LegacyStorageDirectoryTest {

	private val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
	@get:Rule val folder = TemporaryFolder(app.cacheDir)

	@Test
	fun unwritableSharedFolderFallsBackOnlyWhenItsTypeIsMissing() {
		val root = folder.newFolder()
		val shared = File(root, IndexConstants.APP_DIR).apply { createNewFile() }
		val settings = object : OsmandSettings(app, FileSettingsAPIImpl(folder.newFile())) {
			override fun getDefaultInternalStorage() = shared
		}
		val appDirectory = requireNotNull(app.getExternalFilesDirs(null).first())
		settings.setExternalStorageDirectoryPre19(root.path)
		assertEquals(appDirectory, settings.externalStorageDirectory)
		settings.settingsAPI.edit(settings.globalPreferences)
			.putString(OsmandSettings.EXTERNAL_STORAGE_DIR_V19, shared.path).commit()
		assertEquals(appDirectory, settings.externalStorageDirectory)
		assertFalse(settings.isExternalStorageDirectoryTypeSpecifiedV19)
		settings.setExternalStorageDirectoryV19(OsmandSettings.EXTERNAL_STORAGE_TYPE_DEFAULT, shared.path)
		assertEquals(shared, settings.externalStorageDirectory)
	}

	@Test
	fun unwritableCustomFolderKeepsItsPathWithoutAType() {
		val settings = object : OsmandSettings(app, FileSettingsAPIImpl(folder.newFile())) {}
		val root = folder.newFile()
		settings.setExternalStorageDirectoryPre19(root.path)
		assertEquals(File(root, IndexConstants.APP_DIR), settings.externalStorageDirectory)
		settings.settingsAPI.edit(settings.globalPreferences)
			.putString(OsmandSettings.EXTERNAL_STORAGE_DIR_V19, root.path).commit()
		assertEquals(root, settings.externalStorageDirectory)
	}
}
