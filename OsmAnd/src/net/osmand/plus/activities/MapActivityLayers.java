package net.osmand.plus.activities;

import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.osmand.Algoritms;
import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.OsmAndFormatter;
import net.osmand.ResultMatcher;
import net.osmand.data.AmenityType;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.PoiFiltersHelper;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.routing.RoutingHelper;
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
import net.osmand.plus.views.PlanningLayer;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
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
	private PlanningLayer planningLayer;

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
		poiMapLayer = new POIMapLayer(activity);
		// 4. favorites layer
		favoritesLayer = new FavoritesLayer();
		// 5. transport layer
		transportStopsLayer = new TransportStopsLayer();
		// 5.5 transport info layer 
		transportInfoLayer = new TransportInfoLayer(TransportRouteHelper.getInstance());
		mapView.addLayer(transportInfoLayer, 5.5f);
		// 6. point location layer 
		locationLayer = new PointLocationLayer();
		mapView.addLayer(locationLayer, 6);
		// 7. point navigation layer
		navigationLayer = new PointNavigationLayer();
		mapView.addLayer(navigationLayer, 7);
		// 8. context menu layer 
		contextMenuLayer = new ContextMenuLayer(activity);
		mapView.addLayer(contextMenuLayer, 8);
		// 9. map info layer
		mapInfoLayer = new MapInfoLayer(activity, routeLayer);
		mapView.addLayer(mapInfoLayer, 9);
		// 10. route info layer
		routeInfoLayer = new RouteInfoLayer(routingHelper, (LinearLayout) activity.findViewById(R.id.RouteLayout), contextMenuLayer);
		mapView.addLayer(routeInfoLayer, 10);
		// 11. route info layer
		mapControlsLayer = new MapControlsLayer(activity);
		mapView.addLayer(mapControlsLayer, 11);
		// 12. planningLayer for measurement and planning points and tracks
		planningLayer = new PlanningLayer(activity);
		mapView.addLayer(planningLayer, 12);

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
				Toast.makeText(activity, activity.getString(R.string.no_vector_map_loaded), Toast.LENGTH_LONG).show();
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
		
		final TIntArrayList layers = new TIntArrayList();
		final TIntArrayList selectedList = new TIntArrayList();
		final TIntArrayList iconList = new TIntArrayList();
		final OsmandSettings settings = getApplication().getSettings();
		layers.add(R.string.layer_map);
		iconList.add(R.drawable.list_activities_map_src);
		selectedList.add(-1);
		layers.add(R.string.layer_poi);
		iconList.add(R.drawable.list_activities_poi);
		selectedList.add(settings.SHOW_POI_OVER_MAP.get() ? 1 : 0);
		if(settings.SHOW_POI_OVER_MAP.get()){
			layers.add(R.string.layer_poi_label);
			selectedList.add(settings.SHOW_POI_LABEL.get() ? 1 : 0);
			iconList.add(0);
		}
		layers.add(R.string.layer_favorites);
		iconList.add(R.drawable.list_activities_favorites);
		selectedList.add(settings.SHOW_FAVORITES.get() ? 1 : 0);
		layers.add(R.string.layer_gpx_layer);
		selectedList.add(getApplication().getGpxFileToDisplay() != null ? 1 : 0);
		iconList.add(R.drawable.list_activities_gpx_tracks);
		if(routeInfoLayer.couldBeVisible()){
			layers.add(R.string.layer_route);
			selectedList.add(routeInfoLayer.isUserDefinedVisible() ? 1 : 0);
			iconList.add(0);
		}
		layers.add(R.string.layer_transport);
		selectedList.add(settings.SHOW_TRANSPORT_OVER_MAP.get() ? 1 : 0);
		iconList.add(R.drawable.list_activities_transport_stops);
		if(TransportRouteHelper.getInstance().routeIsCalculated()){
			layers.add(R.string.layer_transport_route);
			selectedList.add(routeInfoLayer.isUserDefinedVisible() ? 1 : 0);
			iconList.add(0);
		}
		layers.add(R.string.layer_osm_bugs);
		selectedList.add(settings.SHOW_OSM_BUGS.get() ? 1 : 0);
		iconList.add(R.drawable.list_activities_osm_bugs);
		
		layers.add(R.string.layer_overlay);
		selectedList.add(overlayLayer.getMap() != null ? 1 : 0);
		iconList.add(R.drawable.list_activities_overlay_map);
		layers.add(R.string.layer_underlay);
		selectedList.add(underlayLayer.getMap() != null ? 1 : 0);
		iconList.add(R.drawable.list_activities_underlay_map);
		
		final OnMultiChoiceClickListener listener = new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item, boolean isChecked) {
				if (layers.get(item) == R.string.layer_map) {
					dialog.dismiss();
					selectMapLayer(mapView);
				} else if(layers.get(item) == R.string.layer_poi){
					if(isChecked){
						selectPOIFilterLayer(mapView);
					}
					settings.SHOW_POI_OVER_MAP.set(isChecked);
				} else if(layers.get(item) == R.string.layer_poi_label){
					settings.SHOW_POI_LABEL.set(isChecked);
				} else if(layers.get(item) == R.string.layer_favorites){
					settings.SHOW_FAVORITES.set(isChecked);
				} else if(layers.get(item) == R.string.layer_gpx_layer){
					gpxLayer.clearCurrentGPX();
					if(getApplication().getGpxFileToDisplay() != null){
						getApplication().setGpxFileToDisplay(null, false);
					} else {
						dialog.dismiss();
						showGPXFileLayer(mapView);
					}
				} else if(layers.get(item) == R.string.layer_route){
					routeInfoLayer.setVisible(isChecked);
				} else if(layers.get(item) == R.string.layer_transport_route){
					transportInfoLayer.setVisible(isChecked);
				} else if(layers.get(item) == R.string.layer_transport){
					settings.SHOW_TRANSPORT_OVER_MAP.set(isChecked);
				} else if(layers.get(item) == R.string.layer_osm_bugs){
					settings.SHOW_OSM_BUGS.set(isChecked);
				} else if(layers.get(item) == R.string.layer_overlay){
					if(overlayLayer.getMap() != null){
						settings.MAP_OVERLAY.set(null);
						updateMapSource(mapView, null);
					} else {
						dialog.dismiss();
						selectMapOverlayLayer(mapView, settings.MAP_OVERLAY, settings.MAP_OVERLAY_TRANSPARENCY, 
								overlayLayer);
					}
				} else if(layers.get(item) == R.string.layer_underlay){
					if(underlayLayer.getMap() != null){
						settings.MAP_UNDERLAY.set(null);
						updateMapSource(mapView, null);
					} else {
						dialog.dismiss();
						selectMapOverlayLayer(mapView, settings.MAP_UNDERLAY,settings.MAP_TRANSPARENCY, 
								mapTileLayer, mapVectorLayer);
					}
				}
				updateLayers(mapView);
				mapView.refreshMap();
			}
		};
		Builder b = new AlertDialog.Builder(activity);
		ListView list = new ListView(activity);
//		list.setBackgroundColor(white);
		list.setCacheColorHint(activity.getResources().getColor(R.color.color_transparent));
		b.setView(list);
		final List<String> layerNames = new ArrayList<String>();
		for (int i = 0; i < layers.size(); i++) {
			layerNames.add(getString(layers.get(i)));

		}
		final AlertDialog dlg = b.create();
		final int minWidth = activity.getResources().getDrawable(R.drawable.list_activities_favorites).getMinimumWidth();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, R.layout.layers_list_activity_item, 
				layerNames) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				View row = activity.getLayoutInflater().inflate(R.layout.layers_list_activity_item, null);
				((TextView) row.findViewById(R.id.title)).setText(layerNames.get(position));
				if(iconList.get(position) != 0) {
					Drawable d = activity.getResources().getDrawable(iconList.get(position));
					((ImageView) row.findViewById(R.id.icon)).setImageDrawable(d);
				} else {
					LinearLayout.LayoutParams layoutParams = (android.widget.LinearLayout.LayoutParams) ((ImageView) row.findViewById(R.id.icon)).getLayoutParams();
					layoutParams.leftMargin = minWidth;
				}
				final CheckBox ch = ((CheckBox) row.findViewById(R.id.check_item));
				if(selectedList.get(position) == -1){
					ch.setVisibility(View.INVISIBLE);
				} else {
					ch.setChecked(selectedList.get(position) > 0);
					ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							listener.onClick(dlg, position, isChecked);
						}
					});
				}
//				row.setOnClickListener(new View.OnClickListener() {
//					@Override
//					public void onClick(View v) {
//						if(selectedList.get(position) >= 0) {
//							ch.setChecked(!ch.isChecked());
//						} else {
//							listener.onClick(dlg, position, selectedList.get(position) > 0);
//						}
//					}
//				});
				return row;
			}
		};
		list.setAdapter(adapter);
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(selectedList.get(position) >= 0) {
					CheckBox ch = ((CheckBox) view.findViewById(R.id.check_item));
					ch.setChecked(!ch.isChecked());
				} else {
					listener.onClick(dlg, position, selectedList.get(position) > 0);
				}
			}
		});
		dlg.setCanceledOnTouchOutside(true);
		dlg.show();
	}
	
	public void showGPXFileLayer(final OsmandMapTileView mapView){
		final OsmandSettings settings = getApplication().getSettings();
		selectGPXFileLayer(new CallbackWithObject<GPXFile>() {
			@Override
			public boolean processResult(GPXFile result) {
				GPXFile toShow = result;
				if (toShow == null) {
					if(!settings.SAVE_TRACK_TO_GPX.get()){
						Toast.makeText(activity, R.string.gpx_monitoring_disabled_warn, Toast.LENGTH_SHORT).show();
						return true;
					}
					Map<String, GPXFile> data = activity.getSavingTrackHelper().collectRecordedData();
					if(data.isEmpty()){
						toShow = new GPXFile();						
					} else {
						toShow = data.values().iterator().next();
					}
				}
				
				if(result != null && result.path.contains(getString(R.string.plan_file_name_prefix))){	//if a plan GPX file is loaded, add the points to the pointLocation layer
					activity.getMeasurementActivity().showGPXPlan(result);
				}else{
					getApplication().setGpxFileToDisplay(toShow, result == null);
					updateGPXLayer();
				}
				mapView.refreshMap();
				return true;
			}
		}, true, true);
	}
	
	private void updateGPXLayer(){
		GPXFile gpxFileToDisplay = getApplication().getGpxFileToDisplay();
		if(gpxFileToDisplay == null){
			gpxLayer.setTracks(null);
		} else {
			gpxLayer.setTracks(gpxFileToDisplay.tracks);
		}
	}
	
	public void selectGPXFileLayer(final CallbackWithObject<GPXFile> callbackWithObject, final boolean convertCloudmade,
			final boolean showCurrentGpx) {
		final List<String> list = new ArrayList<String>();
		final OsmandSettings settings = getApplication().getSettings();
		final File dir = settings.extendOsmandPath(ResourceManager.GPX_PATH);
		if (dir != null && dir.canRead()) {
			File[] files = dir.listFiles();
			if (files != null) {
				Arrays.sort(files, new Comparator<File>() {
					@Override
					public int compare(File object1, File object2) {
						if (object1.getName().compareTo(object2.getName()) > 0) {
							return -1;
						} else if (object1.getName().equals(object2.getName())) {
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
			Toast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
		}
		if(!list.isEmpty() || showCurrentGpx){
			Builder builder = new AlertDialog.Builder(activity);
			if(showCurrentGpx){
				list.add(0, getString(R.string.show_current_gpx_title));
			}
			builder.setItems(list.toArray(new String[list.size()]), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					if(showCurrentGpx && which == 0){
						callbackWithObject.processResult(null);
					} else {
						final ProgressDialog dlg = ProgressDialog.show(activity, getString(R.string.loading),
								getString(R.string.loading_data));
						final File f = new File(dir, list.get(which));
						new Thread(new Runnable() {
							@Override
							public void run() {
								final GPXFile res = GPXUtilities.loadGPXFile(activity, f, convertCloudmade);
								dlg.dismiss();
								activity.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										if (res.warning != null) {
											Toast.makeText(activity, res.warning, Toast.LENGTH_LONG).show();
										} else {
											callbackWithObject.processResult(res);
										}
									}
								});
							}

						}, "Loading gpx").start(); //$NON-NLS-1$
					}
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
			if(!f.getFilterId().equals(PoiFilter.BY_NAME_FILTER_ID)){
				userDefined.add(f);
				list.add(f.getName());
			}
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
					PoiFilter f = poiFilters.getFilterById(filterId);
					if(f != null){
						f.clearNameFilter();
					}
					poiMapLayer.setFilter(f);
					mapView.refreshMap();
				}
			}
			
		});
		builder.show();
	}
	
	public void selectMapLayer(final OsmandMapTileView mapView){
		final OsmandSettings settings = getApplication().getSettings();
		
		final LinkedHashMap<String, String> entriesMap = new LinkedHashMap<String, String>();
		
		final String layerOsmVector = "LAYER_OSM_VECTOR";
		final String layerInstallMore = "LAYER_INSTALL_MORE";
		
		entriesMap.put(layerOsmVector, getString(R.string.vector_data));
		entriesMap.putAll(settings.getTileSourceEntries());
		entriesMap.put(layerInstallMore, getString(R.string.install_more));
		
		final List<Entry<String, String>> entriesMapList = new ArrayList<Entry<String, String>>(entriesMap.entrySet());
		
		Builder builder = new AlertDialog.Builder(activity);
		
		String selectedTileSourceKey = settings.MAP_TILE_SOURCES.get();		

		int selectedItem = -1;
		if (settings.MAP_VECTOR_DATA.get()) {
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
					MapRenderRepositories r = ((OsmandApplication) getApplication()).getResourceManager().getRenderer();
					if (r.isEmpty()) {
						Toast.makeText(activity, getString(R.string.no_vector_map_loaded), Toast.LENGTH_LONG).show();
						return;
					} else {
						settings.MAP_VECTOR_DATA.set(true);
					}
					updateMapSource(mapView, null);
				} else if (layerKey.equals(layerInstallMore)) {
					SettingsActivity.installMapLayers(activity, new ResultMatcher<TileSourceTemplate>() {
						TileSourceTemplate template = null;
						int count = 0;
						@Override
						public boolean publish(TileSourceTemplate object) {
							if(object == null){
								if(count == 1){
									settings.MAP_TILE_SOURCES.set(template.getName());
									settings.MAP_VECTOR_DATA.set(false);
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
					settings.MAP_VECTOR_DATA.set(false);
					updateMapSource(mapView, settings.MAP_TILE_SOURCES);
				}

				dialog.dismiss();
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
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == items.length - 1){
					SettingsActivity.installMapLayers(activity, new ResultMatcher<TileSourceTemplate>() {
						TileSourceTemplate template = null;
						int count = 0;
						@Override
						public boolean publish(TileSourceTemplate object) {
							if(object == null){
								if(count == 1){
									mapPref.set(template.getName());
									mapControlsLayer.showAndHideTransparencyBar(transparencyPref, transparencyToChange);
									updateMapSource(mapView, mapPref);
								} else {
									selectMapOverlayLayer(mapView, mapPref, transparencyPref, transparencyToChange);
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
					mapPref.set(keys.get(which));
					mapControlsLayer.showAndHideTransparencyBar(transparencyPref, transparencyToChange);
					updateMapSource(mapView, mapPref);
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
	
	public PlanningLayer getPlanningLayer() {
		return planningLayer;
	}
}
