package net.osmand.plus.extrasettings;


import net.osmand.Version;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.MapInfoControls;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.MapInfoControls.MapInfoControlRegInfo;
import net.osmand.plus.voice.CommandPlayer;
import android.media.AudioManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class OsmandExtraSettings extends OsmandPlugin {
	private static final String ID = "osmand.extrasettings";
	private OsmandSettings settings;
	private OsmandApplication app;
	private boolean registerControls;
	
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
		return app.getString(R.string.extra_settings);
	}
	@Override
	public void registerLayers(MapActivity activity) {
		registerControls = true;
		final OsmandMapTileView view = activity.getMapView();
		final MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		final MapInfoControls mapInfoControls = mapInfoLayer.getMapInfoControls();
		final MapInfoControlRegInfo transparent = mapInfoControls.registerAppearanceWidget(0, R.string.map_widget_transparent,
				"transparent", view.getSettings().TRANSPARENT_MAP_THEME);
		transparent.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				ApplicationMode am = view.getSettings().getApplicationMode();
				view.getSettings().TRANSPARENT_MAP_THEME.set(!view.getSettings().TRANSPARENT_MAP_THEME.get());
				mapInfoLayer.recreateControls();
			}
		});

		final MapInfoControlRegInfo fluorescent = mapInfoControls.registerAppearanceWidget(0, R.string.map_widget_fluorescent,
				"fluorescent", view.getSettings().FLUORESCENT_OVERLAYS);
		fluorescent.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				ApplicationMode am = view.getSettings().getApplicationMode();
				view.getSettings().FLUORESCENT_OVERLAYS.set(!view.getSettings().FLUORESCENT_OVERLAYS.get());
				view.refreshMap();
			}
		});
	}
	
	@Override
	public void updateLayers(final OsmandMapTileView view, MapActivity activity) {
		if(!registerControls) {
			registerLayers(activity);
		}
	}
	

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		PreferenceScreen general = (PreferenceScreen) screen.findPreference(SettingsActivity.SCREEN_ID_GENERAL_SETTINGS);
		
		PreferenceCategory cat = new PreferenceCategory(app);
		cat.setTitle(R.string.extra_settings);
		general.addPreference(cat);
		
		cat.addPreference(activity.createCheckBoxPreference(settings.USE_HIGH_RES_MAPS, 
				R.string.use_high_res_maps, R.string.use_high_res_maps_descr));
		
		if (!Version.isBlackberry(activity)) {
			cat.addPreference(activity.createCheckBoxPreference(settings.USE_TRACKBALL_FOR_MOVEMENTS, 
					R.string.use_trackball, R.string.use_trackball_descr));
			
			ListPreference lp = activity.createListPreference(
					settings.AUDIO_STREAM_GUIDANCE,
					new String[] { app.getString(R.string.voice_stream_music), app.getString(R.string.voice_stream_notification),
							app.getString(R.string.voice_stream_voice_call) }, new Integer[] { AudioManager.STREAM_MUSIC,
							AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_VOICE_CALL }, R.string.choose_audio_stream,
					R.string.choose_audio_stream_descr);
			final OnPreferenceChangeListener prev = lp.getOnPreferenceChangeListener();
			lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					prev.onPreferenceChange(preference, newValue);
					CommandPlayer player = app.getPlayer();
					if (player != null) {
						player.updateAudioStream(settings.AUDIO_STREAM_GUIDANCE.get());
					}
					return true;
				}
			});
			cat.addPreference(lp);
		}
		
		
		PreferenceScreen appearance = (PreferenceScreen) screen.findPreference("appearance_settings");
		PreferenceCategory vectorSettings = new PreferenceCategory(app);
		vectorSettings.setTitle(R.string.pref_vector_rendering);
		vectorSettings.setKey("custom_vector_rendering");
		appearance.addPreference(vectorSettings);

	}
}
