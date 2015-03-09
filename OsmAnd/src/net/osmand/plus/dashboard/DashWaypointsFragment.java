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
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Denis on 24.11.2014.
 */
public class DashWaypointsFragment extends DashLocationFragment {
	public static final String TAG = "DASH_WAYPOINTS_FRAGMENT";
	List<TargetPoint> points = new ArrayList<TargetPoint>();
	private boolean showAll;

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		((TextView) view.findViewById(R.id.fav_text)).setTypeface(typeface);
		((Button) view.findViewById(R.id.show_all)).setTypeface(typeface);
		((TextView) view.findViewById(R.id.fav_text)).setText(getString(R.string.waypoints));

		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showAll = !showAll;
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
		points = showAll ? Collections.singletonList(helper.getPointToNavigate()) :
			helper.getIntermediatePointsWithTarget();
		((Button) mainView.findViewById(R.id.show_all)).setText(showAll? getString(R.string.shared_string_collapse) : 
			getString(R.string.shared_string_show_all));
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

			int id = helper.getPointToNavigate() == point ? R.drawable.list_destination : R.drawable.list_intermediate;
			((ImageView) view.findViewById(R.id.favourite_icon)).setImageDrawable(getMyApplication().getIconsCache()
					.getContentIcon(id));
			DashLocationView dv = new DashLocationView(direction, label, new LatLon(point.getLatitude(),
					point.getLongitude()));
			distances.add(dv);

			name.setText(PointDescription.getSimpleName(point, getActivity()));
			view.findViewById(R.id.navigate_to).setVisibility(View.VISIBLE);
			view.findViewById(R.id.navigate_to).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					selectModel(point, view);
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
		PointDescription name = model.getOriginalPointDescription();
		boolean light = ((OsmandApplication) getActivity().getApplication()).getSettings().isLightContent();
		final PopupMenu optionsMenu = new PopupMenu(getActivity(), v);
		OsmandSettings settings = ((OsmandApplication) getActivity().getApplication()).getSettings();
		DirectionsDialogs.createDirectionsActionsPopUpMenu(optionsMenu, new LatLon(model.getLatitude(), model.getLongitude()),
				model, name, settings.getLastKnownMapZoom(), getActivity(), true);
		MenuItem item = optionsMenu.getMenu().add(
				R.string.shared_string_delete).setIcon(light ?
				R.drawable.ic_action_delete_light : R.drawable.ic_action_delete_dark);
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				getMyApplication().getTargetPointsHelper().removeWayPoint(true, model.index);
				setupTargets();
				return true;
			}
		});
		optionsMenu.show();
	}


}
