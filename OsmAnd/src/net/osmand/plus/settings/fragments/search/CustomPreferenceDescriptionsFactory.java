package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.plus.settings.preferences.SizePreference;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.common.Lists;
import de.KnollFrank.lib.settingssearch.search.provider.PreferenceDescription;

class CustomPreferenceDescriptionsFactory {

	public static List<PreferenceDescription> createCustomPreferenceDescriptions() {
		return Arrays.asList(
				getListPreferenceExDescription(),
				getSwitchPreferenceExDescription(),
				getMultiSelectBooleanPreferenceDescription(),
				getEditTextPreferenceExDescription(),
				getSizePreferenceDescription());
	}

	private static PreferenceDescription<ListPreferenceEx> getListPreferenceExDescription() {
		return new PreferenceDescription<>(
				ListPreferenceEx.class,
				listPreferenceEx ->
						String.join(
								", ",
								concat(
										Optional.ofNullable(listPreferenceEx.getDialogTitle()),
										Optional.ofNullable(listPreferenceEx.getDescription()),
										Optional.ofNullable(listPreferenceEx.getEntries()))));
	}

	private static PreferenceDescription<SwitchPreferenceEx> getSwitchPreferenceExDescription() {
		return new PreferenceDescription<>(
				SwitchPreferenceEx.class,
				switchPreferenceEx ->
						String.join(
								", ",
								Lists.getPresentElements(
										Arrays.asList(
												Optional.ofNullable(switchPreferenceEx.getSummaryOff()),
												Optional.ofNullable(switchPreferenceEx.getSummaryOn()),
												Optional.ofNullable(switchPreferenceEx.getDescription())))));
	}

	private static PreferenceDescription<MultiSelectBooleanPreference> getMultiSelectBooleanPreferenceDescription() {
		return new PreferenceDescription<>(
				MultiSelectBooleanPreference.class,
				multiSelectBooleanPreference ->
						String.join(
								", ",
								concat(
										Optional.ofNullable(multiSelectBooleanPreference.getDialogTitle()),
										Optional.ofNullable(multiSelectBooleanPreference.getDescription()),
										Optional.ofNullable(multiSelectBooleanPreference.getEntries()))));
	}

	private static PreferenceDescription<EditTextPreferenceEx> getEditTextPreferenceExDescription() {
		return new PreferenceDescription<>(
				EditTextPreferenceEx.class,
				editTextPreferenceEx ->
						String.join(
								", ",
								Lists.getPresentElements(
										Arrays.asList(
												Optional.ofNullable(editTextPreferenceEx.getText()),
												Optional.ofNullable(editTextPreferenceEx.getDescription())))));
	}

	private static PreferenceDescription<SizePreference> getSizePreferenceDescription() {
		return new PreferenceDescription<>(
				SizePreference.class,
				sizePreference ->
						String.join(
								", ",
								Lists.getPresentElements(
										Arrays.asList(
												Optional.ofNullable(sizePreference.getDialogTitle()),
												Optional.ofNullable(sizePreference.getSummary())))));
	}

	private static List<CharSequence> concat(final Optional<CharSequence> dialogTitle,
											 final Optional<CharSequence> description,
											 final Optional<CharSequence[]> entries) {
		final List<CharSequence> result = Lists.getPresentElements(Arrays.asList(dialogTitle, description));
		result.addAll(Lists.asList(entries));
		return result;
	}
}
