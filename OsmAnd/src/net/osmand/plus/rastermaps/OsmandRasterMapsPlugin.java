package net.osmand.plus.rastermaps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.osmand.ResultMatcher;
import net.osmand.StateChangedListener;
import net.osmand.access.AccessibleToast;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.DownloadTilesDialog;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class OsmandRasterMapsPlugin extends OsmandPlugin {
	private static final String ID = "osmand.rastermaps";
	private OsmandSettings settings;
	private OsmandApplication app;
	
	private MapTileLayer overlayLayer;
	private MapTileLayer underlayLayer;
	
	public OsmandRasterMapsPlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public boolean init(OsmandApplication app) {
		settings = app.getSettings();
		return true;
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
		return app.getString(R.string.online_map_settings);
	}
	@Override
	public void registerLayers(MapActivity activity) {
		createLayers();
	}

	private void createLayers() {
		underlayLayer = new MapTileLayer(false);
		// mapView.addLayer(underlayLayer, -0.5f);
		overlayLayer = new MapTileLayer(false);
		// mapView.addLayer(overlayLayer, 0.7f);
		settings.MAP_OVERLAY_TRANSPARENCY.addListener(new StateChangedListener<Integer>() {
			@Override
			public void stateChanged(Integer change) {
				overlayLayer.setAlpha(change);
			}
		});
	}
	
	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		updateMapLayers(mapView, null, activity.getMapLayers());
	}
	
	
	public void updateMapLayers(OsmandMapTileView mapView, CommonPreference<String> settingsToWarnAboutMap,
			final MapActivityLayers layers) {
		if(overlayLayer == null) {
			createLayers();
		}
		overlayLayer.setAlpha(settings.MAP_OVERLAY_TRANSPARENCY.get());
		updateLayer(mapView, settings, overlayLayer, settings.MAP_OVERLAY, 0.7f, settings.MAP_OVERLAY == settingsToWarnAboutMap);
		updateLayer(mapView, settings, underlayLayer, settings.MAP_UNDERLAY, -0.5f, settings.MAP_UNDERLAY == settingsToWarnAboutMap);
		layers.updateMapSource(mapView, settingsToWarnAboutMap);
	}
	
	public void updateLayer(OsmandMapTileView mapView, OsmandSettings settings,
			MapTileLayer layer, CommonPreference<String> preference, float layerOrder, boolean warnWhenSelected) {
		ITileSource overlay = settings.getTileSourceByName(preference.get(), warnWhenSelected);
		if(!Algorithms.objectEquals(overlay, layer.getMap())){
			if(overlay == null){
				mapView.removeLayer(layer);
			} else {
				mapView.addLayer(layer, layerOrder);
			}
			layer.setMap(overlay);
			mapView.refreshMap();
		}
	}
	
	public void selectMapOverlayLayer(final OsmandMapTileView mapView, 
			final CommonPreference<String> mapPref, final CommonPreference<Integer> transparencyPref,
			final MapActivity activity){
		final OsmandSettings settings = app.getSettings();
		final MapActivityLayers layers = activity.getMapLayers();
		Map<String, String> entriesMap = settings.getTileSourceEntries();
		final ArrayList<String> keys = new ArrayList<String>(entriesMap.keySet());
		Builder builder = new AlertDialog.Builder(activity);
		final String[] items = new String[entriesMap.size() + 1];
		int i = 0;
		for(String it : entriesMap.values()){
			items[i++] = it;
		}
		
		items[i] = app.getString(R.string.install_more);
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == items.length - 1){
					installMapLayers(activity, new ResultMatcher<TileSourceTemplate>() {
						TileSourceTemplate template = null;
						int count = 0;
						@Override
						public boolean publish(TileSourceTemplate object) {
							if(object == null){
								if(count == 1){
									mapPref.set(template.getName());
									layers.getMapControlsLayer().showTransparencyBar(transparencyPref);
									updateMapLayers(mapView, mapPref, layers);
								} else {
									selectMapOverlayLayer(mapView, mapPref, transparencyPref, activity);
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
					layers.getMapControlsLayer().showTransparencyBar(transparencyPref);
					updateMapLayers(mapView, mapPref, layers);
				}
				dialog.dismiss();
			}
			
		});
		builder.show();
	}
	
	@Override
	public void registerLayerContextMenuActions(final OsmandMapTileView mapView, ContextMenuAdapter adapter, final MapActivity mapActivity) {
		final MapActivityLayers layers = mapActivity.getMapLayers();
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				if (itemId == R.string.layer_map) {
					dialog.dismiss();
					layers.selectMapLayer(mapView);
				} else if(itemId == R.string.layer_overlay){
					if(overlayLayer.getMap() != null){
						settings.MAP_OVERLAY.set(null);
						updateMapLayers(mapView, null, layers);
						layers.getMapControlsLayer().hideTransparencyBar(settings.MAP_OVERLAY_TRANSPARENCY);
					} else {
						dialog.dismiss();
						selectMapOverlayLayer(mapView, settings.MAP_OVERLAY, settings.MAP_OVERLAY_TRANSPARENCY, mapActivity);
					}
				} else if(itemId == R.string.layer_underlay){
					if(underlayLayer.getMap() != null){
						settings.MAP_UNDERLAY.set(null);
						updateMapLayers(mapView, null, layers);
						layers.getMapControlsLayer().hideTransparencyBar(settings.MAP_TRANSPARENCY);
					} else {
						dialog.dismiss();
						selectMapOverlayLayer(mapView, settings.MAP_UNDERLAY,settings.MAP_TRANSPARENCY,
								mapActivity);
					}
				}
			}
		};
		adapter.item(R.string.layer_map).icons(R.drawable.ic_action_globus_dark, R.drawable.ic_action_globus_light)
				.listen(listener).position(0).reg();
		adapter.item(R.string.layer_overlay).selected(overlayLayer.getMap() != null ? 1 : 0).
				icons(R.drawable.ic_action_up_dark, R.drawable.ic_action_up_light).listen(listener).position(10).reg();
		adapter.item(R.string.layer_underlay).selected(underlayLayer.getMap() != null ? 1 : 0) 
				.icons(R.drawable.ic_action_down_dark, R.drawable.ic_action_down_light).listen(listener).position(11).reg();
	}
	
	
	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity, final double latitude, final double longitude, ContextMenuAdapter adapter,
			Object selectedObj) {
		final OsmandMapTileView mapView = mapActivity.getMapView();
		if (mapView.getMainLayer() instanceof MapTileLayer) {
			OnContextMenuClick listener = new OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int resId, int pos, boolean isChecked, DialogInterface dialog) {
					if (resId == R.string.context_menu_item_update_map) {
						mapActivity.getMapActions().reloadTile(mapView.getZoom(), latitude, longitude);
					} else if (resId == R.string.context_menu_item_download_map) {
						DownloadTilesDialog dlg = new DownloadTilesDialog(mapActivity, (OsmandApplication) mapActivity.getApplication(), mapView);
						dlg.openDialog();
					}
				}
			};
			adapter.item(R.string.context_menu_item_update_map).icons(R.drawable.ic_action_refresh_dark, R.drawable.ic_action_refresh_light)
					.listen(listener).reg();
			adapter.item(R.string.context_menu_item_download_map).icons(R.drawable.ic_action_gdown_dark, R.drawable.ic_action_gdown_light)
					.listen(listener).reg();
		}
	}
	

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		Preference grp = new Preference(activity);
		grp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				activity.startActivity(new Intent(activity, SettingsRasterMapsActivity.class));
				return true;
			}
		});
		grp.setSummary(R.string.online_map_settings_descr);
		grp.setTitle(R.string.online_map_settings);
		grp.setKey("map_settings");
		screen.addPreference(grp);
		
	}

	
	
	public static void installMapLayers(final Activity activity, final ResultMatcher<TileSourceTemplate> result) {
		final OsmandApplication app = (OsmandApplication) activity.getApplication();
		final OsmandSettings settings = app.getSettings();
		final Map<String, String> entriesMap = settings.getTileSourceEntries();
		if (!settings.isInternetConnectionAvailable(true)) {
			AccessibleToast.makeText(activity, R.string.internet_not_available, Toast.LENGTH_LONG).show();
			return;
		}
		AsyncTask<Void, Void, List<TileSourceTemplate>> t = new AsyncTask<Void, Void, List<TileSourceTemplate>>() {
			@Override
			protected List<TileSourceTemplate> doInBackground(Void... params) {
				return TileSourceManager.downloadTileSourceTemplates(Version.getVersionAsURLParam(app));
			}
			protected void onPostExecute(final java.util.List<TileSourceTemplate> downloaded) {
				if (downloaded == null || downloaded.isEmpty()) {
					AccessibleToast.makeText(activity, R.string.error_io_error, Toast.LENGTH_SHORT).show();
					return;
				}
				Builder builder = new AlertDialog.Builder(activity);
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
							AccessibleToast.makeText(activity, R.string.tile_source_already_installed, Toast.LENGTH_SHORT).show();
						}
					}
				});
				builder.setNegativeButton(R.string.default_buttons_cancel, null);
				builder.setTitle(R.string.select_tile_source_to_install);
				builder.setPositiveButton(R.string.default_buttons_apply, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						List<TileSourceTemplate> toInstall = new ArrayList<TileSourceTemplate>();
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
		t.execute(new Void[0]);
	}
}
