package net.osmand.plus.mapcontextmenu.other;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import net.osmand.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;

public class DestinationReachedMenuFragment extends Fragment {
	public static final String TAG = "DestinationReachedMenuFragment";
	private static boolean exists = false;
	private DestinationReachedMenu menu;


	public DestinationReachedMenuFragment() {
		exists = true;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MapActivity mapActivity = getMapActivity();
		if (menu == null && mapActivity != null) {
			menu = new DestinationReachedMenu(mapActivity);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || menu == null) {
			return null;
		}
		ContextThemeWrapper ctx = new ContextThemeWrapper(mapActivity, menu.isLight() ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
		LayoutInflater inf = LayoutInflater.from(ctx);
		View view = inf.inflate(R.layout.dest_reached_menu_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(ctx, view);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finishNavigation();
			}
		});

		UiUtilities iconsCache = mapActivity.getMyApplication().getUIUtilities();

		ImageButton closeImageButton = (ImageButton) view.findViewById(R.id.closeImageButton);
		closeImageButton.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_remove_dark, menu.isLight()));
		closeImageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finishNavigation();
			}
		});

		Button removeDestButton = (Button) view.findViewById(R.id.removeDestButton);
		removeDestButton.setCompoundDrawablesWithIntrinsicBounds(
				iconsCache.getIcon(R.drawable.ic_action_done, menu.isLight()), null, null, null);
		removeDestButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finishNavigation();
			}
		});

		Button recalcDestButton = (Button) view.findViewById(R.id.recalcDestButton);
		recalcDestButton.setCompoundDrawablesWithIntrinsicBounds(
				iconsCache.getIcon(R.drawable.ic_action_gdirections_dark, menu.isLight()), null, null, null);
		recalcDestButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					TargetPointsHelper helper = mapActivity.getMyApplication().getTargetPointsHelper();
					TargetPoint target = helper.getPointToNavigate();

					dismissMenu();

					if (target != null) {
						helper.navigateToPoint(new LatLon(target.getLatitude(), target.getLongitude()),
								true, -1, target.getOriginalPointDescription());
						mapActivity.getMapActions().recalculateRoute(false);
						mapActivity.getMapLayers().getMapControlsLayer().startNavigation();
					}
				}
			}
		});

		Button findParkingButton = (Button) view.findViewById(R.id.findParkingButton);

		ApplicationMode appMode = mapActivity.getMyApplication().getRoutingHelper().getAppMode();

		if (!appMode.isDerivedRoutingFrom(ApplicationMode.CAR)) {
			findParkingButton.setVisibility(View.GONE);
		}

		findParkingButton.setCompoundDrawablesWithIntrinsicBounds(
				iconsCache.getIcon(R.drawable.ic_action_parking_dark, menu.isLight()), null, null, null);
		findParkingButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					PoiFiltersHelper helper = mapActivity.getMyApplication().getPoiFilters();
					PoiUIFilter parkingFilter = helper.getFilterById(PoiUIFilter.STD_PREFIX + "parking");
					mapActivity.showQuickSearch(parkingFilter);
				}
				dismissMenu();
			}
		});

		View mainView = view.findViewById(R.id.main_view);
		if (menu.isLandscapeLayout()) {
			AndroidUtils.setBackground(view.getContext(), mainView, !menu.isLight(),
					R.drawable.bg_left_menu_light, R.drawable.bg_left_menu_dark);
		} else {
			AndroidUtils.setBackground(view.getContext(), mainView, !menu.isLight(),
					R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getContextMenu().setBaseFragmentVisibility(false);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getContextMenu().setBaseFragmentVisibility(true);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		exists = false;
	}

	private void finishNavigation() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMyApplication().getTargetPointsHelper().removeWayPoint(true, -1);
			Object contextMenuObj = mapActivity.getContextMenu().getObject();
			if (mapActivity.getContextMenu().isActive()
					&& contextMenuObj instanceof TargetPoint) {
				TargetPoint targetPoint = (TargetPoint) contextMenuObj;
				if (!targetPoint.start && !targetPoint.intermediate) {
					mapActivity.getContextMenu().close();
				}
			}
			OsmandSettings settings = mapActivity.getMyApplication().getSettings();
			settings.APPLICATION_MODE.set(settings.DEFAULT_APPLICATION_MODE.get());
			mapActivity.getMapActions().stopNavigationWithoutConfirm();
			dismissMenu();
		}
	}

	public static boolean isExists() {
		return exists;
	}

	public static void showInstance(DestinationReachedMenu menu) {
		int slideInAnim = menu.getSlideInAnimation();
		int slideOutAnim = menu.getSlideOutAnimation();

		DestinationReachedMenuFragment fragment = new DestinationReachedMenuFragment();
		fragment.menu = menu;
		MapActivity mapActivity = menu.getMapActivity();
		if (mapActivity != null) {
			mapActivity.getSupportFragmentManager().beginTransaction()
					.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG).commitAllowingStateLoss();
		}
	}

	public void dismissMenu() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && !mapActivity.isActivityDestroyed()) {
			mapActivity.getSupportFragmentManager().popBackStack();
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}
}
