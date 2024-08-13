package net.osmand.plus.views;


import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.ResultMatcher;
import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererView;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.*;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuListAdapter;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Object is responsible to maintain layers using by map activity
 */
public class MapLayers {

	private final static String LAYER_OSM_VECTOR = "LAYER_OSM_VECTOR";
	private final static String LAYER_INSTALL_MORE = "LAYER_INSTALL_MORE";
	private final static String LAYER_ADD = "LAYER_ADD";

	private final OsmandApplication app;
	private final MapWidgetRegistry mapWidgetRegistry;

	// the order of layer should be preserved ! when you are inserting new layer
	private MapTileLayer mapTileLayer;
	private MapVectorLayer mapVectorLayer;
	private GPXLayer gpxLayer;
	private TravelSelectionLayer travelSelectionLayer;
	private NetworkRouteSelectionLayer routeSelectionLayer;
	private RouteLayer routeLayer;
	private PreviewRouteLineLayer previewRouteLineLayer;
	private POIMapLayer poiMapLayer;
	private FavouritesLayer mFavouritesLayer;
	private TransportStopsLayer transportStopsLayer;
	private PointLocationLayer locationLayer;
	private RadiusRulerControlLayer radiusRulerControlLayer;
	private DistanceRulerControlLayer distanceRulerControlLayer;
	private PointNavigationLayer navigationLayer;
	private MapMarkersLayer mapMarkersLayer;
	private ImpassableRoadsLayer impassableRoadsLayer;
	private MapInfoLayer mapInfoLayer;
	private MapTextLayer mapTextLayer;
	private ContextMenuLayer contextMenuLayer;
	private MapControlsLayer mapControlsLayer;
	private MapQuickActionLayer mapQuickActionLayer;
	private DownloadedRegionsLayer downloadedRegionsLayer;
	private MeasurementToolLayer measurementToolLayer;

	private StateChangedListener<Integer> transparencyListener;
	private StateChangedListener<Integer> overlayTransparencyListener;
	private StateChangedListener<Boolean> enable3DMapsListener;

	public MapLayers(@NonNull OsmandApplication app) {
		this.app = app;
		this.mapWidgetRegistry = new MapWidgetRegistry(app);
	}

	@NonNull
	public MapWidgetRegistry getMapWidgetRegistry() {
		return mapWidgetRegistry;
	}

	public void createLayers(@NonNull OsmandMapTileView mapView) {
		// first create to make accessible
		mapTextLayer = new MapTextLayer(app);
		// 5.95 all labels
		mapView.addLayer(mapTextLayer, 5.95f);
		// 8. context menu layer 
		contextMenuLayer = new ContextMenuLayer(app);
		mapView.addLayer(contextMenuLayer, 8);
		// mapView.addLayer(underlayLayer, -0.5f);
		mapTileLayer = new MapTileLayer(app, true);
		mapView.addLayer(mapTileLayer, 0.05f);
		mapView.setMainLayer(mapTileLayer);

		// 1-st in the order
		downloadedRegionsLayer = new DownloadedRegionsLayer(app);
		mapView.addLayer(downloadedRegionsLayer, 0.5f, -11.0f);

		// icons are 2-d in the order (icons +1 000 000 or -10.f by zOrder in core)
		// text and shields are 5-th in the order
		mapVectorLayer = new MapVectorLayer(app);
		mapView.addLayer(mapVectorLayer, 0.0f);

		// gpx layer lines 3-d in the order
		// gpx layer points 6-th in the order
		gpxLayer = new GPXLayer(app);
		gpxLayer.setPointsOrder(0.9f);
		mapView.addLayer(gpxLayer, 0.9f, -5.0f);

		travelSelectionLayer = new TravelSelectionLayer(app);
		mapView.addLayer(travelSelectionLayer, 0.95f);

		routeSelectionLayer = new NetworkRouteSelectionLayer(app);
		mapView.addLayer(routeSelectionLayer, 0.99f);

		// route layer, 6-th in the order
		routeLayer = new RouteLayer(app);
		mapView.addLayer(routeLayer, 1.0f, -2.0f);

		// 1.5 preview route line layer
		previewRouteLineLayer = new PreviewRouteLineLayer(app);
		mapView.addLayer(previewRouteLineLayer, 1.5f);

		// 2. osm bugs layer
		// 3. poi layer
		poiMapLayer = new POIMapLayer(app);
		mapView.addLayer(poiMapLayer, 3);
		// 4. favorites layer
		mFavouritesLayer = new FavouritesLayer(app);
		mapView.addLayer(mFavouritesLayer, 4);
		// 4.6 measurement tool layer
		measurementToolLayer = new MeasurementToolLayer(app);
		mapView.addLayer(measurementToolLayer, 4.6f);
		// 5. transport layer
		transportStopsLayer = new TransportStopsLayer(app);
		mapView.addLayer(transportStopsLayer, 5);
		// 5.95 all text labels
		// 6. point location layer 
		locationLayer = new PointLocationLayer(app);
		mapView.addLayer(locationLayer, 6);
		// 7. point navigation layer
		navigationLayer = new PointNavigationLayer(app);
		mapView.addLayer(navigationLayer, 7);
		// 7.3 map markers layer
		mapMarkersLayer = new MapMarkersLayer(app);
		mapView.addLayer(mapMarkersLayer, 7.3f);
		// 7.5 Impassable roads
		impassableRoadsLayer = new ImpassableRoadsLayer(app);
		mapView.addLayer(impassableRoadsLayer, 7.5f);
		// 7.8 radius ruler control layer
		radiusRulerControlLayer = new RadiusRulerControlLayer(app);
		mapView.addLayer(radiusRulerControlLayer, 7.8f);
		// 7.9 ruler by tap control layer
		distanceRulerControlLayer = new DistanceRulerControlLayer(app);
		mapView.addLayer(distanceRulerControlLayer, 7.9f);
		// 8. context menu layer 
		// 9. map info layer
		mapInfoLayer = new MapInfoLayer(app, routeLayer);
		mapView.addLayer(mapInfoLayer, 9);
		// 11. route info layer
		mapControlsLayer = new MapControlsLayer(app);
		mapView.addLayer(mapControlsLayer, 11);
		// 12. quick actions layer
		mapQuickActionLayer = new MapQuickActionLayer(app);
		mapView.addLayer(mapQuickActionLayer, 12);
		contextMenuLayer.setMapQuickActionLayer(mapQuickActionLayer);

		transparencyListener = change -> app.runInUIThread(() -> {
			mapTileLayer.setAlpha(change);
			mapVectorLayer.setAlpha(change);
			mapView.refreshMap();
		});
		app.getSettings().MAP_TRANSPARENCY.addListener(transparencyListener);

		overlayTransparencyListener = change -> app.runInUIThread(() -> {
			MapRendererView mapRenderer = mapView.getMapRenderer();
			if (mapRenderer != null) {
				mapTileLayer.setAlpha(255 - change);
				mapVectorLayer.setAlpha(255 - change);
				mapRenderer.requestRender();
			}
		});
		app.getSettings().MAP_OVERLAY_TRANSPARENCY.addListener(overlayTransparencyListener);

		enable3DMapsListener = change -> app.runInUIThread(() -> {
			MapRendererView mapRenderer = mapView.getMapRenderer();
			if (mapRenderer != null) {
				gpxLayer.setInvalidated(true);
				mapRenderer.requestRender();
			}
		});
		app.getSettings().ENABLE_3D_MAPS.addListener(enable3DMapsListener);

		createAdditionalLayers(null);
	}

	public void createAdditionalLayers(@Nullable MapActivity mapActivity) {
		PluginsHelper.createLayers(app, mapActivity);
		app.getAppCustomization().createLayers(app, mapActivity);
		app.getAidlApi().registerMapLayers(app);
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		for (OsmandMapLayer layer : mapView.getLayers()) {
			MapActivity layerMapActivity = layer.getMapActivity();
			if (mapActivity != null && layerMapActivity != null) {
				layer.setMapActivity(null);
			}
			layer.setMapActivity(mapActivity);
		}
	}

	public boolean hasMapActivity() {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		for (OsmandMapLayer layer : mapView.getLayers()) {
			if (layer.getMapActivity() != null) {
				return true;
			}
		}
		return false;
	}

	public void updateLayers(@Nullable MapActivity mapActivity) {
		OsmandSettings settings = app.getSettings();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		updateMapSource(mapView, settings.MAP_TILE_SOURCES);
		PluginsHelper.refreshLayers(app, mapActivity);
	}

	public void updateMapSource(@NonNull OsmandMapTileView mapView, CommonPreference<String> settingsToWarnAboutMap) {
		OsmandSettings settings = app.getSettings();
		boolean useOpenGLRender = app.useOpenGlRenderer();

		// update transparency
		int mapTransparency = 255;
		if (settings.MAP_UNDERLAY.get() != null) {
			mapTransparency = settings.MAP_TRANSPARENCY.get();
		} else if (useOpenGLRender && settings.MAP_OVERLAY.get() != null) {
			mapTransparency = 255 - settings.MAP_OVERLAY_TRANSPARENCY.get();
		}
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

	public void showMultiChoicePoiFilterDialog(MapActivity mapActivity, DismissListener listener) {
		PoiFiltersHelper poiFilters = app.getPoiFilters();
		ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		List<PoiUIFilter> list = new ArrayList<>();
		for (PoiUIFilter f : poiFilters.getSortedPoiFilters(true)) {
			if (!f.isTopWikiFilter()
					&& !f.isRoutesFilter()
					&& !f.isRouteArticleFilter()
					&& !f.isRouteArticlePointFilter()
					&& !f.isCustomPoiFilter()) {
				addFilterToList(adapter, list, f, true);
			}
		}

		ApplicationMode appMode = app.getSettings().getApplicationMode();
		ViewCreator viewCreator = new ViewCreator(mapActivity, isNightMode());
		viewCreator.setCustomControlsColor(appMode.getProfileColor(isNightMode()));
		ContextMenuListAdapter listAdapter = adapter.toListAdapter(mapActivity, viewCreator);

		Context themedContext = UiUtilities.getThemedContext(mapActivity, isNightMode());
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		ListView listView = new ListView(themedContext);
		listView.setDivider(null);
		listView.setClickable(true);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener((parent, view, position, id) -> {
			ContextMenuItem item = listAdapter.getItem(position);
			if (item != null) {
				item.setSelected(!item.getSelected());
				ItemClickListener clickListener = item.getItemClickListener();
				if (clickListener != null) {
					clickListener.onContextMenuClick(listAdapter, view, item, item.getSelected());
				}
				listAdapter.notifyDataSetChanged();
			}
		});
		builder.setView(listView)
				.setTitle(R.string.show_poi_over_map)
				.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
					for (int i = 0; i < listAdapter.getCount(); i++) {
						ContextMenuItem item = listAdapter.getItem(i);
						PoiUIFilter filter = list.get(i);
						if (item != null && item.getSelected()) {
							if (filter.isStandardFilter()) {
								filter.removeUnsavedFilterByName();
							}
							poiFilters.addSelectedPoiFilter(filter);
						} else {
							poiFilters.removeSelectedPoiFilter(filter);
						}
					}
					mapActivity.getMapView().refreshMap();
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setNeutralButton(" ", (dialog, which) -> showSingleChoicePoiFilterDialog(mapActivity, listener));
		AlertDialog alertDialog = builder.create();
		alertDialog.setOnShowListener(dialog -> {
			Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
			Drawable drawable = app.getUIUtilities().getThemedIcon(R.drawable.ic_action_singleselect);
			neutralButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
			neutralButton.setContentDescription(app.getString(R.string.shared_string_filters));
		});
		alertDialog.setOnDismissListener(dialog -> listener.dismiss());
		alertDialog.show();
	}

	public void showSingleChoicePoiFilterDialog(MapActivity mapActivity, DismissListener listener) {
		PoiFiltersHelper poiFilters = app.getPoiFilters();
		ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		adapter.addItem(new ContextMenuItem(null)
				.setTitleId(R.string.shared_string_search, app)
				.setIcon(R.drawable.ic_action_search_dark));
		List<PoiUIFilter> list = new ArrayList<>();
		list.add(null);
		for (PoiUIFilter f : poiFilters.getSortedPoiFilters(true)) {
			if (!f.isTopWikiFilter()
					&& !f.isRoutesFilter()
					&& !f.isRouteArticleFilter()
					&& !f.isRouteArticlePointFilter()
					&& !f.isCustomPoiFilter()) {
				addFilterToList(adapter, list, f, false);
			}
		}

		ApplicationMode appMode = app.getSettings().getApplicationMode();
		ViewCreator viewCreator = new ViewCreator(mapActivity, isNightMode());
		viewCreator.setCustomControlsColor(appMode.getProfileColor(isNightMode()));
		ContextMenuListAdapter listAdapter = adapter.toListAdapter(mapActivity, viewCreator);

		Context themedContext = UiUtilities.getThemedContext(mapActivity, isNightMode());
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		builder.setAdapter(listAdapter, (dialog, which) -> {
			PoiUIFilter filter = list.get(which);
			if (filter == null) {
				if (mapActivity.getDashboard().isVisible()) {
					mapActivity.getDashboard().hideDashboard();
				}
				mapActivity.getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.NEW, true);
			} else {
				if (filter.isStandardFilter()) {
					filter.removeUnsavedFilterByName();
				}
				poiFilters.clearGeneralSelectedPoiFilters();
				poiFilters.addSelectedPoiFilter(filter);
				updateRoutingPoiFiltersIfNeeded();
				mapActivity.getMapView().refreshMap();
			}
		});
		builder.setTitle(R.string.show_poi_over_map);
		builder.setNegativeButton(R.string.shared_string_dismiss, null);
		builder.setNeutralButton(" ", (dialog, which) -> showMultiChoicePoiFilterDialog(mapActivity, listener));
		AlertDialog alertDialog = builder.create();
		alertDialog.setOnShowListener(dialog -> {
			Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
			Drawable drawable = app.getUIUtilities().getThemedIcon(R.drawable.ic_action_multiselect);
			neutralButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
			neutralButton.setContentDescription(app.getString(R.string.apply_filters));
		});
		alertDialog.setOnDismissListener(dialog -> listener.dismiss());
		alertDialog.show();
	}

	private void addFilterToList(ContextMenuAdapter adapter,
	                             List<PoiUIFilter> list,
	                             PoiUIFilter f,
	                             boolean multiChoice) {
		list.add(f);
		ContextMenuItem item = new ContextMenuItem(null);
		if (multiChoice) {
			item.setSelected(app.getPoiFilters().isPoiFilterSelected(f));
			item.setListener((uiAdapter, view, it, isChecked) -> {
				it.setSelected(isChecked);
				return false;
			});
		}
		item.setTitle(f.getName());
		if (RenderingIcons.containsBigIcon(f.getIconId())) {
			item.setIcon(RenderingIcons.getBigIconResourceId(f.getIconId()));
		} else {
			item.setIcon(R.drawable.mx_special_custom_category);
		}
		item.setColor(app, ContextMenuItem.INVALID_ID);
		item.setUseNaturalIconColor(true);
		adapter.addItem(item);
	}

	public void selectMapSourceLayer(
			@NonNull MapActivity mapActivity,
			@NonNull ContextMenuItem item,
			@NonNull OnDataChangeUiAdapter uiAdapter
	) {
		selectMapLayer(mapActivity, true, app.getSettings().MAP_TILE_SOURCES, mapSourceName -> {
			OsmandSettings settings = app.getSettings();
			item.setDescription(settings.getSelectedMapSourceTitle());
			uiAdapter.onDataSetChanged();
			return true;
		});
	}

	public void selectMapLayer(
			@NonNull MapActivity mapActivity, boolean includeOfflineMaps,
			@NonNull CommonPreference<String> targetLayer,
			@Nullable CallbackWithObject<String> callback
	) {
		if (!PluginsHelper.isActive(OsmandRasterMapsPlugin.class)) {
			app.showToastMessage(R.string.map_online_plugin_is_not_installed);
			return;
		}
		OsmandSettings settings = app.getSettings();

		Map<String, String> entriesMap = new LinkedHashMap<>();
		if (includeOfflineMaps) {
			entriesMap.put(LAYER_OSM_VECTOR, app.getString(R.string.vector_data));
		}
		entriesMap.putAll(settings.getTileSourceEntries());
		entriesMap.put(LAYER_INSTALL_MORE, app.getString(R.string.install_more));
		entriesMap.put(LAYER_ADD, app.getString(R.string.shared_string_add_manually));
		List<Entry<String, String>> entriesMapList = new ArrayList<>(entriesMap.entrySet());

		String selectedTileSourceKey = targetLayer.get();
		int selectedItem = -1;
		if (!settings.MAP_ONLINE_DATA.get() && targetLayer == settings.MAP_TILE_SOURCES) {
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
		String[] items = new String[entriesMapList.size()];
		int i = 0;
		for (Entry<String, String> entry : entriesMapList) {
			items[i++] = entry.getValue();
		}

		boolean nightMode = isNightMode();
		AlertDialogData dialogData = new AlertDialogData(mapActivity, nightMode)
				.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode))
				.setNegativeButton(R.string.shared_string_dismiss, null);

		CustomAlert.showSingleSelection(dialogData, items, selectedItem, v -> {
			int which = (int) v.getTag();
			String layerKey = entriesMapList.get(which).getKey();
			onMapLayerSelected(mapActivity, includeOfflineMaps, targetLayer, callback, layerKey);
		});
	}

	private void onMapLayerSelected(
			@NonNull MapActivity mapActivity, boolean includeOfflineMaps,
			@NonNull CommonPreference<String> targetLayer,
			@Nullable CallbackWithObject<String> callback,
			@NonNull String layerKey
	) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		switch (layerKey) {
			case LAYER_OSM_VECTOR:
				if (targetLayer == settings.MAP_TILE_SOURCES) {
					settings.MAP_ONLINE_DATA.set(false);
				}
				updateLayers(mapActivity);
				if (callback != null) {
					callback.processResult(null);
				}
				break;
			case LAYER_ADD:
				OsmandRasterMapsPlugin.defineNewEditLayer(mapActivity, null, null);
				break;
			case LAYER_INSTALL_MORE:
				OsmandRasterMapsPlugin.installMapLayers(mapActivity, new ResultMatcher<TileSourceTemplate>() {
					TileSourceTemplate template;
					int count;

					@Override
					public boolean publish(TileSourceTemplate object) {
						if (object == null) {
							if (count == 1) {
								targetLayer.set(template.getName());
								if (targetLayer == settings.MAP_TILE_SOURCES) {
									settings.MAP_ONLINE_DATA.set(true);
								}
								updateLayers(mapActivity);
								if (callback != null) {
									callback.processResult(template.getName());
								}
							} else {
								selectMapLayer(mapActivity, includeOfflineMaps, targetLayer, callback);
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
				targetLayer.set(layerKey);
				if (targetLayer == settings.MAP_TILE_SOURCES) {
					settings.MAP_ONLINE_DATA.set(true);
				}
				updateLayers(mapActivity);
				if (callback != null) {
					callback.processResult(layerKey.replace(IndexConstants.SQLITE_EXT, ""));
				}
				break;
		}
	}

	private void updateRoutingPoiFiltersIfNeeded() {
		OsmandSettings settings = app.getSettings();
		RoutingHelper routingHelper = app.getRoutingHelper();
		boolean usingRouting = routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()
				|| routingHelper.isRouteBeingCalculated() || routingHelper.isRouteCalculated();
		ApplicationMode routingMode = routingHelper.getAppMode();
		if (usingRouting && routingMode != settings.getApplicationMode()) {
			settings.setSelectedPoiFilters(routingMode, settings.getSelectedPoiFilters());
		}
	}

	private boolean isNightMode() {
		return app.getDaynightHelper().isNightModeForMapControls();
	}

	public RouteLayer getRouteLayer() {
		return routeLayer;
	}

	public PreviewRouteLineLayer getPreviewRouteLineLayer() {
		return previewRouteLineLayer;
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

	public NetworkRouteSelectionLayer getRouteSelectionLayer() {
		return routeSelectionLayer;
	}

	public TravelSelectionLayer getTravelSelectionLayer() {
		return travelSelectionLayer;
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

	public RadiusRulerControlLayer getRadiusRulerControlLayer() {
		return radiusRulerControlLayer;
	}

	public DistanceRulerControlLayer getDistanceRulerControlLayer() {
		return distanceRulerControlLayer;
	}

	public MapInfoLayer getMapInfoLayer() {
		return mapInfoLayer;
	}

	public MapControlsLayer getMapControlsLayer() {
		return mapControlsLayer;
	}

	public MapActionsHelper getMapActionsHelper() {
		return mapControlsLayer.getMapActionsHelper();
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
