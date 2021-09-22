package net.osmand.plus.views;


import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.ResultMatcher;
import net.osmand.StateChangedListener;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.ItemClickListener;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivity.ShowQuickSearchMode;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.layers.ContextMenuLayer;
import net.osmand.plus.views.layers.DistanceRulerControlLayer;
import net.osmand.plus.views.layers.DownloadedRegionsLayer;
import net.osmand.plus.views.layers.FavouritesLayer;
import net.osmand.plus.views.layers.GPXLayer;
import net.osmand.plus.views.layers.ImpassableRoadsLayer;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.MapMarkersLayer;
import net.osmand.plus.views.layers.MapQuickActionLayer;
import net.osmand.plus.views.layers.MapTextLayer;
import net.osmand.plus.views.layers.POIMapLayer;
import net.osmand.plus.views.layers.PointLocationLayer;
import net.osmand.plus.views.layers.PointNavigationLayer;
import net.osmand.plus.views.layers.PreviewRouteLineLayer;
import net.osmand.plus.views.layers.RadiusRulerControlLayer;
import net.osmand.plus.views.layers.RouteLayer;
import net.osmand.plus.views.layers.TransportStopsLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * Object is responsible to maintain layers using by map activity
 */
public class MapLayers {

	private final Context ctx;

	// the order of layer should be preserved ! when you are inserting new layer
	private MapTileLayer mapTileLayer;
	private MapVectorLayer mapVectorLayer;
	private GPXLayer gpxLayer;
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
	private MapWidgetRegistry mapWidgetRegistry;
	private MeasurementToolLayer measurementToolLayer;

	private StateChangedListener<Integer> transparencyListener;

	public MapLayers(@NonNull Context ctx) {
		this.ctx = ctx;
		this.mapWidgetRegistry = new MapWidgetRegistry((OsmandApplication) ctx.getApplicationContext());
	}

	public MapWidgetRegistry getMapWidgetRegistry() {
		return mapWidgetRegistry;
	}

	public OsmandApplication getApplication() {
		return (OsmandApplication) ctx.getApplicationContext();
	}

	public void createLayers(@NonNull final OsmandMapTileView mapView) {
		OsmandApplication app = getApplication();
		// first create to make accessible
		mapTextLayer = new MapTextLayer(ctx);
		// 5.95 all labels
		mapView.addLayer(mapTextLayer, 5.95f);
		// 8. context menu layer 
		contextMenuLayer = new ContextMenuLayer(ctx);
		mapView.addLayer(contextMenuLayer, 8);
		// mapView.addLayer(underlayLayer, -0.5f);
		mapTileLayer = new MapTileLayer(ctx, true);
		mapView.addLayer(mapTileLayer, 0.0f);
		mapView.setMainLayer(mapTileLayer);

		// 0.5 layer
		mapVectorLayer = new MapVectorLayer(mapTileLayer, false);
		mapView.addLayer(mapVectorLayer, 0.5f);

		downloadedRegionsLayer = new DownloadedRegionsLayer(ctx);
		mapView.addLayer(downloadedRegionsLayer, 0.5f);

		// 0.9 gpx layer
		gpxLayer = new GPXLayer(ctx);
		mapView.addLayer(gpxLayer, 0.9f);

		// 1. route layer
		routeLayer = new RouteLayer(ctx);
		mapView.addLayer(routeLayer, 1);

		// 1.5 preview route line layer
		previewRouteLineLayer = new PreviewRouteLineLayer(ctx);
		mapView.addLayer(previewRouteLineLayer, 1.5f);

		// 2. osm bugs layer
		// 3. poi layer
		poiMapLayer = new POIMapLayer(ctx);
		mapView.addLayer(poiMapLayer, 3);
		// 4. favorites layer
		mFavouritesLayer = new FavouritesLayer(ctx);
		mapView.addLayer(mFavouritesLayer, 4);
		// 4.6 measurement tool layer
		measurementToolLayer = new MeasurementToolLayer(ctx);
		mapView.addLayer(measurementToolLayer, 4.6f);
		// 5. transport layer
		transportStopsLayer = new TransportStopsLayer(ctx);
		mapView.addLayer(transportStopsLayer, 5);
		// 5.95 all text labels
		// 6. point location layer 
		locationLayer = new PointLocationLayer(ctx);
		mapView.addLayer(locationLayer, 6);
		// 7. point navigation layer
		navigationLayer = new PointNavigationLayer(ctx);
		mapView.addLayer(navigationLayer, 7);
		// 7.3 map markers layer
		mapMarkersLayer = new MapMarkersLayer(ctx);
		mapView.addLayer(mapMarkersLayer, 7.3f);
		// 7.5 Impassible roads
		impassableRoadsLayer = new ImpassableRoadsLayer(ctx);
		mapView.addLayer(impassableRoadsLayer, 7.5f);
		// 7.8 radius ruler control layer
		radiusRulerControlLayer = new RadiusRulerControlLayer(ctx);
		mapView.addLayer(radiusRulerControlLayer, 7.8f);
		// 7.9 ruler by tap control layer
		distanceRulerControlLayer = new DistanceRulerControlLayer(ctx);
		mapView.addLayer(distanceRulerControlLayer, 7.9f);
		// 8. context menu layer 
		// 9. map info layer
		mapInfoLayer = new MapInfoLayer(ctx, routeLayer);
		mapView.addLayer(mapInfoLayer, 9);
		// 11. route info layer
		mapControlsLayer = new MapControlsLayer(ctx);
		mapView.addLayer(mapControlsLayer, 11);
		// 12. quick actions layer
		mapQuickActionLayer = new MapQuickActionLayer(ctx);
		mapView.addLayer(mapQuickActionLayer, 12);
		contextMenuLayer.setMapQuickActionLayer(mapQuickActionLayer);

		transparencyListener = change -> app.runInUIThread(() -> {
			mapTileLayer.setAlpha(change);
			mapVectorLayer.setAlpha(change);
			mapView.refreshMap();
		});
		app.getSettings().MAP_TRANSPARENCY.addListener(transparencyListener);

		OsmandPlugin.createLayers(ctx, null);
		app.getAppCustomization().createLayers(ctx, null);
		app.getAidlApi().registerMapLayers(ctx);
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		OsmandMapTileView mapView = getApplication().getOsmandMap().getMapView();
		for (OsmandMapLayer layer : mapView.getLayers()) {
			MapActivity layerMapActivity = layer.getMapActivity();
			if (mapActivity != null && layerMapActivity != null) {
				layer.setMapActivity(null);
			}
			layer.setMapActivity(mapActivity);
		}
	}

	public boolean hasMapActivity() {
		OsmandMapTileView mapView = getApplication().getOsmandMap().getMapView();
		for (OsmandMapLayer layer : mapView.getLayers()) {
			if (layer.getMapActivity() != null) {
				return true;
			}
		}
		return false;
	}

	public void updateLayers(@Nullable MapActivity mapActivity) {
		OsmandSettings settings = getApplication().getSettings();
		OsmandMapTileView mapView = getApplication().getOsmandMap().getMapView();
		updateMapSource(mapView, settings.MAP_TILE_SOURCES);
		OsmandPlugin.refreshLayers(ctx, mapActivity);
	}

	public void updateMapSource(@NonNull OsmandMapTileView mapView, CommonPreference<String> settingsToWarnAboutMap) {
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


	public AlertDialog showGPXFileLayer(@NonNull List<String> files, final MapActivity mapActivity) {
		final OsmandSettings settings = getApplication().getSettings();
		OsmandMapTileView mapView = mapActivity.getMapView();
		DashboardOnMap dashboard = mapActivity.getDashboard();
		CallbackWithObject<GPXFile[]> callbackWithObject = result -> {
			WptPt locToShow = null;
			for (GPXFile g : result) {
				if (g.showCurrentTrack) {
					if (!settings.SAVE_TRACK_TO_GPX.get() && !
							settings.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
						Toast.makeText(ctx,
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
			dashboard.refreshContent(true);
			return true;
		};
		return GpxUiHelper.selectGPXFiles(files, mapActivity, callbackWithObject, getThemeRes(getApplication()), isNightMode(getApplication()));
	}

	public void showMultichoicePoiFilterDialog(final MapActivity mapActivity, final DismissListener listener) {
		final OsmandApplication app = getApplication();
		final PoiFiltersHelper poiFilters = app.getPoiFilters();
		final ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		final List<PoiUIFilter> list = new ArrayList<>();
		for (PoiUIFilter f : poiFilters.getSortedPoiFilters(true)) {
			if (!f.isTopWikiFilter()) {
				addFilterToList(adapter, list, f, true);
			}
		}
		list.add(poiFilters.getCustomPOIFilter());
		adapter.setProfileDependent(true);
		adapter.setNightMode(isNightMode(app));

		final ArrayAdapter<ContextMenuItem> listAdapter = adapter.createListAdapter(mapActivity, !isNightMode(app));
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(mapActivity, getThemeRes(app)));
		final ListView listView = new ListView(ctx);
		listView.setDivider(null);
		listView.setClickable(true);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener((parent, view, position, id) -> {
			ContextMenuItem item = listAdapter.getItem(position);
			if (item != null) {
				item.setSelected(!item.getSelected());
				ItemClickListener clickListener = item.getItemClickListener();
				if (clickListener != null) {
					clickListener.onContextMenuClick(listAdapter, position, position, item.getSelected(), null);
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
				// TODO go to single choice dialog
				.setNeutralButton(" ", (dialog, which) -> showSingleChoicePoiFilterDialog(mapActivity, listener));
		final AlertDialog alertDialog = builder.create();
		alertDialog.setOnShowListener(dialog -> {
			Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
			Drawable drawable = app.getUIUtilities().getThemedIcon(R.drawable.ic_action_singleselect);
			neutralButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
			neutralButton.setContentDescription(app.getString(R.string.shared_string_filters));
		});
		alertDialog.setOnDismissListener(dialog -> listener.dismiss());
		alertDialog.show();
	}

	public void showSingleChoicePoiFilterDialog(final MapActivity mapActivity, final DismissListener listener) {
		final OsmandApplication app = getApplication();
		final PoiFiltersHelper poiFilters = app.getPoiFilters();
		final ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.shared_string_search, app)
				.setIcon(R.drawable.ic_action_search_dark).createItem());
		final List<PoiUIFilter> list = new ArrayList<>();
		list.add(poiFilters.getCustomPOIFilter());
		for (PoiUIFilter f : poiFilters.getSortedPoiFilters(true)) {
			if (!f.isTopWikiFilter()) {
				addFilterToList(adapter, list, f, false);
			}
		}

		final ArrayAdapter<ContextMenuItem> listAdapter = adapter.createListAdapter(mapActivity, !isNightMode(app));
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(mapActivity, getThemeRes(app)));
		builder.setAdapter(listAdapter, (dialog, which) -> {
			PoiUIFilter pf = list.get(which);
			String filterId = pf.getFilterId();
			if (filterId.equals(PoiUIFilter.CUSTOM_FILTER_ID)) {
				if (mapActivity.getDashboard().isVisible()) {
					mapActivity.getDashboard().hideDashboard();
				}
				mapActivity.showQuickSearch(ShowQuickSearchMode.NEW, true);
			} else {
				if (pf.isStandardFilter()) {
					pf.removeUnsavedFilterByName();
				}
				PoiUIFilter wiki = poiFilters.getTopWikiPoiFilter();
				poiFilters.clearSelectedPoiFilters(wiki);
				poiFilters.addSelectedPoiFilter(pf);
				updateRoutingPoiFiltersIfNeeded();
				mapActivity.getMapView().refreshMap();
			}
		});
		builder.setTitle(R.string.show_poi_over_map);
		builder.setNegativeButton(R.string.shared_string_dismiss, null);
		builder.setNeutralButton(" ", (dialog, which) -> showMultichoicePoiFilterDialog(mapActivity, listener));
		final AlertDialog alertDialog = builder.create();
		alertDialog.setOnShowListener(dialog -> {
			Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
			Drawable drawable = app.getUIUtilities().getThemedIcon(R.drawable.ic_action_multiselect);
			neutralButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
			neutralButton.setContentDescription(app.getString(R.string.apply_filters));
		});
		alertDialog.setOnDismissListener(dialog -> listener.dismiss());
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
			builder.setListener((adptr, itemId, position, isChecked, viewCoordinates) -> {
				ContextMenuItem item = adptr.getItem(position);
				if (item != null) {
					item.setSelected(isChecked);
				}
				return false;
			});
		}
		builder.setTitle(f.getName());
		if (RenderingIcons.containsBigIcon(f.getIconId())) {
			builder.setIcon(RenderingIcons.getBigIconResourceId(f.getIconId()));
		} else {
			builder.setIcon(R.drawable.mx_user_defined);
		}
		builder.setColor(ctx, ContextMenuItem.INVALID_ID);
		builder.setSkipPaintingWithoutColor(true);
		adapter.addItem(builder.createItem());
	}

	public void selectMapLayer(@NonNull final MapActivity mapActivity, @Nullable final ContextMenuItem it, @Nullable final ArrayAdapter<ContextMenuItem> adapter) {
		if (!OsmandPlugin.isActive(OsmandRasterMapsPlugin.class)) {
			Toast.makeText(ctx, R.string.map_online_plugin_is_not_installed, Toast.LENGTH_LONG).show();
			return;
		}
		final OsmandSettings settings = getApplication().getSettings();

		final LinkedHashMap<String, String> entriesMap = new LinkedHashMap<>();


		final String layerOsmVector = "LAYER_OSM_VECTOR";
		final String layerInstallMore = "LAYER_INSTALL_MORE";
		final String layerAdd = "LAYER_ADD";

		entriesMap.put(layerOsmVector, getString(R.string.vector_data));
		entriesMap.putAll(settings.getTileSourceEntries());
		entriesMap.put(layerInstallMore, getString(R.string.install_more));
		entriesMap.put(layerAdd, getString(R.string.shared_string_add));

		final List<Entry<String, String>> entriesMapList = new ArrayList<>(entriesMap.entrySet());

		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(mapActivity, getThemeRes(getApplication())));

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

		OsmandApplication app = getApplication();
		boolean nightMode = isNightMode(app);
		int themeRes = getThemeRes(app);
		int selectedModeColor = settings.getApplicationMode().getProfileColor(nightMode);
		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
				items, nightMode, selectedItem, app, selectedModeColor, themeRes, v -> {
					int which = (int) v.getTag();
					String layerKey = entriesMapList.get(which).getKey();
					switch (layerKey) {
						case layerOsmVector:
							settings.MAP_ONLINE_DATA.set(false);
							updateMapSource(mapActivity.getMapView(), null);
							updateItem(it, adapter, null);
							break;
						case layerAdd:
							OsmandRasterMapsPlugin.defineNewEditLayer(mapActivity.getSupportFragmentManager(), null, null);
							break;
						case layerInstallMore:
							OsmandRasterMapsPlugin.installMapLayers(mapActivity, new ResultMatcher<TileSourceTemplate>() {
								TileSourceTemplate template = null;
								int count = 0;

								@Override
								public boolean publish(TileSourceTemplate object) {
									if (object == null) {
										if (count == 1) {
											settings.MAP_TILE_SOURCES.set(template.getName());
											settings.MAP_ONLINE_DATA.set(true);
											updateItem(it, adapter, template.getName());
											updateMapSource(mapActivity.getMapView(), settings.MAP_TILE_SOURCES);
										} else {
											selectMapLayer(mapActivity, it, adapter);
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
							updateItem(it, adapter, layerKey.replace(IndexConstants.SQLITE_EXT, ""));
							updateMapSource(mapActivity.getMapView(), settings.MAP_TILE_SOURCES);
							break;
					}
				}
		);
		builder.setAdapter(dialogAdapter, null);
		builder.setNegativeButton(R.string.shared_string_dismiss, null);
		dialogAdapter.setDialog(builder.show());
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

	private void updateRoutingPoiFiltersIfNeeded() {
		OsmandApplication app = getApplication();
		OsmandSettings settings = app.getSettings();
		RoutingHelper routingHelper = app.getRoutingHelper();
		boolean usingRouting = routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()
				|| routingHelper.isRouteBeingCalculated() || routingHelper.isRouteCalculated();
		ApplicationMode routingMode = routingHelper.getAppMode();
		if (usingRouting && routingMode != settings.getApplicationMode()) {
			settings.setSelectedPoiFilters(routingMode, settings.getSelectedPoiFilters());
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
		return ctx.getString(resId);
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
