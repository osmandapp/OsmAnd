package net.osmand.core.android;

import static net.osmand.IndexConstants.GEOTIFF_DIR;
import static net.osmand.IndexConstants.GEOTIFF_SQLITE_CACHE_DIR;
import static net.osmand.plus.views.OsmandMapTileView.MAP_DEFAULT_COLOR;
import static net.osmand.plus.views.OsmandMapTileView.FOG_DEFAULT_COLOR;
import static net.osmand.plus.views.OsmandMapTileView.FOG_NIGHTMODE_COLOR;
import static net.osmand.plus.views.OsmandMapTileView.SKY_DEFAULT_COLOR;
import static net.osmand.plus.views.OsmandMapTileView.SKY_NIGHTMODE_COLOR;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.jni.ElevationConfiguration;
import net.osmand.core.jni.ElevationConfiguration.SlopeAlgorithm;
import net.osmand.core.jni.ElevationConfiguration.VisualizationStyle;
import net.osmand.core.jni.GeoTiffCollection;
import net.osmand.core.jni.IGeoTiffCollection.RasterType;
import net.osmand.core.jni.IMapTiledSymbolsProvider;
import net.osmand.core.jni.IObfsCollection;
import net.osmand.core.jni.IRasterMapLayerProvider;
import net.osmand.core.jni.MapObjectsSymbolsProvider;
import net.osmand.core.jni.MapPresentationEnvironment;
import net.osmand.core.jni.MapPresentationEnvironment.LanguagePreference;
import net.osmand.core.jni.MapPrimitivesProvider;
import net.osmand.core.jni.MapPrimitivesProvider.Mode;
import net.osmand.core.jni.MapPrimitiviser;
import net.osmand.core.jni.MapRasterLayerProvider_Software;
import net.osmand.core.jni.MapStylesCollection;
import net.osmand.core.jni.ObfMapObjectsProvider;
import net.osmand.core.jni.ObfsCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QListFloat;
import net.osmand.core.jni.QListPointI;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.core.jni.ResolvedMapStyle;
import net.osmand.core.jni.SqliteHeightmapTileProvider;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRuleStorageProperties;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Context container and utility class for MapRendererView and derivatives.
 *
 * @author Alexey Pelykh
 */
public class MapRendererContext {
	private static final String TAG = "MapRendererContext";

	public static final int OBF_RASTER_LAYER = 0;
	public static final int OBF_CONTOUR_LINES_RASTER_LAYER = 6000;
	public static final int OBF_SYMBOL_SECTION = 1;
	public static final int WEATHER_CONTOURS_SYMBOL_SECTION = 2;

	private final OsmandApplication app;

	// input parameters
	private MapStylesCollection mapStylesCollection;
	private Map<ProviderType, ObfsCollection> obfsCollections;
	@NonNull
	private ProviderType providerType;

	private boolean nightMode;
	private boolean useAppLocale;
	private final float density;

	// сached objects
	private final Map<String, ResolvedMapStyle> mapStyles = new HashMap<>();
	private CachedMapPresentation cachedMapPresentation;
	private MapPresentationEnvironment mapPresentationEnvironment;
	private MapPrimitiviser mapPrimitiviser;
	private MapPrimitivesProvider mapPrimitivesProvider;

	private IMapTiledSymbolsProvider obfMapSymbolsProvider;
	private IRasterMapLayerProvider obfMapRasterLayerProvider;
	@Nullable
	private GeoTiffCollection geoTiffCollection;
	private volatile MapRendererView mapRendererView;

	private float cachedReferenceTileSize;

	public MapRendererContext(OsmandApplication app, float density) {
		this.app = app;
		this.density = density;
		this.providerType = ProviderType.getProviderType(isVectorLayerEnabled());
	}

	/**
	 * Bounds specified map renderer view to this context
	 *
	 * @param mapRendererView Reference to MapRendererView
	 */
	public void setMapRendererView(@Nullable MapRendererView mapRendererView) {
		boolean update = (this.mapRendererView != mapRendererView);
		if (update && this.mapRendererView != null)
			this.mapRendererView.stopRenderer();
		this.mapRendererView = mapRendererView;
		if (!update) {
			return;
		}
		if (mapRendererView != null) {
			applyCurrentContextToView();
		}
	}

	@Nullable
	public MapRendererView getMapRendererView() {
		return mapRendererView;
	}
	public boolean isVectorLayerEnabled() {
		return !app.getSettings().MAP_ONLINE_DATA.get();
	}

	public void setNightMode(boolean nightMode) {
		if (nightMode != this.nightMode) {
			this.nightMode = nightMode;
			updateMapSettings(true);
		}
	}

	public void updateLocalization() {
		int zoom = app.getOsmandMap().getMapView().getZoom();
		boolean useAppLocale = MapRenderRepositories.useAppLocaleForMap(app, zoom);
		if (this.useAppLocale != useAppLocale) {
			this.useAppLocale = useAppLocale;
			updateMapSettings(false);
		}
	}

	public void updateMapSettings(boolean forceUpdateProviders) {
		MapRendererView mapRendererView = this.mapRendererView;
		if (mapRendererView instanceof AtlasMapRendererView && cachedReferenceTileSize != getReferenceTileSize()) {
			((AtlasMapRendererView) mapRendererView).setReferenceTileSizeOnScreenInPixels(getReferenceTileSize());
		}
		if (mapPresentationEnvironment != null) {
			updateMapPresentationEnvironment(forceUpdateProviders);
		}
	}

	public void setupObfMap(@NonNull MapStylesCollection mapStylesCollection,
	                        @NonNull Map<ProviderType, ObfsCollection> obfsCollections) {
		this.mapStylesCollection = mapStylesCollection;
		this.obfsCollections = obfsCollections;
		updateMapPresentationEnvironment(false);
		recreateRasterAndSymbolsProvider(providerType);
	}

	public float getDensity() {
		return density;
	}

	protected int getRasterTileSize() {
		return (int) (getReferenceTileSize() * app.getSettings().MAP_DENSITY.get());
	}

	private float getReferenceTileSize() {
		return 256 * Math.max(1, density);
	}

	/**
	 * Update map presentation environment and everything that depends on it
	 */
	private void updateMapPresentationEnvironment(boolean forceUpdateProviders) {
		// Create new map presentation environment
		OsmandSettings settings = app.getSettings();

		int zoom = app.getOsmandMap().getMapView().getZoom();
		String langId = MapRenderRepositories.getMapPreferredLocale(app, zoom);
		boolean transliterate = MapRenderRepositories.transliterateMapNames(app, zoom);
		LanguagePreference langPref = transliterate
				? LanguagePreference.LocalizedOrTransliterated
				: LanguagePreference.LocalizedOrNative;

		loadRendererAddons();
		String rendName = settings.RENDERER.get();
		if (rendName.length() == 0 || rendName.equals(RendererRegistry.DEFAULT_RENDER)) {
			rendName = "default";
		}
		if (!mapStyles.containsKey(rendName)) {
			Log.d(TAG, "Style '" + rendName + "' not in cache");
			if (mapStylesCollection.getStyleByName(rendName) == null) {
				Log.d(TAG, "Unknown '" + rendName + "' style, need to load");
				loadRenderer(rendName);
			}
			ResolvedMapStyle mapStyle = mapStylesCollection.getResolvedStyleByName(rendName);
			if (mapStyle != null) {
				mapStyles.put(rendName, mapStyle);
			} else {
				Log.d(TAG, "Failed to resolve '" + rendName + "', will use 'default'");
				rendName = "default";
			}
		}
		ResolvedMapStyle mapStyle = mapStyles.get(rendName);
		float mapDensity = settings.MAP_DENSITY.get();
		float textScale = settings.TEXT_SCALE.get();

		CachedMapPresentation pres = new CachedMapPresentation(langId, langPref, mapStyle, density, mapDensity, textScale);
		boolean recreateMapPresentation = cachedMapPresentation == null
				|| cachedMapPresentation.shouldRecreateMapPresentation(pres);
		boolean languageParamsChanged = cachedMapPresentation != null
				&& cachedMapPresentation.languageParamsChanged(pres);
		cachedMapPresentation = pres;

		if (recreateMapPresentation) {
			mapPresentationEnvironment = new MapPresentationEnvironment(mapStyle, density, mapDensity, textScale);
		}

		mapPresentationEnvironment.setLocaleLanguageId(langId);
		mapPresentationEnvironment.setLanguagePreference(langPref);
		QStringStringHash convertedStyleSettings = getMapStyleSettings();
		mapPresentationEnvironment.setSettings(convertedStyleSettings);

		if (obfMapRasterLayerProvider != null || obfMapSymbolsProvider != null) {
			if (recreateMapPresentation || forceUpdateProviders) {
				recreateRasterAndSymbolsProvider(providerType);
			} else if (languageParamsChanged) {
				if (mapPrimitivesProvider != null || updateMapPrimitivesProvider(providerType)) {
					updateObfMapSymbolsProvider(mapPrimitivesProvider, providerType);
				}
			}
			setMapBackgroundColor();
		}
		setSkyAndFogColors();
		PluginsHelper.updateMapPresentationEnvironment(this);
	}

	public MapPresentationEnvironment getMapPresentationEnvironment() {
		return mapPresentationEnvironment;
	}

	private void setMapBackgroundColor() {
		RenderingRulesStorage rrs = app.getRendererRegistry().getCurrentSelectedRenderer();
		int color = MAP_DEFAULT_COLOR;
		if (rrs != null) {
			RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
			req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, nightMode);
			if (req.searchRenderingAttribute(RenderingRuleStorageProperties.A_DEFAULT_COLOR)) {
				color = req.getIntPropertyValue(req.ALL.R_ATTR_COLOR_VALUE);
			}
		}
		MapRendererView mapRendererView = this.mapRendererView;
		if (mapRendererView != null) {
			mapRendererView.setBackgroundColor(NativeUtilities.createFColorRGB(color));
		}
	}

	private void setSkyAndFogColors() {
		MapRendererView mapRendererView = this.mapRendererView;
		if (mapRendererView != null) {
			mapRendererView.setSkyColor(NativeUtilities.createFColorRGB(nightMode ? SKY_NIGHTMODE_COLOR : SKY_DEFAULT_COLOR));
			mapRendererView.setFogColor(NativeUtilities.createFColorRGB(nightMode ? FOG_NIGHTMODE_COLOR : FOG_DEFAULT_COLOR));
		}
	}

	private void loadRendererAddons() {
		Map<String, String> rendererAddons = app.getRendererRegistry().getRendererAddons();
		for (Entry<String, String> addonEntry : rendererAddons.entrySet()) {
			String name = addonEntry.getKey();
			String fileName = addonEntry.getValue();
			if (mapStylesCollection.getStyleByName(fileName) == null) {
				try {
					loadStyleFromStream(fileName, app.getRendererRegistry().getInputStream(name));
				} catch (IOException e) {
					Log.e(TAG, "Failed to load '" + fileName + "'", e);
				}
			}
		}
	}

	private void loadRenderer(String rendName) {
		RenderingRulesStorage renderer = app.getRendererRegistry().getRenderer(rendName);
		if (mapStylesCollection.getStyleByName(rendName) == null && renderer != null) {
			try {
				loadStyleFromStream(rendName, app.getRendererRegistry().getInputStream(rendName));
				if (renderer.getDependsName() != null) {
					loadRenderer(renderer.getDependsName());
				}
			} catch (IOException e) {
				Log.e(TAG, "Failed to load '" + rendName + "'", e);
			}
		}
	}

	protected QStringStringHash getMapStyleSettings() {
		// Apply map style settings
		OsmandSettings settings = app.getSettings();
		RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();

		Map<String, String> properties = new HashMap<>();
		for (RenderingRuleProperty property : storage.PROPS.getCustomRules()) {
			String attrName = property.getAttrName();
			if (property.isBoolean()) {
				properties.put(attrName, settings.getRenderBooleanPropertyValue(attrName) + "");
			} else {
				String value = settings.getRenderPropertyValue(attrName);
				if (!Algorithms.isEmpty(value)) {
					properties.put(attrName, value);
				}
			}
		}

		QStringStringHash convertedStyleSettings = new QStringStringHash();
		for (Entry<String, String> setting : properties.entrySet()) {
			convertedStyleSettings.set(setting.getKey(), setting.getValue());
		}
		if (nightMode) {
			convertedStyleSettings.set("nightMode", "true");
		}
		return convertedStyleSettings;
	}

	public void removeDirectory(String dirPath) {
		ObfsCollection obfsCollection = obfsCollections.get(ProviderType.MAIN);
		if (obfsCollection != null) {
			obfsCollection.removeDirectory(dirPath);
		}
		recreateRasterAndSymbolsProvider(ProviderType.MAIN);
	}

	public void addDirectory(String dirPath) {
		ObfsCollection obfsCollection = obfsCollections.get(ProviderType.MAIN);
		if (obfsCollection != null && !obfsCollection.hasDirectory(dirPath)) {
			obfsCollection.addDirectory(dirPath);
		}
		recreateRasterAndSymbolsProvider(ProviderType.MAIN);
	}

	public void recreateRasterAndSymbolsProvider(@NonNull ProviderType providerType) {
		if (updateMapPrimitivesProvider(providerType)) {
			updateObfMapRasterLayerProvider(mapPrimitivesProvider, providerType);
			updateObfMapSymbolsProvider(mapPrimitivesProvider, providerType);
			this.providerType = providerType;
		}
	}

	public void resetRasterAndSymbolsProvider(@NonNull ProviderType providerType) {
		MapRendererView mapRendererView = this.mapRendererView;
		ProviderType currentProviderType = this.providerType;
		if (mapRendererView != null && currentProviderType == providerType) {
			mapRendererView.resetMapLayerProvider(currentProviderType.layerIndex);
			if (obfMapRasterLayerProvider != null) {
				mapRendererView.removeSymbolsProvider(obfMapSymbolsProvider);
			}
		}
	}

	private boolean updateMapPrimitivesProvider(@NonNull ProviderType providerType) {
		IObfsCollection obfsCollection = obfsCollections.get(providerType);
		if (obfsCollection == null) {
			return false;
		}

		mapPrimitiviser = new MapPrimitiviser(mapPresentationEnvironment);
		ObfMapObjectsProvider obfMapObjectsProvider = new ObfMapObjectsProvider(obfsCollection);
		mapPrimitivesProvider = new MapPrimitivesProvider(obfMapObjectsProvider,
				mapPrimitiviser, getRasterTileSize(), providerType.surfaceMode);
		return true;
	}

	public void recreateHeightmapProvider() {
		MapRendererView mapRendererView = this.mapRendererView;
		if (mapRendererView != null) {
			SRTMPlugin srtmPlugin = PluginsHelper.getActivePlugin(SRTMPlugin.class);
			if (srtmPlugin == null || !srtmPlugin.is3DMapsEnabled()) {
				mapRendererView.resetElevationDataProvider();
				return;
			}
			GeoTiffCollection geoTiffCollection = getGeoTiffCollection();
			int elevationTileSize = mapRendererView.getElevationDataTileSize();
			mapRendererView.setElevationDataProvider(new SqliteHeightmapTileProvider(geoTiffCollection, elevationTileSize));
		}
	}
	public void resetHeightmapProvider() {
		MapRendererView mapRendererView = this.mapRendererView;
		if (mapRendererView != null) {
			mapRendererView.resetElevationDataProvider();
		}
	}

	private void updateObfMapRasterLayerProvider(@NonNull MapPrimitivesProvider mapPrimitivesProvider,
	                                             @NonNull ProviderType providerType) {
		// Create new OBF map raster layer provider
		obfMapRasterLayerProvider = new MapRasterLayerProvider_Software(mapPrimitivesProvider, providerType.fillBackground);
		// In case there's bound view and configured layer, perform setup
		MapRendererView mapRendererView = this.mapRendererView;
		if (mapRendererView != null) {
			int previousLayerIndex = this.providerType.layerIndex;
			int newLayerIndex = providerType.layerIndex;

			mapRendererView.resetMapLayerProvider(previousLayerIndex);
			mapRendererView.setMapLayerProvider(newLayerIndex, obfMapRasterLayerProvider);
		}
	}

	private void updateObfMapSymbolsProvider(@NonNull MapPrimitivesProvider mapPrimitivesProvider,
	                                         @NonNull ProviderType providerType) {
		// If there's current provider and bound view, remove it
		MapRendererView mapRendererView = this.mapRendererView;
		if (obfMapSymbolsProvider != null && mapRendererView != null && this.providerType == providerType) {
			mapRendererView.removeSymbolsProvider(obfMapSymbolsProvider);
		}
		// Create new OBF map symbols provider
		obfMapSymbolsProvider = new MapObjectsSymbolsProvider(mapPrimitivesProvider, getReferenceTileSize(), null, false, false);
		// If there's bound view, add new provider
		if (mapRendererView != null) {
			mapRendererView.addSymbolsProvider(providerType.symbolsSectionIndex, obfMapSymbolsProvider);
		}
	}

	private void applyCurrentContextToView() {
		MapRendererView mapRendererView = this.mapRendererView;
		if (mapRendererView == null) {
			return;
		}
		mapRendererView.setMapRendererSetupOptionsConfigurator(
				mapRendererSetupOptions -> mapRendererSetupOptions.setMaxNumberOfRasterMapLayersInBatch(1));
		if (mapRendererView instanceof AtlasMapRendererView) {
			cachedReferenceTileSize = getReferenceTileSize();
			((AtlasMapRendererView) mapRendererView).setReferenceTileSizeOnScreenInPixels(cachedReferenceTileSize);
		}
		updateElevationConfiguration();

		if (obfMapRasterLayerProvider != null) {
			mapRendererView.setMapLayerProvider(providerType.layerIndex, obfMapRasterLayerProvider);
		}
		if (obfMapSymbolsProvider != null) {
			mapRendererView.addSymbolsProvider(providerType.symbolsSectionIndex, obfMapSymbolsProvider);
		}
		recreateHeightmapProvider();
		setMapBackgroundColor();
	}

	public void updateElevationConfiguration() {
		MapRendererView mapRendererView = this.mapRendererView;
		if (mapRendererView == null) {
			return;
		}
		ElevationConfiguration elevationConfiguration = new ElevationConfiguration();
		SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		boolean disableVertexHillshade = plugin != null && plugin.isTerrainLayerEnabled() && plugin.isHillshadeMode();
		if (disableVertexHillshade) {
			elevationConfiguration.setSlopeAlgorithm(SlopeAlgorithm.None);
			elevationConfiguration.setVisualizationStyle(VisualizationStyle.None);
		}
		mapRendererView.setElevationConfiguration(elevationConfiguration);
	}

	public void updateCachedHeightmapTiles() {
		GeoTiffCollection geoTiffCollection = getGeoTiffCollection();
		for (RasterType rasterType : RasterType.values()) {
			geoTiffCollection.refreshTilesInCache(rasterType);
		}
	}

	public void removeCachedHeightmapTiles(@NonNull String filePath) {
		GeoTiffCollection geoTiffCollection = getGeoTiffCollection();
		for (RasterType rasterType : RasterType.values()) {
			geoTiffCollection.removeFileTilesFromCache(rasterType, filePath);
		}
	}

	@NonNull
	public GeoTiffCollection getGeoTiffCollection() {
		if (geoTiffCollection == null) {
			geoTiffCollection = new GeoTiffCollection();
			File sqliteCacheDir = new File(app.getCacheDir(), GEOTIFF_SQLITE_CACHE_DIR);
			if (!sqliteCacheDir.exists()) {
				sqliteCacheDir.mkdir();
			}
			File geotiffDir = app.getAppPath(GEOTIFF_DIR);
			if (!geotiffDir.exists()) {
				geotiffDir.mkdir();
			}
			geoTiffCollection.addDirectory(geotiffDir.getAbsolutePath());
			geoTiffCollection.setLocalCache(sqliteCacheDir.getAbsolutePath());
		}
		return geoTiffCollection;
	}

	@Nullable
	public MapPrimitiviser getMapPrimitiviser() {
		return mapPrimitiviser;
	}

	@Nullable
	public float[] calculateHeights(@NonNull List<LatLon> points) {
		MapRendererView mapRendererView = this.mapRendererView;
		GeoTiffCollection collection = getGeoTiffCollection();
		if (mapRendererView != null) {
			QListPointI qpoints = new QListPointI();
			for (LatLon latLon : points) {
				qpoints.add(new PointI(
						MapUtils.get31TileNumberX(latLon.getLongitude()),
						MapUtils.get31TileNumberY(latLon.getLatitude())));
			}
			QListFloat heights = new QListFloat();
			if (collection.calculateHeights(
					ZoomLevel.ZoomLevel14, mapRendererView.getElevationDataTileSize(), qpoints, heights)) {
				if (heights.size() == points.size()) {
					int size = (int)heights.size();
					float[] res = new float[size];
					for (int i = 0; i < size; i++) {
						res[i] = heights.get(i);
					}
					return res;
				}
			}
		}
		return null;
	}

	private static class CachedMapPresentation {

		@NonNull
		String langId;
		@NonNull
		LanguagePreference langPref;
		@Nullable
		ResolvedMapStyle mapStyle;
		float displayDensityFactor;
		float mapScaleFactor;
		float symbolsScaleFactor;

		public CachedMapPresentation(@NonNull String langId,
		                             @NonNull LanguagePreference langPref,
		                             @Nullable ResolvedMapStyle mapStyle,
		                             float displayDensityFactor,
		                             float mapScaleFactor,
		                             float symbolsScaleFactor) {
			this.langId = langId;
			this.langPref = langPref;
			this.mapStyle = mapStyle;
			this.displayDensityFactor = displayDensityFactor;
			this.mapScaleFactor = mapScaleFactor;
			this.symbolsScaleFactor = symbolsScaleFactor;
		}

		public boolean shouldRecreateMapPresentation(@NonNull CachedMapPresentation other) {
			return Double.compare(displayDensityFactor, other.displayDensityFactor) != 0
					|| Double.compare(mapScaleFactor, other.mapScaleFactor) != 0
					|| Double.compare(symbolsScaleFactor, other.symbolsScaleFactor) != 0
					|| !Algorithms.objectEquals(mapStyle, other.mapStyle);
		}

		public boolean languageParamsChanged(@NonNull CachedMapPresentation other) {
			return !langId.equals(other.langId) || langPref != other.langPref;
		}
	}

	private void loadStyleFromStream(String name, InputStream source) {
		if (source == null) {
			return;
		}
		if (RendererRegistry.DEFAULT_RENDER.equals(name)) {
			Algorithms.closeStream(source);
			return;
		}

		Log.d(TAG, "Going to pass '" + name + "' style content to native");
		byte[] content;
		try {
			ByteArrayOutputStream intermediateBuffer = new ByteArrayOutputStream();
			int nRead;
			byte[] data = new byte[16384];
			while ((nRead = source.read(data, 0, data.length)) != -1) {
				intermediateBuffer.write(data, 0, nRead);
			}
			intermediateBuffer.flush();
			content = intermediateBuffer.toByteArray();
		} catch (IOException e) {
			Log.e(TAG, "Failed to read style content", e);
			return;
		} finally {
			Algorithms.closeStream(source);
		}

		if (!mapStylesCollection.addStyleFromByteArray(
				SwigUtilities.createQByteArrayAsCopyOf(content), name)) {
			Log.w(TAG, "Failed to add style from byte array");
		}
	}

	public enum ProviderType {

		MAIN(OBF_RASTER_LAYER, OBF_SYMBOL_SECTION, Mode.WithSurface, true),
		CONTOUR_LINES(OBF_CONTOUR_LINES_RASTER_LAYER, OBF_SYMBOL_SECTION, Mode.WithoutSurface, false);

		public final int layerIndex;
		public final int symbolsSectionIndex;
		public final boolean fillBackground;
		@NonNull
		public final Mode surfaceMode;

		ProviderType(int layerIndex, int symbolsSectionIndex, @NonNull Mode surfaceMode, boolean fillBackground) {
			this.layerIndex = layerIndex;
			this.symbolsSectionIndex = symbolsSectionIndex;
			this.surfaceMode = surfaceMode;
			this.fillBackground = fillBackground;
		}

		@NonNull
		public static ProviderType getProviderType(boolean vectorLayerEnabled) {
			return vectorLayerEnabled ? MAIN : CONTOUR_LINES;
		}
	}
}
