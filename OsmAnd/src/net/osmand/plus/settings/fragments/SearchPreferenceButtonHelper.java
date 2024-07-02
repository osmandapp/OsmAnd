package net.osmand.plus.settings.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;

import net.osmand.plus.R;

import java.util.Optional;

import de.KnollFrank.lib.preferencesearch.client.SearchConfiguration;
import de.KnollFrank.lib.preferencesearch.client.SearchPreferenceFragments;
import de.KnollFrank.lib.preferencesearch.fragment.FragmentFactory;

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
				rootSearchPreferenceFragment.getActivity().getSupportFragmentManager(),
				createFragmentFactory());
	}

	private SearchConfiguration createSearchConfiguration() {
		return new SearchConfiguration(
				fragmentContainerViewId,
				Optional.empty(),
				rootSearchPreferenceFragment.getClass());
	}

	private static FragmentFactory createFragmentFactory() {
		return new FragmentFactory() {

			@Override
			public Fragment instantiate(final String fragmentClassName, final Context context) {
				return Fragment.instantiate(context, fragmentClassName, createArguments());
			}

			private Bundle createArguments() {
				final Bundle arguments = new Bundle();
				arguments.putBoolean(BaseSettingsFragment.IMPROVE_PERFORMANCE_FOR_PREFERENCE_SEARCH, true);
				return arguments;
			}
		};
	}
}
