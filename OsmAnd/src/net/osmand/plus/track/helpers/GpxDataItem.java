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
import java.util.Map;

public class GpxDataItem extends DataItem {

	@Nullable
	private GPXTrackAnalysis analysis;

	public GpxDataItem(@NonNull OsmandApplication app, @NonNull File file) {
		super(app, file);
	}

	@Nullable
	public GPXTrackAnalysis getAnalysis() {
		return analysis;
	}

	public void setAnalysis(@Nullable GPXTrackAnalysis analysis) {
		this.analysis = analysis;
		updateAnalysisParameters();
	}

	public boolean isValidValue(@NonNull GpxParameter parameter, @Nullable Object value) {
		return value == null && parameter.isNullSupported() || value != null && parameter.getTypeClass() == value.getClass();
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
		for (GpxParameter gpxParameter : GpxParameter.values()) {
			if (gpxParameter.isAnalysisParameter()) {
				map.put(gpxParameter, hasAnalysis ? analysis.getGpxParameter(gpxParameter) : null);
			}
		}
	}

	public void readGpxParams(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile) {
		setParameter(FILE_NAME, getFile().getName());
		setParameter(FILE_DIR, GpxDbUtils.getGpxFileDir(app, file));
		setParameter(FILE_LAST_MODIFIED_TIME, getFile().lastModified());

		for (GpxParameter parameter : GpxParameter.getAppearanceParameters()) {
			readGpxAppearanceParameter(gpxFile, parameter);
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

	public void readGpxAppearanceParameter(@NonNull GPXFile gpxFile, @NonNull GpxParameter parameter) {
		switch (parameter) {
			case COLOR:
				setParameter(COLOR, gpxFile.getColor(null));
				break;
			case WIDTH:
				setParameter(WIDTH, gpxFile.getWidth(null));
				break;
			case SHOW_ARROWS:
				setParameter(SHOW_ARROWS, gpxFile.isShowArrowsSet() ? gpxFile.isShowArrows() : null);
				break;
			case SHOW_START_FINISH:
				setParameter(SHOW_START_FINISH, gpxFile.isShowStartFinishSet() ? gpxFile.isShowStartFinish() : null);
				break;
			case SPLIT_TYPE:
				if (!Algorithms.isEmpty(gpxFile.getSplitType()) && gpxFile.getSplitInterval() > 0) {
					GpxSplitType splitType = GpxSplitType.getSplitTypeByName(gpxFile.getSplitType());
					setParameter(SPLIT_TYPE, splitType.getType());
				}
				break;
			case SPLIT_INTERVAL:
				if (!Algorithms.isEmpty(gpxFile.getSplitType()) && gpxFile.getSplitInterval() > 0) {
					setParameter(SPLIT_INTERVAL, gpxFile.getSplitInterval());
				}
				break;
			case COLORING_TYPE:
				if (!Algorithms.isEmpty(gpxFile.getColoringType())) {
					setParameter(COLORING_TYPE, gpxFile.getColoringType());
				} else if (!Algorithms.isEmpty(gpxFile.getGradientScaleType())) {
					GradientScaleType scaleType = GradientScaleType.getGradientTypeByName(gpxFile.getGradientScaleType());
					ColoringType coloringType = ColoringType.valueOf(scaleType);
					setParameter(COLORING_TYPE, coloringType == null ? null : coloringType.getName(null));
				}
				break;
			case COLOR_PALETTE:
				if (!Algorithms.isEmpty(gpxFile.getGradientColorPalette())) {
					setParameter(COLOR_PALETTE, gpxFile.getGradientColorPalette());
				}
				break;
			case TRACK_VISUALIZATION_TYPE:
				setParameter(TRACK_VISUALIZATION_TYPE, gpxFile.get3DVisualizationType());
				break;
			case TRACK_3D_LINE_POSITION_TYPE:
				setParameter(TRACK_3D_LINE_POSITION_TYPE, gpxFile.get3DLinePositionType());
				break;
			case TRACK_3D_WALL_COLORING_TYPE:
				setParameter(TRACK_3D_WALL_COLORING_TYPE, gpxFile.get3DWallColoringType());
				break;
		}
	}
}
