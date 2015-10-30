package net.osmand.plus.rastermaps;


import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import net.osmand.ResultMatcher;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.views.SeekBarPreference;

import java.util.Map;

public class SettingsRasterMapsActivity extends SettingsBaseActivity {

	private ListPreference tileSourcePreference;
	private ListPreference overlayPreference;
	private ListPreference underlayPreference;
	public static final String MORE_VALUE = "MORE_VALUE";
	public static final String DEFINE_EDIT = "DEFINE_EDIT";


	@Override
    public void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getToolbar().setTitle(R.string.shared_string_online_maps);
		PreferenceScreen grp = getPreferenceScreen();
		OnPreferenceChangeListener listener = createPreferenceListener();
		
		PreferenceCategory cat = new PreferenceCategory(this);
		cat.setTitle(R.string.pref_raster_map);
		grp.addPreference(cat);
	
		// present on configure map
//		addTileSourcePrefs(listener, cat);
		
		cat.addPreference(createCheckBoxPreference(settings.USE_INTERNET_TO_DOWNLOAD_TILES, 
				R.string.use_internet, R.string.use_internet_to_download_tile));
		
//		int startZoom = 1;
//		int endZoom = 18;
//		String[] entries = new String[endZoom - startZoom + 1];
//		Integer[] intValues = new Integer[endZoom - startZoom + 1];
//		for (int i = startZoom; i <= endZoom; i++) {
//			entries[i - startZoom] = i + ""; //$NON-NLS-1$
//			intValues[i - startZoom] = i ;
//		}
//		ListPreference lp = createListPreference(settings.LEVEL_TO_SWITCH_VECTOR_RASTER, 
//				entries, intValues, R.string.level_to_switch_vector_raster, R.string.level_to_switch_vector_raster_descr);
//		cat.addPreference(lp);

		// present on configure map
//		addOverlayPrefs(grp, listener);
		
    }


	@SuppressWarnings("unused")
	private void addOverlayPrefs(PreferenceScreen grp, OnPreferenceChangeListener listener) {
		PreferenceCategory cat;
		cat = new PreferenceCategory(this);
		cat.setTitle(R.string.pref_overlay);
		grp.addPreference(cat);
		
		overlayPreference = new ListPreference(this);
		overlayPreference.setSummary(R.string.map_overlay_descr);
		overlayPreference.setTitle(R.string.map_overlay);
		overlayPreference.setOnPreferenceChangeListener(listener);
		cat.addPreference(overlayPreference);
		underlayPreference = new ListPreference(this);
		underlayPreference.setSummary(R.string.map_underlay_descr);
		underlayPreference.setTitle(R.string.map_underlay);
		underlayPreference.setOnPreferenceChangeListener(listener);
		cat.addPreference(underlayPreference);
		
		SeekBarPreference sp = createSeekBarPreference(settings.MAP_OVERLAY_TRANSPARENCY, R.string.overlay_transparency, R.string.overlay_transparency_descr,
				R.string.modify_transparency, 0, 255);
		cat.addPreference(sp);
		sp = createSeekBarPreference(settings.MAP_TRANSPARENCY, R.string.map_transparency, R.string.map_transparency_descr,
				R.string.modify_transparency, 0, 255);
		cat.addPreference(sp);
	}


	@SuppressWarnings("unused")
	private void addTileSourcePrefs(OnPreferenceChangeListener listener, PreferenceCategory cat) {
		CheckBoxPreference mapVectorData = createCheckBoxPreference(settings.MAP_ONLINE_DATA,
				R.string.map_online_data, R.string.map_online_data_descr);
		cat.addPreference(mapVectorData);
		
		tileSourcePreference = new ListPreference(this);
		tileSourcePreference.setSummary(R.string.map_tile_source_descr);
		tileSourcePreference.setTitle(R.string.map_tile_source);
		tileSourcePreference.setOnPreferenceChangeListener(listener);
		cat.addPreference(tileSourcePreference);
	}


	public void updateAllSettings() {
		super.updateAllSettings();
		updateTileSourceSummary();
	}
	
	private void updateTileSourceSummary() {
		if (tileSourcePreference != null) {
			fillTileSourcesToPreference(tileSourcePreference, settings.MAP_TILE_SOURCES.get(), false);
			fillTileSourcesToPreference(overlayPreference, settings.MAP_OVERLAY.get(), true);
			fillTileSourcesToPreference(underlayPreference, settings.MAP_UNDERLAY.get(), true);

			//		String mapName = " " + osmandSettings.MAP_TILE_SOURCES.get(); //$NON-NLS-1$
			// String summary = tileSourcePreference.getSummary().toString();
			// if (summary.lastIndexOf(':') != -1) {
			// summary = summary.substring(0, summary.lastIndexOf(':') + 1);
			// }
			// tileSourcePreference.setSummary(summary + mapName);
			;
			tileSourcePreference.setSummary(format(R.string.map_tile_source_descr, settings.MAP_TILE_SOURCES.get()));
			overlayPreference.setSummary(format(R.string.map_overlay_descr, settings.MAP_OVERLAY.get()) );
			underlayPreference.setSummary(format(R.string.map_underlay_descr, settings.MAP_UNDERLAY.get()) );
		}
	}
	
	private String format(int r, String string) {
		return getString(r) + " [" + (string == null ? "" : string) + "]";
	}


	private void fillTileSourcesToPreference(ListPreference tileSourcePreference, String value, boolean addNone) {
		Map<String, String> entriesMap = settings.getTileSourceEntries();
		int add = addNone ? 1 : 0;
		String[] entries = new String[entriesMap.size() + 2 + add];
		String[] values = new String[entriesMap.size() + 2 + add];
		int ki = 0;
		if (addNone) {
			entries[ki] = getString(R.string.shared_string_none);
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
		entries[ki] = getMyApplication().getString(R.string.install_more);
		values[ki] = MORE_VALUE;
		ki++;
		entries[ki] = getMyApplication().getString(R.string.maps_define_edit);
		values[ki] = DEFINE_EDIT;
		fill(tileSourcePreference, entries, values, value);
	}
	
	private void fill(ListPreference component, String[] list, String[] values, String selected) {
		component.setEntries(list);
		component.setEntryValues(values);
		component.setValue(selected);
	}
	
	private OnPreferenceChangeListener createPreferenceListener() {
		return new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (preference == tileSourcePreference || preference == overlayPreference 
						|| preference == underlayPreference) {
					if(MORE_VALUE.equals(newValue)){
						OsmandRasterMapsPlugin.installMapLayers(SettingsRasterMapsActivity.this, new ResultMatcher<TileSourceTemplate>() {
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
					} else if(DEFINE_EDIT.equals(newValue)){
						OsmandRasterMapsPlugin.defineNewEditLayer(SettingsRasterMapsActivity.this, new ResultMatcher<TileSourceTemplate>() {
							@Override
							public boolean isCancelled() { return false;}

							@Override
							public boolean publish(TileSourceTemplate object) {
								updateTileSourceSummary();
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
							underlayPreference.setSummary(getString(R.string.map_underlay_descr) + "  [" + settings.MAP_UNDERLAY.get() + "]");
						} else if(preference == overlayPreference){
							settings.MAP_OVERLAY.set(((String) newValue));
							overlayPreference.setSummary(getString(R.string.map_overlay_descr) + "  [" + settings.MAP_OVERLAY.get() + "]");
						}
					}
				}
				return true;
			}
		};
	}

}
