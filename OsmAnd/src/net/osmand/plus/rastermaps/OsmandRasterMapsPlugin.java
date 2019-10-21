package net.osmand.plus.rastermaps;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.ResultMatcher;
import net.osmand.StateChangedListener;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.ItemClickListener;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.OsmandSettings.LayerTransparencySeekbarMode;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.DownloadTilesDialog;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.dialogs.RasterMapMenu;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_DOWNLOAD_MAP;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OVERLAY_MAP;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.UNDERLAY_MAP;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_UPDATE_MAP;

public class OsmandRasterMapsPlugin extends OsmandPlugin {
	public static final String ID = "osmand.rastermaps";
	// Constants for determining the order of items in the additional actions context menu
	private static final int UPDATE_MAP_ITEM_ORDER = 12300;
	private static final int DOWNLOAD_MAP_ITEM_ORDER = 12600;

	private OsmandSettings settings;
	private OsmandApplication app;

	private MapTileLayer overlayLayer;
	private MapTileLayer underlayLayer;
	private StateChangedListener<Integer> overlayLayerListener;

	public OsmandRasterMapsPlugin(OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_world_globe_dark;
	}

	@Override
	public int getAssetResourceName() {
		return R.drawable.online_maps;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.osmand_rastermaps_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.shared_string_online_maps);
	}

	@Override
	public String getHelpFileName() {
		return "feature_articles/online-maps-plugin.html";
	}

	@Override
	public void registerLayers(MapActivity activity) {
		createLayers();
	}

	private void createLayers() {
		underlayLayer = new MapTileLayer(false);
		// mapView.addLayer(underlayLayer, -0.5f);
		overlayLayer = new MapTileLayer(false);
		overlayLayerListener = new StateChangedListener<Integer>() {
			@Override
			public void stateChanged(Integer change) {
				overlayLayer.setAlpha(change);
			}
		};
		// mapView.addLayer(overlayLayer, 0.7f);
		settings.MAP_OVERLAY_TRANSPARENCY.addListener(overlayLayerListener);


	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		updateMapLayers(mapView, null, activity.getMapLayers());
	}


	public void updateMapLayers(OsmandMapTileView mapView, CommonPreference<String> settingsToWarnAboutMap,
								final MapActivityLayers layers) {
		if (overlayLayer == null) {
			createLayers();
		}
		overlayLayer.setAlpha(settings.MAP_OVERLAY_TRANSPARENCY.get());
		if (isActive()) {
			updateLayer(mapView, settings, overlayLayer, settings.MAP_OVERLAY, 0.7f, settings.MAP_OVERLAY == settingsToWarnAboutMap);
		} else {
			mapView.removeLayer(overlayLayer);
			overlayLayer.setMap(null);
		}
		if (isActive()) {
			updateLayer(mapView, settings, underlayLayer, settings.MAP_UNDERLAY, -0.5f, settings.MAP_UNDERLAY == settingsToWarnAboutMap);
		} else {
			mapView.removeLayer(underlayLayer);
			underlayLayer.setMap(null);
		}
		if(settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get() == LayerTransparencySeekbarMode.UNDERLAY &&
				underlayLayer.getMap() != null) {
			layers.getMapControlsLayer().showTransparencyBar(settings.MAP_TRANSPARENCY, true);
		} else if(settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get() == LayerTransparencySeekbarMode.OVERLAY &&
				overlayLayer.getMap() != null) {
			layers.getMapControlsLayer().showTransparencyBar(settings.MAP_OVERLAY_TRANSPARENCY, true);
		}
		layers.updateMapSource(mapView, settingsToWarnAboutMap);
	}

	public void updateLayer(OsmandMapTileView mapView, OsmandSettings settings,
							MapTileLayer layer, CommonPreference<String> preference, float layerOrder, boolean warnWhenSelected) {
		ITileSource overlay = settings.getTileSourceByName(preference.get(), warnWhenSelected);
		if (!Algorithms.objectEquals(overlay, layer.getMap())) {
			if (overlay == null) {
				mapView.removeLayer(layer);
			} else if (mapView.getMapRenderer() == null) {
				mapView.addLayer(layer, layerOrder);
			}
			layer.setMap(overlay);
			mapView.refreshMap();
		}
	}

	public void selectMapOverlayLayer(@NonNull final OsmandMapTileView mapView,
									  @NonNull final CommonPreference<String> mapPref,
									  @NonNull final CommonPreference<String> exMapPref,
									  boolean force,
									  @NonNull final MapActivity activity,
									  @Nullable final OnMapSelectedCallback callback) {
		final MapActivityLayers layers = activity.getMapLayers();
		if (!force && exMapPref.get() != null) {
			mapPref.set(exMapPref.get());
			if (callback != null) {
				callback.onMapSelected(false);
			}
			updateMapLayers(mapView, mapPref, layers);
			return;
		}
		final OsmandSettings settings = app.getSettings();
		Map<String, String> entriesMap = settings.getTileSourceEntries();
		final ArrayList<String> keys = new ArrayList<>(entriesMap.keySet());
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		final String[] items = new String[entriesMap.size() + 1];
		int i = 0;
		for (String it : entriesMap.values()) {
			items[i++] = it;
		}

		items[i] = app.getString(R.string.install_more);
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == items.length - 1) {
					installMapLayers(activity, new ResultMatcher<TileSourceTemplate>() {
						TileSourceTemplate template = null;
						int count = 0;

						@Override
						public boolean publish(TileSourceTemplate object) {
							if (object == null) {
								if (count == 1) {
									mapPref.set(template.getName());
									exMapPref.set(template.getName());
									if (callback != null) {
										callback.onMapSelected(false);
									}
									updateMapLayers(mapView, mapPref, layers);
								} else {
									selectMapOverlayLayer(mapView, mapPref, exMapPref, false, activity, null);
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
				} else {
					mapPref.set(keys.get(which));
					exMapPref.set(keys.get(which));
					if (callback != null) {
						callback.onMapSelected(false);
					}
					updateMapLayers(mapView, mapPref, layers);
				}
				dialog.dismiss();
			}

		})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						if (callback != null) {
							callback.onMapSelected(true);
						}
					}
				});
		builder.show();
	}

	@Override
	public void registerLayerContextMenuActions(final OsmandMapTileView mapView,
												ContextMenuAdapter adapter,
												final MapActivity mapActivity) {
		ContextMenuAdapter.ItemClickListener listener = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int position) {
				int[] viewCoordinates = AndroidUtils.getCenterViewCoordinates(view);
				if (itemId == R.string.layer_overlay) {
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.OVERLAY_MAP, viewCoordinates);
					return false;
				} else if (itemId == R.string.layer_underlay) {
					mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.UNDERLAY_MAP, viewCoordinates);
					return false;
				}
				return true;
			}

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter, int itemId, final int pos, boolean isChecked, int[] viewCoordinates) {
				final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
				if (itemId == R.string.layer_overlay) {
					toggleUnderlayState(mapActivity, RasterMapType.OVERLAY,
							new OnMapSelectedCallback() {
								@Override
								public void onMapSelected(boolean canceled) {
									ContextMenuItem item = adapter.getItem(pos);

									String overlayMapDescr = settings.MAP_OVERLAY.get();

									boolean hasOverlayDescription = overlayMapDescr != null;
									overlayMapDescr = hasOverlayDescription ? overlayMapDescr
											: mapActivity.getString(R.string.shared_string_none);

									item.setDescription(overlayMapDescr);
									item.setSelected(hasOverlayDescription);
									item.setColorRes(hasOverlayDescription ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
									adapter.notifyDataSetChanged();
								}
							});
					return false;
				} else if (itemId == R.string.layer_underlay) {
					toggleUnderlayState(mapActivity, RasterMapType.UNDERLAY,
							new OnMapSelectedCallback() {
								@Override
								public void onMapSelected(boolean canceled) {
									ContextMenuItem item = adapter.getItem(pos);

									String underlayMapDescr = settings.MAP_UNDERLAY.get();

									boolean hasUnderlayDescription = underlayMapDescr != null;
									underlayMapDescr = hasUnderlayDescription ? underlayMapDescr
											: mapActivity.getString(R.string.shared_string_none);

									item.setDescription(underlayMapDescr);
									item.setSelected(hasUnderlayDescription);
									item.setColorRes(hasUnderlayDescription ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);

									adapter.notifyDataSetChanged();

									final CommonPreference<Boolean> hidePolygonsPref =
											mapActivity.getMyApplication().getSettings().getCustomRenderBooleanProperty("noPolygons");
									hidePolygonsPref.set(hasUnderlayDescription);
									RasterMapMenu.refreshMapComplete(mapActivity);
								}
							});
					return false;
				}
				return true;
			}
		};

		if (overlayLayer.getMap() == null) {
			settings.MAP_OVERLAY.set(null);
			settings.MAP_OVERLAY_PREVIOUS.set(null);
		}
		if (underlayLayer.getMap() == null) {
			settings.MAP_UNDERLAY.set(null);
			settings.MAP_UNDERLAY_PREVIOUS.set(null);
		}
		String overlayMapDescr = settings.MAP_OVERLAY.get();
		if (overlayMapDescr!=null && overlayMapDescr.contains(".sqlitedb")) {
			overlayMapDescr = overlayMapDescr.replaceFirst(".sqlitedb", "");
		}
		boolean hasOverlayDescription = overlayMapDescr != null;
		overlayMapDescr = hasOverlayDescription ? overlayMapDescr : mapActivity.getString(R.string.shared_string_none);
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.layer_overlay, mapActivity)
				.setId(OVERLAY_MAP)
				.setDescription(overlayMapDescr)
				.setSelected(hasOverlayDescription)
				.setColor(hasOverlayDescription ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_layer_top_dark)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(listener)
				.setPosition(14)
				.createItem());
		String underlayMapDescr = settings.MAP_UNDERLAY.get();
		if (underlayMapDescr!=null && underlayMapDescr.contains(".sqlitedb")) {
			underlayMapDescr = underlayMapDescr.replaceFirst(".sqlitedb", "");
		}
		boolean hasUnderlayDescription = underlayMapDescr != null;
		underlayMapDescr = hasUnderlayDescription ? underlayMapDescr : mapActivity.getString(R.string.shared_string_none);
		adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.layer_underlay, mapActivity)
				.setId(UNDERLAY_MAP)
				.setDescription(underlayMapDescr)
				.setSelected(hasUnderlayDescription)
				.setColor(hasUnderlayDescription ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setIcon(R.drawable.ic_layer_bottom_dark)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(listener)
				.setPosition(15)
				.createItem());
	}


	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity, final double latitude, final double longitude, ContextMenuAdapter adapter,
											  Object selectedObj) {
		final OsmandMapTileView mapView = mapActivity.getMapView();
		if (mapView.getMainLayer() instanceof MapTileLayer) {
			ItemClickListener listener = new ContextMenuAdapter.ItemClickListener() {
				@Override
				public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int resId, int pos, boolean isChecked, int[] viewCoordinates) {
					if (resId == R.string.context_menu_item_update_map) {
						mapActivity.getMapActions().reloadTile(mapView.getZoom(), latitude, longitude);
					} else if (resId == R.string.shared_string_download_map) {
						DownloadTilesDialog dlg = new DownloadTilesDialog(mapActivity, (OsmandApplication) mapActivity.getApplication(), mapView);
						dlg.openDialog();
					}
					return true;
				}
			};
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.context_menu_item_update_map, mapActivity)
					.setId(MAP_CONTEXT_MENU_UPDATE_MAP)
					.setIcon(R.drawable.ic_action_refresh_dark)
					.setOrder(UPDATE_MAP_ITEM_ORDER)
					.setListener(listener).createItem());
			adapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.shared_string_download_map, mapActivity)
					.setId(MAP_CONTEXT_MENU_DOWNLOAD_MAP)
					.setIcon(R.drawable.ic_action_import)
					.setOrder(DOWNLOAD_MAP_ITEM_ORDER)
					.setListener(listener).createItem());
		}
	}

	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return SettingsRasterMapsActivity.class;
	}


	public static void installMapLayers(final Activity activity, final ResultMatcher<TileSourceTemplate> result) {
		final OsmandApplication app = (OsmandApplication) activity.getApplication();
		final OsmandSettings settings = app.getSettings();
		final Map<String, String> entriesMap = settings.getTileSourceEntries();
		if (!settings.isInternetConnectionAvailable(true)) {
			Toast.makeText(activity, R.string.internet_not_available, Toast.LENGTH_LONG).show();
			return;
		}
		AsyncTask<Void, Void, List<TileSourceTemplate>> t = new AsyncTask<Void, Void, List<TileSourceTemplate>>() {
			@Override
			protected List<TileSourceTemplate> doInBackground(Void... params) {
				return TileSourceManager.downloadTileSourceTemplates(Version.getVersionAsURLParam(app), true);
			}

			protected void onPostExecute(final java.util.List<TileSourceTemplate> downloaded) {
				if (downloaded == null || downloaded.isEmpty()) {
					Toast.makeText(activity, R.string.shared_string_io_error, Toast.LENGTH_SHORT).show();
					return;
				}
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				String[] names = new String[downloaded.size()];
				for (int i = 0; i < names.length; i++) {
					names[i] = downloaded.get(i).getName();
				}
				final boolean[] selected = new boolean[downloaded.size()];
				builder.setMultiChoiceItems(names, selected, new DialogInterface.OnMultiChoiceClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						selected[which] = isChecked;
						if (entriesMap.containsKey(downloaded.get(which).getName()) && isChecked) {
							Toast.makeText(activity, R.string.tile_source_already_installed, Toast.LENGTH_SHORT).show();
						}
					}
				});
				builder.setNegativeButton(R.string.shared_string_cancel, null);
				builder.setTitle(R.string.select_tile_source_to_install);
				builder.setPositiveButton(R.string.shared_string_apply, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
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

				builder.show();
			}
		};
		t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void defineNewEditLayer(final Activity activity, final ResultMatcher<TileSourceTemplate> resultMatcher) {
		final OsmandApplication app = (OsmandApplication) activity.getApplication();
		final OsmandSettings settings = app.getSettings();
		final Map<String, String> entriesMap = settings.getTileSourceEntries(false);
		TileSourceTemplate ts = new TileSourceTemplate("NewMapnik", "http://mapnik.osmand.net/{0}/{1}/{2}.png",
				"png", 17, 5, 256, 16, 32000);
		final TileSourceTemplate[] result = new TileSourceTemplate[]{ts};
		AlertDialog.Builder bld = new AlertDialog.Builder(activity);
		View view = activity.getLayoutInflater().inflate(R.layout.editing_tile_source, null);
		final EditText name = (EditText) view.findViewById(R.id.Name);
		final Spinner existing = (Spinner) view.findViewById(R.id.TileSourceSpinner);
		final EditText urlToLoad = (EditText) view.findViewById(R.id.URLToLoad);
		final EditText minZoom = (EditText) view.findViewById(R.id.MinZoom);
		final EditText maxZoom = (EditText) view.findViewById(R.id.MaxZoom);
		final EditText expire = (EditText) view.findViewById(R.id.ExpirationTime);
		final CheckBox elliptic = (CheckBox) view.findViewById(R.id.EllipticMercator);
		updateTileSourceEditView(ts, name, urlToLoad, minZoom, maxZoom, expire, elliptic);

		final ArrayList<String> templates = new ArrayList<>(entriesMap.keySet());
		templates.add(0, "");

		ArrayAdapter<String> adapter = new ArrayAdapter<>(view.getContext(),
				android.R.layout.simple_spinner_item,
				templates
		);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		existing.setAdapter(adapter);
		existing.setSelection(0);
		existing.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position > 0) {
					File f = ((OsmandApplication) activity.getApplication()).getAppPath(IndexConstants.TILES_INDEX_DIR + templates.get(position));
					TileSourceTemplate template = TileSourceManager.createTileSourceTemplate(f);
					if (template != null) {
						result[0] = template.copy();
						updateTileSourceEditView(result[0], name, urlToLoad, minZoom, maxZoom, expire, elliptic);
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		bld.setView(view);
		bld.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				TileSourceTemplate r = result[0];
				try {
					r.setName(name.getText().toString());
					r.setExpirationTimeMinutes(expire.getText().length() == 0 ? -1 :
							Integer.parseInt(expire.getText().toString()));
					r.setMinZoom(Integer.parseInt(minZoom.getText().toString()));
					r.setMaxZoom(Integer.parseInt(maxZoom.getText().toString()));
					r.setEllipticYTile(elliptic.isChecked());
					r.setUrlToLoad(urlToLoad.getText().toString().equals("") ? null : urlToLoad.getText().toString().replace("{$x}", "{1}")
							.replace("{$y}", "{2}").replace("{$z}", "{0}"));
					if (r.getName().length() > 0) {
						if (settings.installTileSource(r)) {
							Toast.makeText(activity, activity.getString(R.string.edit_tilesource_successfully, r.getName()),
									Toast.LENGTH_SHORT).show();
							resultMatcher.publish(r);
						}
					}
				} catch (RuntimeException e) {
					Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
				}
			}
		});
		bld.setNegativeButton(R.string.shared_string_cancel, null);
		bld.show();
	}

	private static void updateTileSourceEditView(TileSourceTemplate ts, EditText name, final EditText urlToLoad, final EditText minZoom,
												 final EditText maxZoom, EditText expire, final CheckBox elliptic) {
		minZoom.setText(String.valueOf(ts.getMinimumZoomSupported()));
		maxZoom.setText(String.valueOf(ts.getMaximumZoomSupported()));
		name.setText(ts.getName());
		expire.setText(ts.getExpirationTimeMinutes() < 0 ? "" : ts.getExpirationTimeMinutes() + "");
		urlToLoad.setText(ts.getUrlTemplate() == null ? "" :
				ts.getUrlTemplate().replace("{$x}", "{1}").replace("{$y}", "{2}").replace("{$z}", "{0}"));
		elliptic.setChecked(ts.isEllipticYTile());
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
		OsmandMapTileView mapView = mapActivity.getMapView();
		CommonPreference<String> mapTypePreference;
		CommonPreference<String> exMapTypePreference;
		OsmandSettings.CommonPreference<Integer> mapTransparencyPreference;

		//boolean isMapSelected;
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
		MapActivityLayers mapLayers = mapActivity.getMapLayers();
		ITileSource map = layer.getMap();
		final OsmandSettings.LayerTransparencySeekbarMode currentMapTypeSeekbarMode = type ==
			OsmandRasterMapsPlugin.RasterMapType.OVERLAY
			? OsmandSettings.LayerTransparencySeekbarMode.OVERLAY
			: OsmandSettings.LayerTransparencySeekbarMode.UNDERLAY;
		if (map != null) {
			mapTypePreference.set(null);
			if (callback != null) {
				callback.onMapSelected(false);
			}
			updateMapLayers(mapView, null, mapLayers);
			// hide seekbar
			if(currentMapTypeSeekbarMode == settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get()) {
				settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.set(LayerTransparencySeekbarMode.UNDEFINED);
				mapLayers.getMapControlsLayer().hideTransparencyBar();
			}
		} else {
			settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.set(currentMapTypeSeekbarMode);
			selectMapOverlayLayer(mapView, mapTypePreference, exMapTypePreference, false, mapActivity, callback);
		}
	}

	public enum RasterMapType {
		OVERLAY,
		UNDERLAY
	}

	public interface OnMapSelectedCallback {
		void onMapSelected(boolean canceled);
	}
}
