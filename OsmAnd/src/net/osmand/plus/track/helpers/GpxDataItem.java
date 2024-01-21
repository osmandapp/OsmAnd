package net.osmand.plus.track.helpers;

import static net.osmand.plus.track.helpers.GpxParameter.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GpxDataItem {

	@NonNull
	private final File file;
	private final Map<GpxParameter, Object> map = new HashMap<>();

	@Nullable
	private GPXTrackAnalysis analysis;

	public GpxDataItem(@NonNull OsmandApplication app, @NonNull File file) {
		this.file = file;
		initFileParameters(app);
	}

	private void initFileParameters(@NonNull OsmandApplication app) {
		map.put(FILE_NAME, file.getName());
		map.put(FILE_DIR, GpxDbUtils.getGpxFileDir(app, file));
		map.put(FILE_LAST_MODIFIED_TIME, file.lastModified());
	}

	@NonNull
	public File getFile() {
		return file;
	}

	@Nullable
	public GPXTrackAnalysis getAnalysis() {
		return analysis;
	}

	public void setAnalysis(@Nullable GPXTrackAnalysis analysis) {
		this.analysis = analysis;
		updateAnalysisParameters();
	}

	public boolean hasData() {
		return !map.isEmpty();
	}

	@SuppressWarnings("unchecked")
	public <T> T getParameter(@NonNull GpxParameter parameter) {
		Object value = map.containsKey(parameter) ? map.get(parameter) : parameter.getDefaultValue();
		return ((Class<T>) parameter.getTypeClass()).cast(value);
	}

	public boolean setParameter(@NonNull GpxParameter parameter, @Nullable Object value) {
		if (parameter.isValidValue(value)) {
			map.put(parameter, value);
			return true;
		}
		return false;
	}

	public void copyData(@NonNull GpxDataItem item) {
		for (Map.Entry<GpxParameter, Object> entry : item.map.entrySet()) {
			GpxParameter parameter = entry.getKey();
			if (!CollectionUtils.equalsToAny(parameter, FILE_NAME, FILE_DIR, FILE_LAST_MODIFIED_TIME)) {
				map.put(parameter, entry.getValue());
			}
		}
		setAnalysis(item.analysis);
	}

	private void updateAnalysisParameters() {
		boolean hasAnalysis = analysis != null;
		map.put(TOTAL_DISTANCE, hasAnalysis ? analysis.totalDistance : null);
		map.put(TOTAL_TRACKS, hasAnalysis ? analysis.totalTracks : null);
		map.put(START_TIME, hasAnalysis ? analysis.startTime : null);
		map.put(END_TIME, hasAnalysis ? analysis.endTime : null);
		map.put(TIME_SPAN, hasAnalysis ? analysis.timeSpan : null);
		map.put(TIME_MOVING, hasAnalysis ? analysis.timeMoving : null);
		map.put(EXPECTED_ROUTE_DURATION, hasAnalysis ? analysis.expectedRouteDuration : null);
		map.put(TOTAL_DISTANCE_MOVING, hasAnalysis ? analysis.totalDistanceMoving : null);
		map.put(DIFF_ELEVATION_UP, hasAnalysis ? analysis.diffElevationUp : null);
		map.put(DIFF_ELEVATION_DOWN, hasAnalysis ? analysis.diffElevationDown : null);
		map.put(AVG_ELEVATION, hasAnalysis ? analysis.avgElevation : null);
		map.put(MIN_ELEVATION, hasAnalysis ? analysis.minElevation : null);
		map.put(MAX_ELEVATION, hasAnalysis ? analysis.maxElevation : null);
		map.put(MAX_SPEED, hasAnalysis ? analysis.maxSpeed : null);
		map.put(AVG_SPEED, hasAnalysis ? analysis.avgSpeed : null);
		map.put(POINTS, hasAnalysis ? analysis.points : null);
		map.put(WPT_POINTS, hasAnalysis ? analysis.wptPoints : null);
		map.put(WPT_CATEGORY_NAMES, hasAnalysis ? Algorithms.encodeCollection(analysis.wptCategoryNames) : null);
		map.put(START_LAT, hasAnalysis && analysis.latLonStart != null ? analysis.latLonStart.getLatitude() : null);
		map.put(START_LON, hasAnalysis && analysis.latLonStart != null ? analysis.latLonStart.getLongitude() : null);
	}

	public void readGpxParams(@NonNull GPXFile gpxFile) {
		setParameter(COLOR, gpxFile.getColor(0));
		setParameter(WIDTH, gpxFile.getWidth(null));
		setParameter(SHOW_ARROWS, gpxFile.isShowArrows());
		setParameter(SHOW_START_FINISH, gpxFile.isShowStartFinish());

		if (!Algorithms.isEmpty(gpxFile.getSplitType()) && gpxFile.getSplitInterval() > 0) {
			GpxSplitType splitType = GpxSplitType.getSplitTypeByName(gpxFile.getSplitType());
			setParameter(SPLIT_TYPE, splitType.getType());
			setParameter(SPLIT_INTERVAL, gpxFile.getSplitInterval());
		}

		if (!Algorithms.isEmpty(gpxFile.getColoringType())) {
			setParameter(COLORING_TYPE, gpxFile.getColoringType());
		} else if (!Algorithms.isEmpty(gpxFile.getGradientScaleType())) {
			GradientScaleType scaleType = GradientScaleType.getGradientTypeByName(gpxFile.getGradientScaleType());
			ColoringType coloringType = ColoringType.fromGradientScaleType(scaleType);
			setParameter(COLORING_TYPE, coloringType == null ? null : coloringType.getName(null));
		}

		Map<String, String> extensions = gpxFile.getExtensionsToRead();
		setParameter(SMOOTHING_THRESHOLD, GpsFilterHelper.SmoothingFilter.getSmoothingThreshold(extensions));
		setParameter(MIN_FILTER_SPEED, GpsFilterHelper.SpeedFilter.getMinFilterSpeed(extensions));
		setParameter(MAX_FILTER_SPEED, GpsFilterHelper.SpeedFilter.getMaxFilterSpeed(extensions));
		setParameter(MIN_FILTER_ALTITUDE, GpsFilterHelper.AltitudeFilter.getMinFilterAltitude(extensions));
		setParameter(MAX_FILTER_ALTITUDE, GpsFilterHelper.AltitudeFilter.getMaxFilterAltitude(extensions));
		setParameter(MAX_FILTER_HDOP, GpsFilterHelper.HdopFilter.getMaxFilterHdop(extensions));
		setParameter(FILE_CREATION_TIME, gpxFile.metadata.time);
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GpxDataItem)) {
			return false;
		}
		GpxDataItem other = (GpxDataItem) obj;
		return file.equals(other.file);
	}
}
