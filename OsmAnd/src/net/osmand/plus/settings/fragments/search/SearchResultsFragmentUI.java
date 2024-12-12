package net.osmand.plus.settings.fragments.search;

import static androidx.annotation.VisibleForTesting.PACKAGE_PRIVATE;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

@VisibleForTesting(otherwise = PACKAGE_PRIVATE)
public class SearchResultsFragmentUI implements de.KnollFrank.lib.settingssearch.search.ui.SearchResultsFragmentUI {

	@VisibleForTesting
	@IdRes
	public static final int SEARCH_RESULTS_VIEW_ID = R.id.searchResultsCustom;

	@Override
	public @LayoutRes int getRootViewId() {
		return R.layout.custom_searchresults_fragment;
	}

	@Override
	public RecyclerView getSearchResultsView(final View rootView) {
		return rootView.findViewById(SEARCH_RESULTS_VIEW_ID);
	}
}
