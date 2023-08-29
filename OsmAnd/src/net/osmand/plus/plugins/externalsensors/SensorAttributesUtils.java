package net.osmand.plus.plugins.externalsensors;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.gpx.PointAttribute;
import net.osmand.gpx.PointsAttributesData;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.charts.ChartUtils;
import net.osmand.plus.charts.GPXDataSetAxisType;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.charts.OrderedLineDataSet;
import net.osmand.plus.plugins.externalsensors.pointAttributes.BikeCadence;
import net.osmand.plus.plugins.externalsensors.pointAttributes.BikePower;
import net.osmand.plus.plugins.externalsensors.pointAttributes.HeartRate;
import net.osmand.plus.plugins.externalsensors.pointAttributes.SensorSpeed;
import net.osmand.plus.plugins.externalsensors.pointAttributes.Temperature;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;

import java.text.MessageFormat;
import java.util.List;

public class SensorAttributesUtils {

	public static final String SENSOR_TAG_HEART_RATE = "hr";
	public static final String SENSOR_TAG_SPEED = "osmand:speed_sensor";
	public static final String SENSOR_TAG_CADENCE = "cadence";
	public static final String SENSOR_TAG_BIKE_POWER = "power";
	public static final String SENSOR_TAG_TEMPERATURE = "temp";
	public static final String SENSOR_TAG_DISTANCE = "osmand:bike_distance_sensor";

	private static final String[] SENSOR_GPX_TAGS = {
			SENSOR_TAG_HEART_RATE,
			SENSOR_TAG_SPEED,
			SENSOR_TAG_CADENCE,
			SENSOR_TAG_BIKE_POWER,
			SENSOR_TAG_TEMPERATURE
	};

	public static boolean hasHeartRateData(@NonNull GPXTrackAnalysis analysis) {
		return getHeartRateData(analysis).hasData();
	}

	public static boolean hasSensorSpeedData(@NonNull GPXTrackAnalysis analysis) {
		return getSensorSpeedData(analysis).hasData();
	}

	public static boolean hasBikeCadenceData(@NonNull GPXTrackAnalysis analysis) {
		return getBikeCadenceData(analysis).hasData();
	}

	public static boolean hasBikePowerData(@NonNull GPXTrackAnalysis analysis) {
		return getBikePowerData(analysis).hasData();
	}

	public static boolean hasTemperatureData(@NonNull GPXTrackAnalysis analysis) {
		return getTemperatureData(analysis).hasData();
	}

	@NonNull
	public static PointsAttributesData<HeartRate> getHeartRateData(@NonNull GPXTrackAnalysis analysis) {
		return analysis.getAttributesData(SENSOR_TAG_HEART_RATE);
	}

	@NonNull
	public static PointsAttributesData<SensorSpeed> getSensorSpeedData(@NonNull GPXTrackAnalysis analysis) {
		return analysis.getAttributesData(SENSOR_TAG_SPEED);
	}

	@NonNull
	public static PointsAttributesData<BikeCadence> getBikeCadenceData(@NonNull GPXTrackAnalysis analysis) {
		return analysis.getAttributesData(SENSOR_TAG_CADENCE);
	}

	@NonNull
	public static PointsAttributesData<BikePower> getBikePowerData(@NonNull GPXTrackAnalysis analysis) {
		return analysis.getAttributesData(SENSOR_TAG_BIKE_POWER);
	}

	@NonNull
	public static PointsAttributesData<Temperature> getTemperatureData(@NonNull GPXTrackAnalysis analysis) {
		return analysis.getAttributesData(SENSOR_TAG_TEMPERATURE);
	}

	public static float getPointAttribute(@NonNull WptPt wptPt, @NonNull String key, float defaultValue) {
		return Algorithms.parseFloatSilently(wptPt.getExtensionsToRead().get(key), defaultValue);
	}

	public static void getAvailableGPXDataSetTypes(@NonNull GPXTrackAnalysis analysis, @NonNull List<GPXDataSetType[]> availableTypes) {
		if (hasSensorSpeedData(analysis)) {
			availableTypes.add(new GPXDataSetType[] {GPXDataSetType.SENSOR_SPEED});
		}
		if (hasHeartRateData(analysis)) {
			availableTypes.add(new GPXDataSetType[] {GPXDataSetType.SENSOR_HEART_RATE});
		}
		if (hasBikePowerData(analysis)) {
			availableTypes.add(new GPXDataSetType[] {GPXDataSetType.SENSOR_BIKE_POWER});
		}
		if (hasBikeCadenceData(analysis)) {
			availableTypes.add(new GPXDataSetType[] {GPXDataSetType.SENSOR_BIKE_CADENCE});
		}
		if (hasTemperatureData(analysis)) {
			availableTypes.add(new GPXDataSetType[] {GPXDataSetType.SENSOR_TEMPERATURE});
		}
	}

	public static void onAnalysePoint(@NonNull GPXTrackAnalysis analysis, @NonNull WptPt point, float distance,
	                                  int timeDiff, boolean firstPoint, boolean lastPoint) {
		for (String tag : SENSOR_GPX_TAGS) {
			float defaultValue = SENSOR_TAG_TEMPERATURE.equals(tag) ? Float.NaN : 0;
			float value = getPointAttribute(point, tag, defaultValue);
			PointsAttributesData<PointAttribute<?>> data = analysis.getAttributesData(tag);

			addPointAttribute(data, value, distance, timeDiff, firstPoint, lastPoint);
		}
	}

	private static void addPointAttribute(@NonNull PointsAttributesData<PointAttribute<?>> data, float value,
	                                      float distance, int timeDiff, boolean firstPoint, boolean lastPoint) {
		switch (data.getKey()) {
			case SENSOR_TAG_HEART_RATE:
				int heartRate = value > 0 ? (int) value : 0;
				data.addPointAttribute(new HeartRate(heartRate, distance, timeDiff, firstPoint, lastPoint));
				break;
			case SENSOR_TAG_SPEED:
				data.addPointAttribute(new SensorSpeed(value, distance, timeDiff, firstPoint, lastPoint));
				break;
			case SENSOR_TAG_CADENCE:
				data.addPointAttribute(new BikeCadence(value, distance, timeDiff, firstPoint, lastPoint));
				break;
			case SENSOR_TAG_BIKE_POWER:
				data.addPointAttribute(new BikePower(value, distance, timeDiff, firstPoint, lastPoint));
				break;
			case SENSOR_TAG_TEMPERATURE:
				data.addPointAttribute(new Temperature(value, distance, timeDiff, firstPoint, lastPoint));
				break;
		}
	}

	@Nullable
	public static OrderedLineDataSet getOrderedLineDataSet(@NonNull OsmandApplication app,
	                                                       @NonNull LineChart chart,
	                                                       @NonNull GPXTrackAnalysis analysis,
	                                                       @NonNull GPXDataSetType graphType,
	                                                       @NonNull GPXDataSetAxisType axisType,
	                                                       boolean calcWithoutGaps, boolean useRightAxis) {
		switch (graphType) {
			case SENSOR_SPEED: {
				if (hasSensorSpeedData(analysis)) {
					return createSensorDataSet(app, chart, analysis, graphType, axisType, useRightAxis, true, calcWithoutGaps);
				}
			}
			case SENSOR_HEART_RATE: {
				if (hasHeartRateData(analysis)) {
					return createSensorDataSet(app, chart, analysis, graphType, axisType, useRightAxis, true, calcWithoutGaps);
				}
			}
			case SENSOR_BIKE_POWER: {
				if (hasBikePowerData(analysis)) {
					return createSensorDataSet(app, chart, analysis, graphType, axisType, useRightAxis, true, calcWithoutGaps);
				}
			}
			case SENSOR_BIKE_CADENCE: {
				if (hasBikeCadenceData(analysis)) {
					return createSensorDataSet(app, chart, analysis, graphType, axisType, useRightAxis, true, calcWithoutGaps);
				}
			}
			case SENSOR_TEMPERATURE: {
				if (hasTemperatureData(analysis)) {
					return createSensorDataSet(app, chart, analysis, graphType, axisType, useRightAxis, true, calcWithoutGaps);
				}
			}
		}
		return null;
	}

	@NonNull
	public static OrderedLineDataSet createSensorDataSet(@NonNull OsmandApplication app,
	                                                     @NonNull LineChart chart,
	                                                     @NonNull GPXTrackAnalysis analysis,
	                                                     @NonNull GPXDataSetType graphType,
	                                                     @NonNull GPXDataSetAxisType axisType,
	                                                     boolean useRightAxis,
	                                                     boolean drawFilled,
	                                                     boolean calcWithoutGaps) {
		OsmandSettings settings = app.getSettings();
		boolean nightMode = !settings.isLightContent();

		float divX = ChartUtils.getDivX(app, chart, analysis, axisType, calcWithoutGaps);

		Pair<Float, Float> pair = ChartUtils.getScalingY(app, graphType);
		float mulY = pair != null ? pair.first : 1f;
		float divY = pair != null ? pair.second : Float.NaN;

		boolean speedInTrack = analysis.hasSpeedInTrack();
		int textColor = ColorUtilities.getColor(app, graphType.getTextColorId(!speedInTrack));
		YAxis yAxis = ChartUtils.getYAxis(chart, textColor, useRightAxis);
		yAxis.setAxisMinimum(0f);

		List<Entry> values = ChartUtils.getPointAttributeValues(analysis, graphType, axisType, divX, mulY, divY, calcWithoutGaps);
		OrderedLineDataSet dataSet = new OrderedLineDataSet(values, "", graphType, axisType, !useRightAxis);

		String format = null;
		if (dataSet.getYMax() < 3) {
			format = "{0,number,0.#} ";
		}
		String formatY = format;
		String mainUnitY = graphType.getMainUnitY(app);
		yAxis.setValueFormatter((value, axis) -> {
			if (!Algorithms.isEmpty(formatY)) {
				return MessageFormat.format(formatY + mainUnitY, value);
			} else {
				return OsmAndFormatter.formatInteger((int) (value + 0.5), mainUnitY, app);
			}
		});

		dataSet.setDivX(divX);
		dataSet.setUnits(mainUnitY);

		int color = ColorUtilities.getColor(app, graphType.getFillColorId(!speedInTrack));
		ChartUtils.setupDataSet(app, dataSet, color, color, drawFilled, false, useRightAxis, nightMode);

		return dataSet;
	}
}
