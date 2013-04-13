/**
 * 
 */
package net.osmand.plus.activities;

import java.util.Comparator;
import java.util.List;

import net.londatiga.android.QuickAction;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;
import net.osmand.plus.ClientContext;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;

/**
 * 
 */
public class FavouritesListActivity extends ListActivity implements SearchActivityChild {

	public static final String SELECT_FAVORITE_POINT_INTENT_KEY = "SELECT_FAVORITE_POINT_INTENT_KEY"; 
	public static final int SELECT_FAVORITE_POINT_RESULT_OK = 1;
	
	private FavouritesAdapter favouritesAdapter;
	private LatLon location;

	private boolean selectFavoriteMode;
	private OsmandSettings settings;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		ListView lv = new ListView(this);
		lv.setId(android.R.id.list);
		lv.setCacheColorHint(getResources().getColor(R.color.activity_background));
		lv.setDivider(getResources().getDrawable(R.drawable.tab_text_separator));
		setContentView(lv);

		settings = ((OsmandApplication) getApplication()).getSettings();
		favouritesAdapter = new FavouritesAdapter(((OsmandApplication) getApplication()).getFavorites().getFavouritePoints());
		setListAdapter(favouritesAdapter);
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = getIntent();
		if (intent != null) {
			selectFavoriteMode = intent.hasExtra(SELECT_FAVORITE_POINT_INTENT_KEY);
			if (intent.hasExtra(SearchActivity.SEARCH_LAT) && intent.hasExtra(SearchActivity.SEARCH_LON)) {
				double lat = intent.getDoubleExtra(SearchActivity.SEARCH_LAT, 0);
				double lon = intent.getDoubleExtra(SearchActivity.SEARCH_LON, 0);
				if (lat != 0 || lon != 0) {
					location = new LatLon(lat, lon);
				}
			}
		}
		if (!isSelectFavoriteMode()) {
			if (location == null && getParent() instanceof SearchActivity) {
				location = ((SearchActivity) getParent()).getSearchPoint();
			}
			if (location == null) {
				location = settings.getLastKnownMapLocation();
			}
		}
		locationUpdate(location);

	}

	@Override
	public void locationUpdate(LatLon l) {
		location = l;
		favouritesAdapter.sort(new Comparator<FavouritePoint>() {
			@Override
			public int compare(FavouritePoint object1, FavouritePoint object2) {
				if (location != null) {
					double d1 = MapUtils.getDistance(location, object1.getLatitude(), object1.getLongitude());
					double d2 = MapUtils.getDistance(location, object2.getLatitude(), object2.getLongitude());
					if (d1 == d2) {
						return 0;
					} else if (d1 > d2) {
						return 1;
					}
					return -1;
				} else {
					return favouritesAdapter.getName(object1).compareTo(favouritesAdapter.getName(object2));
				}
			}
		});
	}
	
	public boolean isSelectFavoriteMode(){
		return selectFavoriteMode;
	}
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		if (!isSelectFavoriteMode()) {
			QuickAction qa = new QuickAction(v);
			FavouritePoint point = favouritesAdapter.getItem(position);
			String name = getString(R.string.favorite) + ": " + point.getName();
			LatLon location = new LatLon(point.getLatitude(), point.getLongitude());
			View.OnClickListener onshow = new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					settings.SHOW_FAVORITES.set(true);							
				}
			};
			MapActivityActions.createDirectionsActions(qa, location, point, name, settings.getLastKnownMapZoom(), this,
					true, onshow);
			qa.show();
		} else {
			Intent intent = getIntent();
			intent.putExtra(SELECT_FAVORITE_POINT_INTENT_KEY, favouritesAdapter.getItem(position));
			setResult(SELECT_FAVORITE_POINT_RESULT_OK, intent);
			finish();
		}
	}

	class FavouritesAdapter extends ArrayAdapter<FavouritePoint> {
		

		public FavouritesAdapter(List<FavouritePoint> list) {
			super(FavouritesListActivity.this, R.layout.favourites_list_item, list);
		}
		
		public String getName(FavouritePoint model){
			return model.getCategory() + " : " + model.getName();
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.favourites_list_item, parent, false);
			}

			TextView label = (TextView) row.findViewById(R.id.favourite_label);
			ImageView icon = (ImageView) row.findViewById(R.id.favourite_icon);
			final FavouritePoint model = getItem(position);
			if (model.isStored()) {
				icon.setImageResource(R.drawable.list_activities_favorites);
			} else {
				icon.setImageResource(R.drawable.opened_poi);
			}
			String distance = "";
			if (location != null) {
				int dist = (int) (MapUtils.getDistance(model.getLatitude(), model.getLongitude(), location.getLatitude(), location
						.getLongitude()));
				distance = OsmAndFormatter.getFormattedDistance(dist, (ClientContext) getApplication()) + "  " ;
			}
			
			label.setText(distance + getName(model), BufferType.SPANNABLE);
			((Spannable) label.getText()).setSpan(new ForegroundColorSpan(getResources().getColor(R.color.color_distance)), 0, distance.length(), 0);
			final CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);
			row.findViewById(R.id.favourite_icon).setVisibility(View.VISIBLE);
			ch.setVisibility(View.GONE);
			return row;
		}

	}


}
