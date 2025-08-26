package net.osmand.plus.search.dialogs;

import androidx.annotation.NonNull;

public class QuickSearchMainListFragment extends QuickSearchListFragment {

	@Override
	@NonNull
	public SearchListFragmentType getType() {
		return SearchListFragmentType.MAIN;
	}
}
