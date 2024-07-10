package net.osmand.plus.settings.fragments;

import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.KnollFrank.lib.preferencesearch.common.Lists;
import de.KnollFrank.lib.preferencesearch.search.provider.PreferenceDescription;

class CustomPreferenceDescriptionsFactory {

	/* FK-TODO: make custom Preferences searchable:
	   - SizePreference
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
				preference ->
						String.join(
								", ",
								concat(
										Optional.ofNullable(preference.getEntries()),
										Optional.ofNullable(preference.getDescription()),
										Optional.ofNullable(preference.getDialogTitle()))));
	}

	private static PreferenceDescription<SwitchPreferenceEx> getSwitchPreferenceExDescription() {
		return new PreferenceDescription<>(
				SwitchPreferenceEx.class,
				preference ->
						String.join(
								", ",
								Lists.getNonEmptyElements(
										Arrays.asList(
												Optional.ofNullable(preference.getSummaryOff()),
												Optional.ofNullable(preference.getSummaryOn()),
												Optional.ofNullable(preference.getDescription())))));
	}

	private static PreferenceDescription<MultiSelectBooleanPreference> getMultiSelectBooleanPreferenceDescription() {
		return new PreferenceDescription<>(
				MultiSelectBooleanPreference.class,
				preference ->
						String.join(
								", ",
								concat(
										Optional.ofNullable(preference.getEntries()),
										Optional.ofNullable(preference.getDescription()),
										Optional.ofNullable(preference.getDialogTitle()))));
	}

	private static PreferenceDescription<EditTextPreferenceEx> getEditTextPreferenceExDescription() {
		return new PreferenceDescription<>(
				EditTextPreferenceEx.class,
				preference ->
						String.join(
								", ",
								Lists.getNonEmptyElements(
										Arrays.asList(
												Optional.ofNullable(preference.getText()),
												Optional.ofNullable(preference.getDescription())))));
	}

	private static List<CharSequence> concat(final Optional<CharSequence[]> elements,
											 final Optional<? extends CharSequence>... evenMoreElements) {
		final List<CharSequence> result = new ArrayList<>();
		result.addAll(Lists.asList(elements));
		result.addAll(Lists.getNonEmptyElements(Arrays.asList(evenMoreElements)));
		return result;
	}
}
