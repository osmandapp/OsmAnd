package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.POJOTestFactory.copy;
import static net.osmand.plus.settings.fragments.search.POJOTestFactory.createSearchablePreferencePOJO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.plus.settings.fragments.MainSettingsFragment;
import net.osmand.plus.settings.fragments.VehicleParametersFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.List;

import de.KnollFrank.lib.settingssearch.PreferencePath;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferencePOJO;

@RunWith(AndroidJUnit4.class)
public class SearchResultsByPreferencePathSorterTest {

	@Test
	public void shouldSortSearchResultsByPreferencePath() {
		// Given
		final SearchablePreferencePOJO defaultSpeed =
				createSearchablePreferencePOJO(
						"default speed",
						VehicleParametersFragment.class);
		defaultSpeed.setPreferencePath(new PreferencePath(List.of(defaultSpeed)));

		final SearchablePreferencePOJO car =
				createSearchablePreferencePOJO(
						"car",
						MainSettingsFragment.class);
		car.setPreferencePath(new PreferencePath(List.of(car)));

		final SearchablePreferencePOJO defaultSpeedOfCar = copy(defaultSpeed);
		defaultSpeedOfCar.setPreferencePath(new PreferencePath(List.of(car, defaultSpeedOfCar)));

		final SearchablePreferencePOJO walk =
				createSearchablePreferencePOJO(
						"walk",
						MainSettingsFragment.class);
		walk.setPreferencePath(new PreferencePath(List.of(walk)));

		final SearchablePreferencePOJO defaultSpeedOfWalk = copy(defaultSpeed);
		defaultSpeedOfWalk.setPreferencePath(new PreferencePath(List.of(walk, defaultSpeedOfWalk)));

		final Collection<SearchablePreferencePOJO> searchResults =
				List.of(
						defaultSpeedOfWalk,
						defaultSpeed,
						defaultSpeedOfCar);
		final de.KnollFrank.lib.settingssearch.results.SearchResultsSorter searchResultsSorter = new SearchResultsByPreferencePathSorter();

		// When
		final List<SearchablePreferencePOJO> sortedSearchResults = searchResultsSorter.sort(searchResults);

		// Then
		assertThat(
				sortedSearchResults,
				contains(
						defaultSpeed,
						defaultSpeedOfCar,
						defaultSpeedOfWalk));
	}
}
