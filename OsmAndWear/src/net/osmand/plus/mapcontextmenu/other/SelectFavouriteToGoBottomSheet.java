package net.osmand.plus.mapcontextmenu.other;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.SpecialPointType;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.PointType;

import static net.osmand.plus.routepreparationmenu.AddPointBottomSheetDialog.ADD_FAVORITE_TO_ROUTE_REQUEST_CODE;

public class SelectFavouriteToGoBottomSheet extends SelectFavouriteBottomSheet {

	public static final String POINT_TYPE_KEY = "point_type";

	protected PointType mPointType;

	@Override
	public void createMenuItems(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
		super.createMenuItems(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			mPointType = PointType.valueOf(args.getString(POINT_TYPE_KEY));
		}
	}

	@Override
	protected void onFavouriteSelected(@NonNull FavouritePoint point) {
		OsmandApplication app = getMyApplication();
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		FavouritesHelper favorites = app.getFavoritesHelper();
		LatLon ll = new LatLon(point.getLatitude(), point.getLongitude());
		switch (mPointType) {
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
				favorites.setSpecialPoint(ll, SpecialPointType.HOME, null);
				break;
			case WORK:
				favorites.setSpecialPoint(ll, SpecialPointType.WORK, null);
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
			routeMenu.setupFields(mPointType);
		}
	}

	private void updateMapRouteInfoMenuFromIcon(MapRouteInfoMenu routeMenu) {
		if (mPointType == PointType.START) {
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
	public void onPause() {
		super.onPause();
		setupMapRouteInfoMenuSpinners(getMapRouteInfoMenu());
	}

	public static void showInstance(@NonNull MapActivity activity, @NonNull Fragment target, @NonNull PointType pointType) {
		SelectFavouriteToGoBottomSheet fragment = new SelectFavouriteToGoBottomSheet();
		Bundle args = new Bundle();
		args.putString(POINT_TYPE_KEY, pointType.name());
		fragment.setArguments(args);
		fragment.setTargetFragment(target, ADD_FAVORITE_TO_ROUTE_REQUEST_CODE);
		showFragment(activity, fragment);
	}

}
