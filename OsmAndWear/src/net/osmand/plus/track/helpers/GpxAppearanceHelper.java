package net.osmand.plus.track.helpers;

import static net.osmand.shared.gpx.GpxParameter.ADDITIONAL_EXAGGERATION;
import static net.osmand.shared.gpx.GpxParameter.COLOR;
import static net.osmand.shared.gpx.GpxParameter.ELEVATION_METERS;
import static net.osmand.shared.gpx.GpxParameter.COLOR_PALETTE;
import static net.osmand.shared.gpx.GpxParameter.SHOW_ARROWS;
import static net.osmand.shared.gpx.GpxParameter.SHOW_START_FINISH;
import static net.osmand.shared.gpx.GpxParameter.TRACK_3D_LINE_POSITION_TYPE;
import static net.osmand.shared.gpx.GpxParameter.TRACK_3D_WALL_COLORING_TYPE;
import static net.osmand.shared.gpx.GpxParameter.TRACK_VISUALIZATION_TYPE;
import static net.osmand.shared.gpx.GpxParameter.WIDTH;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.Gpx3DLinePositionType;
import net.osmand.plus.track.Gpx3DVisualizationType;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.routing.Gpx3DWallColorType;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDirItem;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxParameter;
import net.osmand.shared.io.KFile;

import java.io.File;

public class GpxAppearanceHelper {

	private final OsmandSettings settings;
	private final GpxDbHelper gpxDbHelper;

	private TrackDrawInfo trackDrawInfo;

	public GpxAppearanceHelper(@NonNull OsmandApplication app) {
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

	public boolean hasTrackDrawInfoForTrack(@NonNull GpxFile gpxFile) {
		return trackDrawInfo != null && (trackDrawInfo.isCurrentRecording() && gpxFile.isShowCurrentTrack()
				|| gpxFile.getPath().equals(trackDrawInfo.getFilePath()));
	}

	public boolean isShowArrowsForTrack(@NonNull GpxFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.isShowArrows();
		} else if (gpxFile.isShowCurrentTrack()) {
			return settings.CURRENT_TRACK_SHOW_ARROWS.get();
		} else {
			Boolean show = getAppearanceParameter(new File(gpxFile.getPath()), SHOW_ARROWS);
			if (show != null) {
				return show;
			}
			return gpxFile.isShowArrows();
		}
	}

	public boolean isShowStartFinishForTrack(@NonNull GpxFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.isShowStartFinish();
		} else if (gpxFile.isShowCurrentTrack()) {
			return settings.CURRENT_TRACK_SHOW_START_FINISH.get();
		} else {
			Boolean show = getAppearanceParameter(new File(gpxFile.getPath()), SHOW_START_FINISH);
			if (show != null) {
				return show;
			}
			return gpxFile.isShowStartFinish();
		}
	}

	public Gpx3DVisualizationType getTrackVisualizationForTrack(@NonNull GpxFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.getTrackVisualizationType();
		} else if (gpxFile.isShowCurrentTrack()) {
			return Gpx3DVisualizationType.get3DVisualizationType(settings.CURRENT_TRACK_3D_VISUALIZATION_TYPE.get());
		} else {
			String trackVisualizationType = getAppearanceParameter(new File(gpxFile.getPath()), TRACK_VISUALIZATION_TYPE);
			if (trackVisualizationType != null) {
				return Gpx3DVisualizationType.get3DVisualizationType(trackVisualizationType);
			}
			return Gpx3DVisualizationType.get3DVisualizationType(gpxFile.get3DVisualizationType());
		}
	}

	public Gpx3DWallColorType getTrackWallColorType(@NonNull GpxFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.getTrackWallColorType();
		} else if (gpxFile.isShowCurrentTrack()) {
			return Gpx3DWallColorType.Companion.get3DWallColorType(settings.CURRENT_TRACK_3D_WALL_COLORING_TYPE.get());
		} else {
			String trackWallColorType = getAppearanceParameter(new File(gpxFile.getPath()), TRACK_3D_WALL_COLORING_TYPE);
			if (trackWallColorType != null) {
				return Gpx3DWallColorType.Companion.get3DWallColorType(trackWallColorType);
			}
			return Gpx3DWallColorType.Companion.get3DWallColorType(gpxFile.get3DWallColoringType());
		}
	}

	public Gpx3DLinePositionType getTrackLinePositionType(@NonNull GpxFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.getTrackLinePositionType();
		} else if (gpxFile.getShowCurrentTrack()) {
			return Gpx3DLinePositionType.get3DLinePositionType(settings.CURRENT_TRACK_3D_LINE_POSITION_TYPE.get());
		} else {
			String trackLinePositionType = getAppearanceParameter(new File(gpxFile.getPath()), TRACK_3D_LINE_POSITION_TYPE);
			if (trackLinePositionType != null) {
				return Gpx3DLinePositionType.get3DLinePositionType(trackLinePositionType);
			}
			return Gpx3DLinePositionType.get3DLinePositionType(gpxFile.get3DWallColoringType());
		}
	}

	public float getAdditionalExaggeration(@NonNull GpxFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.getAdditionalExaggeration();
		} else if (gpxFile.getShowCurrentTrack()) {
			return settings.CURRENT_TRACK_ADDITIONAL_EXAGGERATION.get();
		} else {
			Double exaggeration = getAppearanceParameter(new File(gpxFile.getPath()), ADDITIONAL_EXAGGERATION);
			if (exaggeration != null) {
				return exaggeration.floatValue();
			}
			return gpxFile.getAdditionalExaggeration();
		}
	}

	public float getElevationMeters(@NonNull GpxFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.getElevationMeters();
		} else if (gpxFile.isShowCurrentTrack()) {
			return settings.CURRENT_TRACK_ELEVATION_METERS.get();
		} else {
			Double elevation = getAppearanceParameter(new File(gpxFile.getPath()), ELEVATION_METERS);
			if (elevation != null) {
				return elevation.floatValue();
			}
			return gpxFile.getElevationMeters();
		}
	}

	@Nullable
	public String getTrackWidth(@NonNull GpxFile gpxFile, @Nullable String defaultWidth) {
		String width;
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			width = trackDrawInfo.getWidth();
		} else if (gpxFile.getShowCurrentTrack()) {
			width = settings.CURRENT_TRACK_WIDTH.get();
		} else {
			width = getAppearanceParameter(new File(gpxFile.getPath()), WIDTH);
		}
		return width != null ? width : gpxFile.getWidth(defaultWidth);
	}

	public int getTrackColor(@NonNull GpxFile gpxFile, int defaultColor) {
		Integer color;
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			color = trackDrawInfo.getColor();
		} else if (gpxFile.getShowCurrentTrack()) {
			color = settings.CURRENT_TRACK_COLOR.get();
		} else {
			color = getAppearanceParameter(new File(gpxFile.getPath()), COLOR);
		}
		return color != null ? color : gpxFile.getColor(defaultColor);
	}

	@Nullable
	public String getGradientPaletteName(@NonNull GpxFile gpxFile) {
		String gradientPalette;
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			gradientPalette = trackDrawInfo.getGradientColorName();
		} else if (gpxFile.isShowCurrentTrack()) {
			gradientPalette = settings.CURRENT_GRADIENT_PALETTE.get();
		} else {
			gradientPalette = getAppearanceParameter(new File(gpxFile.getPath()), COLOR_PALETTE);
		}
		return gradientPalette != null ? gradientPalette : gpxFile.getGradientColorPalette();
	}

	@Nullable
	public String getColoringType(@NonNull GpxFile gpxFile) {
		if (hasTrackDrawInfoForTrack(gpxFile)) {
			return trackDrawInfo.getColoringType().getName(trackDrawInfo.getRouteInfoAttribute());
		}
		return null;
	}

	@NonNull
	public <T> T requireParameter(@NonNull GpxDataItem item, @NonNull GpxParameter parameter) {
		Object value = getAppearanceParameter(item, parameter);
		if (value == null) {
			value = parameter.getDefaultValue();
		}
		if (value == null) {
			throw new IllegalStateException("Requested parameter '" + parameter + "' is null.");
		} else {
			T res = SharedUtil.castGpxParameter(parameter, value);
			if (res == null) {
				throw new IllegalStateException("Requested parameter '" + parameter + "' cast is null.");
			}
			return res;
		}
	}

	@Nullable
	public <T> T getParameter(@NonNull GpxDataItem item, @NonNull GpxParameter parameter) {
		Object value = getAppearanceParameter(item, parameter);
		if (value == null) {
			value = parameter.getDefaultValue();
		}
		return SharedUtil.castGpxParameter(parameter, value);
	}

	@Nullable
	public <T> T getAppearanceParameter(@NonNull File file, @NonNull GpxParameter parameter) {
		GpxDataItem item = gpxDbHelper.getItem(SharedUtil.kFile(file));
		if (item != null) {
			return getAppearanceParameter(item, parameter);
		}
		return null;
	}

	@Nullable
	public <T> T getAppearanceParameter(@NonNull GpxDataItem item, @NonNull GpxParameter parameter) {
		Object value = item.getParameter(parameter);
		if (value != null) {
			return SharedUtil.castGpxParameter(parameter, value);
		}
		KFile dir = item.getFile().getParentFile();
		if (dir != null) {
			GpxDirItem dirItem = gpxDbHelper.getGpxDirItem(dir);
			value = dirItem.getParameter(parameter);
			if (value != null) {
				return SharedUtil.castGpxParameter(parameter, value);
			}
		}
		return null;
	}
}