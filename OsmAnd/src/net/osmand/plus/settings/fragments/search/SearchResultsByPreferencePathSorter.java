package net.osmand.plus.settings.fragments.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import de.KnollFrank.lib.settingssearch.PreferencePath;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferencePOJO;
import de.KnollFrank.lib.settingssearch.results.SearchablePreferencePOJOComparatorFactory;

class SearchResultsByPreferencePathSorter implements de.KnollFrank.lib.settingssearch.results.SearchResultsSorter {

	private static final Comparator<SearchablePreferencePOJO> PREFERENCE_BY_PREFERENCE_PATH_COMPARATOR = getPreferenceByPreferencePathComparator();

	@Override
	public List<SearchablePreferencePOJO> sort(final Collection<SearchablePreferencePOJO> searchResults) {
		return searchResults
				.stream()
				.sorted(PREFERENCE_BY_PREFERENCE_PATH_COMPARATOR)
				.collect(Collectors.toList());
	}

	private static Comparator<SearchablePreferencePOJO> getPreferenceByPreferencePathComparator() {
		return Comparator.comparing(
				SearchablePreferencePOJO::getPreferencePath,
				getPreferencePathComparator());
	}

	private static Comparator<PreferencePath> getPreferencePathComparator() {
		return Comparator.comparing(
				preferencePath -> reverse(preferencePath.preferences()),
				new LexicographicalListComparator<>(SearchablePreferencePOJOComparatorFactory.lexicographicalComparator()));
	}

	private static <T> List<T> reverse(final List<T> ts) {
		final List<T> tsReversed = new ArrayList<>(ts);
		Collections.reverse(tsReversed);
		return tsReversed;
	}
}
