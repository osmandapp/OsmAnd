package net.osmand.plus.routepreparationmenu.cards;

import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.ShowRouteInfoDialogFragment;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.directionInfo;

public class SimpleRouteCard extends BaseCard {

	private MapActivity mapActivity;
	private GPXFile gpx;

	public SimpleRouteCard(MapActivity mapActivity, GPXFile gpx) {
		super(mapActivity);
		this.mapActivity = mapActivity;
		this.gpx = gpx;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.route_info_statistic;
	}

	@Override
	public void update() {
		if (view != null) {
			RoutingHelper routingHelper = mapActivity.getRoutingHelper();

			view.setBackgroundColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.route_info_bg_dark : R.color.route_info_bg_light));

			view.findViewById(R.id.dividerToDropDown).setVisibility(View.VISIBLE);
			view.findViewById(R.id.route_info_details_card).setVisibility(View.VISIBLE);

			View info = view.findViewById(R.id.info_container);
			info.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ShowRouteInfoDialogFragment.showDialog(mapActivity.getSupportFragmentManager());
				}
			});

			ImageView infoIcon = (ImageView) view.findViewById(R.id.InfoIcon);
			ImageView durationIcon = (ImageView) view.findViewById(R.id.DurationIcon);
			View infoDistanceView = view.findViewById(R.id.InfoDistance);
			View infoDurationView = view.findViewById(R.id.InfoDuration);
			if (directionInfo >= 0) {
				infoIcon.setVisibility(View.GONE);
				durationIcon.setVisibility(View.GONE);
				infoDistanceView.setVisibility(View.GONE);
				infoDurationView.setVisibility(View.GONE);
			} else {
				infoIcon.setImageDrawable(getColoredIcon(R.drawable.ic_action_route_distance, R.color.route_info_unchecked_mode_icon_color));
				infoIcon.setVisibility(View.VISIBLE);
				durationIcon.setImageDrawable(getColoredIcon(R.drawable.ic_action_time_span, R.color.route_info_unchecked_mode_icon_color));
				durationIcon.setVisibility(View.VISIBLE);
				infoDistanceView.setVisibility(View.VISIBLE);
				infoDurationView.setVisibility(View.VISIBLE);
			}
			if (directionInfo >= 0 && routingHelper.getRouteDirections() != null
					&& directionInfo < routingHelper.getRouteDirections().size()) {
				RouteDirectionInfo ri = routingHelper.getRouteDirections().get(directionInfo);
			} else {
				TextView distanceText = (TextView) view.findViewById(R.id.DistanceText);
				TextView distanceTitle = (TextView) view.findViewById(R.id.DistanceTitle);
				TextView durationText = (TextView) view.findViewById(R.id.DurationText);
				TextView durationTitle = (TextView) view.findViewById(R.id.DurationTitle);

				distanceText.setText(OsmAndFormatter.getFormattedDistance(app.getRoutingHelper().getLeftDistance(), app));

				durationText.setText(OsmAndFormatter.getFormattedDuration(app.getRoutingHelper().getLeftTime(), app));
				durationTitle.setText(app.getString(R.string.arrive_at_time, OsmAndFormatter.getFormattedTime(app.getRoutingHelper().getLeftTime(), true)));

				AndroidUtils.setTextPrimaryColor(app, distanceText, nightMode);
				AndroidUtils.setTextSecondaryColor(app, distanceTitle, nightMode);
				AndroidUtils.setTextPrimaryColor(app, durationText, nightMode);
				AndroidUtils.setTextSecondaryColor(app, durationTitle, nightMode);
			}
			view.findViewById(R.id.details_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ShowRouteInfoDialogFragment.showDialog(mapActivity.getSupportFragmentManager());
				}
			});

			buildHeader(view);
			applyDayNightMode();
		}
	}

	protected void applyDayNightMode() {
		FrameLayout detailsButton = view.findViewById(R.id.details_button);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, detailsButton, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
			AndroidUtils.setBackground(app, view.findViewById(R.id.details_button_descr), nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setBackground(app, view.findViewById(R.id.details_button_descr), nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
		}
		int color = ContextCompat.getColor(mapActivity, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);
		AndroidUtils.setBackground(app, view.findViewById(R.id.dividerToDropDown), nightMode, R.color.divider_light, R.color.divider_dark);
		AndroidUtils.setBackground(app, view.findViewById(R.id.info_divider), nightMode, R.color.activity_background_light, R.color.route_info_cancel_button_color_dark);
		AndroidUtils.setBackground(app, view.findViewById(R.id.route_info_details_card), nightMode, R.color.activity_background_light, R.color.route_info_cancel_button_color_dark);
		AndroidUtils.setBackground(app, view.findViewById(R.id.RouteInfoControls), nightMode, R.color.route_info_bg_light, R.color.route_info_bg_dark);

		((TextView) view.findViewById(R.id.details_button_descr)).setTextColor(color);
	}

	private void buildHeader(View headerView) {
		final LineChart mChart = (LineChart) headerView.findViewById(R.id.chart);
		final GPXUtilities.GPXTrackAnalysis analysis = gpx.getAnalysis(0);

		GpxUiHelper.setupGPXChart(mChart, 4, 4f, 4f, !nightMode, false);
		GpxUiHelper.OrderedLineDataSet elevationDataSet;
		GpxUiHelper.OrderedLineDataSet slopeDataSet;
		if (analysis.hasElevationData) {
			List<ILineDataSet> dataSets = new ArrayList<>();
			elevationDataSet = GpxUiHelper.createGPXElevationDataSet(app, mChart, analysis,
					GpxUiHelper.GPXDataSetAxisType.DISTANCE, false, true);
			if (elevationDataSet != null) {
				dataSets.add(elevationDataSet);
			}
			slopeDataSet = GpxUiHelper.createGPXSlopeDataSet(app, mChart, analysis,
					GpxUiHelper.GPXDataSetAxisType.DISTANCE, elevationDataSet.getValues(), true, true);
			if (slopeDataSet != null) {
				dataSets.add(slopeDataSet);
			}
			mChart.setData(new LineData(dataSets));
			mChart.setVisibility(View.VISIBLE);
		} else {
			mChart.setVisibility(View.GONE);
		}
	}
}