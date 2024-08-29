package net.osmand.plus.routepreparationmenu.cards;

import static android.graphics.Typeface.DEFAULT;
import static net.osmand.plus.charts.ChartUtils.createGPXElevationDataSet;
import static net.osmand.plus.charts.ChartUtils.createGPXSlopeDataSet;
import static net.osmand.plus.charts.GPXDataSetAxisType.DISTANCE;
import static net.osmand.plus.settings.enums.TrackApproximationType.MANUAL;
import static net.osmand.plus.utils.AndroidUtils.spToPx;
import static net.osmand.plus.utils.ColorUtilities.getPrimaryTextColor;
import static net.osmand.plus.utils.ColorUtilities.getSecondaryIconColor;
import static net.osmand.plus.utils.ColorUtilities.getSecondaryTextColor;
import static net.osmand.plus.utils.OsmAndFormatter.getFormattedAlt;
import static net.osmand.plus.utils.OsmAndFormatter.getFormattedDistanceValue;
import static net.osmand.plus.utils.OsmAndFormatter.getFormattedDuration;
import static net.osmand.plus.utils.OsmAndFormatter.getFormattedTimeShort;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.ElevationChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.charts.ChartUtils;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.charts.OrderedLineDataSet;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.EmissionHelper;
import net.osmand.plus.routepreparationmenu.EmissionHelper.MotorType;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class SimpleRouteCard extends MapBaseCard {

	private final GpxFile gpxFile;
	private final RoutingHelper routingHelper;

	private LineData lineData;

	public SimpleRouteCard(@NonNull MapActivity mapActivity, @NonNull GpxFile gpxFile) {
		super(mapActivity);
		this.gpxFile = gpxFile;
		routingHelper = mapActivity.getRoutingHelper();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.route_info_statistic;
	}

	@Override
	protected void updateContent() {
		setupChart();
		setupInfoRows();
		setupDetailsButton();
		setupAttachToRoadsCard();
		view.findViewById(R.id.route_info_controls).setOnClickListener(v -> notifyCardPressed());
	}

	private void setupInfoRows() {
		setupFirstRow();
		setupSecondRow();
	}

	private void setupSecondRow() {
		GpxTrackAnalysis analysis = gpxFile.getAnalysis(0);
		boolean hasElevationData = analysis.hasElevationData();
		if (hasElevationData) {
			TextView uphill = view.findViewById(R.id.uphill);
			TextView downhill = view.findViewById(R.id.downhill);

			uphill.setText(getFormattedAlt(analysis.getDiffElevationUp(), app));
			downhill.setText(getFormattedAlt(analysis.getDiffElevationDown(), app));
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.uphill_container), hasElevationData);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.downhill_container), hasElevationData);
		setupEmission();
	}

	private void setupEmission() {
		EmissionHelper emissionHelper = new EmissionHelper(app);
		MotorType motorType = emissionHelper.getMotorTypeForMode(routingHelper.getAppMode());
		if (motorType != null) {
			emissionHelper.getEmission(motorType, routingHelper.getLeftDistance(), result -> {
				TextView textView = view.findViewById(R.id.emission);
				textView.setText(result);
				AndroidUiHelper.updateVisibility(view.findViewById(R.id.emission_container), true);
				return true;
			});
		}
	}

	private void setupChart() {
		ElevationChart chart = view.findViewById(R.id.chart);
		GpxTrackAnalysis analysis = gpxFile.getAnalysis(0);

		if (analysis.hasElevationData()) {
			ChartUtils.setupElevationChart(chart, 10f, 4f, false);

			LineData data = lineData;
			if (data == null) {
				List<ILineDataSet> dataSets = new ArrayList<>();
				OrderedLineDataSet slopeDataSet;
				OrderedLineDataSet elevationDataSet = createGPXElevationDataSet(app, chart, analysis,
						GPXDataSetType.ALTITUDE, DISTANCE, false, true, false);
				dataSets.add(elevationDataSet);
				slopeDataSet = createGPXSlopeDataSet(app, chart, analysis, GPXDataSetType.SLOPE, DISTANCE,
						elevationDataSet.getEntries(), true, true, false);
				if (slopeDataSet != null) {
					dataSets.add(slopeDataSet);
				}
				data = new LineData(dataSets);
				lineData = data;
			}
			chart.setData(data);
		}
		AndroidUiHelper.updateVisibility(chart, analysis.hasElevationData());
	}

	private void setupDetailsButton() {
		DialogButton button = view.findViewById(R.id.details_button);
		button.setOnClickListener(v -> notifyCardPressed());
		AndroidUtils.setBackground(app, button.getButtonView(), nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
	}

	private void setupFirstRow() {
		SpannableStringBuilder builder = new SpannableStringBuilder();
		setupDistance(builder);
		setupBullet(builder);
		setupDuration(builder);
		setupArrivalTime(builder);

		TextView textView = view.findViewById(R.id.firstRow);
		textView.setText(builder);
	}

	private void setupArrivalTime(@NonNull SpannableStringBuilder builder) {
		int index = builder.length();
		String arriveTime = getFormattedTimeShort(routingHelper.getLeftTime(), true);

		builder.append(" (").append(arriveTime).append(")");
		builder.setSpan(new AbsoluteSizeSpan(spToPx(app, 20)), index, builder.length(), 0);
		builder.setSpan(new CustomTypefaceSpan(DEFAULT), index, builder.length(), 0);
		builder.setSpan(new ForegroundColorSpan(getSecondaryTextColor(app, nightMode)), index, builder.length(), 0);
	}

	private void setupBullet(@NonNull SpannableStringBuilder builder) {
		int index = builder.length();
		builder.append(" • ");
		builder.setSpan(new AbsoluteSizeSpan(spToPx(app, 20)), index, builder.length(), 0);
		builder.setSpan(new CustomTypefaceSpan(DEFAULT), index, builder.length(), 0);
		builder.setSpan(new ForegroundColorSpan(getSecondaryIconColor(app, nightMode)), index, builder.length(), 0);
	}

	private void setupDuration(@NonNull SpannableStringBuilder builder) {
		String duration = getFormattedDuration(routingHelper.getLeftTime(), app);
		String[] items = duration.split(" ");
		for (String item : items) {
			int index = builder.length();
			builder.append(item).append(" ");
			if (TextUtils.isDigitsOnly(item) || Algorithms.stringsEqual("<1", item)) {
				setupNumberSpans(builder, index);
			} else {
				setupTextSpans(builder, index);
			}
		}
	}

	private void setupDistance(@NonNull SpannableStringBuilder builder) {
		int distance = routingHelper.getLeftDistance();
		FormattedValue value = getFormattedDistanceValue(distance, app);

		int index = builder.length();
		builder.append(value.value);
		setupNumberSpans(builder, index);

		index = builder.length();
		builder.append(" ").append(value.unit);
		setupTextSpans(builder, index);
	}

	private void setupAttachToRoadsCard() {
		FrameLayout container = view.findViewById(R.id.attach_to_roads_banner_container);
		container.removeAllViews();

		GpxFile gpxFile = routingHelper.getCurrentGPX();
		ApplicationMode appMode = routingHelper.getAppMode();
		if (gpxFile != null && !gpxFile.isAttachedToRoads() && settings.DETAILED_TRACK_GUIDANCE.getModeValue(appMode) == MANUAL) {
			AttachTrackToRoadsBannerCard card = new AttachTrackToRoadsBannerCard(mapActivity);
			card.setListener(getListener());
			container.addView(card.build(mapActivity));
			AndroidUiHelper.updateVisibility(container, true);
		} else {
			AndroidUiHelper.updateVisibility(container, false);
		}
	}

	private void setupTextSpans(@NonNull SpannableStringBuilder builder, int index) {
		builder.setSpan(new AbsoluteSizeSpan(spToPx(app, 16)), index, builder.length(), 0);
		builder.setSpan(new CustomTypefaceSpan(DEFAULT), index, builder.length(), 0);
		builder.setSpan(new ForegroundColorSpan(getSecondaryTextColor(app, nightMode)), index, builder.length(), 0);
	}

	private void setupNumberSpans(@NonNull SpannableStringBuilder builder, int index) {
		builder.setSpan(new AbsoluteSizeSpan(spToPx(app, 20)), index, builder.length(), 0);
		builder.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), index, builder.length(), 0);
		builder.setSpan(new ForegroundColorSpan(getPrimaryTextColor(app, nightMode)), index, builder.length(), 0);
	}
}