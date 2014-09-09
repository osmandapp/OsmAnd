package net.osmand.plus.download;

import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;
import net.osmand.plus.activities.OsmandExpandableListFragment;

import java.util.List;
import java.util.Map;

/**
 * Created by Denis on 09.09.2014.
 */
public class UpdatesIndexFragment extends OsmandExpandableListFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public void onResume() {
		super.onResume();

		Map<IndexItem, List<DownloadEntry>> map = getDownloadActivity().getEntriesToDownload();

	}

	@Override
	public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i2, long l) {
		return false;
	}

	public DownloadActivity getDownloadActivity() { return (DownloadActivity)getActivity(); }
}
