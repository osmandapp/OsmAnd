package net.osmand.plus.activities;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import android.widget.*;
import net.osmand.CallbackWithObject;
import net.osmand.ResultMatcher;
import net.osmand.StateChangedListener;
import net.osmand.access.AccessibleToast;
import net.osmand.data.AmenityType;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.*;
import net.osmand.plus.ContextMenuAdapter.Item;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.helpers.GpxUiHelper;
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
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.plus.views.PointLocationLayer;
import net.osmand.plus.views.PointNavigationLayer;
import net.osmand.plus.views.RouteLayer;
import net.osmand.plus.views.MapTextLayer;
import net.osmand.plus.views.TransportInfoLayer;
import net.osmand.plus.views.TransportStopsLayer;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

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
	private GpxSelectionHelper gpxSelectionHelper;

	public MapActivityLayers(MapActivity activity) {
		this.activity = activity;
		gpxSelectionHelper = getApplication().getSelectedGpxHelper();
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
		mapVectorLayer = new MapVectorLayer(mapTileLayer);
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
		locationLayer = new PointLocationLayer(MapActivity.getMapViewTrackingUtilities());
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
		if(mapView.getLayers().contains(transportStopsLayer) != settings.SHOW_TRANSPORT_OVER_MAP.get()){
			if(settings.SHOW_TRANSPORT_OVER_MAP.get()){
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

	private final class LayerMenuListener  {
		private final ContextMenuAdapter adapter;
		private final OsmandMapTileView mapView;
		private final OsmandSettings settings;
		DialogInterface dialog;
		
		private LayerMenuListener(ContextMenuAdapter adapter,
				OsmandMapTileView mapView, OsmandSettings settings) {
			this.adapter = adapter;
			this.mapView = mapView;
			this.settings = settings;
		}

		public void setDialog(DialogInterface dialog) {
			this.dialog = dialog;
		}
		
		public void onClick(int item, boolean isChecked) {
			int itemId = adapter.getItemId(item);
			OnContextMenuClick clck = adapter.getClickAdapter(item);
			if(clck != null) {
				clck.onContextMenuClick(itemId, item, isChecked, dialog);
			} else if(itemId == R.string.layer_poi){
				if(isChecked){
					selectPOIFilterLayer(mapView, null);
				}
				settings.SHOW_POI_OVER_MAP.set(isChecked);
			} else if(itemId == R.string.layer_amenity_label){
				settings.SHOW_POI_LABEL.set(isChecked);
			} else if(itemId == R.string.layer_favorites){
				settings.SHOW_FAVORITES.set(isChecked);
			} else if(itemId == R.string.layer_gpx_layer){
				if(getApplication().getSelectedGpxHelper().isShowingAnyGpxFiles()){
					getApplication().getSelectedGpxHelper().clearAllGpxFileToShow();
				} else {
					dialog.dismiss();
					showGPXFileLayer(getAlreadySelectedGpx(), mapView);
				}
			} else if(itemId == R.string.layer_transport_route){
				transportInfoLayer.setVisible(isChecked);
			} else if(itemId == R.string.layer_transport){
				settings.SHOW_TRANSPORT_OVER_MAP.set(isChecked);
			}
			updateLayers(mapView);
			mapView.refreshMap();
		}
	}
	
	public void openLayerSelectionDialog(final OsmandMapTileView mapView){
		final OsmandSettings settings = getApplication().getSettings();
		final ContextMenuAdapter adapter = new ContextMenuAdapter(activity);
		// String appMode = " [" + settings.getApplicationMode().toHumanString(view.getApplication()) +"] ";
		adapter.item(R.string.layer_poi).selected(settings.SHOW_POI_OVER_MAP.get() ? 1 : 0)
				.icons(R.drawable.ic_action_info_dark, R.drawable.ic_action_info_light).reg();
		adapter.item(R.string.layer_amenity_label).selected(settings.SHOW_POI_LABEL.get() ? 1 : 0) 
				.icons(R.drawable.ic_action_text_dark, R.drawable.ic_action_text_light).reg();
		adapter.item(R.string.layer_favorites).selected(settings.SHOW_FAVORITES.get() ? 1 : 0) 
				.icons(R.drawable.ic_action_fav_dark, R.drawable.ic_action_fav_light).reg();
		adapter.item(R.string.layer_gpx_layer).selected(
				getApplication().getSelectedGpxHelper().isShowingAnyGpxFiles()? 1 : 0)
//				.icons(R.drawable.ic_action_foot_dark, R.drawable.ic_action_foot_light)
				.icons(R.drawable.ic_action_polygom_dark, R.drawable.ic_action_polygom_light)
				.reg();
		adapter.item(R.string.layer_transport).selected( settings.SHOW_TRANSPORT_OVER_MAP.get() ? 1 : 0)
				.icons(R.drawable.ic_action_bus_dark, R.drawable.ic_action_bus_light).reg(); 
		if(TransportRouteHelper.getInstance().routeIsCalculated()){
			adapter.item(R.string.layer_transport_route).selected(1 )
				.icons(R.drawable.ic_action_bus_dark, R.drawable.ic_action_bus_light).reg();
		}
		
		
		OsmandPlugin.registerLayerContextMenu(mapView, adapter, activity);
		getApplication().getAppCustomization().prepareLayerContextMenu(activity, adapter);
		
		
		final LayerMenuListener listener = new LayerMenuListener(adapter, mapView, settings);
		Builder b = new AlertDialog.Builder(activity);
		
		final int padding = (int) (12 * activity.getResources().getDisplayMetrics().density + 0.5f);
		final boolean light = getApplication().getSettings().isLightContentMenu();
		final int layout;
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			layout = R.layout.list_menu_item;
		} else {
			layout = R.layout.list_menu_item_native;
		}

		final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(
			    activity, layout, R.id.title, adapter.getItemNames()){
			        @Override
					public View getView(final int position, View convertView, ViewGroup parent) {
						// User super class to create the View
						View v = activity.getLayoutInflater().inflate(layout, null);
 			            TextView tv = (TextView)v.findViewById(R.id.title);
			            tv.setText(adapter.getItemName(position));

						//if it's gpx or poi layer - need to show settings icon
						//need imageview and specialItemId o
						final ImageView settingsImage = (ImageView) v.findViewById(R.id.icon_settings);
						final int specialItemId = adapter.getItemId(position);
						if ((specialItemId == R.string.layer_poi || specialItemId == R.string.layer_gpx_layer)
								&& adapter.getSelection(position) > 0) {

							settingsImage.setVisibility(View.VISIBLE);
							//setting icon depending on theme
							if(light){
								settingsImage.setImageResource(R.drawable.ic_action_settings_light);
							} else {
								settingsImage.setImageResource(R.drawable.ic_action_settings_dark);
							}

							if (specialItemId == R.string.layer_poi){
								settingsImage.setOnClickListener(new View.OnClickListener() {
									@Override
									public void onClick(View view) {
										selectPOIFilterLayer(mapView, null);
									}
								});
							} else if (specialItemId == R.string.layer_gpx_layer) {
								settingsImage.setOnClickListener(new View.OnClickListener() {
									@Override
									public void onClick(View view) {
										if (listener.dialog != null) {
											listener.dialog.dismiss();
										}
										showGPXFileLayer(getAlreadySelectedGpx(), mapView);
									}
								});
							}
						}

			            //Put the image on the TextView
			            if(adapter.getImageId(position, light) != 0) {
			            	tv.setCompoundDrawablesWithIntrinsicBounds(adapter.getImageId(position, light), 0, 0, 0);
			            } else {
			            	tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_transparent, 0, 0, 0);
			            }
			            tv.setCompoundDrawablePadding(padding);

						final CheckBox ch = ((CheckBox) v.findViewById(R.id.check_item));
						if(adapter.getSelection(position) == -1){
							ch.setVisibility(View.INVISIBLE);
						} else {
							ch.setOnCheckedChangeListener(null);
							ch.setChecked(adapter.getSelection(position) > 0);
							ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
								@Override
								public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
									if (specialItemId == R.string.layer_poi){
										if (isChecked){
											settingsImage.setVisibility(View.VISIBLE);
										} else {
											settingsImage.setVisibility(View.GONE);
										}
									}
									listener.onClick(position, isChecked);
								}
							});
						}
			            return v;
			        }
			    };

	    OnClickListener onClickListener = new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int position) {
			}
		};
		b.setAdapter(listAdapter, onClickListener);
		b.setPositiveButton(R.string.default_buttons_ok, null);

	    final AlertDialog dlg = b.create();
	    listener.setDialog(dlg); 
		dlg.setCanceledOnTouchOutside(true);
		dlg.getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(adapter.getSelection(position) >= 0) {
					listener.onClick(position, !(adapter.getSelection(position) > 0));
					adapter.setSelection(position, adapter.getSelection(position) > 0 ? 0 : 1);
					listAdapter.notifyDataSetInvalidated();
				} else {
					listener.onClick(position, adapter.getSelection(position) > 0);
				}				
			}
		});
		dlg.show();
	}

	public void showGPXFileLayer(List<String> files, final OsmandMapTileView mapView) {
		final OsmandSettings settings = getApplication().getSettings();
		CallbackWithObject<GPXFile[]> callbackWithObject = new CallbackWithObject<GPXFile[]>() {
			@Override
			public boolean processResult(GPXFile[] result) {
				WptPt locToShow = null;
				for (GPXFile g : result) {
					if (g.showCurrentTrack) {
						if (!settings.SAVE_TRACK_TO_GPX.get() && !
								settings.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
							AccessibleToast.makeText(activity, R.string.gpx_monitoring_disabled_warn, Toast.LENGTH_SHORT).show();
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
				return true;
			}
		};

		if (files == null) {
			GpxUiHelper.selectGPXFile(activity, true, true, callbackWithObject);
		} else {
			GpxUiHelper.selectGPXFile(files, activity, true, true, callbackWithObject);
		}
	}
	
	
	private List<String> getAlreadySelectedGpx(){
		if (gpxSelectionHelper == null){
			return null;
		}
		List<GpxSelectionHelper.SelectedGpxFile> selectedGpxFiles = gpxSelectionHelper.getSelectedGPXFiles();
		List<String> files = new ArrayList<String>();
		for (GpxSelectionHelper.SelectedGpxFile file : selectedGpxFiles) {
			files.add(file.getGpxFile().path);
		}
		return files;
	}
	
	
	public AlertDialog selectPOIFilterLayer(final OsmandMapTileView mapView, final PoiFilter[] selected){
		final List<PoiFilter> userDefined = new ArrayList<PoiFilter>();
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
		
		for (PoiFilter f : poiFilters.getUserDefinedPoiFilters()) {
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
		ListAdapter listAdapter ;
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			listAdapter =
				adapter.createListAdapter(activity, R.layout.list_menu_item, app.getSettings().isLightContentMenu());
		} else {
			listAdapter =
				adapter.createListAdapter(activity, R.layout.list_menu_item_native, app.getSettings().isLightContentMenu());
		}
		builder.setAdapter(listAdapter, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == 1){
					String filterId = PoiFilter.CUSTOM_FILTER_ID; 
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
					PoiFilter f = poiFilters.getFilterById(filterId);
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
	
}
