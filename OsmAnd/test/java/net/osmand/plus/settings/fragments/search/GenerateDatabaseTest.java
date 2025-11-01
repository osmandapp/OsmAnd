package net.osmand.plus.settings.fragments.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.clickSearchButton;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.searchView;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.plus.activities.MapActivity;
import net.osmand.test.common.AndroidTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class GenerateDatabaseTest extends AndroidTest {

	@Rule
	public ActivityScenarioRule<MapActivity> activityRule = new ActivityScenarioRule<>(MapActivity.class);

	@Test
	public void generateDatabaseAndWaitForCompletion() {
		clickSearchButton(app);
		onView(searchView()).perform(replaceText("tst"), closeSoftKeyboard());
	}
}
