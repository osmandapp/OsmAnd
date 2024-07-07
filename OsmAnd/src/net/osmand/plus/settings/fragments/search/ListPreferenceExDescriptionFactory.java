package net.osmand.plus.settings.fragments.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import net.osmand.plus.settings.preferences.ListPreferenceEx;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.KnollFrank.lib.preferencesearch.search.provider.PreferenceDescription;

class ListPreferenceExDescriptionFactory {

	public static PreferenceDescription<ListPreferenceEx> getListPreferenceExDescription() {
		return new PreferenceDescription<>(
				ListPreferenceEx.class,
				new SearchableInfoProvider());
	}

	static class SearchableInfoProvider implements de.KnollFrank.lib.preferencesearch.search.provider.SearchableInfoProvider<ListPreferenceEx> {

		@Override
		public String getSearchableInfo(final ListPreferenceEx preference) {
			return String.join(
					", ",
					concat(
							Optional.ofNullable(preference.getEntries()),
							Optional.ofNullable(preference.getDescription())));
		}


		// FK-TODO: move method to Strings class
		static List<CharSequence> concat(final Optional<CharSequence[]> entries,
										 final Optional<String> description) {
			final Builder<CharSequence> builder = ImmutableList.builder();
			entries.map(Arrays::asList).ifPresent(builder::addAll);
			description.ifPresent(builder::add);
			return builder.build();
		}
	}
}
