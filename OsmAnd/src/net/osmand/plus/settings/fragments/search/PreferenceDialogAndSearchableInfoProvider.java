package net.osmand.plus.settings.fragments.search;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.feedback.SendAnalyticsBottomSheetDialogFragment;
import net.osmand.plus.plugins.development.DevelopmentSettingsFragment;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;
import net.osmand.plus.settings.bottomsheets.VehicleParametersBottomSheet;
import net.osmand.plus.settings.fragments.GlobalSettingsFragment;
import net.osmand.plus.settings.fragments.VehicleParametersFragment;
import net.osmand.plus.settings.preferences.SizePreference;
import net.osmand.plus.simulation.SimulateLocationFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.provider.PreferenceDialogAndSearchableInfoByPreferenceDialogProvider;

class PreferenceDialogAndSearchableInfoProvider implements de.KnollFrank.lib.settingssearch.provider.PreferenceDialogAndSearchableInfoProvider {

	@Override
	public Optional<PreferenceDialogAndSearchableInfoByPreferenceDialogProvider> getPreferenceDialogAndSearchableInfoByPreferenceDialogProvider(final PreferenceFragmentCompat hostOfPreference, final Preference preference) {
		// FK-TODO: handle more preference dialogs, which shall be searchable
		if (isSendAnonymousData(preference)) {
			return Optional.of(
					new PreferenceDialogAndSearchableInfoByPreferenceDialogProvider<>(
							new SendAnalyticsBottomSheetDialogFragment(),
							SendAnalyticsBottomSheetDialogFragment::getSearchableInfo));
		}
		if (isSimulateYourLocation(preference)) {
			// FK-FIXME: when OsmAnd development plugin is activated (or deactivated) then recompute PreferenceGraph in order to take into account (or forget) the preferences of this plugin.
			return Optional.of(
					new PreferenceDialogAndSearchableInfoByPreferenceDialogProvider<>(
							createSimulateLocationFragment(),
							SimulateLocationFragment::getSearchableInfo));
		}
		if (preference instanceof SizePreference && hostOfPreference instanceof final VehicleParametersFragment vehicleParametersFragment) {
			return Optional.of(
					new PreferenceDialogAndSearchableInfoByPreferenceDialogProvider<>(
							createVehicleParametersBottomSheet(preference, vehicleParametersFragment),
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

	private static SimulateLocationFragment createSimulateLocationFragment() {
		final SimulateLocationFragment simulateLocationFragment = new SimulateLocationFragment();
		simulateLocationFragment.setGpxFile(null);
		return simulateLocationFragment;
	}

	private static VehicleParametersBottomSheet createVehicleParametersBottomSheet(final Preference preference, final VehicleParametersFragment vehicleParametersFragment) {
		// adapted from VehicleParametersBottomSheet.showInstance()
		final Bundle args = new Bundle();
		args.putString(BasePreferenceBottomSheet.PREFERENCE_ID, preference.getKey());
		final VehicleParametersBottomSheet vehicleParametersBottomSheet = new VehicleParametersBottomSheet();
		vehicleParametersBottomSheet.setArguments(args);
		vehicleParametersBottomSheet.setUsedOnMap(false);
		vehicleParametersBottomSheet.setAppMode(vehicleParametersFragment.getSelectedAppMode());
		vehicleParametersBottomSheet.setPreference(preference);
		vehicleParametersBottomSheet.setConfigureSettingsSearch(true);
		return vehicleParametersBottomSheet;
	}
}
