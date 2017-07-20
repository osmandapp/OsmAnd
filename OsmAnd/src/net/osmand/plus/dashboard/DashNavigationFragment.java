package net.osmand.plus.dashboard;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.ShowRouteInfoDialogFragment;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.routing.RoutingHelper;

/**
 */
public class DashNavigationFragment extends DashBaseFragment {
	public static final String TAG = "DASH_NAVIGATION_FRAGMENT";
	private static final int TITLE_ID = R.string.current_route;
	public static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashboardOnMap.DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};

	@Override
	public View initView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		((TextView) view.findViewById(R.id.fav_text)).setText(TITLE_ID);
		((TextView)view.findViewById(R.id.show_all)).setText(R.string.info_button);
		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				ShowRouteInfoDialogFragment.showDialog(getActivity().getSupportFragmentManager());
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
		final RoutingHelper routingHelper = getMyApplication().getRoutingHelper();
		getActivity();
		if (!routingHelper.isRouteCalculated() || 
				(!(getActivity() instanceof MapActivity))) {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.GONE);
			return;
		} else {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.VISIBLE);
		}
		final MapActivity map =  (MapActivity) getActivity();
		LinearLayout favorites = (LinearLayout) mainView.findViewById(R.id.items);
		favorites.removeAllViews();
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dash_navigation, null, false);			
		TextView name = (TextView) view.findViewById(R.id.name);
		ImageView icon = (ImageView) view.findViewById(R.id.icon);
		ImageView cancel = (ImageView) view.findViewById(R.id.cancel);
		ImageView play = (ImageView) view.findViewById(R.id.play);
		name.setText(routingHelper.getGeneralRouteInformation());
		icon.setImageDrawable(getMyApplication().getIconsCache().getIcon(R.drawable.ic_action_start_navigation, 
				R.color.color_myloc_distance));
		cancel.setImageDrawable(getMyApplication().getIconsCache().getThemedIcon(R.drawable.ic_action_remove_dark)
				);
		cancel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AlertDialog dlg = map.getMapActions().stopNavigationActionConfirm();
				dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
					
					@Override
					public void onDismiss(DialogInterface dialog) {
						setupNavigation();
						DashWaypointsFragment f = dashboard.getFragmentByClass(DashWaypointsFragment.class);
						if(f != null) {
							f.onOpenDash();
						}
					}
				});
			}
		});
		int nav;
		if(routingHelper.isFollowingMode()) {
			nav = R.string.cancel_navigation;
		} else {
			nav = R.string.cancel_route;
		} 
		cancel.setContentDescription(getString(nav));
		updatePlayButton(routingHelper, map, play);
		favorites.addView(view);
	}

	private void updatePlayButton(final RoutingHelper routingHelper, final MapActivity map, final ImageView play) {
		boolean toContinueNavigation = routingHelper.isRoutePlanningMode();
		play.setImageDrawable(getMyApplication().getIconsCache().getThemedIcon(
						toContinueNavigation ? R.drawable.ic_play_dark : R.drawable.ic_pause)
				);
		play.setContentDescription(getString(toContinueNavigation ? R.string.continue_navigation :
			R.string.pause_navigation));
		play.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(routingHelper.isRoutePlanningMode()) {
					routingHelper.setRoutePlanningMode(false);
					routingHelper.setFollowingMode(true);
				} else {
					routingHelper.setRoutePlanningMode(true);
					routingHelper.setFollowingMode(false);
					routingHelper.setPauseNavigation(true);
				}
				updatePlayButton(routingHelper, map, play);
				map.getMapViewTrackingUtilities().switchToRoutePlanningMode();
				map.refreshMap();
			}
		});
	}

}
