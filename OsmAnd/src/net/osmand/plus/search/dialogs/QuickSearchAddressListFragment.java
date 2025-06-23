package net.osmand.plus.search.dialogs;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public class QuickSearchAddressListFragment extends QuickSearchListFragment {

	public static final int TITLE = R.string.address;

	@Override
	@NonNull
	public SearchListFragmentType getType() {
		return SearchListFragmentType.ADDRESS;
	}
}