package net.osmand.plus.settings.datastorage

import android.os.Build
import android.system.Os
import android.util.Pair
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
class SharedStorageMigrationTest {

	@Test
	fun fallbackOffersMigrationOnlyForTheConfiguredLegacyFolder() {
		val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
		// A regular file cannot be used as a writable data directory.
		val legacy = File.createTempFile("legacy", ".tmp", cache)
		val other = File.createTempFile("custom", ".tmp", cache)
		try {
			assertTrue(SharedStorageWarningFragment.dialogShowRequired(legacy, legacy, legacy))
			assertFalse(SharedStorageWarningFragment.dialogShowRequired(other, other, legacy))
			assertFalse(SharedStorageWarningFragment.dialogShowRequired(other, legacy, legacy))
			assertFalse(SharedStorageWarningFragment.dialogShowRequired(cache, cache, cache))
		} finally {
			legacy.delete()
			other.delete()
		}
	}

	@Test
	fun legacyFolderCanBeConfiguredThroughASymlink() {
		val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
		val volume = Files.createTempDirectory(cache.toPath(), "shared-storage").toFile()
		val legacy = File(volume, "osmand")
		val link = File(cache, "${volume.name}-link")
		try {
			assertTrue(legacy.mkdir())
			assertTrue(legacy.setWritable(false, false))
			Os.symlink(volume.absolutePath, link.absolutePath)
			val alias = File(link, legacy.name)
			assertTrue(SharedStorageWarningFragment.dialogShowRequired(alias, alias, legacy))
			assertTrue(SharedStorageWarningFragment.dialogShowRequired(alias, legacy, legacy))
			val mixedCase = File(link, "OsmAnd")
			assertTrue(DataStorageHelper.isSharedStorageDirectory(mixedCase, legacy))
			assertTrue(SharedStorageWarningFragment.dialogShowRequired(mixedCase, legacy, legacy))
			assertFalse(DataStorageHelper.isSharedStorageDirectory(File(cache, "OsmAnd"), legacy))
		} finally {
			link.delete()
			legacy.setWritable(true, true)
			legacy.delete()
			volume.delete()
		}
	}

	@Test
	fun sharedMigrationRemainsVisibleAfterCopyAndResume() {
		val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		val finished = app.settings.SHARED_STORAGE_MIGRATION_FINISHED
		val previous = if (finished.isSet) finished.get() else null
		val startupDirectory = OsmandApplication::class.java.getDeclaredField("unavailableStorageDirectoryAtStartup")
		startupDirectory.isAccessible = true
		val originalDirectory = app.unavailableStorageDirectoryAtStartup
		val blocked = File.createTempFile("blocked-volume", ".tmp", app.cacheDir)
		val restartSelected = AtomicBoolean()
		try {
			startupDirectory.set(app, blocked)
			finished.set(true)
			ActivityScenario.launch(MapActivity::class.java).use { scenario ->
				scenario.onActivity { activity ->
					val manager = activity.supportFragmentManager
					manager.executePendingTransactions()
					// Establish that this launch would show the warning, without acknowledging it.
					manager.fragments.filterIsInstance<StorageUnavailableBottomSheet>().single().dismissNow()
					val storage = DataStorageHelper(app)
					val progress = StorageMigrationFragment.showInstance(manager,
						storage.getStorage(DataStorageHelper.INTERNAL_STORAGE), storage.getStorage(DataStorageHelper.SHARED_STORAGE),
						Pair(1024L, 1024L), 0, 1, true, true, { restartSelected.set(true) }, null)
					manager.executePendingTransactions()
					assertTrue(StorageMigrationFragment.isSharedStorageMigrationShowing(activity.fragmentsHelper))
					progress.onFilesCopyFinished(emptyMap(), emptyList())
				}
				scenario.moveToState(Lifecycle.State.CREATED)
				scenario.moveToState(Lifecycle.State.RESUMED)
				scenario.onActivity { activity ->
					val manager = activity.supportFragmentManager
					val progress = manager.fragments.filterIsInstance<StorageMigrationFragment>().single()
					assertTrue(progress.requireDialog().isShowing)
					assertTrue(StorageMigrationFragment.isSharedStorageMigrationShowing(activity.fragmentsHelper))
					assertFalse(manager.fragments.any { it is StorageUnavailableBottomSheet })
				}
				onView(withText(R.string.shared_string_restart)).inRoot(isDialog()).perform(click())
				assertTrue(restartSelected.get())
			}
		} finally {
			startupDirectory.set(app, originalDirectory)
			if (previous == null) finished.resetToDefault() else finished.set(previous)
			blocked.delete()
		}
	}
}
