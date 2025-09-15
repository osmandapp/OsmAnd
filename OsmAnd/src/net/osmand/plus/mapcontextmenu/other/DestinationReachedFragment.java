package net.osmand.plus.mapcontextmenu.other;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.FRAGMENT_DESTINATION_REACHED_ID;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.routing.RouteCalculationProgressListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.utils.AndroidUtils;

/**
 * Fragment that appears when the user reaches the destination.
 *
 * Although visually similar to a BottomSheet, this screen is implemented
 * as a standard full-screen Fragment due to legacy design and internal logic.
 *
 * ⚠️ Do not convert to BottomSheetFragment without carefully evaluating its integration,
 * as it may rely on full-screen behavior and custom navigation handling.
 */
public class DestinationReachedFragment extends BaseOsmAndFragment implements RouteCalculationProgressListener {

	public static final String TAG = DestinationReachedFragment.class.getSimpleName();

	private static final String SHOULD_HIDE_MENU = "should_hide_menu";

	private static boolean shown;

	private MapActivity mapActivity;
	private MapContextMenu ctxMenu;
	private boolean isLandscapeLayout;
	private boolean shouldHideMenu;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mapActivity = (MapActivity) requireActivity();
		ctxMenu = mapActivity.getContextMenu();
		isLandscapeLayout = !AndroidUiHelper.isOrientationPortrait(mapActivity);
		app.getRoutingHelper().addCalculationProgressListener(this);
		if (savedInstanceState != null) {
			shouldHideMenu = savedInstanceState.getBoolean(SHOULD_HIDE_MENU);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.dest_reached_menu_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(mapActivity, view);
		view.setOnClickListener(v -> finishNavigation());

		ImageButton btnClose = view.findViewById(R.id.closeImageButton);
		btnClose.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_remove_dark, !nightMode));
		btnClose.setOnClickListener(v -> finishNavigation());

		Button btnRemoveDest = view.findViewById(R.id.removeDestButton);
		Drawable doneIcon = uiUtilities.getIcon(R.drawable.ic_action_done, !nightMode);
		btnRemoveDest.setCompoundDrawablesWithIntrinsicBounds(doneIcon, null, null, null);
		btnRemoveDest.setOnClickListener(v -> finishNavigation());

		Button btnRecalcDest = view.findViewById(R.id.recalcDestButton);
		Drawable directionIcon = uiUtilities.getIcon(R.drawable.ic_action_gdirections_dark, !nightMode);
		btnRecalcDest.setCompoundDrawablesWithIntrinsicBounds(directionIcon, null, null, null);
		btnRecalcDest.setOnClickListener(v -> {
			if (mapActivity != null) {
				TargetPointsHelper helper = mapActivity.getApp().getTargetPointsHelper();
				TargetPoint target = helper.getPointToNavigate();

				dismiss();

				if (target != null) {
					helper.navigateToPoint(new LatLon(target.getLatitude(), target.getLongitude()),
							true, -1, target.getOriginalPointDescription());
					mapActivity.getMapActions().recalculateRoute(false);
					mapActivity.getMapActions().startNavigation();
				}
			}
		});

		Button btnFindParking = view.findViewById(R.id.findParkingButton);

		ApplicationMode routingAppMode = mapActivity.getApp().getRoutingHelper().getAppMode();

		if (!routingAppMode.isDerivedRoutingFrom(ApplicationMode.CAR)) {
			btnFindParking.setVisibility(View.GONE);
		}

		Drawable parkingIcon = uiUtilities.getIcon(R.drawable.ic_action_parking_dark, !nightMode);
		btnFindParking.setCompoundDrawablesWithIntrinsicBounds(parkingIcon, null, null, null);
		btnFindParking.setOnClickListener(v -> {
			PoiFiltersHelper helper = mapActivity.getApp().getPoiFilters();
			PoiUIFilter parkingFilter = helper.getFilterById(PoiUIFilter.STD_PREFIX + "parking");
			mapActivity.getFragmentsHelper().showQuickSearch(parkingFilter);
			dismiss();
		});

		View mainView = view.findViewById(R.id.main_view);
		if (isLandscapeLayout) {
			AndroidUtils.setBackground(view.getContext(), mainView, nightMode,
					R.drawable.bg_left_menu_light, R.drawable.bg_left_menu_dark);
		} else {
			AndroidUtils.setBackground(view.getContext(), mainView, nightMode,
					R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		ctxMenu.setBaseFragmentVisibility(false);
	}

	@Override
	public void onStop() {
		super.onStop();
		ctxMenu.setBaseFragmentVisibility(true);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		app.getRoutingHelper().removeCalculationProgressListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (shouldHideMenu) {
			shouldHideMenu = false;
			dismiss();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SHOULD_HIDE_MENU, shouldHideMenu);
	}

	private void finishNavigation() {
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		targetPointsHelper.removeWayPoint(true, -1);
		MapContextMenu ctxMenu = mapActivity.getContextMenu();
		Object contextMenuObj = ctxMenu.getObject();
		if (ctxMenu.isActive() && contextMenuObj instanceof TargetPoint targetPoint) {
			if (!targetPoint.start && !targetPoint.intermediate) {
				ctxMenu.close();
			}
		}
		settings.setApplicationMode(settings.DEFAULT_APPLICATION_MODE.get());
		mapActivity.getMapActions().stopNavigationWithoutConfirm();
		dismiss();
	}

	public void dismiss() {
		FragmentManager fm = mapActivity.getSupportFragmentManager();
		if (!fm.isStateSaved()) {
			fm.popBackStack();
		} else {
			// Indicates that the menu could not be closed immediately.
			// In which case the menu should be closed next time when it will possible.
			shouldHideMenu = true;
		}
	}

	@Override
	public void onCalculationStart() {
		// Hide dialog if a new route available.
		dismiss();
	}

	@Override
	public void onUpdateCalculationProgress(int progress) {
	}

	@Override
	public void onRequestPrivateAccessRouting() {
	}

	@Override
	public void onCalculationFinish() {
	}

	public static boolean wasShown() {
		return shown;
	}

	public static void resetShownState() {
		shown = false;
	}

	public static void show(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getApp();
		OsmAndAppCustomization customization = app.getAppCustomization();
		if (!shown && customization.isFeatureEnabled(FRAGMENT_DESTINATION_REACHED_ID)) {
			shown = true;
			NavigationSession carNavigationSession = app.getCarNavigationSession();
			if (carNavigationSession == null || !carNavigationSession.hasStarted()) {
				showInstance(mapActivity);
			}
		}
	}

	private static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager fm = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG)) {
			int slideInAnim = R.anim.slide_in_bottom;
			int slideOutAnim = R.anim.slide_out_bottom;
			if (!AndroidUiHelper.isOrientationPortrait(activity)) {
				boolean isRtl = AndroidUtils.isLayoutRtl(activity);
				slideInAnim = isRtl ? R.anim.slide_in_right : R.anim.slide_in_left;
				slideOutAnim = isRtl ? R.anim.slide_out_right : R.anim.slide_out_left;
			}
			DestinationReachedFragment fragment = new DestinationReachedFragment();
			fm.beginTransaction()
					.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
