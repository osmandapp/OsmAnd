package net.osmand.plus.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.osmand.access.AccessibleToast;
import net.osmand.Algoritms;
import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.OsmAndFormatter;
import net.osmand.GPXUtilities.GPXFileResult;
import net.osmand.data.AmenityType;
import net.osmand.map.ITileSource;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.PoiFiltersHelper;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.views.BaseMapLayer;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.FavoritesLayer;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmBugsLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.plus.views.PointLocationLayer;
import net.osmand.plus.views.PointNavigationLayer;
import net.osmand.plus.views.RouteInfoLayer;
import net.osmand.plus.views.RouteLayer;
import net.osmand.plus.views.TransportInfoLayer;
import net.osmand.plus.views.TransportStopsLayer;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * Object is responsible to maintain layers using by map activity 
 */
public class MapActivityLayers {
	
	private final MapActivity activity;
	
	// the order of layer should be preserved ! when you are inserting new layer
	private MapTileLayer mapTileLayer; 
	private MapVectorLayer mapVectorLayer;
	private MapTileLayer overlayLayer;
	private MapTileLayer underlayLayer;
	private GPXLayer gpxLayer;
	private RouteLayer routeLayer;
	private OsmBugsLayer osmBugsLayer;
	private POIMapLayer poiMapLayer;
	private FavoritesLayer favoritesLayer;
	private TransportStopsLayer transportStopsLayer;
	private TransportInfoLayer transportInfoLayer;
	private PointLocationLayer locationLayer;
	private PointNavigationLayer navigationLayer;
	private MapInfoLayer mapInfoLayer;
	private ContextMenuLayer contextMenuLayer;
	private RouteInfoLayer routeInfoLayer;
	private MapControlsLayer mapControlsLayer;


	public MapActivityLayers(MapActivity activity) {
		this.activity = activity;
	}

	public OsmandApplication getApplication(){
		return (OsmandApplication) activity.getApplication();
	}
	
	
	public void createLayers(OsmandMapTileView mapView){
		
		RoutingHelper routingHelper = ((OsmandApplication) getApplication()).getRoutingHelper();
		
		underlayLayer = new MapTileLayer(false);
		// mapView.addLayer(underlayLayer, -0.5f);
		
		mapTileLayer = new MapTileLayer(true);
		mapView.addLayer(mapTileLayer, 0.0f);
		mapView.setMainLayer(mapTileLayer);
		
		// 0.5 layer
		mapVectorLayer = new MapVectorLayer(mapTileLayer);
		mapView.addLayer(mapVectorLayer, 0.5f);
		
		overlayLayer = new MapTileLayer(false);
		// mapView.addLayer(overlayLayer, 0.7f);
		
		// 0.9 gpx layer
		gpxLayer = new GPXLayer();
		mapView.addLayer(gpxLayer, 0.9f);
		
		// 1. route layer
		routeLayer = new RouteLayer(routingHelper);
		mapView.addLayer(routeLayer, 1);
		
		// 2. osm bugs layer
		osmBugsLayer = new OsmBugsLayer(activity);
		// 3. poi layer
		poiMapLayer = new POIMapLayer();
		// 4. favorites layer
		favoritesLayer = new FavoritesLayer();
		// 5. transport layer
		transportStopsLayer = new TransportStopsLayer();
		// 5.5 transport info layer 
		transportInfoLayer = new TransportInfoLayer(TransportRouteHelper.getInstance());
		mapView.addLayer(transportInfoLayer, 5.5f);
		// 6. point navigation layer
		navigationLayer = new PointNavigationLayer();
		mapView.addLayer(navigationLayer, 6);
		// 7. point location layer 
		locationLayer = new PointLocationLayer();
		mapView.addLayer(locationLayer, 7);
		// 8. map info layer
		mapInfoLayer = new MapInfoLayer(activity, routeLayer);
		mapView.addLayer(mapInfoLayer, 8);
		// 9. context menu layer 
		contextMenuLayer = new ContextMenuLayer(activity);
		mapView.addLayer(contextMenuLayer, 9);
		// 10. route info layer
		routeInfoLayer = new RouteInfoLayer(routingHelper, activity);
		mapView.addLayer(routeInfoLayer, 10);
		
		// 11. route info layer
		mapControlsLayer = new MapControlsLayer(activity);
		mapView.addLayer(mapControlsLayer, 11);

	}
	
	
	public void updateLayers(OsmandMapTileView mapView){
		OsmandSettings settings = getApplication().getSettings();
		if(mapView.getLayers().contains(transportStopsLayer) != settings.SHOW_TRANSPORT_OVER_MAP.get()){
			if(settings.SHOW_TRANSPORT_OVER_MAP.get()){
				mapView.addLayer(transportStopsLayer, 5);
			} else {
				mapView.removeLayer(transportStopsLayer);
			}
		}
		if(mapView.getLayers().contains(osmBugsLayer) != settings.SHOW_OSM_BUGS.get()){
			if(settings.SHOW_OSM_BUGS.get()){
				mapView.addLayer(osmBugsLayer, 2);
			} else {
				mapView.removeLayer(osmBugsLayer);
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
		updateGPXLayer();
	}
	
	public void updateMapSource(OsmandMapTileView mapView, CommonPreference<String> settingsToWarnAboutMap){
		OsmandSettings settings = getApplication().getSettings();
		
		// update transparency
		overlayLayer.setAlpha(settings.MAP_OVERLAY_TRANSPARENCY.get());
		int mapTransparency = settings.MAP_UNDERLAY.get() == null ? 255 :  settings.MAP_TRANSPARENCY.get();
		mapTileLayer.setAlpha(mapTransparency);
		mapVectorLayer.setAlpha(mapTransparency);
		
		// update overlay layer
		updateLayer(mapView, settings, overlayLayer, settings.MAP_OVERLAY, 0.7f, settings.MAP_OVERLAY == settingsToWarnAboutMap);
		updateLayer(mapView, settings, underlayLayer, settings.MAP_UNDERLAY, -0.5f, settings.MAP_UNDERLAY == settingsToWarnAboutMap);
		
		boolean vectorData = settings.MAP_VECTOR_DATA.get();
		OsmandApplication app = ((OsmandApplication)getApplication());
		ResourceManager rm = app.getResourceManager();
		if(vectorData && !app.isApplicationInitializing()){
			if(rm.getRenderer().isEmpty()){
				AccessibleToast.makeText(activity, activity.getString(R.string.no_vector_map_loaded), Toast.LENGTH_LONG).show();
				vectorData = false;
			}
		}
		ITileSource newSource = settings.getMapTileSource(settings.MAP_TILE_SOURCES == settingsToWarnAboutMap);
		ITileSource oldMap = mapTileLayer.getMap();
		if(oldMap instanceof SQLiteTileSource){
			((SQLiteTileSource)oldMap).closeDB();
		}
		mapTileLayer.setMap(newSource);
		mapTileLayer.setVisible(!vectorData);
		mapVectorLayer.setVisible(vectorData);
		if(vectorData){
			mapView.setMainLayer(mapVectorLayer);
		} else {
			mapView.setMainLayer(mapTileLayer);
		}
	}

	private void updateLayer(OsmandMapTileView mapView, OsmandSettings settings,
			MapTileLayer layer, CommonPreference<String> preference, float layerOrder, boolean warnWhenSelected) {
		ITileSource overlay = settings.getTileSourceByName(preference.get(), warnWhenSelected);
		if(!Algoritms.objectEquals(overlay, layer.getMap())){
			if(overlay == null){
				mapView.removeLayer(layer);
			} else {
				mapView.addLayer(layer, layerOrder);
			}
			layer.setMap(overlay);
			mapView.refreshMap();
		}
	}
	
	public void openLayerSelectionDialog(final OsmandMapTileView mapView){
		List<String> layersList = new ArrayList<String>();
		final OsmandSettings settings = getApplication().getSettings();
		layersList.add(getString(R.string.layer_map));
		layersList.add(getString(R.string.layer_poi));
		layersList.add(getString(R.string.layer_favorites));
		layersList.add(getString(R.string.layer_overlay));
		layersList.add(getString(R.string.layer_underlay));
		layersList.add(getString(R.string.layer_gpx_layer));
		layersList.add(getString(R.string.layer_transport));
		layersList.add(getString(R.string.layer_osm_bugs));
		
		
		final int routeInfoInd = routeInfoLayer.couldBeVisible() ? layersList.size() : -1;
		if(routeInfoLayer.couldBeVisible()){
			layersList.add(getString(R.string.layer_route));
		}
		final int transportRouteInfoInd = !TransportRouteHelper.getInstance().routeIsCalculated() ? - 1 : layersList.size(); 
		if(transportRouteInfoInd > -1){
			layersList.add(getString(R.string.layer_transport_route));
		}
		
		final boolean[] selected = new boolean[layersList.size()];
		Arrays.fill(selected, true);
		selected[1] = settings.SHOW_POI_OVER_MAP.get();
		selected[2] = settings.SHOW_FAVORITES.get();
		selected[3] = overlayLayer.getMap() != null;
		selected[4] = underlayLayer.getMap() != null;
		selected[5] = gpxLayer.isVisible();
		selected[6] = settings.SHOW_TRANSPORT_OVER_MAP.get();
		selected[7] = settings.SHOW_OSM_BUGS.get();
		if(routeInfoInd != -1){
			selected[routeInfoInd] = routeInfoLayer.isUserDefinedVisible(); 
		}
		if(transportRouteInfoInd != -1){
			selected[transportRouteInfoInd] = transportInfoLayer.isVisible(); 
		}
		
		Builder builder = new AlertDialog.Builder(activity);
		builder.setMultiChoiceItems(layersList.toArray(new String[layersList.size()]), selected, new DialogInterface.OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int item, boolean isChecked) {
				if (item == 0) {
					dialog.dismiss();
					selectMapLayer(mapView);
				} else if(item == 1){
					if(isChecked){
						selectPOIFilterLayer(mapView);
					}
					settings.SHOW_POI_OVER_MAP.set(isChecked);
				} else if(item == 2){
					settings.SHOW_FAVORITES.set(isChecked);
				} else if(item == 3){
					if(overlayLayer.getMap() != null){
						settings.MAP_OVERLAY.set(null);
						updateMapSource(mapView, null);
					} else {
						dialog.dismiss();
						selectMapOverlayLayer(mapView, settings.MAP_OVERLAY, settings.MAP_OVERLAY_TRANSPARENCY, 
								overlayLayer);
					}
				} else if(item == 4){
					if(underlayLayer.getMap() != null){
						settings.MAP_UNDERLAY.set(null);
						updateMapSource(mapView, null);
					} else {
						dialog.dismiss();
						selectMapOverlayLayer(mapView, settings.MAP_UNDERLAY,settings.MAP_TRANSPARENCY, 
								mapTileLayer, mapVectorLayer);
					}
				} else if(item == 5){
					if(gpxLayer.isVisible()){
						getApplication().setGpxFileToDisplay(null);
						gpxLayer.clearCurrentGPX();
					} else {
						dialog.dismiss();
						showGPXFileLayer(mapView);
					}
				} else if(item == 6){
					settings.SHOW_TRANSPORT_OVER_MAP.set(isChecked);
				} else if(item == 7){
					settings.SHOW_OSM_BUGS.set(isChecked);
				} else if(item == routeInfoInd){
					routeInfoLayer.setVisible(isChecked);
				} else if(item == transportRouteInfoInd){
					transportInfoLayer.setVisible(isChecked);
				}
				updateLayers(mapView);
				mapView.refreshMap();
			}
		});
		builder.show();
	}
	
	public void showGPXFileLayer(final OsmandMapTileView mapView){
		final OsmandSettings settings = getApplication().getSettings();
		selectGPXFileLayer(new CallbackWithObject<GPXFileResult>() {
			@Override
			public boolean processResult(GPXFileResult result) {
				settings.SHOW_FAVORITES.set(true);
				if (result != null) {
					getApplication().setGpxFileToDisplay(result);
					updateGPXLayer();
					mapView.refreshMap();
				}
				return true;
			}
		});
	}
	
	private void updateGPXLayer(){
		GPXFileResult gpxFileToDisplay = getApplication().getGpxFileToDisplay();
		if(gpxFileToDisplay == null){
			gpxLayer.setTracks(null);
		} else {
			gpxLayer.setTracks(gpxFileToDisplay.locations);
		}
	}
	
	public void selectGPXFileLayer(final CallbackWithObject<GPXFileResult> callbackWithObject) {
		final List<String> list = new ArrayList<String>();
		final OsmandSettings settings = getApplication().getSettings();
		final File dir = settings.extendOsmandPath(ResourceManager.APP_DIR + SavingTrackHelper.TRACKS_PATH);
		if (dir != null && dir.canRead()) {
			File[] files = dir.listFiles();
			if (files != null) {
				Arrays.sort(files, new Comparator<File>() {
					@Override
					public int compare(File object1, File object2) {
						if (object1.lastModified() > object2.lastModified()) {
							return -1;
						} else if (object1.lastModified() == object2.lastModified()) {
							return 0;
						}
						return 1;
					}

				});

				for (File f : files) {
					if (f.getName().endsWith(".gpx")) { //$NON-NLS-1$
						list.add(f.getName());
					}
				}
			}
		}
		
		if(list.isEmpty()){
			AccessibleToast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
		} else {
			Builder builder = new AlertDialog.Builder(activity);
			builder.setItems(list.toArray(new String[list.size()]), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					final ProgressDialog dlg = ProgressDialog.show(activity, getString(R.string.loading),
							getString(R.string.loading_data));
					final File f = new File(dir, list.get(which));
					new Thread(new Runnable() {
						@Override
						public void run() {
							final GPXFileResult res = GPXUtilities.loadGPXFile(activity, f);
							if (res.error != null) {
								activity.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										AccessibleToast.makeText(activity, res.error, Toast.LENGTH_LONG).show();
									}
								});

							}
							dlg.dismiss();
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									callbackWithObject.processResult(res);
									
								}
							});
						}

						
					}, "Loading gpx").start(); //$NON-NLS-1$
				}

			});
			builder.show();
		}
	}
	
	private void selectPOIFilterLayer(final OsmandMapTileView mapView){
		final List<PoiFilter> userDefined = new ArrayList<PoiFilter>();
		List<String> list = new ArrayList<String>();
		list.add(getString(R.string.any_poi));
		
		final PoiFiltersHelper poiFilters = ((OsmandApplication)getApplication()).getPoiFilters();
		for (PoiFilter f : poiFilters.getUserDefinedPoiFilters()) {
			userDefined.add(f);
			list.add(f.getName());
		}
		for(AmenityType t : AmenityType.values()){
			list.add(OsmAndFormatter.toPublicString(t, activity));
		}
		Builder builder = new AlertDialog.Builder(activity);
		builder.setItems(list.toArray(new String[list.size()]), new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String filterId;
				if (which == 0) {
					filterId = PoiFiltersHelper.getOsmDefinedFilterId(null);
				} else if (which <= userDefined.size()) {
					filterId = userDefined.get(which - 1).getFilterId();
				} else {
					filterId = PoiFiltersHelper.getOsmDefinedFilterId(AmenityType.values()[which - userDefined.size() - 1]);
				}
				if(filterId.equals(PoiFilter.CUSTOM_FILTER_ID)){
					Intent newIntent = new Intent(activity, EditPOIFilterActivity.class);
					newIntent.putExtra(EditPOIFilterActivity.AMENITY_FILTER, filterId);
					newIntent.putExtra(EditPOIFilterActivity.SEARCH_LAT, mapView.getLatitude());
					newIntent.putExtra(EditPOIFilterActivity.SEARCH_LON, mapView.getLongitude());
					activity.startActivity(newIntent);
				} else {
					getApplication().getSettings().setPoiFilterForMap(filterId);
					poiMapLayer.setFilter(poiFilters.getFilterById(filterId));
					mapView.refreshMap();
				}
			}
			
		});
		builder.show();
	}
	
	private void selectMapLayer(final OsmandMapTileView mapView){
		final OsmandSettings settings = getApplication().getSettings();
		Map<String, String> entriesMap = settings.getTileSourceEntries();
		Builder builder = new AlertDialog.Builder(activity);
		final ArrayList<String> keys = new ArrayList<String>(entriesMap.keySet());
		final String[] items = new String[entriesMap.size() + 2];
		items[0] = getString(R.string.vector_data);
		int i = 1;
		for(String it : entriesMap.values()){
			items[i++] = it;
		}
		
		items[i] = getString(R.string.install_more);
		builder.setItems(items, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					MapRenderRepositories r = ((OsmandApplication) getApplication()).getResourceManager().getRenderer();
					if (r.isEmpty()) {
						AccessibleToast.makeText(activity, getString(R.string.no_vector_map_loaded), Toast.LENGTH_LONG).show();
						return;
					} else {
						settings.MAP_VECTOR_DATA.set(true);
					}
					updateMapSource(mapView, null);
				} else if (which == items.length - 1) {
					SettingsActivity.installMapLayers(activity, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							openLayerSelectionDialog(mapView);
						}
					});
				} else {
					settings.MAP_TILE_SOURCES.set(keys.get(which - 1));
					settings.MAP_VECTOR_DATA.set(false);
					updateMapSource(mapView, settings.MAP_TILE_SOURCES);
				}

			}
			
		});
		builder.show();
	}
	
	
	private void selectMapOverlayLayer(final OsmandMapTileView mapView, 
			final CommonPreference<String> mapPref, final CommonPreference<Integer> transparencyPref,
			final BaseMapLayer... transparencyToChange){
		final OsmandSettings settings = getApplication().getSettings();
		Map<String, String> entriesMap = settings.getTileSourceEntries();
		final ArrayList<String> keys = new ArrayList<String>(entriesMap.keySet());
		Builder builder = new AlertDialog.Builder(activity);
		final String[] items = new String[entriesMap.size() + 1];
		int i = 0;
		for(String it : entriesMap.values()){
			items[i++] = it;
		}
		
		items[i] = getString(R.string.install_more);
		builder.setItems(items, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == items.length - 1){
					SettingsActivity.installMapLayers(activity, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							openLayerSelectionDialog(mapView);							
						}
					});
				} else {
					mapPref.set(keys.get(which));
					mapControlsLayer.showAndHideTransparencyBar(transparencyPref, transparencyToChange);
					updateMapSource(mapView, mapPref);
				}
				
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
	
	public ContextMenuLayer getContextMenuLayer() {
		return contextMenuLayer;
	}
	
	public FavoritesLayer getFavoritesLayer() {
		return favoritesLayer;
	}
	public PointLocationLayer getLocationLayer() {
		return locationLayer;
	}
	
	public MapInfoLayer getMapInfoLayer() {
		return mapInfoLayer;
	}
	
	public POIMapLayer getPoiMapLayer() {
		return poiMapLayer;
	}
	
	public OsmBugsLayer getOsmBugsLayer() {
		return osmBugsLayer;
	}
}
