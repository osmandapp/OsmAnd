package net.osmand.plus.plugins.weather;

import static net.osmand.core.android.MapRendererContext.WEATHER_CONTOURS_SYMBOL_SECTION;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_CLOUD;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRECIPITATION;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_PRESSURE;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_TEMPERATURE;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_UNDEFINED;
import static net.osmand.plus.plugins.weather.WeatherBand.WEATHER_BAND_WIND_SPEED;

import android.content.Context;
import android.graphics.Canvas;

import androidx.annotation.NonNull;

import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.GeoTileObjectsProvider;
import net.osmand.core.jni.IMapLayerProvider;
import net.osmand.core.jni.MapLayerConfiguration;
import net.osmand.core.jni.MapObjectsSymbolsProvider;
import net.osmand.core.jni.MapPrimitivesProvider;
import net.osmand.core.jni.MapPrimitiviser;
import net.osmand.core.jni.MapRasterLayerProvider_Software;
import net.osmand.core.jni.SymbolSubsectionConfiguration;
import net.osmand.core.jni.WeatherTileResourcesManager;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.weather.WeatherBand.WeatherBandType;
import net.osmand.plus.plugins.weather.units.WeatherUnit;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.layers.base.BaseMapLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

public class WeatherContourLayer extends BaseMapLayer {

	private final OsmandSettings settings;
	private final WeatherPlugin weatherPlugin;
	private final WeatherHelper weatherHelper;

	private IMapLayerProvider rasterMapProvider;
	private MapObjectsSymbolsProvider mapObjectsSymbolsProvider;
	private GeoTileObjectsProvider geoTileObjectsProvider;
	private MapPrimitivesProvider mapPrimitivesProvider;

	private WeatherUnit weatherUnitCached;
	private boolean weatherEnabledCached;
	private boolean contoursEnabledCached;
	private int cachedTransparency;
	private int bandСached;
	private long dateTime;
	private long cachedDateTime;

	public WeatherContourLayer(@NonNull Context context) {
		super(context);
		OsmandApplication app = getApplication();
		this.settings = app.getSettings();
		this.weatherHelper = app.getWeatherHelper();
		this.weatherPlugin = PluginsHelper.getPlugin(WeatherPlugin.class);
		setDateTime(System.currentTimeMillis());
	}

	public long getDateTime() {
		return dateTime;
	}

	public void setDateTime(long dateTime) {
		this.dateTime = WeatherHelper.roundForecastTimeToHour(dateTime);
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
	public int getMaximumShownMapZoom() {
		return 22;
	}

	@Override
	public int getMinimumShownMapZoom() {
		return 1;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tilesRect, OsmandMapLayer.DrawSettings drawSettings) {
	}

	private void recreateLayerProvider(@NonNull MapRendererView mapRenderer,
	                                   @NonNull WeatherTileResourcesManager resourcesManager,
	                                   @WeatherBandType short band) {
		MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
		MapPrimitiviser mapPrimitiviser = mapContext != null ? mapContext.getMapPrimitiviser() : null;
		if (mapPrimitiviser == null) {
			return;
		}
		resetLayerProvider();

		SymbolSubsectionConfiguration symbolSubsectionConfiguration = new SymbolSubsectionConfiguration();
		symbolSubsectionConfiguration.setOpacityFactor(cachedTransparency / 100.0f);
		mapRenderer.setSymbolSubsectionConfiguration(WEATHER_CONTOURS_SYMBOL_SECTION,
				symbolSubsectionConfiguration);

		MapLayerConfiguration mapLayerConfiguration = new MapLayerConfiguration();
		mapLayerConfiguration.setOpacityFactor(cachedTransparency / 100.0f);
		mapRenderer.setMapLayerConfiguration(view.getLayerIndex(this), mapLayerConfiguration);

		RotatedTileBox tb = view.getCurrentRotatedTileBox();
		int screenWidth = tb.getPixWidth();
		int screenHeight = tb.getPixHeight();
		int cacheSize = (screenWidth * 2 / (int) resourcesManager.getTileSize()) * (screenHeight * 2 / (int) resourcesManager.getTileSize());
		int rasterTileSize = (int) (resourcesManager.getTileSize() * resourcesManager.getDensityFactor());

		geoTileObjectsProvider = new GeoTileObjectsProvider(resourcesManager, dateTime, band, false, cacheSize);
		mapPrimitivesProvider = new MapPrimitivesProvider(geoTileObjectsProvider, mapPrimitiviser, rasterTileSize);

		mapObjectsSymbolsProvider = new MapObjectsSymbolsProvider(mapPrimitivesProvider, rasterTileSize);
		mapRenderer.addSymbolsProvider(WEATHER_CONTOURS_SYMBOL_SECTION, mapObjectsSymbolsProvider);

		rasterMapProvider = new MapRasterLayerProvider_Software(mapPrimitivesProvider, false);
		mapRenderer.setMapLayerProvider(view.getLayerIndex(this), rasterMapProvider);
	}

	private void resetLayerProvider() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			mapRenderer.resetMapLayerProvider(view.getLayerIndex(this));
			if (mapObjectsSymbolsProvider != null) {
				mapRenderer.removeSymbolsProvider(mapObjectsSymbolsProvider);
			}
			mapObjectsSymbolsProvider = null;
			mapPrimitivesProvider = null;
			geoTileObjectsProvider = null;
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tilesRect, DrawSettings drawSettings) {
		super.onPrepareBufferImage(canvas, tilesRect, drawSettings);

		MapRendererView mapRenderer = getMapRenderer();
		WeatherTileResourcesManager resourcesManager = weatherHelper.getWeatherResourcesManager();
		if (view == null || mapRenderer == null || resourcesManager == null) {
			return;
		}
		if (resourcesManager.getBandSettings().empty()) {
			return;
		}

		short band = WEATHER_BAND_UNDEFINED;
		WeatherContour weatherContour = weatherPlugin.getSelectedContoursType();

		if (weatherContour == WeatherContour.TEMPERATURE) {
			band = WEATHER_BAND_TEMPERATURE;
		} else if (weatherContour == WeatherContour.PRESSURE) {
			band = WEATHER_BAND_PRESSURE;
		} else if (weatherContour == WeatherContour.CLOUDS) {
			band = WEATHER_BAND_CLOUD;
		} else if (weatherContour == WeatherContour.WIND) {
			band = WEATHER_BAND_WIND_SPEED;
		} else if (weatherContour == WeatherContour.PRECIPITATION) {
			band = WEATHER_BAND_PRECIPITATION;
		}
		WeatherUnit weatherUnit = null;
		WeatherBand weatherBand = weatherHelper.getWeatherBand(band);
		if (weatherBand != null) {
			weatherUnit = weatherBand.getBandUnit();
		}
		boolean weatherUnitChanged = weatherUnit != weatherUnitCached;
		weatherUnitCached = weatherUnit;
		boolean weatherEnabled = weatherPlugin.isWeatherEnabled();
		boolean weatherEnabledChanged = weatherEnabled != weatherEnabledCached;
		weatherEnabledCached = weatherEnabled;
		boolean contoursEnabled = weatherPlugin.isContoursEnabled();
		boolean contoursEnabledChanged = contoursEnabled != contoursEnabledCached;
		contoursEnabledCached = contoursEnabled;
		int transparency = weatherPlugin.getContoursTransparency();
		boolean transparencyChanged = cachedTransparency != transparency;
		boolean bandChanged = bandСached != band;
		bandСached = band;
		cachedTransparency = transparency;
		boolean dateTimeChanged = cachedDateTime != dateTime;
		cachedDateTime = dateTime;
		if (weatherEnabledChanged || contoursEnabledChanged || transparencyChanged || bandChanged
				|| weatherUnitChanged || dateTimeChanged || mapActivityInvalidated) {
			if (weatherEnabled && contoursEnabled && band != WEATHER_BAND_UNDEFINED) {
				recreateLayerProvider(mapRenderer, resourcesManager, band);
			} else {
				resetLayerProvider();
			}
		}
		mapActivityInvalidated = false;
	}
}
