package net.osmand.plus.routepreparationmenu.cards;

import static net.osmand.plus.helpers.FontCache.getRobotoMedium;
import static net.osmand.plus.helpers.FontCache.getRobotoRegular;
import static net.osmand.plus.helpers.GpxUiHelper.GPXDataSetAxisType.DISTANCE;
import static net.osmand.plus.helpers.GpxUiHelper.createGPXElevationDataSet;
import static net.osmand.plus.helpers.GpxUiHelper.createGPXSlopeDataSet;
import static net.osmand.plus.utils.AndroidUtils.spToPx;
import static net.osmand.plus.utils.ColorUtilities.getPrimaryTextColor;
import static net.osmand.plus.utils.ColorUtilities.getSecondaryIconColor;
import static net.osmand.plus.utils.ColorUtilities.getSecondaryTextColor;
import static net.osmand.plus.utils.OsmAndFormatter.getFormattedAlt;
import static net.osmand.plus.utils.OsmAndFormatter.getFormattedDistanceValue;
import static net.osmand.plus.utils.OsmAndFormatter.getFormattedDuration;
import static net.osmand.plus.utils.OsmAndFormatter.getFormattedTimeShort;
import static net.osmand.plus.utils.UiUtilities.DialogButtonType.STROKED;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.routepreparationmenu.EmissionHelper;
import net.osmand.plus.routepreparationmenu.EmissionHelper.MotorType;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class SimpleRouteCard extends MapBaseCard {

	private final GPXFile gpxFile;
	private final RoutingHelper routingHelper;

	private LineData lineData;

	public SimpleRouteCard(@NonNull MapActivity mapActivity, @NonNull GPXFile gpxFile) {
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
		view.findViewById(R.id.route_info_controls).setOnClickListener(v -> notifyCardPressed());
	}

	private void setupInfoRows() {
		setupFirstRow();
		setupSecondRow();
	}

	private void setupSecondRow() {
		GPXTrackAnalysis analysis = gpxFile.getAnalysis(0);
		if (analysis.hasElevationData) {
			TextView uphill = view.findViewById(R.id.uphill);
			TextView downhill = view.findViewById(R.id.downhill);

			uphill.setText(getFormattedAlt(analysis.diffElevationUp, app));
			downhill.setText(getFormattedAlt(analysis.diffElevationDown, app));
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.uphill_container), analysis.hasElevationData);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.downhill_container), analysis.hasElevationData);
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
		LineChart chart = view.findViewById(R.id.chart);
		GPXTrackAnalysis analysis = gpxFile.getAnalysis(0);

		if (analysis.hasElevationData) {
			GpxUiHelper.setupGPXChart(chart, 10f, 4f, false);

			LineData data = lineData;
			if (data == null) {
				List<ILineDataSet> dataSets = new ArrayList<>();
				OrderedLineDataSet slopeDataSet;
				OrderedLineDataSet elevationDataSet = createGPXElevationDataSet(app, chart, analysis,
						DISTANCE, false, true, false);
				dataSets.add(elevationDataSet);
				slopeDataSet = createGPXSlopeDataSet(app, chart, analysis, DISTANCE,
						elevationDataSet.getEntries(), true, true, false);
				if (slopeDataSet != null) {
					dataSets.add(slopeDataSet);
				}
				data = new LineData(dataSets);
				lineData = data;
			}
			chart.setData(data);
		}
		AndroidUiHelper.updateVisibility(chart, analysis.hasElevationData);
	}

	private void setupDetailsButton() {
		View button = view.findViewById(R.id.details_button);
		button.setOnClickListener(v -> notifyCardPressed());
		UiUtilities.setupDialogButton(nightMode, button, STROKED, R.string.shared_string_details);
		AndroidUtils.setBackground(app, button, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
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
		builder.setSpan(new CustomTypefaceSpan(getRobotoRegular(app)), index, builder.length(), 0);
		builder.setSpan(new ForegroundColorSpan(getSecondaryTextColor(app, nightMode)), index, builder.length(), 0);
	}

	private void setupBullet(@NonNull SpannableStringBuilder builder) {
		int index = builder.length();
		builder.append(" • ");
		builder.setSpan(new AbsoluteSizeSpan(spToPx(app, 20)), index, builder.length(), 0);
		builder.setSpan(new CustomTypefaceSpan(getRobotoRegular(app)), index, builder.length(), 0);
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
		MetricsConstants constants = app.getSettings().METRIC_SYSTEM.get();
		FormattedValue value = getFormattedDistanceValue(distance, app, true, constants);

		int index = builder.length();
		builder.append(value.value);
		setupNumberSpans(builder, index);

		index = builder.length();
		builder.append(" ").append(value.unit);
		setupTextSpans(builder, index);
	}

	private void setupTextSpans(@NonNull SpannableStringBuilder builder, int index) {
		builder.setSpan(new AbsoluteSizeSpan(spToPx(app, 16)), index, builder.length(), 0);
		builder.setSpan(new CustomTypefaceSpan(getRobotoRegular(app)), index, builder.length(), 0);
		builder.setSpan(new ForegroundColorSpan(getSecondaryTextColor(app, nightMode)), index, builder.length(), 0);
	}

	private void setupNumberSpans(@NonNull SpannableStringBuilder builder, int index) {
		builder.setSpan(new AbsoluteSizeSpan(spToPx(app, 20)), index, builder.length(), 0);
		builder.setSpan(new CustomTypefaceSpan(getRobotoMedium(app)), index, builder.length(), 0);
		builder.setSpan(new ForegroundColorSpan(getPrimaryTextColor(app, nightMode)), index, builder.length(), 0);
	}
}