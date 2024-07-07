package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.Strings.joinNonNullElements;

import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.util.Arrays;

import de.KnollFrank.lib.preferencesearch.search.provider.PreferenceDescription;
import de.KnollFrank.lib.preferencesearch.search.provider.SearchableInfoProvider;

class SwitchPreferenceExDescriptionFactory {

	public static PreferenceDescription<SwitchPreferenceEx> getSwitchPreferenceExDescription() {
		return new PreferenceDescription<>(
				SwitchPreferenceEx.class,
				new SearchableInfoProvider<SwitchPreferenceEx>() {

					@Override
					public String getSearchableInfo(final SwitchPreferenceEx preference) {
						return joinNonNullElements(
								", ",
								Arrays.asList(
										preference.getSummaryOff(),
										preference.getSummaryOn(),
										preference.getDescription()));
					}
				});
	}
}
