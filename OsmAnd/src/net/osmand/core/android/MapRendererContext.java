package net.osmand.core.android;

import static net.osmand.IndexConstants.GEOTIFF_DIR;
import static net.osmand.IndexConstants.GEOTIFF_SQLITE_CACHE_DIR;
import static net.osmand.IndexConstants.OPENGL_SHADERS_CACHE_DIR;
import static net.osmand.plus.views.OsmandMapTileView.FOG_DEFAULT_COLOR;
import static net.osmand.plus.views.OsmandMapTileView.FOG_NIGHTMODE_COLOR;
import static net.osmand.plus.views.OsmandMapTileView.MAP_DEFAULT_COLOR;
import static net.osmand.plus.views.OsmandMapTileView.SKY_DEFAULT_COLOR;
import static net.osmand.plus.views.OsmandMapTileView.SKY_NIGHTMODE_COLOR;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.core.jni.*;
import net.osmand.core.jni.ElevationConfiguration.SlopeAlgorithm;
import net.osmand.core.jni.ElevationConfiguration.VisualizationStyle;
import net.osmand.core.jni.IGeoTiffCollection.RasterType;
import net.osmand.core.jni.MapPresentationEnvironment.LanguagePreference;
import net.osmand.core.jni.MapPrimitivesProvider.Mode;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.render.RenderingClass;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
	public static final int POI_SYMBOL_SECTION = 1000;
	public static final int TOP_PLACES_POI_SECTION = 1001;
	public static final int SELECTED_POI_SECTION = 1002;
	public static final int FAVORITES_SECTION = 1003;
	public static boolean IGNORE_CORE_PRELOADED_STYLES = false; // enable to debug default.render.xml changes

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
	private boolean heightmapsActive;

	public boolean showDebugPrimivitisationTiles = false;

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
	public synchronized void setMapRendererView(@Nullable MapRendererView mapRendererView) {
		if (this.mapRendererView == mapRendererView) {
			return;
		}
		this.mapRendererView = mapRendererView;
		if (mapRendererView != null) {
			applyCurrentContextToView();
		}
	}

	public synchronized void suspendMapRendererView(@Nullable MapRendererView mapRendererView) {
		if (this.mapRendererView != null && (mapRendererView == null || this.mapRendererView == mapRendererView)) {
			this.mapRendererView.handleOnPause();
		}
	}

	public synchronized void releaseMapRendererView(@Nullable MapRendererView mapRendererView) {
		if (this.mapRendererView != null && (mapRendererView == null || this.mapRendererView == mapRendererView)) {
			this.mapRendererView.stopRenderer();
			this.mapRendererView = null;
		}
	}

	@Nullable
	public MapRendererView getMapRendererView() {
		return mapRendererView;
	}

	public boolean isVectorLayerEnabled() {
		return !app.getSettings().MAP_ONLINE_DATA.get();
	}

	public boolean isHeightmapsActive() {
		return heightmapsActive;
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
		float mapDensity = app.getSettings().MAP_DENSITY.get();
		float mapDensityAligned = mapDensity > 2.0f ? 2.0f : Math.min(mapDensity, 1.0f);
		return (int) (getReferenceTileSize() * mapDensityAligned);
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
		LanguagePreference langPref = MapRenderRepositories.getMapLanguageSetting(app, zoom);

		loadRendererAddons();
		String rendName = settings.RENDERER.get();
		if (rendName.length() == 0 || rendName.equals(RendererRegistry.DEFAULT_RENDER)) {
			rendName = "default";
		}
		int tryCount = 0;
		while (true) {
			if (mapStyles.containsKey(rendName)) {
				break;
			} else {
				Log.d(TAG, "Style '" + rendName + "' not in cache");
				if (mapStylesCollection.getStyleByName(rendName) == null || IGNORE_CORE_PRELOADED_STYLES) {
					Log.d(TAG, "Unknown '" + rendName + "' style, need to load");
					loadRenderer(rendName);
				}
				ResolvedMapStyle mapStyle = mapStylesCollection.getResolvedStyleByName(rendName);
				if (mapStyle != null) {
					mapStyles.put(rendName, mapStyle);
					break;
				} else {
					Log.d(TAG, "Failed to resolve '" + rendName + "', will use 'default'");
					rendName = "default";
				}
			}
			if (tryCount < 3) {
				tryCount++;
				if (tryCount > 1)
				{
					Log.e(TAG, "Failed to load '" + rendName + "' style, will keep trying");
					app.showToastMessage(R.string.cant_load_map_styles);
				}
			}
		}
		ResolvedMapStyle mapStyle = mapStyles.get(rendName);
		float mapDensity = settings.MAP_DENSITY.get();
		float textScale = settings.TEXT_SCALE.get();
		QStringStringHash styleSettings = getMapStyleSettings();

		CachedMapPresentation pres = new CachedMapPresentation(langId, langPref, mapStyle, styleSettings, density, mapDensity, textScale);
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
		mapPresentationEnvironment.setSettings(styleSettings);

		if (obfMapRasterLayerProvider != null || obfMapSymbolsProvider != null) {
			if (recreateMapPresentation || forceUpdateProviders) {
				recreateRasterAndSymbolsProvider(providerType);
			} else if (languageParamsChanged) {
				if (mapPrimitivesProvider != null || updateMapPrimitivesProvider(providerType)) {
					updateOrRemoveObfMapSymbolsProvider(mapPrimitivesProvider, providerType);
				}
			}
			setMapBackgroundColor();
		}
		setSkyAndFogColors();
		PluginsHelper.updateMapPresentationEnvironment(this);
	}

	@Nullable
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
				loadStyleFromStream(fileName, app.getRendererRegistry().getInputStream(name));
			}
		}
	}

	private void loadRenderer(String rendName) {
		RenderingRulesStorage renderer = app.getRendererRegistry().getRenderer(rendName);
		if ((mapStylesCollection.getStyleByName(rendName) == null || IGNORE_CORE_PRELOADED_STYLES) && renderer != null) {
			loadStyleFromStream(rendName, app.getRendererRegistry().getInputStream(rendName));
			if (renderer.getDependsName() != null) {
				loadRenderer(renderer.getDependsName());
			}
		}
	}

	@NonNull
	protected QStringStringHash getMapStyleSettings() {
		// Apply map style settings
		OsmandSettings settings = app.getSettings();
		RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();

		List<RenderingRuleProperty> customRules = storage.PROPS.getCustomRules();
		Map<String, RenderingClass> renderingClasses = storage.getRenderingClasses();
		Map<String, String> properties = new LinkedHashMap<>(customRules.size() + renderingClasses.size());

		for (RenderingRuleProperty property : customRules) {
			String attrName = property.getAttrName();
			if (property.isBoolean()) {
				properties.put(attrName, String.valueOf(settings.getRenderBooleanPropertyValue(attrName)));
			} else {
				String value = settings.getRenderPropertyValue(property);
				if (!Algorithms.isEmpty(value)) {
					properties.put(attrName, value);
				}
			}
		}
		Map<String, Boolean> parentsStates = new HashMap<>();
		for (Map.Entry<String, RenderingClass> entry : renderingClasses.entrySet()) {
			String name = entry.getKey();
			RenderingClass renderingClass = entry.getValue();
			boolean enabled = settings.getBooleanRenderClassProperty(renderingClass).get();

			String parentName = renderingClass.getParentName();
			if (parentName != null && parentsStates.containsKey(parentName) && !parentsStates.get(parentName)) {
				enabled = false;
			}
			properties.put(name, String.valueOf(enabled));
			parentsStates.put(name, enabled);
		}
		QStringStringHash styleSettings = new QStringStringHash();
		for (Map.Entry<String, String> setting : properties.entrySet()) {
			styleSettings.set(setting.getKey(), setting.getValue());
		}
		if (nightMode) {
			styleSettings.set("nightMode", "true");
		}
		return styleSettings;
	}

	public void removeDirectory(String dirPath) {
		ObfsCollection obfsCollection = obfsCollections.get(ProviderType.MAIN);
		if (obfsCollection != null) {
			obfsCollection.removeDirectory(dirPath);
		}
		recreateRasterAndSymbolsProvider(this.providerType);
	}

	public void addDirectory(String dirPath) {
		ObfsCollection obfsCollection = obfsCollections.get(ProviderType.MAIN);
		if (obfsCollection != null && !obfsCollection.hasDirectory(dirPath)) {
			obfsCollection.addDirectory(dirPath);
		}
		recreateRasterAndSymbolsProvider(this.providerType);
	}

	public void recreateRasterAndSymbolsProvider(@NonNull ProviderType providerType) {
		if (updateMapPrimitivesProvider(providerType)) {
			updateObfMapRasterLayerProvider(mapPrimitivesProvider, providerType);
			updateOrRemoveObfMapSymbolsProvider(mapPrimitivesProvider, providerType);
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
		int renderingThreadsLimit = app.getSettings().MAX_RENDERING_THREADS.get();
		mapPrimitiviser = new MapPrimitiviser(mapPresentationEnvironment);
		ObfMapObjectsProvider obfMapObjectsProvider = new ObfMapObjectsProvider(obfsCollection,
				ObfMapObjectsProvider.Mode.BinaryMapObjectsAndRoads,
				renderingThreadsLimit > 3 || renderingThreadsLimit == 0 ? 2 : 1);
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
				heightmapsActive = false;
				return;
			}
			GeoTiffCollection geoTiffCollection = getGeoTiffCollection();
			int elevationTileSize = mapRendererView.getElevationDataTileSize();
			mapRendererView.setElevationDataProvider(new SqliteHeightmapTileProvider(geoTiffCollection, elevationTileSize));
			heightmapsActive = true;
		} else {
			heightmapsActive = false;
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
		if (showDebugPrimivitisationTiles) {
			obfMapRasterLayerProvider = new MapPrimitivesMetricsLayerProvider(mapPrimitivesProvider);
		} else {
			obfMapRasterLayerProvider = new MapRasterLayerProvider_Software(mapPrimitivesProvider, providerType.fillBackground);
		}
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

	private void updateOrRemoveObfMapSymbolsProvider(@NonNull MapPrimitivesProvider mapPrimitivesProvider,
											 @NonNull ProviderType providerType) {
		if (showDebugPrimivitisationTiles) {
			if (obfMapSymbolsProvider != null && mapRendererView != null && this.providerType == providerType) {
				mapRendererView.removeSymbolsProvider(obfMapSymbolsProvider);
			}
		} else {
			updateObfMapSymbolsProvider(mapPrimitivesProvider, providerType);
		}
	}
	public void presetMapRendererOptions(@NonNull MapRendererView mapRendererView, boolean MSAAEnabled) {
		File shadersCache = new File(app.getCacheDir(), OPENGL_SHADERS_CACHE_DIR);
		if (!shadersCache.exists()) {
			shadersCache.mkdir();
		}
		mapRendererView.setupOptions.setPathToOpenGLShadersCache(shadersCache.getAbsolutePath());
		mapRendererView.setupOptions.setMaxNumberOfRasterMapLayersInBatch(8);
		mapRendererView.setMSAAEnabled(MSAAEnabled);
	}

	private void applyCurrentContextToView() {
		MapRendererView mapRendererView = this.mapRendererView;
		if (mapRendererView == null) {
			return;
		}
		if (mapRendererView instanceof AtlasMapRendererView) {
			cachedReferenceTileSize = getReferenceTileSize();
			((AtlasMapRendererView) mapRendererView).setReferenceTileSizeOnScreenInPixels(cachedReferenceTileSize);
		}
		updateElevationConfiguration();

		if (obfMapRasterLayerProvider != null) {
			mapRendererView.resetMapLayerProvider(providerType.layerIndex);
			mapRendererView.setMapLayerProvider(providerType.layerIndex, obfMapRasterLayerProvider);
		}
		if (obfMapSymbolsProvider != null) {
			mapRendererView.addSymbolsProvider(providerType.symbolsSectionIndex, obfMapSymbolsProvider);
		}
		recreateHeightmapProvider();
		updateVerticalExaggerationScale();
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

	public void updateVerticalExaggerationScale() {
		MapRendererView mapRendererView = this.mapRendererView;
		if (mapRendererView == null) {
			return;
		}
		SRTMPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		if (plugin != null) {
			mapRendererView.setElevationScaleFactor(plugin.getVerticalExaggerationScale());
		}
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
		QStringStringHash styleSettings;
		float displayDensityFactor;
		float mapScaleFactor;
		float symbolsScaleFactor;

		public CachedMapPresentation(@NonNull String langId,
		                             @NonNull LanguagePreference langPref,
		                             @Nullable ResolvedMapStyle mapStyle,
		                             QStringStringHash styleSettings,
		                             float displayDensityFactor,
		                             float mapScaleFactor,
		                             float symbolsScaleFactor) {
			this.langId = langId;
			this.langPref = langPref;
			this.mapStyle = mapStyle;
			this.styleSettings = styleSettings;
			this.displayDensityFactor = displayDensityFactor;
			this.mapScaleFactor = mapScaleFactor;
			this.symbolsScaleFactor = symbolsScaleFactor;
		}

		public boolean shouldRecreateMapPresentation(@NonNull CachedMapPresentation other) {
			return Double.compare(displayDensityFactor, other.displayDensityFactor) != 0
					|| Double.compare(mapScaleFactor, other.mapScaleFactor) != 0
					|| Double.compare(symbolsScaleFactor, other.symbolsScaleFactor) != 0
					|| !Algorithms.objectEquals(mapStyle, other.mapStyle)
					|| styleSettingsChanged(other);
		}

		public boolean styleSettingsChanged(@NonNull CachedMapPresentation other) {
			QStringList names = other.styleSettings.keys();
			for (int i = 0; i < names.size(); i++) {
				String name = names.get(i);
				if (name.equals("appMode") || name.equals("baseAppMode")) {
					continue;
				}
				if (!styleSettings.has_key(name) || !Algorithms.objectEquals(other.styleSettings.get(name), styleSettings.get(name))) {
					return true;
				}
			}
			return false;
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

	public List<RenderedObject> retrievePolygonsAroundMapObject(PointI point, Object object, ZoomLevel zoomLevel) {
		List<RenderedObject> rendPolygons = retrievePolygonsAroundPoint(point, zoomLevel, false);
		List<LatLon> objectPolygon = null;
		if (object instanceof Amenity amenity) {
			objectPolygon = amenity.getPolygon();
		}
		if (object instanceof RenderedObject renderedObject) {
			objectPolygon = renderedObject.getPolygon();
		}
		if (object instanceof BaseDetailsObject detailsObject) {
			objectPolygon = detailsObject.getSyntheticAmenity().getPolygon();
		}
		List<RenderedObject> res = new ArrayList<>();
		if (objectPolygon != null) {
			for (RenderedObject r : rendPolygons) {
				if (Algorithms.isFirstPolygonInsideSecond(objectPolygon, r.getPolygon())) {
					res.add(r);
				}
			}
		} else {
			res = rendPolygons;
		}
		return res;
	}

	public List<RenderedObject> retrievePolygonsAroundPoint(PointI point, ZoomLevel zoomLevel, boolean withPoints) {
		List<RenderedObject> res = new ArrayList<>();
		if (mapPrimitivesProvider != null) {
			MapObjectList polygons = mapPrimitivesProvider.retreivePolygons(point, zoomLevel);
			if (polygons.size() > 0) {
				for (int i = 0; i < polygons.size(); i++) {
					MapObject polygon = polygons.get(i);
					RenderedObject renderedObject = createRenderedObjectForPolygon(polygon, i);
					if (renderedObject != null) {
						res.add(renderedObject);
					}
				}
			}
		}
		return res;
	}

	private RenderedObject createRenderedObjectForPolygon(MapObject mapObject, int order) {
		RenderedObject object = new RenderedObject();
		QStringStringList tags = mapObject.getResolvedAttributesListPairs();
		for (int i = 0; i < tags.size(); i++) {
			QStringStringPair pair = tags.get(i);
			String key = pair.getFirst();
			String value = pair.getSecond();
			if ("osmand_change".equals(key) && "delete".equals(value)) {
				return null;
			}
			object.putTag(key, value);
		}

		QStringStringHash names = mapObject.getCaptionsInAllLanguages();
		QStringList namesKeys = names.keys();
		for (int i = 0; i < namesKeys.size(); i++) {
			String key = namesKeys.get(i);
			String value = names.get(key);
			if ("osmand_change".equals(key) && "delete".equals(value)) {
				return null;
			}
			object.setName(key, value);
		}

		QVectorPointI points31 = mapObject.getPoints31();
		QuadRect rect = new QuadRect();
		for (int i = 0; i < points31.size(); i++) {
			PointI p = points31.get(i);
			object.addLocation(p.getX(), p.getY());
			rect.expand(p.getX(), p.getY(), p.getX(), p.getY());
		}
		object.setBbox((int)rect.left, (int)rect.top, (int)rect.right, (int)rect.bottom);
		ObfMapObject obfMapObject;
		try {
			obfMapObject = ObfMapObject.dynamic_pointer_cast(mapObject);
		} catch (Exception eObfMapObject) {
			obfMapObject = null;
		}
		if (obfMapObject != null) {
			object.setId(obfMapObject.getId().getId().longValue());
		}
		object.markAsPolygon(true);
		object.setOrder(order);
		object.setLabelX(mapObject.getLabelCoordinateX());
		object.setLabelY(mapObject.getLabelCoordinateY());

		if (Algorithms.isEmpty(object.getName())) {
			String captionInNativeLanguage = mapObject.getCaptionInNativeLanguage();
			if (!Algorithms.isEmpty(captionInNativeLanguage)) {
				object.setName(captionInNativeLanguage);
			} else {
				Map<String, String> namesMap = object.getNamesMap(true);
				if (!Algorithms.isEmpty(namesMap)) {
					for (Entry<String, String> entry : namesMap.entrySet()) {
						object.setName(entry.getValue());
						break;
					}
				}
			}
		}
		return object;
	}
}
