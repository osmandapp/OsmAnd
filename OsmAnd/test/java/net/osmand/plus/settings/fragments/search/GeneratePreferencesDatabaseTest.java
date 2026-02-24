package net.osmand.plus.settings.fragments.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static net.osmand.plus.settings.fragments.search.SearchButtonClick.clickSearchButton;
import static net.osmand.plus.settings.fragments.search.SettingsSearchTestHelper.searchView;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.test.common.AndroidTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RunWith(AndroidJUnit4.class)
public class GeneratePreferencesDatabaseTest extends AndroidTest {

	@Rule
	public ActivityScenarioRule<MapActivity> activityRule = new ActivityScenarioRule<>(MapActivity.class);

	@Before
	public void setLocaleFromArguments() {
		this
				.getTestLocaleFromArguments()
				.map(Locale::new)
				.ifPresent(this::setLocale);
	}

	@Test
	public void generateDatabaseAndWaitForCompletion() {
		enableAvailablePlugins();
		clickSearchButton(app);
		onView(searchView()).perform(replaceText("tst"), closeSoftKeyboard());
	}

	private Optional<String> getTestLocaleFromArguments() {
		return Optional.ofNullable(
				InstrumentationRegistry
						.getArguments()
						.getString("testLocale"));
	}

	private void setLocale(final Locale locale) {
		app.getSettings().PREFERRED_LOCALE.set(locale.getLanguage());
		app.getLocaleHelper().checkPreferredLocale();
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
