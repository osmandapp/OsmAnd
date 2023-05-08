package net.osmand.plus.charts;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM;
import static net.osmand.plus.utils.OsmAndFormatter.FEET_IN_ONE_METER;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_KILOMETER;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_MILE;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE;
import static net.osmand.plus.utils.OsmAndFormatter.YARDS_IN_ONE_METER;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.settings.enums.SpeedConstants;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.util.Algorithms;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChartUtils {

	public static final int CHART_LABEL_COUNT = 3;
	private static final int MAX_CHART_DATA_ITEMS = 10000;

	public static void setupGPXChart(@NonNull LineChart mChart) {
		setupGPXChart(mChart, 24f, 16f, true);
	}

	public static void setupGPXChart(@NonNull LineChart mChart, float topOffset, float bottomOffset,
	                                 boolean useGesturesAndScale) {
		setupGPXChart(mChart, topOffset, bottomOffset, useGesturesAndScale, null);
	}

	public static void setupGPXChart(@NonNull LineChart mChart, float topOffset, float bottomOffset,
	                                 boolean useGesturesAndScale, @Nullable Drawable markerIcon) {
		GpxMarkerView markerView = new GpxMarkerView(mChart.getContext(), markerIcon);
		setupGPXChart(mChart, markerView, topOffset, bottomOffset, useGesturesAndScale);
	}

	public static void setupGPXChart(@NonNull LineChart mChart, @NonNull GpxMarkerView markerView,
	                                 float topOffset, float bottomOffset, boolean useGesturesAndScale) {
		Context context = mChart.getContext();

		mChart.setHardwareAccelerationEnabled(true);
		mChart.setTouchEnabled(useGesturesAndScale);
		mChart.setDragEnabled(useGesturesAndScale);
		mChart.setScaleEnabled(useGesturesAndScale);
		mChart.setPinchZoom(useGesturesAndScale);
		mChart.setScaleYEnabled(false);
		mChart.setAutoScaleMinMaxEnabled(true);
		mChart.setDrawBorders(false);
		mChart.getDescription().setEnabled(false);
		mChart.setMaxVisibleValueCount(10);
		mChart.setMinOffset(0f);
		mChart.setDragDecelerationEnabled(false);

		mChart.setExtraTopOffset(topOffset);
		mChart.setExtraBottomOffset(bottomOffset);

		// create a custom MarkerView (extend MarkerView) and specify the layout
		// to use for it
		markerView.setChartView(mChart); // For bounds control
		mChart.setMarker(markerView); // Set the marker to the chart
		mChart.setDrawMarkers(true);

		ChartLabel chartLabel = new ChartLabel(context, R.layout.chart_label);
		chartLabel.setChart(mChart);
		mChart.setYAxisLabelView(chartLabel);

		int xAxisRulerColor = ContextCompat.getColor(context, R.color.gpx_chart_black_grid);
		int labelsColor = ContextCompat.getColor(context, R.color.description_font_and_bottom_sheet_icons);
		XAxis xAxis = mChart.getXAxis();
		xAxis.setDrawAxisLine(true);
		xAxis.setDrawAxisLineBehindData(false);
		xAxis.setAxisLineWidth(1);
		xAxis.setAxisLineColor(xAxisRulerColor);
		xAxis.setDrawGridLines(true);
		xAxis.setDrawGridLinesBehindData(false);
		xAxis.setGridLineWidth(1.5f);
		xAxis.setGridColor(xAxisRulerColor);
		xAxis.enableGridDashedLine(25f, Float.MAX_VALUE, 0f);
		xAxis.setPosition(BOTTOM);
		xAxis.setTextColor(labelsColor);

		int dp4 = AndroidUtils.dpToPx(context, 4);
		int yAxisGridColor = AndroidUtils.getColorFromAttr(context, R.attr.chart_grid_line_color);

		YAxis leftYAxis = mChart.getAxisLeft();
		leftYAxis.enableGridDashedLine(dp4, dp4, 0f);
		leftYAxis.setGridColor(yAxisGridColor);
		leftYAxis.setGridLineWidth(1f);
		leftYAxis.setDrawBottomYGridLine(false);
		leftYAxis.setDrawAxisLine(false);
		leftYAxis.setDrawGridLinesBehindData(false);
		leftYAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
		leftYAxis.setXOffset(16f);
		leftYAxis.setYOffset(-6f);
		leftYAxis.setLabelCount(CHART_LABEL_COUNT, true);

		YAxis rightYAxis = mChart.getAxisRight();
		rightYAxis.setDrawAxisLine(false);
		rightYAxis.setDrawGridLines(false);
		rightYAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
		rightYAxis.setXOffset(16f);
		rightYAxis.setYOffset(-6f);
		rightYAxis.setLabelCount(CHART_LABEL_COUNT, true);
		rightYAxis.setEnabled(false);

		Legend legend = mChart.getLegend();
		legend.setEnabled(false);
	}

	private static float setupAxisDistance(OsmandApplication ctx, AxisBase axisBase, float meters) {
		OsmandSettings settings = ctx.getSettings();
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		float divX;

		String format1 = "{0,number,0.#} ";
		String format2 = "{0,number,0.##} ";
		String fmt = null;
		float granularity = 1f;
		int mainUnitStr;
		float mainUnitInMeters;
		if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
			mainUnitStr = R.string.km;
			mainUnitInMeters = METERS_IN_KILOMETER;
		} else if (mc == MetricsConstants.NAUTICAL_MILES_AND_METERS || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
			mainUnitStr = R.string.nm;
			mainUnitInMeters = METERS_IN_ONE_NAUTICALMILE;
		} else {
			mainUnitStr = R.string.mile;
			mainUnitInMeters = METERS_IN_ONE_MILE;
		}
		if (meters > 9.99f * mainUnitInMeters) {
			fmt = format1;
			granularity = .1f;
		}
		if (meters >= 100 * mainUnitInMeters ||
				meters > 9.99f * mainUnitInMeters ||
				meters > 0.999f * mainUnitInMeters ||
				mc == MetricsConstants.MILES_AND_FEET && meters > 0.249f * mainUnitInMeters ||
				mc == MetricsConstants.MILES_AND_METERS && meters > 0.249f * mainUnitInMeters ||
				mc == MetricsConstants.MILES_AND_YARDS && meters > 0.249f * mainUnitInMeters ||
				mc == MetricsConstants.NAUTICAL_MILES_AND_METERS && meters > 0.99f * mainUnitInMeters ||
				mc == MetricsConstants.NAUTICAL_MILES_AND_FEET && meters > 0.99f * mainUnitInMeters) {

			divX = mainUnitInMeters;
			if (fmt == null) {
				fmt = format2;
				granularity = .01f;
			}
		} else {
			fmt = null;
			granularity = 1f;
			if (mc == MetricsConstants.KILOMETERS_AND_METERS || mc == MetricsConstants.MILES_AND_METERS) {
				divX = 1f;
				mainUnitStr = R.string.m;
			} else if (mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
				divX = 1f / FEET_IN_ONE_METER;
				mainUnitStr = R.string.foot;
			} else if (mc == MetricsConstants.MILES_AND_YARDS) {
				divX = 1f / YARDS_IN_ONE_METER;
				mainUnitStr = R.string.yard;
			} else {
				divX = 1f;
				mainUnitStr = R.string.m;
			}
		}

		String formatX = fmt;
		String mainUnitX = ctx.getString(mainUnitStr);

		axisBase.setGranularity(granularity);
		axisBase.setValueFormatter((value, axis) -> {
			if (!Algorithms.isEmpty(formatX)) {
				return MessageFormat.format(formatX + mainUnitX, value);
			} else {
				return OsmAndFormatter.formatInteger((int) (value + 0.5), mainUnitX, ctx);
			}
		});

		return divX;
	}

	private static float setupXAxisTime(XAxis xAxis, long timeSpan) {
		boolean useHours = timeSpan / HOUR_IN_MILLIS > 0;
		xAxis.setGranularity(1f);
		xAxis.setValueFormatter((value, axis) -> formatXAxisTime((int) (value + 0.5), useHours));
		return 1f;
	}

	public static String formatXAxisTime(int seconds, boolean useHours) {
		if (useHours) {
			return OsmAndFormatter.getFormattedDurationShort(seconds);
		} else {
			int minutes = (seconds / 60) % 60;
			int sec = seconds % 60;
			return (minutes < 10 ? "0" + minutes : minutes) + ":" + (sec < 10 ? "0" + sec : sec);
		}
	}

	private static float setupXAxisTimeOfDay(@NonNull XAxis xAxis, long startTime) {
		xAxis.setGranularity(1f);
		xAxis.setValueFormatter((seconds, axis) -> {
			long time = startTime + (long) (seconds * 1000);
			return OsmAndFormatter.getFormattedFullTime(time);
		});
		return 1f;
	}

	private static List<Entry> calculateElevationArray(GPXTrackAnalysis analysis, GPXDataSetAxisType axisType,
	                                                   float divX, float convEle, boolean useGeneralTrackPoints, boolean calcWithoutGaps) {
		List<Entry> values = new ArrayList<>();
		List<GPXUtilities.Elevation> elevationData = analysis.elevationData;
		float nextX = 0;
		float nextY;
		float elev;
		float prevElevOrig = -80000;
		float prevElev = 0;
		int i = -1;
		int lastIndex = elevationData.size() - 1;
		Entry lastEntry = null;
		float lastXSameY = -1;
		boolean hasSameY = false;
		float x = 0f;
		for (GPXUtilities.Elevation e : elevationData) {
			i++;
			if (axisType == GPXDataSetAxisType.TIME || axisType == GPXDataSetAxisType.TIMEOFDAY) {
				x = e.timeDiff;
			} else {
				x = e.distance;
			}
			if (x >= 0) {
				if (!(calcWithoutGaps && e.firstPoint && lastEntry != null)) {
					nextX += x / divX;
				}
				if (!Float.isNaN(e.elevation)) {
					elev = e.elevation;
					if (prevElevOrig != -80000) {
						if (elev > prevElevOrig) {
							//elev -= 1f;
						} else if (prevElevOrig == elev && i < lastIndex) {
							hasSameY = true;
							lastXSameY = nextX;
							continue;
						}
						if (prevElev == elev && i < lastIndex) {
							hasSameY = true;
							lastXSameY = nextX;
							continue;
						}
						if (hasSameY) {
							values.add(new Entry(lastXSameY, lastEntry.getY()));
						}
						hasSameY = false;
					}
					if (useGeneralTrackPoints && e.firstPoint && lastEntry != null) {
						values.add(new Entry(nextX, lastEntry.getY()));
					}
					prevElevOrig = e.elevation;
					prevElev = elev;
					nextY = elev * convEle;
					lastEntry = new Entry(nextX, nextY);
					values.add(lastEntry);
				}
			}
		}
		return values;
	}

	public static void setupHorizontalGPXChart(OsmandApplication app, HorizontalBarChart chart, int yLabelsCount,
	                                           float topOffset, float bottomOffset, boolean useGesturesAndScale, boolean nightMode) {
		chart.setHardwareAccelerationEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
		chart.setTouchEnabled(useGesturesAndScale);
		chart.setDragEnabled(useGesturesAndScale);
		chart.setScaleYEnabled(false);
		chart.setAutoScaleMinMaxEnabled(true);
		chart.setDrawBorders(true);
		chart.getDescription().setEnabled(false);
		chart.setDragDecelerationEnabled(false);

		chart.setExtraTopOffset(topOffset);
		chart.setExtraBottomOffset(bottomOffset);

		XAxis xl = chart.getXAxis();
		xl.setDrawLabels(false);
		xl.setEnabled(false);
		xl.setDrawAxisLine(false);
		xl.setDrawGridLines(false);

		YAxis yl = chart.getAxisLeft();
		yl.setLabelCount(yLabelsCount);
		yl.setDrawLabels(false);
		yl.setEnabled(false);
		yl.setDrawAxisLine(false);
		yl.setDrawGridLines(false);
		yl.setAxisMinimum(0f);

		YAxis yr = chart.getAxisRight();
		yr.setLabelCount(yLabelsCount);
		yr.setDrawAxisLine(false);
		yr.setDrawGridLines(false);
		yr.setAxisMinimum(0f);
		chart.setMinOffset(0);

		int mainFontColor = ColorUtilities.getPrimaryTextColor(app, nightMode);
		yl.setTextColor(mainFontColor);
		yr.setTextColor(mainFontColor);

		chart.setFitBars(true);
		chart.setBorderColor(ColorUtilities.getDividerColor(app, nightMode));

		Legend l = chart.getLegend();
		l.setEnabled(false);
	}

	public static <E> BarData buildStatisticChart(@NonNull OsmandApplication app,
	                                              @NonNull HorizontalBarChart mChart,
	                                              @NonNull RouteStatisticsHelper.RouteStatistics routeStatistics,
	                                              @NonNull GPXTrackAnalysis analysis,
	                                              boolean useRightAxis,
	                                              boolean nightMode) {

		XAxis xAxis = mChart.getXAxis();
		xAxis.setEnabled(false);

		YAxis yAxis;
		if (useRightAxis) {
			yAxis = mChart.getAxisRight();
			yAxis.setEnabled(true);
		} else {
			yAxis = mChart.getAxisLeft();
		}
		float divX = setupAxisDistance(app, yAxis, analysis.totalDistance);

		List<RouteStatisticsHelper.RouteSegmentAttribute> segments = routeStatistics.elements;
		List<BarEntry> entries = new ArrayList<>();
		float[] stacks = new float[segments.size()];
		int[] colors = new int[segments.size()];
		for (int i = 0; i < stacks.length; i++) {
			RouteStatisticsHelper.RouteSegmentAttribute segment = segments.get(i);
			stacks[i] = segment.getDistance() / divX;
			colors[i] = segment.getColor();
		}
		entries.add(new BarEntry(0, stacks));
		BarDataSet barDataSet = new BarDataSet(entries, "");
		barDataSet.setColors(colors);
		barDataSet.setHighLightColor(ColorUtilities.getSecondaryTextColor(app, nightMode));
		BarData dataSet = new BarData(barDataSet);
		dataSet.setDrawValues(false);
		dataSet.setBarWidth(1);
		mChart.getAxisRight().setAxisMaximum(dataSet.getYMax());
		mChart.getAxisLeft().setAxisMaximum(dataSet.getYMax());

		return dataSet;
	}

	public static OrderedLineDataSet createGPXElevationDataSet(@NonNull OsmandApplication ctx,
	                                                           @NonNull LineChart mChart,
	                                                           @NonNull GPXTrackAnalysis analysis,
	                                                           @NonNull GPXDataSetAxisType axisType,
	                                                           boolean useRightAxis,
	                                                           boolean drawFilled,
	                                                           boolean calcWithoutGaps) {
		OsmandSettings settings = ctx.getSettings();
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		boolean useFeet = (mc == MetricsConstants.MILES_AND_FEET) || (mc == MetricsConstants.MILES_AND_YARDS) || (mc == MetricsConstants.NAUTICAL_MILES_AND_FEET);
		boolean light = settings.isLightContent();
		float convEle = useFeet ? 3.28084f : 1.0f;

		float divX;
		XAxis xAxis = mChart.getXAxis();
		if (axisType == GPXDataSetAxisType.TIME && analysis.isTimeSpecified()) {
			divX = setupXAxisTime(xAxis, calcWithoutGaps ? analysis.timeSpanWithoutGaps : analysis.timeSpan);
		} else if (axisType == GPXDataSetAxisType.TIMEOFDAY && analysis.isTimeSpecified()) {
			divX = setupXAxisTimeOfDay(xAxis, analysis.startTime);
		} else {
			divX = setupAxisDistance(ctx, xAxis, calcWithoutGaps ? analysis.totalDistanceWithoutGaps : analysis.totalDistance);
		}

		String mainUnitY = useFeet ? ctx.getString(R.string.foot) : ctx.getString(R.string.m);

		YAxis yAxis;
		if (useRightAxis) {
			yAxis = mChart.getAxisRight();
			yAxis.setEnabled(true);
		} else {
			yAxis = mChart.getAxisLeft();
		}
		yAxis.setTextColor(ActivityCompat.getColor(mChart.getContext(), R.color.gpx_chart_blue_label));
		yAxis.setGranularity(1f);
		yAxis.resetAxisMinimum();
		yAxis.setValueFormatter((value, axis) -> OsmAndFormatter.formatInteger((int) (value + 0.5), mainUnitY, ctx));

		List<Entry> values = calculateElevationArray(analysis, axisType, divX, convEle, true, calcWithoutGaps);

		OrderedLineDataSet dataSet = new OrderedLineDataSet(values, "", GPXDataSetType.ALTITUDE,
				axisType, !useRightAxis);
		dataSet.priority = (float) (analysis.avgElevation - analysis.minElevation) * convEle;
		dataSet.divX = divX;
		dataSet.mulY = convEle;
		dataSet.divY = Float.NaN;
		dataSet.units = mainUnitY;

		dataSet.setColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_blue));
		dataSet.setLineWidth(1f);
		if (drawFilled) {
			dataSet.setFillAlpha(128);
			dataSet.setFillColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_blue));
			dataSet.setDrawFilled(true);
		} else {
			dataSet.setDrawFilled(false);
		}

		dataSet.setDrawValues(false);
		dataSet.setValueTextSize(9f);
		dataSet.setFormLineWidth(1f);
		dataSet.setFormSize(15.f);

		dataSet.setDrawCircles(false);
		dataSet.setDrawCircleHole(false);

		dataSet.setHighlightEnabled(true);
		dataSet.setDrawVerticalHighlightIndicator(true);
		dataSet.setDrawHorizontalHighlightIndicator(false);
		dataSet.setHighLightColor(ColorUtilities.getSecondaryTextColor(mChart.getContext(), !light));

		//dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

		dataSet.setFillFormatter((ds, dataProvider) -> dataProvider.getYChartMin());
		if (useRightAxis) {
			dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
		}
		return dataSet;
	}

	public static OrderedLineDataSet createGPXSpeedDataSet(@NonNull OsmandApplication ctx,
	                                                       @NonNull LineChart mChart,
	                                                       @NonNull GPXTrackAnalysis analysis,
	                                                       @NonNull GPXDataSetAxisType axisType,
	                                                       boolean useRightAxis,
	                                                       boolean drawFilled,
	                                                       boolean calcWithoutGaps) {
		OsmandSettings settings = ctx.getSettings();
		boolean light = settings.isLightContent();

		float divX;
		XAxis xAxis = mChart.getXAxis();
		if (axisType == GPXDataSetAxisType.TIME && analysis.isTimeSpecified()) {
			divX = setupXAxisTime(xAxis, calcWithoutGaps ? analysis.timeSpanWithoutGaps : analysis.timeSpan);
		} else if (axisType == GPXDataSetAxisType.TIMEOFDAY && analysis.isTimeSpecified()) {
			divX = setupXAxisTimeOfDay(xAxis, analysis.startTime);
		} else {
			divX = setupAxisDistance(ctx, xAxis, calcWithoutGaps ? analysis.totalDistanceWithoutGaps : analysis.totalDistance);
		}

		SpeedConstants sps = settings.SPEED_SYSTEM.get();
		float mulSpeed = Float.NaN;
		float divSpeed = Float.NaN;
		String mainUnitY = sps.toShortString(ctx);
		if (sps == SpeedConstants.KILOMETERS_PER_HOUR) {
			mulSpeed = 3.6f;
		} else if (sps == SpeedConstants.MILES_PER_HOUR) {
			mulSpeed = 3.6f * METERS_IN_KILOMETER / METERS_IN_ONE_MILE;
		} else if (sps == SpeedConstants.NAUTICALMILES_PER_HOUR) {
			mulSpeed = 3.6f * METERS_IN_KILOMETER / METERS_IN_ONE_NAUTICALMILE;
		} else if (sps == SpeedConstants.MINUTES_PER_KILOMETER) {
			divSpeed = METERS_IN_KILOMETER / 60.0f;
		} else if (sps == SpeedConstants.MINUTES_PER_MILE) {
			divSpeed = METERS_IN_ONE_MILE / 60.0f;
		} else {
			mulSpeed = 1f;
		}

		YAxis yAxis;
		if (useRightAxis) {
			yAxis = mChart.getAxisRight();
			yAxis.setEnabled(true);
		} else {
			yAxis = mChart.getAxisLeft();
		}
		if (analysis.hasSpeedInTrack()) {
			yAxis.setTextColor(ActivityCompat.getColor(mChart.getContext(), R.color.gpx_chart_orange_label));
		} else {
			yAxis.setTextColor(ActivityCompat.getColor(mChart.getContext(), R.color.gpx_chart_red_label));
		}

		yAxis.setAxisMinimum(0f);

		ArrayList<Entry> values = new ArrayList<>();
		List<GPXUtilities.Speed> speedData = analysis.speedData;
		float currentX = 0;

		for (int i = 0; i < speedData.size(); i++) {

			GPXUtilities.Speed s = speedData.get(i);

			float stepX = axisType == GPXDataSetAxisType.TIME || axisType == GPXDataSetAxisType.TIMEOFDAY
					? s.timeDiff
					: s.distance;

			if (i == 0 || stepX > 0) {
				if (!(calcWithoutGaps && s.firstPoint)) {
					currentX += stepX / divX;
				}

				float currentY = Float.isNaN(divSpeed)
						? s.speed * mulSpeed
						: divSpeed / s.speed;
				if (currentY < 0 || Float.isInfinite(currentY)) {
					currentY = 0;
				}

				if (s.firstPoint && currentY != 0) {
					values.add(new Entry(currentX, 0));
				}
				values.add(new Entry(currentX, currentY));
				if (s.lastPoint && currentY != 0) {
					values.add(new Entry(currentX, 0));
				}
			}
		}

		OrderedLineDataSet dataSet = new OrderedLineDataSet(values, "", GPXDataSetType.SPEED,
				axisType, !useRightAxis);

		String format = null;
		if (dataSet.getYMax() < 3) {
			format = "{0,number,0.#} ";
		}
		String formatY = format;
		yAxis.setValueFormatter((value, axis) -> {
			if (!Algorithms.isEmpty(formatY)) {
				return MessageFormat.format(formatY + mainUnitY, value);
			} else {
				return OsmAndFormatter.formatInteger((int) (value + 0.5), mainUnitY, ctx);
			}
		});

		if (Float.isNaN(divSpeed)) {
			dataSet.priority = analysis.avgSpeed * mulSpeed;
		} else {
			dataSet.priority = divSpeed / analysis.avgSpeed;
		}
		dataSet.divX = divX;
		if (Float.isNaN(divSpeed)) {
			dataSet.mulY = mulSpeed;
			dataSet.divY = Float.NaN;
		} else {
			dataSet.divY = divSpeed;
			dataSet.mulY = Float.NaN;
		}
		dataSet.units = mainUnitY;

		if (analysis.hasSpeedInTrack()) {
			dataSet.setColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_orange));
		} else {
			dataSet.setColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_red));
		}
		dataSet.setLineWidth(1f);
		if (drawFilled) {
			dataSet.setFillAlpha(128);
			if (analysis.hasSpeedInTrack()) {
				dataSet.setFillColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_orange));
			} else {
				dataSet.setFillColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_red));
			}
			dataSet.setDrawFilled(true);
		} else {
			dataSet.setDrawFilled(false);
		}
		dataSet.setDrawValues(false);
		dataSet.setValueTextSize(9f);
		dataSet.setFormLineWidth(1f);
		dataSet.setFormSize(15.f);

		dataSet.setDrawCircles(false);
		dataSet.setDrawCircleHole(false);

		dataSet.setHighlightEnabled(true);
		dataSet.setDrawVerticalHighlightIndicator(true);
		dataSet.setDrawHorizontalHighlightIndicator(false);
		dataSet.setHighLightColor(ColorUtilities.getSecondaryTextColor(mChart.getContext(), !light));

		//dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

		if (useRightAxis) {
			dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
		}
		return dataSet;
	}

	public static OrderedLineDataSet createGPXSlopeDataSet(@NonNull OsmandApplication ctx,
	                                                       @NonNull LineChart mChart,
	                                                       @NonNull GPXTrackAnalysis analysis,
	                                                       @NonNull GPXDataSetAxisType axisType,
	                                                       @Nullable List<Entry> eleValues,
	                                                       boolean useRightAxis,
	                                                       boolean drawFilled,
	                                                       boolean calcWithoutGaps) {
		OsmandSettings settings = ctx.getSettings();
		boolean light = settings.isLightContent();
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		boolean useFeet = (mc == MetricsConstants.MILES_AND_FEET) || (mc == MetricsConstants.MILES_AND_YARDS) || (mc == MetricsConstants.NAUTICAL_MILES_AND_FEET);
		float convEle = useFeet ? 3.28084f : 1.0f;
		float totalDistance = calcWithoutGaps ? analysis.totalDistanceWithoutGaps : analysis.totalDistance;

		float divX;
		XAxis xAxis = mChart.getXAxis();
		if (axisType == GPXDataSetAxisType.TIME && analysis.isTimeSpecified()) {
			divX = setupXAxisTime(xAxis, calcWithoutGaps ? analysis.timeSpanWithoutGaps : analysis.timeSpan);
		} else if (axisType == GPXDataSetAxisType.TIMEOFDAY && analysis.isTimeSpecified()) {
			divX = setupXAxisTimeOfDay(xAxis, analysis.startTime);
		} else {
			divX = setupAxisDistance(ctx, xAxis, calcWithoutGaps ? analysis.totalDistanceWithoutGaps : analysis.totalDistance);
		}

		final String mainUnitY = "%";

		YAxis yAxis;
		if (useRightAxis) {
			yAxis = mChart.getAxisRight();
			yAxis.setEnabled(true);
		} else {
			yAxis = mChart.getAxisLeft();
		}
		yAxis.setTextColor(ActivityCompat.getColor(mChart.getContext(), R.color.gpx_chart_green_label));
		yAxis.setGranularity(1f);
		yAxis.resetAxisMinimum();
		yAxis.setValueFormatter((value, axis) -> OsmAndFormatter.formatInteger((int) (value + 0.5), mainUnitY, ctx));

		List<Entry> values;
		if (eleValues == null) {
			values = calculateElevationArray(analysis, GPXDataSetAxisType.DISTANCE, 1f, 1f, false, calcWithoutGaps);
		} else {
			values = new ArrayList<>(eleValues.size());
			for (Entry e : eleValues) {
				values.add(new Entry(e.getX() * divX, e.getY() / convEle));
			}
		}

		if (Algorithms.isEmpty(values)) {
			if (useRightAxis) {
				yAxis.setEnabled(false);
			}
			return null;
		}

		int lastIndex = values.size() - 1;

		double STEP = 5;
		int l = 10;
		while (l > 0 && totalDistance / STEP > MAX_CHART_DATA_ITEMS) {
			STEP = Math.max(STEP, totalDistance / (values.size() * l--));
		}

		double[] calculatedDist = new double[(int) (totalDistance / STEP) + 1];
		double[] calculatedH = new double[(int) (totalDistance / STEP) + 1];
		int nextW = 0;
		for (int k = 0; k < calculatedDist.length; k++) {
			if (k > 0) {
				calculatedDist[k] = calculatedDist[k - 1] + STEP;
			}
			while (nextW < lastIndex && calculatedDist[k] > values.get(nextW).getX()) {
				nextW++;
			}
			double pd = nextW == 0 ? 0 : values.get(nextW - 1).getX();
			double ph = nextW == 0 ? values.get(0).getY() : values.get(nextW - 1).getY();
			calculatedH[k] = ph + (values.get(nextW).getY() - ph) / (values.get(nextW).getX() - pd) * (calculatedDist[k] - pd);
		}

		double SLOPE_PROXIMITY = Math.max(100, STEP * 2);

		if (totalDistance - SLOPE_PROXIMITY < 0) {
			if (useRightAxis) {
				yAxis.setEnabled(false);
			}
			return null;
		}

		double[] calculatedSlopeDist = new double[(int) ((totalDistance - SLOPE_PROXIMITY) / STEP) + 1];
		double[] calculatedSlope = new double[(int) ((totalDistance - SLOPE_PROXIMITY) / STEP) + 1];

		int index = (int) ((SLOPE_PROXIMITY / STEP) / 2);
		for (int k = 0; k < calculatedSlopeDist.length; k++) {
			calculatedSlopeDist[k] = calculatedDist[index + k];
			calculatedSlope[k] = (calculatedH[2 * index + k] - calculatedH[k]) * 100 / SLOPE_PROXIMITY;
			if (Double.isNaN(calculatedSlope[k])) {
				calculatedSlope[k] = 0;
			}
		}

		List<Entry> slopeValues = new ArrayList<>(calculatedSlopeDist.length);
		float prevSlope = -80000;
		float slope;
		float x;
		float lastXSameY = 0;
		boolean hasSameY = false;
		Entry lastEntry = null;
		lastIndex = calculatedSlopeDist.length - 1;
		float timeSpanInSeconds = analysis.timeSpan / 1000f;
		for (int i = 0; i < calculatedSlopeDist.length; i++) {
			if ((axisType == GPXDataSetAxisType.TIMEOFDAY || axisType == GPXDataSetAxisType.TIME) && analysis.isTimeSpecified()) {
				x = (timeSpanInSeconds * i) / calculatedSlopeDist.length;
			} else {
				x = (float) calculatedSlopeDist[i] / divX;
			}
			slope = (float) calculatedSlope[i];
			if (prevSlope != -80000) {
				if (prevSlope == slope && i < lastIndex) {
					hasSameY = true;
					lastXSameY = x;
					continue;
				}
				if (hasSameY) {
					slopeValues.add(new Entry(lastXSameY, lastEntry.getY()));
				}
				hasSameY = false;
			}
			prevSlope = slope;
			lastEntry = new Entry(x, slope);
			slopeValues.add(lastEntry);
		}

		OrderedLineDataSet dataSet = new OrderedLineDataSet(slopeValues, "", GPXDataSetType.SLOPE,
				axisType, !useRightAxis);
		dataSet.divX = divX;
		dataSet.units = mainUnitY;

		dataSet.setColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_green));
		dataSet.setLineWidth(1f);
		if (drawFilled) {
			dataSet.setFillAlpha(128);
			dataSet.setFillColor(ContextCompat.getColor(mChart.getContext(), R.color.gpx_chart_green));
			dataSet.setDrawFilled(true);
		} else {
			dataSet.setDrawFilled(false);
		}

		dataSet.setDrawValues(false);
		dataSet.setValueTextSize(9f);
		dataSet.setFormLineWidth(1f);
		dataSet.setFormSize(15.f);

		dataSet.setDrawCircles(false);
		dataSet.setDrawCircleHole(false);

		dataSet.setHighlightEnabled(true);
		dataSet.setDrawVerticalHighlightIndicator(true);
		dataSet.setDrawHorizontalHighlightIndicator(false);
		dataSet.setHighLightColor(ColorUtilities.getSecondaryTextColor(mChart.getContext(), !light));

		//dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

		/*
		dataSet.setFillFormatter(new IFillFormatter() {
			@Override
			public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
				return dataProvider.getYChartMin();
			}
		});
		*/
		if (useRightAxis) {
			dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
		}
		return dataSet;
	}

	public enum GPXDataSetType {
		ALTITUDE(R.string.altitude, R.drawable.ic_action_altitude_average),
		SPEED(R.string.shared_string_speed, R.drawable.ic_action_speed),
		SLOPE(R.string.shared_string_slope, R.drawable.ic_action_altitude_ascent);

		private final int stringId;
		private final int imageId;

		GPXDataSetType(int stringId, int imageId) {
			this.stringId = stringId;
			this.imageId = imageId;
		}

		public String getName(@NonNull Context ctx) {
			return ctx.getString(stringId);
		}

		public int getStringId() {
			return stringId;
		}

		public int getImageId() {
			return imageId;
		}

		public Drawable getImageDrawable(@NonNull OsmandApplication app) {
			return app.getUIUtilities().getThemedIcon(imageId);
		}

		public static String getName(@NonNull Context ctx, @NonNull GPXDataSetType[] types) {
			List<String> list = new ArrayList<>();
			for (GPXDataSetType type : types) {
				list.add(type.getName(ctx));
			}
			Collections.sort(list);
			StringBuilder sb = new StringBuilder();
			for (String s : list) {
				if (sb.length() > 0) {
					sb.append("/");
				}
				sb.append(s);
			}
			return sb.toString();
		}

		public static Drawable getImageDrawable(@NonNull OsmandApplication app, @NonNull GPXDataSetType[] types) {
			if (types.length > 0) {
				return types[0].getImageDrawable(app);
			} else {
				return null;
			}
		}
	}

	public enum GPXDataSetAxisType {
		DISTANCE(R.string.distance, R.drawable.ic_action_marker_dark),
		TIME(R.string.shared_string_time, R.drawable.ic_action_time),
		TIMEOFDAY(R.string.time_of_day, R.drawable.ic_action_time_span);

		private final int stringId;
		private final int imageId;

		GPXDataSetAxisType(int stringId, int imageId) {
			this.stringId = stringId;
			this.imageId = imageId;
		}

		public String getName(Context ctx) {
			return ctx.getString(stringId);
		}

		public int getStringId() {
			return stringId;
		}

		public int getImageId() {
			return imageId;
		}

		public Drawable getImageDrawable(OsmandApplication app) {
			return app.getUIUtilities().getThemedIcon(imageId);
		}
	}

	public enum LineGraphType {
		ALTITUDE,
		SLOPE,
		SPEED
	}

	public static List<ILineDataSet> getDataSets(LineChart chart,
	                                             OsmandApplication app,
	                                             GPXTrackAnalysis analysis,
	                                             @NonNull LineGraphType firstType,
	                                             @Nullable LineGraphType secondType,
	                                             boolean calcWithoutGaps) {
		if (app == null || chart == null || analysis == null) {
			return new ArrayList<>();
		}
		List<ILineDataSet> result = new ArrayList<>();
		if (secondType == null) {
			ILineDataSet dataSet = getDataSet(chart, app, analysis, calcWithoutGaps, false, firstType);
			if (dataSet != null) {
				result.add(dataSet);
			}
		} else {
			OrderedLineDataSet dataSet1 = getDataSet(chart, app, analysis, calcWithoutGaps, false, firstType);
			OrderedLineDataSet dataSet2 = getDataSet(chart, app, analysis, calcWithoutGaps, true, secondType);
			if (dataSet1 == null && dataSet2 == null) {
				return new ArrayList<>();
			} else if (dataSet1 == null) {
				result.add(dataSet2);
			} else if (dataSet2 == null) {
				result.add(dataSet1);
			} else if (dataSet1.getPriority() < dataSet2.getPriority()) {
				result.add(dataSet2);
				result.add(dataSet1);
			} else {
				result.add(dataSet1);
				result.add(dataSet2);
			}
		}
		return result;
	}

	private static OrderedLineDataSet getDataSet(@NonNull LineChart chart,
	                                             @NonNull OsmandApplication app,
	                                             @NonNull GPXTrackAnalysis analysis,
	                                             boolean calcWithoutGaps,
	                                             boolean useRightAxis,
	                                             @NonNull LineGraphType type) {
		OrderedLineDataSet dataSet = null;
		switch (type) {
			case ALTITUDE: {
				if (analysis.hasElevationData) {
					dataSet = createGPXElevationDataSet(app, chart,
							analysis, GPXDataSetAxisType.DISTANCE, useRightAxis, true, calcWithoutGaps);
				}
				break;
			}
			case SLOPE:
				if (analysis.hasElevationData) {
					dataSet = createGPXSlopeDataSet(app, chart,
							analysis, GPXDataSetAxisType.DISTANCE, null, useRightAxis, true, calcWithoutGaps);
				}
				break;
			case SPEED: {
				if (analysis.hasSpeedData) {
					dataSet = createGPXSpeedDataSet(app, chart,
							analysis, GPXDataSetAxisType.DISTANCE, useRightAxis, true, calcWithoutGaps);
				}
				break;
			}
		}
		return dataSet;
	}
}