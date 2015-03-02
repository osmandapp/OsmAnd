/**
 *
 */
package net.osmand.plus.activities;

import java.util.Comparator;
import java.util.List;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.PopupMenu;
import android.view.*;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.util.MapUtils;

import android.app.Activity;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 *
 */
public class FavoritesListFragment extends ListFragment implements SearchActivityChild {

	public static final String SELECT_FAVORITE_POINT_INTENT_KEY = "SELECT_FAVORITE_POINT_INTENT_KEY";
	public static final int SELECT_FAVORITE_POINT_RESULT_OK = 1;

	private FavouritesAdapter favouritesAdapter;

	private boolean selectFavoriteMode;
	private OsmandSettings settings;
	private LatLon location;


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		settings = ((OsmandApplication) getApplication()).getSettings();
		OsmandApplication app = (OsmandApplication) getApplication();
		favouritesAdapter = new FavouritesAdapter(activity, app.getFavorites().getFavouritePoints());
		setListAdapter(favouritesAdapter);
		setHasOptionsMenu(true);
	}

	private OsmandApplication getApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public void onResume() {
		super.onResume();
		Intent intent = getActivity().getIntent();
		if (intent != null) {
			selectFavoriteMode = intent.hasExtra(SELECT_FAVORITE_POINT_INTENT_KEY);
			if (intent.hasExtra(SearchActivity.SEARCH_LAT) && intent.hasExtra(SearchActivity.SEARCH_LON)) {
				double lat = intent.getDoubleExtra(SearchActivity.SEARCH_LAT, 0);
				double lon = intent.getDoubleExtra(SearchActivity.SEARCH_LON, 0);
				if (lat != 0 || lon != 0) {
					favouritesAdapter.location = new LatLon(lat, lon);
				}
			}
		}

		if (location == null && getActivity() instanceof SearchActivity) {
			location = ((SearchActivity) getActivity()).getSearchPoint();
		}
		if (location == null) {
			location = settings.getLastKnownMapLocation();
		}

		locationUpdate(location);

	}

	@Override
	public void locationUpdate(LatLon l) {
		location = l;
		if (favouritesAdapter != null) {
			favouritesAdapter.updateLocation(l);
		}
	}

	public boolean isSelectFavoriteMode() {
		return selectFavoriteMode;
	}


	@Override
	public void onCreateOptionsMenu(Menu onCreate, MenuInflater inflater) {
		if (getActivity() instanceof SearchActivity) {
			((SearchActivity) getActivity()).getClearToolbar(false);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

		if (!isSelectFavoriteMode()) {
			FavouritePoint point = favouritesAdapter.getItem(position);
			LatLon location = new LatLon(point.getLatitude(), point.getLongitude());
			final PopupMenu optionsMenu = new PopupMenu(getActivity(), v);
			DirectionsDialogs.createDirectionActionsPopUpMenu(optionsMenu, location, point, point.getPointDescription(), settings.getLastKnownMapZoom(),
					getActivity(), true, false);
			optionsMenu.show();
		} else {
			Intent intent = getActivity().getIntent();
			intent.putExtra(SELECT_FAVORITE_POINT_INTENT_KEY, favouritesAdapter.getItem(position));
			getActivity().setResult(SELECT_FAVORITE_POINT_RESULT_OK, intent);
			getActivity().finish();
		}
	}

	public static class FavouritesAdapter extends ArrayAdapter<FavouritePoint> {
		private Activity activity;
		private LatLon location;
		private OsmandApplication app;
		Drawable arrowImage;

		public LatLon getLocation() {
			return location;
		}

		public void updateLocation(LatLon l) {
			location = l;
			sort(new Comparator<FavouritePoint>() {
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
						return getName(object1).compareTo(getName(object2));
					}
				}
			});
		}

		public FavouritesAdapter(Activity activity, List<FavouritePoint> list) {
			super(activity, R.layout.favorites_list_item, list);
			this.activity = activity;
			this.app = ((OsmandApplication) activity.getApplication());
			boolean light = app.getSettings().isLightContent();
			arrowImage = activity.getResources().getDrawable(R.drawable.ic_destination_arrow_white);
			arrowImage.mutate();
			if (light) {
				arrowImage.setColorFilter(activity.getResources().getColor(R.color.color_distance), PorterDuff.Mode.MULTIPLY);
			} else {
				arrowImage.setColorFilter(activity.getResources().getColor(R.color.color_distance), PorterDuff.Mode.MULTIPLY);
			}
		}

		public String getName(FavouritePoint model) {
			return model.getName();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = activity.getLayoutInflater();
				row = inflater.inflate(R.layout.favorites_list_item, parent, false);
			}

			TextView name = (TextView) row.findViewById(R.id.favourite_label);
			TextView distanceText = (TextView) row.findViewById(R.id.distance);
			ImageView icon = (ImageView) row.findViewById(R.id.favourite_icon);
			ImageView direction = (ImageView) row.findViewById(R.id.direction);
			direction.setImageDrawable(arrowImage);
			direction.setVisibility(View.VISIBLE);
			final FavouritePoint favorite = getItem(position);
			if (!favorite.getCategory().isEmpty()) {
				row.findViewById(R.id.group_image).setVisibility(View.VISIBLE);
			} else {
				row.findViewById(R.id.group_image).setVisibility(View.GONE);
			}
			((TextView) row.findViewById(R.id.group_name)).setText(favorite.getCategory());

			icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(activity, favorite.getColor()));
			String distance = "";
			if (location != null) {
				int dist = (int) (MapUtils.getDistance(favorite.getLatitude(), favorite.getLongitude(), location.getLatitude(), location
						.getLongitude()));
				distance = OsmAndFormatter.getFormattedDistance(dist, app) + "  ";
			}
			distanceText.setText(distance);
			name.setText(getName(favorite));
			final CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);
			row.findViewById(R.id.favourite_icon).setVisibility(View.VISIBLE);
			ch.setVisibility(View.GONE);
			return row;
		}

	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}
}
