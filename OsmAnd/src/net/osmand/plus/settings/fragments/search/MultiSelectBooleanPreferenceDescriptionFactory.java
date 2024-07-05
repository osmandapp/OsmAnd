package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.ListPreferenceExDescriptionFactory.SearchableInfoProvider.concat;

import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;

import java.util.Optional;

import de.KnollFrank.lib.preferencesearch.search.provider.DefaultSummaryResetter;
import de.KnollFrank.lib.preferencesearch.search.provider.DefaultSummarySetter;
import de.KnollFrank.lib.preferencesearch.search.provider.ISummarySetter;
import de.KnollFrank.lib.preferencesearch.search.provider.PreferenceDescription;
import de.KnollFrank.lib.preferencesearch.search.provider.SearchableInfoProvider;

class MultiSelectBooleanPreferenceDescriptionFactory {

	public static PreferenceDescription<MultiSelectBooleanPreference> getMultiSelectBooleanPreferenceDescription() {
		return new PreferenceDescription<>(
				MultiSelectBooleanPreference.class,
				new SearchableInfoProvider<MultiSelectBooleanPreference>() {

					@Override
					public String getSearchableInfo(final MultiSelectBooleanPreference preference) {
						return String.join(
								", ",
								concat(
										Optional.ofNullable(preference.getEntries()),
										Optional.ofNullable(preference.getDescription())));
					}
				},
				new ISummarySetter<MultiSelectBooleanPreference>() {

					@Override
					public void setSummary(final MultiSelectBooleanPreference preference, final CharSequence summary) {
						new DefaultSummarySetter().setSummary(preference, summary);
					}
				},
				DefaultSummaryResetter::new);
	}
}
