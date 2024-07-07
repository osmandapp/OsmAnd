package net.osmand.plus.settings.fragments;

import static de.KnollFrank.lib.preferencesearch.common.Strings.joinNonNullElements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import de.KnollFrank.lib.preferencesearch.search.provider.PreferenceDescription;

class CustomPreferenceDescriptionsFactory {

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
				preference ->
						String.join(
								", ",
								concat(
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
								concat(
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

	private static List<CharSequence> concat(final Optional<CharSequence[]> entries,
											 final Optional<String> description) {
		final Builder<CharSequence> builder = ImmutableList.builder();
		entries.map(Arrays::asList).ifPresent(builder::addAll);
		description.ifPresent(builder::add);
		return builder.build();
	}
}
