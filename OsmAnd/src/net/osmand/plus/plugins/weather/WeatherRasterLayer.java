package net.osmand.plus.plugins.weather;

import android.content.Context;
import android.graphics.Canvas;

import androidx.annotation.NonNull;

import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.BandIndexList;
import net.osmand.core.jni.MapLayerConfiguration;
import net.osmand.core.jni.WeatherRasterLayerProvider;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.BaseMapLayer;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class WeatherRasterLayer extends BaseMapLayer {
	private static final long HOUR_IN_MILLISECONDS = 60 * 60 * 1000;
	private static final long DAY_IN_MILLISECONDS = 24 * HOUR_IN_MILLISECONDS;
	private final WeatherHelper weatherHelper;
	private final WeatherSettings weatherSettings;
	private final WeatherPlugin plugin;
	private final WeatherLayer weatherLayer;

	private WeatherRasterLayerProvider provider;

	private boolean weatherEnabledCached;
	private List<WeatherBand> enabledBandsCached;
	private int bandsSettingsVersionCached;
	private boolean requireTimePeriodChange;
	private long timePeriodStart;
	private long timePeriodEnd;
	private long timePeriodStep;
	private long dateTime;
	private long cachedDateTime;

	private final List<StateChangedListener<Float>> alphaChangeListeners = new ArrayList<>();

	public enum WeatherLayer {
		LOW,
		HIGH
	}

	public WeatherRasterLayer(@NonNull Context context, @NonNull WeatherLayer weatherLayer) {
		super(context);
		OsmandApplication app = getApplication();
		this.weatherHelper = app.getWeatherHelper();
		this.weatherSettings = weatherHelper.getWeatherSettings();
		this.weatherLayer = weatherLayer;
		this.plugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		setDateTime(System.currentTimeMillis());
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		for (WeatherBand weatherBand : weatherHelper.getWeatherBands()) {
			CommonPreference<Float> preference = weatherBand.getAlphaPreference();
			if (preference != null) {
				StateChangedListener<Float> listener = change -> weatherHelper.updateBandsSettings();
				preference.addListener(listener);
				alphaChangeListeners.add(listener);
			}
		}
	}

	public long getDateTime() {
		return dateTime;
	}

	public void setDateTime(long dateTime) {
		long dayStart = OsmAndFormatter.getStartOfToday();
		long dayEnd = dayStart + DAY_IN_MILLISECONDS;
		long step = timePeriodStep;
		if (dateTime > dayEnd)
			step = HOUR_IN_MILLISECONDS * 3;
		else if (dateTime < dayEnd || step < HOUR_IN_MILLISECONDS)
			step = HOUR_IN_MILLISECONDS;
		dayStart = OsmAndFormatter.getStartOfDayForTime(timePeriodStart);
		dayEnd = dayStart + DAY_IN_MILLISECONDS;
		if (dateTime < dayStart || dateTime > dayEnd) {
			dayStart = OsmAndFormatter.getStartOfDayForTime(dateTime);
			dayEnd = dayStart + DAY_IN_MILLISECONDS;
		}
		long prevTime = (dateTime - dayStart) / step * step + dayStart;
		long nextTime = prevTime + step;
		long nearestTime = dateTime - prevTime < nextTime - dateTime ? prevTime : nextTime;
		if (timePeriodStep != step
				|| (timePeriodStart > dayStart && nearestTime <= timePeriodStart)
				|| (timePeriodEnd < dayEnd && nearestTime >= timePeriodEnd)) {
			timePeriodStart = Math.max(nearestTime - step * 2, dayStart);
			timePeriodEnd = Math.min(nearestTime + step * 2, dayEnd);
			timePeriodStep = step;
			requireTimePeriodChange = true;
		}
		this.dateTime = dateTime;
	}

	@Override
	public void destroyLayer() {
		super.destroyLayer();

		for (StateChangedListener<Float> listener : alphaChangeListeners) {
			for (WeatherBand weatherBand : weatherHelper.getWeatherBands()) {
				CommonPreference<Float> preference = weatherBand.getAlphaPreference();
				if (preference != null) {
					preference.removeListener(listener);
				}
			}
		}
	}

	@Override
	protected void cleanupResources() {
		super.cleanupResources();
		resetLayerProvider();
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
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
		for (WeatherBand weatherBand : enabledBandsCached) {
			short bandIndex = weatherBand.getBandIndex();
			if (bandIndex != WeatherBand.WEATHER_BAND_UNDEFINED) {
				bands.add(bandIndex);
			}
		}
		if (!bands.isEmpty()) {
			net.osmand.core.jni.WeatherLayer weatherLayer = this.weatherLayer == WeatherLayer.LOW
					? net.osmand.core.jni.WeatherLayer.Low : net.osmand.core.jni.WeatherLayer.High;
			if (provider == null) {
				requireTimePeriodChange = false;
				provider = new WeatherRasterLayerProvider(resourcesManager, weatherLayer,
						timePeriodStart, timePeriodEnd, timePeriodStep, bands, false);
				mapRenderer.setMapLayerProvider(view.getLayerIndex(this), provider);
				MapLayerConfiguration mapLayerConfiguration = new MapLayerConfiguration();
				mapLayerConfiguration.setOpacityFactor(1.0f);
				mapRenderer.setMapLayerConfiguration(view.getLayerIndex(this), mapLayerConfiguration);
			}
			if (requireTimePeriodChange) {
				requireTimePeriodChange = false;
				provider.setDateTime(timePeriodStart, timePeriodEnd, timePeriodStep);
				mapRenderer.changeTimePeriod();
			}
			mapRenderer.setDateTime(dateTime);
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
		WeatherTileResourcesManager resourcesManager = weatherHelper.getWeatherResourcesManager();
		if (view == null || mapRenderer == null || resourcesManager == null
				|| resourcesManager.getBandSettings().empty()) {
			return;
		}

		if (shouldUpdateLayer() || mapActivityInvalidated || mapRendererChanged) {
			if (shouldDrawLayer()) {
				recreateLayerProvider(mapRenderer, resourcesManager);
			} else {
				resetLayerProvider();
			}
		}
		mapRendererChanged = false;
		mapActivityInvalidated = false;
	}

	private boolean shouldDrawLayer() {
		return (weatherSettings.weatherEnabled.get() || plugin.hasCustomForecast())
				&& !Algorithms.isEmpty(getVisibleBands());
	}

	private boolean shouldUpdateLayer() {
		boolean weatherEnabled = weatherSettings.weatherEnabled.get() || plugin.hasCustomForecast();
		boolean weatherEnabledChanged = weatherEnabled != weatherEnabledCached;
		weatherEnabledCached = weatherEnabled;

		List<WeatherBand> enabledBands = getVisibleBands();
		boolean layersChanged = !Algorithms.objectEquals(enabledBands, enabledBandsCached);
		enabledBandsCached = enabledBands;

		int bandsSettingsVersion = weatherHelper.getBandsSettingsVersion();
		boolean bandsSettingsChanged = bandsSettingsVersion != bandsSettingsVersionCached;
		bandsSettingsVersionCached = bandsSettingsVersion;

		boolean dateTimeChanged = cachedDateTime != dateTime;
		cachedDateTime = dateTime;

		if (weatherEnabledChanged || layersChanged || bandsSettingsChanged)
			requireTimePeriodChange = true;

		return weatherEnabledChanged || layersChanged || bandsSettingsChanged || dateTimeChanged;
	}

	@NonNull
	private List<WeatherBand> getVisibleBands() {
		return plugin.hasCustomForecast() ? weatherHelper.getVisibleForecastBands() : weatherHelper.getVisibleBands();
	}
}