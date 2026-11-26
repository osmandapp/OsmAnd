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

	@Override
	@NonNull
	public SearchListFragmentType getType() {
		return SearchListFragmentType.MAIN;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListAdapter().registerDataSetObserver(listObserver);
	}

	@Override
	public void onDestroyView() {
		QuickSearchListAdapter adapter = getListAdapter();
		if (adapter != null) {
			adapter.unregisterDataSetObserver(listObserver);
		}
		super.onDestroyView();
	}
}
