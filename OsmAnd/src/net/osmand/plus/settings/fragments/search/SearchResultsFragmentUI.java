package net.osmand.plus.settings.fragments.search;

import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

class SearchResultsFragmentUI implements de.KnollFrank.lib.settingssearch.search.ui.SearchResultsFragmentUI {

	@Override
	public @LayoutRes int getRootViewId() {
		return R.layout.custom_searchresults_fragment;
	}

	@Override
	public RecyclerView getSearchResultsView(final View rootView) {
		return rootView.findViewById(R.id.searchResultsCustom);
	}
}
