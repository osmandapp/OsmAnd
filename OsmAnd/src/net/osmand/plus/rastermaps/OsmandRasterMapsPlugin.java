package net.osmand.plus.rastermaps;

import java.util.ArrayList;
import java.util.Map;

import net.osmand.ResultMatcher;
import net.osmand.StateChangedListener;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.DownloadTilesDialog;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.SeekBarPreference;
import net.osmand.util.Algorithms;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

public class OsmandRasterMapsPlugin extends OsmandPlugin {
	private static final String ID = "osmand.rastermaps";
	private OsmandSettings settings;
	private OsmandApplication app;
	private ListPreference tileSourcePreference;
	private ListPreference overlayPreference;
	private ListPreference underlayPreference;
	
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
					SettingsActivity.installMapLayers(activity, new ResultMatcher<TileSourceTemplate>() {
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
		adapter.registerSelectedItem(R.string.layer_map, -1, R.drawable.list_activities_globus, listener, 0);
		adapter.registerSelectedItem(R.string.layer_overlay, overlayLayer.getMap() != null ? 1 : 0, 
				R.drawable.list_activities_overlay_map, listener, -1);
		adapter.registerSelectedItem(R.string.layer_underlay, underlayLayer.getMap() != null ? 1 : 0, 
				R.drawable.list_activities_underlay_map, listener, -1);
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
			adapter.registerItem(R.string.context_menu_item_update_map, R.drawable.list_activities_update_map, listener, -1);
			adapter.registerItem(R.string.context_menu_item_download_map, R.drawable.list_activities_download_map, listener, -1);
		}
	}
	
	@Override
	public void settingsActivityUpdate(SettingsActivity activity) {
		updateTileSourceSummary();
	}

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		PreferenceGroup grp = screen.getPreferenceManager().createPreferenceScreen(activity);
		grp.setSummary(R.string.online_map_settings_descr);
		grp.setTitle(R.string.online_map_settings);
		grp.setKey("map_settings");
		screen.addPreference(grp);
		

		OnPreferenceChangeListener listener = createPreferenceListener(activity);
		
		PreferenceCategory cat = new PreferenceCategory(activity);
		cat.setTitle(R.string.pref_raster_map);
		grp.addPreference(cat);
		
		CheckBoxPreference mapVectorData = activity.createCheckBoxPreference(settings.MAP_ONLINE_DATA,
				R.string.map_online_data, R.string.map_online_data_descr);
//		final OnPreferenceChangeListener parent = mapVectorData.getOnPreferenceChangeListener();
//		MapRenderRepositories r = app.getResourceManager().getRenderer();
//		if(r.isEmpty()){
//			AccessibleToast.makeText(this, app.getString(R.string.no_vector_map_loaded), Toast.LENGTH_LONG).show();
//			return false;
//		}
		cat.addPreference(mapVectorData);
		
		tileSourcePreference = new ListPreference(activity);
		tileSourcePreference.setSummary(R.string.map_tile_source_descr);
		tileSourcePreference.setTitle(R.string.map_tile_source);
		tileSourcePreference.setOnPreferenceChangeListener(listener);
		cat.addPreference(tileSourcePreference);
		
		cat.addPreference(activity.createCheckBoxPreference(settings.USE_INTERNET_TO_DOWNLOAD_TILES, 
				R.string.use_internet, R.string.use_internet_to_download_tile));
		
		int startZoom = 1;
		int endZoom = 18;
		String[] entries = new String[endZoom - startZoom + 1];
		Integer[] intValues = new Integer[endZoom - startZoom + 1];
		for (int i = startZoom; i <= endZoom; i++) {
			entries[i - startZoom] = i + ""; //$NON-NLS-1$
			intValues[i - startZoom] = i ;
		}
		ListPreference lp = activity.createListPreference(settings.LEVEL_TO_SWITCH_VECTOR_RASTER, 
				entries, intValues, R.string.level_to_switch_vector_raster, R.string.level_to_switch_vector_raster_descr);
		cat.addPreference(lp);
		
		// try without, Issue 823:
//		int startZoom = 12;
//		int endZoom = 19;
//		entries = new String[endZoom - startZoom + 1];
//		Integer[] intValues = new Integer[endZoom - startZoom + 1];
//		for (int i = startZoom; i <= endZoom; i++) {
//			entries[i - startZoom] = i + ""; //$NON-NLS-1$
//			intValues[i - startZoom] = i ;
//		}
		// registerListPreference(osmandSettings.MAX_LEVEL_TO_DOWNLOAD_TILE, screen, entries, intValues);
		
		
		cat = new PreferenceCategory(activity);
		cat.setTitle(R.string.pref_overlay);
		grp.addPreference(cat);
		
		overlayPreference = new ListPreference(activity);
		overlayPreference.setSummary(R.string.map_overlay_descr);
		overlayPreference.setTitle(R.string.map_overlay);
		overlayPreference.setOnPreferenceChangeListener(listener);
		cat.addPreference(overlayPreference);
		underlayPreference = new ListPreference(activity);
		underlayPreference.setSummary(R.string.map_underlay_descr);
		underlayPreference.setTitle(R.string.map_underlay);
		underlayPreference.setOnPreferenceChangeListener(listener);
		cat.addPreference(underlayPreference);
		
		SeekBarPreference sp = activity.createSeekBarPreference(settings.MAP_OVERLAY_TRANSPARENCY, R.string.overlay_transparency, R.string.overlay_transparency_descr,
				R.string.modify_transparency, 0, 255);
		cat.addPreference(sp);
		sp = activity.createSeekBarPreference(settings.MAP_TRANSPARENCY, R.string.map_transparency, R.string.map_transparency_descr,
				R.string.modify_transparency, 0, 255);
		cat.addPreference(sp);
	}

	private OnPreferenceChangeListener createPreferenceListener(final SettingsActivity activity) {
		return new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (preference == tileSourcePreference  || preference == overlayPreference 
						|| preference == underlayPreference) {
					if(SettingsActivity.MORE_VALUE.equals(newValue)){
						SettingsActivity.installMapLayers(activity, new ResultMatcher<TileSourceTemplate>() {
							@Override
							public boolean isCancelled() { return false;}

							@Override
							public boolean publish(TileSourceTemplate object) {
								if(object == null){
									updateTileSourceSummary();
								}
								return true;
							}
						});
					} else if(preference == tileSourcePreference){
						settings.MAP_TILE_SOURCES.set((String) newValue);
						updateTileSourceSummary();
					} else {
						if(((String) newValue).length() == 0){
							newValue = null;
						}
						if(preference == underlayPreference){
							settings.MAP_UNDERLAY.set(((String) newValue));
							underlayPreference.setSummary(app.getString(R.string.map_underlay_descr) + "  [" + settings.MAP_UNDERLAY.get() + "]");
						} else if(preference == overlayPreference){
							settings.MAP_OVERLAY.set(((String) newValue));
							overlayPreference.setSummary(app.getString(R.string.map_overlay_descr) + "  [" + settings.MAP_OVERLAY.get() + "]");
						}
					}
				}
				return true;
			}
		};
	}
	
	private void updateTileSourceSummary() {
		if (tileSourcePreference != null) {
			fillTileSourcesToPreference(tileSourcePreference, settings.MAP_TILE_SOURCES.get(), false);
			fillTileSourcesToPreference(overlayPreference, settings.MAP_OVERLAY.get(), true);
			fillTileSourcesToPreference(underlayPreference, settings.MAP_UNDERLAY.get(), true);

			//		String mapName = " " + settings.MAP_TILE_SOURCES.get(); //$NON-NLS-1$
			// String summary = tileSourcePreference.getSummary().toString();
			// if (summary.lastIndexOf(':') != -1) {
			// summary = summary.substring(0, summary.lastIndexOf(':') + 1);
			// }
			// tileSourcePreference.setSummary(summary + mapName);
			tileSourcePreference.setSummary(app.getString(R.string.map_tile_source_descr) + "  [" + settings.MAP_TILE_SOURCES.get() + "]");
			overlayPreference.setSummary(app.getString(R.string.map_overlay_descr) + "  [" + settings.MAP_OVERLAY.get() + "]");
			underlayPreference.setSummary(app.getString(R.string.map_underlay_descr) + "  [" + settings.MAP_UNDERLAY.get() + "]");
		}
	}
	
	private void fillTileSourcesToPreference(ListPreference tileSourcePreference, String value, boolean addNone) {
		Map<String, String> entriesMap = settings.getTileSourceEntries();
		int add = addNone ? 1 : 0;
		String[] entries = new String[entriesMap.size() + 1 + add];
		String[] values = new String[entriesMap.size() + 1 + add];
		int ki = 0;
		if (addNone) {
			entries[ki] = app.getString(R.string.default_none);
			values[ki] = "";
			ki++;
		}
		if (value == null) {
			value = "";
		}

		for (Map.Entry<String, String> es : entriesMap.entrySet()) {
			entries[ki] = es.getValue();
			values[ki] = es.getKey();
			ki++;
		}
		entries[ki] = app.getString(R.string.install_more);
		values[ki] = SettingsActivity.MORE_VALUE;
		fill(tileSourcePreference, entries, values, value);
	}
	
	private void fill(ListPreference component, String[] list, String[] values, String selected) {
		component.setEntries(list);
		component.setEntryValues(values);
		component.setValue(selected);
	}
}
