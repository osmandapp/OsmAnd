package net.osmand.plus.activities;


import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import net.osmand.CallbackWithObject;
import net.osmand.ResultMatcher;
import net.osmand.StateChangedListener;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.activities.MapActivity.ShowQuickSearchMode;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickActionRegistry;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.DownloadedRegionsLayer;
import net.osmand.plus.views.FavouritesLayer;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.ImpassableRoadsLayer;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.MapMarkersLayer;
import net.osmand.plus.views.MapQuickActionLayer;
import net.osmand.plus.views.MapTextLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.plus.views.PointLocationLayer;
import net.osmand.plus.views.PointNavigationLayer;
import net.osmand.plus.views.RouteLayer;
import net.osmand.plus.views.RulerControlLayer;
import net.osmand.plus.views.TransportStopsLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

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
	private FavouritesLayer mFavouritesLayer;
	private TransportStopsLayer transportStopsLayer;
	private PointLocationLayer locationLayer;
	private RulerControlLayer rulerControlLayer;
	private PointNavigationLayer navigationLayer;
	private MapMarkersLayer mapMarkersLayer;
	private ImpassableRoadsLayer impassableRoadsLayer;
	private MapInfoLayer mapInfoLayer;
	private MapTextLayer mapTextLayer;
	private ContextMenuLayer contextMenuLayer;
	private MapControlsLayer mapControlsLayer;
	private MapQuickActionLayer mapQuickActionLayer;
	private DownloadedRegionsLayer downloadedRegionsLayer;
	private MapWidgetRegistry mapWidgetRegistry;
	private QuickActionRegistry quickActionRegistry;
	private MeasurementToolLayer measurementToolLayer;
	
	private StateChangedListener<Integer> transparencyListener;

	public MapActivityLayers(MapActivity activity) {
		this.activity = activity;
		this.mapWidgetRegistry = new MapWidgetRegistry(activity.getMyApplication());
		this.quickActionRegistry = new QuickActionRegistry(activity.getMyApplication().getSettings());
	}

	public QuickActionRegistry getQuickActionRegistry() {
		return quickActionRegistry;
	}

	public MapWidgetRegistry getMapWidgetRegistry() {
		return mapWidgetRegistry;
	}

	public OsmandApplication getApplication() {
		return (OsmandApplication) activity.getApplication();
	}


	public void createLayers(final OsmandMapTileView mapView) {

		OsmandApplication app = getApplication();
		RoutingHelper routingHelper = app.getRoutingHelper();
		// first create to make accessible
		mapTextLayer = new MapTextLayer();
		// 5.95 all labels
		mapView.addLayer(mapTextLayer, 5.95f);
		// 8. context menu layer 
		contextMenuLayer = new ContextMenuLayer(activity);
		mapView.addLayer(contextMenuLayer, 8);
		// mapView.addLayer(underlayLayer, -0.5f);
		mapTileLayer = new MapTileLayer(true);
		mapView.addLayer(mapTileLayer, 0.0f);
		mapView.setMainLayer(mapTileLayer);

		// 0.5 layer
		mapVectorLayer = new MapVectorLayer(mapTileLayer, false);
		mapView.addLayer(mapVectorLayer, 0.5f);

		downloadedRegionsLayer = new DownloadedRegionsLayer(activity);
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
		mapView.addLayer(poiMapLayer, 3);
		// 4. favorites layer
		mFavouritesLayer = new FavouritesLayer();
		mapView.addLayer(mFavouritesLayer, 4);
		// 4.6 measurement tool layer
		measurementToolLayer = new MeasurementToolLayer();
		mapView.addLayer(measurementToolLayer, 4.6f);
		// 5. transport layer
		transportStopsLayer = new TransportStopsLayer(activity);
		mapView.addLayer(transportStopsLayer, 5);
		// 5.95 all text labels
		// 6. point location layer 
		locationLayer = new PointLocationLayer(activity.getMapViewTrackingUtilities());
		mapView.addLayer(locationLayer, 6);
		// 7. point navigation layer
		navigationLayer = new PointNavigationLayer(activity);
		mapView.addLayer(navigationLayer, 7);
		// 7.3 map markers layer
		mapMarkersLayer = new MapMarkersLayer(activity);
		mapView.addLayer(mapMarkersLayer, 7.3f);
		// 7.5 Impassible roads
		impassableRoadsLayer = new ImpassableRoadsLayer(activity);
		mapView.addLayer(impassableRoadsLayer, 7.5f);
		// 7.8 ruler control layer
		rulerControlLayer = new RulerControlLayer(activity);
		mapView.addLayer(rulerControlLayer, 7.8f);
		// 8. context menu layer 
		// 9. map info layer
		mapInfoLayer = new MapInfoLayer(activity, routeLayer);
		mapView.addLayer(mapInfoLayer, 9);
		// 11. route info layer
		mapControlsLayer = new MapControlsLayer(activity);
		mapView.addLayer(mapControlsLayer, 11);
		// 12. quick actions layer
		mapQuickActionLayer = new MapQuickActionLayer(activity, contextMenuLayer);
		mapView.addLayer(mapQuickActionLayer, 12);
		contextMenuLayer.setMapQuickActionLayer(mapQuickActionLayer);
		mapControlsLayer.setMapQuickActionLayer(mapQuickActionLayer);

		transparencyListener = new StateChangedListener<Integer>() {
			@Override
			public void stateChanged(Integer change) {
				mapTileLayer.setAlpha(change);
				mapVectorLayer.setAlpha(change);
				mapView.refreshMap();
			}
		};
		app.getSettings().MAP_TRANSPARENCY.addListener(transparencyListener);

		OsmandPlugin.createLayers(mapView, activity);
		app.getAppCustomization().createLayers(mapView, activity);
		app.getAidlApi().registerMapLayers(activity);
	}


	public void updateLayers(OsmandMapTileView mapView) {
		OsmandSettings settings = getApplication().getSettings();
		updateMapSource(mapView, settings.MAP_TILE_SOURCES);
		boolean showStops = settings.getCustomRenderBooleanProperty(OsmandSettings.TRANSPORT_STOPS_OVER_MAP).get();
		transportStopsLayer.setShowTransportStops(showStops);
		OsmandPlugin.refreshLayers(mapView, activity);
	}

	public void updateMapSource(OsmandMapTileView mapView, CommonPreference<String> settingsToWarnAboutMap) {
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
		if (vectorData) {
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
							Toast.makeText(activity,
									R.string.gpx_monitoring_disabled_warn, Toast.LENGTH_LONG).show();
						}
						break;
					} else {
						locToShow = g.findPointToShow();
					}
				}
				getApplication().getSelectedGpxHelper().setGpxFileToDisplay(result);
				if (locToShow != null) {
					mapView.getAnimatedDraggingThread().startMoving(locToShow.lat, locToShow.lon,
							mapView.getZoom(), true);
				}
				mapView.refreshMap();
				activity.getDashboard().refreshContent(true);
				return true;
			}
		};
		return GpxUiHelper.selectGPXFiles(files, activity, callbackWithObject, getThemeRes(getApplication()));
	}


	public void showMultichoicePoiFilterDialog(final OsmandMapTileView mapView, final DismissListener listener) {
		final OsmandApplication app = getApplication();
		final PoiFiltersHelper poiFilters = app.getPoiFilters();
		final ContextMenuAdapter adapter = new ContextMenuAdapter();
		final List<PoiUIFilter> list = new ArrayList<>();
		for (PoiUIFilter f : poiFilters.getTopDefinedPoiFilters()) {
			addFilterToList(adapter, list, f, true);
		}
		for (PoiUIFilter f : poiFilters.getSearchPoiFilters()) {
			addFilterToList(adapter, list, f, true);
		}
		list.add(poiFilters.getCustomPOIFilter());

		final ArrayAdapter<ContextMenuItem> listAdapter = adapter.createListAdapter(activity, !isNightMode(app));
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, getThemeRes(app)));
		final ListView listView = new ListView(activity);
		listView.setDivider(null);
		listView.setClickable(true);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				ContextMenuItem item = listAdapter.getItem(position);
				item.setSelected(!item.getSelected());
				item.getItemClickListener().onContextMenuClick(listAdapter, position, position, item.getSelected(), null);
				listAdapter.notifyDataSetChanged();
			}
		});
		builder.setView(listView)
				.setTitle(R.string.show_poi_over_map)
				.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						for (int i = 0; i < listAdapter.getCount(); i++) {
							ContextMenuItem item = listAdapter.getItem(i);
								PoiUIFilter filter = list.get(i);
							if (item.getSelected()) {
								if (filter.isStandardFilter()) {
									filter.removeUnsavedFilterByName();
								}
								getApplication().getPoiFilters().addSelectedPoiFilter(filter);
							} else {
								getApplication().getPoiFilters().removeSelectedPoiFilter(filter);
							}
						}
						mapView.refreshMap();
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				// TODO go to single choice dialog
				.setNeutralButton(" ", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						showSingleChoicePoiFilterDialog(mapView, listener);
					}
				});
		final AlertDialog alertDialog = builder.create();
		alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
				Drawable drawable = app.getUIUtilities().getThemedIcon(R.drawable.ic_action_singleselect);
				neutralButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
				neutralButton.setContentDescription(app.getString(R.string.shared_string_filters));
			}
		});
		alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				listener.dismiss();
			}
		});
		alertDialog.show();
	}

	public void showSingleChoicePoiFilterDialog(final OsmandMapTileView mapView, final DismissListener listener) {
		final OsmandApplication app = getApplication();
		final PoiFiltersHelper poiFilters = app.getPoiFilters();
		final ContextMenuAdapter adapter = new ContextMenuAdapter();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.shared_string_search, app)
				.setIcon(R.drawable.ic_action_search_dark).createItem());
		final List<PoiUIFilter> list = new ArrayList<>();
		list.add(poiFilters.getCustomPOIFilter());
		for (PoiUIFilter f : poiFilters.getTopDefinedPoiFilters()) {
			addFilterToList(adapter, list, f, false);
		}
		for (PoiUIFilter f : poiFilters.getSearchPoiFilters()) {
			addFilterToList(adapter, list, f, false);
		}

		final ArrayAdapter<ContextMenuItem> listAdapter = adapter.createListAdapter(activity, !isNightMode(app));
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, getThemeRes(app)));
		builder.setAdapter(listAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				PoiUIFilter pf = list.get(which);
				String filterId = pf.getFilterId();
				if (filterId.equals(PoiUIFilter.CUSTOM_FILTER_ID)) {
					if (activity.getDashboard().isVisible()) {
						activity.getDashboard().hideDashboard();
					}
					activity.showQuickSearch(ShowQuickSearchMode.NEW, true);
				} else {
					if (pf.isStandardFilter()) {
						pf.removeUnsavedFilterByName();
					}
					getApplication().getPoiFilters().clearSelectedPoiFilters();
					getApplication().getPoiFilters().addSelectedPoiFilter(pf);
					mapView.refreshMap();
				}
			}

		});
		builder.setTitle(R.string.show_poi_over_map);
		builder.setNegativeButton(R.string.shared_string_dismiss, null);
		builder.setNeutralButton(" ", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showMultichoicePoiFilterDialog(mapView, listener);
			}
		});
		final AlertDialog alertDialog = builder.create();
		alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
				Drawable drawable = app.getUIUtilities().getThemedIcon(R.drawable.ic_action_multiselect);
				neutralButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
				neutralButton.setContentDescription(app.getString(R.string.apply_filters));
			}
		});
		alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				listener.dismiss();
			}
		});
		alertDialog.show();
	}

	private void addFilterToList(final ContextMenuAdapter adapter,
								 final List<PoiUIFilter> list,
								 final PoiUIFilter f,
								 boolean multichoice) {
		list.add(f);
		ContextMenuItem.ItemBuilder builder = new ContextMenuItem.ItemBuilder();
		if (multichoice) {
			builder.setSelected(getApplication().getPoiFilters().isPoiFilterSelected(f));
			builder.setListener(new ContextMenuAdapter.ItemClickListener() {
				@Override
				public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter,
												  int itemId, int position, boolean isChecked, int[] viewCoordinates) {
					ContextMenuItem item = adapter.getItem(position);
					item.setSelected(isChecked);
					return false;
				}
			});
		}
		builder.setTitle(f.getName());
		if (RenderingIcons.containsBigIcon(f.getIconId())) {
			builder.setIcon(RenderingIcons.getBigIconResourceId(f.getIconId()));
		} else {
			builder.setIcon(R.drawable.mx_user_defined);
		}
		builder.setColor(ContextMenuItem.INVALID_ID);
		builder.setSkipPaintingWithoutColor(true);
		adapter.addItem(builder.createItem());
	}

	public void selectMapLayer(final OsmandMapTileView mapView, @Nullable final ContextMenuItem it, @Nullable final ArrayAdapter<ContextMenuItem> adapter) {
		if (OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) == null) {
			Toast.makeText(activity, R.string.map_online_plugin_is_not_installed, Toast.LENGTH_LONG).show();
			return;
		}
		final OsmandSettings settings = getApplication().getSettings();

		final LinkedHashMap<String, String> entriesMap = new LinkedHashMap<>();


		final String layerOsmVector = "LAYER_OSM_VECTOR";
		final String layerInstallMore = "LAYER_INSTALL_MORE";
		final String layerEditInstall = "LAYER_EDIT";

		entriesMap.put(layerOsmVector, getString(R.string.vector_data));
		entriesMap.putAll(settings.getTileSourceEntries());
		entriesMap.put(layerInstallMore, getString(R.string.install_more));
		entriesMap.put(layerEditInstall, getString(R.string.maps_define_edit));

		final List<Entry<String, String>> entriesMapList = new ArrayList<>(entriesMap.entrySet());

		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, getThemeRes(getApplication())));

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

		builder.setSingleChoiceItems(items, selectedItem, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String layerKey = entriesMapList.get(which).getKey();
				switch (layerKey) {
					case layerOsmVector:
						settings.MAP_ONLINE_DATA.set(false);
						updateMapSource(mapView, null);
						updateItem(it, adapter, null);
						break;
					case layerEditInstall:
						OsmandRasterMapsPlugin.defineNewEditLayer(activity, new ResultMatcher<TileSourceTemplate>() {

							@Override
							public boolean publish(TileSourceTemplate object) {
								settings.MAP_TILE_SOURCES.set(object.getName());
								settings.MAP_ONLINE_DATA.set(true);
								if(it != null) {
									it.setDescription(object.getName());
								}
								updateMapSource(mapView, settings.MAP_TILE_SOURCES);
								return true;
							}

							@Override
							public boolean isCancelled() {
								return false;
							}

						});
						break;
					case layerInstallMore:
						OsmandRasterMapsPlugin.installMapLayers(activity, new ResultMatcher<TileSourceTemplate>() {
							TileSourceTemplate template = null;
							int count = 0;

							@Override
							public boolean publish(TileSourceTemplate object) {
								if (object == null) {
									if (count == 1) {
										settings.MAP_TILE_SOURCES.set(template.getName());
										settings.MAP_ONLINE_DATA.set(true);
										updateItem(it, adapter, template.getName());
										updateMapSource(mapView, settings.MAP_TILE_SOURCES);
									} else {
										selectMapLayer(mapView, it, adapter);
									}
								} else {
									count++;
									template = object;
								}
								return false;
							}

							@Override
							public boolean isCancelled() {
								return false;
							}
						});
						break;
					default:
						settings.MAP_TILE_SOURCES.set(layerKey);
						settings.MAP_ONLINE_DATA.set(true);
						updateItem(it, adapter, layerKey);
						updateMapSource(mapView, settings.MAP_TILE_SOURCES);
						break;
				}

				dialog.dismiss();
			}

		});
		builder.setNegativeButton(R.string.shared_string_dismiss, null);
		builder.show();
	}

	private void updateItem(@Nullable ContextMenuItem item,
							@Nullable ArrayAdapter<ContextMenuItem> adapter,
							@Nullable String description) {
		if (item != null) {
			item.setDescription(description);
		}
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	private boolean isNightMode(OsmandApplication app) {
		if (app == null) {
			return false;
		}
		return app.getDaynightHelper().isNightModeForMapControls();
	}
	
	private int getThemeRes(OsmandApplication app) {
		return isNightMode(app) ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
	}

	private String getString(int resId) {
		return activity.getString(resId);
	}

	public RouteLayer getRouteLayer() {
		return routeLayer;
	}

	public PointNavigationLayer getNavigationLayer() {
		return navigationLayer;
	}

	public ImpassableRoadsLayer getImpassableRoadsLayer() {
		return impassableRoadsLayer;
	}

	public GPXLayer getGpxLayer() {
		return gpxLayer;
	}

	public ContextMenuLayer getContextMenuLayer() {
		return contextMenuLayer;
	}

	public FavouritesLayer getFavouritesLayer() {
		return mFavouritesLayer;
	}

	public MeasurementToolLayer getMeasurementToolLayer() {
		return measurementToolLayer;
	}

	public MapTextLayer getMapTextLayer() {
		return mapTextLayer;
	}

	public PointLocationLayer getLocationLayer() {
		return locationLayer;
	}

	public RulerControlLayer getRulerControlLayer() {
		return rulerControlLayer;
	}

	public MapInfoLayer getMapInfoLayer() {
		return mapInfoLayer;
	}

	public MapControlsLayer getMapControlsLayer() {
		return mapControlsLayer;
	}

	public MapQuickActionLayer getMapQuickActionLayer() {
		return mapQuickActionLayer;
	}

	public MapMarkersLayer getMapMarkersLayer() {
		return mapMarkersLayer;
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
	
	public TransportStopsLayer getTransportStopsLayer() {
		return transportStopsLayer;
	}


	public DownloadedRegionsLayer getDownloadedRegionsLayer() {
		return downloadedRegionsLayer;
	}

	public interface DismissListener {
		void dismiss();
	}
}
