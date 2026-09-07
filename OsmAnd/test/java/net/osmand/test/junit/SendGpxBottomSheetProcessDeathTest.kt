package net.osmand.test.junit

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.IndexConstants
import net.osmand.plus.OsmandApplication
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.osmedit.dialogs.SendGpxBottomSheetFragment
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Regression test for issue #25881 - `NullPointerException: Attempt to get length of null array`
 * from `SendGpxBottomSheetFragment.getDefaultActivity()`, fixed by PR #25888.
 *
 * `showInstance()` used to assign the `File[]` straight to a field of the new fragment and rely on
 * `setRetainInstance(true)`. That survives a configuration change but not process death: when the
 * `FragmentManager` restores its state afterwards it builds a *fresh* fragment and gives it back
 * only the persisted arguments, so `files` was null and the `for (File file : files)` loop in
 * `getDefaultActivity()` threw while the dialog was rebuilding its view - which is why the reported
 * stack starts in `MapActivity.onStart()`.
 *
 * The restore is reproduced the way the `FragmentManager` performs it, with a new instance carrying
 * the arguments the original fragment had, because arguments are the only per-fragment state
 * `showInstance()` can persist. The test deliberately asserts on behaviour rather than on the
 * argument key: [showFromRealApi] goes through the production `showInstance()`, and whatever it
 * leaves behind is what the restored fragment gets.
 *
 * Both tests fail with the reported `NullPointerException` on the code before PR #25888.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SendGpxBottomSheetProcessDeathTest {

	companion object {
		private const val APP_INIT_TIMEOUT_SEC = 120L
		private const val TRACK_NAME = "send_gpx_process_death_test.gpx"
	}

	@get:Rule
	val scenarioRule = ActivityScenarioRule(MapActivity::class.java)

	private lateinit var app: OsmandApplication

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(APP_INIT_TIMEOUT_SEC)
		while (app.isApplicationInitializing && System.currentTimeMillis() < deadline) {
			Thread.sleep(200)
		}
	}

	@After
	fun dismissSheet() {
		onMainThread { manager ->
			(manager.findFragmentByTag(SendGpxBottomSheetFragment.TAG) as? DialogFragment)
				?.dismissAllowingStateLoss()
			manager.executePendingTransactions()
		}
	}

	/**
	 * The sheet is shown through the production API, then rebuilt from the state that outlives the
	 * process: it must come back showing the same tracks instead of throwing.
	 */
	@Test
	fun sheetRestoredAfterProcessDeathKeepsItsFiles() {
		val arguments = showFromRealApi()
		removeSheet()

		val thrown = restoreWith(arguments)
		assertNull("Restoring the sheet after process death threw:\n" + stackTraceOf(thrown), thrown)
		assertTrue("The restored sheet dismissed itself, so the tracks to upload were lost"
				+ " - showInstance() did not persist them", isSheetShown())
	}

	/**
	 * The same restore when nothing at all was persisted, which is exactly what the code before
	 * PR #25888 produced. Dismissing is acceptable, crashing is not.
	 */
	@Test
	fun sheetRestoredWithoutArgumentsDismissesInsteadOfCrashing() {
		val thrown = restoreWith(null)
		assertNull("Restoring the sheet without arguments threw:\n" + stackTraceOf(thrown), thrown)
		assertFalse("The sheet stayed up with no tracks to upload", isSheetShown())
	}

	/**
	 * Shows the sheet the way the app does and returns the state the `FragmentManager` would keep
	 * across process death.
	 */
	private fun showFromRealApi(): Bundle? {
		var arguments: Bundle? = null
		onMainThread { manager ->
			SendGpxBottomSheetFragment.showInstance(manager, arrayOf(trackFile()), null)
			manager.executePendingTransactions()
			arguments = manager.findFragmentByTag(SendGpxBottomSheetFragment.TAG)?.arguments
		}
		return arguments
	}

	/**
	 * Recreates the fragment the way the `FragmentManager` does when it restores its state: a new
	 * instance that knows nothing but its arguments.
	 */
	private fun restoreWith(arguments: Bundle?): Throwable? {
		val thrown = AtomicReference<Throwable?>(null)
		onMainThread { manager ->
			try {
				val restored = SendGpxBottomSheetFragment()
				restored.arguments = arguments
				restored.show(manager, SendGpxBottomSheetFragment.TAG)
				manager.executePendingTransactions()
			} catch (error: Throwable) {
				thrown.set(error)
			}
		}
		return thrown.get()
	}

	private fun removeSheet() {
		onMainThread { manager ->
			(manager.findFragmentByTag(SendGpxBottomSheetFragment.TAG) as? DialogFragment)
				?.dismissAllowingStateLoss()
			manager.executePendingTransactions()
		}
	}

	private fun isSheetShown(): Boolean {
		var shown = false
		onMainThread { manager ->
			shown = manager.findFragmentByTag(SendGpxBottomSheetFragment.TAG)?.isAdded == true
		}
		return shown
	}

	private fun trackFile(): File = File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), TRACK_NAME)

	private fun onMainThread(action: (FragmentManager) -> Unit) {
		scenarioRule.scenario.onActivity { activity -> action(activity.supportFragmentManager) }
	}

	private fun stackTraceOf(throwable: Throwable?): String {
		if (throwable == null) {
			return ""
		}
		val writer = StringWriter()
		throwable.printStackTrace(PrintWriter(writer))
		return writer.toString()
	}
}
