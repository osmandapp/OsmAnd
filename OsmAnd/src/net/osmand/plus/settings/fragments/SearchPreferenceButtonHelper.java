package net.osmand.plus.settings.fragments;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IdRes;

import net.osmand.plus.R;

import java.util.Optional;

import de.KnollFrank.lib.preferencesearch.SearchConfiguration;
import de.KnollFrank.lib.preferencesearch.SearchPreferenceFragments;

class SearchPreferenceButtonHelper {

    private final BaseSettingsFragment baseSettingsFragment;
    private final @IdRes int fragmentContainerViewId;

    public SearchPreferenceButtonHelper(final BaseSettingsFragment baseSettingsFragment,
                                        final @IdRes int fragmentContainerViewId) {
        this.baseSettingsFragment = baseSettingsFragment;
        this.fragmentContainerViewId = fragmentContainerViewId;
    }

    public void configureSearchPreferenceButton(final ImageView searchPreferenceButton) {
        searchPreferenceButton.setOnClickListener(v -> showSearchPreferenceFragment());
        searchPreferenceButton.setImageDrawable(baseSettingsFragment.getIcon(R.drawable.searchpreference_ic_search));
        searchPreferenceButton.setVisibility(View.VISIBLE);
    }

    private void showSearchPreferenceFragment() {
        final SearchPreferenceFragments searchPreferenceFragments =
                new SearchPreferenceFragments(createSearchConfiguration());
        searchPreferenceFragments.showSearchPreferenceFragment();
    }

    private SearchConfiguration createSearchConfiguration() {
        return new SearchConfiguration(
                Optional.of(baseSettingsFragment.getMapActivity()),
                fragmentContainerViewId,
                Optional.empty(),
                baseSettingsFragment.getClass());
    }
}
