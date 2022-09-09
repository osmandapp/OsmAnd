package net.osmand.core.android;

import static net.osmand.IndexConstants.HEIGHTMAP_INDEX_DIR;

import android.util.Log;

import net.osmand.core.jni.IMapTiledSymbolsProvider;
import net.osmand.core.jni.IObfsCollection;
import net.osmand.core.jni.IRasterMapLayerProvider;
import net.osmand.core.jni.MapObjectsSymbolsProvider;
import net.osmand.core.jni.MapPresentationEnvironment;
import net.osmand.core.jni.MapPresentationEnvironment.LanguagePreference;
import net.osmand.core.jni.MapPrimitivesProvider;
import net.osmand.core.jni.MapPrimitiviser;
import net.osmand.core.jni.MapRasterLayerProvider_Software;
import net.osmand.core.jni.MapRendererSetupOptions;
import net.osmand.core.jni.MapStylesCollection;
import net.osmand.core.jni.ObfMapObjectsProvider;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.core.jni.ResolvedMapStyle;
import net.osmand.core.jni.SqliteHeightmapTileProvider;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.TileSqliteDatabasesCollection;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleStorageProperties;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Context container and utility class for MapRendererView and derivatives. 
 * @author Alexey Pelykh
 */
public class MapRendererContext {
    private static final String TAG = "MapRendererContext";

	public static final int OBF_RASTER_LAYER = 0;
	private final OsmandApplication app;
	
	// input parameters
	private MapStylesCollection mapStylesCollection;
	private IObfsCollection obfsCollection;
	
	private boolean nightMode;
	private final float density;
	
	// сached objects
	private final Map<String, ResolvedMapStyle> mapStyles = new HashMap<>();
	private CachedMapPresentation presentationObjectParams;
	private MapPresentationEnvironment mapPresentationEnvironment;
	
	private IMapTiledSymbolsProvider obfMapSymbolsProvider;
	private IRasterMapLayerProvider obfMapRasterLayerProvider;
	private MapRendererView mapRendererView;
	
	private float cachedReferenceTileSize;
	
	public MapRendererContext(OsmandApplication app, float density) {
		this.app = app;
		this.density = density;
	}
	
	/**
	 * Bounds specified map renderer view to this context
	 * @param mapRendererView Reference to MapRendererView
	 */
	public void setMapRendererView(MapRendererView mapRendererView) {
		boolean update = (this.mapRendererView != mapRendererView);
		this.mapRendererView = mapRendererView;
		if (!update) {
			return;
		}
		if (mapRendererView != null) {
			applyCurrentContextToView();
		}
	}

	public boolean isVectorLayerEnabled() {
		return !app.getSettings().MAP_ONLINE_DATA.get();
	}

	public void setNightMode(boolean nightMode) {
		if (nightMode != this.nightMode) {
			this.nightMode = nightMode;
			updateMapSettings();
		}
	}
	
	public void updateMapSettings() {
		if (mapRendererView instanceof AtlasMapRendererView && cachedReferenceTileSize != getReferenceTileSize()) {
			((AtlasMapRendererView) mapRendererView).setReferenceTileSizeOnScreenInPixels(getReferenceTileSize());
		}
		if(mapPresentationEnvironment != null) {
			updateMapPresentationEnvironment();
		}
	}
	
	/**
	 * Setup OBF map on layer 0 with symbols
	 * @param obfsCollection OBFs collection
	 */
	public void setupObfMap(MapStylesCollection mapStylesCollection, IObfsCollection obfsCollection) {
		this.obfsCollection = obfsCollection;
		this.mapStylesCollection = mapStylesCollection;
		updateMapPresentationEnvironment();
		if (isVectorLayerEnabled()) {
			recreateRasterAndSymbolsProvider();
		}
	}

	protected int getRasterTileSize() {
		return (int)(getReferenceTileSize() * app.getSettings().MAP_DENSITY.get());
	}
	
	private float getReferenceTileSize() {
		return 256 * Math.max(1, density);
	}
	
	/**
	 * Update map presentation environment and everything that depends on it
	 */
	private void updateMapPresentationEnvironment() {
		// Create new map presentation environment
		String langId = app.getSettings().MAP_PREFERRED_LOCALE.get();
		// TODO make setting
		LanguagePreference langPref = LanguagePreference.LocalizedOrNative;
		String rendName = app.getSettings().RENDERER.get();
		if (rendName.length() == 0 || rendName.equals(RendererRegistry.DEFAULT_RENDER)) {
			rendName = "default";
		}
		if (!mapStyles.containsKey(rendName)) {
            Log.d(TAG, "Style '" + rendName + "' not in cache");
            if (mapStylesCollection.getStyleByName(rendName) == null) {
                Log.d(TAG, "Unknown '" + rendName + "' style, need to load");

                // Ensure parents are loaded (this may also trigger load)
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
		CachedMapPresentation pres = new CachedMapPresentation(langId, langPref, mapStyle, density,
				app.getSettings().MAP_DENSITY.get(), app.getSettings().TEXT_SCALE.get());
		if (this.presentationObjectParams == null || !this.presentationObjectParams.equalsFields(pres)) {
			this.presentationObjectParams = pres;
			mapPresentationEnvironment = new MapPresentationEnvironment(mapStyle, density,
					app.getSettings().MAP_DENSITY.get(), app.getSettings().TEXT_SCALE.get(), langId,
					langPref);
		}

		QStringStringHash convertedStyleSettings = getMapStyleSettings();
		mapPresentationEnvironment.setSettings(convertedStyleSettings);

		if ((obfMapRasterLayerProvider != null || obfMapSymbolsProvider != null) && isVectorLayerEnabled()) {
			recreateRasterAndSymbolsProvider();
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
		OsmandSettings prefs = app.getSettings();
		RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();
		Map<String, String> props = new HashMap<String, String>();
		for (RenderingRuleProperty customProp : storage.PROPS.getCustomRules()) {
			if (RenderingRuleStorageProperties.UI_CATEGORY_HIDDEN.equals(customProp.getCategory())){
				continue;
			} else if (customProp.isBoolean()) {
				CommonPreference<Boolean> pref = prefs.getCustomRenderBooleanProperty(customProp.getAttrName());
				props.put(customProp.getAttrName(), pref.get() + "");
			} else {
				CommonPreference<String> settings = prefs.getCustomRenderProperty(customProp.getAttrName());
				String res = settings.get();
				if (!Algorithms.isEmpty(res)) {
					props.put(customProp.getAttrName(), res);
				}
			}
		}

		QStringStringHash convertedStyleSettings = new QStringStringHash();
        for (Map.Entry<String, String> setting : props.entrySet()) {
            convertedStyleSettings.set(setting.getKey(), setting.getValue());
        }
		if (nightMode) {
			convertedStyleSettings.set("nightMode", "true");
		}
		return convertedStyleSettings;
	}
	
	public void recreateRasterAndSymbolsProvider() {
		// Create new map primitiviser
		// TODO Victor ask MapPrimitiviser, ObfMapObjectsProvider  
		MapPrimitiviser mapPrimitiviser = new MapPrimitiviser(mapPresentationEnvironment);
		ObfMapObjectsProvider obfMapObjectsProvider = new ObfMapObjectsProvider(obfsCollection);
		// Create new map primitives provider
		MapPrimitivesProvider mapPrimitivesProvider = new MapPrimitivesProvider(obfMapObjectsProvider, mapPrimitiviser,
				getRasterTileSize());
		updateObfMapRasterLayerProvider(mapPrimitivesProvider);
		updateObfMapSymbolsProvider(mapPrimitivesProvider);
	}

	public void resetRasterAndSymbolsProvider() {
		if (mapRendererView != null) {
			mapRendererView.resetMapLayerProvider(OBF_RASTER_LAYER);
		}
		if (obfMapSymbolsProvider != null && mapRendererView != null) {
			mapRendererView.removeSymbolsProvider(obfMapSymbolsProvider);
		}
	}

	public void recreateHeightmapProvider() {
		if (mapRendererView != null) {
			if (!app.getSettings().SHOW_HEIGHTMAPS.get()) {
				mapRendererView.resetElevationDataProvider();
				return;
			}
			File heightMapDir = app.getAppPath(HEIGHTMAP_INDEX_DIR);
			if (!heightMapDir.exists()) {
				heightMapDir.mkdir();
			}
			TileSqliteDatabasesCollection heightsCollection = new TileSqliteDatabasesCollection();
			heightsCollection.addDirectory(heightMapDir.getAbsolutePath());
			mapRendererView.setElevationDataProvider(new SqliteHeightmapTileProvider(heightsCollection,
					mapRendererView.getElevationDataTileSize()));
		}
	}

	private void updateObfMapRasterLayerProvider(MapPrimitivesProvider mapPrimitivesProvider) {
		// Create new OBF map raster layer provider
		obfMapRasterLayerProvider = new MapRasterLayerProvider_Software(mapPrimitivesProvider);
		// In case there's bound view and configured layer, perform setup
		if (mapRendererView != null) {
			mapRendererView.setMapLayerProvider(OBF_RASTER_LAYER, obfMapRasterLayerProvider);
		}
	}
	
	private void updateObfMapSymbolsProvider(MapPrimitivesProvider mapPrimitivesProvider) {
		// If there's current provider and bound view, remove it
		if (obfMapSymbolsProvider != null && mapRendererView != null) {
			mapRendererView.removeSymbolsProvider(obfMapSymbolsProvider);
		}
		// Create new OBF map symbols provider
		obfMapSymbolsProvider = new MapObjectsSymbolsProvider(mapPrimitivesProvider,
				getReferenceTileSize());
		// If there's bound view, add new provider
		if (mapRendererView != null) {
			mapRendererView.addSymbolsProvider(obfMapSymbolsProvider);
		}
	}
	
	private void applyCurrentContextToView() {
		mapRendererView.setMapRendererSetupOptionsConfigurator(
				new MapRendererView.IMapRendererSetupOptionsConfigurator() {
					@Override
					public void configureMapRendererSetupOptions(
							MapRendererSetupOptions mapRendererSetupOptions) {
						mapRendererSetupOptions.setMaxNumberOfRasterMapLayersInBatch(1);
					}
				});
		if (mapRendererView instanceof AtlasMapRendererView) {
			cachedReferenceTileSize = getReferenceTileSize();
			((AtlasMapRendererView)mapRendererView).setReferenceTileSizeOnScreenInPixels(cachedReferenceTileSize);
		}

		if (isVectorLayerEnabled()) {
			// Layers
			if (obfMapRasterLayerProvider != null) {
				mapRendererView.setMapLayerProvider(OBF_RASTER_LAYER, obfMapRasterLayerProvider);
			}
			// Symbols
			if (obfMapSymbolsProvider != null) {
				mapRendererView.addSymbolsProvider(obfMapSymbolsProvider);
			}
			// Heightmap
			recreateHeightmapProvider();
		}
	}

	private static class CachedMapPresentation {
		String langId ;
		LanguagePreference langPref;
		ResolvedMapStyle mapStyle;
		float displayDensityFactor;
		float mapScaleFactor;
		float symbolsScaleFactor;
		
		public CachedMapPresentation(String langId,
				LanguagePreference langPref, ResolvedMapStyle mapStyle,
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
		
		
		public boolean equalsFields(CachedMapPresentation other ) {
			if (Double.compare(displayDensityFactor, other.displayDensityFactor) != 0)
				return false;
			if (Double.compare(mapScaleFactor, other.mapScaleFactor) != 0)
				return false;
			if (Double.compare(symbolsScaleFactor, other.symbolsScaleFactor) != 0)
				return false;
			if (langId == null) {
				if (other.langId != null)
					return false;
			} else if (!langId.equals(other.langId))
				return false;
			if (langPref != other.langPref)
				return false;
			if (mapStyle == null) {
				return other.mapStyle == null;
			} else return mapStyle.equals(other.mapStyle);
		}
	}

    private void loadStyleFromStream(String name, InputStream source) {
    	if (source == null) {
    		return;
    	}
        if (RendererRegistry.DEFAULT_RENDER.equals(name)) {
	        try {
	            source.close();
	        } catch (IOException ignored) {
	        }
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
        } catch(IOException e) {
            Log.e(TAG, "Failed to read style content", e);
            return;
        } finally {
            try {
            	source.close();
            } catch (IOException ignored) {
            }
        }

        if (!mapStylesCollection.addStyleFromByteArray(
                SwigUtilities.createQByteArrayAsCopyOf(content), name)) {
            Log.w(TAG, "Failed to add style from byte array");
        }
    }
}
