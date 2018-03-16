package net.osmand.plus.mapcontextmenu.other;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import net.osmand.Location;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithTitleAndButton;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.util.MapUtils;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FavouritesBottomSheetMenuFragment extends MenuBottomSheetDialogFragment
		implements OsmAndLocationProvider.OsmAndCompassListener, OsmAndLocationProvider.OsmAndLocationListener {

	public static final String TARGET = "target";
	public static final String INTERMEDIATE = "intermediate";
	public static final String TAG = "FavouritesBottomSheetMenuFragment";
	private static final String IS_SORTED = "sorted";
	private static final String SORTED_BY_TYPE = "sortedByType";

	private Location location;
	private Float heading;
	private List<FavouritePoint> favouritePoints;
	private FavouritesAdapter adapter;
	private RecyclerView recyclerView;
	private boolean sortByDist = false;
	private boolean isSorted = false;
	private boolean locationUpdateStarted;
	private boolean compassUpdateAllowed = true;
	private boolean target;
	private boolean intermediate;
	private BottomSheetItemWithTitleAndButton title;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args != null) {
			target = args.getBoolean(TARGET);
			intermediate = args.getBoolean(INTERMEDIATE);
		}

		favouritePoints = getMyApplication().getFavorites().getVisibleFavouritePoints();
		recyclerView = new RecyclerView(getContext());
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		location = getMyApplication().getLocationProvider().getLastKnownLocation();
		title = (BottomSheetItemWithTitleAndButton) new BottomSheetItemWithTitleAndButton.Builder()
				.setButtonIcons(null, getIcon(sortByDist ? R.drawable.ic_action_list_sort : R.drawable.ic_action_sort_by_name,
						nightMode ? R.color.route_info_go_btn_inking_dark : R.color.dash_search_icon_light))
				.setButtonTitle(getString(sortByDist ? R.string.sort_by_distance : R.string.sort_by_name))
				.setonButtonClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						title.setButtonIcons(null, getIcon(sortByDist ? R.drawable.ic_action_list_sort : R.drawable.ic_action_sort_by_name,
								nightMode ? R.color.route_info_go_btn_inking_dark : R.color.dash_search_icon_light));
						title.setButtonText(getString(sortByDist ? R.string.sort_by_distance : R.string.sort_by_name));
						sortFavourites();
					}
				})
				.setTitle(getString(R.string.favourites))
				.setLayoutId(R.layout.bottom_sheet_item_with_title_and_button)
				.create();
		items.add(title);

		adapter = new FavouritesAdapter(getContext(), favouritePoints);
		adapter.setItemClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int position = recyclerView.getChildAdapterPosition(v);
				if (position == RecyclerView.NO_POSITION) {
					return;
				}
				selectFavorite(favouritePoints.get(position));
			}
		});
		recyclerView.setAdapter(adapter);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				compassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(recyclerView)
				.create());
		if (savedInstanceState != null && savedInstanceState.getBoolean(IS_SORTED)) {
			sortByDist = savedInstanceState.getBoolean(SORTED_BY_TYPE);
			sortFavourites();
		}
	}

	private void selectFavorite(FavouritePoint point) {
		TargetPointsHelper targetPointsHelper = getMyApplication().getTargetPointsHelper();
		LatLon ll = new LatLon(point.getLatitude(), point.getLongitude());
		if (intermediate) {
			targetPointsHelper.navigateToPoint(ll, true, targetPointsHelper.getIntermediatePoints().size(), point.getPointDescription());
		} else if (target) {
			targetPointsHelper.navigateToPoint(ll, true, -1, point.getPointDescription());
		} else {
			targetPointsHelper.setStartPoint(ll, true, point.getPointDescription());
		}
		MapRouteInfoMenu routeMenu = getMapRouteInfoMenu();
		if (routeMenu != null) {
			setupMapRouteInfoMenuSpinners(routeMenu);
			updateMapRouteInfoMenuFromIcon(routeMenu);
		}
		dismiss();
	}

	private void setupMapRouteInfoMenuSpinners(MapRouteInfoMenu routeMenu) {
		if (routeMenu != null) {
			routeMenu.setupSpinners(target, intermediate);
		}
	}

	private void updateMapRouteInfoMenuFromIcon(MapRouteInfoMenu routeMenu) {
		if (!intermediate) {
			routeMenu.updateFromIcon();
		}
	}

	private MapRouteInfoMenu getMapRouteInfoMenu() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			MapActivity map = ((MapActivity) activity);
			return map.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu();
		} else {
			return null;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		adapter.setScreenOrientation(DashLocationFragment.getScreenOrientation(getActivity()));
		startLocationUpdate();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopLocationUpdate();
		setupMapRouteInfoMenuSpinners(getMapRouteInfoMenu());
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
						location = getMyApplication().getLocationProvider().getLastKnownLocation();
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

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(IS_SORTED, isSorted);
		outState.putBoolean(SORTED_BY_TYPE, !sortByDist);
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	private void sortFavourites() {
		if (location == null) {
			return;
		}
		final Collator inst = Collator.getInstance();
		final LatLon latLon = new LatLon(location.getLatitude(), location.getLongitude());
		Collections.sort(favouritePoints, new Comparator<FavouritePoint>() {
			@Override
			public int compare(FavouritePoint lhs, FavouritePoint rhs) {
				if (sortByDist) {
					double ld = MapUtils.getDistance(latLon, lhs.getLatitude(),
							lhs.getLongitude());
					double rd = MapUtils.getDistance(latLon, rhs.getLatitude(),
							rhs.getLongitude());
					return Double.compare(ld, rd);
				}
				return inst.compare(lhs.getName(), rhs.getName());
			}
		});
		sortByDist = !sortByDist;
		isSorted = true;
		adapter.notifyDataSetChanged();
	}
}
