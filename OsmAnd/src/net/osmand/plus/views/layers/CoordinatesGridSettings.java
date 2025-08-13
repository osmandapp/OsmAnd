package net.osmand.plus.views.layers;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.math.MathUtils;

import net.osmand.core.jni.GridConfiguration;
import net.osmand.core.jni.GridConfiguration.Format;
import net.osmand.core.jni.GridConfiguration.Projection;
import net.osmand.core.jni.GridParameters;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.GridFormat;
import net.osmand.plus.settings.enums.GridLabelsPosition;
import net.osmand.plus.views.OsmandMapTileView;

public class CoordinatesGridSettings {

	public static final int SUPPORTED_MAX_ZOOM = 22;

	private final OsmandApplication app;
	private final OsmandSettings settings;

	public CoordinatesGridSettings(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
	}

	public void toggleEnable() {
		setEnabled(!isEnabled());
	}

	public boolean isEnabled() {
		return isEnabled(settings.getApplicationMode());
	}

	public boolean isEnabled(@NonNull ApplicationMode appMode) {
		return settings.SHOW_COORDINATES_GRID.getModeValue(appMode);
	}

	public void setEnabled(boolean enabled) {
		setEnabled(settings.getApplicationMode(), enabled);
	}

	public void setEnabled(@NonNull ApplicationMode appMode, boolean enabled) {
		settings.SHOW_COORDINATES_GRID.setModeValue(appMode, enabled);
	}

	@NonNull
	public GridFormat getGridFormat(@NonNull ApplicationMode appMode) {
		return settings.COORDINATE_GRID_FORMAT.getModeValue(appMode);
	}

	public void setGridFormat(@NonNull ApplicationMode appMode, @NonNull GridFormat format) {
		settings.COORDINATE_GRID_FORMAT.setModeValue(appMode, format);
	}

	@ColorInt
	public int getGridColorDay() {
		return getGridColor(false);
	}

	@ColorInt
	public int getGridColorNight() {
		return getGridColor(true);
	}

	@ColorInt
	public int getGridColor(boolean nightMode) {
		return getGridColor(settings.getApplicationMode(), nightMode);
	}

	@ColorInt
	public int getGridColor(@NonNull ApplicationMode appMode, boolean nightMode) {
		return nightMode
				? settings.COORDINATES_GRID_COLOR_NIGHT.getModeValue(appMode)
				: settings.COORDINATES_GRID_COLOR_DAY.getModeValue(appMode);
	}

	public void setGridColor(@NonNull ApplicationMode appMode, @ColorInt int color, boolean nightMode) {
		if (nightMode) {
			settings.COORDINATES_GRID_COLOR_NIGHT.setModeValue(appMode, color);
		} else {
			settings.COORDINATES_GRID_COLOR_DAY.setModeValue(appMode, color);
		}
	}

	public void resetGridColors(@NonNull ApplicationMode appMode) {
		settings.COORDINATES_GRID_COLOR_DAY.resetModeToDefault(appMode);
		settings.COORDINATES_GRID_COLOR_NIGHT.resetModeToDefault(appMode);
	}

	@NonNull
	public GridLabelsPosition getGridLabelsPosition(@NonNull ApplicationMode appMode) {
		return settings.COORDINATES_GRID_LABELS_POSITION.getModeValue(appMode);
	}

	public void setGridLabelsPosition(@NonNull ApplicationMode appMode, @NonNull GridLabelsPosition position) {
		settings.COORDINATES_GRID_LABELS_POSITION.setModeValue(appMode, position);
	}

	@NonNull
	public Limits<Integer> getZoomLevelsWithRestrictions(@NonNull ApplicationMode appMode) {
		return getZoomLevelsWithRestrictions(appMode, getGridFormat(appMode));
	}

	@NonNull
	public Limits<Integer> getZoomLevelsWithRestrictions(@NonNull ApplicationMode appMode,
	                                                     @NonNull GridFormat gridFormat) {
		Limits<Integer> selected = getZoomLevels(appMode);
		Limits<Integer> supported = getSupportedZoomLevels(gridFormat);
		int min = MathUtils.clamp(selected.min(), supported.min(), supported.max());
		int max = MathUtils.clamp(selected.max(), supported.min(), supported.max());
		return new Limits<>(min, max);
	}

	@NonNull
	public Limits<Integer> getZoomLevels() {
		return getZoomLevels(settings.getApplicationMode());
	}

	@NonNull
	public Limits<Integer> getZoomLevels(@NonNull ApplicationMode appMode) {
		int min = settings.COORDINATE_GRID_MIN_ZOOM.getModeValue(appMode);
		int max = settings.COORDINATE_GRID_MAX_ZOOM.getModeValue(appMode);
		return new Limits<>(min, max);
	}

	public void setZoomLevels(@NonNull ApplicationMode appMode, @NonNull Limits<Integer> zoomLevels) {
		settings.COORDINATE_GRID_MIN_ZOOM.setModeValue(appMode, zoomLevels.min());
		settings.COORDINATE_GRID_MAX_ZOOM.setModeValue(appMode, zoomLevels.max());
	}

	public void resetZoomLevels(@NonNull ApplicationMode appMode) {
		settings.COORDINATE_GRID_MIN_ZOOM.resetModeToDefault(appMode);
		settings.COORDINATE_GRID_MAX_ZOOM.resetModeToDefault(appMode);
	}

	@NonNull
	public Limits<Integer> getSupportedZoomLevels() {
		return getSupportedZoomLevels(settings.getApplicationMode());
	}

	@NonNull
	public Limits<Integer> getSupportedZoomLevels(@NonNull ApplicationMode appMode) {
		return getSupportedZoomLevels(getGridFormat(appMode));
	}

	@NonNull
	public Limits<Integer> getSupportedZoomLevels(@NonNull GridFormat gridFormat) {
		int minZoom = 1;
		if (isGridSupported(app)) {
			GridConfiguration config = new GridConfiguration();
			Projection projection = gridFormat.getProjection();
			config.setPrimaryProjection(projection);
			config.setSecondaryProjection(projection);

			Format format = gridFormat.getFormat();
			config.setPrimaryFormat(format);
			config.setSecondaryFormat(format);
			config.setProjectionParameters();

			GridParameters params = config.getGridParameters();
			ZoomLevel min = params.getMinZoom();
			minZoom = min.swigValue();
		}
		return new Limits<>(minZoom, SUPPORTED_MAX_ZOOM);
	}

	public float getTextScale(@NonNull ApplicationMode appMode) {
		OsmandMapTileView mapTileView = app.getOsmandMap().getMapView();
		return settings.TEXT_SCALE.getModeValue(appMode) * mapTileView.getDensity();
	}

	public static boolean isGridSupported(@NonNull OsmandApplication app) {
		return app.getSettings().USE_OPENGL_RENDER.get() && Version.isOpenGlAvailable(app);
	}
}
