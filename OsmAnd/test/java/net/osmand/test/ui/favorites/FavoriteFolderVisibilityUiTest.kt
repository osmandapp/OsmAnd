package net.osmand.test.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.PerformException
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.osmand.data.FavouritePoint
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.myplaces.MyPlacesActivity
import net.osmand.plus.myplaces.favorites.FavouritesHelper
import net.osmand.plus.myplaces.favorites.add.AddFavoriteOptions
import net.osmand.test.common.AndroidTest
import net.osmand.test.common.EspressoUtils.onDialogView
import net.osmand.test.common.EspressoUtils.waitForView
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@LargeTest
@RunWith(AndroidJUnit4::class)
class FavoriteFolderVisibilityUiTest : AndroidTest() {

	private companion object {
		const val DIALOG_CLOSE_TIMEOUT_MS = 5000L
	}

	private lateinit var helper: FavouritesHelper
	private var showFavoritesBefore = true
	private val root = "ui26433-${UUID.randomUUID().toString().take(6)}"

	private val folderA get() = "$root/A"
	private val folderB get() = "$root/A/B"
	private val folderD get() = "$root/A/D"

	@Before
	fun prepareFavorites() {
		helper = app.favoritesHelper
		showFavoritesBefore = settings.SHOW_FAVORITES.get()
		settings.SHOW_FAVORITES.set(true)
		addPoint("b", folderB)
		addPoint("d", folderD)
		helper.saveCurrentPointsIntoFile(false)
	}

	@After
	fun removeFavorites() {
		if (!::helper.isInitialized) {
			return
		}
		for (group in helper.favoriteGroups.toList()) {
			if (group.name.startsWith(root)) {
				group.points.toList().forEach { helper.deleteFavourite(it, false) }
				helper.deleteGroup(group, false)
			}
		}
		helper.saveCurrentPointsIntoFile(false)
		settings.SHOW_FAVORITES.set(showFavoritesBefore)
	}

	@Test
	@Throws(Throwable::class)
	fun hidingFolderFromMenuHidesSubfoldersAndShowsHiddenLabel() {
		val hidden = app.getString(R.string.shared_string_hidden)

		ActivityScenario.launch<MyPlacesActivity>(favoritesTabIntent()).use {
			openFolder(root)
			assertRowDescription("A", not(containsString(hidden)))

			openFolderMenu("A")
			onDialogView(withId(R.id.compound_button)).check(matches(isChecked()))
			onDialogView(withText(R.string.shared_string_show_on_map)).perform(click())

			assertFalse(helper.isFolderVisible(folderA))
			assertFalse(findPoint("b").isVisible)
			assertFalse(findPoint("d").isVisible)
			assertRowDescription("A", containsString(hidden))

			openFolder("A")
			assertRowDescription("B", containsString(hidden))
			assertRowDescription("D", containsString(hidden))
			Espresso.pressBack()
			assertRowDescription("A", containsString(hidden))

			openFolderMenu("A")
			onDialogView(withId(R.id.compound_button)).check(matches(not(isChecked())))
			onDialogView(withText(R.string.shared_string_show_on_map)).perform(click())

			assertTrue(helper.isFolderVisible(folderA))
			assertTrue(findPoint("b").isVisible)
			assertTrue(findPoint("d").isVisible)
			assertRowDescription("A", not(containsString(hidden)))
		}
	}

	private fun favoritesTabIntent(): Intent {
		val params = Bundle()
		params.putInt(MyPlacesActivity.TAB_ID, MyPlacesActivity.FAV_TAB)
		return Intent(app, MyPlacesActivity::class.java)
			.putExtra(MapActivity.INTENT_PARAMS, params)
	}

	private fun rowContentOf(title: String): Matcher<View> {
		return allOf(
			withId(R.id.container),
			hasDescendant(allOf(withId(R.id.title), withText(title)))
		)
	}

	private fun rowOf(title: String): Matcher<View> {
		return allOf(rowContentOf(title), isDisplayed())
	}

	@Throws(Throwable::class)
	private fun scrollToRow(title: String) {
		waitForDialogClosed()
		val list = allOf(withId(R.id.recycler_view), isDisplayed(), hasDescendant(rowContentOf(title)))
		waitForView(list)
		val deadline = SystemClock.uptimeMillis() + DIALOG_CLOSE_TIMEOUT_MS
		while (true) {
			try {
				onView(list)
					.perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(rowContentOf(title)))
				break
			} catch (e: PerformException) {
				if (SystemClock.uptimeMillis() > deadline) {
					throw e
				}
				SystemClock.sleep(100)
			}
		}
		waitForView(rowOf(title))
	}

	@Throws(Throwable::class)
	private fun openFolder(title: String) {
		scrollToRow(title)
		onView(rowOf(title)).perform(click())
	}

	@Throws(Throwable::class)
	private fun openFolderMenu(title: String) {
		scrollToRow(title)
		onView(allOf(withId(R.id.menu_button), isDescendantOfA(rowOf(title)))).perform(click())
		waitForDialogView(withText(R.string.shared_string_show_on_map))
	}

	@Throws(Throwable::class)
	private fun waitForDialogView(matcher: Matcher<View>) {
		waitForView(matcher)
		onDialogView(matcher).check(matches(isDisplayed()))
	}

	private fun waitForDialogClosed() {
		val deadline = SystemClock.uptimeMillis() + DIALOG_CLOSE_TIMEOUT_MS
		while (SystemClock.uptimeMillis() < deadline) {
			try {
				onView(withText(R.string.shared_string_show_on_map)).check(doesNotExist())
				return
			} catch (e: AssertionError) {
				SystemClock.sleep(100)
			}
		}
		throw AssertionError("folder menu did not close")
	}

	@Throws(Throwable::class)
	private fun assertRowDescription(title: String, textMatcher: Matcher<String>) {
		scrollToRow(title)
		waitForView(
			allOf(
				withId(R.id.description),
				isDescendantOfA(rowOf(title)),
				withText(textMatcher)
			)
		)
	}

	private fun addPoint(suffix: String, category: String) {
		val point = FavouritePoint(50.0, 30.0, "$root-$suffix", category)
		point.altitude = 10.0
		helper.addFavourite(point, AddFavoriteOptions().setSortAndSave(false).setLookupAddress(false))
	}

	private fun findPoint(suffix: String): FavouritePoint {
		val name = "$root-$suffix"
		val point = helper.favouritePoints.firstOrNull { it.name == name }
		assertNotNull("point $name not found", point)
		return point!!
	}
}
