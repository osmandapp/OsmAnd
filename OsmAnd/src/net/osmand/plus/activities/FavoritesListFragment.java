/**
 *
 */
package net.osmand.plus.activities;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.activities.search.SearchActivity.SearchActivityChild;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.util.MapUtils;

import java.util.Comparator;
import java.util.List;

public class FavoritesListFragment extends OsmAndListFragment implements SearchActivityChild, OsmAndCompassListener {

	public static final String SELECT_FAVORITE_POINT_INTENT_KEY = "SELECT_FAVORITE_POINT_INTENT_KEY";
	public static final int SELECT_FAVORITE_POINT_RESULT_OK = 1;

	private FavouritesAdapter favouritesAdapter;

	private boolean selectFavoriteMode;
	private OsmandSettings settings;
	private boolean compassRegistered;



	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Intent intent = activity.getIntent();
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
		settings = ((OsmandApplication) getApplication()).getSettings();
		OsmandApplication app = (OsmandApplication) getApplication();
		favouritesAdapter = new FavouritesAdapter(activity, app.getFavorites().getFavouritePoints(),
				!selectFavoriteMode);
		setListAdapter(favouritesAdapter);
		setHasOptionsMenu(true);
	}

	private OsmandApplication getApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (favouritesAdapter.location == null && getActivity() instanceof SearchActivity) {
			favouritesAdapter.location = ((SearchActivity) getActivity()).getSearchPoint();
		}
		if (favouritesAdapter.location == null) {
			favouritesAdapter.location = settings.getLastKnownMapLocation();
		}
		favouritesAdapter.screenOrientation = DashLocationFragment.getScreenOrientation(getActivity());
		locationUpdate(favouritesAdapter.location);
	}

	@Override
	public void locationUpdate(LatLon l) {
		if (getActivity() instanceof SearchActivity) {
			if (((SearchActivity) getActivity()).isSearchAroundCurrentLocation() && l != null) {
				if (!compassRegistered) {
					((OsmandApplication) getActivity().getApplication()).getLocationProvider().addCompassListener(this);
					compassRegistered = true;
				}
				favouritesAdapter.searchAroundLocation = true;
			} else {
				favouritesAdapter.searchAroundLocation = false;
			}
		}
		if (favouritesAdapter != null) {
			favouritesAdapter.updateLocation(l);
		}

	}

	@Override
	public void onPause() {
		super.onPause();
		if(getActivity() instanceof SearchActivity) {
			((OsmandApplication) getActivity().getApplication()).getLocationProvider().removeCompassListener(this);
			compassRegistered = false;
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
			showItemPopupOptionsMenu(point, getActivity(), v);
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
		private boolean searchAroundLocation;
		private int screenOrientation;
		private Float heading;
		private boolean shoudShowMenuButton;

		public FavouritesAdapter(Activity activity, List<FavouritePoint> list,
								 boolean shoudShowMenuButton) {
			super(activity, R.layout.favorites_list_item, list);
			this.activity = activity;
			this.app = ((OsmandApplication) activity.getApplication());
			this.shoudShowMenuButton = shoudShowMenuButton;
		}

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

		public String getName(FavouritePoint model) {
			return model.getName();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = activity.getLayoutInflater(); // favourite dank
				row = inflater.inflate(R.layout.favorites_list_item, parent, false);
			}

			TextView name = (TextView) row.findViewById(R.id.favourite_label);
			TextView distanceText = (TextView) row.findViewById(R.id.distance);
			ImageView icon = (ImageView) row.findViewById(R.id.favourite_icon);
			ImageView direction = (ImageView) row.findViewById(R.id.direction);
			ImageView giImage= (ImageView)row.findViewById(R.id.group_image);
			direction.setVisibility(View.VISIBLE);
			final FavouritePoint favorite = getItem(position);
			if (shoudShowMenuButton) {
				ImageButton options = (ImageButton) row.findViewById(R.id.options);
				options.setFocusable(false);
				options.setImageDrawable(((OsmandApplication) activity.getApplication())
						.getIconsCache().getContentIcon(R.drawable.ic_overflow_menu_white));
				options.setVisibility(View.VISIBLE);
				options.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showItemPopupOptionsMenu(favorite, activity, v);
					}
				});
			}
			if (!favorite.getCategory().isEmpty()) {
				giImage.setVisibility(View.VISIBLE);
				giImage.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.ic_small_group));
			} else {
				giImage.setVisibility(View.GONE);
			}
			((TextView) row.findViewById(R.id.group_name)).setText(favorite.getCategory());

			icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(activity, favorite.getColor(), 0));
			DashLocationFragment.updateLocationView(!searchAroundLocation, location, heading, direction, distanceText,
					favorite.getLatitude(), favorite.getLongitude(), screenOrientation, app, activity);

			name.setText(getName(favorite));
			final CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);
			icon.setVisibility(View.VISIBLE);
			ch.setVisibility(View.GONE);
			return row;
		}

	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public void updateCompassValue(float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction) on non-compass
		// devices
		float lastHeading = favouritesAdapter.heading != null ? favouritesAdapter.heading : 99;
		favouritesAdapter.heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, favouritesAdapter.heading)) > 5) {
			favouritesAdapter.notifyDataSetChanged();
		} else {
			favouritesAdapter.heading = lastHeading;
		}
	}

	public static void showItemPopupOptionsMenu(FavouritePoint point, Activity activity, View view) {
		LatLon location = new LatLon(point.getLatitude(), point.getLongitude());
		final PopupMenu optionsMenu = new PopupMenu(activity, view);
		DirectionsDialogs.createDirectionActionsPopUpMenu(optionsMenu, location,
				point, point.getPointDescription(),
				((OsmandApplication) activity.getApplication()).getSettings().getLastKnownMapZoom(),
				activity, true, false);
		optionsMenu.show();
	}
}
