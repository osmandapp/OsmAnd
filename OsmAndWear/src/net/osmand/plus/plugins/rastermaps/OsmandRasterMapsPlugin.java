package net.osmand.plus.plugins.rastermaps;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.HIDE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_DOWNLOAD_MAP;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_UPDATE_MAP;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OVERLAY_MAP;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_RASTER_MAPS;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.UNDERLAY_MAP;
import static net.osmand.plus.resources.ResourceManager.ResourceListener;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.CallbackWithObject;
import net.osmand.ResultMatcher;
import net.osmand.StateChangedListener;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.mapsource.EditMapSourceDialogFragment;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.MapLayerType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OsmandRasterMapsPlugin extends OsmandPlugin {

	public static final String NO_POLYGONS_ATTR = "noPolygons";
	public static final String HIDE_WATER_POLYGONS_ATTR = "hideWaterPolygons";

	// Constants for determining the order of items in the additional actions context menu
	private static final int UPDATE_MAP_ITEM_ORDER = 12300;
	private static final int DOWNLOAD_MAP_ITEM_ORDER = 12600;
	private static final float ZORDER_UNDERLAY = -0.5f;
	private static final float ZORDER_OVERLAY = 0.7f;

	private final OsmandSettings settings;

	private MapTileLayer overlayLayer;
	private MapTileLayer underlayLayer;
	private StateChangedListener<String> underlayListener;
	private StateChangedListener<Integer> overlayLayerListener;
	private CallbackWithObject<Boolean> updateConfigureMapItemCallback;

	public OsmandRasterMapsPlugin(OsmandApplication app) {
		super(app);
		settings = app.getSettings();
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_world_globe_dark;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.online_maps);
	}

	@Override
	public String getId() {
		return PLUGIN_RASTER_MAPS;
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		return app.getString(R.string.osmand_rastermaps_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.shared_string_online_maps);
	}

	@Override
	public boolean init(@NonNull OsmandApplication app, Activity activity) {
		underlayListener = change -> {
			if (updateConfigureMapItemCallback != null) {
				boolean selected = settings.MAP_UNDERLAY.get() != null;
				updateConfigureMapItemCallback.processResult(selected);
			}
		};
		settings.MAP_UNDERLAY.addListener(underlayListener);

		ResourceListener resourceListener = new ResourceListener() {
			@Override
			public void onMapClosed(String fileName) {
				clearLayer(fileName);
			}
		};
		app.getResourceManager().addResourceListener(resourceListener);
		return true;
	}

	@Override
	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		createLayers(context);
	}

	private void createLayers(@NonNull Context context) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		if (underlayLayer != null) {
			mapView.removeLayer(underlayLayer);
		}
		if (overlayLayer != null) {
			mapView.removeLayer(overlayLayer);
		}
		underlayLayer = new MapTileLayer(context, false);
		overlayLayer = new MapTileLayer(context, false);
		overlayLayerListener = change -> app.runInUIThread(() -> overlayLayer.setAlpha(change));
		settings.MAP_OVERLAY_TRANSPARENCY.addListener(overlayLayerListener);
	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		updateMapLayers(context, mapActivity, null);
	}

	public void updateMapLayers(@NonNull Context context, @Nullable MapActivity mapActivity,
	                            @Nullable CommonPreference<String> settingsToWarnAboutMap) {
		if (overlayLayer == null) {
			createLayers(context);
		}
		overlayLayer.setAlpha(settings.MAP_OVERLAY_TRANSPARENCY.get());
		overlayLayer.updateParameter();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		// Overlay
		if (isActive()) {
			updateLayer(mapView, settings, overlayLayer, settings.MAP_OVERLAY, ZORDER_OVERLAY, settings.MAP_OVERLAY == settingsToWarnAboutMap);
		} else {
			mapView.removeLayer(overlayLayer);
			overlayLayer.setMap(null);
		}
		// Underlay
		if (isActive()) {
			updateLayer(mapView, settings, underlayLayer, settings.MAP_UNDERLAY, ZORDER_UNDERLAY, settings.MAP_UNDERLAY == settingsToWarnAboutMap);
		} else {
			mapView.removeLayer(underlayLayer);
			underlayLayer.setMap(null);
		}
		if (mapActivity != null) {
			MapLayers layers = mapActivity.getMapLayers();
			if (settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get() == LayerTransparencySeekbarMode.UNDERLAY &&
					underlayLayer.getMap() != null || underlayLayer.getMapTileAdapter() != null) {
				layers.getMapControlsLayer().getMapTransparencyHelper().showTransparencyBar(settings.MAP_TRANSPARENCY);
			} else if (settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get() == LayerTransparencySeekbarMode.OVERLAY &&
					overlayLayer.getMap() != null || overlayLayer.getMapTileAdapter() != null) {
				if (settings.SHOW_MAP_LAYER_PARAMETER.get()) {
					layers.getMapControlsLayer().getMapTransparencyHelper().showParameterBar(overlayLayer);
				} else {
					layers.getMapControlsLayer().getMapTransparencyHelper().showTransparencyBar(settings.MAP_OVERLAY_TRANSPARENCY);
				}
			} else {
				layers.getMapControlsLayer().getMapTransparencyHelper().hideTransparencyBar();
			}
		}
		app.getOsmandMap().getMapLayers().updateMapSource(mapView, settingsToWarnAboutMap);
	}

	public boolean updateLayer(OsmandMapTileView mapView, OsmandSettings settings,
	                           MapTileLayer layer, CommonPreference<String> preference,
	                           float layerOrder, boolean warnWhenSelected) {
		ITileSource overlay = settings.getTileSourceByName(preference.get(), warnWhenSelected);
		if (!Algorithms.objectEquals(overlay, layer.getMap())) {
			if (overlay == null) {
				mapView.removeLayer(layer);
			} else if (!mapView.isLayerExists(layer)) {
				mapView.addLayer(layer, layerOrder);
			}
			layer.setMap(overlay);
			mapView.refreshMap();
			return true;
		}
		return false;
	}

	public void clearLayer(@NonNull String tileSourceName) {
		int idx = tileSourceName.lastIndexOf(SQLiteTileSource.EXT);
		if (idx > 0) {
			tileSourceName = tileSourceName.substring(0, idx);
		}
		if (overlayLayer != null && overlayLayer.getMap() != null
				&& tileSourceName.equals(overlayLayer.getMap().getName())) {
			overlayLayer.setMap(null);
		}
		if (Algorithms.stringsEqual(tileSourceName, settings.MAP_OVERLAY.get())) {
			settings.MAP_OVERLAY.set(null);
		}
		if (Algorithms.stringsEqual(tileSourceName, settings.MAP_OVERLAY_PREVIOUS.get())) {
			settings.MAP_OVERLAY_PREVIOUS.set(null);
		}
		if (underlayLayer != null && underlayLayer.getMap() != null
				&& tileSourceName.equals(underlayLayer.getMap().getName())) {
			underlayLayer.setMap(null);
		}
		if (Algorithms.stringsEqual(tileSourceName, settings.MAP_UNDERLAY.get())) {
			settings.MAP_UNDERLAY.set(null);
		}
		if (Algorithms.stringsEqual(tileSourceName, settings.MAP_UNDERLAY_PREVIOUS.get())) {
			settings.MAP_UNDERLAY_PREVIOUS.set(null);
		}
	}

	public void selectMapOverlayLayer(@NonNull CommonPreference<String> mapPref,
	                                  @NonNull CommonPreference<String> exMapPref,
	                                  boolean force,
	                                  @NonNull MapActivity mapActivity,
	                                  @Nullable OnMapSelectedCallback callback) {
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		if (!force && exMapPref.get() != null) {
			mapPref.set(exMapPref.get());
			updateMapLayers(mapActivity, mapActivity, mapPref);
			if (callback != null) {
				callback.onMapSelected(false);
			}
			return;
		}
		OsmandSettings settings = app.getSettings();
		Map<String, String> entriesMap = settings.getTileSourceEntries();
		ArrayList<String> keys = new ArrayList<>(entriesMap.keySet());
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(mapActivity, getThemeRes(mapActivity)));
		String[] items = new String[entriesMap.size() + 1];
		int i = 0;
		for (String it : entriesMap.values()) {
			items[i++] = it;
		}

		items[i] = app.getString(R.string.install_more);
		builder.setSingleChoiceItems(items, -1, (dialog, which) -> {
					MapActivity activity = mapActivityRef.get();
					if (activity == null || activity.isFinishing()) {
						return;
					}
					if (which == items.length - 1) {
						installMapLayers(activity, new ResultMatcher<TileSourceTemplate>() {
							TileSourceTemplate template;
							int count;
							boolean cancel;

							@Override
							public boolean publish(TileSourceTemplate object) {
								MapActivity mapActv = mapActivityRef.get();
								if (mapActv == null || mapActv.isFinishing()) {
									cancel = true;
									return false;
								}
								if (object == null) {
									if (count == 1) {
										mapPref.set(template.getName());
										exMapPref.set(template.getName());
										updateMapLayers(mapActv, mapActv, mapPref);
										if (callback != null) {
											callback.onMapSelected(false);
										}
									} else {
										selectMapOverlayLayer(mapPref, exMapPref, false, mapActv, null);
									}
								} else {
									count++;
									template = object;
								}
								return false;
							}

							@Override
							public boolean isCancelled() {
								return cancel;
							}
						});
					} else {
						mapPref.set(keys.get(which));
						exMapPref.set(keys.get(which));
						updateMapLayers(activity, activity, mapPref);
						if (callback != null) {
							callback.onMapSelected(false);
						}
					}
					dialog.dismiss();
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setOnDismissListener(dialog -> {
					if (callback != null) {
						callback.onMapSelected(true);
					}
				});
		builder.show();
	}

	@Override
	protected void registerLayerContextMenuActions(@NonNull ContextMenuAdapter adapter, @NonNull MapActivity mapActivity, @NonNull List<RenderingRuleProperty> customRules) {
		if (!isEnabled()) {
			return;
		}

		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		ItemClickListener listener = new OnRowItemClick() {

			@Override
			public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter,
			                              @NonNull View view, @NonNull ContextMenuItem item) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null && !mapActivity.isFinishing()) {
					int[] viewCoordinates = AndroidUtils.getCenterViewCoordinates(view);
					int itemId = item.getTitleId();
					if (itemId == R.string.layer_overlay) {
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.OVERLAY_MAP, viewCoordinates);
						return false;
					} else if (itemId == R.string.layer_underlay) {
						mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.UNDERLAY_MAP, viewCoordinates);
						return false;
					}
				}
				return true;
			}

			@Override
			public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter,
			                                  @Nullable View view, @NonNull ContextMenuItem item,
			                                  boolean isChecked) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity == null || mapActivity.isFinishing()) {
					return false;
				}
				int itemId = item.getTitleId();
				if (itemId == R.string.layer_overlay) {
					toggleUnderlayState(mapActivity, RasterMapType.OVERLAY,
							canceled -> {
								MapActivity mapActv = mapActivityRef.get();
								if (mapActv != null && !mapActv.isFinishing()) {
									String overlayMapDescr = mapActv.getMyApplication().getSettings().MAP_OVERLAY.get();
									boolean hasOverlayDescription = overlayMapDescr != null;
									overlayMapDescr = hasOverlayDescription ? overlayMapDescr
											: mapActv.getString(R.string.shared_string_none);
									item.setDescription(overlayMapDescr);
									item.setSelected(hasOverlayDescription);
									item.setColor(app, hasOverlayDescription ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
									if (uiAdapter != null) {
										uiAdapter.onDataSetChanged();
									}
								}
							});
					return false;
				} else if (itemId == R.string.layer_underlay) {
					toggleUnderlayState(mapActivity, RasterMapType.UNDERLAY,
							canceled -> {
								MapActivity mapActv = mapActivityRef.get();
								if (mapActv != null && !mapActv.isFinishing()) {
									String underlayMapDescr = settings.MAP_UNDERLAY.get();

									boolean hasUnderlayDescription = underlayMapDescr != null;
									underlayMapDescr = hasUnderlayDescription
											? underlayMapDescr
											: mapActv.getString(R.string.shared_string_none);

									item.setDescription(underlayMapDescr);
									item.setSelected(hasUnderlayDescription);
									item.setColor(app, hasUnderlayDescription ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
									if (uiAdapter != null) {
										uiAdapter.onDataSetChanged();
									}
									if (!canceled) {
										mapActv.refreshMapComplete();
									}
								}
							});
					return false;
				}
				return true;
			}
		};

		updateConfigureMapItemCallback = result -> {
			MapActivity ma = mapActivityRef.get();
			if (ma != null) {
				ConfigureMapFragment fragment = ConfigureMapFragment.getVisibleInstance(ma);
				if (fragment != null) {
					fragment.onRefreshItem(HIDE_ID);
				}
				return true;
			}
			return false;
		};

		String overlayMapDescr = settings.MAP_OVERLAY.get();
		if (overlayMapDescr != null) {
			overlayMapDescr = settings.getTileSourceTitle(overlayMapDescr);
		}
		boolean hasOverlayDescription = overlayMapDescr != null;
		overlayMapDescr = hasOverlayDescription ? overlayMapDescr : mapActivity.getString(R.string.shared_string_none);
		adapter.addItem(new ContextMenuItem(OVERLAY_MAP)
				.setTitleId(R.string.layer_overlay, mapActivity)
				.setDescription(overlayMapDescr)
				.setSelected(hasOverlayDescription)
				.setColor(app, hasOverlayDescription ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_layer_top)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(listener)
				.setItemDeleteAction(settings.MAP_OVERLAY, settings.MAP_OVERLAY_PREVIOUS, settings.MAP_OVERLAY_TRANSPARENCY));
		String underlayMapDescr = settings.MAP_UNDERLAY.get();
		if (underlayMapDescr != null) {
			underlayMapDescr = settings.getTileSourceTitle(underlayMapDescr);
		}
		boolean hasUnderlayDescription = underlayMapDescr != null;
		underlayMapDescr = hasUnderlayDescription ? underlayMapDescr : mapActivity.getString(R.string.shared_string_none);
		adapter.addItem(new ContextMenuItem(UNDERLAY_MAP)
				.setTitleId(R.string.layer_underlay, mapActivity)
				.setDescription(underlayMapDescr)
				.setSelected(hasUnderlayDescription)
				.setColor(app, hasUnderlayDescription ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_layer_bottom)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(listener)
				.setItemDeleteAction(settings.MAP_UNDERLAY, settings.MAP_UNDERLAY_PREVIOUS));
	}

	@Override
	public void registerMapContextMenuActions(@NonNull MapActivity mapActivity,
	                                          double latitude, double longitude,
	                                          ContextMenuAdapter adapter, Object selectedObj, boolean configureMenu) {
		boolean mapTileLayer = mapActivity.getMapView().getMainLayer() instanceof MapTileLayer
				|| overlayLayer != null && overlayLayer.getMap() != null && overlayLayer.getMap().couldBeDownloadedFromInternet()
				|| underlayLayer != null && underlayLayer.getMap() != null && underlayLayer.getMap().couldBeDownloadedFromInternet();
		if (configureMenu || mapTileLayer) {
			adapter.addItem(createMapMenuItem(mapActivity, mapTileLayer, true));
			adapter.addItem(createMapMenuItem(mapActivity, mapTileLayer, false));
		}
	}

	@NonNull
	private ContextMenuItem createMapMenuItem(@NonNull MapActivity mapActivity,
	                                          boolean mainMapTileLayer,
	                                          boolean updateTiles) {
		ContextMenuItem item;
		if (updateTiles) {
			item = new ContextMenuItem(MAP_CONTEXT_MENU_UPDATE_MAP)
					.setTitleId(R.string.context_menu_item_update_map, mapActivity)
					.setIcon(R.drawable.ic_action_refresh_dark)
					.setOrder(UPDATE_MAP_ITEM_ORDER);
		} else {
			item = new ContextMenuItem(MAP_CONTEXT_MENU_DOWNLOAD_MAP)
					.setTitleId(R.string.shared_string_download_map, mapActivity)
					.setIcon(R.drawable.ic_action_import)
					.setOrder(DOWNLOAD_MAP_ITEM_ORDER);
		}

		if (mainMapTileLayer) {
			WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
			ItemClickListener listener = (uiAdapter, view, _item, isChecked) -> {
				MapActivity activityForSelect = mapActivityRef.get();
				if (AndroidUtils.isActivityNotDestroyed(activityForSelect)) {
					MapLayerBottomSheet.showInstance(app,
							activityForSelect.getSupportFragmentManager(),
							getDownloadableLayerNameIds(),
							updateTiles);
				}
				return true;
			};
			item.setListener(listener);
		}

		return item;
	}

	@NonNull
	private List<MapLayerType> getDownloadableLayerNameIds() {
		List<MapLayerType> layerTypes = new ArrayList<>();
		OsmandMapLayer mainLayer = app.getOsmandMap().getMapView().getMainLayer();
		if (mainLayer instanceof MapTileLayer && ((MapTileLayer) mainLayer).getMap().couldBeDownloadedFromInternet()) {
			layerTypes.add(MapLayerType.MAP_SOURCE);
		}
		if (isMapLayerDownloadable(app.getSettings().MAP_OVERLAY.get())) {
			layerTypes.add(MapLayerType.MAP_OVERLAY);
		}
		if (isMapLayerDownloadable(app.getSettings().MAP_UNDERLAY.get())) {
			layerTypes.add(MapLayerType.MAP_UNDERLAY);
		}
		return layerTypes;
	}

	private boolean isMapLayerDownloadable(String layerName) {
		ITileSource overlayLayerMapSource = app.getSettings().getTileSourceByName(layerName, false);
		return overlayLayerMapSource != null && overlayLayerMapSource.couldBeDownloadedFromInternet();
	}

	public static void installMapLayers(@NonNull Activity activity, ResultMatcher<TileSourceTemplate> result) {
		WeakReference<Activity> activityRef = new WeakReference<>(activity);
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		OsmandSettings settings = app.getSettings();
		if (!settings.isInternetConnectionAvailable(true)) {
			Toast.makeText(activity, R.string.internet_not_available, Toast.LENGTH_LONG).show();
			return;
		}
		AsyncTask<Void, Void, List<TileSourceTemplate>> t = new AsyncTask<Void, Void, List<TileSourceTemplate>>() {
			@Override
			protected List<TileSourceTemplate> doInBackground(Void... params) {
				return TileSourceManager.downloadTileSourceTemplates(Version.getVersionAsURLParam(app), true);
			}

			protected void onPostExecute(java.util.List<TileSourceTemplate> downloaded) {
				Activity activity = activityRef.get();
				if (activity == null || activity.isFinishing()) {
					return;
				}
				OsmandApplication app = (OsmandApplication) activity.getApplication();
				if (downloaded == null || downloaded.isEmpty()) {
					Toast.makeText(activity, R.string.shared_string_io_error, Toast.LENGTH_SHORT).show();
					return;
				}
				String[] names = new String[downloaded.size()];
				for (int i = 0; i < names.length; i++) {
					names[i] = downloaded.get(i).getName();
				}
				boolean[] selected = new boolean[downloaded.size()];
				boolean nightMode = isNightMode(activity);

				AlertDialogData dialogData = new AlertDialogData(activity, nightMode)
						.setTitle(R.string.select_tile_source_to_install)
						.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode))
						.setNegativeButton(R.string.shared_string_cancel, null)
						.setPositiveButton(R.string.shared_string_apply, (dialog, which) -> {
							Activity _activity = activityRef.get();
							if (_activity != null && !_activity.isFinishing()) {
								List<TileSourceTemplate> toInstall = new ArrayList<>();
								for (int i = 0; i < selected.length; i++) {
									if (selected[i]) {
										toInstall.add(downloaded.get(i));
									}
								}
								for (TileSourceTemplate ts : toInstall) {
									if (settings.installTileSource(ts)) {
										if (result != null) {
											result.publish(ts);
										}
									}
								}
								// at the end publish null to show end of process
								if (!toInstall.isEmpty() && result != null) {
									result.publish(null);
								}
							}
						});

				CustomAlert.showMultiSelection(dialogData, names, selected, v -> {
					Activity _activity = activityRef.get();
					if (_activity != null && !_activity.isFinishing()) {
						Map<String, String> entriesMap = settings.getTileSourceEntries();
						int which = (int) v.getTag();
						selected[which] = !selected[which];
						if (entriesMap.containsKey(downloaded.get(which).getName()) && selected[which]) {
							Toast.makeText(_activity, R.string.tile_source_already_installed, Toast.LENGTH_SHORT).show();
						}
					}
				});
			}
		};
		t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void defineNewEditLayer(@NonNull FragmentActivity activity, @Nullable Fragment targetFragment, @Nullable String editedFileName) {
		EditMapSourceDialogFragment.showInstance(activity, targetFragment, editedFileName);
	}

	public MapTileLayer getUnderlayLayer() {
		return underlayLayer;
	}

	public MapTileLayer getOverlayLayer() {
		return overlayLayer;
	}

	public void toggleUnderlayState(@NonNull MapActivity mapActivity,
	                                @NonNull RasterMapType type,
	                                @Nullable OnMapSelectedCallback callback) {
		CommonPreference<String> mapTypePreference;
		CommonPreference<String> exMapTypePreference;
		MapTileLayer layer;
		if (type == RasterMapType.OVERLAY) {
			mapTypePreference = settings.MAP_OVERLAY;
			exMapTypePreference = settings.MAP_OVERLAY_PREVIOUS;
			layer = overlayLayer;
		} else {
			// Underlay expected
			mapTypePreference = settings.MAP_UNDERLAY;
			exMapTypePreference = settings.MAP_UNDERLAY_PREVIOUS;
			layer = underlayLayer;
		}
		MapLayers mapLayers = mapActivity.getMapLayers();
		ITileSource map = layer.getMap();
		LayerTransparencySeekbarMode currentMapTypeSeekbarMode = type == RasterMapType.OVERLAY
				? LayerTransparencySeekbarMode.OVERLAY
				: LayerTransparencySeekbarMode.UNDERLAY;
		if (map != null) {
			mapTypePreference.set(null);
			updateMapLayers(mapActivity, mapActivity, null);
			if (callback != null) {
				callback.onMapSelected(false);
			}
			// hide seekbar
			if (currentMapTypeSeekbarMode == settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get()) {
				settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.set(LayerTransparencySeekbarMode.UNDEFINED);
				mapLayers.getMapControlsLayer().getMapTransparencyHelper().hideTransparencyBar();
			}
		} else {
			settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.set(currentMapTypeSeekbarMode);
			selectMapOverlayLayer(mapTypePreference, exMapTypePreference, false, mapActivity, callback);
		}
	}

	private static boolean isNightMode(Context context) {
		if (context == null) {
			return false;
		}
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		return app.getDaynightHelper().isNightMode(context instanceof MapActivity);
	}

	private static int getThemeRes(Context context) {
		return isNightMode(context) ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
	}

	public enum RasterMapType {
		OVERLAY,
		UNDERLAY
	}

	public interface OnMapSelectedCallback {
		void onMapSelected(boolean canceled);
	}

	@Override
	protected List<QuickActionType> getQuickActionTypes() {
		List<QuickActionType> quickActionTypes = new ArrayList<>();
		quickActionTypes.add(MapSourceAction.TYPE);
		quickActionTypes.add(MapOverlayAction.TYPE);
		quickActionTypes.add(MapUnderlayAction.TYPE);
		return quickActionTypes;
	}
}
