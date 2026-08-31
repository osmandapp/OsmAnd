package net.osmand.test.ui.search

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.search.dialogs.SearchScopeChip
import net.osmand.test.common.AndroidTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Regression test for https://github.com/osmandapp/OsmAnd/issues/25667: a NullPointerException in
 * `AndroidComposeViewAccessibilityDelegateCompat.onViewDetachedFromWindow()` raised while a
 * `ListView` resets its items.
 *
 * Compose 1.11 dereferences `View.getHandler()` with `!!` there, although that handler is null for
 * a view that is not attached to a window. `AbsListView.RecycleBin` produces exactly such a view:
 * it detaches a recycled item from the window and later puts it back into the list with
 * `attachViewToParent()`, which does not re-dispatch `onAttachedToWindow()`. The following
 * `ListView.resetList() -> ViewGroup.removeAllViewsInLayout()` detaches that item a second time
 * and every `ComposeView` inside it crashes.
 *
 * [recycleThroughScrapHeap] replays that `RecycleBin` sequence on the real `search_list_item`
 * layout, which hosts a [SearchScopeChip].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ComposeChipDetachFromWindowTest : AndroidTest() {

	@get:Rule
	val scenarioRule = ActivityScenarioRule(MapActivity::class.java)

	/** The workaround applied in [net.osmand.plus.OsmandApplication] must keep the list alive. */
	@Test
	fun searchListItemSurvivesDetachOfRecycledItem() {
		val error = runRecycledItemDetachScenario()
		assertNull("Detaching a recycled search list item crashed: $error", error)
	}

	/**
	 * Guards the test above from silently becoming vacuous, and doubles as a reminder: as soon as
	 * this one starts failing, the upstream bug is fixed and
	 * `OsmandApplication.applyComposeWorkarounds()` can be dropped together with this test.
	 */
	@OptIn(ExperimentalComposeUiApi::class)
	@Test
	fun composeStillCrashesWithoutTheWorkaround() {
		val enabled = AndroidComposeUiFlags.isViewBasedSemanticsHandlerEnabled
		AndroidComposeUiFlags.isViewBasedSemanticsHandlerEnabled = true
		val error = try {
			runRecycledItemDetachScenario()
		} finally {
			AndroidComposeUiFlags.isViewBasedSemanticsHandlerEnabled = enabled
		}
		assertTrue(
			"Expected the upstream Compose NPE, got $error. If Compose has fixed b/486998514, "
					+ "remove OsmandApplication.applyComposeWorkarounds() and this test.",
			error is NullPointerException
		)
	}

	private fun runRecycledItemDetachScenario(): Throwable? {
		val caught = AtomicReference<Throwable?>()
		scenarioRule.scenario.onActivity { activity ->
			val content = activity.findViewById<ViewGroup>(android.R.id.content)
			val container = ScrapContainer(activity)
			content.addView(container, ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
			try {
				val item = activity.layoutInflater.inflate(R.layout.search_list_item, container, false)
				item.findViewById<SearchScopeChip>(R.id.search_scope_chip)
					.setScopeName("Test scope", false)
				// Composes as soon as the item is attached to the window.
				container.addView(item)

				container.recycleThroughScrapHeap(item)
				container.resetList()
			} catch (error: Throwable) {
				caught.set(error)
			} finally {
				content.removeView(container)
			}
		}
		return caught.get()
	}

	/** Replays the parts of `AbsListView` / `ViewGroup` that lead to the crash. */
	private class ScrapContainer(context: Context) : FrameLayout(context) {

		/**
		 * `RecycleBin` detaches an item from the window and later reattaches it to the list with
		 * `attachViewToParent()`, which leaves the whole subtree without an `AttachInfo`.
		 */
		fun recycleThroughScrapHeap(child: View) {
			val params = child.layoutParams
			detachViewFromParent(child)
			removeDetachedView(child, false)
			attachViewToParent(child, 0, params)
		}

		/** `AbsListView.resetList()`. */
		fun resetList() = removeAllViewsInLayout()
	}
}
