package net.osmand.plus.dashboard;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dashboard.tools.DashFragmentData.DefaultShouldShow;
import net.osmand.plus.routepreparationmenu.ChooseRouteFragment;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.AndroidUtils;

/**
 */
public class DashNavigationFragment extends DashBaseFragment {
	public static final String TAG = "DASH_NAVIGATION_FRAGMENT";
	private static final int TITLE_ID = R.string.current_route;
	public static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};

	@Override
	public View initView(@Nullable ViewGroup container, @Nullable Bundle savedState) {
		View view = inflate(R.layout.dash_common_fragment, container, false);
		((TextView) view.findViewById(R.id.fav_text)).setText(TITLE_ID);
		((TextView) view.findViewById(R.id.show_all)).setText(R.string.info_button);

		(view.findViewById(R.id.show_all)).setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				closeDashboard();
				ChooseRouteFragment.showInstance(activity.getSupportFragmentManager());
			}
		});
		return view;
	}

	@Override
	public void onOpenDash() {
		setupNavigation();
	}

	public void setupNavigation() {
		View mainView = getView();
		if (mainView == null) return;

		RoutingHelper routingHelper = app.getRoutingHelper();
		boolean isMap = getActivity() instanceof MapActivity;
		if (!routingHelper.isRouteCalculated() || !isMap) {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.GONE);
			return;
		} else {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.VISIBLE);
		}

		MapActivity mapActivity = requireMapActivity();
		LinearLayout llFavorites = mainView.findViewById(R.id.items);
		llFavorites.removeAllViews();

		View view = inflate(R.layout.dash_navigation);
		TextView name = view.findViewById(R.id.name);
		ImageView icon = view.findViewById(R.id.icon);
		ImageView cancel = view.findViewById(R.id.cancel);
		ImageView play = view.findViewById(R.id.play);

		name.setText(routingHelper.getGeneralRouteInformation());
		icon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_start_navigation, R.color.color_myloc_distance));
		cancel.setImageDrawable(uiUtilities.getThemedIcon(R.drawable.ic_action_remove_dark));
		cancel.setOnClickListener(v -> mapActivity.getMapActions().stopNavigationActionConfirm(dialog -> {
			setupNavigation();
			DashWaypointsFragment f = dashboard.getFragmentByClass(DashWaypointsFragment.class);
			if (f != null) {
				f.onOpenDash();
			}
		}));

		int contentDescId = routingHelper.isFollowingMode() ? R.string.cancel_navigation : R.string.cancel_route;
		cancel.setContentDescription(getString(contentDescId));
		updatePlayButton(routingHelper, mapActivity, play);
		llFavorites.addView(view);
	}

	private void updatePlayButton(RoutingHelper routingHelper, MapActivity map, ImageView playBtn) {
		boolean continueNavigation = routingHelper.isRoutePlanningMode();

		int iconId = continueNavigation ? R.drawable.ic_play_dark : R.drawable.ic_pause;
		playBtn.setImageDrawable(uiUtilities.getThemedIcon(iconId));

		int contentDescId = continueNavigation ? R.string.continue_navigation : R.string.pause_navigation;
		playBtn.setContentDescription(getString(contentDescId));

		playBtn.setOnClickListener(v -> {
			if (routingHelper.isRoutePlanningMode()) {
				routingHelper.resumeNavigation();
			} else {
				routingHelper.pauseNavigation();
			}
			updatePlayButton(routingHelper, map, playBtn);
			AndroidUtils.requestNotificationPermissionIfNeeded(map);
			map.getMapViewTrackingUtilities().switchRoutePlanningMode();
			map.refreshMap();
		});
	}
}
