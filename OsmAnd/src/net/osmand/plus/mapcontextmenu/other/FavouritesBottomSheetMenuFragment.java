package net.osmand.plus.mapcontextmenu.other;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoritesListener;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemTitleWithDescrAndButton;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.PointType;
import net.osmand.util.MapUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FavouritesBottomSheetMenuFragment extends MenuBottomSheetDialogFragment
		implements OsmAndLocationProvider.OsmAndCompassListener, OsmAndLocationProvider.OsmAndLocationListener {

	public static final String POINT_TYPE_KEY = "point_type";
	public static final String TAG = "FavouritesBottomSheetMenuFragment";
	private static final String IS_SORTED = "sorted";
	private static final String SORTED_BY_TYPE = "sortedByType";
	private static final int SORT_TYPE_DIST = 1;
	private static final int SORT_TYPE_NAME = 2;
	private static final int SORT_TYPE_CATEGORY = 3;
	private static int getNextType(int type) {
		return type % SORT_TYPE_CATEGORY + 1;
	}

	private List<FavouritePoint> favouritePoints = new ArrayList<>();
	private FavouritesAdapter adapter;
	private RecyclerView recyclerView;
	private int sortByDist = SORT_TYPE_DIST;
	private boolean isSorted = false;
	private boolean locationUpdateStarted;
	private boolean compassUpdateAllowed = true;
	private PointType pointType;
	private Location location;
	private float lastHeading;

	private FavoritesListener favoritesListener;

	@Override
	public void createMenuItems(final Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args != null) {
			pointType = PointType.valueOf(args.getString(POINT_TYPE_KEY));
		}
		if (savedInstanceState != null && savedInstanceState.getBoolean(IS_SORTED)) {
			sortByDist = savedInstanceState.getInt(SORTED_BY_TYPE);
		}
		adapter = new FavouritesAdapter(getMyApplication(), favouritePoints);
		FavouritesDbHelper helper = getMyApplication().getFavorites();
		if (helper.isFavoritesLoaded()) {
			loadFavorites();
		} else {
			helper.addListener(favoritesListener = new FavoritesListener() {
				@Override
				public void onFavoritesLoaded() {
					loadFavorites();
					adapter.notifyDataSetChanged();
				}

				@Override
				public void onFavoriteDataUpdated(@NonNull FavouritePoint favouritePoint) {
				}
			});
		}
		recyclerView = new RecyclerView(getContext());
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		recyclerView = (RecyclerView) View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.recyclerview, null);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		sortFavourites();
		final BottomSheetItemTitleWithDescrAndButton[] title = new BottomSheetItemTitleWithDescrAndButton[1];
		title[0] = (BottomSheetItemTitleWithDescrAndButton) new BottomSheetItemTitleWithDescrAndButton.Builder()
				.setButtonIcons(null, getIconForButton(getNextType(sortByDist)))
				.setButtonTitle(getTextForButton(getNextType(sortByDist)))
				.setOnButtonClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						sortByDist = getNextType(sortByDist);
						sortFavourites();
						int next = getNextType(sortByDist);
						title[0].setButtonIcons(null, getIconForButton(next));
						title[0].setButtonText(getTextForButton(next));
						title[0].setDescription(getTextForButton(sortByDist));
					}
				})
				.setDescription(getTextForButton(sortByDist))
				.setTitle(getString(R.string.favourites))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_descr_and_button)
				.create();
		items.add(title[0]);

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
	}

	private void loadFavorites() {
		favouritePoints.clear();
		favouritePoints.addAll(getMyApplication().getFavorites().getVisibleFavouritePoints());
		if (favouritePoints.isEmpty()) {
			favouritePoints.addAll(getMyApplication().getFavorites().getFavouritePoints());
		}
	}

	private Drawable getIconForButton(int type) {
		return getIcon(type == SORT_TYPE_DIST ? R.drawable.ic_action_list_sort : R.drawable.ic_action_sort_by_name,
				nightMode ? R.color.multi_selection_menu_close_btn_dark : R.color.multi_selection_menu_close_btn_light);
	}

	private String getTextForButton(int sortByDist) {
		int r = R.string.sort_by_distance;
		if(sortByDist == SORT_TYPE_CATEGORY) {
			r = R.string.sort_by_category;
		} else if(sortByDist == SORT_TYPE_NAME) {
			r = R.string.sort_by_name;
		}
		return getString(r);
	}

	private void selectFavorite(FavouritePoint point) {
		OsmandApplication app = getMyApplication();
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		FavouritesDbHelper favorites = app.getFavorites();
		LatLon ll = new LatLon(point.getLatitude(), point.getLongitude());
		switch (pointType) {
			case START:
				targetPointsHelper.setStartPoint(ll, true, point.getPointDescription(app));
				break;
			case TARGET:
				if (getActivity() != null) {
					targetPointsHelper.navigateToPoint(ll, true, -1, point.getPointDescription(app));
					OsmAndLocationProvider.requestFineLocationPermissionIfNeeded(getActivity());
				}
				break;
			case INTERMEDIATE:
				targetPointsHelper.navigateToPoint(ll, true, targetPointsHelper.getIntermediatePoints().size(), point.getPointDescription(app));
				break;
			case HOME:
				favorites.setSpecialPoint(ll, FavouritePoint.SpecialPointType.HOME, null);
				break;
			case WORK:
				favorites.setSpecialPoint(ll, FavouritePoint.SpecialPointType.WORK, null);
				break;
		}
		MapRouteInfoMenu routeMenu = getMapRouteInfoMenu();
		if (routeMenu != null) {
			setupMapRouteInfoMenuSpinners(routeMenu);
			updateMapRouteInfoMenuFromIcon(routeMenu);
		}
		Fragment fragment = getTargetFragment();
		if (fragment != null) {
			fragment.onActivityResult(getTargetRequestCode(), 0, null);
		}
		dismiss();
	}

	private void setupMapRouteInfoMenuSpinners(MapRouteInfoMenu routeMenu) {
		if (routeMenu != null) {
			routeMenu.setupFields(pointType);
		}
	}

	private void updateMapRouteInfoMenuFromIcon(MapRouteInfoMenu routeMenu) {
		if (pointType == PointType.START) {
			routeMenu.updateFromIcon();
		}
	}

	private MapRouteInfoMenu getMapRouteInfoMenu() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			MapActivity map = ((MapActivity) activity);
			return map.getMapRouteInfoMenu();
		} else {
			return null;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		startLocationUpdate();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopLocationUpdate();
		setupMapRouteInfoMenuSpinners(getMapRouteInfoMenu());
		if (favoritesListener != null) {
			getMyApplication().getFavorites().removeListener(favoritesListener);
			favoritesListener = null;
		}
	}

	@Override
	public void updateLocation(Location location) {
		if (!MapUtils.areLatLonEqual(this.location, location)) {
			this.location = location;
			updateLocationUi();
		}
	}

	@Override
	public void updateCompassValue(float value) {
		if (Math.abs(MapUtils.degreesDiff(lastHeading, value)) > 5) {
			lastHeading = value;
			updateLocationUi();
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
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(IS_SORTED, isSorted);
		outState.putInt(SORTED_BY_TYPE, sortByDist);
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	private void sortFavourites() {
		final Collator inst = Collator.getInstance();
		Location stale = getMyApplication().getLocationProvider().getLastStaleKnownLocation();
		final LatLon latLon = stale != null ? new LatLon(stale.getLatitude(), stale.getLongitude()) : 
			getMyApplication().getMapViewTrackingUtilities().getMapLocation();

		Collections.sort(favouritePoints, new Comparator<FavouritePoint>() {
			@Override
			public int compare(FavouritePoint lhs, FavouritePoint rhs) {
				if (sortByDist == SORT_TYPE_DIST && latLon != null) {
					double ld = MapUtils.getDistance(latLon, lhs.getLatitude(),
							lhs.getLongitude());
					double rd = MapUtils.getDistance(latLon, rhs.getLatitude(),
							rhs.getLongitude());
					return Double.compare(ld, rd);
				}

				if(sortByDist == SORT_TYPE_CATEGORY) {
					int cat = inst.compare(lhs.getCategoryDisplayName(getMyApplication()), rhs.getCategoryDisplayName(getMyApplication()));
					if(cat != 0) {
						return cat;
					}
				}
				int name = inst.compare(lhs.getDisplayName(getMyApplication()), rhs.getDisplayName(getMyApplication()));
				return name;
			}
		});

		isSorted = true;
		adapter.notifyDataSetChanged();
		recyclerView.getLayoutManager().scrollToPosition(0);
	}

	@Override
	protected int getCustomHeight() {
		return AndroidUtils.dpToPx(getContext(), 300);
	}
}
