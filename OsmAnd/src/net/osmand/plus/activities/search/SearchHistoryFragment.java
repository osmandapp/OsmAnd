package net.osmand.plus.activities.search;

import java.util.List;

import net.londatiga.android.QuickAction;
import net.osmand.data.LatLon;
import net.osmand.plus.ClientContext;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.SearchHistoryHelper;
import net.osmand.plus.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import net.osmand.util.MapUtils;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.actionbarsherlock.app.SherlockListFragment;

public class SearchHistoryFragment extends SherlockListFragment  implements SearchActivityChild {
	private LatLon location;
	private SearchHistoryHelper helper;
	private Button clearButton;
	public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT;
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON;
	private HistoryAdapter historyAdapter;

	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		helper = SearchHistoryHelper.getInstance((ClientContext) getActivity().getApplicationContext());
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		historyAdapter = new HistoryAdapter(helper.getHistoryEntries());
		clearButton = new Button(getActivity());
		clearButton.setText(R.string.clear_all);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				helper.removeAll();
				historyAdapter.clear();
				clearButton.setVisibility(View.GONE);
			}
		});
		getListView().addFooterView(clearButton);
		setListAdapter(historyAdapter);
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		FragmentActivity activity = getActivity();
		Intent intent = activity.getIntent();
		if (intent != null) {
			double lat = intent.getDoubleExtra(SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SEARCH_LON, 0);
			if (lat != 0 || lon != 0) {
				location = new LatLon(lat, lon);
			}
		}
		if (location == null && activity instanceof SearchActivity) {
			location = ((SearchActivity) activity).getSearchPoint();
		}
		if (location == null) {
			location = ((OsmandApplication) activity.getApplication()).getSettings().getLastKnownMapLocation();
		}

		clearButton.setVisibility(historyAdapter.isEmpty() ? View.GONE : View.VISIBLE);
		
	}

	@Override
	public void locationUpdate(LatLon l) {
		location = l;
		historyAdapter.notifyDataSetChanged();
	}


	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		HistoryEntry model = ((HistoryAdapter) getListAdapter()).getItem(position);
		selectModel(model, v);
	}

	private void selectModel(final HistoryEntry model, View v) {
		QuickAction qa = new QuickAction(v);
		String name = model.getName();
		OsmandSettings settings = ((OsmandApplication) getActivity().getApplication()).getSettings();
		OnClickListener onShow = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				helper.selectEntry(model);				
			}
		};
		MapActivityActions.createDirectionsActions(qa, new LatLon(model.getLat(), model.getLon()),
				model, name, settings.getLastKnownMapZoom(), getActivity(), false, onShow);
		qa.show();
	}

	class HistoryAdapter extends ArrayAdapter<HistoryEntry> {

		public HistoryAdapter(List<HistoryEntry> list) {
			super(getActivity(), R.layout.search_history_list_item, list);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.search_history_list_item, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.label);
			String distance = "";
			ImageButton icon = (ImageButton) row.findViewById(R.id.remove);
			final HistoryEntry model = getItem(position);
			if (location != null) {
				int dist = (int) (MapUtils.getDistance(location, model.getLat(), model.getLon()));
				distance = OsmAndFormatter.getFormattedDistance(dist, (ClientContext) getActivity().getApplication()) + "  ";
			}
			label.setText(distance + model.getName(), BufferType.SPANNABLE);
			((Spannable) label.getText()).setSpan(new ForegroundColorSpan(getResources().getColor(R.color.color_distance)), 0, distance.length(), 0);
			icon.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					helper.remove(model);
					historyAdapter.remove(model);
				}

			});
			View.OnClickListener clickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					selectModel(model, v);
				}
			};

			label.setOnClickListener(clickListener);
			return row;
		}
	}

}
