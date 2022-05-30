package net.osmand.plus.routepreparationmenu.cards;

import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetAxisType;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.ChartPointLayer;
import net.osmand.plus.measurementtool.graph.CommonChartAdapter;
import net.osmand.plus.routing.RoutingHelper;

import java.util.ArrayList;
import java.util.List;

public class RouteStatisticCard extends MapBaseCard {

	public static final int DETAILS_BUTTON_INDEX = 0;
	public static final int START_BUTTON_INDEX = 1;

	private GPXFile gpx;
	private GpxDisplayItem gpxItem;
	@Nullable
	private OrderedLineDataSet slopeDataSet;
	@Nullable
	private OrderedLineDataSet elevationDataSet;
	private OnClickListener onAnalyseClickListener;
	private CommonChartAdapter graphAdapter;

	public RouteStatisticCard(MapActivity mapActivity, GPXFile gpx,
							  OnClickListener onAnalyseClickListener) {
		super(mapActivity);
		this.gpx = gpx;
		this.onAnalyseClickListener = onAnalyseClickListener;
		this.gpxItem = GpxUiHelper.makeGpxDisplayItem(app, gpx, ChartPointLayer.ROUTE);
	}

	@Nullable
	public GPXFile getGpx() {
		return gpx;
	}

	@Nullable
	public GpxDisplayItem getGpxItem() {
		return gpxItem;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.route_info_header;
	}

	@Override
	protected void updateContent() {
		OsmandApplication app = getMyApplication();
		RoutingHelper routingHelper = app.getRoutingHelper();

		((ImageView) view.findViewById(R.id.distance_icon)).setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_route_distance));
		((ImageView) view.findViewById(R.id.time_icon)).setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_time_span));

		int dist = routingHelper.getLeftDistance();
		String text = OsmAndFormatter.getFormattedDistance(dist, app);
		SpannableStringBuilder distanceStr = new SpannableStringBuilder(text);
		int spaceIndex = text.indexOf(" ");
		if (spaceIndex != -1) {
			distanceStr.setSpan(new ForegroundColorSpan(getMainFontColor()), 0, spaceIndex, 0);
		}
		TextView distanceTv = (TextView) view.findViewById(R.id.distance);
		distanceTv.setText(distanceStr);

		int time = routingHelper.getLeftTime();
		SpannableStringBuilder timeStr = new SpannableStringBuilder();
		timeStr.append(OsmAndFormatter.getFormattedDuration(time, app));
		spaceIndex = timeStr.toString().lastIndexOf(" ");
		if (spaceIndex != -1) {
			timeStr.setSpan(new ForegroundColorSpan(getMainFontColor()), 0, spaceIndex, 0);
		}
		TextView timeTv = (TextView) view.findViewById(R.id.time);
		timeTv.setText(timeStr);

		TextView arriveTimeTv = (TextView) view.findViewById(R.id.time_desc);
		String arriveStr = app.getString(R.string.arrive_at_time, OsmAndFormatter.getFormattedTime(time, true));
		arriveTimeTv.setText(arriveStr);

		buildSlopeInfo();
		updateButtons();

		if (isTransparentBackground()) {
			view.setBackgroundDrawable(null);
		}
	}

	@Override
	public int getTopViewHeight() {
		View altitudeContainer = view.findViewById(R.id.altitude_container);
		return (int) altitudeContainer.getY();
	}

	private void updateButtons() {
		FrameLayout detailsButton = view.findViewById(R.id.details_button);
		TextView detailsButtonDescr = view.findViewById(R.id.details_button_descr);
		AndroidUtils.setBackground(app, detailsButton, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
		AndroidUtils.setBackground(app, detailsButtonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		detailsButton.setOnClickListener(v -> notifyButtonPressed(DETAILS_BUTTON_INDEX));

		FrameLayout startButton = view.findViewById(R.id.start_button);
		TextView startButtonDescr = view.findViewById(R.id.start_button_descr);
		AndroidUtils.setBackground(app, startButton, nightMode, R.drawable.btn_active_light, R.drawable.btn_active_dark);
		int color = ContextCompat.getColor(app, R.color.card_and_list_background_light);
		startButton.setOnClickListener(v -> notifyButtonPressed(START_BUTTON_INDEX));
		RoutingHelper helper = app.getRoutingHelper();
		if (helper.isFollowingMode() || helper.isPauseNavigation()) {
			startButtonDescr.setText(R.string.shared_string_resume);
		} else {
			startButtonDescr.setText(R.string.shared_string_control_start);
		}
		startButtonDescr.setTextColor(color);
	}

	private void buildSlopeInfo() {
		GPXTrackAnalysis analysis = gpx.getAnalysis(0);

		buildHeader(analysis);
		if (analysis.hasElevationData) {
			((TextView) view.findViewById(R.id.average_text)).setText(OsmAndFormatter.getFormattedAlt(analysis.avgElevation, app));

			String min = OsmAndFormatter.getFormattedAlt(analysis.minElevation, app);
			String max = OsmAndFormatter.getFormattedAlt(analysis.maxElevation, app);
			((TextView) view.findViewById(R.id.range_text)).setText(min + " - " + max);

			String asc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationUp, app);
			String desc = OsmAndFormatter.getFormattedAlt(analysis.diffElevationDown, app);
			((TextView) view.findViewById(R.id.descent_text)).setText(desc);
			((TextView) view.findViewById(R.id.ascent_text)).setText(asc);

			((ImageView) view.findViewById(R.id.average_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_altitude_average));
			((ImageView) view.findViewById(R.id.range_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_altitude_average));
			((ImageView) view.findViewById(R.id.descent_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_altitude_descent));
			((ImageView) view.findViewById(R.id.ascent_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_altitude_ascent));

			TextView analyseButtonDescr = (TextView) view.findViewById(R.id.analyse_button_descr);

			FrameLayout analyseButton = (FrameLayout) view.findViewById(R.id.analyse_button);
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
				AndroidUtils.setBackground(app, analyseButton, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
				AndroidUtils.setBackground(app, analyseButtonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
			} else {
				AndroidUtils.setBackground(app, analyseButton, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
			}
			analyseButton.setOnClickListener(onAnalyseClickListener);
		}
		view.findViewById(R.id.altitude_container).setVisibility(analysis.hasElevationData ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.slope_info_divider).setVisibility(analysis.hasElevationData ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.slope_container).setVisibility(analysis.hasElevationData ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.buttons_container).setVisibility(analysis.hasElevationData ? View.VISIBLE : View.GONE);
	}

	@Nullable
	public OrderedLineDataSet getSlopeDataSet() {
		return slopeDataSet;
	}

	@Nullable
	public OrderedLineDataSet getElevationDataSet() {
		return elevationDataSet;
	}

	@Nullable
	public CommonChartAdapter getGraphAdapter() {
		return graphAdapter;
	}

	private void buildHeader(GPXTrackAnalysis analysis) {
		LineChart mChart = view.findViewById(R.id.chart);
		GpxUiHelper.setupGPXChart(mChart, 24f, 16f, true);
		graphAdapter = new CommonChartAdapter(app, mChart, true);

		if (analysis.hasElevationData) {
			List<ILineDataSet> dataSets = new ArrayList<>();
			OrderedLineDataSet slopeDataSet;
			OrderedLineDataSet elevationDataSet = GpxUiHelper.createGPXElevationDataSet(app, mChart, analysis,
					GPXDataSetAxisType.DISTANCE, false, true, false);
			dataSets.add(elevationDataSet);
			slopeDataSet = GpxUiHelper.createGPXSlopeDataSet(app, mChart, analysis,
					GPXDataSetAxisType.DISTANCE, elevationDataSet.getValues(), true, true, false);
			if (slopeDataSet != null) {
				dataSets.add(slopeDataSet);
			}
			this.elevationDataSet = elevationDataSet;
			this.slopeDataSet = slopeDataSet;

			graphAdapter.updateContent(new LineData(dataSets), gpxItem);
			mChart.setVisibility(View.VISIBLE);
		} else {
			mChart.setVisibility(View.GONE);
		}
	}
}