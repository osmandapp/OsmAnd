package net.osmand.plus.download;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.app.SherlockListFragment;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandExpandableListFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Denis on 09.09.2014.
 */
public class UpdatesIndexFragment extends SherlockListFragment {

	private OsmandRegions osmandRegions;
	private java.text.DateFormat format;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		format = getMyApplication().getResourceManager().getDateFormat();
		osmandRegions = getMyApplication().getResourceManager().getOsmandRegions();
		setListAdapter(new UpdateIndexAdapter(getDownloadActivity(), R.layout.download_index_list_item, DownloadActivity.downloadListIndexThread.getItemsToUpdate()));
	}

	@Override
	public void onResume() {
		super.onResume();

		Map<IndexItem, List<DownloadEntry>> map = getDownloadActivity().getEntriesToDownload();

	}

	public void updateItemsList(List<IndexItem> items){
		UpdateIndexAdapter adapter = (UpdateIndexAdapter) getListAdapter();
		if (adapter == null){
			return;
		}
		adapter.clear();
		for (IndexItem item : items){
			adapter.add(item);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
	}

	public DownloadActivity getDownloadActivity() { return (DownloadActivity)getActivity(); }

	private class UpdateIndexAdapter extends ArrayAdapter<IndexItem>{
		List<IndexItem> items;

		public UpdateIndexAdapter(Context context, int resource, List<IndexItem> items) {
			super(context, resource, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			if(v == null){
				LayoutInflater inflater = (LayoutInflater) getDownloadActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.download_index_list_item, null);
			}

			TextView name = (TextView) v.findViewById(R.id.download_item);
			TextView description = (TextView) v.findViewById(R.id.download_descr);
			IndexItem e = items.get(position);
			String eName = e.getVisibleDescription(getMyApplication()) + "\n" + e.getVisibleName(getMyApplication(), osmandRegions);
			name.setText(eName.trim()); //$NON-NLS-1$
			String d = e.getDate(format) + "\n" + e.getSizeDescription(getMyApplication());
			description.setText(d);

			CheckBox ch = (CheckBox) v.findViewById(R.id.check_download_item);
			ch.setChecked(getDownloadActivity().getEntriesToDownload().containsKey(e));

			return v;
		}
	}

	public OsmandApplication getMyApplication() { return getDownloadActivity().getMyApplication(); }
}
