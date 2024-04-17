package net.osmand.plus.track.helpers;

import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.SHOW_ARROWS;
import static net.osmand.gpx.GpxParameter.SHOW_START_FINISH;
import static net.osmand.gpx.GpxParameter.WIDTH;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GpxParameter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
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