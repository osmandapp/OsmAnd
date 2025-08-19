package net.osmand.plus.dashboard;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.dashboard.tools.DashFragmentData.DefaultShouldShow;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.AndroidUiHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;

/**
 */
public class DashWaypointsFragment extends DashLocationFragment {

	public static final String TAG = "DASH_WAYPOINTS_FRAGMENT";
	public static final int TITLE_ID = R.string.shared_string_waypoints;
	private static boolean SHOW_ALL;
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
		((TextView) view.findViewById(R.id.fav_text)).setText(getString(TITLE_ID));
		return view;
	}

	@Override
	public void onOpenDash() {
		setupView();
	}
	
	public void setupView() {
		setupTargets();
	}

	private void setupTargets() {
		View mainView = getView();
		if (mainView == null) return;

		TargetPointsHelper helper = app.getTargetPointsHelper();
		if (helper.getPointToNavigate() == null) {
			AndroidUiHelper.updateVisibility(mainView.findViewById(R.id.main_fav), false);
			return;
		}

		AndroidUiHelper.updateVisibility(mainView.findViewById(R.id.main_fav), true);

		((Button) mainView.findViewById(R.id.show_all)).setText(SHOW_ALL
				? getString(R.string.shared_string_collapse)
				: getString(R.string.shared_string_show_all));

		mainView.findViewById(R.id.show_all).setOnClickListener(v -> {
			SHOW_ALL = !SHOW_ALL;
			setupView();
		});

		mainView.findViewById(R.id.show_all).setVisibility(
				helper.getIntermediatePoints().isEmpty() ? View.INVISIBLE : View.VISIBLE);
		((TextView) mainView.findViewById(R.id.fav_text)).setText(getString(R.string.shared_string_waypoints) + " (" +
				helper.getIntermediatePointsWithTarget().size() + ")");
		LinearLayout favorites = mainView.findViewById(R.id.items);
		favorites.removeAllViews();

		List<TargetPoint> targetPoints = SHOW_ALL
				? helper.getIntermediatePointsWithTarget()
				: Collections.singletonList(helper.getPointToNavigate());
		List<DashLocationView> distances = new ArrayList<>();

		for (TargetPoint point : targetPoints) {
			View view = inflate(R.layout.favorites_list_item);
			TextView name = view.findViewById(R.id.favourite_label);
			TextView label = view.findViewById(R.id.distance);
			ImageView direction = view.findViewById(R.id.direction);
			direction.setVisibility(View.VISIBLE);
			label.setVisibility(View.VISIBLE);
			view.findViewById(R.id.divider).setVisibility(View.VISIBLE);
			view.findViewById(R.id.group_image).setVisibility(View.GONE);

			boolean target = helper.getPointToNavigate() == point;
			int iconId = !target ? R.drawable.list_intermediate :  R.drawable.list_destination;

			((ImageView) view.findViewById(R.id.favourite_icon))
					.setImageDrawable(uiUtilities.getIcon(iconId, 0));
			DashLocationView dv = new DashLocationView(direction, label,
					new LatLon(point.getLatitude(), point.getLongitude()));
			distances.add(dv);

			name.setText(PointDescription.getSimpleName(point, getActivity()));
			ImageButton options = view.findViewById(R.id.options);
			options.setVisibility(View.VISIBLE);
			boolean optionsVisible = (SHOW_ALL && !app.getTargetPointsHelper().getIntermediatePoints().isEmpty());
			
			options.setImageDrawable(app.getUIUtilities().
					getThemedIcon(optionsVisible ? R.drawable.ic_overflow_menu_white :
							R.drawable.ic_action_remove_dark));
			options.setOnClickListener(v -> {
				if (optionsVisible) {
					selectTargetModel(point, v);
				} else {
					deletePointConfirm(point, v);
				}
			});
			
			ImageButton navigate = view.findViewById(R.id.navigate_to);
			navigate.setImageDrawable(app.getUIUtilities().
					getThemedIcon(R.drawable.ic_action_gdirections_dark));
			navigate.setVisibility(target ? View.VISIBLE : View.GONE);
			navigate.setOnClickListener(v -> dashboard.navigationAction());
			
			view.setOnClickListener(v -> {
				if (getActivity() != null) {
					settings.setMapLocationToShow(point.getLatitude(), point.getLongitude(),
							15, point.getPointDescription(getActivity()), false,
							point);
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			});
			favorites.addView(view);
		}
		this.distances = distances;
	}
	
	protected void deletePointConfirm(TargetPoint point, View view) {
		boolean target = point == app.getTargetPointsHelper().getPointToNavigate();
		AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
		// Stop the navigation
		builder.setTitle(getString(R.string.delete_target_point));
		builder.setMessage(PointDescription.getSimpleName(point, app));
		builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
			app.getTargetPointsHelper().removeWayPoint(true, target ? -1 : point.index);
			setupView();
		});
		builder.setNegativeButton(R.string.shared_string_no, null);
		builder.show();		
	}

	private void selectTargetModel(TargetPoint point, View view) {
		PopupMenu optionsMenu = new PopupMenu(requireActivity(), view);
		DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
		MenuItem item; 
		boolean target = point == app.getTargetPointsHelper().getPointToNavigate();

		if (SHOW_ALL && !app.getTargetPointsHelper().getIntermediatePoints().isEmpty()) {
			List<TargetPoint> allTargets = app.getTargetPointsHelper().getIntermediatePointsWithTarget();

			if (point.index > 0 || target) {
				int ind = target ? allTargets.size() - 1 : point.index;
				item = optionsMenu.getMenu().add(R.string.waypoint_visit_before)
						.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_up_dark));
				item.setOnMenuItemClickListener(menuItem -> {
					TargetPoint remove = allTargets.remove(ind - 1);
					allTargets.add(ind, remove);
					app.getTargetPointsHelper().reorderAllTargetPoints(allTargets, true);
					setupView();
					return true;
				});
			}

			if (!target) {
				item = optionsMenu.getMenu().add(R.string.waypoint_visit_after)
						.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_down_dark));
				item.setOnMenuItemClickListener(menuItem -> {
					TargetPoint remove = allTargets.remove(point.index + 1);
					allTargets.add(point.index, remove);
					app.getTargetPointsHelper().reorderAllTargetPoints(allTargets, true);
					setupView();
					return true;
				});
			}
		}
		item = optionsMenu.getMenu().add(R.string.shared_string_remove)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_remove_dark));
		item.setOnMenuItemClickListener(menuItem -> {
			deletePointConfirm(point, view);
			return true;
		});
		optionsMenu.show();
	}
}