/**
 * 
 */
package net.osmand.plus.activities;

import java.util.Comparator;
import java.util.List;

import net.osmand.FavouritePoint;
import net.osmand.OsmAndFormatter;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * 
 */
public class FavouritesListActivity extends ListActivity implements SearchActivityChild {

	public static final String SELECT_FAVORITE_POINT_INTENT_KEY = "SELECT_FAVORITE_POINT_INTENT_KEY"; 
	public static final int SELECT_FAVORITE_POINT_RESULT_OK = 1;
	
	private FavouritesAdapter favouritesAdapter;
	private LatLon location;

	private boolean selectFavoriteMode;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		ListView lv = new ListView(this);
		lv.setId(android.R.id.list);
		lv.setCacheColorHint(getResources().getColor(R.color.activity_background));
		lv.setDivider(getResources().getDrawable(R.drawable.tab_text_separator));
		setContentView(lv);

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
				location = OsmandSettings.getOsmandSettings(this).getLastKnownMapLocation();
			}
		}
		locationUpdate(location);

		if (!isSelectFavoriteMode()) {
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					return FavouritesListActivity.this.onItemLongClick(position);
				}
			});
		}
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
	
	
	private boolean onItemLongClick(int pos) {
		final FavouritePoint entry = favouritesAdapter.getItem(pos);
		AlertDialog.Builder builder = new AlertDialog.Builder(FavouritesListActivity.this);
		builder.setTitle(entry.getName());
		OnClickListener onClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					OsmandSettings settings = OsmandSettings.getOsmandSettings(FavouritesListActivity.this);
					settings.setMapLocationToShow(entry.getLatitude(), entry.getLongitude(),  settings.getLastKnownMapZoom(), 
							 null, getString(R.string.favorite)+" : " + entry.getName(), entry); //$NON-NLS-1$
				} else if (which == 1) {
					OsmandSettings.getOsmandSettings(FavouritesListActivity.this).setPointToNavigate(entry.getLatitude(),
							entry.getLongitude(), getString(R.string.favorite) + " : " + entry.getName());
				}
				MapActivity.launchMapActivityMoveToTop(FavouritesListActivity.this);
			}
		};
		builder.setItems(new String[] { getString(R.string.show_poi_on_map), getString(R.string.navigate_to) }, onClickListener);
		builder.show();
		return true;
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		if (!isSelectFavoriteMode()) {
			OsmandSettings settings = OsmandSettings.getOsmandSettings(this);
			FavouritePoint point = favouritesAdapter.getItem(position);
			settings.SHOW_FAVORITES.set(true);
			settings.setMapLocationToShow(point.getLatitude(), point.getLongitude(), settings.getLastKnownMapZoom(), null,
					getString(R.string.favorite) + " : " + point.getName(), point); //$NON-NLS-1$
			MapActivity.launchMapActivityMoveToTop(FavouritesListActivity.this);
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
			TextView distanceLabel = (TextView) row.findViewById(R.id.favouritedistance_label);
			ImageView icon = (ImageView) row.findViewById(R.id.favourite_icon);
			final FavouritePoint model = getItem(position);
			if (model.isStored()) {
				icon.setImageResource(R.drawable.favorites);
			} else {
				icon.setImageResource(R.drawable.opened_poi);
			}
			if (location != null) {
				int dist = (int) (MapUtils.getDistance(model.getLatitude(), model.getLongitude(), location.getLatitude(), location
						.getLongitude()));
				distanceLabel.setText(OsmAndFormatter.getFormattedDistance(dist, FavouritesListActivity.this));
				distanceLabel.setVisibility(View.VISIBLE);
			} else {
				distanceLabel.setVisibility(View.GONE);
			}

			label.setText(getName(model));
			final CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);
			row.findViewById(R.id.favourite_icon).setVisibility(View.VISIBLE);
			ch.setVisibility(View.GONE);
			return row;
		}

	}


}
