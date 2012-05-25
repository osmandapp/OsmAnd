package net.osmand.plus.development;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class OsmandDevelopmentPlugin extends OsmandPlugin {
	private static final String ID = "osmand.development";
	private OsmandSettings settings;
	private OsmandApplication app;
	
	public OsmandDevelopmentPlugin(OsmandApplication app) {
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
		return app.getString(R.string.osmand_development_plugin_description);
	}
	@Override
	public String getName() {
		return app.getString(R.string.osmand_development_plugin_name);
	}
	@Override
	public void registerLayers(MapActivity activity) {
	}
	

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		PreferenceScreen general = (PreferenceScreen) screen.findPreference(SettingsActivity.SCREEN_ID_GENERAL_SETTINGS);
		
		PreferenceCategory cat = new PreferenceCategory(app);
		cat.setTitle(R.string.debugging_and_development);
		general.addPreference(cat);
		
		CheckBoxPreference dbg = activity.createCheckBoxPreference(settings.DEBUG_RENDERING_INFO, 
				R.string.trace_rendering, R.string.trace_rendering_descr);
		cat.addPreference(dbg);
		CheckBoxPreference animate = activity.createCheckBoxPreference(settings.TEST_ANIMATE_ROUTING, 
				R.string.animate_routing, R.string.simulate_route_progression_manually);
		cat.addPreference(animate);
		
		Preference pref = new Preference(app);
		pref.setTitle(R.string.test_voice_prompts);
		pref.setSummary(R.string.play_commands_of_currently_selected_voice);
		pref.setKey("test_voice_commands");
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				activity.startActivity(new Intent(activity, TestVoiceActivity.class));
				return true;
			}
		});
		cat.addPreference(pref);
	}
}
