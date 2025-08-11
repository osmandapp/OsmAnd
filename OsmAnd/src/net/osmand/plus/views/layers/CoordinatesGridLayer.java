package net.osmand.plus.views.layers;

import android.graphics.Canvas;
import android.view.View;

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
import net.osmand.core.jni.ZoomLevel;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.GridFormat;
import net.osmand.plus.settings.enums.GridLabelsPosition;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.ViewChangeProvider.ViewChangeListener;
import net.osmand.plus.views.controls.VerticalWidgetPanel;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

import java.util.Objects;

public class CoordinatesGridLayer extends OsmandMapLayer {

	private static final float DEFAULT_MARGIN_FACTOR = 8.0f;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final CoordinatesGridSettings gridSettings;
	private final OsmandMapTileView mapTileView;

	private GridConfiguration gridConfig;
	private GridMarksProvider marksProvider;

	private GridFormat cachedGridFormat;
	private Limits<Integer> cachedZoomLimits;
	private GridLabelsPosition cachedLabelsPosition;
	@ColorInt private int cachedGridColorDay;
	@ColorInt private int cachedGridColorNight;
	private Float cachedTextScale;
	private Boolean cachedGridEnabled;
	private boolean cachedNightMode;
	private final StateChangedListener settingsListener;

	private final ViewHeightChangeListener panelsHeightListener;
	private VerticalWidgetPanel topWidgetsPanel;
	private VerticalWidgetPanel bottomWidgetsPanel;
	private boolean marginFactorUpdateNeeded = false;

	public CoordinatesGridLayer(@NonNull OsmandApplication app) {
		super(app);
		this.app = app;
		settings = app.getSettings();
		gridSettings = new CoordinatesGridSettings(app);
		mapTileView = app.getOsmandMap().getMapView();

		panelsHeightListener = () -> {
			marginFactorUpdateNeeded = true;
			updateGridSettings();
		};

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
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity != null) {
			topWidgetsPanel = mapActivity.findViewById(R.id.top_widgets_panel);
			bottomWidgetsPanel = mapActivity.findViewById(R.id.map_bottom_widgets_panel);
			if (topWidgetsPanel != null) {
				topWidgetsPanel.addViewChangeListener(panelsHeightListener);
			}
			if (bottomWidgetsPanel != null) {
				bottomWidgetsPanel.addViewChangeListener(panelsHeightListener);
			}
		} else {
			if (topWidgetsPanel != null) {
				topWidgetsPanel.removeViewChangeListener(panelsHeightListener);
			}
			if (bottomWidgetsPanel != null) {
				bottomWidgetsPanel.removeViewChangeListener(panelsHeightListener);
			}
			topWidgetsPanel = null;
			bottomWidgetsPanel = null;
		}
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
		ApplicationMode appMode = settings.getApplicationMode();
		boolean updateAppearance = false;
		boolean zoomLevelsUpdated = false;
		boolean marginFactorUpdated = false;

		boolean show = gridSettings.isEnabled(appMode);
		if (show) {
			if (gridConfig == null || !mapRenderer.hasSymbolsProvider(marksProvider)) {
				gridConfig = new GridConfiguration();
				initVariables(appMode);
				updateAppearance = true;
			} else {
				updateAppearance = updateVariables(appMode);
				zoomLevelsUpdated = updateZoomLevels(appMode);
				marginFactorUpdated = updateLabelsMarginFactor();
			}
			if (updateAppearance) {
				cleanupMarksProvider(mapRenderer);
				updateGridAppearance();
			}
		}
		boolean updated = updateAppearance || zoomLevelsUpdated || marginFactorUpdated;
		if (gridConfig != null && (cachedGridEnabled != show || updated)) {
			cachedGridEnabled = show;
			updateGridVisibility(mapRenderer, cachedGridEnabled);
		}
	}

	private void initVariables(@NonNull ApplicationMode appMode) {
		cachedGridFormat = gridSettings.getGridFormat(appMode);
		cachedLabelsPosition = gridSettings.getGridLabelsPosition(appMode);
		cachedGridColorDay = gridSettings.getGridColor(appMode, false);
		cachedGridColorNight = gridSettings.getGridColor(appMode, true);
		cachedTextScale = gridSettings.getTextScale(appMode);
		cachedGridEnabled = gridSettings.isEnabled(appMode);
		cachedZoomLimits = gridSettings.getZoomLevelsWithRestrictions(appMode, cachedGridFormat);
		cachedNightMode = isNightMode();
	}

	private boolean updateVariables(@NonNull ApplicationMode appMode) {
		boolean updated = false;
		GridFormat newGridFormat = gridSettings.getGridFormat(appMode);
		if (cachedGridFormat != newGridFormat) {
			cachedGridFormat = newGridFormat;
			updated = true;
		}
		int newGridColorDay = gridSettings.getGridColor(appMode, false);
		if (cachedGridColorDay != newGridColorDay) {
			cachedGridColorDay = newGridColorDay;
			updated = true;
		}
		int newGridColorNight = gridSettings.getGridColor(appMode, true);
		if (cachedGridColorNight != newGridColorNight) {
			cachedGridColorNight = newGridColorNight;
			updated = true;
		}
		float newTextScale = gridSettings.getTextScale(appMode);
		if (Math.abs(cachedTextScale - newTextScale) >= 0.0001f) {
			cachedTextScale = newTextScale;
			updated = true;
		}
		GridLabelsPosition newLabelsPosition = gridSettings.getGridLabelsPosition(appMode);
		if (cachedLabelsPosition != newLabelsPosition) {
			cachedLabelsPosition = newLabelsPosition;
			updated = true;
		}
		boolean newNightMode = isNightMode();
		if (cachedNightMode != newNightMode) {
			cachedNightMode = newNightMode;
			updated = true;
		}
		return updated;
	}

	private void updateGridAppearance() {
		Format format = cachedGridFormat.getFormat();
		Projection projection = cachedGridFormat.getProjection();
		ZoomLevel minZoom = ZoomLevel.swigToEnum(cachedZoomLimits.min());
		ZoomLevel maxZoom = ZoomLevel.swigToEnum(cachedZoomLimits.max());

		int colorInt = cachedNightMode ? cachedGridColorNight : cachedGridColorDay;
		FColorARGB color = NativeUtilities.createFColorARGB(colorInt);
		int haloColorInt = ColorUtilities.getContrastColor(app, colorInt, true);
		FColorARGB haloColor = NativeUtilities.createFColorARGB(haloColorInt);

		gridConfig.setPrimaryProjection(projection);
		gridConfig.setPrimaryFormat(format);
		gridConfig.setPrimaryColor(color);
		gridConfig.setPrimaryMinZoomLevel(minZoom);
		gridConfig.setPrimaryMaxZoomLevel(maxZoom);

		gridConfig.setSecondaryProjection(projection);
		gridConfig.setSecondaryFormat(format);
		gridConfig.setSecondaryColor(color);
		gridConfig.setSecondaryMinZoomLevel(minZoom);
		gridConfig.setSecondaryMaxZoomLevel(maxZoom);

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

	private boolean updateZoomLevels(@NonNull ApplicationMode appMode) {
		Limits<Integer> newZoomLimits = gridSettings.getZoomLevelsWithRestrictions(appMode, cachedGridFormat);
		if (!Objects.equals(cachedZoomLimits, newZoomLimits)) {
			cachedZoomLimits = newZoomLimits;
			if (gridConfig != null) {
				ZoomLevel minZoom = ZoomLevel.swigToEnum(cachedZoomLimits.min());
				ZoomLevel maxZoom = ZoomLevel.swigToEnum(cachedZoomLimits.max());
				gridConfig.setPrimaryMinZoomLevel(minZoom);
				gridConfig.setPrimaryMaxZoomLevel(maxZoom);
				gridConfig.setSecondaryMinZoomLevel(minZoom);
				gridConfig.setSecondaryMaxZoomLevel(maxZoom);
				return true;
			}
		}
		return false;
	}

	private boolean updateLabelsMarginFactor() {
		if (marginFactorUpdateNeeded) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null && topWidgetsPanel != null && bottomWidgetsPanel != null) {
				float top = DEFAULT_MARGIN_FACTOR;
				float bottom = DEFAULT_MARGIN_FACTOR;
				float screenHeight = AndroidUtils.getScreenHeight(mapActivity);

				if (topWidgetsPanel.getVisibility() == View.VISIBLE) {
					int topWidgetsHeight = topWidgetsPanel.getMeasuredHeight();
					if (topWidgetsHeight > 0) {
						float calculated = (screenHeight * 0.9f) / topWidgetsHeight;
						top = Math.min(calculated, DEFAULT_MARGIN_FACTOR);
					}
				}
				if (bottomWidgetsPanel.getVisibility() == View.VISIBLE) {
					int bottomWidgetsHeight = bottomWidgetsPanel.getMeasuredHeight();
					if (bottomWidgetsHeight > 0) {
						float calculated = screenHeight / bottomWidgetsHeight;
						bottom = Math.min(calculated, DEFAULT_MARGIN_FACTOR);
					}
				}
				gridConfig.setPrimaryTopMarginFactor(top);
				gridConfig.setSecondaryTopMarginFactor(top);
				gridConfig.setPrimaryBottomMarginFactor(bottom);
				gridConfig.setSecondaryBottomMarginFactor(bottom);
				marginFactorUpdateNeeded = false;
				return true;
			}
		}
		return false;
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

	private boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	private interface ViewHeightChangeListener extends ViewChangeListener {

		@Override
		default void onSizeChanged(@NonNull View view, int w, int h, int oldWidth, int oldHeight) {
			if (h != oldHeight) {
				onViewHeightChanged();
			}
		}

		default void onVisibilityChanged(@NonNull View view, int visibility) {
			onViewHeightChanged();
		}

		void onViewHeightChanged();
	}
}