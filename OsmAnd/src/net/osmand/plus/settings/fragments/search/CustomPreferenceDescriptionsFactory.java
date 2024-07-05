package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.util.Arrays;
import java.util.List;

import de.KnollFrank.lib.preferencesearch.search.provider.CustomPreferenceDescription;
import de.KnollFrank.lib.preferencesearch.search.provider.DefaultSummaryResetter;
import de.KnollFrank.lib.preferencesearch.search.provider.DefaultSummarySetter;

public class CustomPreferenceDescriptionsFactory {

	/* FK-TODO: make custom Preferences searchable:
	   - ColorPreferenceCompat
	   + SwitchPreferenceEx
	   - SizePreference
	   + ListPreferenceEx
	   + MultiSelectBooleanPreference
	   + EditTextPreferenceEx
   */
	public static List<CustomPreferenceDescription> createCustomPreferenceDescriptions() {
		return Arrays.asList(
				new CustomPreferenceDescription<>(
						ListPreferenceEx.class,
						new ListPreferenceExSearchableInfoProvider(),
						// FK-FIXME: problem: setSummary("test") dos not yield "test" when calling getSummary()
						(preference, summary) -> new DefaultSummarySetter().setSummary(preference, summary),
						DefaultSummaryResetter::new),
				new CustomPreferenceDescription<>(
						SwitchPreferenceEx.class,
						new SwitchPreferenceExSearchableInfoProvider(),
						new SwitchPreferenceExSummarySetter(),
						SwitchPreferenceExSummaryResetter::new),
				new CustomPreferenceDescription<>(
						MultiSelectBooleanPreference.class,
						new MultiSelectBooleanPreferenceSearchableInfoProvider(),
						(preference, summary) -> new DefaultSummarySetter().setSummary(preference, summary),
						DefaultSummaryResetter::new),
				new CustomPreferenceDescription<>(
						EditTextPreferenceEx.class,
						new EditTextPreferenceExSearchableInfoProvider(),
						(preference, summary) -> new DefaultSummarySetter().setSummary(preference, summary),
						DefaultSummaryResetter::new));
	}
}
