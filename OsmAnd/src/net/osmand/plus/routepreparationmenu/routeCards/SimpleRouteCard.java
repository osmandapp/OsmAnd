package net.osmand.plus.routepreparationmenu.routeCards;

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
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.ShowRouteInfoDialogFragment;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.directionInfo;

public class SimpleRouteCard extends BaseRouteCard {

	private MapActivity mapActivity;
	private GPXUtilities.GPXFile gpx;
	private final RoutingHelper routingHelper;

	public SimpleRouteCard(MapActivity mapActivity, boolean nightMode, GPXUtilities.GPXFile gpx) {
		super(mapActivity.getMyApplication(), nightMode);
		this.mapActivity = mapActivity;
		this.gpx = gpx;
		routingHelper = mapActivity.getRoutingHelper();
	}

	@Override
	public void bindViewHolder() {
		view = mapActivity.getLayoutInflater().inflate(R.layout.route_info_statistic, null);
		view.setBackgroundColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.route_info_bg_dark : R.color.route_info_bg_light));

		view.findViewById(R.id.dividerToDropDown).setVisibility(View.VISIBLE);
		view.findViewById(R.id.route_info_details_card).setVisibility(View.VISIBLE);
		final OsmandApplication ctx = mapActivity.getMyApplication();

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
			infoIcon.setImageDrawable(ctx.getUIUtilities().getIcon(R.drawable.ic_action_route_distance, R.color.route_info_unchecked_mode_icon_color));
			infoIcon.setVisibility(View.VISIBLE);
			durationIcon.setImageDrawable(ctx.getUIUtilities().getIcon(R.drawable.ic_action_time_span, R.color.route_info_unchecked_mode_icon_color));
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

			distanceText.setText(OsmAndFormatter.getFormattedDistance(ctx.getRoutingHelper().getLeftDistance(), ctx));

			durationText.setText(OsmAndFormatter.getFormattedDuration(ctx.getRoutingHelper().getLeftTime(), ctx));
			durationTitle.setText(ctx.getString(R.string.arrive_at_time, OsmAndFormatter.getFormattedTime(ctx.getRoutingHelper().getLeftTime(), true)));

			AndroidUtils.setTextPrimaryColor(ctx, distanceText, nightMode);
			AndroidUtils.setTextSecondaryColor(ctx, distanceTitle, nightMode);
			AndroidUtils.setTextPrimaryColor(ctx, durationText, nightMode);
			AndroidUtils.setTextSecondaryColor(ctx, durationTitle, nightMode);
		}

		FrameLayout detailsButton = view.findViewById(R.id.details_button);

		AndroidUtils.setBackground(app, detailsButton, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, view.findViewById(R.id.details_button_descr), nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setBackground(app, view.findViewById(R.id.details_button_descr), nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		}
		int color = ContextCompat.getColor(mapActivity, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);
		AndroidUtils.setBackground(ctx, view.findViewById(R.id.dividerToDropDown), nightMode,
				R.color.divider_light, R.color.divider_dark);
		AndroidUtils.setBackground(ctx, view.findViewById(R.id.info_divider), nightMode,
				R.color.activity_background_light, R.color.route_info_cancel_button_color_dark);

		AndroidUtils.setBackground(ctx, view.findViewById(R.id.route_info_details_card), nightMode,
				R.color.activity_background_light, R.color.route_info_cancel_button_color_dark);
		AndroidUtils.setBackground(ctx, view.findViewById(R.id.RouteInfoControls), nightMode,
				R.color.route_info_bg_light, R.color.route_info_bg_dark);

		((TextView) view.findViewById(R.id.details_button_descr)).setTextColor(color);

		detailsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ShowRouteInfoDialogFragment.showDialog(mapActivity.getSupportFragmentManager());
			}
		});

		buildHeader(view);
	}

	private void buildHeader(View headerView) {
		OsmandApplication app = mapActivity.getMyApplication();
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
			elevationDataSet = null;
			slopeDataSet = null;
			mChart.setVisibility(View.GONE);
		}
	}
}