package net.osmand.plus.search.dialogs;

import net.osmand.plus.R;

public class QuickSearchCategoriesListFragment extends QuickSearchListFragment {

	public static final int TITLE = R.string.search_categories;

	@Override
	public SearchListFragmentType getType() {
		return SearchListFragmentType.CATEGORIES;
	}
}