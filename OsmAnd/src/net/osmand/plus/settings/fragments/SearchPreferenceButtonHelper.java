package net.osmand.plus.settings.fragments;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import net.osmand.plus.R;
import net.osmand.plus.feedback.SendAnalyticsBottomSheetDialogFragment;
import net.osmand.plus.plugins.development.DevelopmentSettingsFragment;
import net.osmand.plus.simulation.SimulateLocationFragment;

import java.util.Optional;

import de.KnollFrank.lib.preferencesearch.client.SearchConfiguration;
import de.KnollFrank.lib.preferencesearch.client.SearchPreferenceFragments;
import de.KnollFrank.lib.preferencesearch.fragment.DefaultFragmentFactory;
import de.KnollFrank.lib.preferencesearch.provider.PreferenceDialogProvider;

class SearchPreferenceButtonHelper {

	private final BaseSettingsFragment rootSearchPreferenceFragment;
	private final @IdRes int fragmentContainerViewId;

	public SearchPreferenceButtonHelper(final BaseSettingsFragment rootSearchPreferenceFragment,
										final @IdRes int fragmentContainerViewId) {
		this.rootSearchPreferenceFragment = rootSearchPreferenceFragment;
		this.fragmentContainerViewId = fragmentContainerViewId;
	}

	public void configureSearchPreferenceButton(final ImageView searchPreferenceButton) {
		searchPreferenceButton.setOnClickListener(v -> showSearchPreferenceFragment());
		searchPreferenceButton.setImageDrawable(rootSearchPreferenceFragment.getIcon(R.drawable.searchpreference_ic_search));
		searchPreferenceButton.setVisibility(View.VISIBLE);
	}

	private void showSearchPreferenceFragment() {
		createSearchPreferenceFragments().showSearchPreferenceFragment();
	}

	private SearchPreferenceFragments createSearchPreferenceFragments() {
		return new SearchPreferenceFragments(
				createSearchConfiguration(),
				(preference, host) -> true,
				preference -> !(preference instanceof PreferenceGroup),
				CustomPreferenceDescriptionsFactory.createCustomPreferenceDescriptions(),
				new DefaultFragmentFactory(),
				rootSearchPreferenceFragment.getActivity().getSupportFragmentManager(),
				new PreferenceDialogProvider() {

					@Override
					public Optional<Fragment> getPreferenceDialog(final Class<? extends PreferenceFragmentCompat> hostOfPreference, final Preference preference) {
						// FK-TODO: handle more preference dialogs, which shall be searchable
						if (isSendAnonymousData(preference)) {
							return Optional.of(new SendAnalyticsBottomSheetDialogFragment());
						}
						if (isSimulateYourLocation(preference)) {
							final SimulateLocationFragment preferenceDialog = new SimulateLocationFragment();
							preferenceDialog.setGpxFile(null);
							// fragment.usedOnMap = false;
							return Optional.of(preferenceDialog);
						}
						return Optional.empty();
					}

					private boolean isSendAnonymousData(final Preference preference) {
						return GlobalSettingsFragment.SEND_ANONYMOUS_DATA_PREF_ID.equals(preference.getKey());
					}

					private boolean isSimulateYourLocation(final Preference preference) {
						return DevelopmentSettingsFragment.SIMULATE_YOUR_LOCATION.equals(preference.getKey());
					}
				},
				preferenceDialog -> {
					if (preferenceDialog instanceof SendAnalyticsBottomSheetDialogFragment) {
						return ((SendAnalyticsBottomSheetDialogFragment) preferenceDialog).getSearchableInfo();
					}
					if (preferenceDialog instanceof SimulateLocationFragment) {
						return ((SimulateLocationFragment) preferenceDialog).getSearchableInfo();
					}
					throw new IllegalArgumentException();
				});
	}

	private SearchConfiguration createSearchConfiguration() {
		return new SearchConfiguration(
				fragmentContainerViewId,
				Optional.empty(),
				rootSearchPreferenceFragment.getClass());
	}
}
