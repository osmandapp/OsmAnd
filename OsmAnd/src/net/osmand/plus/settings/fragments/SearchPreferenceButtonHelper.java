package net.osmand.plus.settings.fragments;

import android.view.View;
import android.widget.ImageView;

import com.bytehamster.lib.preferencesearch.SearchConfiguration;
import com.bytehamster.lib.preferencesearch.SearchPreferenceFragments;

import net.osmand.plus.R;

import java.util.Optional;

class SearchPreferenceButtonHelper {

    private final BaseSettingsFragment baseSettingsFragment;
    private static Optional<SearchConfiguration> searchConfigurationCache = Optional.empty();

    public SearchPreferenceButtonHelper(final BaseSettingsFragment baseSettingsFragment) {
        this.baseSettingsFragment = baseSettingsFragment;
    }

    public void configureSearchPreferenceButton(final ImageView searchPreferenceButton) {
        searchPreferenceButton.setOnClickListener(v -> showSearchPreferenceFragment());
        searchPreferenceButton.setImageDrawable(baseSettingsFragment.getIcon(R.drawable.searchpreference_ic_search));
        searchPreferenceButton.setVisibility(View.VISIBLE);
    }

    private void showSearchPreferenceFragment() {
        final SearchPreferenceFragments searchPreferenceFragments =
                new SearchPreferenceFragments(getSearchConfiguration());
        searchPreferenceFragments.showSearchPreferenceFragment();
    }

    private SearchConfiguration getSearchConfiguration() {
        if (!searchConfigurationCache.isPresent()) {
            searchConfigurationCache =
                    Optional.of(
                            baseSettingsFragment
                                    .getMapActivity()
                                    .createSearchConfiguration(new MainSettingsFragment()));
        }
        return searchConfigurationCache.get();
    }
}
