package net.osmand.plus.charts;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static com.github.mikephil.charting.charts.ElevationChart.GRID_LINE_LENGTH_X_AXIS_DP;
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
import com.github.mikephil.charting.charts.ElevationChart;
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
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;

import net.osmand.gpx.ElevationDiffsCalculator;
import net.osmand.gpx.ElevationDiffsCalculator.Extremum;
import net.osmand.gpx.GPXInterpolator;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.dialogs.MemoryInfo;
import net.osmand.plus.download.local.dialogs.MemoryInfo.MemoryItem;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.router.RouteStatisticsHelper.RouteSegmentAttribute;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;
import net.osmand.shared.ColorPalette;
import net.osmand.shared.ColorPalette.ColorValue;
import net.osmand.shared.settings.enums.SpeedConstants;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.PointAttributes;
import net.osmand.util.Algorithms;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class ChartUtils {

	public static final int CHART_LABEL_COUNT = 3;
	private static final int MAX_CHART_DATA_ITEMS = 10000;

	public static void setupElevationChart(ElevationChart chart) {
		setupElevationChart(chart, 24f, 16f, true);
	}

	public static void setupElevationChart(@NonNull ElevationChart chart, float topOffset, float bottomOffset,
	                                       boolean useGesturesAndScale) {
		setupElevationChart(chart, topOffset, bottomOffset, useGesturesAndScale, null);
	}

	public static void setupElevationChart(@NonNull ElevationChart chart, float topOffset, float bottomOffset,
	                                       boolean useGesturesAndScale, @Nullable Drawable markerIcon) {
		GpxMarkerView markerView = new GpxMarkerView(chart.getContext(), markerIcon);
		setupElevationChart(chart, markerView, topOffset, bottomOffset, useGesturesAndScale);
	}

	public static void setupElevationChart(@NonNull ElevationChart chart, @NonNull GpxMarkerView markerView, float topOffset, float bottomOffset, boolean useGesturesAndScale) {
		Context context = chart.getContext();
		int labelsColor = ContextCompat.getColor(context, R.color.text_color_secondary_light);
		int yAxisGridColor = AndroidUtils.getColorFromAttr(context, R.attr.chart_y_grid_line_axis_color);
		int xAxisGridColor = AndroidUtils.getColorFromAttr(context, R.attr.chart_x_grid_line_axis_color);
		chart.setupGPXChart(markerView, topOffset, bottomOffset, xAxisGridColor, labelsColor,
				yAxisGridColor, FontCache.getMediumFont(), useGesturesAndScale);
	}

	private static float setupAxisDistance(OsmandApplication ctx, AxisBase axisBase, double meters) {
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
				boolean shouldShowUnit = axis.mEntries.length >= 1 && axis.mEntries[0] == value;
				return MessageFormat.format(shouldShowUnit ? formatX + mainUnitX : formatX, value);
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

	private static List<Entry> calculateElevationArray(GpxTrackAnalysis analysis,
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
		int lastIndex = analysis.getPointAttributes().size() - 1;
		Entry lastEntry = null;
		float lastXSameY = -1;
		boolean hasSameY = false;
		float x = 0f;
		for (PointAttributes attribute : analysis.getPointAttributes()) {
			i++;
			if (axisType == TIME || axisType == TIME_OF_DAY) {
				x = attribute.getTimeDiff();
			} else {
				x = attribute.getDistance();
			}
			if (x >= 0) {
				if (!(calcWithoutGaps && attribute.getFirstPoint() && lastEntry != null)) {
					nextX += x / divX;
				}
				if (!Float.isNaN(attribute.getElevation())) {
					elev = attribute.getElevation();
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
					if (useGeneralTrackPoints && attribute.getFirstPoint() && lastEntry != null) {
						values.add(new Entry(nextX, lastEntry.getY()));
					}
					prevElevOrig = attribute.getElevation();
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
	                                              @NonNull GpxTrackAnalysis analysis,
	                                              boolean useRightAxis,
	                                              boolean nightMode) {

		XAxis xAxis = chart.getXAxis();
		xAxis.setEnabled(false);

		YAxis yAxis = getAndEnableYAxis(chart, null, useRightAxis);
		float divX = setupAxisDistance(app, yAxis, analysis.getTotalDistance());

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

	public static void setupGradientChart(OsmandApplication app, LineChart chart, float topOffset, float bottomOffset,
	                                      boolean useGesturesAndScale, int xAxisGridColor, int labelsColor) {
		chart.setExtraRightOffset(16.0F);
		chart.setExtraLeftOffset(16.0F);
		chart.setExtraTopOffset(topOffset);
		chart.setExtraBottomOffset(bottomOffset);

		chart.setHardwareAccelerationEnabled(true);
		chart.setTouchEnabled(useGesturesAndScale);
		chart.setDragEnabled(useGesturesAndScale);
		chart.setScaleEnabled(useGesturesAndScale);
		chart.setPinchZoom(useGesturesAndScale);
		chart.setScaleYEnabled(false);
		chart.setAutoScaleMinMaxEnabled(true);
		chart.setDrawBorders(false);
		chart.getDescription().setEnabled(false);
		chart.setMaxVisibleValueCount(10);
		chart.setMinOffset(0.0F);
		chart.setDragDecelerationEnabled(false);

		XAxis xAxis = chart.getXAxis();
		xAxis.setDrawAxisLine(true);
		xAxis.setAxisLineWidth(1.0F);
		xAxis.setAxisLineColor(xAxisGridColor);
		xAxis.setDrawGridLines(true);
		xAxis.setGridLineWidth(1.0F);
		xAxis.setGridColor(xAxisGridColor);
		xAxis.enableGridDashedLine(Utils.dpToPx(app, GRID_LINE_LENGTH_X_AXIS_DP), Float.MAX_VALUE, 0.0F);
		xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
		xAxis.setTextColor(labelsColor);
		xAxis.setAvoidFirstLastClipping(true);
		xAxis.setEnabled(true);

		YAxis leftYAxis = chart.getAxisLeft();
		leftYAxis.setEnabled(false);

		YAxis rightYAxis = chart.getAxisRight();
		rightYAxis.setEnabled(false);

		Legend legend = chart.getLegend();
		legend.setEnabled(false);
	}

	@NonNull
	public static <E> LineData buildGradientChart(@NonNull OsmandApplication app,
	                                              @NonNull LineChart chart,
	                                              @NonNull ColorPalette colorPalette,
	                                              @Nullable IAxisValueFormatter valueFormatter,
	                                              boolean nightMode) {

		XAxis xAxis = chart.getXAxis();
		xAxis.setEnabled(false);

		List<ColorValue> colorValues = colorPalette.getColors();
		int[] colors = new int[colorValues.size()];
		List<Entry> entries = new ArrayList<>();

		for (int i = 0; i < colorValues.size(); i++) {
			int clr = colorValues.get(i).getClr();
			colors[i] = clr;
			entries.add(new Entry((float) colorValues.get(i).getValue(), 0));
		}

		LineDataSet barDataSet = new LineDataSet(entries, "");
		barDataSet.setColors(colors);
		barDataSet.setHighLightColor(ColorUtilities.getSecondaryTextColor(app, nightMode));
		LineData dataSet = new LineData(barDataSet);
		dataSet.setDrawValues(false);
		chart.getXAxis().setValueFormatter(valueFormatter);

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
	                                                           @NonNull GpxTrackAnalysis analysis,
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
		dataSet.setPriority((float) ((analysis.getAvgElevation() - analysis.getMinElevation()) * convEle));
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
	                                                       @NonNull GpxTrackAnalysis analysis,
	                                                       @NonNull GPXDataSetType graphType,
	                                                       @NonNull GPXDataSetAxisType axisType,
	                                                       boolean useRightAxis,
	                                                       boolean setYAxisMinimum,
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
		if (setYAxisMinimum) {
			yAxis.setAxisMinimum(0f);
		} else {
			yAxis.resetAxisMinimum();
		}

		List<Entry> values = getPointAttributeValues(graphType.getDataKey(), analysis.getPointAttributes(), axisType, divX, mulSpeed, divSpeed, calcWithoutGaps);
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
			dataSet.setPriority(analysis.getAvgSpeed() * mulSpeed);
		} else {
			dataSet.setPriority(divSpeed / analysis.getAvgSpeed());
		}
		dataSet.setDivX(divX);
		dataSet.setUnits(mainUnitY);

		int color = ColorUtilities.getColor(app, graphType.getFillColorId(!speedInTrack));
		setupDataSet(app, dataSet, color, color, drawFilled, false, useRightAxis, nightMode);

		return dataSet;
	}

	public static float getDivX(@NonNull OsmandApplication app, @NonNull LineChart lineChart,
	                            @NonNull GpxTrackAnalysis analysis, @NonNull GPXDataSetAxisType axisType,
	                            boolean calcWithoutGaps) {
		XAxis xAxis = lineChart.getXAxis();
		if (axisType == TIME && analysis.isTimeSpecified()) {
			return setupXAxisTime(xAxis, calcWithoutGaps ? analysis.getTimeSpanWithoutGaps() : analysis.getTimeSpan());
		} else if (axisType == TIME_OF_DAY && analysis.isTimeSpecified()) {
			return setupXAxisTimeOfDay(xAxis, analysis.getStartTime());
		} else {
			return setupAxisDistance(app, xAxis, calcWithoutGaps ? analysis.getTotalDistanceWithoutGaps() : analysis.getTotalDistance());
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
	                                                  @NonNull List<PointAttributes> pointAttributes,
	                                                  @NonNull GPXDataSetAxisType axisType,
	                                                  float divX, float mulY, float divY,
	                                                  boolean calcWithoutGaps) {
		List<Entry> values = new ArrayList<>();
		float currentX = 0;

		for (int i = 0; i < pointAttributes.size(); i++) {
			PointAttributes attribute = pointAttributes.get(i);

			float stepX = axisType == TIME || axisType == TIME_OF_DAY ? attribute.getTimeDiff() : attribute.getDistance();

			if (i == 0 || stepX > 0) {
				if (!(calcWithoutGaps && attribute.getFirstPoint())) {
					currentX += stepX / divX;
				}
				if (attribute.hasValidValue(key)) {
					float value = attribute.getAttributeValue(key);
					float currentY = Float.isNaN(divY) ? value * mulY : divY / value;
					if (currentY < 0 || Float.isInfinite(currentY)) {
						currentY = 0;
					}
					if (attribute.getFirstPoint() && currentY != 0) {
						values.add(new Entry(currentX, 0));
					}
					values.add(new Entry(currentX, currentY));
					if (attribute.getLastPoint() && currentY != 0) {
						values.add(new Entry(currentX, 0));
					}
				}
			}
		}
		return values;
	}


	public static YAxis getAndEnableYAxis(BarLineChartBase<?> chart, Integer textColor, boolean useRightAxis) {
		YAxis yAxis = getYAxis(chart, textColor, useRightAxis);
		yAxis.setEnabled(true);
		return yAxis;
	}

	public static YAxis getYAxis(BarLineChartBase<?> chart, Integer textColor, boolean useRightAxis) {
		YAxis yAxis = useRightAxis ? chart.getAxisRight() : chart.getAxisLeft();
		if (textColor != null) {
			yAxis.setTextColor(textColor);
		}
		return yAxis;
	}

	public static OrderedLineDataSet createGPXSlopeDataSet(@NonNull OsmandApplication app,
	                                                       @NonNull LineChart chart,
	                                                       @NonNull GpxTrackAnalysis analysis,
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
		double totalDistance = calcWithoutGaps ? analysis.getTotalDistanceWithoutGaps() : analysis.getTotalDistance();

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
		float timeSpanInSeconds = analysis.getTimeSpan() / 1000f;
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
	                                             GpxTrackAnalysis analysis,
	                                             @NonNull GPXDataSetType firstType,
	                                             @Nullable GPXDataSetType secondType,
	                                             boolean calcWithoutGaps) {
		return getDataSets(chart, app, analysis, firstType, secondType, DISTANCE, calcWithoutGaps);
	}

	public static List<ILineDataSet> getDataSets(LineChart chart,
	                                             OsmandApplication app,
	                                             GpxTrackAnalysis analysis,
	                                             @NonNull GPXDataSetType firstType,
	                                             @Nullable GPXDataSetType secondType,
	                                             GPXDataSetAxisType gpxDataSetAxisType,
	                                             boolean calcWithoutGaps) {
		if (app == null || chart == null || analysis == null) {
			return new ArrayList<>();
		}
		List<ILineDataSet> result = new ArrayList<>();
		if (secondType == null) {
			ILineDataSet dataSet = getDataSet(app, chart, analysis, firstType, null, gpxDataSetAxisType, calcWithoutGaps, false);
			if (dataSet != null) {
				result.add(dataSet);
			}
		} else {
			OrderedLineDataSet dataSet1 = getDataSet(app, chart, analysis, firstType, secondType, gpxDataSetAxisType, calcWithoutGaps, false);
			OrderedLineDataSet dataSet2 = getDataSet(app, chart, analysis, secondType, firstType, gpxDataSetAxisType, calcWithoutGaps, true);
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
	                                            @NonNull GpxTrackAnalysis analysis,
	                                            @NonNull GPXDataSetType graphType,
	                                            @Nullable GPXDataSetType otherGraphType,
	                                            GPXDataSetAxisType gpxDataSetAxisType,
	                                            boolean calcWithoutGaps,
	                                            boolean useRightAxis) {
		switch (graphType) {
			case ALTITUDE:
			case ALTITUDE_EXTRM: {
				if (analysis.hasElevationData()) {
					return createGPXElevationDataSet(app, chart, analysis, graphType, gpxDataSetAxisType, useRightAxis, true, calcWithoutGaps);
				}
			}
			case SLOPE: {
				if (analysis.hasElevationData()) {
					return createGPXSlopeDataSet(app, chart, analysis, graphType, gpxDataSetAxisType, null, useRightAxis, true, calcWithoutGaps);
				}
			}
			case SPEED: {
				if (analysis.hasSpeedData()) {
					boolean setYAxisMinimum = otherGraphType != GPXDataSetType.ZOOM_ANIMATED
							&& otherGraphType != GPXDataSetType.ZOOM_NON_ANIMATED;
					return createGPXSpeedDataSet(app, chart, analysis, graphType, gpxDataSetAxisType, useRightAxis, setYAxisMinimum, true, calcWithoutGaps);
				}
			}
			default: {
				return PluginsHelper.getOrderedLineDataSet(chart, analysis, graphType, gpxDataSetAxisType, calcWithoutGaps, useRightAxis);
			}
		}
	}
}