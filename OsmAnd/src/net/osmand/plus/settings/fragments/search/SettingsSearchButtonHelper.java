package net.osmand.plus.settings.fragments.search;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IdRes;

import net.osmand.plus.R;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.client.SearchConfiguration;
import de.KnollFrank.lib.settingssearch.client.SearchPreferenceFragments;
import de.KnollFrank.lib.settingssearch.graph.ComputeAndPersist;
import de.KnollFrank.lib.settingssearch.graph.SearchablePreferenceScreenGraphLoader;

public class SettingsSearchButtonHelper {

	private final BaseSettingsFragment rootSearchPreferenceFragment;
	private final @IdRes int fragmentContainerViewId;

	public SettingsSearchButtonHelper(final BaseSettingsFragment rootSearchPreferenceFragment,
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
		return SearchPreferenceFragments
				.builder(
						createSearchConfiguration(),
						rootSearchPreferenceFragment.getActivity().getSupportFragmentManager())
				.withFragmentFactory(new FragmentFactory())
				.withPreferenceConnected2PreferenceFragmentProvider(new PreferenceConnected2PreferenceFragmentProvider())
				.withPrepareShow(new PrepareShow())
				.withSearchableInfoProvider(new SearchableInfoProvider())
				.withPreferenceDialogAndSearchableInfoProvider(new PreferenceDialogAndSearchableInfoProvider())
				.withWrapSearchablePreferenceScreenGraphProvider(
						(searchablePreferenceScreenGraphProvider, preferenceManager) -> {
							final boolean persist = false;
							return persist ?
									new ComputeAndPersist(
											searchablePreferenceScreenGraphProvider,
											preferenceManager.getContext()) :
									new SearchablePreferenceScreenGraphLoader(
											R.raw.searchable_preference_screen_graph,
											preferenceManager);
						})
				.build();
	}

	private SearchConfiguration createSearchConfiguration() {
		return new SearchConfiguration(
				fragmentContainerViewId,
				Optional.empty(),
				rootSearchPreferenceFragment.getClass());
	}
}
