package net.osmand.plus.mapcontextmenu.other;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.Location;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.util.MapUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FavouritesBottomSheetMenuFragment extends MenuBottomSheetDialogFragment
		implements OsmAndLocationProvider.OsmAndCompassListener, OsmAndLocationProvider.OsmAndLocationListener {

	public static final String TARGET = "target";
	public static final String INTERMEDIATE = "intermediate";
	public static final String TAG = "AddGroupBottomSheetDialogFragment";

	private MapActivity mapActivity;

	private Location location;
	private Float heading;
	private boolean sortByDist = false;

	private boolean locationUpdateStarted;
	private boolean compassUpdateAllowed = true;
	private List<FavouritePoint> list;
	private FavouritesAdapter adapter;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		FavouritesDbHelper favouritesDbHelper = getMyApplication().getFavorites();
		mapActivity = (MapActivity) getActivity();
		list = favouritesDbHelper.getVisibleFavouritePoints();

		View titleView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.bottom_sheet_item_favourite_title, null);
		View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.fragment_marker_add_group_bottom_sheet_dialog, null);

		TextView title = (TextView) titleView.findViewById(R.id.title);
		final ImageView sortIconView = (ImageView) titleView.findViewById(R.id.sort_icon);
		final TextView sortText = (TextView) titleView.findViewById(R.id.sort_text);
		LinearLayout sort = (LinearLayout) titleView.findViewById(R.id.sort_by);
		final RecyclerView recyclerView = (RecyclerView) mainView.findViewById(R.id.groups_recycler_view);

		title.setText(R.string.favourites);
		sortIconView.setImageDrawable(getIcon(R.drawable.ic_action_sort_by_name, nightMode ? R.color.route_info_go_btn_inking_dark : R.color.dash_search_icon_light));
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		sortText.setText(R.string.sort_by_name);
		sort.setOnClickListener(new View.OnClickListener() {
			                        @Override
			                        public void onClick(View v) {
				                        if (location == null) {
					                        return;
				                        }
				                        sortFavourites(sortByDist, list);
				                        sortText.setText(sortByDist ? R.string.sort_by_distance : R.string.sort_by_name);
				                        sortIconView.setImageDrawable(getIcon(sortByDist ? R.drawable.ic_action_list_sort : R.drawable.ic_action_sort_by_name,
						                        nightMode ? R.color.route_info_go_btn_inking_dark : R.color.dash_search_icon_light));
				                        adapter.notifyDataSetChanged();
			                        }
		                        }
		);
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(titleView)
				.create());

		createAdapter();
		adapter.setAdapterListener(new FavouritesAdapter.FavouritesAdapterListener() {
			@Override
			public void onItemClick(View view) {
				int position = recyclerView.getChildAdapterPosition(view);
				if (position == RecyclerView.NO_POSITION) {
					return;
				}
				selectFavorite(list.get(position));
			}
		});
		recyclerView.setLayoutManager(new LinearLayoutManager(mapActivity, LinearLayoutManager.VERTICAL, false));
		recyclerView.setAdapter(adapter);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				compassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});
		items.add(new BaseBottomSheetItem.Builder().
				setCustomView(mainView).
				create());
	}

	private void sortFavourites(boolean sortByDist, List<FavouritePoint> favourites) {
		if (sortByDist) {
			Collections.sort(favourites, new Comparator<FavouritePoint>() {
				@Override
				public int compare(FavouritePoint lhs, FavouritePoint rhs) {
					LatLon latLon = new LatLon(location.getLatitude(), location.getLongitude());
					double d1 = MapUtils.getDistance(latLon, lhs.getLatitude(), lhs.getLongitude());
					double d2 = MapUtils.getDistance(latLon, rhs.getLatitude(), rhs.getLongitude());
					if (d1 == d2) {
						return 0;
					} else if (d1 > d2) {
						return 1;
					}
					return -1;
				}
			});
			this.sortByDist = false;
		} else {
			Collections.sort(favourites, new Comparator<FavouritePoint>() {
				@Override
				public int compare(FavouritePoint lhs, FavouritePoint rhs) {
					return lhs.getName().compareTo(rhs.getName());
				}
			});
			this.sortByDist = true;
		}
	}

	private void selectFavorite(FavouritePoint point) {
		Bundle args = getArguments();
		boolean target = args.getBoolean(TARGET);
		boolean intermediate = args.getBoolean(INTERMEDIATE);
		final MapRouteInfoMenu routeMenu = mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu();

		LatLon ll = new LatLon(point.getLatitude(), point.getLongitude());
		if (intermediate) {
			routeMenu.getTargets().navigateToPoint(ll, true, routeMenu.getTargets().getIntermediatePoints().size(), point.getPointDescription());
		} else if (target) {
			routeMenu.getTargets().navigateToPoint(ll, true, -1, point.getPointDescription());
		} else {
			routeMenu.getTargets().setStartPoint(ll, true, point.getPointDescription());
		}
		if (!intermediate) {
			routeMenu.updateFromIcon();
		}
		dismiss();
	}

	public void createAdapter() {
		adapter = new FavouritesAdapter(mapActivity, list);
	}

	@Override
	public void onResume() {
		super.onResume();
		adapter.setScreenOrientation(DashLocationFragment.getScreenOrientation(mapActivity));
		startLocationUpdate();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopLocationUpdate();
	}

	@Override
	public void updateLocation(Location location) {
		boolean newLocation = this.location == null && location != null;
		boolean locationChanged = this.location != null && location != null
				&& this.location.getLatitude() != location.getLatitude()
				&& this.location.getLongitude() != location.getLongitude();
		if (newLocation || locationChanged) {
			this.location = location;
			updateLocationUi();
		}
	}

	@Override
	public void updateCompassValue(float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			updateLocationUi();
		} else {
			heading = lastHeading;
		}
	}

	private void updateLocationUi() {
		if (!compassUpdateAllowed) {
			return;
		}
		final MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null && adapter != null) {
			mapActivity.getMyApplication().runInUIThread(new Runnable() {
				@Override
				public void run() {
					if (location == null) {
						location = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
					}
					if (location == null) {
						return;
					}
					adapter.setUseCenter(false);
					adapter.setLocation(new LatLon(location.getLatitude(), location.getLongitude()));
					adapter.setHeading(heading != null ? heading : 99);
					adapter.notifyDataSetChanged();
				}
			});
		}
	}

	private void startLocationUpdate() {
		OsmandApplication app = getMyApplication();
		if (app != null && !locationUpdateStarted) {
			locationUpdateStarted = true;
			app.getLocationProvider().removeCompassListener(app.getLocationProvider().getNavigationInfo());
			app.getLocationProvider().addCompassListener(this);
			app.getLocationProvider().addLocationListener(this);
			updateLocationUi();
		}
	}

	private void stopLocationUpdate() {
		OsmandApplication app = getMyApplication();
		if (app != null && locationUpdateStarted) {
			locationUpdateStarted = false;
			app.getLocationProvider().removeLocationListener(this);
			app.getLocationProvider().removeCompassListener(this);
			app.getLocationProvider().addCompassListener(app.getLocationProvider().getNavigationInfo());
		}
	}
}
