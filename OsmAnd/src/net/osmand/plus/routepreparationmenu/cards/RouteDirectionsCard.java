package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;

import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.RouteDetailsFragment;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.views.TurnPathHelper;
import net.osmand.plus.views.mapwidgets.LanesDrawable;
import net.osmand.util.Algorithms;

import java.util.List;

public class RouteDirectionsCard extends MapBaseCard {

	public RouteDirectionsCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.route_directions_card;
	}

	@Override
	protected void updateContent() {
		LinearLayout root = (LinearLayout) view.findViewById(R.id.items);
		root.removeAllViews();
		createRouteDirections(root);
	}

	private void createRouteDirections(LinearLayout cardsContainer) {
		List<RouteDirectionInfo> routeDirections = app.getRoutingHelper().getRouteDirections();
		for (int i = 0; i < routeDirections.size(); i++) {
			RouteDirectionInfo routeDirectionInfo = routeDirections.get(i);
			View view = getRouteDirectionView(i, routeDirectionInfo, routeDirections);
			cardsContainer.addView(view);
		}
	}

	private static String getTimeDescription(OsmandApplication app, RouteDirectionInfo model) {
		final int timeInSeconds = model.getExpectedTime();
		return Algorithms.formatDuration(timeInSeconds, app.accessibilityEnabled());
	}

	private View getRouteDirectionView(final int directionInfoIndex, RouteDirectionInfo model, List<RouteDirectionInfo> directionsInfo) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return null;
		}
		OsmandApplication app = mapActivity.getMyApplication();
		ContextThemeWrapper context = new ContextThemeWrapper(mapActivity, nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme);
		View row = LayoutInflater.from(context).inflate(R.layout.route_info_list_item, null);

		TextView label = (TextView) row.findViewById(R.id.description);
		TextView distanceLabel = (TextView) row.findViewById(R.id.distance);
		TextView timeLabel = (TextView) row.findViewById(R.id.time);
		TextView cumulativeDistanceLabel = (TextView) row.findViewById(R.id.cumulative_distance);
		TextView cumulativeTimeLabel = (TextView) row.findViewById(R.id.cumulative_time);
		ImageView directionIcon = (ImageView) row.findViewById(R.id.direction);
		ImageView lanesIcon = (ImageView) row.findViewById(R.id.lanes);
		row.findViewById(R.id.divider).setVisibility(directionInfoIndex == directionsInfo.size() - 1 ? View.INVISIBLE : View.VISIBLE);

		TurnPathHelper.RouteDrawable drawable = new TurnPathHelper.RouteDrawable(mapActivity.getResources(), true);
		drawable.setColorFilter(new PorterDuffColorFilter(getActiveColor(), PorterDuff.Mode.SRC_ATOP));
		drawable.setRouteType(model.getTurnType());
		directionIcon.setImageDrawable(drawable);

		int[] lanes = model.getTurnType().getLanes();
		if (lanes != null){
			LanesDrawable lanesDrawable = new LanesDrawable(mapActivity,1);
			lanesDrawable.lanes = lanes;
			lanesDrawable.isTurnByTurn = true;
			lanesDrawable.isNightMode = nightMode;
			lanesDrawable.updateBounds();
			lanesIcon.setImageDrawable(lanesDrawable);
			lanesIcon.setVisibility(View.VISIBLE);
		}

		label.setText(model.getDescriptionRoutePart());
		if (model.distance > 0) {
			distanceLabel.setText(OsmAndFormatter.getFormattedDistance(model.distance, app));
			timeLabel.setText(getTimeDescription(app, model));
			row.setContentDescription(label.getText() + " " + timeLabel.getText());
		} else {
			if (Algorithms.isEmpty(label.getText().toString())) {
				label.setText(mapActivity.getString((directionInfoIndex != directionsInfo.size() - 1) ? R.string.arrived_at_intermediate_point : R.string.arrived_at_destination));
			}
			distanceLabel.setText("");
			timeLabel.setText("");
			row.setContentDescription("");
		}
		RouteDetailsFragment.CumulativeInfo cumulativeInfo = RouteDetailsFragment.getRouteDirectionCumulativeInfo(directionInfoIndex, directionsInfo);
		cumulativeDistanceLabel.setText(OsmAndFormatter.getFormattedDistance(cumulativeInfo.distance, app));
		cumulativeTimeLabel.setText(Algorithms.formatDuration(cumulativeInfo.time, app.accessibilityEnabled()));
		row.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardButtonPressed(RouteDirectionsCard.this, directionInfoIndex);
				}
			}
		});
		return row;
	}
}