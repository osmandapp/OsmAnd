package net.osmand.plus.settings.fragments;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;
import static net.osmand.plus.settings.fragments.MainSettingsFragment.APP_PROFILES;

import android.content.Context;
import android.os.Bundle;
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
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.simulation.SimulateLocationFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.PreferencePath;
import de.KnollFrank.lib.settingssearch.PreferenceWithHost;
import de.KnollFrank.lib.settingssearch.client.SearchConfiguration;
import de.KnollFrank.lib.settingssearch.client.SearchPreferenceFragments;
import de.KnollFrank.lib.settingssearch.fragment.DefaultFragmentFactory;
import de.KnollFrank.lib.settingssearch.fragment.FragmentFactory;
import de.KnollFrank.lib.settingssearch.provider.IsPreferenceSearchable;
import de.KnollFrank.lib.settingssearch.provider.PreferenceDialogAndSearchableInfoByPreferenceDialogProvider;
import de.KnollFrank.lib.settingssearch.provider.PreferenceDialogAndSearchableInfoProvider;
import de.KnollFrank.lib.settingssearch.provider.ShowPreferencePath;

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
				new FragmentFactory() {

					@Override
					public Fragment instantiate(final String fragmentClassName, final Optional<PreferenceWithHost> src, final Context context) {
						final Fragment fragment = new DefaultFragmentFactory().instantiate(fragmentClassName, src, context);
						// FK-FIXME: use for "speed cameras" then only one result for Moped (Moped > Navigation Settings > {Voice prompts, Screen alerts} > Speed cameras) is displayed instead of many results for car, truck, ...
						src
								.map(preferenceWithHost -> preferenceWithHost.preference)
								.ifPresent(preference -> {
									// FK-TODO: DRY: copied from MainSettingsFragment.onPreferenceClick():
									if (preference.getParent() != null && APP_PROFILES.equals(preference.getParent().getKey())) {
										final ApplicationMode appMode = ApplicationMode.valueOfStringKey(preference.getKey(), null);
										final Bundle args = new Bundle();
										if (appMode != null) {
											args.putString(APP_MODE_KEY, appMode.getStringKey());
										}
										fragment.setArguments(args);
									}
								});
						return fragment;
					}
				},
				CustomPreferenceDescriptionsFactory.createCustomPreferenceDescriptions(),
				new PreferenceDialogAndSearchableInfoProvider() {

					@Override
					public Optional<PreferenceDialogAndSearchableInfoByPreferenceDialogProvider> getPreferenceDialogAndSearchableInfoByPreferenceDialogProvider(final PreferenceFragmentCompat hostOfPreference, final Preference preference) {
						// FK-TODO: handle more preference dialogs, which shall be searchable
						if (isSendAnonymousData(preference)) {
							return Optional.of(
									new PreferenceDialogAndSearchableInfoByPreferenceDialogProvider(
											new SendAnalyticsBottomSheetDialogFragment(),
											customDialogFragment -> ((SendAnalyticsBottomSheetDialogFragment) customDialogFragment).getSearchableInfo()));
						}
						if (isSimulateYourLocation(preference)) {
							final SimulateLocationFragment preferenceDialog = new SimulateLocationFragment();
							preferenceDialog.setGpxFile(null);
							// fragment.usedOnMap = false;
							return Optional.of(
									new PreferenceDialogAndSearchableInfoByPreferenceDialogProvider(
											preferenceDialog,
											customDialogFragment -> ((SimulateLocationFragment) customDialogFragment).getSearchableInfo()));
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
				new IsPreferenceSearchable() {

					@Override
					public boolean isPreferenceOfHostSearchable(final Preference preference, final PreferenceFragmentCompat host) {
						return true;
					}
				},
				new ShowPreferencePath() {

					@Override
					public boolean show(final PreferencePath preferencePath) {
						return preferencePath
								.getPreference()
								.map(preference -> !(preference instanceof PreferenceGroup))
								.orElse(false);
					}
				},
				rootSearchPreferenceFragment.getActivity().getSupportFragmentManager());
	}

	private SearchConfiguration createSearchConfiguration() {
		return new SearchConfiguration(
				fragmentContainerViewId,
				Optional.empty(),
				rootSearchPreferenceFragment.getClass());
	}
}