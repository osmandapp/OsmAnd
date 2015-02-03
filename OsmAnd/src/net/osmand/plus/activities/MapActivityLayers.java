package net.osmand.plus.activities;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import net.osmand.CallbackWithObject;
import net.osmand.ResultMatcher;
import net.osmand.StateChangedListener;
import net.osmand.access.AccessibleToast;
import net.osmand.data.AmenityType;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.Item;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.poi.PoiLegacyFilter;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.DownloadedRegionsLayer;
import net.osmand.plus.views.FavoritesLayer;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.MapTextLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.plus.views.PointLocationLayer;
import net.osmand.plus.views.PointNavigationLayer;
import net.osmand.plus.views.RouteLayer;
import net.osmand.plus.views.TransportInfoLayer;
import net.osmand.plus.views.TransportStopsLayer;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.ListAdapter;
import android.widget.Toast;

/**
 * Object is responsible to maintain layers using by map activity 
 */
public class MapActivityLayers {

	private final MapActivity activity;
	
	// the order of layer should be preserved ! when you are inserting new layer
	private MapTileLayer mapTileLayer; 
	private MapVectorLayer mapVectorLayer;
	private GPXLayer gpxLayer;
	private RouteLayer routeLayer;
	private POIMapLayer poiMapLayer;
	private FavoritesLayer favoritesLayer;
	private TransportStopsLayer transportStopsLayer;
	private TransportInfoLayer transportInfoLayer;
	private PointLocationLayer locationLayer;
	private PointNavigationLayer navigationLayer;
	private MapInfoLayer mapInfoLayer;
	private MapTextLayer mapTextLayer;
	private ContextMenuLayer contextMenuLayer;
	private MapControlsLayer mapControlsLayer;
	private DownloadedRegionsLayer downloadedRegionsLayer;

	public MapActivityLayers(MapActivity activity) {
		this.activity = activity;
	}

	public OsmandApplication getApplication(){
		return (OsmandApplication) activity.getApplication();
	}
	
	
	public void createLayers(final OsmandMapTileView mapView){
		
		OsmandApplication app = (OsmandApplication) getApplication();
		RoutingHelper routingHelper = app.getRoutingHelper();
		// first create to make accessible
		mapTextLayer = new MapTextLayer();
		// 5.95 all labels
		mapView.addLayer(mapTextLayer, 5.95f);
		// mapView.addLayer(underlayLayer, -0.5f);
		mapTileLayer = new MapTileLayer(true);
		mapView.addLayer(mapTileLayer, 0.0f);
		mapView.setMainLayer(mapTileLayer);
		
		// 0.5 layer
		mapVectorLayer = new MapVectorLayer(mapTileLayer, false);
		mapView.addLayer(mapVectorLayer, 0.5f);

		downloadedRegionsLayer = new DownloadedRegionsLayer();
		mapView.addLayer(downloadedRegionsLayer, 0.5f);

		// 0.9 gpx layer
		gpxLayer = new GPXLayer();
		mapView.addLayer(gpxLayer, 0.9f);
		
		// 1. route layer
		routeLayer = new RouteLayer(routingHelper);
		mapView.addLayer(routeLayer, 1);
		
		// 2. osm bugs layer
		// 3. poi layer
		poiMapLayer = new POIMapLayer(activity);
		// 4. favorites layer
		favoritesLayer = new FavoritesLayer();
		// 5. transport layer
		transportStopsLayer = new TransportStopsLayer();
		// 5.5 transport info layer 
		transportInfoLayer = new TransportInfoLayer(TransportRouteHelper.getInstance());
		mapView.addLayer(transportInfoLayer, 5.5f);
		// 5.95 all text labels
		// 6. point location layer 
		locationLayer = new PointLocationLayer(activity.getMapViewTrackingUtilities());
		mapView.addLayer(locationLayer, 6);
		// 7. point navigation layer
		navigationLayer = new PointNavigationLayer(activity);
		mapView.addLayer(navigationLayer, 7);
		// 8. context menu layer 
		contextMenuLayer = new ContextMenuLayer(activity);
		mapView.addLayer(contextMenuLayer, 8);
		// 9. map info layer
		mapInfoLayer = new MapInfoLayer(activity, routeLayer);
		mapView.addLayer(mapInfoLayer, 9);
		// 11. route info layer
		mapControlsLayer = new MapControlsLayer(activity);
		mapView.addLayer(mapControlsLayer, 11);
		
		app.getSettings().MAP_TRANSPARENCY.addListener(new StateChangedListener<Integer>() {
			@Override
			public void stateChanged(Integer change) {
				mapTileLayer.setAlpha(change);
				mapVectorLayer.setAlpha(change);
				mapView.refreshMap();
			}
		});
		
		OsmandPlugin.createLayers(mapView, activity);
		app.getAppCustomization().createLayers(mapView, activity);
	}

	
	public void updateLayers(OsmandMapTileView mapView){
		OsmandSettings settings = getApplication().getSettings();
		updateMapSource(mapView, settings.MAP_TILE_SOURCES);
		boolean showStops = settings.getCustomRenderBooleanProperty(OsmandSettings.TRANSPORT_STOPS_OVER_MAP).get();
		if(mapView.getLayers().contains(transportStopsLayer) != showStops){
			if(showStops){
				mapView.addLayer(transportStopsLayer, 5);
			} else {
				mapView.removeLayer(transportStopsLayer);
			}
		}
		

		if(mapView.getLayers().contains(poiMapLayer) != settings.SHOW_POI_OVER_MAP.get()){
			if(settings.SHOW_POI_OVER_MAP.get()){
				mapView.addLayer(poiMapLayer, 3);
			} else {
				mapView.removeLayer(poiMapLayer);
			}
		}
		
		if(mapView.getLayers().contains(favoritesLayer) != settings.SHOW_FAVORITES.get()){
			if(settings.SHOW_FAVORITES.get()){
				mapView.addLayer(favoritesLayer, 4);
			} else {
				mapView.removeLayer(favoritesLayer);
			}
		}
		OsmandPlugin.refreshLayers(mapView, activity);
	}
	
	public void updateMapSource(OsmandMapTileView mapView, CommonPreference<String> settingsToWarnAboutMap){
		OsmandSettings settings = getApplication().getSettings();
		
		// update transparency
		int mapTransparency = settings.MAP_UNDERLAY.get() == null ? 255 : settings.MAP_TRANSPARENCY.get();
		mapTileLayer.setAlpha(mapTransparency);
		mapVectorLayer.setAlpha(mapTransparency);
		
		ITileSource newSource = settings.getMapTileSource(settings.MAP_TILE_SOURCES == settingsToWarnAboutMap);
		ITileSource oldMap = mapTileLayer.getMap();
		if (newSource != oldMap) {
			if (oldMap instanceof SQLiteTileSource) {
				((SQLiteTileSource) oldMap).closeDB();
			}
			mapTileLayer.setMap(newSource);
		}
		
		boolean vectorData = !settings.MAP_ONLINE_DATA.get();
		mapTileLayer.setVisible(!vectorData);
		mapVectorLayer.setVisible(vectorData);
		if(vectorData){
			mapView.setMainLayer(mapVectorLayer);
		} else {
			mapView.setMainLayer(mapTileLayer);
		}
	}

	
	
	

	public AlertDialog showGPXFileLayer(List<String> files, final OsmandMapTileView mapView) {
		final OsmandSettings settings = getApplication().getSettings();
		CallbackWithObject<GPXFile[]> callbackWithObject = new CallbackWithObject<GPXFile[]>() {
			@Override
			public boolean processResult(GPXFile[] result) {
				WptPt locToShow = null;
				for (GPXFile g : result) {
					if (g.showCurrentTrack) {
						if (!settings.SAVE_TRACK_TO_GPX.get() && !
								settings.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
							AccessibleToast.makeText(activity, R.string.gpx_monitoring_disabled_warn, Toast.LENGTH_LONG).show();
						} else {
							g.path = getString(R.string.show_current_gpx_title);
						}
						break;
					}
					if (!g.showCurrentTrack || locToShow == null) {
						locToShow = g.findPointToShow();
					}
				}
				getApplication().getSelectedGpxHelper().setGpxFileToDisplay(result);
				if (locToShow != null) {
					mapView.getAnimatedDraggingThread().startMoving(locToShow.lat, locToShow.lon,
							mapView.getZoom(), true);
				}
				mapView.refreshMap();
				activity.getMapActions().refreshDrawer();
				return true;
			}
		};

		if (files == null) {
			return GpxUiHelper.selectGPXFile(activity, true, true, callbackWithObject);
		} else {
			return GpxUiHelper.selectGPXFile(files, activity, true, true, callbackWithObject);
		}
	}
	
	
	
	
	
	public AlertDialog selectPOIFilterLayer(final OsmandMapTileView mapView, final PoiLegacyFilter[] selected){
		final List<PoiLegacyFilter> userDefined = new ArrayList<PoiLegacyFilter>();
		OsmandApplication app = (OsmandApplication)getApplication();
		final PoiFiltersHelper poiFilters = app.getPoiFilters();
		final ContextMenuAdapter adapter = new ContextMenuAdapter(activity);
		
		Item is = adapter.item(getString(R.string.any_poi));
		if(RenderingIcons.containsBigIcon("null")) {
			is.icon(RenderingIcons.getBigIconResourceId("null"));
		}
		is.reg();
		// 2nd custom
		adapter.item(getString(R.string.poi_filter_custom_filter)).icon(RenderingIcons.getBigIconResourceId("user_defined")).reg();
		
		for (PoiLegacyFilter f : poiFilters.getUserDefinedPoiFilters()) {
			Item it = adapter.item(f.getName());
			if (RenderingIcons.containsBigIcon(f.getSimplifiedId())) {
				it.icon(RenderingIcons.getBigIconResourceId(f.getSimplifiedId()));
			} else {
				it.icon(RenderingIcons.getBigIconResourceId("user_defined"));
			}
			it.reg();
			userDefined.add(f);
		}
		final AmenityType[] categories = AmenityType.getCategories();
		for(AmenityType t : categories){
			Item it = adapter.item(OsmAndFormatter.toPublicString(t, activity.getMyApplication()));
			if(RenderingIcons.containsBigIcon(t.toString().toLowerCase())) {
				it.icon(RenderingIcons.getBigIconResourceId(t.toString().toLowerCase()));
			}
			it.reg();
		}
		Builder builder = new AlertDialog.Builder(activity);
		ListAdapter listAdapter =adapter.createListAdapter(activity, app.getSettings().isLightContentMenu());
		builder.setAdapter(listAdapter, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == 1){
					String filterId = PoiLegacyFilter.CUSTOM_FILTER_ID; 
					getApplication().getSettings().setPoiFilterForMap(filterId);
					Intent newIntent = new Intent(activity, EditPOIFilterActivity.class);
					newIntent.putExtra(EditPOIFilterActivity.AMENITY_FILTER, filterId);
					newIntent.putExtra(EditPOIFilterActivity.SEARCH_LAT, mapView.getLatitude());
					newIntent.putExtra(EditPOIFilterActivity.SEARCH_LON, mapView.getLongitude());
					activity.startActivity(newIntent);
				} else {
					String filterId;
					if (which == 0) {
						filterId = PoiFiltersHelper.getOsmDefinedFilterId(null);
					} else if (which <= userDefined.size() + 1) {
						filterId = userDefined.get(which - 2).getFilterId();
					} else {
						filterId = PoiFiltersHelper.getOsmDefinedFilterId(categories[which - userDefined.size() - 2]);
					}
					getApplication().getSettings().setPoiFilterForMap(filterId);
					PoiLegacyFilter f = poiFilters.getFilterById(filterId);
					if (f != null) {
						f.clearNameFilter();
					}
					poiMapLayer.setFilter(f);
					mapView.refreshMap();
					if(selected != null && selected.length > 0) {
						selected[0] = f;
					}
				}
			}
			
		});
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		return builder.show();
	}

	public void selectMapLayer(final OsmandMapTileView mapView){
		if(OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) == null) {
			AccessibleToast.makeText(activity, R.string.map_online_plugin_is_not_installed, Toast.LENGTH_LONG).show();
			return;
		}
		final OsmandSettings settings = getApplication().getSettings();
		
		final LinkedHashMap<String, String> entriesMap = new LinkedHashMap<String, String>();
		
		
		final String layerOsmVector = "LAYER_OSM_VECTOR";
		final String layerInstallMore = "LAYER_INSTALL_MORE";
		final String layerEditInstall = "LAYER_EDIT";
		
		entriesMap.put(layerOsmVector, getString(R.string.vector_data));
		entriesMap.putAll(settings.getTileSourceEntries());
		entriesMap.put(layerInstallMore, getString(R.string.install_more));
		entriesMap.put(layerEditInstall, getString(R.string.maps_define_edit));
		
		final List<Entry<String, String>> entriesMapList = new ArrayList<Entry<String, String>>(entriesMap.entrySet());
		
		Builder builder = new AlertDialog.Builder(activity);
		
		String selectedTileSourceKey = settings.MAP_TILE_SOURCES.get();		

		int selectedItem = -1;
		if (!settings.MAP_ONLINE_DATA.get()) {
			selectedItem = 0;
		} else {
		
			Entry<String, String> selectedEntry = null;
			for (Entry<String, String> entry : entriesMap.entrySet()) {
				if (entry.getKey().equals(selectedTileSourceKey)) {
					selectedEntry = entry;
					break;
				}
			}
			if (selectedEntry != null) {
				selectedItem = 0;
				entriesMapList.remove(selectedEntry);
				entriesMapList.add(0, selectedEntry);
			}
		}
		
		final String[] items = new String[entriesMapList.size()];
		int i = 0;
		for (Entry<String, String> entry : entriesMapList) {
			items[i++] = entry.getValue();
		}
		
		builder.setSingleChoiceItems(items, selectedItem, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String layerKey = entriesMapList.get(which).getKey();
				if (layerKey.equals(layerOsmVector)) {
					settings.MAP_ONLINE_DATA.set(false);
					updateMapSource(mapView, null);
				} else if (layerKey.equals(layerEditInstall)) {
					OsmandRasterMapsPlugin.defineNewEditLayer(activity, new ResultMatcher<TileSourceTemplate>() {

						@Override
						public boolean publish(TileSourceTemplate object) {
							settings.MAP_TILE_SOURCES.set(object.getName());
							settings.MAP_ONLINE_DATA.set(true);
							updateMapSource(mapView, settings.MAP_TILE_SOURCES);
							return true;
						}

						@Override
						public boolean isCancelled() {
							return false;
						}
						
					});
				} else if (layerKey.equals(layerInstallMore)) {
					OsmandRasterMapsPlugin.installMapLayers(activity, new ResultMatcher<TileSourceTemplate>() {
						TileSourceTemplate template = null;
						int count = 0;
						@Override
						public boolean publish(TileSourceTemplate object) {
							if(object == null){
								if(count == 1){
									settings.MAP_TILE_SOURCES.set(template.getName());
									settings.MAP_ONLINE_DATA.set(true);
									updateMapSource(mapView, settings.MAP_TILE_SOURCES);
								} else {
									selectMapLayer(mapView);
								}
							} else {
								count ++;
								template = object;
							}
							return false;
						}
						
						@Override
						public boolean isCancelled() {
							return false;
						}
					});
				} else {
					settings.MAP_TILE_SOURCES.set(layerKey);
					settings.MAP_ONLINE_DATA.set(true);
					updateMapSource(mapView, settings.MAP_TILE_SOURCES);
				}

				dialog.dismiss();
			}
			
		});
		builder.show();
	}

	
	private String getString(int resId) {
		return activity.getString(resId);
	}

	public PointNavigationLayer getNavigationLayer() {
		return navigationLayer;
	}
	
	public GPXLayer getGpxLayer() {
		return gpxLayer;
	}
	
	public ContextMenuLayer getContextMenuLayer() {
		return contextMenuLayer;
	}
	
	public FavoritesLayer getFavoritesLayer() {
		return favoritesLayer;
	}
	
	public MapTextLayer getMapTextLayer() {
		return mapTextLayer;
	}
	
	public PointLocationLayer getLocationLayer() {
		return locationLayer;
	}
	
	public MapInfoLayer getMapInfoLayer() {
		return mapInfoLayer;
	}
	
	public MapControlsLayer getMapControlsLayer() {
		return mapControlsLayer;
	}
	
	
	public MapTileLayer getMapTileLayer() {
		return mapTileLayer;
	}
	
	public MapVectorLayer getMapVectorLayer() {
		return mapVectorLayer;
	}
	
	public POIMapLayer getPoiMapLayer() {
		return poiMapLayer;
	}
	
	public TransportInfoLayer getTransportInfoLayer() {
		return transportInfoLayer;
	}
	
}
