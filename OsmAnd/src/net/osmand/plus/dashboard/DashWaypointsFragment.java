package net.osmand.plus.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Denis on 24.11.2014.
 */
public class DashWaypointsFragment extends DashLocationFragment {
	public static final String TAG = "DASH_WAYPOINTS_FRAGMENT";
	List<TargetPoint> points = new ArrayList<TargetPoint>();
	private static boolean SHOW_ALL;

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		((TextView) view.findViewById(R.id.fav_text)).setText(getString(R.string.waypoints));
		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				SHOW_ALL = !SHOW_ALL;
				setupTargets();
			}
		});
		return view;
	}

	@Override
	public void onOpenDash() {
		setupTargets();
	}

	public void setupTargets() {
		View mainView = getView();
		final TargetPointsHelper helper = getMyApplication().getTargetPointsHelper();
		if (helper.getPointToNavigate() == null) {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.GONE);
			return;
		} else {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.VISIBLE);
		}
		points = SHOW_ALL ? helper.getIntermediatePointsWithTarget() :
			Collections.singletonList(helper.getPointToNavigate());
		((Button) mainView.findViewById(R.id.show_all)).setText(SHOW_ALL? getString(R.string.shared_string_collapse) : 
			getString(R.string.shared_string_show_all));
		((Button) mainView.findViewById(R.id.show_all)).setVisibility(
				helper.getIntermediatePoints().size() == 0 ? View.INVISIBLE : View.VISIBLE);
		((TextView) mainView.findViewById(R.id.fav_text)).setText(getString(R.string.waypoints) + " (" + 
				helper.getIntermediatePointsWithTarget().size()+")");
		LinearLayout favorites = (LinearLayout) mainView.findViewById(R.id.items);
		favorites.removeAllViews();
		List<DashLocationView> distances = new ArrayList<DashLocationFragment.DashLocationView>();
		for (final TargetPoint point : points) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.favorites_list_item, null, false);
			TextView name = (TextView) view.findViewById(R.id.favourite_label);
			TextView label = (TextView) view.findViewById(R.id.distance);
			ImageView direction = (ImageView) view.findViewById(R.id.direction);
			direction.setVisibility(View.VISIBLE);
			label.setVisibility(View.VISIBLE);
			view.findViewById(R.id.divider).setVisibility(View.VISIBLE);
			view.findViewById(R.id.group_image).setVisibility(View.GONE);

			boolean target = helper.getPointToNavigate() == point;
			int id = target ? R.drawable.list_destination : R.drawable.list_intermediate;
			((ImageView) view.findViewById(R.id.favourite_icon)).setImageDrawable(getMyApplication().getIconsCache()
					.getContentIcon(id));
			DashLocationView dv = new DashLocationView(direction, label, new LatLon(point.getLatitude(),
					point.getLongitude()));
			distances.add(dv);

			name.setText(PointDescription.getSimpleName(point, getActivity()));
			ImageButton options =  ((ImageButton)view.findViewById(R.id.options));
			options.setVisibility(View.VISIBLE);
			options.setImageDrawable(getMyApplication().getIconsCache().
					getContentIcon(R.drawable.ic_overflow_menu_white));
			options.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					selectModel(point, view);
				}
			});
			
			ImageButton navigate =  ((ImageButton)view.findViewById(R.id.navigate_to));
			navigate.setImageDrawable(getMyApplication().getIconsCache().
					getContentIcon(R.drawable.ic_action_gdirections_dark));
			navigate.setVisibility(target? View.VISIBLE : View.GONE);
			navigate.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					dashboard.navigationAction();
				}
			});
			
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					getMyApplication().getSettings().setMapLocationToShow(point.getLatitude(), point.getLongitude(),
							15, point.getPointDescription(getActivity()), false,
							point); //$NON-NLS-1$
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			});
			favorites.addView(view);
		}
		this.distances = distances;
	}
	private void selectModel(final TargetPoint model, View v) {
		boolean light = ((OsmandApplication) getActivity().getApplication()).getSettings().isLightContent();
		final PopupMenu optionsMenu = new PopupMenu(getActivity(), v);
		DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
		MenuItem 
		item = optionsMenu.getMenu().add(
				R.string.shared_string_add_to_favorites).setIcon(getMyApplication().getIconsCache().
						getContentIcon(R.drawable.ic_action_fav_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Bundle args = new Bundle();
				Dialog dlg = FavoriteDialogs.createAddFavouriteDialog(getActivity(), args);
				dlg.show();
				FavoriteDialogs.prepareAddFavouriteDialog(getActivity(), dlg, args, model.getLatitude(), model.getLongitude(),
						model.getOriginalPointDescription());
				return true;
			}
		});
		final boolean target = model == getMyApplication().getTargetPointsHelper().getPointToNavigate();
		if(SHOW_ALL && getMyApplication().getTargetPointsHelper().getIntermediatePoints().size() > 0) {
			final List<TargetPoint> allTargets = getMyApplication().getTargetPointsHelper().getIntermediatePointsWithTarget();
			if (model.index > 0 || target) {
				final int ind = target ? allTargets.size() - 1 : model.index;
				item = optionsMenu.getMenu().add(R.string.waypoint_visit_before)
						.setIcon(getMyApplication().getIconsCache().
								getContentIcon(R.drawable.ic_action_up_dark));
				item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						TargetPoint remove = allTargets.remove(ind - 1);
						allTargets.add(ind, remove);
						getMyApplication().getTargetPointsHelper().reorderAllTargetPoints(allTargets, true);
						setupTargets();
						return true;
					}
				});
			}
			if (!target) {
				item = optionsMenu.getMenu().add(R.string.waypoint_visit_after)
						.setIcon(getMyApplication().getIconsCache().
								getContentIcon(R.drawable.ic_action_down_dark));
				item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						TargetPoint remove = allTargets.remove(model.index + 1);
						allTargets.add(model.index, remove);
						getMyApplication().getTargetPointsHelper().reorderAllTargetPoints(allTargets, true);
						setupTargets();
						return true;
					}
				});
			}
		}
		item = optionsMenu.getMenu().add(
				R.string.shared_string_delete).setIcon(getMyApplication().getIconsCache().
						getContentIcon(R.drawable.ic_action_delete_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				getMyApplication().getTargetPointsHelper().removeWayPoint(true, target ? -1 :  model.index);
				setupTargets();
				return true;
			}
		});
		optionsMenu.show();
	}


}
