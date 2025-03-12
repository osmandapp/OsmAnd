package net.osmand.plus.helpers;

import androidx.annotation.NonNull;

import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.ColorARGB;
import net.osmand.core.jni.FColorARGB;
import net.osmand.core.jni.GridConfiguration;
import net.osmand.core.jni.GridConfiguration.Projection;
import net.osmand.core.jni.GridMarksProvider;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.GridFormat;
import net.osmand.plus.views.OsmandMapTileView;

public class CoordinatesGridHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private GridMarksProvider gridMarksProvider;
	private StateChangedListener gridSettingsListener;

	public CoordinatesGridHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();

		gridSettingsListener = this::onUpdateGridSettings;
		settings.SHOW_COORDINATES_GRID.addListener(gridSettingsListener);
		settings.COORDINATE_GRID_FORMAT.addListener(gridSettingsListener);
		settings.COORDINATE_GRID_MIN_ZOOM.addListener(gridSettingsListener);
		settings.COORDINATE_GRID_MAX_ZOOM.addListener(gridSettingsListener);
		settings.COORDINATES_FORMAT.addListener(gridSettingsListener);
	}

	public <T> void onUpdateGridSettings(@NonNull T changed) {
		updateGridSettings();
	}

	public void updateGridSettings() {
		OsmandMapTileView mapTileView = app.getOsmandMap().getMapView();
		MapRendererView mapRenderer = mapTileView.getMapRenderer();
		if (mapRenderer != null) {
			applyGridSettings(mapRenderer);
		}
	}

	public void applyGridSettings(@NonNull MapRendererView mapRenderer) {
		OsmandMapTileView mapTileView = app.getOsmandMap().getMapView();
		int zoom = mapTileView.getZoom();
		int minZoom = settings.COORDINATE_GRID_MIN_ZOOM.get();
		int maxZoom = settings.COORDINATE_GRID_MAX_ZOOM.get();
		boolean isAvailableZoom = zoom >= minZoom && zoom <= maxZoom;
		boolean show = isAvailableZoom && settings.SHOW_COORDINATES_GRID.get();

		GridFormat gridFormat = settings.COORDINATE_GRID_FORMAT.get();
		FColorARGB color = new FColorARGB(1.0f, 0.1f, 0.0f, 0.8f);
		GridConfiguration gridConfiguration = new GridConfiguration();
		gridConfiguration.setPrimaryGrid(show);
		gridConfiguration.setPrimaryProjection(gridFormat.getProjection());
		gridConfiguration.getGridParameters().getMinZoom();
		gridConfiguration.setPrimaryColor(color);
		gridConfiguration.setSecondaryGrid(show);
		gridConfiguration.setSecondaryProjection(gridFormat.getProjection());
		gridConfiguration.setSecondaryFormat(gridFormat.getFormat());
		gridConfiguration.setSecondaryColor(color);
		mapRenderer.setGridConfiguration(gridConfiguration);
		if (gridMarksProvider != null) {
			mapRenderer.removeSymbolsProvider(gridMarksProvider);
			gridMarksProvider = null;
		}
		if (show) {
			float textScale = settings.TEXT_SCALE.get() * mapTileView.getDensity();
			gridMarksProvider = new GridMarksProvider();
			FColorARGB haloColor = new FColorARGB(0.5f, 1.0f, 1.0f, 1.0f);
			TextRasterizer.Style primaryMarksStyle = new TextRasterizer.Style();
			primaryMarksStyle.setColor(new ColorARGB(color));
			primaryMarksStyle.setHaloColor(new ColorARGB(haloColor));
			primaryMarksStyle.setHaloRadius((int) (3.0f * textScale));
			primaryMarksStyle.setSize(16.0f * textScale);
			primaryMarksStyle.setBold(true);
			primaryMarksStyle.setTextAlignment(TextRasterizer.Style.TextAlignment.Under);
			gridMarksProvider.setPrimaryStyle(primaryMarksStyle, 2.0f * textScale);
			gridMarksProvider.setPrimary(false, "Equator", "", "Prime meridian", "180th meridian");
			TextRasterizer.Style secondaryMarksStyle = new TextRasterizer.Style();
			secondaryMarksStyle.setColor(new ColorARGB(color));
			secondaryMarksStyle.setHaloColor(new ColorARGB(haloColor));
			secondaryMarksStyle.setHaloRadius((int) (3.0f * textScale));
			secondaryMarksStyle.setSize(16.0f * textScale);
			secondaryMarksStyle.setBold(true);
			gridMarksProvider.setSecondaryStyle(secondaryMarksStyle, 2.0f * textScale);
			if (gridFormat.getProjection() == Projection.UTM) {
				gridMarksProvider.setSecondary(true, "", "", "", "");
			} else {
				gridMarksProvider.setSecondary(true, "N", "S", "E", "W");
			}
			mapRenderer.addSymbolsProvider(gridMarksProvider);
		}
	}
}
