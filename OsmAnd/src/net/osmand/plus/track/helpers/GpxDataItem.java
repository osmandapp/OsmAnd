package net.osmand.plus.track.helpers;

import static net.osmand.gpx.GpxParameter.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GpxParameter;
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
	public final Map<GpxParameter, Object> map = new HashMap<>();

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
		for(GpxParameter gpxParameter: values()) {
			if(gpxParameter.isAnalysisParameter()) {
				map.put(gpxParameter, hasAnalysis ? analysis.getGpxParameter(gpxParameter) : null);
			}
		}
	}

	public void readGpxParams(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile) {
		setParameter(FILE_NAME, getFile().getName());
		setParameter(FILE_DIR, GpxDbUtils.getGpxFileDir(app, file));
		setParameter(FILE_LAST_MODIFIED_TIME, getFile().lastModified());
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
			ColoringType coloringType = ColoringType.valueOf(scaleType);
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
