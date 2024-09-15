package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.common.Lists;
import de.KnollFrank.lib.settingssearch.search.provider.PreferenceDescription;

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
				preference -> {
					final ListPreferenceEx listPreferenceEx = (ListPreferenceEx) preference;
					return String.join(
							", ",
							concat(
									Optional.ofNullable(listPreferenceEx.getDialogTitle()),
									Optional.ofNullable(listPreferenceEx.getDescription()),
									Optional.ofNullable(listPreferenceEx.getEntries())));
				});
	}

	private static PreferenceDescription<SwitchPreferenceEx> getSwitchPreferenceExDescription() {
		return new PreferenceDescription<>(
				SwitchPreferenceEx.class,
				preference -> {
					final SwitchPreferenceEx switchPreferenceEx = (SwitchPreferenceEx) preference;
					return String.join(
							", ",
							Lists.getPresentElements(
									Arrays.asList(
											Optional.ofNullable(switchPreferenceEx.getSummaryOff()),
											Optional.ofNullable(switchPreferenceEx.getSummaryOn()),
											Optional.ofNullable(switchPreferenceEx.getDescription()))));
				});
	}

	private static PreferenceDescription<MultiSelectBooleanPreference> getMultiSelectBooleanPreferenceDescription() {
		return new PreferenceDescription<>(
				MultiSelectBooleanPreference.class,
				preference -> {
					final MultiSelectBooleanPreference multiSelectBooleanPreference = (MultiSelectBooleanPreference) preference;
					return String.join(
							", ",
							concat(
									Optional.ofNullable(multiSelectBooleanPreference.getDialogTitle()),
									Optional.ofNullable(multiSelectBooleanPreference.getDescription()),
									Optional.ofNullable(multiSelectBooleanPreference.getEntries())));
				});
	}

	private static PreferenceDescription<EditTextPreferenceEx> getEditTextPreferenceExDescription() {
		return new PreferenceDescription<>(
				EditTextPreferenceEx.class,
				preference -> {
					final EditTextPreferenceEx textPreferenceEx = (EditTextPreferenceEx) preference;
					return String.join(
							", ",
							Lists.getPresentElements(
									Arrays.asList(
											Optional.ofNullable(textPreferenceEx.getText()),
											Optional.ofNullable(textPreferenceEx.getDescription()))));
				});
	}

	private static List<CharSequence> concat(final Optional<? extends CharSequence> dialogTitle,
											 final Optional<? extends CharSequence> description,
											 final Optional<CharSequence[]> entries) {
		final List<CharSequence> result = new ArrayList<>();
		dialogTitle.ifPresent(result::add);
		description.ifPresent(result::add);
		result.addAll(Lists.asList(entries));
		return result;
	}
}
