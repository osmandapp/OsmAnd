package net.osmand.plus.activities.search;

import java.util.List;

import android.support.v4.app.ListFragment;
import android.view.*;
import net.osmand.data.LatLon;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.util.MapUtils;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.ActionMenuView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;


public class SearchHistoryFragment extends ListFragment implements SearchActivityChild {
	private LatLon location;
	private SearchHistoryHelper helper;
	private Button clearButton;
	public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT;
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON;
	private HistoryAdapter historyAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.search_history, container, false);
		clearButton = (Button) view.findViewById(R.id.clearAll);
		clearButton.setText(R.string.clear_all);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				helper.removeAll();
				historyAdapter.clear();
				clearButton.setVisibility(View.GONE);
			}
		});
		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		helper = SearchHistoryHelper.getInstance((OsmandApplication) getActivity().getApplicationContext());
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		historyAdapter = new HistoryAdapter(helper.getHistoryEntries());
		setListAdapter(historyAdapter);
		setHasOptionsMenu(true);
	}
	
	
	@Override
	public void onResume() {
		super.onResume();

		//Hardy: onResume() code is needed so that search origin is properly reflected in tab contents when origin has been changed on one tab, then tab is changed to another one.
		location = null;
		FragmentActivity activity = getActivity();
		Intent intent = activity.getIntent();
		if (intent != null) {
			double lat = intent.getDoubleExtra(SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SEARCH_LON, 0);
			if (lat != 0 || lon != 0) {
				historyAdapter.location = new LatLon(lat, lon);
			}
		}
		if (location == null && activity instanceof SearchActivity) {
			location = ((SearchActivity) activity).getSearchPoint();
		}
		if (location == null) {
			location = ((OsmandApplication) activity.getApplication()).getSettings().getLastKnownMapLocation();
		}
		locationUpdate(location);
		clearButton.setVisibility(historyAdapter.isEmpty() ? View.GONE : View.VISIBLE);
		
	}

	@Override
	public void locationUpdate(LatLon l) {
		//location = l;
		if(historyAdapter != null) {
			historyAdapter.updateLocation(l);
		}
	}


	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		HistoryEntry model = ((HistoryAdapter) getListAdapter()).getItem(position);
		selectModel(model, v);
	}

	private void selectModel(final HistoryEntry model, View v) {
		ContextMenuAdapter qa = new ContextMenuAdapter(v.getContext());
		qa.setAnchor(v);
		String name = model.getName();
		OsmandSettings settings = ((OsmandApplication) getActivity().getApplication()).getSettings();
		OnClickListener onShow = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				helper.selectEntry(model);				
			}
		};
		DirectionsDialogs.createDirectionsActions(qa, new LatLon(model.getLat(), model.getLon()),
				model, name, settings.getLastKnownMapZoom(), getActivity(), false);
		MapActivityActions.showObjectContextMenu(qa, getActivity(), onShow);
	}

	class HistoryAdapter extends ArrayAdapter<HistoryEntry> {
		private LatLon location;

		public void updateLocation(LatLon l) {
			location = l;
			notifyDataSetChanged();
		}

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
				distance = OsmAndFormatter.getFormattedDistance(dist, (OsmandApplication) getActivity().getApplication()) + "  ";
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

	@Override
	public void onCreateOptionsMenu(Menu onCreate, MenuInflater inflater) {
		if(getActivity() instanceof SearchActivity) {
			 ((SearchActivity) getActivity()).getClearToolbar(false);
		}
	}
}
