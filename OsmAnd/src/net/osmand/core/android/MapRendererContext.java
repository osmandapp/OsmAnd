package net.osmand.core.android;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.osmand.core.jni.IMapTiledSymbolsProvider;
import net.osmand.core.jni.IObfsCollection;
import net.osmand.core.jni.IRasterMapLayerProvider;
import net.osmand.core.jni.MapObjectsSymbolsProvider;
import net.osmand.core.jni.MapPresentationEnvironment;
import net.osmand.core.jni.MapPrimitivesProvider;
import net.osmand.core.jni.MapPrimitiviser;
import net.osmand.core.jni.MapRasterLayerProvider_Software;
import net.osmand.core.jni.MapStylesCollection;
import net.osmand.core.jni.ObfMapObjectsProvider;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.core.jni.MapPresentationEnvironment.LanguagePreference;
import net.osmand.core.jni.ResolvedMapStyle;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

/**
 * Context container and utility class for MapRendererView and derivatives. 
 * 
 * @author Alexey Pelykh
 *
 */
public class MapRendererContext {

	private static final int OBF_RASTER_LAYER = 0;
	private OsmandApplication app;
	
	/**
	 * Cached map styles per name
	 */
	private Map<String, ResolvedMapStyle> mapStyles = new HashMap<String, ResolvedMapStyle>();
	
	/**
	 * Reference to OBF map symbols provider (if used)
	 */
	private IMapTiledSymbolsProvider obfMapSymbolsProvider;
	
	/**
	 * Map styles collection
	 */
	private MapStylesCollection mapStylesCollection;
	
	/**
	 * Reference to map presentation environment (if used)
	 */
	private MapPresentationEnvironment mapPresentationEnvironment;

	/**
	 * Reference to OBFs collection (if present)
	 */
	private IObfsCollection obfsCollection;
	
	/**
	 * Reference to map renderer view that is currently managed by this
	 * context
	 */
	private MapRendererView mapRendererView;
	

	/**
	 * Display density factor
	 */
	private float displayDensityFactor = 1;
	
	/**
	 * Reference tile size on screen in pixels
	 */
	private float referenceTileSizef = 256;
	
	/**
	 * Raster tile size in texels
	 */
	private int rasterTileSize = 256;
	
	private CachedMapPresentation presentationObjectParams;

	
	public MapRendererContext(OsmandApplication app) {
		this.app = app;
	}
	
	/**
	 * Bounds specified map renderer view to this context
	 * @param mapRendererView Reference to MapRendererView
	 */
	public void setMapRendererView(MapRendererView mapRendererView) {
		boolean update = (this.mapRendererView != mapRendererView);
		if (!update) {
			return;
		}
		this.mapRendererView = mapRendererView;
		if (mapRendererView != null) {
			apply();
		}
	}
	
	
	private class CachedMapPresentation {
		String langId ;
		LanguagePreference langPref;
		ResolvedMapStyle mapStyle;
		double displayDensityFactor;
		
		public CachedMapPresentation(String langId,
				LanguagePreference langPref, ResolvedMapStyle mapStyle,
				double displayDensityFactor) {
			this.langId = langId;
			this.langPref = langPref;
			this.mapStyle = mapStyle;
			this.displayDensityFactor = displayDensityFactor;
		}
		
		
		public boolean equalsFields(CachedMapPresentation other ) {
			if (Double.doubleToLongBits(displayDensityFactor) != Double
					.doubleToLongBits(other.displayDensityFactor))
				return false;
			if (langId == null) {
				if (other.langId != null)
					return false;
			} else if (!langId.equals(other.langId))
				return false;
			if (langPref != other.langPref)
				return false;
			if (mapStyle == null) {
				if (other.mapStyle != null)
					return false;
			} else if (!mapStyle.equals(other.mapStyle))
				return false;
			return true;
		}
		
	}

	
	/**
	 * Set display density factor and update context (if needed) 
	 * @param displayDensityFactor New display density factor
	 */
	public void setDisplayDensityFactor(float displayDensityFactor) {
		this.displayDensityFactor = displayDensityFactor;
		referenceTileSizef = 256.0f * displayDensityFactor;
		rasterTileSize = Integer.highestOneBit((int) referenceTileSizef - 1) * 2;

		if (mapRendererView instanceof AtlasMapRendererView)
			((AtlasMapRendererView) mapRendererView)
					.setReferenceTileSizeOnScreenInPixels(referenceTileSizef);
		if (mapPresentationEnvironment != null)
			updateMapPresentationEnvironment();
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
		if(rendName.length() == 0 || rendName.equals(RendererRegistry.DEFAULT_RENDER)) {
			rendName = "default";
		}
		if(!mapStyles.containsKey(rendName)) {
			mapStyles.put(rendName, mapStylesCollection.getResolvedStyleByName(rendName));
		}
		ResolvedMapStyle mapStyle = mapStyles.get(rendName);
		CachedMapPresentation pres = new CachedMapPresentation(langId, langPref, mapStyle, displayDensityFactor);
		if (this.presentationObjectParams == null
				|| !this.presentationObjectParams.equalsFields(pres)) {
			this.presentationObjectParams = pres;
			mapPresentationEnvironment = new MapPresentationEnvironment(
					mapStyle, displayDensityFactor, langId, langPref);
		}

		// Apply map style settings
		OsmandSettings prefs = app.getSettings();
		RenderingRulesStorage storage = app.getRendererRegistry()
				.getCurrentSelectedRenderer();
		Map<String, String> props = new HashMap<String, String>();
		for (RenderingRuleProperty customProp : storage.PROPS.getCustomRules()) {
			if (customProp.isBoolean()) {
				CommonPreference<Boolean> pref = prefs
						.getCustomRenderBooleanProperty(customProp
								.getAttrName());
				props.put(customProp.getAttrName(), pref.get() + "");
			} else {
				CommonPreference<String> settings = prefs
						.getCustomRenderProperty(customProp.getAttrName());
				String res = settings.get();
				if (!Algorithms.isEmpty(res)) {
					props.put(customProp.getAttrName(), res);
				}
			}
		}

		QStringStringHash convertedStyleSettings = new QStringStringHash();
		for (Iterator<Map.Entry<String, String>> itSetting = props.entrySet()
				.iterator(); itSetting.hasNext();) {
			Map.Entry<String, String> setting = itSetting.next();
			convertedStyleSettings.set(setting.getKey(), setting.getValue());
		}
		if(nightMode) {
			convertedStyleSettings.set("nightMode", "true");
		}
		mapPresentationEnvironment.setSettings(convertedStyleSettings);

		// Update all dependencies
		if (mapPrimitiviser != null) {
			updateMapPrimitiviser();
		}
	}
	
	/**
	 * Reference to map primitiviser (if used)
	 */
	private MapPrimitiviser mapPrimitiviser; 
	
	/**
	 * Update map primitiviser and everything that depends on it
	 */
	private void updateMapPrimitiviser() {
		// Create new map primitiviser
		mapPrimitiviser = new MapPrimitiviser(mapPresentationEnvironment);
		
		// Update all dependencies
		if (mapPrimitivesProvider != null)
			updateMapPrimitivesProvider();
	}
	
	/**
	 * Reference to OBF map objects provider (if used)
	 */
	private ObfMapObjectsProvider _obfMapObjectsProvider;
	
	/**
	 * Update OBF map objects provider and everything that depends on it
	 */
	private void updateObfMapObjectsProvider() {
		_obfMapObjectsProvider = new ObfMapObjectsProvider(
				obfsCollection);
		
		// Update all dependencies
		if (mapPrimitivesProvider != null)
			updateMapPrimitivesProvider();
	}
	
	/**
	 * Reference to map primitives provider (if used)
	 */
	private MapPrimitivesProvider mapPrimitivesProvider;
	
	/**
	 * Update map primitives provider and everything that depends on it
	 */
	private void updateMapPrimitivesProvider() {
		// Create new map primitives provider
		mapPrimitivesProvider = new MapPrimitivesProvider(
				_obfMapObjectsProvider,
				mapPrimitiviser,
				rasterTileSize);
		
		// Update all dependencies
		if (obfMapRasterLayerProvider != null)
			updateObfMapRasterLayerProvider();
		if (obfMapSymbolsProvider != null)
			updateObfMapSymbolsProvider();
	}
	
	/**
	 * Reference to OBF map raster layer provider (if used)
	 */
	private IRasterMapLayerProvider obfMapRasterLayerProvider;
	
	/**
	 * Index of OBF map raster layer in bound map renderer view (if set)
	 */
	private Integer obfMapRasterLayer;
	private boolean nightMode;
	
	/**
	 * Update OBF map raster layer provider and everything that depends on it
	 */
	private void updateObfMapRasterLayerProvider() {
		// Create new OBF map raster layer provider
		obfMapRasterLayerProvider = new MapRasterLayerProvider_Software(
				mapPrimitivesProvider);
		
		// In case there's bound view and configured layer, perform setup
		if(mapRendererView != null && obfMapRasterLayer != null)
			mapRendererView.setMapLayerProvider(obfMapRasterLayer, obfMapRasterLayerProvider);
	}
	
	
	
	/**
	 * Update OBF map symbols provider and everything that depends on it
	 */
	private void updateObfMapSymbolsProvider() {
		// If there's current provider and bound view, remove it
		if (obfMapSymbolsProvider != null && mapRendererView != null)
			mapRendererView.removeSymbolsProvider(obfMapSymbolsProvider);
		
		// Create new OBF map symbols provider
		obfMapSymbolsProvider = new MapObjectsSymbolsProvider(
				mapPrimitivesProvider,
				referenceTileSizef);
		
		// If there's bound view, add new provider
		if (mapRendererView != null)
			mapRendererView.addSymbolsProvider(obfMapSymbolsProvider);
	}
	
	/**
	 * Apply current context to view
	 */
	private void apply() {
		if (mapRendererView instanceof AtlasMapRendererView)
			((AtlasMapRendererView)mapRendererView).setReferenceTileSizeOnScreenInPixels(referenceTileSizef);
		
		// Layers
		if (obfMapRasterLayer != null && obfMapRasterLayerProvider != null)
			mapRendererView.setMapLayerProvider(obfMapRasterLayer, obfMapRasterLayerProvider);
		
		// Symbols
		if (obfMapSymbolsProvider != null)
			mapRendererView.addSymbolsProvider(obfMapSymbolsProvider);
	}
	
	/**
	 * Setup OBF map on layer 0 with symbols
	 * @param obfsCollection OBFs collection
	 */
	public void setupObfMap(MapStylesCollection mapStylesCollection, IObfsCollection obfsCollection) {
		this.obfsCollection = obfsCollection;
		this.mapStylesCollection = mapStylesCollection;
		this.obfMapRasterLayer = OBF_RASTER_LAYER;
		updateMapPresentationEnvironment();
		updateMapPrimitiviser();
		updateMapPrimitivesProvider();
		updateObfMapObjectsProvider();
		updateObfMapRasterLayerProvider();
		updateObfMapSymbolsProvider();
	}
	
	public void updateMapSettings() {
		if (mapPresentationEnvironment != null) {
			updateMapPresentationEnvironment();
		}
	}

	public boolean isNightMode() {
		return nightMode;
	}

	public void setNightMode(boolean nightMode) {
		this.nightMode = nightMode;
		updateMapSettings();
	}
}
