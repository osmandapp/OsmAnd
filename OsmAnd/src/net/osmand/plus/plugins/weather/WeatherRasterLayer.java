package net.osmand.plus.plugins.weather;

import android.content.Context;
import android.graphics.Canvas;

import androidx.annotation.NonNull;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.BandIndexList;
import net.osmand.core.jni.MapLayerConfiguration;
import net.osmand.core.jni.WeatherBand;
import net.osmand.core.jni.WeatherRasterLayerProvider;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.BaseMapLayer;
import net.osmand.util.Algorithms;

import java.util.List;

public class WeatherRasterLayer extends BaseMapLayer {

	private final OsmandSettings settings;
	private final WeatherPlugin weatherPlugin;

	private WeatherRasterLayerProvider provider;

	private final WeatherLayer weatherLayer;
	private boolean weatherEnabledCached;
	private List<WeatherInfoType> enabledLayersCached;
	private int bandsSettingsVersionCached;
	private long dateTime;
	private long cachedDateTime;

	public enum WeatherLayer {
		LOW,
		HIGH
	}

	public WeatherRasterLayer(@NonNull Context context, @NonNull WeatherLayer weatherLayer) {
		super(context);
		this.settings = getApplication().getSettings();
		this.weatherPlugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		this.weatherLayer = weatherLayer;
		this.dateTime = System.currentTimeMillis();
	}

	public long getDateTime() {
		return dateTime;
	}

	public void setDateTime(long dateTime) {
		this.dateTime = dateTime;
	}

	@Override
	public void destroyLayer() {
		super.destroyLayer();
		resetLayerProvider();
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);
	}

	@Override
	public int getMaximumShownMapZoom() {
		return 22;
	}

	@Override
	public int getMinimumShownMapZoom() {
		return 1;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tilesRect, DrawSettings drawSettings) {
	}

	private void recreateLayerProvider(@NonNull MapRendererView mapRenderer, @NonNull WeatherTileResourcesManager resourcesManager) {
		BandIndexList bands = new BandIndexList();
		for (WeatherInfoType weatherInfoType : enabledLayersCached) {
			switch (weatherInfoType) {
				case TEMPERATURE:
					bands.add((short) WeatherBand.Temperature.swigValue());
					break;
				case PRECIPITATION:
					bands.add((short) WeatherBand.Precipitation.swigValue());
					break;
				case WIND:
					bands.add((short) WeatherBand.WindSpeed.swigValue());
					break;
				case CLOUDS:
					bands.add((short) WeatherBand.Cloud.swigValue());
					break;
				case PRESSURE:
					bands.add((short) WeatherBand.Pressure.swigValue());
					break;
			}
		}
		if (!bands.isEmpty()) {
			net.osmand.core.jni.WeatherLayer weatherLayer = this.weatherLayer == WeatherLayer.LOW
					? net.osmand.core.jni.WeatherLayer.Low : net.osmand.core.jni.WeatherLayer.High;

			provider = new WeatherRasterLayerProvider(resourcesManager, weatherLayer, dateTime, bands, false);
			mapRenderer.setMapLayerProvider(view.getLayerIndex(this), provider);

			MapLayerConfiguration mapLayerConfiguration = new MapLayerConfiguration();
			mapLayerConfiguration.setOpacityFactor(1.0f);
			mapRenderer.setMapLayerConfiguration(view.getLayerIndex(this), mapLayerConfiguration);
		}
	}

	private void resetLayerProvider() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			int layerIndex = view.getLayerIndex(this);
			mapRenderer.resetMapLayerProvider(layerIndex);
			provider = null;
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tilesRect, DrawSettings drawSettings) {
		super.onPrepareBufferImage(canvas, tilesRect, drawSettings);
		MapRendererView mapRenderer = getMapRenderer();
		WeatherTileResourcesManager resourcesManager = weatherPlugin.getWeatherResourcesManager();
		if (view == null || mapRenderer == null || resourcesManager == null) {
			return;
		}
		if (resourcesManager.getBandSettings().empty()) {
			return;
		}

		ApplicationMode appMode = settings.getApplicationMode();
		boolean weatherEnabled = weatherPlugin.isWeatherEnabled(appMode);
		boolean weatherEnabledChanged = weatherEnabled != weatherEnabledCached;
		weatherEnabledCached = weatherEnabled;
		List<WeatherInfoType> enabledLayers = weatherPlugin.getEnabledLayers(appMode);
		boolean layersChanged = !Algorithms.objectEquals(enabledLayers, enabledLayersCached);
		enabledLayersCached = enabledLayers;
		int bandsSettingsVersion = weatherPlugin.getBandsSettingsVersion();
		boolean bandsSettingsChanged = bandsSettingsVersion != bandsSettingsVersionCached;
		bandsSettingsVersionCached = bandsSettingsVersion;
		boolean dateTimeChanged = cachedDateTime != dateTime;
		cachedDateTime = dateTime;
		if (weatherEnabledChanged || layersChanged || bandsSettingsChanged || dateTimeChanged || mapActivityInvalidated) {
			if (weatherEnabled && !enabledLayers.isEmpty()) {
				recreateLayerProvider(mapRenderer, resourcesManager);
			} else {
				resetLayerProvider();
			}
		}
		mapActivityInvalidated = false;
	}
}
