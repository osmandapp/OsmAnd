package net.osmand.plus.settings.fragments.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.clickSearchButton;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.searchView;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.test.common.AndroidTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class GeneratePreferencesDatabaseTest extends AndroidTest {

	@Rule
	public ActivityScenarioRule<MapActivity> activityRule = new ActivityScenarioRule<>(MapActivity.class);

	@Test
	public void generateDatabaseAndWaitForCompletion() {
		enableAvailablePlugins();
		clickSearchButton(app);
		onView(searchView()).perform(replaceText("tst"), closeSoftKeyboard());
	}

	private void enableAvailablePlugins() {
		enablePlugins(net.osmand.plus.plugins.PluginsHelper.getAvailablePlugins());
	}

	private void enablePlugins(final List<OsmandPlugin> plugins) {
		for (final OsmandPlugin plugin : plugins) {
			PluginsHelper.enablePlugin(plugin.getClass(), app);
		}
	}
}
