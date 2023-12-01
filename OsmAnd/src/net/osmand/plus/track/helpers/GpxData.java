package net.osmand.plus.track.helpers;

import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_AVG_ELEVATION;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_AVG_SPEED;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_COLOR;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_COLORING_TYPE;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_DIFF_ELEVATION_DOWN;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_DIFF_ELEVATION_UP;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_END_TIME;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_FILE_CREATION_TIME;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_MAX_ELEVATION;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_MAX_FILTER_ALTITUDE;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_MAX_FILTER_HDOP;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_MAX_FILTER_SPEED;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_MAX_SPEED;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_MIN_ELEVATION;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_MIN_FILTER_ALTITUDE;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_MIN_FILTER_SPEED;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_POINTS;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_SHOW_ARROWS;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_SHOW_START_FINISH;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_SMOOTHING_THRESHOLD;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_SPLIT_INTERVAL;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_SPLIT_TYPE;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_START_LAT;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_START_LON;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_START_TIME;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_TIME_MOVING;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_TIME_SPAN;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_TOTAL_DISTANCE;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_TOTAL_DISTANCE_MOVING;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_TOTAL_TRACKS;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_WIDTH;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_WPT_CATEGORY_NAMES;
import static net.osmand.plus.track.helpers.GpxParameter.GPX_COL_WPT_POINTS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.plus.track.helpers.GpsFilterHelper.AltitudeFilter;
import net.osmand.plus.track.helpers.GpsFilterHelper.HdopFilter;
import net.osmand.plus.track.helpers.GpsFilterHelper.SmoothingFilter;
import net.osmand.plus.track.helpers.GpsFilterHelper.SpeedFilter;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;

public class GpxData {

	private final Map<GpxParameter<?>, Object> map = new HashMap<>();

	@Nullable
	private GPXTrackAnalysis analysis;

	public boolean hasData() {
		return !map.isEmpty();
	}

	public <T> T getValue(@NonNull GpxParameter<T> parameter) {
		return map.containsKey(parameter) ? (T) map.get(parameter) : parameter.getDefaultValue();
	}

	public <T> boolean setValue(@NonNull GpxParameter<?> parameter, @Nullable T value) {
		if (parameter.isValidValue(value)) {
			map.put(parameter, value);
			return true;
		}
		return false;
	}

	public void copyData(@NonNull GpxData data) {
		map.clear();
		map.putAll(data.map);
		setAnalysis(data.analysis);
	}

	@Nullable
	public GPXTrackAnalysis getAnalysis() {
		return analysis;
	}

	public void setAnalysis(@Nullable GPXTrackAnalysis analysis) {
		this.analysis = analysis;
		updateAnalysisParameters();
	}

	private void updateAnalysisParameters() {
		boolean hasAnalysis = analysis != null;
		map.put(GPX_COL_TOTAL_DISTANCE, hasAnalysis ? analysis.totalDistance : null);
		map.put(GPX_COL_TOTAL_TRACKS, hasAnalysis ? analysis.totalTracks : null);
		map.put(GPX_COL_START_TIME, hasAnalysis ? analysis.startTime : null);
		map.put(GPX_COL_END_TIME, hasAnalysis ? analysis.endTime : null);
		map.put(GPX_COL_TIME_SPAN, hasAnalysis ? analysis.timeSpan : null);
		map.put(GPX_COL_TIME_MOVING, hasAnalysis ? analysis.timeMoving : null);
		map.put(GPX_COL_TOTAL_DISTANCE_MOVING, hasAnalysis ? analysis.totalDistanceMoving : null);
		map.put(GPX_COL_DIFF_ELEVATION_UP, hasAnalysis ? analysis.diffElevationUp : null);
		map.put(GPX_COL_DIFF_ELEVATION_DOWN, hasAnalysis ? analysis.diffElevationDown : null);
		map.put(GPX_COL_AVG_ELEVATION, hasAnalysis ? analysis.avgElevation : null);
		map.put(GPX_COL_MIN_ELEVATION, hasAnalysis ? analysis.minElevation : null);
		map.put(GPX_COL_MAX_ELEVATION, hasAnalysis ? analysis.maxElevation : null);
		map.put(GPX_COL_MAX_SPEED, hasAnalysis ? analysis.maxSpeed : null);
		map.put(GPX_COL_AVG_SPEED, hasAnalysis ? analysis.avgSpeed : null);
		map.put(GPX_COL_POINTS, hasAnalysis ? analysis.points : null);
		map.put(GPX_COL_WPT_POINTS, hasAnalysis ? analysis.wptPoints : null);
		map.put(GPX_COL_WPT_CATEGORY_NAMES, hasAnalysis ? Algorithms.encodeCollection(analysis.wptCategoryNames) : null);
		map.put(GPX_COL_START_LAT, hasAnalysis && analysis.latLonStart != null ? analysis.latLonStart.getLatitude() : null);
		map.put(GPX_COL_START_LON, hasAnalysis && analysis.latLonStart != null ? analysis.latLonStart.getLongitude() : null);
	}

	public void readGpxParams(@NonNull GPXFile gpxFile) {
		setValue(GPX_COL_COLOR, gpxFile.getColor(0));
		setValue(GPX_COL_WIDTH, gpxFile.getWidth(null));
		setValue(GPX_COL_SHOW_ARROWS, gpxFile.isShowArrows());
		setValue(GPX_COL_SHOW_START_FINISH, gpxFile.isShowStartFinish());

		if (!Algorithms.isEmpty(gpxFile.getSplitType()) && gpxFile.getSplitInterval() > 0) {
			GpxSplitType splitType = GpxSplitType.getSplitTypeByName(gpxFile.getSplitType());
			setValue(GPX_COL_SPLIT_TYPE, splitType.getType());
			setValue(GPX_COL_SPLIT_INTERVAL, gpxFile.getSplitInterval());
		}

		if (!Algorithms.isEmpty(gpxFile.getColoringType())) {
			setValue(GPX_COL_COLORING_TYPE, gpxFile.getColoringType());
		} else if (!Algorithms.isEmpty(gpxFile.getGradientScaleType())) {
			GradientScaleType scaleType = GradientScaleType.getGradientTypeByName(gpxFile.getGradientScaleType());
			ColoringType coloringType = ColoringType.fromGradientScaleType(scaleType);
			setValue(GPX_COL_COLORING_TYPE, coloringType == null ? null : coloringType.getName(null));
		}

		Map<String, String> extensions = gpxFile.getExtensionsToRead();
		setValue(GPX_COL_SMOOTHING_THRESHOLD, SmoothingFilter.getSmoothingThreshold(extensions));
		setValue(GPX_COL_MIN_FILTER_SPEED, SpeedFilter.getMinFilterSpeed(extensions));
		setValue(GPX_COL_MAX_FILTER_SPEED, SpeedFilter.getMaxFilterSpeed(extensions));
		setValue(GPX_COL_MIN_FILTER_ALTITUDE, AltitudeFilter.getMinFilterAltitude(extensions));
		setValue(GPX_COL_MAX_FILTER_ALTITUDE, AltitudeFilter.getMaxFilterAltitude(extensions));
		setValue(GPX_COL_MAX_FILTER_HDOP, HdopFilter.getMaxFilterHdop(extensions));
		setValue(GPX_COL_FILE_CREATION_TIME, gpxFile.metadata.time);
	}
}