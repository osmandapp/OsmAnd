package net.osmand.test.ui.search

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchTab
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment.QuickSearchType
import net.osmand.plus.search.dialogs.QuickSearchMainListFragment
import net.osmand.test.common.AndroidTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MapActivity handles a uiMode change (dark theme on or off) itself and refreshes its fragments by
 * detaching and attaching them. The search dialog then replaces its main list; the old list gets
 * its view created again and destroyed before onActivityCreated, and must not unregister its
 * observer a second time ("Observer ... was not registered").
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class QuickSearchFragmentsRefreshTest : AndroidTest() {

	private val instrumentation = InstrumentationRegistry.getInstrumentation()

	@Test
	fun searchSurvivesFragmentsRefreshOnUiModeChange() {
		ActivityScenario.launch(MapActivity::class.java).use { scenario ->
			lateinit var activity: MapActivity
			scenario.onActivity {
				activity = it
				QuickSearchDialogFragment.showInstance(it, "", null,
					QuickSearchType.REGULAR, QuickSearchTab.HISTORY, null)
			}
			instrumentation.waitForIdleSync()
			val listBefore = onMain { findMainList(activity) }
			assertNotNull("Precondition: the main list registered its observer in onActivityCreated",
				onMain { listBefore.listAdapter })

			// what MapActivity.onConfigurationChanged posts, run at once
			onMain {
				activity.fragmentsHelper.updateFragments()
				activity.supportFragmentManager.executePendingTransactions()
			}
			val listAfter = onMain { findMainList(activity) }
			assertNotSame("Precondition: the refreshed search replaced its main list", listBefore, listAfter)
			assertNotNull("The new main list has no adapter", onMain { listAfter.listAdapter })
		}
	}

	private fun findMainList(activity: MapActivity): QuickSearchMainListFragment {
		val dialog = activity.supportFragmentManager
			.findFragmentByTag(QuickSearchDialogFragment.TAG) as QuickSearchDialogFragment?
		assertNotNull("Search dialog is not shown", dialog)
		val list = dialog!!.childFragmentManager.fragments.filterIsInstance<QuickSearchMainListFragment>()
		assertNotNull("Search has not exactly one main list: $list", list.singleOrNull())
		return list.single()
	}

	private fun <T> onMain(action: () -> T): T {
		var result: Result<T>? = null
		instrumentation.runOnMainSync { result = runCatching(action) }
		return result!!.getOrThrow()
	}
}
