package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.Strings.joinNonNullElements;

import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

	private static PreferenceDescription<ListPreferenceEx> getListPreferenceExDescription() {
		return new PreferenceDescription<>(
				ListPreferenceEx.class,
				preference -> String.join(
						", ",
						Strings.concat(
								Optional.ofNullable(preference.getEntries()),
								Optional.ofNullable(preference.getDescription()))));
	}

	private static PreferenceDescription<SwitchPreferenceEx> getSwitchPreferenceExDescription() {
		return new PreferenceDescription<>(
				SwitchPreferenceEx.class,
				preference ->
						joinNonNullElements(
								", ",
								Arrays.asList(
										preference.getSummaryOff(),
										preference.getSummaryOn(),
										preference.getDescription())));
	}

	private static PreferenceDescription<MultiSelectBooleanPreference> getMultiSelectBooleanPreferenceDescription() {
		return new PreferenceDescription<>(
				MultiSelectBooleanPreference.class,
				preference ->
						String.join(
								", ",
								Strings.concat(
										Optional.ofNullable(preference.getEntries()),
										Optional.ofNullable(preference.getDescription()))));
	}

	private static PreferenceDescription<EditTextPreferenceEx> getEditTextPreferenceExDescription() {
		return new PreferenceDescription<>(
				EditTextPreferenceEx.class,
				preference ->
						joinNonNullElements(
								", ",
								Arrays.asList(
										preference.getText(),
										preference.getDescription())));
	}
}
