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
import net.osmand.core.jni.ObfMapObjectsProvider;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.core.jni.MapPresentationEnvironment.LanguagePreference;
import net.osmand.core.jni.ResolvedMapStyle;

/**
 * Context container and utility class for MapRendererView and derivatives. 
 * 
 * @author Alexey Pelykh
 *
 */
public class MapRendererContext {

	public MapRendererContext() {
	}
	
	public MapRendererContext(MapRendererView mapRendererView) {
		_mapRendererView = mapRendererView;
	}
	
	/**
	 * Synchronisation object used to perform state changes atomically
	 */
	private final Object _syncObject = new Object(); 
	
	/**
	 * Reference to map renderer view that is currently managed by this
	 * context
	 */
	private MapRendererView _mapRendererView;
	
	/**
	 * Get currently bound map renderer view
	 * @return Reference to MapRendererView 
	 */
	public MapRendererView getMapRendererView() {
		synchronized (_syncObject) {
			return _mapRendererView;
		}
	}
	
	/**
	 * Bounds specified map renderer view to this context
	 * @param mapRendererView Reference to MapRendererView
	 */
	public void setMapRendererView(MapRendererView mapRendererView) {
		synchronized (_syncObject) {
			boolean update = (_mapRendererView != mapRendererView);
			if (!update)
				return;
			
			_mapRendererView = mapRendererView;
			if (_mapRendererView != null)
				apply();
		}
	}
	
	/**
	 * Display density factor
	 */
	private float _displayDensityFactor = 1;
	
	/**
	 * Reference tile size on screen in pixels
	 */
	private float _referenceTileSize = 256;
	
	/**
	 * Raster tile size in texels
	 */
	private int _rasterTileSize = 256;

	/**
	 * Get current display density factor 
	 * @return Display density factor
	 */
	public float getDisplayDensityFactor() {
		synchronized (_syncObject) {
			return _displayDensityFactor;	
		}
	}
	
	/**
	 * Set display density factor and update context (if needed) 
	 * @param displayDensityFactor New display density factor
	 */
	public void setDisplayDensityFactor(float displayDensityFactor) {
		synchronized (_syncObject) {
			boolean update = (_displayDensityFactor != displayDensityFactor);
			if (!update)
				return;
			
			_displayDensityFactor = displayDensityFactor;
			_referenceTileSize = 256.0f * _displayDensityFactor;
			_rasterTileSize = Integer.highestOneBit((int)_referenceTileSize - 1) * 2;
			
			if (_mapRendererView instanceof AtlasMapRendererView)
				((AtlasMapRendererView)_mapRendererView).setReferenceTileSizeOnScreenInPixels(_referenceTileSize);
			if (_mapPresentationEnvironment != null)
				updateMapPresentationEnvironment();
		}
	}
	
	/**
	 * Reference to resolved map style (if used)
	 */
	private ResolvedMapStyle _mapStyle;
	
	/**
	 * Get current map style
	 * @return Reference to current map style
	 */
	public ResolvedMapStyle getMapStyle() {
		synchronized (_syncObject) {
			return _mapStyle;			
		}
	}
	
	/**
	 * Set map style and update context (if needed)
	 * @param mapStyle
	 */
	public void setMapStyle(ResolvedMapStyle mapStyle) {
		synchronized (_syncObject) {
			boolean update = (_mapStyle != mapStyle);
			if (!update)
				return;
			
			_mapStyle = mapStyle;
			if (_mapPresentationEnvironment != null)
				updateMapPresentationEnvironment();
		}
	}
	
	/**
	 * Reference to map style settings (if present) 
	 */
	private Map<String, String> _mapStyleSettings;
	
	/**
	 * Get current map style settings
	 * @return
	 */
	public Map<String, String> getMapStyleSettings() {
		synchronized (_syncObject) {
			if (_mapStyleSettings == null)
				return null;
			return Collections.unmodifiableMap(_mapStyleSettings);
		}
	}
	
	/**
	 * Set map style settings and update context (if needed)
	 * @param mapStyleSettings Map style settings
	 */
	public void setMapStyleSettings(Map<String, String> mapStyleSettings) {
		synchronized (_syncObject) {
			boolean update = !_mapStyleSettings.equals(mapStyleSettings);
			if (!update)
				return;
			
			_mapStyleSettings = new HashMap<String, String>(mapStyleSettings);
			if (_mapPresentationEnvironment != null)
				updateMapPresentationEnvironment();
		}
	}
	
	/**
	 * Locale language
	 */
	private String _localeLanguageId = "en";
	
	/**
	 * Get current locale language
	 * @return Locale language identifier
	 */
	public String getLocaleLanguageId() {
		synchronized (_syncObject) {
			return _localeLanguageId;	
		}
	}
	
	/**
	 * Set current locale language and update context (if needed)
	 * @param localeLanguageId Locale language identifier
	 */
	public void setLocaleLanguageId(String localeLanguageId) {
		synchronized (_syncObject) {
			boolean update = !_localeLanguageId.equals(localeLanguageId);
			if (!update)
				return;
			
			_localeLanguageId = localeLanguageId;
			if (_mapPresentationEnvironment != null)
				updateMapPresentationEnvironment();
		}
	}
	
	/**
	 * Language preference
	 */
	private LanguagePreference _languagePreference = LanguagePreference.LocalizedAndNative;
	
	/**
	 * Get current language preference
	 * @return Language preference
	 */
	public LanguagePreference getLanguagePreference() {
		synchronized (_syncObject) {
			return _languagePreference;
		}
	}
	
	/**
	 * Set language preference and update context (if needed)
	 * @param languagePreference
	 */
	public void setLanguagePreference(LanguagePreference languagePreference) {
		synchronized (_syncObject) {
			boolean update = (_languagePreference != languagePreference);
			if (!update)
				return;
			
			_languagePreference = languagePreference;
			if (_mapPresentationEnvironment != null)
				updateMapPresentationEnvironment();
		}
	}
	
	/**
	 * Reference to OBFs collection (if present)
	 */
	private IObfsCollection _obfsCollection;
	
	/**
	 * Get current OBFs collection
	 * @return OBFs collection
	 */
	public IObfsCollection getObfsCollection() {
		synchronized (_syncObject) {
			return _obfsCollection;	
		}
	}
	
	/**
	 * Set OBFs collection and update context (if needed)
	 * @param obfsCollection
	 */
	public void setObfsCollection(IObfsCollection obfsCollection) {
		synchronized (_syncObject) {
			boolean update = (_obfsCollection != obfsCollection);
			if (!update)
				return;
			
			_obfsCollection = obfsCollection;
			if (_obfMapObjectsProvider != null)
				updateObfMapObjectsProvider();
		}
	}
	
	/**
	 * Reference to map presentation environment (if used)
	 */
	private MapPresentationEnvironment _mapPresentationEnvironment;

	/**
	 * Update map presentation environment and everything that depends on it
	 */
	private void updateMapPresentationEnvironment() {
		// Create new map presentation environment
		_mapPresentationEnvironment = new MapPresentationEnvironment(
				_mapStyle,
				_displayDensityFactor,
				_localeLanguageId,
				_languagePreference);

		// Apply map style settings
		if (_mapStyleSettings != null) {
			QStringStringHash convertedStyleSettings = new QStringStringHash();
			for (Iterator<Map.Entry<String, String>> itSetting = _mapStyleSettings
					.entrySet().iterator(); itSetting.hasNext();) {
				Map.Entry<String, String> setting = itSetting.next();
				convertedStyleSettings.set(setting.getKey(), setting.getValue());
			}
			_mapPresentationEnvironment.setSettings(convertedStyleSettings);
		}

		// Update all dependencies
		if (_mapPrimitiviser != null)
			updateMapPrimitiviser();
	}
	
	/**
	 * Reference to map primitiviser (if used)
	 */
	private MapPrimitiviser _mapPrimitiviser; 
	
	/**
	 * Update map primitiviser and everything that depends on it
	 */
	private void updateMapPrimitiviser() {
		// Create new map primitiviser
		_mapPrimitiviser = new MapPrimitiviser(_mapPresentationEnvironment);
		
		// Update all dependencies
		if (_mapPrimitivesProvider != null)
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
				_obfsCollection);
		
		// Update all dependencies
		if (_mapPrimitivesProvider != null)
			updateMapPrimitivesProvider();
	}
	
	/**
	 * Reference to map primitives provider (if used)
	 */
	private MapPrimitivesProvider _mapPrimitivesProvider;
	
	/**
	 * Update map primitives provider and everything that depends on it
	 */
	private void updateMapPrimitivesProvider() {
		// Create new map primitives provider
		_mapPrimitivesProvider = new MapPrimitivesProvider(
				_obfMapObjectsProvider,
				_mapPrimitiviser,
				_rasterTileSize);
		
		// Update all dependencies
		if (_obfMapRasterLayerProvider != null)
			updateObfMapRasterLayerProvider();
		if (_obfMapSymbolsProvider != null)
			updateObfMapSymbolsProvider();
	}
	
	/**
	 * Reference to OBF map raster layer provider (if used)
	 */
	private IRasterMapLayerProvider _obfMapRasterLayerProvider;
	
	/**
	 * Index of OBF map raster layer in bound map renderer view (if set)
	 */
	private Integer _obfMapRasterLayer;
	
	/**
	 * Update OBF map raster layer provider and everything that depends on it
	 */
	private void updateObfMapRasterLayerProvider() {
		// Create new OBF map raster layer provider
		_obfMapRasterLayerProvider = new MapRasterLayerProvider_Software(
				_mapPrimitivesProvider);
		
		// In case there's bound view and configured layer, perform setup
		if(_mapRendererView != null && _obfMapRasterLayer != null)
			_mapRendererView.setMapLayerProvider(_obfMapRasterLayer, _obfMapRasterLayerProvider);
	}
	
	/**
	 * Reference to OBF map symbols provider (if used)
	 */
	private IMapTiledSymbolsProvider _obfMapSymbolsProvider;
	
	/**
	 * Update OBF map symbols provider and everything that depends on it
	 */
	private void updateObfMapSymbolsProvider() {
		// If there's current provider and bound view, remove it
		if (_obfMapSymbolsProvider != null && _mapRendererView != null)
			_mapRendererView.removeSymbolsProvider(_obfMapSymbolsProvider);
		
		// Create new OBF map symbols provider
		_obfMapSymbolsProvider = new MapObjectsSymbolsProvider(
				_mapPrimitivesProvider,
				_referenceTileSize);
		
		// If there's bound view, add new provider
		if (_mapRendererView != null)
			_mapRendererView.addSymbolsProvider(_obfMapSymbolsProvider);
	}
	
	/**
	 * Apply current context to view
	 */
	private void apply() {
		if (_mapRendererView instanceof AtlasMapRendererView)
			((AtlasMapRendererView)_mapRendererView).setReferenceTileSizeOnScreenInPixels(_referenceTileSize);
		
		// Layers
		if (_obfMapRasterLayer != null && _obfMapRasterLayerProvider != null)
			_mapRendererView.setMapLayerProvider(_obfMapRasterLayer, _obfMapRasterLayerProvider);
		
		// Symbols
		if (_obfMapSymbolsProvider != null)
			_mapRendererView.addSymbolsProvider(_obfMapSymbolsProvider);
	}
	
	/**
	 * Setup OBF map on layer 0 with symbols
	 * @param obfsCollection OBFs collection
	 */
	public void setupObfMap(ResolvedMapStyle mapStyle, IObfsCollection obfsCollection) {
		setupObfMap(mapStyle, obfsCollection, 0, true);
	}
	
	/**
	 * Setup OBF map on specified layer with optional symbols
	 * @param obfsCollection OBFs collection
	 * @param layer Layer index
	 * @param withSymbols True if with symbols, false otherwise
	 */
	public void setupObfMap(ResolvedMapStyle mapStyle,
			IObfsCollection obfsCollection,
			int layer,
			boolean withSymbols) {
		synchronized (_syncObject) {
			boolean update = false;
			
			if (_mapStyle != mapStyle) {
				_mapStyle = mapStyle;
				update = true;
			}
			
			if (_obfsCollection != obfsCollection) {
				_obfsCollection = obfsCollection;
				update = true;
			}
			
			if (_obfMapRasterLayer == null || _obfMapRasterLayer != layer) {
				_obfMapRasterLayer = layer;
				update = true;
			}
			
			if (withSymbols != (_obfMapSymbolsProvider != null)) {
				update = true;
			}
			
			if (!update)
				return;
			
			updateMapPresentationEnvironment();
			updateMapPrimitiviser();
			updateMapPrimitivesProvider();
			updateObfMapObjectsProvider();
			updateObfMapRasterLayerProvider();
			updateObfMapSymbolsProvider();
		}
	}
}
