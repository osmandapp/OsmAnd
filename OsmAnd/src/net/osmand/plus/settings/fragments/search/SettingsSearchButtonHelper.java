package net.osmand.plus.settings.fragments.search;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IdRes;

import net.osmand.plus.R;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.client.SearchConfiguration;
import de.KnollFrank.lib.settingssearch.client.SearchPreferenceFragments;
import de.KnollFrank.lib.settingssearch.common.task.OnUiThreadRunnerFactory;

public class SettingsSearchButtonHelper {

	private final BaseSettingsFragment rootSearchPreferenceFragment;
	private final @IdRes int fragmentContainerViewId;

	public SettingsSearchButtonHelper(final BaseSettingsFragment rootSearchPreferenceFragment,
									  final @IdRes int fragmentContainerViewId) {
		this.rootSearchPreferenceFragment = rootSearchPreferenceFragment;
		this.fragmentContainerViewId = fragmentContainerViewId;
	}

	public void configureSearchPreferenceButton(final ImageView searchPreferenceButton) {
		onClickShowSearchPreferenceFragment(searchPreferenceButton);
		searchPreferenceButton.setImageDrawable(rootSearchPreferenceFragment.getIcon(R.drawable.searchpreference_ic_search));
		searchPreferenceButton.setVisibility(View.VISIBLE);
	}

	private void onClickShowSearchPreferenceFragment(final ImageView searchPreferenceButton) {
		final SearchPreferenceFragments searchPreferenceFragments = createSearchPreferenceFragments();
		searchPreferenceButton.setOnClickListener(v -> searchPreferenceFragments.showSearchPreferenceFragment());
	}

	private SearchPreferenceFragments createSearchPreferenceFragments() {
		return SearchPreferenceFragments
				.builder(
						createSearchConfiguration(),
						rootSearchPreferenceFragment.requireActivity().getSupportFragmentManager(),
						rootSearchPreferenceFragment.requireContext(),
						OnUiThreadRunnerFactory.fromActivity(rootSearchPreferenceFragment.requireActivity()))
				.withFragmentFactory(new FragmentFactory())
				.withPreferenceConnected2PreferenceFragmentProvider(new PreferenceConnected2PreferenceFragmentProvider())
				.withPrepareShow(new PrepareShow())
				.withSearchableInfoProvider(new SearchableInfoProvider())
				.withPreferenceDialogAndSearchableInfoProvider(new PreferenceDialogAndSearchableInfoProvider())
				.withPreferenceSearchablePredicate(new PreferenceSearchablePredicate())
				.withIncludePreferenceInSearchResultsPredicate(new IncludePreferenceInSearchResultsPredicate())
				.build();
	}

	private SearchConfiguration createSearchConfiguration() {
		return new SearchConfiguration(
				fragmentContainerViewId,
				Optional.empty(),
				rootSearchPreferenceFragment.getClass());
	}
}
