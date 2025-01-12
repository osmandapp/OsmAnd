package net.osmand.plus.settings.fragments.search;

import android.view.View;
import android.widget.CheckBox;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.fragment.app.FragmentContainerView;

import net.osmand.plus.R;

import de.KnollFrank.lib.settingssearch.search.SearchForQueryAndDisplayResultsCommand;
import de.KnollFrank.lib.settingssearch.search.ui.ProgressContainerUI;

class SearchPreferenceFragmentUI implements de.KnollFrank.lib.settingssearch.search.ui.SearchPreferenceFragmentUI {

	private final SearchResultsFilter searchResultsFilter;

	public SearchPreferenceFragmentUI(final SearchResultsFilter searchResultsFilter) {
		this.searchResultsFilter = searchResultsFilter;
	}

	@Override
	public @LayoutRes int getRootViewId() {
		return R.layout.custom_searchpreference_fragment;
	}

	@Override
	public SearchView getSearchView(final View rootView) {
		return rootView.findViewById(R.id.searchView);
	}

	@Override
	public FragmentContainerView getSearchResultsFragmentContainerView(final View rootView) {
		return rootView.findViewById(R.id.searchResultsFragmentContainerView);
	}

	@Override
	public ProgressContainerUI getProgressContainerUI(View rootView) {
		return new ProgressContainerUI() {

			@Override
			public View getRoot() {
				return rootView.findViewById(R.id.progressContainerCustom);
			}

			@Override
			public TextView getProgressText() {
				return getRoot().findViewById(de.KnollFrank.lib.settingssearch.R.id.progressText);
			}
		};
	}

	@Override
	public void onSearchReady(final View rootView, final SearchForQueryAndDisplayResultsCommand searchForQueryAndDisplayResultsCommand) {
		configureCheckbox(
				rootView.findViewById(R.id.searchResultsCheckBox),
				searchForQueryAndDisplayResultsCommand);
	}

	private void configureCheckbox(final CheckBox searchResultsCheckBox, final SearchForQueryAndDisplayResultsCommand searchForQueryAndDisplayResultsCommand) {
		searchResultsCheckBox.setChecked(searchResultsFilter.isIgnoreSearchResults());
		searchResultsCheckBox.setOnCheckedChangeListener(
				(_checkBox, isChecked) -> {
					searchResultsFilter.setIgnoreSearchResults(isChecked);
					searchForQueryAndDisplayResultsCommand.searchForQueryAndDisplayResults();
				});
	}
}
