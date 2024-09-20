package net.osmand.plus.settings.fragments.search;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.feedback.SendAnalyticsBottomSheetDialogFragment;
import net.osmand.plus.plugins.development.DevelopmentSettingsFragment;
import net.osmand.plus.settings.bottomsheets.VehicleParametersBottomSheet;
import net.osmand.plus.settings.fragments.GlobalSettingsFragment;
import net.osmand.plus.settings.fragments.VehicleParametersFragment;
import net.osmand.plus.settings.preferences.SizePreference;
import net.osmand.plus.simulation.SimulateLocationFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.provider.PreferenceDialogAndSearchableInfoByPreferenceDialogProvider;

class PreferenceDialogAndSearchableInfoProvider implements de.KnollFrank.lib.settingssearch.provider.PreferenceDialogAndSearchableInfoProvider {

	@Override
	public Optional<PreferenceDialogAndSearchableInfoByPreferenceDialogProvider> getPreferenceDialogAndSearchableInfoByPreferenceDialogProvider(
			final PreferenceFragmentCompat hostOfPreference,
			final Preference preference) {
		// FK-TODO: handle more preference dialogs, which shall be searchable
		if (hostOfPreference instanceof final SearchablePreferenceDialogProvider searchablePreferenceDialogProvider) {
			return searchablePreferenceDialogProvider.getPreferenceDialogAndSearchableInfoByPreferenceDialogProvider(preference);
		}
		if (isSendAnonymousData(preference)) {
			return Optional.of(
					new PreferenceDialogAndSearchableInfoByPreferenceDialogProvider<>(
							SendAnalyticsBottomSheetDialogFragment.createInstance(null),
							SendAnalyticsBottomSheetDialogFragment::getSearchableInfo));
		}
		if (isSimulateYourLocation(preference)) {
			// FK-FIXME: when OsmAnd development plugin is activated (or deactivated) then recompute PreferenceGraph in order to take into account (or forget) the preferences of this plugin.
			return Optional.of(
					new PreferenceDialogAndSearchableInfoByPreferenceDialogProvider<>(
							SimulateLocationFragment.createInstance(null, false),
							SimulateLocationFragment::getSearchableInfo));
		}
		if (preference instanceof SizePreference && hostOfPreference instanceof final VehicleParametersFragment vehicleParametersFragment) {
			return Optional.of(
					new PreferenceDialogAndSearchableInfoByPreferenceDialogProvider<>(
							VehicleParametersBottomSheet.createInstance(
									preference.getKey(),
									null,
									false,
									vehicleParametersFragment.getSelectedAppMode(),
									true,
									Optional.of(preference)),
							VehicleParametersBottomSheet::getSearchableInfo));
		}
		return Optional.empty();
	}

	private boolean isSendAnonymousData(final Preference preference) {
		return GlobalSettingsFragment.SEND_ANONYMOUS_DATA_PREF_ID.equals(preference.getKey());
	}

	private boolean isSimulateYourLocation(final Preference preference) {
		return DevelopmentSettingsFragment.SIMULATE_YOUR_LOCATION.equals(preference.getKey());
	}
}
