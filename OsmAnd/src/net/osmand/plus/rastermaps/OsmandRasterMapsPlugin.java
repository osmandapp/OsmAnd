package net.osmand.plus.rastermaps;

import java.util.Map;

import net.osmand.ResultMatcher;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.SeekBarPreference;
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
		return app.getString(R.string.osmand_rastermaps_plugin_name);
	}
	@Override
	public void registerLayers(MapActivity activity) {
	}
	
	@Override
	public void settingsActivityUpdate(SettingsActivity activity) {
		updateTileSourceSummary();
	}

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		PreferenceGroup general = (PreferenceGroup) screen.findPreference("global_settings");

		PreferenceGroup grp = screen.getPreferenceManager().createPreferenceScreen(activity);
		grp.setSummary(R.string.online_map_settings_descr);
		grp.setTitle(R.string.online_map_settings);
		grp.setKey("map_settings");
		grp.setOrder(0);
		general.addPreference(grp);
		

		OnPreferenceChangeListener listener = createPreferenceListener(activity);
		
		PreferenceCategory cat = new PreferenceCategory(activity);
		cat.setTitle(R.string.pref_raster_map);
		grp.addPreference(cat);
		
		CheckBoxPreference mapVectorData = activity.createCheckBoxPreference(settings.MAP_VECTOR_DATA, 
				R.string.map_vector_data, R.string.map_vector_data_descr);
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
