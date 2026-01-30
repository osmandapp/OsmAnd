package net.osmand.plus.track.helpers;

import static net.osmand.plus.routing.ColoringStyleAlgorithms.isAvailableInSubscription;
import static net.osmand.plus.track.Gpx3DVisualizationType.FIXED_HEIGHT;
import static net.osmand.shared.gpx.GpxParameter.*;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.CachedTrack;
import net.osmand.plus.track.Gpx3DLinePositionType;
import net.osmand.plus.track.Gpx3DVisualizationType;
import net.osmand.plus.track.Track3DStyle;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.shared.gpx.ColoringPurpose;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.GpxDirItem;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxParameter;
import net.osmand.shared.palette.data.PaletteUtils;
import net.osmand.shared.routing.ColoringType;
import net.osmand.shared.routing.Gpx3DWallColorType;
import net.osmand.util.Algorithms;

import java.io.File;

public class GpxAppearanceHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final GpxDbHelper gpxDbHelper;

	@Nullable
	private TrackDrawInfo trackDrawInfo;

	@ColorInt
	private int disabledColor;

	public GpxAppearanceHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.gpxDbHelper = app.getGpxDbHelper();
		this.disabledColor = ContextCompat.getColor(app, R.color.gpx_disabled_color);
	}

	public void setDisabledColor(@ColorInt int disabledColor) {
		this.disabledColor = disabledColor;
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

	@Nullable
	public TrackDrawInfo getTrackDrawInfoForTrack(@NonNull GpxFile gpxFile) {
		return hasTrackDrawInfoForTrack(gpxFile) ? trackDrawInfo : null;
	}

	public boolean isShowArrowsForTrack(@NonNull GpxFile gpxFile, @Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem) {
		TrackDrawInfo drawInfo = getTrackDrawInfoForTrack(gpxFile);
		if (drawInfo != null) {
			return drawInfo.isShowArrows();
		} else if (gpxFile.isShowCurrentTrack()) {
			return settings.CURRENT_TRACK_SHOW_ARROWS.get();
		} else if (gpxItem != null) {
			Boolean show = getAppearanceParameter(gpxItem, dirItem, SHOW_ARROWS);
			if (show != null) {
				return show;
			}
		}
		return gpxFile.isShowArrows();
	}

	public boolean isShowStartFinishForTrack(@NonNull GpxFile gpxFile, @Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem) {
		TrackDrawInfo drawInfo = getTrackDrawInfoForTrack(gpxFile);
		if (drawInfo != null) {
			return drawInfo.isShowStartFinish();
		} else if (gpxFile.isShowCurrentTrack()) {
			return settings.CURRENT_TRACK_SHOW_START_FINISH.get();
		} else if (gpxItem != null) {
			Boolean show = getAppearanceParameter(gpxItem, dirItem, SHOW_START_FINISH);
			if (show != null) {
				return show;
			}
		}
		return gpxFile.isShowStartFinish();
	}

	public Gpx3DVisualizationType getTrackVisualizationForTrack(@NonNull GpxFile gpxFile, @Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem) {
		TrackDrawInfo drawInfo = getTrackDrawInfoForTrack(gpxFile);
		if (drawInfo != null) {
			return drawInfo.getTrackVisualizationType();
		} else if (gpxFile.isShowCurrentTrack()) {
			return Gpx3DVisualizationType.get3DVisualizationType(settings.CURRENT_TRACK_3D_VISUALIZATION_TYPE.get());
		} else if (gpxItem != null) {
			String trackVisualizationType = getAppearanceParameter(gpxItem, dirItem, TRACK_VISUALIZATION_TYPE);
			if (trackVisualizationType != null) {
				return Gpx3DVisualizationType.get3DVisualizationType(trackVisualizationType);
			}
		}
		return Gpx3DVisualizationType.get3DVisualizationType(gpxFile.get3DVisualizationType());
	}

	public Gpx3DWallColorType getTrackWallColorType(@NonNull GpxFile gpxFile, @Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem) {
		TrackDrawInfo drawInfo = getTrackDrawInfoForTrack(gpxFile);
		if (drawInfo != null) {
			return drawInfo.getTrackWallColorType();
		} else if (gpxFile.isShowCurrentTrack()) {
			return Gpx3DWallColorType.Companion.get3DWallColorType(settings.CURRENT_TRACK_3D_WALL_COLORING_TYPE.get());
		} else if (gpxItem != null) {
			String trackWallColorType = getAppearanceParameter(gpxItem, dirItem, TRACK_3D_WALL_COLORING_TYPE);
			if (trackWallColorType != null) {
				return Gpx3DWallColorType.Companion.get3DWallColorType(trackWallColorType);
			}
		}
		return Gpx3DWallColorType.Companion.get3DWallColorType(gpxFile.get3DWallColoringType());
	}

	public Gpx3DLinePositionType getTrackLinePositionType(@NonNull GpxFile gpxFile, @Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem) {
		TrackDrawInfo drawInfo = getTrackDrawInfoForTrack(gpxFile);
		if (drawInfo != null) {
			return drawInfo.getTrackLinePositionType();
		} else if (gpxFile.getShowCurrentTrack()) {
			return Gpx3DLinePositionType.get3DLinePositionType(settings.CURRENT_TRACK_3D_LINE_POSITION_TYPE.get());
		} else if (gpxItem != null) {
			String trackLinePositionType = getAppearanceParameter(gpxItem, dirItem, TRACK_3D_LINE_POSITION_TYPE);
			if (trackLinePositionType != null) {
				return Gpx3DLinePositionType.get3DLinePositionType(trackLinePositionType);
			}
		}
		return Gpx3DLinePositionType.get3DLinePositionType(gpxFile.get3DWallColoringType());
	}

	public float getAdditionalExaggeration(@NonNull GpxFile gpxFile, @Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem) {
		TrackDrawInfo drawInfo = getTrackDrawInfoForTrack(gpxFile);
		if (drawInfo != null) {
			return drawInfo.getAdditionalExaggeration();
		} else if (gpxFile.getShowCurrentTrack()) {
			return settings.CURRENT_TRACK_ADDITIONAL_EXAGGERATION.get();
		} else if (gpxItem != null) {
			Double exaggeration = getAppearanceParameter(gpxItem, dirItem, ADDITIONAL_EXAGGERATION);
			if (exaggeration != null) {
				return exaggeration.floatValue();
			}
		}
		return gpxFile.getAdditionalExaggeration();
	}

	public float getElevationMeters(@NonNull GpxFile gpxFile, @Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem) {
		TrackDrawInfo drawInfo = getTrackDrawInfoForTrack(gpxFile);
		if (drawInfo != null) {
			return drawInfo.getElevationMeters();
		} else if (gpxFile.isShowCurrentTrack()) {
			return settings.CURRENT_TRACK_ELEVATION_METERS.get();
		} else if (gpxItem != null) {
			Double elevation = getAppearanceParameter(gpxItem, dirItem, ELEVATION_METERS);
			if (elevation != null) {
				return elevation.floatValue();
			}
		}
		return gpxFile.getElevationMeters();
	}

	@Nullable
	public String getTrackWidth(@NonNull GpxFile gpxFile, @Nullable String defaultWidth, @Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem) {
		String width = null;
		TrackDrawInfo drawInfo = getTrackDrawInfoForTrack(gpxFile);
		if (drawInfo != null) {
			width = drawInfo.getWidth();
		} else if (gpxFile.getShowCurrentTrack()) {
			width = settings.CURRENT_TRACK_WIDTH.get();
		} else if (gpxItem != null) {
			width = getAppearanceParameter(gpxItem, dirItem, WIDTH);
		}
		return width != null ? width : gpxFile.getWidth(defaultWidth);
	}

	public int getTrackColor(@NonNull GpxFile gpxFile, @Nullable Integer defaultColor, @Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem) {
		Integer color = null;
		TrackDrawInfo drawInfo = getTrackDrawInfoForTrack(gpxFile);
		if (drawInfo != null) {
			color = drawInfo.getColor();
		} else if (gpxFile.getShowCurrentTrack()) {
			color = settings.CURRENT_TRACK_COLOR.get();
		} else if (gpxItem != null) {
			color = getAppearanceParameter(gpxItem, dirItem, COLOR);
		}
		return color != null ? color : gpxFile.getColor(defaultColor);
	}

	@Nullable
	public String getGradientPaletteName(@NonNull GpxFile gpxFile, @Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem) {
		String gradientPalette = null;
		TrackDrawInfo drawInfo = getTrackDrawInfoForTrack(gpxFile);
		if (drawInfo != null) {
			gradientPalette = drawInfo.getGradientColorName();
		} else if (gpxFile.isShowCurrentTrack()) {
			gradientPalette = settings.CURRENT_GRADIENT_PALETTE.get();
		} else if (gpxItem != null) {
			gradientPalette = getAppearanceParameter(gpxItem, dirItem, COLOR_PALETTE);
		}
		return gradientPalette != null ? gradientPalette : gpxFile.getGradientColorPalette();
	}

	@Nullable
	public String getColoringType(@NonNull GpxFile gpxFile) {
		TrackDrawInfo drawInfo = getTrackDrawInfoForTrack(gpxFile);
		return drawInfo != null ? drawInfo.getColoringType().getName(drawInfo.getRouteInfoAttribute()) : null;
	}

	@NonNull
	public <T> T requireParameter(@NonNull GpxDataItem gpxItem, @NonNull GpxParameter parameter) {
		GpxDirItem dirItem = gpxDbHelper.getGpxDirItem(gpxItem);
		Object value = getAppearanceParameter(gpxItem, dirItem, parameter);
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
	public <T> T getParameter(@NonNull GpxDataItem gpxItem, @NonNull GpxParameter parameter) {
		GpxDirItem dirItem = gpxDbHelper.getGpxDirItem(gpxItem);
		Object value = getAppearanceParameter(gpxItem, dirItem, parameter);
		if (value == null) {
			value = parameter.getDefaultValue();
		}
		return SharedUtil.castGpxParameter(parameter, value);
	}

	@Nullable
	public <T> T getAppearanceParameter(@NonNull File file, @NonNull GpxParameter parameter) {
		GpxDataItem gpxItem = gpxDbHelper.getItem(SharedUtil.kFile(file));
		if (gpxItem != null) {
			GpxDirItem dirItem = gpxDbHelper.getGpxDirItem(gpxItem);
			return getAppearanceParameter(gpxItem, dirItem, parameter);
		}
		return null;
	}

	@Nullable
	public <T> T getAppearanceParameter(@NonNull GpxDataItem gpxItem, @Nullable GpxDirItem dirItem, @NonNull GpxParameter parameter) {
		Object value = gpxItem.getParameter(parameter);
		if (value != null) {
			return SharedUtil.castGpxParameter(parameter, value);
		}
		if (dirItem != null) {
			value = dirItem.getParameter(parameter);
			if (value != null) {
				return SharedUtil.castGpxParameter(parameter, value);
			}
		}
		return null;
	}

	public Gpx3DVisualizationType getTrackVisualizationType(@NonNull GpxFile gpxFile,
			@Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem, boolean selected) {
		return selected ? getTrackVisualizationForTrack(gpxFile, gpxItem, dirItem) : Gpx3DVisualizationType.NONE;
	}

	public Gpx3DWallColorType getTrackWallColorType(@NonNull GpxFile gpxFile,
			@Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem, boolean selected) {
		return selected ? getTrackWallColorType(gpxFile, gpxItem, dirItem) : Gpx3DWallColorType.NONE;
	}

	@NonNull
	public Track3DStyle getTrack3DStyle(@NonNull GpxFile gpxFile, @Nullable GpxDataItem gpxItem,
			@Nullable GpxDirItem dirItem, boolean selected) {
		Gpx3DVisualizationType type = getTrackVisualizationType(gpxFile, gpxItem, dirItem, selected);
		float exaggeration = type != FIXED_HEIGHT ? getTrackExaggeration(gpxFile, gpxItem, dirItem, selected) : 1f;

		return new Track3DStyle(type, getTrackWallColorType(gpxFile, gpxItem, dirItem, selected),
				getTrackLinePositionType(gpxFile, gpxItem, dirItem, selected), exaggeration, getElevationMeters(gpxFile, gpxItem, dirItem, selected));
	}

	public Gpx3DLinePositionType getTrackLinePositionType(@NonNull GpxFile gpxFile,
			@Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem, boolean selected) {
		return selected ? getTrackLinePositionType(gpxFile, gpxItem, dirItem) : Gpx3DLinePositionType.TOP;
	}

	public float getTrackExaggeration(@NonNull GpxFile gpxFile, @Nullable GpxDataItem gpxItem,
			@Nullable GpxDirItem dirItem, boolean selected) {
		return selected ? getAdditionalExaggeration(gpxFile, gpxItem, dirItem) : 1f;
	}

	public float getElevationMeters(@NonNull GpxFile gpxFile, @Nullable GpxDataItem gpxItem,
			@Nullable GpxDirItem dirItem, boolean selected) {
		return selected ? getElevationMeters(gpxFile, gpxItem, dirItem) : 1000;
	}

	public boolean isShowArrowsForTrack(@NonNull GpxFile gpxFile, @Nullable GpxDataItem gpxItem,
			@Nullable GpxDirItem dirItem, boolean selected) {
		return selected && isShowArrowsForTrack(gpxFile, gpxItem, dirItem);
	}

	public boolean isShowStartFinishForTrack(@NonNull GpxFile gpxFile,
			@Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem, boolean selected) {
		return selected && isShowStartFinishForTrack(gpxFile, gpxItem, dirItem);
	}

	public int getTrackColor(@NonNull GpxFile gpxFile, @Nullable Integer defaultColor,
			@Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem, boolean selected) {
		return selected ? getTrackColor(gpxFile, defaultColor, gpxItem, dirItem) : disabledColor;
	}

	@NonNull
	public String getTrackGradientPalette(@NonNull GpxFile gpxFile, @Nullable GpxDataItem gpxItem,
			@Nullable GpxDirItem dirItem, boolean selected) {
		String gradientPaletteName = getGradientPaletteName(gpxFile, gpxItem, dirItem);
		return !Algorithms.isEmpty(gradientPaletteName) ? gradientPaletteName : PaletteUtils.DEFAULT_NAME;
	}

	public String getAvailableOrDefaultColoringType(@NonNull CachedTrack cachedTrack,
			@Nullable GpxDataItem gpxItem, @Nullable GpxDirItem dirItem, boolean selected) {
		if (!selected) {
			return ColoringType.TRACK_SOLID.getName(null);
		}
		GpxFile gpxFile = cachedTrack.getSelectedGpxFile().getGpxFileToDisplay();
		String drawInfoColoringType = getColoringType(gpxFile);
		if (!Algorithms.isEmpty(drawInfoColoringType)) {
			return drawInfoColoringType;
		}

		String defaultColoringType = ColoringType.TRACK_SOLID.getName(null);
		ColoringType coloringType = null;
		String routeInfoAttribute = null;
		boolean isCurrentTrack = gpxFile.isShowCurrentTrack();

		if (isCurrentTrack) {
			coloringType = settings.CURRENT_TRACK_COLORING_TYPE.get();
			routeInfoAttribute = settings.CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE.get();
		} else if (gpxItem != null) {
			coloringType = ColoringType.Companion.requireValueOf(ColoringPurpose.TRACK, gpxItem.getParameter(COLORING_TYPE));
			routeInfoAttribute = ColoringType.Companion.getRouteInfoAttribute(gpxItem.getParameter(COLORING_TYPE));
		}

		if (coloringType == null) {
			return defaultColoringType;
		} else if (!isAvailableInSubscription(app, new ColoringStyle(coloringType, routeInfoAttribute))) {
			return defaultColoringType;
		} else if (cachedTrack.isColoringTypeAvailable(coloringType, routeInfoAttribute)) {
			return coloringType.getName(routeInfoAttribute);
		} else {
			if (!isCurrentTrack) {
				gpxDbHelper.updateDataItemParameter(gpxItem, COLORING_TYPE, defaultColoringType);
			}
			return defaultColoringType;
		}
	}
}