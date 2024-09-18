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
import de.KnollFrank.lib.settingssearch.search.provider.SearchableInfoProvider;

class CustomCustomSearchableInfoProviderFactory {

	public static SearchableInfoProvider createCustomSearchableInfoProvider() {
		return preference -> {
			if (preference instanceof final ListPreferenceEx listPreferenceEx) {
				return Optional.of(getListPreferenceExSearchableInfo(listPreferenceEx));
			}
			if (preference instanceof final SwitchPreferenceEx switchPreferenceEx) {
				return Optional.of(getSwitchPreferenceExSearchableInfo(switchPreferenceEx));
			}
			if (preference instanceof final MultiSelectBooleanPreference multiSelectBooleanPreference) {
				return Optional.of(getMultiSelectBooleanPreferenceSearchableInfo(multiSelectBooleanPreference));
			}
			if (preference instanceof final EditTextPreferenceEx editTextPreferenceEx) {
				return Optional.of(getEditTextPreferenceExSearchableInfo(editTextPreferenceEx));
			}
			if (preference instanceof final SizePreference sizePreference) {
				return Optional.of(getSizePreferenceSearchableInfo(sizePreference));
			}
			return Optional.empty();
		};
	}

	private static String getListPreferenceExSearchableInfo(final ListPreferenceEx listPreferenceEx) {
		return String.join(
				", ",
				concat(
						Optional.ofNullable(listPreferenceEx.getDialogTitle()),
						Optional.ofNullable(listPreferenceEx.getDescription()),
						Optional.ofNullable(listPreferenceEx.getEntries())));
	}

	private static String getSwitchPreferenceExSearchableInfo(final SwitchPreferenceEx switchPreferenceEx) {
		return String.join(
				", ",
				Lists.getPresentElements(
						Arrays.asList(
								Optional.ofNullable(switchPreferenceEx.getSummaryOff()),
								Optional.ofNullable(switchPreferenceEx.getSummaryOn()),
								Optional.ofNullable(switchPreferenceEx.getDescription()))));
	}

	private static String getMultiSelectBooleanPreferenceSearchableInfo(final MultiSelectBooleanPreference multiSelectBooleanPreference) {
		return String.join(
				", ",
				concat(
						Optional.ofNullable(multiSelectBooleanPreference.getDialogTitle()),
						Optional.ofNullable(multiSelectBooleanPreference.getDescription()),
						Optional.ofNullable(multiSelectBooleanPreference.getEntries())));
	}

	private static String getEditTextPreferenceExSearchableInfo(final EditTextPreferenceEx editTextPreferenceEx) {
		return String.join(
				", ",
				Lists.getPresentElements(
						Arrays.asList(
								Optional.ofNullable(editTextPreferenceEx.getText()),
								Optional.ofNullable(editTextPreferenceEx.getDescription()))));
	}

	private static String getSizePreferenceSearchableInfo(final SizePreference sizePreference) {
		return String.join(
				", ",
				Lists.getPresentElements(
						Arrays.asList(
								Optional.ofNullable(sizePreference.getDialogTitle()),
								Optional.ofNullable(sizePreference.getSummary()))));
	}

	private static List<CharSequence> concat(final Optional<CharSequence> dialogTitle,
											 final Optional<CharSequence> description,
											 final Optional<CharSequence[]> entries) {
		final List<CharSequence> result = Lists.getPresentElements(Arrays.asList(dialogTitle, description));
		result.addAll(Lists.asList(entries));
		return result;
	}
}
