package net.osmand.plus.osmedit;

import android.content.DialogInterface;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.InputType;
import net.osmand.data.Amenity;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.OsmandMapTileView;

public class OsmEditingPlugin extends OsmandPlugin {
	private static final String ID = "osm.editing";
	private OsmandSettings settings;
	private OsmandApplication app;
	
	@Override
	public String getId() {
		return ID;
	}
	
	public OsmEditingPlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public boolean init(OsmandApplication app) {
		settings = app.getSettings();
		return true;
	}
	
	private OsmBugsLayer osmBugsLayer;
	private EditingPOIActivity poiActions;
	
	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity){
		if (osmBugsLayer == null) {
			registerLayers(activity);
		}
		if(mapView.getLayers().contains(osmBugsLayer) != settings.SHOW_OSM_BUGS.get()){
			if(settings.SHOW_OSM_BUGS.get()){
				mapView.addLayer(osmBugsLayer, 2);
			} else {
				mapView.removeLayer(osmBugsLayer);
			}
		}
	}
	
	@Override
	public void registerLayers(MapActivity activity){
		osmBugsLayer = new OsmBugsLayer(activity);
	}
	
	public OsmBugsLayer getBugsLayer(MapActivity activity) {
		if(osmBugsLayer == null) {
			registerLayers(activity);
		}
		return osmBugsLayer;
	}

	@Override
	public void mapActivityCreate(MapActivity activity) {
		activity.addDialogProvider(getPoiActions(activity));
		activity.addDialogProvider(getBugsLayer(activity));
	}
	
	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		PreferenceScreen general = (PreferenceScreen) screen.findPreference(SettingsActivity.SCREEN_ID_GENERAL_SETTINGS);
		PreferenceCategory cat = new PreferenceCategory(app);
		cat.setTitle(R.string.osm_settings);
		general.addPreference(cat);
		
		EditTextPreference userName = activity.createEditTextPreference(settings.USER_NAME, R.string.user_name, R.string.user_name_descr);
		cat.addPreference(userName);
		EditTextPreference pwd = activity.createEditTextPreference(settings.USER_PASSWORD, R.string.user_password, R.string.user_password_descr);
		pwd.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		cat.addPreference(pwd);
		
		CheckBoxPreference poiEdit = activity.createCheckBoxPreference(settings.OFFLINE_EDITION,
				R.string.offline_edition, R.string.offline_edition_descr);
		cat.addPreference(poiEdit);
		
		Preference pref = new Preference(app);
		pref.setTitle(R.string.local_openstreetmap_settings);
		pref.setSummary(R.string.local_openstreetmap_settings_descr);
		pref.setKey("local_openstreetmap_points");
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				activity.startActivity(new Intent(activity, LocalOpenstreetmapActivity.class));
				return true;
			}
		});
		cat.addPreference(pref);
	}
	
	public EditingPOIActivity getPoiActions(MapActivity activity) {
		if(poiActions == null) {
			poiActions = new EditingPOIActivity(activity);
		}
		return poiActions;
	}
	
	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity, final double latitude, final double longitude, ContextMenuAdapter adapter,
			final Object selectedObj) {
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int resId, int pos, boolean isChecked, DialogInterface dialog) {
				if (resId == R.string.context_menu_item_create_poi) {
					getPoiActions(mapActivity).showCreateDialog(latitude, longitude);
				} else if (resId == R.string.context_menu_item_open_bug) {
					if(osmBugsLayer == null) {
						registerLayers(mapActivity);
					}
					osmBugsLayer.openBug(latitude, longitude);
				} else if (resId == R.string.poi_context_menu_delete) {
					getPoiActions(mapActivity).showDeleteDialog((Amenity) selectedObj);
				} else if (resId == R.string.poi_context_menu_modify) {
					getPoiActions(mapActivity).showEditDialog((Amenity) selectedObj);
				}
			}
		};
		if(selectedObj instanceof Amenity) {
			adapter.registerItem(R.string.poi_context_menu_modify, R.drawable.list_activities_poi_modify, listener, 1);
			adapter.registerItem(R.string.poi_context_menu_delete, R.drawable.list_activities_poi_remove, listener, 2);
		} else {
			adapter.registerItem(R.string.context_menu_item_create_poi, R.drawable.list_view_create_poi, listener, -1);
		}
		adapter.registerItem(R.string.context_menu_item_open_bug, R.drawable.list_activities_osm_bugs, listener, -1);
	}

	@Override
	public void registerLayerContextMenuActions(OsmandMapTileView mapView, ContextMenuAdapter adapter, MapActivity mapActivity) {
		adapter.registerSelectedItem(R.string.layer_osm_bugs, settings.SHOW_OSM_BUGS.get() ? 1 : 0, R.drawable.list_activities_osm_bugs,
				new OnContextMenuClick() {

					@Override
					public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
						if (itemId == R.string.layer_osm_bugs) {
							settings.SHOW_OSM_BUGS.set(isChecked);
						}

					}
				}, 5);

	}

	@Override
	public String getDescription() {
		return app.getString(R.string.osm_editing_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.osm_settings);
	}

}
