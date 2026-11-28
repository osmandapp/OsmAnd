package net.osmand.plus.search.dialogs;

import android.database.DataSetObserver;
import android.os.Bundle;

import androidx.annotation.NonNull;

public class QuickSearchMainListFragment extends QuickSearchListFragment {

	private final DataSetObserver listObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			getDialogFragment().onMainListChanged();
		}
	};
	private QuickSearchListAdapter observedAdapter;

	@Override
	@NonNull
	public SearchListFragmentType getType() {
		return SearchListFragmentType.MAIN;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		observedAdapter = getListAdapter();
		observedAdapter.registerDataSetObserver(listObserver);
	}

	@Override
	public void onDestroyView() {
		// a view created again without onActivityCreated, as when the search dialog is
		// refreshed after a uiMode change, has no observer registered
		if (observedAdapter != null) {
			observedAdapter.unregisterDataSetObserver(listObserver);
			observedAdapter = null;
		}
		super.onDestroyView();
	}
}
