package net.osmand.plus.backup

import android.app.Instrumentation
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_SYNC
import net.osmand.plus.backup.ui.status.BackupStatus
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class StorageUnavailableBackupTest {

	private val instrumentation = InstrumentationRegistry.getInstrumentation()
	private val app by lazy {
		Instrumentation.newApplication(FallbackApplication::class.java, instrumentation.targetContext) as FallbackApplication
	}

	@Test
	fun firstStatusBeforePreparation() {
		val result = PrepareBackupResult(false)
		assertNull(result.error)
		val status = BackupStatus.getBackupStatus(app, result)
		assertEquals(BackupStatus.STORAGE_UNAVAILABLE, status)
		assertFalse(status.canSync())
		assertEquals(R.string.storage_unavailable_title, status.warningTitleRes)
		assertEquals(R.string.storage_unavailable_cloud_description, status.warningDescriptionRes)
	}

	@Test
	fun backgroundStartFinishesOnceOnMainThread() = checkCompletion(false)

	@Test
	fun preparedCallbackFinishesOnceOnMainThread() = checkCompletion(true)

	private fun checkCompletion(preparedFirst: Boolean) {
		val key = javaClass.name
		val count = AtomicInteger()
		val completed = CountDownLatch(1)
		var error: String? = null
		var mainThread = false
		val listener = object : SyncBackupTask.OnBackupSyncListener {
			override fun onBackupSyncFinished(message: String?) {
				error = message
				mainThread = Looper.myLooper() == Looper.getMainLooper()
				count.incrementAndGet()
				completed.countDown()
			}
		}
		val task = SyncBackupTask(app, key, SYNC_OPERATION_SYNC, listener)
		instrumentation.runOnMainSync { app.networkSettingsHelper.registerSyncBackupTask(key, task) }
		try {
			val backgroundStart = {
				assertFalse(app.backupHelper.isBackupPreparing)
				task.doInBackground()
			}
			val preparedCallback = { instrumentation.runOnMainSync { task.onBackupPrepared(null) } }
			if (preparedFirst) preparedCallback() else backgroundStart()
			assertTrue("First entry must finish the task", completed.await(5, TimeUnit.SECONDS))
			if (preparedFirst) backgroundStart() else preparedCallback()
			instrumentation.waitForIdleSync()
			assertEquals(1, count.get())
			assertTrue(mainThread)
			assertEquals(app.getString(R.string.storage_unavailable_cloud_description), error)
			assertNull(app.networkSettingsHelper.getSyncTask(key))
		} finally {
			instrumentation.runOnMainSync {
				app.backupHelper.removePrepareBackupListener(task)
				if (app.networkSettingsHelper.getSyncTask(key) === task) {
					app.networkSettingsHelper.unregisterSyncBackupTask(key)
				}
			}
		}
	}

	class FallbackApplication : OsmandApplication() {
		private val runningApp get() = applicationContext as OsmandApplication
		override fun getUnavailableStorageDirectoryAtStartup() = File("/unavailable")
		override fun getNetworkSettingsHelper() = runningApp.networkSettingsHelper
		override fun getBackupHelper() = runningApp.backupHelper
		override fun runInUIThread(run: Runnable) = runningApp.runInUIThread(run)
	}
}
