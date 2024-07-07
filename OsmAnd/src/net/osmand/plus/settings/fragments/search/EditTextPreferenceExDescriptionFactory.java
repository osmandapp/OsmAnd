package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.Strings.joinNonNullElements;

import net.osmand.plus.settings.preferences.EditTextPreferenceEx;

import java.util.Arrays;

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
				});
	}
}
