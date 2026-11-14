package net.osmand.test.ui.importfiles

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.development.LogcatActivity
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.FileUtils
import net.osmand.test.common.AndroidTest
import net.osmand.test.common.AssetUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenFileForResultTest : AndroidTest() {

	private val instrumentation = InstrumentationRegistry.getInstrumentation()

	@Test
	fun fileOpenedForResultShowsMapInItsOwnTask() {
		openFileFromOtherTask { caller, intent ->
			instrumentation.runOnMainSync { caller.startActivityForResult(intent, 1) }
		}
	}

	@Test
	fun fileOpenedForResultThroughChooserShowsMapInItsOwnTask() {
		openFileFromOtherTask { caller, intent ->
			// Like the system chooser, an activity started for result passes the result on to the app it opens
			val monitor = instrumentation.addMonitor(LogcatActivity::class.java.name, null, false)
			instrumentation.runOnMainSync { caller.startActivityForResult(Intent(app, LogcatActivity::class.java), 1) }
			val chooser = instrumentation.waitForMonitorWithTimeout(monitor, TIMEOUT_MS)
			instrumentation.removeMonitor(monitor)
			instrumentation.runOnMainSync { chooser.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)) }
		}
	}

	private fun openFileFromOtherTask(open: (caller: Activity, intent: Intent) -> Unit) {
		// The map in OsmAnd's own task, as after a start from the launcher
		app.startActivity(Intent(app, MapActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
		val mapTaskId = waitForResumedMapActivity().taskId

		// An activity in another task stands in for a file manager or a messenger
		val caller = instrumentation.startActivitySync(Intent(app, LogcatActivity::class.java)
			.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
		try {
			assertNotEquals(mapTaskId, caller.taskId)

			val file = File(FileUtils.getTempDir(app), GPX_NAME)
			AssetUtils.copyAssetToFile(testContext, GPX_NAME, file)
			open(caller, Intent(Intent.ACTION_VIEW)
				.setDataAndType(AndroidUtils.getUriForFile(app, file), "application/gpx+xml")
				.setPackage(app.packageName))

			assertEquals("Map opened in the caller's task", mapTaskId, waitForResumedMapActivity().taskId)
		} finally {
			instrumentation.runOnMainSync { caller.finishAndRemoveTask() }
		}
	}

	private fun waitForResumedMapActivity(): MapActivity {
		val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
		while (SystemClock.uptimeMillis() < deadline) {
			var mapActivity: MapActivity? = null
			instrumentation.runOnMainSync {
				mapActivity = ActivityLifecycleMonitorRegistry.getInstance()
					.getActivitiesInStage(Stage.RESUMED).filterIsInstance<MapActivity>().firstOrNull()
			}
			mapActivity?.let { return it }
			SystemClock.sleep(100)
		}
		throw AssertionError("Map was not shown")
	}

	companion object {
		private const val GPX_NAME = "gpx_recalc_test.gpx"
		private const val TIMEOUT_MS = 10_000L
	}
}
