package net.osmand.plus.settings.fragments;

import android.view.View;
import android.widget.ImageView;

import androidx.preference.PreferenceFragmentCompat;

import com.bytehamster.lib.preferencesearch.SearchConfiguration;
import com.bytehamster.lib.preferencesearch.SearchPreferenceFragments;

import net.osmand.plus.R;

class SearchPreferenceButtonHelper {

    private final BaseSettingsFragment baseSettingsFragment;

    public SearchPreferenceButtonHelper(final BaseSettingsFragment baseSettingsFragment) {
        this.baseSettingsFragment = baseSettingsFragment;
    }

    public void configureSearchPreferenceButton(final ImageView searchPreferenceButton) {
        searchPreferenceButton.setOnClickListener(v -> showSearchPreferenceFragment(new MainSettingsFragment()));
        searchPreferenceButton.setImageDrawable(baseSettingsFragment.getIcon(R.drawable.searchpreference_ic_search));
        searchPreferenceButton.setVisibility(View.VISIBLE);
    }

    private void showSearchPreferenceFragment(final PreferenceFragmentCompat root) {
        final SearchConfiguration searchConfiguration = baseSettingsFragment.getMapActivity().createSearchConfiguration(root);
        final SearchPreferenceFragments searchPreferenceFragments = new SearchPreferenceFragments(searchConfiguration);
        searchPreferenceFragments.showSearchPreferenceFragment();
    }
}
