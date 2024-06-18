package net.osmand.plus.track.helpers;

import static net.osmand.gpx.GpxParameter.ADDITIONAL_EXAGGERATION;
import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.ELEVATION_METERS;
import static net.osmand.gpx.GpxParameter.COLOR_PALETTE;
import static net.osmand.gpx.GpxParameter.SHOW_ARROWS;
import static net.osmand.gpx.GpxParameter.SHOW_START_FINISH;
import static net.osmand.gpx.GpxParameter.TRACK_3D_LINE_POSITION_TYPE;
import static net.osmand.gpx.GpxParameter.TRACK_3D_WALL_COLORING_TYPE;
import static net.osmand.gpx.GpxParameter.TRACK_VISUALIZATION_TYPE;
import static net.osmand.gpx.GpxParameter.WIDTH;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GpxParameter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.Gpx3DLinePositionType;
import net.osmand.plus.track.Gpx3DVisualizationType;
import net.osmand.plus.track.Gpx3DWallColorType;
import net.osmand.plus.track.TrackDrawInfo;

import java.io.File;

public class GpxAppearanceHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final GpxDbHelper gpxDbHelper;

	private TrackDrawInfo trackDrawInfo;

	public GpxAppearanceHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
		gpxDbHelper = app.getGpxDbHelper();
	}

	@Nullable
	public TrackDrawInfo getTrackDrawInfo() {
		return trackDrawInfo;
	}

	public boolean isInTrackAppearanceMode() {
		return trackDrawInfo != null;
	}

	public void setTrackDrawInfo(@Nullable TrackDrawInfo trackDrawInfo) {
		this.trackDrawInfo = trackDrawInfo;
	}

	public boolean hasTrackDrawInfoForTrack(@NonNull GPXFile gpxFile) {
		return trackDrawInfo != null && (trackDrawInfo.isCurrentRecording() && gpxFile.showCurrentTrack
				|| gpxFile.path.equals(trackDrawInfo.getFilePath()));
	}

	public boolean isShowArrowsForTrack(@NonNull GPXFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.isShowArrows();
		} else if (gpxFile.showCurrentTrack) {
			return settings.CURRENT_TRACK_SHOW_ARROWS.get();
		} else {
			Boolean show = getAppearanceParameter(new File(gpxFile.path), SHOW_ARROWS);
			if (show != null) {
				return show;
			}
			return gpxFile.isShowArrows();
		}
	}

	public boolean isShowStartFinishForTrack(@NonNull GPXFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.isShowStartFinish();
		} else if (gpxFile.showCurrentTrack) {
			return settings.CURRENT_TRACK_SHOW_START_FINISH.get();
		} else {
			Boolean show = getAppearanceParameter(new File(gpxFile.path), SHOW_START_FINISH);
			if (show != null) {
				return show;
			}
			return gpxFile.isShowStartFinish();
		}
	}

	public Gpx3DVisualizationType getTrackVisualizationForTrack(@NonNull GPXFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.getTrackVisualizationType();
		} else if (gpxFile.showCurrentTrack) {
			return Gpx3DVisualizationType.get3DVisualizationType(settings.CURRENT_TRACK_3D_VISUALIZATION_TYPE.get());
		} else {
			String trackVisualizationType = getAppearanceParameter(new File(gpxFile.path), TRACK_VISUALIZATION_TYPE);
			if (trackVisualizationType != null) {
				return Gpx3DVisualizationType.get3DVisualizationType(trackVisualizationType);
			}
			return Gpx3DVisualizationType.get3DVisualizationType(gpxFile.get3DVisualizationType());
		}
	}

	public Gpx3DWallColorType getTrackWallColorType(@NonNull GPXFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.getTrackWallColorType();
		} else if (gpxFile.showCurrentTrack) {
			return Gpx3DWallColorType.get3DWallColorType(settings.CURRENT_TRACK_3D_WALL_COLORING_TYPE.get());
		} else {
			String trackWallColorType = getAppearanceParameter(new File(gpxFile.path), TRACK_3D_WALL_COLORING_TYPE);
			if (trackWallColorType != null) {
				return Gpx3DWallColorType.get3DWallColorType(trackWallColorType);
			}
			return Gpx3DWallColorType.get3DWallColorType(gpxFile.get3DWallColoringType());
		}
	}

	public Gpx3DLinePositionType getTrackLinePositionType(@NonNull GPXFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.getTrackLinePositionType();
		} else if (gpxFile.showCurrentTrack) {
			return Gpx3DLinePositionType.get3DLinePositionType(settings.CURRENT_TRACK_3D_LINE_POSITION_TYPE.get());
		} else {
			String trackLinePositionType = getAppearanceParameter(new File(gpxFile.path), TRACK_3D_LINE_POSITION_TYPE);
			if (trackLinePositionType != null) {
				return Gpx3DLinePositionType.get3DLinePositionType(trackLinePositionType);
			}
			return Gpx3DLinePositionType.get3DLinePositionType(gpxFile.get3DWallColoringType());
		}
	}

	public float getAdditionalExaggeration(@NonNull GPXFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.getAdditionalExaggeration();
		} else if (gpxFile.showCurrentTrack) {
			return settings.CURRENT_TRACK_ADDITIONAL_EXAGGERATION.get();
		} else {
			Double exaggeration = getAppearanceParameter(new File(gpxFile.path), ADDITIONAL_EXAGGERATION);
			if (exaggeration != null) {
				return exaggeration.floatValue();
			}
			return gpxFile.getAdditionalExaggeration();
		}
	}

	public float getElevationMeters(@NonNull GPXFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.getElevationMeters();
		} else if (gpxFile.showCurrentTrack) {
			return settings.CURRENT_TRACK_ELEVATION_METERS.get();
		} else {
			Double elevation = getAppearanceParameter(new File(gpxFile.path), ELEVATION_METERS);
			if (elevation != null) {
				return elevation.floatValue();
			}
			return gpxFile.getElevationMeters();
		}
	}

	@Nullable
	public String getTrackWidth(@NonNull GPXFile gpxFile, @Nullable String defaultWidth) {
		String width;
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			width = trackDrawInfo.getWidth();
		} else if (gpxFile.showCurrentTrack) {
			width = settings.CURRENT_TRACK_WIDTH.get();
		} else {
			width = getAppearanceParameter(new File(gpxFile.path), WIDTH);
		}
		return width != null ? width : gpxFile.getWidth(defaultWidth);
	}

	public int getTrackColor(@NonNull GPXFile gpxFile, int defaultColor) {
		Integer color;
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			color = trackDrawInfo.getColor();
		} else if (gpxFile.showCurrentTrack) {
			color = settings.CURRENT_TRACK_COLOR.get();
		} else {
			color = getAppearanceParameter(new File(gpxFile.path), COLOR);
		}
		return color != null ? color : gpxFile.getColor(defaultColor);
	}

	@Nullable
	public String getGradientPaletteName(@NonNull GPXFile gpxFile) {
		String gradientPalette;
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			gradientPalette = trackDrawInfo.getGradientColorName();
		} else if (gpxFile.showCurrentTrack) {
			gradientPalette = settings.CURRENT_GRADIENT_PALETTE.get();
		} else {
			gradientPalette = getAppearanceParameter(new File(gpxFile.path), COLOR_PALETTE);
		}
		return gradientPalette != null ? gradientPalette : gpxFile.getGradientColorPalette();
	}

	@Nullable
	public String getColoringType(@NonNull GPXFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.getColoringType().getName(trackDrawInfo.getRouteInfoAttribute());
		}
		return null;
	}

	@NonNull
	@SuppressWarnings("unchecked")
	public <T> T requireParameter(@NonNull GpxDataItem item, @NonNull GpxParameter parameter) {
		Object value = getAppearanceParameter(item, parameter);
		if (value == null) {
			value = parameter.getDefaultValue();
		}
		if (value == null) {
			throw new IllegalStateException("Requested parameter '" + parameter + "' is null.");
		} else {
			return ((Class<T>) parameter.getTypeClass()).cast(value);
		}
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T getParameter(@NonNull GpxDataItem item, @NonNull GpxParameter parameter) {
		Object value = getAppearanceParameter(item, parameter);
		if (value == null) {
			value = parameter.getDefaultValue();
		}
		return ((Class<T>) parameter.getTypeClass()).cast(value);
	}

	@Nullable
	public <T> T getAppearanceParameter(@NonNull File file, @NonNull GpxParameter parameter) {
		GpxDataItem item = gpxDbHelper.getItem(file);
		if (item != null) {
			return getAppearanceParameter(item, parameter);
		}
		return null;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T getAppearanceParameter(@NonNull GpxDataItem item, @NonNull GpxParameter parameter) {
		Object value = item.getParameter(parameter);
		if (value != null) {
			return ((Class<T>) parameter.getTypeClass()).cast(value);
		}
		File dir = item.getFile().getParentFile();
		if (dir != null) {
			GpxDirItem dirItem = gpxDbHelper.getGpxDirItem(dir);
			value = dirItem.getParameter(parameter);
			if (value != null) {
				return ((Class<T>) parameter.getTypeClass()).cast(value);
			}
		}
		return null;
	}
}