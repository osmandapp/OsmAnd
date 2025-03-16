package net.osmand.plus.helpers;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.SimpleStateChangeListener;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.ColorARGB;
import net.osmand.core.jni.FColorARGB;
import net.osmand.core.jni.GridConfiguration;
import net.osmand.core.jni.GridConfiguration.Format;
import net.osmand.core.jni.GridConfiguration.Projection;
import net.osmand.core.jni.GridMarksProvider;
import net.osmand.core.jni.GridParameters;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.core.jni.TextRasterizer.Style.TextAlignment;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.GridFormat;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;

public class CoordinatesGridHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final OsmandMapTileView mapTileView;

	private GridConfiguration gridConfig;
	private GridMarksProvider marksProvider;

	private GridFormat cachedGridFormat;
	@ColorInt
	private int cachedGridColor;
	@ColorInt
	private int cachedHaloColor;
	private Float cachedTextScale;
	private Boolean cachedGridShow;
	private SimpleStateChangeListener settingsListener;

	public CoordinatesGridHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
		mapTileView = app.getOsmandMap().getMapView();

		settingsListener = this::updateGridSettings;
		settings.SHOW_COORDINATES_GRID.addListener(settingsListener);
		settings.COORDINATE_GRID_FORMAT.addListener(settingsListener);
		settings.COORDINATE_GRID_MIN_ZOOM.addListener(settingsListener);
		settings.COORDINATE_GRID_MAX_ZOOM.addListener(settingsListener);
		settings.COORDINATES_FORMAT.addListener(settingsListener);
		settings.TEXT_SCALE.addListener(settingsListener);
	}

	public void updateGridSettings() {
		MapRendererView mapRenderer = mapTileView.getMapRenderer();
		if (mapRenderer != null) {
			updateGridSettings(mapRenderer);
		}
	}

	public void updateGridSettings(@NonNull MapRendererView mapRenderer) {
		boolean updateAppearance;
		ApplicationMode appMode = settings.getApplicationMode();
		if (gridConfig == null) {
			gridConfig = new GridConfiguration();
			initVariables(appMode);
			setupMapZoomListener();
			updateAppearance = true;
		} else {
			updateAppearance = updateVariables(appMode);
		}
		if (updateAppearance) {
			cleanupMarksProvider(mapRenderer);
			updateGridAppearance();
		}
		boolean show = shouldShowGrid(appMode, cachedGridFormat, getCurrentZoom());
		if (cachedGridShow != show || updateAppearance) {
			cachedGridShow = show;
			updateGridVisibility(mapRenderer, cachedGridShow);
		}
	}

	private void initVariables(@NonNull ApplicationMode appMode) {
		cachedGridFormat = getGridFormat(appMode);
		cachedGridColor = getGridColor(appMode);
		cachedHaloColor = getHaloColor(appMode);
		cachedTextScale = getTextScale(appMode);
		cachedGridShow = shouldShowGrid(appMode, cachedGridFormat, getCurrentZoom());
	}

	private void setupMapZoomListener() {
		mapTileView.addManualZoomChangeListener(manual -> updateGridSettings());
	}

	private boolean updateVariables(@NonNull ApplicationMode appMode) {
		boolean updated = false;
		GridFormat newGridFormat = getGridFormat(appMode);
		if (cachedGridFormat != newGridFormat) {
			cachedGridFormat = newGridFormat;
			updated = true;
		}
		int newGridColor = getGridColor(appMode);
		if (cachedGridColor != newGridColor) {
			cachedGridColor = newGridColor;
			updated = true;
		}
		int newHaloColor = getHaloColor(appMode);
		if (cachedHaloColor != newHaloColor) {
			cachedHaloColor = newHaloColor;
			updated = true;
		}
		float newTextScale = getTextScale(appMode);
		if (Math.abs(cachedTextScale - newTextScale) >= 0.0001f) {
			cachedTextScale = newTextScale;
			updated = true;
		}
		return updated;
	}

	private void updateGridAppearance() {
		Projection projection = cachedGridFormat.getProjection();
		Format format = cachedGridFormat.getFormat();
		FColorARGB color = NativeUtilities.createFColorARGB(cachedGridColor);
		FColorARGB haloColor = NativeUtilities.createFColorARGB(cachedHaloColor);

		gridConfig.setPrimaryProjection(projection);
		gridConfig.setPrimaryFormat(format);
		gridConfig.setPrimaryColor(color);

		gridConfig.setSecondaryProjection(projection);
		gridConfig.setSecondaryFormat(format);
		gridConfig.setSecondaryColor(color);

		marksProvider = new GridMarksProvider();
		TextRasterizer.Style primaryStyle = createMarksStyle(color, haloColor, TextAlignment.Under);
		TextRasterizer.Style secondaryStyle = createMarksStyle(color, haloColor, null);

		marksProvider.setPrimaryStyle(primaryStyle, 2.0f * cachedTextScale);
		String equator = app.getString(R.string.equator);
		String primeMeridian = app.getString(R.string.prime_meridian);
		String meridian180 = app.getString(R.string.meridian_180);
		marksProvider.setPrimary(false, equator, "", primeMeridian, meridian180);

		marksProvider.setSecondaryStyle(secondaryStyle, 2.0f * cachedTextScale);
		if (projection == Projection.UTM) {
			marksProvider.setSecondary(true, "", "", "", "");
		} else {
			marksProvider.setSecondary(true, "N", "S", "E", "W");
		}
	}

	@NonNull
	private TextRasterizer.Style createMarksStyle(@NonNull FColorARGB color,
	                                              @NonNull FColorARGB haloColor,
	                                              @Nullable TextAlignment textAlignment) {
		TextRasterizer.Style style = new TextRasterizer.Style();
		style.setColor(new ColorARGB(color));
		style.setHaloColor(new ColorARGB(haloColor));
		style.setHaloRadius((int) (3.0f * cachedTextScale));
		style.setSize(16.0f * cachedTextScale);
		style.setBold(true);
		if (textAlignment != null) {
			style.setTextAlignment(textAlignment);
		}
		return style;
	}

	private void updateGridVisibility(@NonNull MapRendererView mapRenderer, boolean visible) {
		gridConfig.setPrimaryGrid(visible);
		gridConfig.setSecondaryGrid(visible);
		mapRenderer.setGridConfiguration(gridConfig);
		if (visible) {
			mapRenderer.addSymbolsProvider(marksProvider);
		} else {
			mapRenderer.removeSymbolsProvider(marksProvider);
		}
	}

	private void cleanupMarksProvider(@NonNull MapRendererView mapRenderer) {
		if (marksProvider != null) {
			mapRenderer.removeSymbolsProvider(marksProvider);
			marksProvider = null;
		}
	}

	private int getCurrentZoom() {
		return mapTileView.getZoom();
	}

	private float getTextScale(@NonNull ApplicationMode appMode) {
		OsmandMapTileView mapTileView = app.getOsmandMap().getMapView();
		return settings.TEXT_SCALE.getModeValue(appMode) * mapTileView.getDensity();
	}

	private boolean shouldShowGrid(@NonNull ApplicationMode appMode,
	                               @NonNull GridFormat gridFormat, int zoom) {
		Limits<Integer> limits = getZoomLevelsWithRestrictions(appMode, gridFormat);
		return isEnabled(appMode) && zoom >= limits.min() && zoom <= limits.max();
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
	public int getGridColor(@NonNull ApplicationMode appMode) {
		// temporally use predefined color, it will be set from the preferences in the future
		return Color.parseColor("#FF1A00CC");
	}

	@ColorInt
	public int getHaloColor(@NonNull ApplicationMode appMode) {
		// temporally use predefined color, it will be set from the preferences in the future
		return Color.parseColor("#80FFFFFF");
	}

	@NonNull
	public Limits<Integer> getZoomLevelsWithRestrictions() {
		return getZoomLevelsWithRestrictions(settings.getApplicationMode());
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
		int min = Math.max(supported.min(), selected.min());
		int max = Math.min(supported.max(), selected.max());
		return new Limits<>(min, max);
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
		GridConfiguration config = new GridConfiguration();
		Projection projection = gridFormat.getProjection();
		config.setPrimaryProjection(projection);
		config.setSecondaryProjection(projection);
		Format format = gridFormat.getFormat();
		config.setPrimaryFormat(format);
		config.setSecondaryFormat(format);

		GridParameters params = config.getGridParameters();
		ZoomLevel min = params.getMinZoom();
		ZoomLevel max = params.getMaxZoomForMixed();
		return new Limits<>(min.swigValue(), max.swigValue());
	}
}
