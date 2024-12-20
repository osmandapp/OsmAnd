package net.osmand.plus.search.dialogs;

import net.osmand.plus.R;

public class QuickSearchAddressListFragment extends QuickSearchListFragment {

	public static final int TITLE = R.string.address;

	@Override
	public SearchListFragmentType getType() {
		return SearchListFragmentType.ADDRESS;
	}
}