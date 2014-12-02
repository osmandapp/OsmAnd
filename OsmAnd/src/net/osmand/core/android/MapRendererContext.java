package net.osmand.core.android;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.osmand.core.jni.IMapTiledSymbolsProvider;
import net.osmand.core.jni.IObfsCollection;
import net.osmand.core.jni.IRasterMapLayerProvider;
import net.osmand.core.jni.MapObjectsSymbolsProvider;
import net.osmand.core.jni.MapPresentationEnvironment;
import net.osmand.core.jni.MapPresentationEnvironment.LanguagePreference;
import net.osmand.core.jni.MapPrimitivesProvider;
import net.osmand.core.jni.MapPrimitiviser;
import net.osmand.core.jni.MapRasterLayerProvider_Software;
import net.osmand.core.jni.MapStylesCollection;
import net.osmand.core.jni.ObfMapObjectsProvider;
import net.osmand.core.jni.QStringStringHash;
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
 * @author Alexey Pelykh
 *
 */
public class MapRendererContext {

	private static final int OBF_RASTER_LAYER = 0;
	private OsmandApplication app;
	
	// input parameters
	private MapStylesCollection mapStylesCollection;
	private IObfsCollection obfsCollection;
	
	private boolean nightMode;
	private final float density;
	
	// ached objects
	private Map<String, ResolvedMapStyle> mapStyles = new HashMap<String, ResolvedMapStyle>();
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
		if (!update) {
			return;
		}
		this.mapRendererView = mapRendererView;
		if (mapRendererView != null) {
			applyCurrentContextToView();
		}
	}

	public boolean isNightMode() {
		return nightMode;
	}

	public void setNightMode(boolean nightMode) {
		this.nightMode = nightMode;
		updateMapSettings();
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
		recreateRasterAndSymbolsProvider();
	}

	protected float getDisplayDensityFactor() {
		return (float) Math.pow(2, Math.sqrt((app.getSettings().getSettingsZoomScale()  + density)));
	}

	protected int getRasterTileSize() {
		return Integer.highestOneBit((int) getReferenceTileSize() - 1) * 2;
	}
	
	private float getReferenceTileSize() {
		return 256 * getDisplayDensityFactor();
	}
	
	/**
	 * Update map presentation environment and everything that depends on it
	 */
	private void updateMapPresentationEnvironment() {
		float displayDensityFactor = getDisplayDensityFactor();
		// Create new map presentation environment
		String langId = app.getSettings().MAP_PREFERRED_LOCALE.get();
		// TODO make setting
		LanguagePreference langPref = LanguagePreference.LocalizedOrNative;
		String rendName = app.getSettings().RENDERER.get();
		if (rendName.length() == 0 || rendName.equals(RendererRegistry.DEFAULT_RENDER)) {
			rendName = "default";
		}
		if (!mapStyles.containsKey(rendName)) {
			mapStyles.put(rendName, mapStylesCollection.getResolvedStyleByName(rendName));
		}
		ResolvedMapStyle mapStyle = mapStyles.get(rendName);
		CachedMapPresentation pres = new CachedMapPresentation(langId, langPref, mapStyle, displayDensityFactor);
		if (this.presentationObjectParams == null || !this.presentationObjectParams.equalsFields(pres)) {
			this.presentationObjectParams = pres;
			mapPresentationEnvironment = new MapPresentationEnvironment(mapStyle, displayDensityFactor, langId,
					langPref);
		}

		QStringStringHash convertedStyleSettings = getMapStyleSettings();
		mapPresentationEnvironment.setSettings(convertedStyleSettings);

		if (obfMapRasterLayerProvider != null || obfMapSymbolsProvider != null) {
			recreateRasterAndSymbolsProvider();
		}
	}

	protected QStringStringHash getMapStyleSettings() {
		// Apply map style settings
		OsmandSettings prefs = app.getSettings();
		RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();
		Map<String, String> props = new HashMap<String, String>();
		for (RenderingRuleProperty customProp : storage.PROPS.getCustomRules()) {
			if (customProp.isBoolean()) {
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
		for (Iterator<Map.Entry<String, String>> itSetting = props.entrySet().iterator(); itSetting.hasNext();) {
			Map.Entry<String, String> setting = itSetting.next();
			convertedStyleSettings.set(setting.getKey(), setting.getValue());
		}
		if (nightMode) {
			convertedStyleSettings.set("nightMode", "true");
		}
		return convertedStyleSettings;
	}
	
	private void recreateRasterAndSymbolsProvider() {
		// Create new map primitiviser
		MapPrimitiviser mapPrimitiviser = new MapPrimitiviser(mapPresentationEnvironment);
		ObfMapObjectsProvider obfMapObjectsProvider = new ObfMapObjectsProvider(obfsCollection);
		// Create new map primitives provider
		MapPrimitivesProvider mapPrimitivesProvider = new MapPrimitivesProvider(obfMapObjectsProvider, mapPrimitiviser,
				getRasterTileSize());
		updateObfMapRasterLayerProvider(mapPrimitivesProvider);
		updateObfMapSymbolsProvider(mapPrimitivesProvider);
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
		obfMapSymbolsProvider = new MapObjectsSymbolsProvider(mapPrimitivesProvider, getReferenceTileSize());
		// If there's bound view, add new provider
		if (mapRendererView != null) {
			mapRendererView.addSymbolsProvider(obfMapSymbolsProvider);
		}
	}
	
	private void applyCurrentContextToView() {
		if (mapRendererView instanceof AtlasMapRendererView) {
			cachedReferenceTileSize = getReferenceTileSize();
			((AtlasMapRendererView)mapRendererView).setReferenceTileSizeOnScreenInPixels(cachedReferenceTileSize);
		}
		// Layers
		if (obfMapRasterLayerProvider != null) {
			mapRendererView.setMapLayerProvider(OBF_RASTER_LAYER, obfMapRasterLayerProvider);
		}
		// Symbols
		if (obfMapSymbolsProvider != null) {
			mapRendererView.addSymbolsProvider(obfMapSymbolsProvider);
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
	
}
