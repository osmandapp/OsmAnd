package net.osmand.plus.routepreparationmenu.cards;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.routepreparationmenu.Co2Computer;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;

import java.util.ArrayList;
import java.util.List;

public class SimpleRouteCard extends MapBaseCard {

	private MapActivity mapActivity;
	private GPXFile gpx;
	private LineData data;

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
	protected void updateContent() {
		RoutingHelper routingHelper = mapActivity.getRoutingHelper();

		view.findViewById(R.id.dividerToDropDown).setVisibility(View.VISIBLE);
		view.findViewById(R.id.route_info_details_card).setVisibility(View.VISIBLE);

		View info = view.findViewById(R.id.info_container);
		info.setOnClickListener(v -> notifyCardPressed());

		ImageView infoIcon = (ImageView) view.findViewById(R.id.InfoIcon);
		ImageView durationIcon = (ImageView) view.findViewById(R.id.DurationIcon);
		View infoDistanceView = view.findViewById(R.id.InfoDistance);
		View infoDurationView = view.findViewById(R.id.InfoDuration);

		infoIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_route_distance));
		infoIcon.setVisibility(View.VISIBLE);
		durationIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_time_span));
		durationIcon.setVisibility(View.VISIBLE);
		infoDistanceView.setVisibility(View.VISIBLE);
		infoDurationView.setVisibility(View.VISIBLE);

		TextView distanceText = (TextView) view.findViewById(R.id.DistanceText);
		TextView durationText = (TextView) view.findViewById(R.id.DurationText);
		TextView durationTitle = (TextView) view.findViewById(R.id.DurationTitle);

		distanceText.setText(OsmAndFormatter.getFormattedDistance(routingHelper.getLeftDistance(), app));
		durationText.setText(OsmAndFormatter.getFormattedDuration(routingHelper.getLeftTime(), app));
		durationTitle.setText(app.getString(R.string.arrive_at_time, OsmAndFormatter.getFormattedTime(routingHelper.getLeftTime(), true)));

		ImageView co2Icon = (ImageView) view.findViewById(R.id.CO2Icon);
		co2Icon.setImageDrawable(getContentIcon(R.drawable.ic_co2_24dp));
		final View co2Layout = view.findViewById(R.id.co2_container);
		final TextView co2Text = view.findViewById(R.id.CO2Text);
		boolean emitCO2 = Co2Computer.modeEmitCo2(routingHelper.getAppMode());
		if (emitCO2) {
			co2Text.setText(Co2Computer.getFormattedCO2(routingHelper.getLeftDistance(), app));
			co2Layout.setVisibility(View.VISIBLE);
			co2Icon.setVisibility(View.VISIBLE);
		} else {
			co2Layout.setVisibility(View.GONE);
			co2Icon.setVisibility(View.GONE);
		}

		view.findViewById(R.id.details_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardButtonPressed(SimpleRouteCard.this, 0);
				}
			}
		});
		view.findViewById(R.id.details_button).setOnClickListener(v -> notifyButtonPressed(0));

		FrameLayout detailsButton = view.findViewById(R.id.details_button);
		AndroidUtils.setBackground(app, detailsButton, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
		AndroidUtils.setBackground(app, view.findViewById(R.id.details_button_descr), nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);

		buildHeader(view);
	}

	private void buildHeader(View headerView) {
		final LineChart mChart = headerView.findViewById(R.id.chart);
		final GPXTrackAnalysis analysis = gpx.getAnalysis(0);

		GpxUiHelper.setupGPXChart(mChart, 10f, 4f, false);
		if (analysis.hasElevationData) {
			LineData data = this.data;
			if (data == null) {
				List<ILineDataSet> dataSets = new ArrayList<>();
				OrderedLineDataSet slopeDataSet;
				OrderedLineDataSet elevationDataSet = GpxUiHelper.createGPXElevationDataSet(app, mChart, analysis,
						GpxUiHelper.GPXDataSetAxisType.DISTANCE, false, true, false);
				dataSets.add(elevationDataSet);
				slopeDataSet = GpxUiHelper.createGPXSlopeDataSet(app, mChart, analysis,
						GpxUiHelper.GPXDataSetAxisType.DISTANCE, elevationDataSet.getValues(), true, true, false);
				if (slopeDataSet != null) {
					dataSets.add(slopeDataSet);
				}
				data = new LineData(dataSets);
				this.data = data;
			}
			mChart.setData(data);
			mChart.setVisibility(View.VISIBLE);
		} else {
			mChart.setVisibility(View.GONE);
		}
	}
}