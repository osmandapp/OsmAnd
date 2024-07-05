package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.util.Arrays;
import java.util.List;

import de.KnollFrank.lib.preferencesearch.search.provider.DefaultSummaryResetter;
import de.KnollFrank.lib.preferencesearch.search.provider.DefaultSummarySetter;
import de.KnollFrank.lib.preferencesearch.search.provider.PreferenceDescription;

public class CustomPreferenceDescriptionsFactory {

	/* FK-TODO: make custom Preferences searchable:
	   - ColorPreferenceCompat
	   + SwitchPreferenceEx
	   - SizePreference
	   + ListPreferenceEx
	   + MultiSelectBooleanPreference
	   + EditTextPreferenceEx
   */
	public static List<PreferenceDescription> createCustomPreferenceDescriptions() {
		return Arrays.asList(
				new PreferenceDescription<>(
						ListPreferenceEx.class,
						new ListPreferenceExSearchableInfoProvider(),
						// FK-FIXME: problem: setSummary("test"), but then getSummary() != "test".
						(preference, summary) -> new DefaultSummarySetter().setSummary(preference, summary),
						DefaultSummaryResetter::new),
				new PreferenceDescription<>(
						SwitchPreferenceEx.class,
						new SwitchPreferenceExSearchableInfoProvider(),
						new SwitchPreferenceExSummarySetter(),
						SwitchPreferenceExSummaryResetter::new),
				new PreferenceDescription<>(
						MultiSelectBooleanPreference.class,
						new MultiSelectBooleanPreferenceSearchableInfoProvider(),
						(preference, summary) -> new DefaultSummarySetter().setSummary(preference, summary),
						DefaultSummaryResetter::new),
				new PreferenceDescription<>(
						EditTextPreferenceEx.class,
						new EditTextPreferenceExSearchableInfoProvider(),
						(preference, summary) -> new DefaultSummarySetter().setSummary(preference, summary),
						DefaultSummaryResetter::new));
	}
}
