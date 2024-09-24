package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.CustomSearchableInfoProviderFactory.createCustomSearchableInfoProvider;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IdRes;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.client.SearchConfiguration;
import de.KnollFrank.lib.settingssearch.client.SearchPreferenceFragments;

public class SettingsSearchButtonHelper {

	private final BaseSettingsFragment rootSearchPreferenceFragment;
	private final OsmandSettings settings;
	private final @IdRes int fragmentContainerViewId;

	public SettingsSearchButtonHelper(final BaseSettingsFragment rootSearchPreferenceFragment,
									  final OsmandSettings settings,
									  final @IdRes int fragmentContainerViewId) {
		this.rootSearchPreferenceFragment = rootSearchPreferenceFragment;
		this.settings = settings;
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
		final FragmentFactoryAndPrepareShow fragmentFactoryAndPrepareShow = new FragmentFactoryAndPrepareShow();
		return SearchPreferenceFragments
				.builder(
						createSearchConfiguration(),
						rootSearchPreferenceFragment.getActivity().getSupportFragmentManager())
				.withFragmentFactory(fragmentFactoryAndPrepareShow)
				.withPreferenceConnected2PreferenceFragmentProvider(new PreferenceConnected2PreferenceFragmentProvider(settings))
				.withPrepareShow(fragmentFactoryAndPrepareShow)
				.withSearchableInfoProvider(createCustomSearchableInfoProvider())
				.withPreferenceDialogAndSearchableInfoProvider(
						(hostOfPreference, preference) ->
								// FK-TODO: handle more preference dialogs, which shall be searchable
								// FK-FIXME: when OsmAnd development plugin is activated (or deactivated) then recompute PreferenceGraph in order to take into account (or forget) the preferences of this plugin.
								hostOfPreference instanceof final SearchablePreferenceDialogProvider searchablePreferenceDialogProvider ?
										searchablePreferenceDialogProvider.getPreferenceDialogAndSearchableInfoByPreferenceDialogProvider(preference) :
										Optional.empty())
				.build();
	}

	private SearchConfiguration createSearchConfiguration() {
		return new SearchConfiguration(
				fragmentContainerViewId,
				Optional.empty(),
				rootSearchPreferenceFragment.getClass());
	}
}
