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

class CustomSearchableInfoProviderFactory {

	public static SearchableInfoProvider createCustomSearchableInfoProvider() {
		return preference -> {
			if (preference instanceof final ListPreferenceEx listPreferenceEx) {
				return Optional.of(getSearchableInfo(listPreferenceEx));
			}
			if (preference instanceof final SwitchPreferenceEx switchPreferenceEx) {
				return Optional.of(getSearchableInfo(switchPreferenceEx));
			}
			if (preference instanceof final MultiSelectBooleanPreference multiSelectBooleanPreference) {
				return Optional.of(getSearchableInfo(multiSelectBooleanPreference));
			}
			if (preference instanceof final EditTextPreferenceEx editTextPreferenceEx) {
				return Optional.of(getSearchableInfo(editTextPreferenceEx));
			}
			if (preference instanceof final SizePreference sizePreference) {
				return Optional.of(getSizePreferenceSearchableInfo(sizePreference));
			}
			return Optional.empty();
		};
	}

	private static String getSearchableInfo(final ListPreferenceEx preference) {
		return String.join(
				", ",
				concat(
						Optional.ofNullable(preference.getDialogTitle()),
						Optional.ofNullable(preference.getDescription()),
						Optional.ofNullable(preference.getEntries())));
	}

	private static String getSearchableInfo(final SwitchPreferenceEx preference) {
		return String.join(
				", ",
				Lists.getPresentElements(
						Arrays.asList(
								Optional.ofNullable(preference.getSummaryOff()),
								Optional.ofNullable(preference.getSummaryOn()),
								Optional.ofNullable(preference.getDescription()))));
	}

	private static String getSearchableInfo(final MultiSelectBooleanPreference preference) {
		return String.join(
				", ",
				concat(
						Optional.ofNullable(preference.getDialogTitle()),
						Optional.ofNullable(preference.getDescription()),
						Optional.ofNullable(preference.getEntries())));
	}

	private static String getSearchableInfo(final EditTextPreferenceEx preference) {
		return String.join(
				", ",
				Lists.getPresentElements(
						Arrays.asList(
								Optional.ofNullable(preference.getText()),
								Optional.ofNullable(preference.getDescription()))));
	}

	private static String getSizePreferenceSearchableInfo(final SizePreference preference) {
		return String.join(
				", ",
				Lists.getPresentElements(
						Arrays.asList(
								Optional.ofNullable(preference.getDialogTitle()),
								Optional.ofNullable(preference.getSummary()))));
	}

	private static List<CharSequence> concat(final Optional<CharSequence> dialogTitle,
											 final Optional<CharSequence> description,
											 final Optional<CharSequence[]> entries) {
		final List<CharSequence> result = Lists.getPresentElements(Arrays.asList(dialogTitle, description));
		result.addAll(Lists.asList(entries));
		return result;
	}
}
