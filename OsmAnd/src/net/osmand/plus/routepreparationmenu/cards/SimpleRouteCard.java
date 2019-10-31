package net.osmand.plus.routepreparationmenu.cards;

import android.os.Build;
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
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.routing.RoutingHelper;

import java.util.ArrayList;
import java.util.List;

public class SimpleRouteCard extends BaseCard {

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
		info.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardPressed(SimpleRouteCard.this);
				}
			}
		});

		ImageView infoIcon = (ImageView) view.findViewById(R.id.InfoIcon);
		ImageView durationIcon = (ImageView) view.findViewById(R.id.DurationIcon);
		View infoDistanceView = view.findViewById(R.id.InfoDistance);
		View infoDurationView = view.findViewById(R.id.InfoDuration);
//		if (directionInfo >= 0) {
//			infoIcon.setVisibility(View.GONE);
//			durationIcon.setVisibility(View.GONE);
//			infoDistanceView.setVisibility(View.GONE);
//			infoDurationView.setVisibility(View.GONE);
//		} else {
		infoIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_route_distance));
		infoIcon.setVisibility(View.VISIBLE);
		durationIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_time_span));
		durationIcon.setVisibility(View.VISIBLE);
		infoDistanceView.setVisibility(View.VISIBLE);
		infoDurationView.setVisibility(View.VISIBLE);
//		}
//		if (directionInfo >= 0 && routingHelper.getRouteDirections() != null
//				&& directionInfo < routingHelper.getRouteDirections().size()) {
//			RouteDirectionInfo ri = routingHelper.getRouteDirections().get(directionInfo);
//		} else {
		TextView distanceText = (TextView) view.findViewById(R.id.DistanceText);
		TextView distanceTitle = (TextView) view.findViewById(R.id.DistanceTitle);
		TextView durationText = (TextView) view.findViewById(R.id.DurationText);
		TextView durationTitle = (TextView) view.findViewById(R.id.DurationTitle);

		distanceText.setText(OsmAndFormatter.getFormattedDistance(routingHelper.getLeftDistance(), app));
		durationText.setText(OsmAndFormatter.getFormattedDuration(routingHelper.getLeftTime(), app));
		durationTitle.setText(app.getString(R.string.arrive_at_time, OsmAndFormatter.getFormattedTime(routingHelper.getLeftTime(), true)));

		view.findViewById(R.id.details_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardButtonPressed(SimpleRouteCard.this, 0);
				}
			}
		});

		FrameLayout detailsButton = view.findViewById(R.id.details_button);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, detailsButton, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
			AndroidUtils.setBackground(app, view.findViewById(R.id.details_button_descr), nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setBackground(app, view.findViewById(R.id.details_button_descr), nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
		}

		buildHeader(view);
	}

	private void buildHeader(View headerView) {
		final LineChart mChart = (LineChart) headerView.findViewById(R.id.chart);
		final GPXUtilities.GPXTrackAnalysis analysis = gpx.getAnalysis(0);

		GpxUiHelper.setupGPXChart(mChart, 4, 10f, 4f, !nightMode, false);
		if (analysis.hasElevationData) {
			LineData data = this.data;
			if (data == null) {
				List<ILineDataSet> dataSets = new ArrayList<>();
				OrderedLineDataSet slopeDataSet = null;
				OrderedLineDataSet elevationDataSet = GpxUiHelper.createGPXElevationDataSet(app, mChart, analysis,
						GpxUiHelper.GPXDataSetAxisType.DISTANCE, false, true);
				if (elevationDataSet != null) {
					dataSets.add(elevationDataSet);
					slopeDataSet = GpxUiHelper.createGPXSlopeDataSet(app, mChart, analysis,
							GpxUiHelper.GPXDataSetAxisType.DISTANCE, elevationDataSet.getValues(), true, true);
				}
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