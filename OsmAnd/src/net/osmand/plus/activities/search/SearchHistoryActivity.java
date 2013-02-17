package net.osmand.plus.activities.search;

import java.util.List;

import net.londatiga.android.QuickAction;
import net.osmand.osm.LatLon;
import net.osmand.util.MapUtils;
import net.osmand.plus.ClientContext;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.SearchHistoryHelper;
import net.osmand.plus.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
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

public class SearchHistoryActivity extends ListActivity  implements SearchActivityChild {
	private LatLon location;
	private SearchHistoryHelper helper;
	private Button clearButton;
	public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT;
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ListView lv = new ListView(this);
		lv.setCacheColorHint(getResources().getColor(R.color.activity_background));
		lv.setDivider(getResources().getDrawable(R.drawable.tab_text_separator));
		lv.setId(android.R.id.list);

		setContentView(lv);

		helper = SearchHistoryHelper.getInstance((ClientContext) getApplication());

		clearButton = new Button(this);
		clearButton.setText(R.string.clear_all);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				helper.removeAll();
				setListAdapter(new HistoryAdapter(helper.getHistoryEntries()));
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = getIntent();
		if (intent != null) {
			double lat = intent.getDoubleExtra(SEARCH_LAT, 0);
			double lon = intent.getDoubleExtra(SEARCH_LON, 0);
			if (lat != 0 || lon != 0) {
				location = new LatLon(lat, lon);
			}
		}
		if (location == null && getParent() instanceof SearchActivity) {
			location = ((SearchActivity) getParent()).getSearchPoint();
		}
		if (location == null) {
			location = ((OsmandApplication) getApplication()).getSettings().getLastKnownMapLocation();
		}

		List<HistoryEntry> historyEntries = helper.getHistoryEntries();

		getListView().removeFooterView(clearButton);
		if (!historyEntries.isEmpty()) {
			getListView().addFooterView(clearButton);
		}
		setListAdapter(new HistoryAdapter(historyEntries));
	}

	@Override
	public void locationUpdate(LatLon l) {
		location = l;
		((HistoryAdapter) getListAdapter()).notifyDataSetChanged();
	}


	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		HistoryEntry model = ((HistoryAdapter) getListAdapter()).getItem(position);
		selectModel(model, v);
	}

	private void selectModel(final HistoryEntry model, View v) {
		QuickAction qa = new QuickAction(v);
		String name = model.getName();
		OsmandSettings settings = ((OsmandApplication) getApplication()).getSettings();
		OnClickListener onShow = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				helper.selectEntry(model);				
			}
		};
		MapActivityActions.createDirectionsActions(qa, new LatLon(model.getLat(), model.getLon()),
				model, name, settings.getLastKnownMapZoom(), this, false, onShow);
		qa.show();
	}

	class HistoryAdapter extends ArrayAdapter<HistoryEntry> {

		public HistoryAdapter(List<HistoryEntry> list) {
			super(SearchHistoryActivity.this, R.layout.search_history_list_item, list);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.search_history_list_item, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.label);
			String distance = "";
			ImageButton icon = (ImageButton) row.findViewById(R.id.remove);
			final HistoryEntry model = getItem(position);
			if (location != null) {
				int dist = (int) (MapUtils.getDistance(location, model.getLat(), model.getLon()));
				distance = OsmAndFormatter.getFormattedDistance(dist, (ClientContext) getApplication()) + "  ";
			}
			label.setText(distance + model.getName(), BufferType.SPANNABLE);
			((Spannable) label.getText()).setSpan(new ForegroundColorSpan(getResources().getColor(R.color.color_distance)), 0, distance.length(), 0);
			icon.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					helper.remove(model);
					setListAdapter(new HistoryAdapter(helper.getHistoryEntries()));
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
