package net.osmand.plus.extrasettings;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import android.media.AudioManager;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class OsmandExtraSettings extends OsmandPlugin {
	private static final String ID = "osmand.extrasettings";
	private OsmandSettings settings;
	private OsmandApplication app;
	
	public OsmandExtraSettings(OsmandApplication app) {
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
		return app.getString(R.string.osmand_extra_settings_description);
	}
	@Override
	public String getName() {
		return app.getString(R.string.osmand_extra_settings_name);
	}
	@Override
	public void registerLayers(MapActivity activity) {
	}
	

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		PreferenceScreen general = (PreferenceScreen) screen.findPreference(SettingsActivity.SCREEN_ID_GENERAL_SETTINGS);
		
		PreferenceCategory cat = new PreferenceCategory(app);
		cat.setTitle(R.string.extra_settings);
		general.addPreference(cat);
		
		cat.addPreference(activity.createCheckBoxPreference(settings.USE_HIGH_RES_MAPS, 
				R.string.use_high_res_maps, R.string.use_high_res_maps_descr));
		cat.addPreference(activity.createCheckBoxPreference(settings.USE_TRACKBALL_FOR_MOVEMENTS, 
				R.string.use_trackball, R.string.use_trackball_descr));
		
		ListPreference lp = activity.createListPreference(settings.AUDIO_STREAM_GUIDANCE, 
				new String[] {app.getString(R.string.voice_stream_music), app.getString(R.string.voice_stream_notification),
				app.getString(R.string.voice_stream_voice_call)},
				new Integer[] {AudioManager.STREAM_MUSIC, AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_VOICE_CALL},
				R.string.choose_audio_stream, R.string.choose_audio_stream_descr);
		cat.addPreference(lp);
	}
}
