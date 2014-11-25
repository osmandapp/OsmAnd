package net.osmand.plus.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import net.osmand.plus.R;
import net.osmand.plus.download.BaseDownloadActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadEntry;
import net.osmand.plus.download.IndexItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis on 21.11.2014.
 */
public class DashUpdatesFragment extends DashBaseFragment {
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_updates_fragment, container, false);
		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent intent = new Intent(view.getContext(), getMyApplication().getAppCustomization().getDownloadIndexActivity());
				intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.UPDATES_TAB);
				getActivity().startActivity(intent);
			}
		});
		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (BaseDownloadActivity.downloadListIndexThread != null){
			updatedDownloadsList(BaseDownloadActivity.downloadListIndexThread.getItemsToUpdate());
		}
	}

	public void updatedDownloadsList(List<IndexItem> list) {
		View mainView = getView();
		//it may be null because download index thread is async
		if (mainView == null) {
			return;
		}
		if (list.size() > 0) {
			mainView.setVisibility(View.VISIBLE);
		} else {
			mainView.setVisibility(View.GONE);
			return;
		}

		((TextView)mainView.findViewById(R.id.update_count)).setText(String.valueOf(list.size()));

		LinearLayout updates = (LinearLayout) mainView.findViewById(R.id.updates_items);
		updates.removeAllViews();

		for (int i = 0; i < list.size(); i++) {
			final IndexItem item = list.get(i);
			if (i > 2) {
				break;
			}
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.dash_updates_item, null, false);
			String name = item.getVisibleName(getMyApplication(), getMyApplication().getResourceManager().getOsmandRegions());
			String d = item.getDate(getMyApplication().getResourceManager().getDateFormat()) + ", " + item.getSizeDescription(getMyApplication());
			String eName = name.replace("\n", " ");
			((TextView) view.findViewById(R.id.map_name)).setText(eName);
			((TextView) view.findViewById(R.id.map_descr)).setText(d);
			(view.findViewById(R.id.btn_download)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					List<DownloadEntry> download = item.createDownloadEntry(getMyApplication(), item.getType(), new ArrayList<DownloadEntry>());
					getDownloadActivity().getEntriesToDownload().put(item, download);
				}
			});
			updates.addView(view);
		}
	}

	private BaseDownloadActivity getDownloadActivity(){
		return (BaseDownloadActivity)getActivity();
	}
}
