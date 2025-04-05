package net.osmand.plus.views.layers;

import android.graphics.Canvas;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.ColorARGB;
import net.osmand.core.jni.FColorARGB;
import net.osmand.core.jni.GridConfiguration;
import net.osmand.core.jni.GridConfiguration.Format;
import net.osmand.core.jni.GridConfiguration.Projection;
import net.osmand.core.jni.GridMarksProvider;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.core.jni.TextRasterizer.Style.TextAlignment;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.GridFormat;
import net.osmand.plus.settings.enums.GridLabelsPosition;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

public class CoordinatesGridLayer extends OsmandMapLayer {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final CoordinatesGridHelper gridHelper;
	private final OsmandMapTileView mapTileView;

	private GridConfiguration gridConfig;
	private GridMarksProvider marksProvider;

	private GridFormat cachedGridFormat;
	private GridLabelsPosition cachedLabelsPosition;
	@ColorInt private int cachedGridColorDay;
	@ColorInt private int cachedGridColorNight;
	private Float cachedTextScale;
	private Boolean cachedGridShow;
	private boolean cachedNightMode;
	private StateChangedListener settingsListener;

	public CoordinatesGridLayer(@NonNull OsmandApplication app) {
		super(app);
		this.app = app;
		settings = app.getSettings();
		gridHelper = new CoordinatesGridHelper(app);
		mapTileView = app.getOsmandMap().getMapView();

		settingsListener = this::onPreferenceChange;
		settings.SHOW_COORDINATES_GRID.addListener(settingsListener);
		settings.COORDINATE_GRID_FORMAT.addListener(settingsListener);
		settings.COORDINATE_GRID_MIN_ZOOM.addListener(settingsListener);
		settings.COORDINATE_GRID_MAX_ZOOM.addListener(settingsListener);
		settings.COORDINATES_FORMAT.addListener(settingsListener);
		settings.COORDINATES_GRID_LABELS_POSITION.addListener(settingsListener);
		settings.COORDINATES_GRID_COLOR_DAY.addListener(settingsListener);
		settings.COORDINATES_GRID_COLOR_NIGHT.addListener(settingsListener);
		settings.TEXT_SCALE.addListener(settingsListener);
		setInvalidated(true);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		// do nothing
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		updateGridSettings();
	}

	private <T> void onPreferenceChange(@Nullable T newValue) {
		updateGridSettings();
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
		if (gridConfig == null || !mapRenderer.hasSymbolsProvider(marksProvider)) {
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
		boolean show = gridHelper.shouldShowGrid(appMode, cachedGridFormat, getCurrentZoom());
		if (cachedGridShow != show || updateAppearance) {
			cachedGridShow = show;
			updateGridVisibility(mapRenderer, cachedGridShow);
		}
		setInvalidated(false);
	}

	private void initVariables(@NonNull ApplicationMode appMode) {
		cachedGridFormat = gridHelper.getGridFormat(appMode);
		cachedLabelsPosition = gridHelper.getGridLabelsPosition(appMode);
		cachedGridColorDay = gridHelper.getGridColor(appMode, false);
		cachedGridColorNight = gridHelper.getGridColor(appMode, true);
		cachedTextScale = gridHelper.getTextScale(appMode);
		cachedNightMode = isNightMode();
		cachedGridShow = gridHelper.shouldShowGrid(appMode, cachedGridFormat, getCurrentZoom());
	}

	private boolean updateVariables(@NonNull ApplicationMode appMode) {
		boolean updated = false;
		GridFormat newGridFormat = gridHelper.getGridFormat(appMode);
		if (cachedGridFormat != newGridFormat) {
			cachedGridFormat = newGridFormat;
			updated = true;
		}
		int newGridColorDay = gridHelper.getGridColor(appMode, false);
		if (cachedGridColorDay != newGridColorDay) {
			cachedGridColorDay = newGridColorDay;
			updated = true;
		}
		int newGridColorNight = gridHelper.getGridColor(appMode, true);
		if (cachedGridColorNight != newGridColorNight) {
			cachedGridColorNight = newGridColorNight;
			updated = true;
		}
		float newTextScale = gridHelper.getTextScale(appMode);
		if (Math.abs(cachedTextScale - newTextScale) >= 0.0001f) {
			cachedTextScale = newTextScale;
			updated = true;
		}
		GridLabelsPosition newLabelsPosition = gridHelper.getGridLabelsPosition(appMode);
		if (cachedLabelsPosition != newLabelsPosition) {
			cachedLabelsPosition = newLabelsPosition;
			updated = true;
		}
		boolean newNightMode = isNightMode();
		if (cachedNightMode != newNightMode) {
			cachedNightMode = newNightMode;
			updated = true;
		}
		return updated || invalidated;
	}

	private void updateGridAppearance() {
		Format format = cachedGridFormat.getFormat();
		Projection projection = cachedGridFormat.getProjection();

		int colorInt = cachedNightMode ? cachedGridColorNight : cachedGridColorDay;
		FColorARGB color = NativeUtilities.createFColorARGB(colorInt);
		int haloColorInt = ColorUtilities.getContrastColor(app, colorInt, true);
		FColorARGB haloColor = NativeUtilities.createFColorARGB(haloColorInt);

		gridConfig.setPrimaryProjection(projection);
		gridConfig.setPrimaryFormat(format);
		gridConfig.setPrimaryColor(color);

		gridConfig.setSecondaryProjection(projection);
		gridConfig.setSecondaryFormat(format);
		gridConfig.setSecondaryColor(color);

		marksProvider = new GridMarksProvider();
		TextRasterizer.Style primaryStyle = createMarksStyle(color, haloColor, TextAlignment.Under);
		TextRasterizer.Style secondaryStyle = createMarksStyle(color, haloColor, null);

		marksProvider.setPrimaryStyle(primaryStyle, 2.0f * cachedTextScale, true);
		String equator = app.getString(R.string.equator);
		String primeMeridian = app.getString(R.string.prime_meridian);
		String meridian180 = app.getString(R.string.meridian_180);
		marksProvider.setPrimary(false, equator, "", primeMeridian, meridian180);

		boolean drawLabelsInCenter = cachedLabelsPosition == GridLabelsPosition.CENTER;
		marksProvider.setSecondaryStyle(secondaryStyle, 2.0f * cachedTextScale, drawLabelsInCenter);
		if (cachedGridFormat.needSuffixes()) {
			marksProvider.setSecondary(true, "N", "S", "E", "W");
		} else {
			marksProvider.setSecondary(true, "", "", "", "");
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

	private void setupMapZoomListener() {
		OsmandMapTileView mapTileView = app.getOsmandMap().getMapView();
		mapTileView.addMapZoomChangeListener(manual -> setInvalidated(true));
	}

	private int getCurrentZoom() {
		return mapTileView.getZoom();
	}

	private boolean isNightMode() {
		return app.getDaynightHelper().isNightMode();
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}
}