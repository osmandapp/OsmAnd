package net.osmand.plus.settings.fragments;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IdRes;

import net.osmand.plus.R;

import java.util.Optional;

import de.KnollFrank.lib.preferencesearch.client.SearchConfiguration;
import de.KnollFrank.lib.preferencesearch.client.SearchPreferenceFragments;
import de.KnollFrank.lib.preferencesearch.fragment.DefaultFragmentFactory;

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
				CustomPreferenceDescriptionsFactory.createCustomPreferenceDescriptions(),
				new DefaultFragmentFactory(),
				rootSearchPreferenceFragment.getActivity().getSupportFragmentManager());
	}

	private SearchConfiguration createSearchConfiguration() {
		return new SearchConfiguration(
				fragmentContainerViewId,
				Optional.empty(),
				rootSearchPreferenceFragment.getClass());
	}
}
