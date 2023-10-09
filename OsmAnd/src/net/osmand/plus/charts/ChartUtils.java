package net.osmand.plus.charts;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM;
import static net.osmand.plus.charts.GPXDataSetAxisType.DISTANCE;
import static net.osmand.plus.charts.GPXDataSetAxisType.TIME;
import static net.osmand.plus.charts.GPXDataSetAxisType.TIME_OF_DAY;
import static net.osmand.plus.utils.OsmAndFormatter.FEET_IN_ONE_METER;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_KILOMETER;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_MILE;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE;
import static net.osmand.plus.utils.OsmAndFormatter.YARDS_IN_ONE_METER;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarLineChartBase;
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

import net.osmand.gpx.ElevationDiffsCalculator;
import net.osmand.gpx.ElevationDiffsCalculator.Extremum;
import net.osmand.gpx.GPXInterpolator;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.PointAttributes;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.dialogs.MemoryInfo;
import net.osmand.plus.download.local.dialogs.MemoryInfo.MemoryItem;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.settings.enums.SpeedConstants;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.router.RouteStatisticsHelper.RouteSegmentAttribute;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;
import net.osmand.util.Algorithms;

import java.text.MessageFormat;
import java.util.ArrayList;
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
		int labelsColor = ContextCompat.getColor(context, R.color.text_color_secondary_light);
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

	private static List<Entry> calculateElevationArray(GPXTrackAnalysis analysis,
	                                                   GPXDataSetAxisType axisType,
	                                                   float divX, float convEle,
	                                                   boolean useGeneralTrackPoints,
	                                                   boolean calcWithoutGaps) {
		List<Entry> values = new ArrayList<>();
		float nextX = 0;
		float nextY;
		float elev;
		float prevElevOrig = -80000;
		float prevElev = 0;
		int i = -1;
		int lastIndex = analysis.pointAttributes.size() - 1;
		Entry lastEntry = null;
		float lastXSameY = -1;
		boolean hasSameY = false;
		float x = 0f;
		for (PointAttributes attribute : analysis.pointAttributes) {
			i++;
			if (axisType == TIME || axisType == TIME_OF_DAY) {
				x = attribute.timeDiff;
			} else {
				x = attribute.distance;
			}
			if (x >= 0) {
				if (!(calcWithoutGaps && attribute.firstPoint && lastEntry != null)) {
					nextX += x / divX;
				}
				if (!Float.isNaN(attribute.elevation)) {
					elev = attribute.elevation;
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
					if (useGeneralTrackPoints && attribute.firstPoint && lastEntry != null) {
						values.add(new Entry(nextX, lastEntry.getY()));
					}
					prevElevOrig = attribute.elevation;
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
		chart.setHardwareAccelerationEnabled(true);
		chart.setTouchEnabled(useGesturesAndScale);
		chart.setDragEnabled(useGesturesAndScale);
		chart.setScaleYEnabled(false);
		chart.setAutoScaleMinMaxEnabled(true);
		chart.setDrawBorders(false);
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

	@NonNull
	public static <E> BarData buildStatisticChart(@NonNull OsmandApplication app,
	                                              @NonNull HorizontalBarChart chart,
	                                              @NonNull RouteStatistics routeStatistics,
	                                              @NonNull GPXTrackAnalysis analysis,
	                                              boolean useRightAxis,
	                                              boolean nightMode) {

		XAxis xAxis = chart.getXAxis();
		xAxis.setEnabled(false);

		YAxis yAxis = getYAxis(chart, null, useRightAxis);
		float divX = setupAxisDistance(app, yAxis, analysis.totalDistance);

		List<RouteSegmentAttribute> segments = routeStatistics.elements;
		List<BarEntry> entries = new ArrayList<>();
		float[] stacks = new float[segments.size()];
		int[] colors = new int[segments.size()];
		for (int i = 0; i < stacks.length; i++) {
			RouteSegmentAttribute segment = segments.get(i);
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
		chart.getAxisRight().setAxisMaximum(dataSet.getYMax());
		chart.getAxisLeft().setAxisMaximum(dataSet.getYMax());

		return dataSet;
	}

	@NonNull
	public static BarData buildStatisticChart(@NonNull OsmandApplication app,
	                                          @NonNull HorizontalBarChart chart,
	                                          @NonNull MemoryInfo memoryInfo,
	                                          boolean nightMode) {
		List<MemoryItem> items = memoryInfo.getItems();

		int size = items.size();
		int[] colors = new int[size];
		float[] stacks = new float[size];

		for (int i = 0; i < items.size(); i++) {
			MemoryItem item = items.get(i);
			stacks[i] = item.getValue();
			colors[i] = item.getColor();
		}

		List<BarEntry> entries = new ArrayList<>();
		entries.add(new BarEntry(0, stacks));

		BarDataSet barDataSet = new BarDataSet(entries, "");
		barDataSet.setColors(colors);
		barDataSet.setHighLightColor(ColorUtilities.getSecondaryTextColor(app, nightMode));

		BarData dataSet = new BarData(barDataSet);
		dataSet.setDrawValues(false);
		dataSet.setBarWidth(1);

		chart.getXAxis().setEnabled(false);
		chart.getAxisRight().setAxisMaximum(dataSet.getYMax());
		chart.getAxisLeft().setAxisMaximum(dataSet.getYMax());

		return dataSet;
	}

	public static OrderedLineDataSet createGPXElevationDataSet(@NonNull OsmandApplication app,
	                                                           @NonNull LineChart chart,
	                                                           @NonNull GPXTrackAnalysis analysis,
	                                                           @NonNull GPXDataSetType graphType,
	                                                           @NonNull GPXDataSetAxisType axisType,
	                                                           boolean useRightAxis,
	                                                           boolean drawFilled,
	                                                           boolean calcWithoutGaps) {
		OsmandSettings settings = app.getSettings();
		boolean useFeet = settings.METRIC_SYSTEM.get().shouldUseFeet();
		float convEle = useFeet ? 3.28084f : 1.0f;

		float divX = getDivX(app, chart, analysis, axisType, calcWithoutGaps);

		String mainUnitY = graphType.getMainUnitY(app);

		if (graphType != GPXDataSetType.ALTITUDE_EXTRM) {
			int textColor = ColorUtilities.getColor(app, graphType.getTextColorId(false));
			YAxis yAxis = getYAxis(chart, textColor, useRightAxis);
			yAxis.setGranularity(1f);
			yAxis.resetAxisMinimum();
			yAxis.setValueFormatter((value, axis) -> OsmAndFormatter.formatInteger((int) (value + 0.5), mainUnitY, app));
		}

		List<Entry> values = calculateElevationArray(analysis, axisType, divX, convEle, true, calcWithoutGaps);
		if (values.size() > 0 && graphType == GPXDataSetType.ALTITUDE_EXTRM) {
			List<Entry> elevationEntries = values;
			ElevationDiffsCalculator elevationDiffsCalc = new ElevationDiffsCalculator() {
				@Override
				public double getPointDistance(int index) {
					return elevationEntries.get(index).getX() * divX;
				}

				@Override
				public double getPointElevation(int index) {
					return elevationEntries.get(index).getY();
				}

				@Override
				public int getPointsCount() {
					return elevationEntries.size();
				}
			};
			elevationDiffsCalc.calculateElevationDiffs();
			List<Extremum> extremums = elevationDiffsCalc.getExtremums();
			if (extremums.size() < 3) {
				return null;
			}
			values = new ArrayList<>();
			for (Extremum extremum : extremums) {
				values.add(new Entry((float) (extremum.getDist() / divX), (float) extremum.getEle()));
			}
		}

		OrderedLineDataSet dataSet = new OrderedLineDataSet(values, "", graphType, axisType, !useRightAxis);
		dataSet.setPriority((float) ((analysis.avgElevation - analysis.minElevation) * convEle));
		dataSet.setDivX(divX);
		dataSet.setUnits(mainUnitY);

		boolean nightMode = !settings.isLightContent();
		int color = ColorUtilities.getColor(app, graphType.getFillColorId(false));
		setupDataSet(app, dataSet, color, color, drawFilled, graphType == GPXDataSetType.ALTITUDE_EXTRM, useRightAxis, nightMode);
		dataSet.setFillFormatter((ds, dataProvider) -> dataProvider.getYChartMin());

		return dataSet;
	}

	public static void setupDataSet(OsmandApplication app, OrderedLineDataSet dataSet,
	                                @ColorInt int color, @ColorInt int fillColor, boolean drawFilled,
	                                boolean drawCircles, boolean useRightAxis, boolean nightMode) {
		if (drawCircles) {
			dataSet.setCircleColor(color);
			dataSet.setCircleRadius(3);
			dataSet.setCircleHoleColor(0);
			dataSet.setCircleHoleRadius(2);
			dataSet.setDrawCircleHole(false);
			dataSet.setDrawCircles(true);
			dataSet.setColor(0);
		} else {
			dataSet.setDrawCircles(false);
			dataSet.setDrawCircleHole(false);
			dataSet.setColor(color);
		}

		dataSet.setLineWidth(1f);
		if (drawFilled && !drawCircles) {
			dataSet.setFillAlpha(128);
			dataSet.setFillColor(fillColor);
		}
		dataSet.setDrawFilled(drawFilled && !drawCircles);

		dataSet.setDrawValues(false);
		if (drawCircles) {
			dataSet.setHighlightEnabled(false);
			dataSet.setDrawVerticalHighlightIndicator(false);
			dataSet.setDrawHorizontalHighlightIndicator(false);
		} else {
			dataSet.setValueTextSize(9f);
			dataSet.setFormLineWidth(1f);
			dataSet.setFormSize(15.f);

			dataSet.setHighlightEnabled(true);
			dataSet.setDrawVerticalHighlightIndicator(true);
			dataSet.setDrawHorizontalHighlightIndicator(false);
			dataSet.setHighLightColor(ColorUtilities.getSecondaryTextColor(app, nightMode));
		}
		if (useRightAxis) {
			dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
		}
	}

	public static OrderedLineDataSet createGPXSpeedDataSet(@NonNull OsmandApplication app,
	                                                       @NonNull LineChart chart,
	                                                       @NonNull GPXTrackAnalysis analysis,
	                                                       @NonNull GPXDataSetType graphType,
	                                                       @NonNull GPXDataSetAxisType axisType,
	                                                       boolean useRightAxis,
	                                                       boolean drawFilled,
	                                                       boolean calcWithoutGaps) {
		OsmandSettings settings = app.getSettings();
		boolean nightMode = !settings.isLightContent();

		float divX = getDivX(app, chart, analysis, axisType, calcWithoutGaps);

		Pair<Float, Float> pair = ChartUtils.getScalingY(app, graphType);
		float mulSpeed = pair != null ? pair.first : Float.NaN;
		float divSpeed = pair != null ? pair.second : Float.NaN;

		boolean speedInTrack = analysis.hasSpeedInTrack();
		int textColor = ColorUtilities.getColor(app, graphType.getTextColorId(!speedInTrack));
		YAxis yAxis = getYAxis(chart, textColor, useRightAxis);
		yAxis.setAxisMinimum(0f);

		List<Entry> values = getPointAttributeValues(graphType.getDataKey(), analysis, axisType, divX, mulSpeed, divSpeed, calcWithoutGaps);
		OrderedLineDataSet dataSet = new OrderedLineDataSet(values, "", graphType, axisType, !useRightAxis);

		String mainUnitY = graphType.getMainUnitY(app);
		String formatY = dataSet.getYMax() < 3 ? "{0,number,0.#} " : null;
		yAxis.setValueFormatter((value, axis) -> {
			if (!Algorithms.isEmpty(formatY)) {
				return MessageFormat.format(formatY + mainUnitY, value);
			} else {
				return OsmAndFormatter.formatInteger((int) (value + 0.5), mainUnitY, app);
			}
		});

		if (Float.isNaN(divSpeed)) {
			dataSet.setPriority(analysis.avgSpeed * mulSpeed);
		} else {
			dataSet.setPriority(divSpeed / analysis.avgSpeed);
		}
		dataSet.setDivX(divX);
		dataSet.setUnits(mainUnitY);

		int color = ColorUtilities.getColor(app, graphType.getFillColorId(!speedInTrack));
		setupDataSet(app, dataSet, color, color, drawFilled, false, useRightAxis, nightMode);

		return dataSet;
	}

	public static float getDivX(@NonNull OsmandApplication app, @NonNull LineChart lineChart,
	                            @NonNull GPXTrackAnalysis analysis, @NonNull GPXDataSetAxisType axisType,
	                            boolean calcWithoutGaps) {
		XAxis xAxis = lineChart.getXAxis();
		if (axisType == TIME && analysis.isTimeSpecified()) {
			return setupXAxisTime(xAxis, calcWithoutGaps ? analysis.timeSpanWithoutGaps : analysis.timeSpan);
		} else if (axisType == TIME_OF_DAY && analysis.isTimeSpecified()) {
			return setupXAxisTimeOfDay(xAxis, analysis.startTime);
		} else {
			return setupAxisDistance(app, xAxis, calcWithoutGaps ? analysis.totalDistanceWithoutGaps : analysis.totalDistance);
		}
	}

	@Nullable
	public static Pair<Float, Float> getScalingY(@NonNull OsmandApplication app, @NonNull GPXDataSetType graphType) {
		if (graphType == GPXDataSetType.SPEED || graphType == GPXDataSetType.SENSOR_SPEED) {
			float mulSpeed = Float.NaN;
			float divSpeed = Float.NaN;
			SpeedConstants speedConstants = app.getSettings().SPEED_SYSTEM.get();
			if (speedConstants == SpeedConstants.KILOMETERS_PER_HOUR) {
				mulSpeed = 3.6f;
			} else if (speedConstants == SpeedConstants.MILES_PER_HOUR) {
				mulSpeed = 3.6f * METERS_IN_KILOMETER / METERS_IN_ONE_MILE;
			} else if (speedConstants == SpeedConstants.NAUTICALMILES_PER_HOUR) {
				mulSpeed = 3.6f * METERS_IN_KILOMETER / METERS_IN_ONE_NAUTICALMILE;
			} else if (speedConstants == SpeedConstants.MINUTES_PER_KILOMETER) {
				divSpeed = METERS_IN_KILOMETER / 60.0f;
			} else if (speedConstants == SpeedConstants.MINUTES_PER_MILE) {
				divSpeed = METERS_IN_ONE_MILE / 60.0f;
			} else {
				mulSpeed = 1f;
			}
			return new Pair<>(mulSpeed, divSpeed);
		}
		return null;
	}

	@NonNull
	public static List<Entry> getPointAttributeValues(@NonNull String key,
	                                                  @NonNull GPXTrackAnalysis analysis,
	                                                  @NonNull GPXDataSetAxisType axisType,
	                                                  float divX, float mulY, float divY,
	                                                  boolean calcWithoutGaps) {
		List<Entry> values = new ArrayList<>();
		float currentX = 0;

		for (int i = 0; i < analysis.pointAttributes.size(); i++) {
			PointAttributes attribute = analysis.pointAttributes.get(i);

			float stepX = axisType == TIME || axisType == TIME_OF_DAY ? attribute.timeDiff : attribute.distance;

			if (i == 0 || stepX > 0) {
				if (!(calcWithoutGaps && attribute.firstPoint)) {
					currentX += stepX / divX;
				}
				if (attribute.hasValidValue(key)) {
					float value = attribute.getAttributeValue(key);
					float currentY = Float.isNaN(divY) ? value * mulY : divY / value;
					if (currentY < 0 || Float.isInfinite(currentY)) {
						currentY = 0;
					}
					if (attribute.firstPoint && currentY != 0) {
						values.add(new Entry(currentX, 0));
					}
					values.add(new Entry(currentX, currentY));
					if (attribute.lastPoint && currentY != 0) {
						values.add(new Entry(currentX, 0));
					}
				}
			}
		}
		return values;
	}

	public static YAxis getYAxis(BarLineChartBase<?> chart, Integer textColor, boolean useRightAxis) {
		YAxis yAxis = useRightAxis ? chart.getAxisRight() : chart.getAxisLeft();
		yAxis.setEnabled(true);
		if (textColor != null) {
			yAxis.setTextColor(textColor);
		}
		return yAxis;
	}

	public static OrderedLineDataSet createGPXSlopeDataSet(@NonNull OsmandApplication app,
	                                                       @NonNull LineChart chart,
	                                                       @NonNull GPXTrackAnalysis analysis,
	                                                       @NonNull GPXDataSetType graphType,
	                                                       @NonNull GPXDataSetAxisType axisType,
	                                                       @Nullable List<Entry> eleValues,
	                                                       boolean useRightAxis,
	                                                       boolean drawFilled,
	                                                       boolean calcWithoutGaps) {
		OsmandSettings settings = app.getSettings();
		boolean nightMode = !settings.isLightContent();
		MetricsConstants mc = settings.METRIC_SYSTEM.get();
		boolean useFeet = (mc == MetricsConstants.MILES_AND_FEET) || (mc == MetricsConstants.MILES_AND_YARDS) || (mc == MetricsConstants.NAUTICAL_MILES_AND_FEET);
		float convEle = useFeet ? 3.28084f : 1.0f;
		float totalDistance = calcWithoutGaps ? analysis.totalDistanceWithoutGaps : analysis.totalDistance;

		float divX = getDivX(app, chart, analysis, axisType, calcWithoutGaps);

		String mainUnitY = graphType.getMainUnitY(app);

		int textColor = ColorUtilities.getColor(app, graphType.getTextColorId(false));
		YAxis yAxis = getYAxis(chart, textColor, useRightAxis);
		yAxis.setGranularity(1f);
		yAxis.resetAxisMinimum();
		yAxis.setValueFormatter((value, axis) -> OsmAndFormatter.formatInteger((int) (value + 0.5), mainUnitY, app));

		List<Entry> values;
		if (eleValues == null) {
			values = calculateElevationArray(analysis, DISTANCE, 1f, 1f, false, calcWithoutGaps);
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

		double STEP = 5;
		int l = 10;
		while (l > 0 && totalDistance / STEP > MAX_CHART_DATA_ITEMS) {
			STEP = Math.max(STEP, totalDistance / (values.size() * l--));
		}
		GPXInterpolator interpolator = new GPXInterpolator(values.size(), totalDistance, STEP) {
			@Override
			public double getX(int index) {
				return values.get(index).getX();
			}

			@Override
			public double getY(int index) {
				return values.get(index).getY();
			}
		};
		interpolator.interpolate();

		double[] calculatedDist = interpolator.getCalculatedX();
		double[] calculatedH = interpolator.getCalculatedY();
		if (calculatedDist == null || calculatedH == null) {
			return null;
		}

		double SLOPE_PROXIMITY = Math.max(20, STEP * 2);

		if (totalDistance - SLOPE_PROXIMITY < 0) {
			if (useRightAxis) {
				yAxis.setEnabled(false);
			}
			return null;
		}

		double[] calculatedSlopeDist = new double[(int) (totalDistance / STEP) + 1];
		double[] calculatedSlope = new double[(int) (totalDistance / STEP) + 1];

		int threshold = Math.max(2, (int) ((SLOPE_PROXIMITY / STEP) / 2));
		if (calculatedSlopeDist.length <= 4) {
			return null;
		}
		for (int k = 0; k < calculatedSlopeDist.length; k++) {
			calculatedSlopeDist[k] = calculatedDist[k];
			if (k < threshold) {
				calculatedSlope[k] = (-1.5 * calculatedH[k] + 2.0 * calculatedH[k + 1] - 0.5 * calculatedH[k + 2]) * 100 / STEP;
			} else if (k >= calculatedSlopeDist.length - threshold) {
				calculatedSlope[k] = (0.5 * calculatedH[k - 2] - 2.0 * calculatedH[k - 1] + 1.5 * calculatedH[k]) * 100 / STEP;
			} else {
				calculatedSlope[k] = (calculatedH[threshold + k] - calculatedH[k - threshold]) * 100 / SLOPE_PROXIMITY;
			}
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
		int lastIndex = calculatedSlopeDist.length - 1;
		float timeSpanInSeconds = analysis.timeSpan / 1000f;
		for (int i = 0; i < calculatedSlopeDist.length; i++) {
			if ((axisType == TIME_OF_DAY || axisType == TIME) && analysis.isTimeSpecified()) {
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

		OrderedLineDataSet dataSet = new OrderedLineDataSet(slopeValues, "", GPXDataSetType.SLOPE, axisType, !useRightAxis);
		dataSet.setDivX(divX);
		dataSet.setUnits(mainUnitY);

		int color = ColorUtilities.getColor(app, graphType.getFillColorId(false));
		setupDataSet(app, dataSet, color, color, drawFilled, false, useRightAxis, nightMode);

		/*
		dataSet.setFillFormatter(new IFillFormatter() {
			@Override
			public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
				return dataProvider.getYChartMin();
			}
		});
		*/

		return dataSet;
	}

	public static List<ILineDataSet> getDataSets(LineChart chart,
	                                             OsmandApplication app,
	                                             GPXTrackAnalysis analysis,
	                                             @NonNull GPXDataSetType firstType,
	                                             @Nullable GPXDataSetType secondType,
	                                             boolean calcWithoutGaps) {
		if (app == null || chart == null || analysis == null) {
			return new ArrayList<>();
		}
		List<ILineDataSet> result = new ArrayList<>();
		if (secondType == null) {
			ILineDataSet dataSet = getDataSet(app, chart, analysis, firstType, calcWithoutGaps, false);
			if (dataSet != null) {
				result.add(dataSet);
			}
		} else {
			OrderedLineDataSet dataSet1 = getDataSet(app, chart, analysis, firstType, calcWithoutGaps, false);
			OrderedLineDataSet dataSet2 = getDataSet(app, chart, analysis, secondType, calcWithoutGaps, true);
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
		/* Do not show extremums because of too heavy approximation
		if ((firstType == GPXDataSetType.ALTITUDE || secondType == GPXDataSetType.ALTITUDE)
				&& PluginsHelper.isActive(OsmandDevelopmentPlugin.class)) {
			OrderedLineDataSet dataSet = getDataSet(app, chart, analysis, GPXDataSetType.ALTITUDE_EXTRM, calcWithoutGaps, false);
			if (dataSet != null) {
				result.add(dataSet);
			}
		}
		*/
		return result;
	}

	@Nullable
	public static OrderedLineDataSet getDataSet(@NonNull OsmandApplication app,
	                                            @NonNull LineChart chart,
	                                            @NonNull GPXTrackAnalysis analysis,
	                                            @NonNull GPXDataSetType graphType,
	                                            boolean calcWithoutGaps,
	                                            boolean useRightAxis) {
		switch (graphType) {
			case ALTITUDE:
			case ALTITUDE_EXTRM: {
				if (analysis.hasElevationData()) {
					return createGPXElevationDataSet(app, chart, analysis, graphType, DISTANCE, useRightAxis, true, calcWithoutGaps);
				}
			}
			case SLOPE: {
				if (analysis.hasElevationData()) {
					return createGPXSlopeDataSet(app, chart, analysis, graphType, DISTANCE, null, useRightAxis, true, calcWithoutGaps);
				}
			}
			case SPEED: {
				if (analysis.hasSpeedData()) {
					return createGPXSpeedDataSet(app, chart, analysis, graphType, DISTANCE, useRightAxis, true, calcWithoutGaps);
				}
			}
			default: {
				return PluginsHelper.getOrderedLineDataSet(chart, analysis, graphType, DISTANCE, calcWithoutGaps, useRightAxis);
			}
		}
	}
}