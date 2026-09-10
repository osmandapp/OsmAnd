package net.osmand.test.junit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.OsmandApplication
import net.osmand.plus.download.DownloadIndexesThread
import net.osmand.plus.settings.backend.OsmandSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Regression test for the background map update check, which was meant to ask the server for the
 * index list at most once every two days and in practice asked roughly every three minutes.
 *
 * `AppInitializer.checkMapUpdates()` took the age of `LAST_CHECKED_UPDATES` - a timestamp written
 * with `System.currentTimeMillis()` in `DownloadIndexesThread` - and compared it against
 * `2 * 24 * 60 * 60L`, a constant written in seconds. That literal is 172_800, so the gate read
 * "at most one `/get_indexes` request every 172.8 seconds", and the only remaining brake was
 * `new Random().nextInt(5) == 0`. Both the comparison and the lottery were introduced together in
 * `3609492cc7` ("Update indexes"), so the missing `* 1000` had been there from the start.
 *
 * `checkMapUpdates()` is private and its only caller is `MapActivity.onCreate()` by way of
 * `WhatsNewDialogFragment.shouldShowDialog()`, so the tests drive the public entry point
 * [net.osmand.plus.AppInitializer.checkAppVersionChanged] and watch
 * [DownloadIndexesThread.getCurrentRunningTask] for the reload task that a passing gate starts.
 *
 * With the lottery gone the gate is deterministic and a single call decides each case. The tests
 * still sweep [ATTEMPTS] calls, because it costs about a second and it keeps the pair conclusive
 * if a probabilistic brake is ever put back in front of a broken interval - there a single call
 * would only catch the regression one time in five.
 */
@RunWith(AndroidJUnit4::class)
class MapUpdatesCheckIntervalTest {

	companion object {
		/** Calls per test. See the class comment for why one call is not enough. */
		private const val ATTEMPTS = 200

		/** Returned by [firstAttemptThatStartedReload] when no attempt started a reload. */
		private const val NO_RELOAD = -1

		/**
		 * `ReloadIndexesTask` is a private inner class of [DownloadIndexesThread], so it can only
		 * be recognised by name. Naming it keeps an unrelated download task that happens to be
		 * running from being mistaken for the reload under test; should the class ever be renamed,
		 * [reloadFiresWhenDueAndClosesTheIntervalBehindIt] fails and says so.
		 */
		private const val RELOAD_TASK = "ReloadIndexesTask"

		/** Comfortably past the broken 172.8s gate and comfortably inside the intended two days. */
		private val RECENTLY_CHECKED = TimeUnit.MINUTES.toMillis(10)

		/** Past the intended two days, so a check really is due. */
		private val CHECKED_LONG_AGO = TimeUnit.DAYS.toMillis(3)

		private val TASK_IDLE_TIMEOUT = TimeUnit.MINUTES.toMillis(3)
		private const val TASK_POLL_MS = 200L
	}

	private lateinit var app: OsmandApplication
	private lateinit var settings: OsmandSettings
	private lateinit var downloadThread: DownloadIndexesThread

	private var savedLastChecked = 0L

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation()
			.targetContext.applicationContext as OsmandApplication
		settings = app.settings
		downloadThread = app.downloadThread
		savedLastChecked = settings.LAST_CHECKED_UPDATES.get()

		assertTrue("a download task was already running when the test started",
			awaitNoRunningTask())

		// The first call after the installed version changed returns early, without reaching
		// checkMapUpdates(). Spend that call here, with the timestamp fresh so that it cannot
		// start a reload of its own on either build.
		settings.LAST_CHECKED_UPDATES.set(System.currentTimeMillis())
		InstrumentationRegistry.getInstrumentation()
			.runOnMainSync { app.appInitializer.checkAppVersionChanged() }
	}

	@After
	fun restoreSettings() {
		awaitNoRunningTask()
		settings.LAST_CHECKED_UPDATES.set(savedLastChecked)
	}

	/**
	 * The whole point of the gate: a check made ten minutes ago still counts as recent. Ten minutes
	 * is three and a half times the effective interval the units bug produced, so on that build the
	 * reload fires almost at once - within a couple of attempts while the lottery still stood in
	 * front of it, on the very first attempt without it.
	 */
	@Test
	fun indexListIsNotReloadedTenMinutesAfterTheLastCheck() {
		requireInternet()
		settings.LAST_CHECKED_UPDATES.set(System.currentTimeMillis() - RECENTLY_CHECKED)

		val attempt = firstAttemptThatStartedReload()

		assertEquals("checkMapUpdates() started a /get_indexes reload on attempt $attempt of" +
				" $ATTEMPTS, ${TimeUnit.MILLISECONDS.toMinutes(RECENTLY_CHECKED)} minutes after the" +
				" previous check - the two day interval is not being honoured",
			NO_RELOAD, attempt)
	}

	/**
	 * The positive control for the test above, in two halves. A check that is genuinely due has to
	 * start a reload on the very first call, since nothing probabilistic stands between the gate
	 * and the request any more; and once that reload has finished, the interval has to be closed
	 * behind it, which is what proves `LAST_CHECKED_UPDATES` was written at all.
	 *
	 * Without this, "no reload" in the test above would prove nothing - a build where the check
	 * never fires under any circumstances would pass it.
	 */
	@Test
	fun reloadFiresWhenDueAndClosesTheIntervalBehindIt() {
		requireInternet()
		settings.LAST_CHECKED_UPDATES.set(System.currentTimeMillis() - CHECKED_LONG_AGO)

		val whenDue = firstAttemptThatStartedReload()
		assertEquals("checkMapUpdates() did not start a /get_indexes reload on the first call," +
				" although the last check was ${TimeUnit.MILLISECONDS.toDays(CHECKED_LONG_AGO)} days" +
				" old (started on attempt $whenDue of $ATTEMPTS)",
			1, whenDue)

		assertTrue("the reload never finished", awaitNoRunningTask())

		val afterwards = firstAttemptThatStartedReload()
		assertEquals("checkMapUpdates() started a second /get_indexes reload on attempt" +
				" $afterwards of $ATTEMPTS, right after the previous one finished - the completed" +
				" reload did not close the interval behind it",
			NO_RELOAD, afterwards)
	}

	/**
	 * Calls the entry point until a reload task appears and returns the 1-based attempt that
	 * started it, or [NO_RELOAD].
	 *
	 * The call and the check share one main-thread block on purpose. `AsyncTask` runs
	 * `onPreExecute()` - which registers the task - synchronously on the caller, and
	 * `onPostExecute()` - which unregisters it - on the main thread, so a reload cannot start and
	 * finish unseen between the two statements.
	 */
	private fun firstAttemptThatStartedReload(): Int {
		val instrumentation = InstrumentationRegistry.getInstrumentation()
		var startedOn = NO_RELOAD
		var whatsNewBranchTaken = 0
		for (attempt in 1..ATTEMPTS) {
			instrumentation.runOnMainSync {
				if (app.appInitializer.checkAppVersionChanged()) {
					whatsNewBranchTaken++
				} else if (downloadThread.currentRunningTask?.javaClass?.simpleName == RELOAD_TASK) {
					startedOn = attempt
				}
			}
			if (startedOn != NO_RELOAD) {
				break
			}
		}
		assertEquals("checkAppVersionChanged() took the What's New branch and never reached" +
				" checkMapUpdates()", 0, whatsNewBranchTaken)
		return startedOn
	}

	/**
	 * Offline, `checkMapUpdates()` never reaches `runReloadIndexFilesSilent()` at all and both tests
	 * would pass for the wrong reason, so skip rather than report a green run.
	 */
	private fun requireInternet() {
		assumeTrue("the device is offline, the update check cannot be exercised",
			settings.isInternetConnectionAvailable(true))
	}

	/** Waits for any download task to finish, so that one run cannot mask the next. */
	private fun awaitNoRunningTask(): Boolean {
		val deadline = System.currentTimeMillis() + TASK_IDLE_TIMEOUT
		while (downloadThread.currentRunningTask != null && System.currentTimeMillis() < deadline) {
			Thread.sleep(TASK_POLL_MS)
		}
		return downloadThread.currentRunningTask == null
	}
}
