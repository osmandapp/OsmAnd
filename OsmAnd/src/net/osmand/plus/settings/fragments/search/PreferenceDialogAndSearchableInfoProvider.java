package net.osmand.plus.settings.fragments.search;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.feedback.SendAnalyticsBottomSheetDialogFragment;
import net.osmand.plus.plugins.development.DevelopmentSettingsFragment;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;
import net.osmand.plus.settings.bottomsheets.CustomizableSingleSelectionBottomSheet;
import net.osmand.plus.settings.bottomsheets.VehicleParametersBottomSheet;
import net.osmand.plus.settings.enums.MarkerDisplayOption;
import net.osmand.plus.settings.fragments.GlobalSettingsFragment;
import net.osmand.plus.settings.fragments.ProfileOptionsDialogController;
import net.osmand.plus.settings.fragments.VehicleParametersFragment;
import net.osmand.plus.settings.fragments.profileappearance.ProfileAppearanceFragment;
import net.osmand.plus.settings.preferences.SizePreference;
import net.osmand.plus.simulation.SimulateLocationFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.provider.PreferenceDialogAndSearchableInfoByPreferenceDialogProvider;

class PreferenceDialogAndSearchableInfoProvider implements de.KnollFrank.lib.settingssearch.provider.PreferenceDialogAndSearchableInfoProvider {

	private final OsmandApplication osmandApplication;

	public PreferenceDialogAndSearchableInfoProvider(final OsmandApplication osmandApplication) {
		this.osmandApplication = osmandApplication;
	}

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
		if (hostOfPreference instanceof final ProfileAppearanceFragment profileAppearanceFragment) {
			// adapted from ProfileAppearanceFragment.onPreferenceClick()
			final OsmandSettings settings = osmandApplication.getSettings();
			if (settings.VIEW_ANGLE_VISIBILITY.getId().equals(preference.getKey())) {
				return Optional.of(
						new PreferenceDialogAndSearchableInfoByPreferenceDialogProvider<>(
								createCustomizableSingleSelectionBottomSheet(
										profileAppearanceFragment.getScreenController().getProfileOptionController(),
										osmandApplication.getString(R.string.view_angle),
										osmandApplication.getString(R.string.view_angle_description),
										settings.VIEW_ANGLE_VISIBILITY),
								CustomizableSingleSelectionBottomSheet::getSearchableInfo));
			}
			if (settings.LOCATION_RADIUS_VISIBILITY.getId().equals(preference.getKey())) {
				return Optional.of(
						new PreferenceDialogAndSearchableInfoByPreferenceDialogProvider<>(
								createCustomizableSingleSelectionBottomSheet(
										profileAppearanceFragment.getScreenController().getProfileOptionController(),
										osmandApplication.getString(R.string.location_radius),
										osmandApplication.getString(R.string.location_radius_description),
										settings.LOCATION_RADIUS_VISIBILITY),
								CustomizableSingleSelectionBottomSheet::getSearchableInfo));
			}
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

	private static VehicleParametersBottomSheet createVehicleParametersBottomSheet(final Preference preference,
																				   final VehicleParametersFragment vehicleParametersFragment) {
		// adapted from VehicleParametersBottomSheet.showInstance()
		final Bundle args = new Bundle();
		args.putString(BasePreferenceBottomSheet.PREFERENCE_ID, preference.getKey());
		final VehicleParametersBottomSheet bottomSheet = new VehicleParametersBottomSheet();
		bottomSheet.setArguments(args);
		bottomSheet.setUsedOnMap(false);
		bottomSheet.setAppMode(vehicleParametersFragment.getSelectedAppMode());
		bottomSheet.setPreference(preference);
		bottomSheet.setConfigureSettingsSearch(true);
		return bottomSheet;
	}

	private CustomizableSingleSelectionBottomSheet createCustomizableSingleSelectionBottomSheet(
			final ProfileOptionsDialogController optionsDialogController,
			final String title,
			final String description,
			final CommonPreference<MarkerDisplayOption> preference) {
		optionsDialogController.prepareShowDialog(title, description, preference);
		final CustomizableSingleSelectionBottomSheet bottomSheet = new CustomizableSingleSelectionBottomSheet();
		bottomSheet.setProcessId(optionsDialogController.getProcessId());
		bottomSheet.setUsedOnMap(true);
		return bottomSheet;
	}
}
