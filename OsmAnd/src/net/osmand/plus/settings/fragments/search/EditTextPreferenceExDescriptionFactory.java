package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.SearchableInfoProviderHelper.joinNonNullElements;

import net.osmand.plus.settings.preferences.EditTextPreferenceEx;

import java.util.Arrays;

import de.KnollFrank.lib.preferencesearch.search.provider.DefaultSummaryResetter;
import de.KnollFrank.lib.preferencesearch.search.provider.DefaultSummarySetter;
import de.KnollFrank.lib.preferencesearch.search.provider.ISummarySetter;
import de.KnollFrank.lib.preferencesearch.search.provider.PreferenceDescription;
import de.KnollFrank.lib.preferencesearch.search.provider.SearchableInfoProvider;

class EditTextPreferenceExDescriptionFactory {

	public static PreferenceDescription<EditTextPreferenceEx> getEditTextPreferenceExDescription() {
		return new PreferenceDescription<>(
				EditTextPreferenceEx.class,
				new SearchableInfoProvider<EditTextPreferenceEx>() {

					@Override
					public String getSearchableInfo(final EditTextPreferenceEx preference) {
						return joinNonNullElements(
								", ",
								Arrays.asList(
										preference.getText(),
										preference.getDescription()));
					}
				},
				new ISummarySetter<EditTextPreferenceEx>() {

					@Override
					public void setSummary(final EditTextPreferenceEx preference, final CharSequence summary) {
						new DefaultSummarySetter().setSummary(preference, summary);
					}
				},
				DefaultSummaryResetter::new);
	}
}
