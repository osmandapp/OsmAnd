package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.EditTextPreferenceExDescriptionFactory.getEditTextPreferenceExDescription;
import static net.osmand.plus.settings.fragments.search.ListPreferenceExDescriptionFactory.getListPreferenceExDescription;
import static net.osmand.plus.settings.fragments.search.MultiSelectBooleanPreferenceDescriptionFactory.getMultiSelectBooleanPreferenceDescription;
import static net.osmand.plus.settings.fragments.search.SwitchPreferenceExDescriptionFactory.getSwitchPreferenceExDescription;

import java.util.Arrays;
import java.util.List;

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
				getListPreferenceExDescription(),
				getSwitchPreferenceExDescription(),
				getMultiSelectBooleanPreferenceDescription(),
				getEditTextPreferenceExDescription());
	}
}
