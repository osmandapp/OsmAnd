package net.osmand.plus.extrasettings;


import java.util.Arrays;

import net.osmand.Version;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.MapInfoControls;
import net.osmand.plus.views.MapInfoControls.MapInfoControlRegInfo;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.voice.CommandPlayer;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
	public void registerLayers(final MapActivity activity) {
		registerControls = true;
		final OsmandMapTileView view = activity.getMapView();
		final MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		final MapInfoControls mapInfoControls = mapInfoLayer.getMapInfoControls();
		
		final OsmandPreference<Float> textSizePref = view.getSettings().MAP_TEXT_SIZE;
		final MapInfoControlRegInfo textSize = mapInfoControls.registerAppearanceWidget(R.drawable.widget_text_size, R.string.map_text_size, 
				"text_size", textSizePref);
		textSize.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				final Float[] floatValues = new Float[] {0.6f, 0.8f, 1.0f, 1.2f, 1.5f, 1.75f, 2f};
				String[] entries = new String[floatValues.length];
				for (int i = 0; i < floatValues.length; i++) {
					entries[i] = (int) (floatValues[i] * 100) +" %";
				}
				Builder b = new AlertDialog.Builder(view.getContext());
				b.setTitle(R.string.map_text_size);
				int i = Arrays.binarySearch(floatValues, textSizePref.get());
				b.setSingleChoiceItems(entries, i, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						textSizePref.set(floatValues[which]);
						app.getResourceManager().getRenderer().clearCache();
						view.refreshMap(true);
						dialog.dismiss();
					}
				});
				b.show();
			}
		});
		final MapInfoControlRegInfo showRuler = mapInfoControls.registerAppearanceWidget(R.drawable.widget_ruler, R.string.map_widget_show_ruler, 
				"showRuler", view.getSettings().SHOW_RULER);
		showRuler.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				view.getSettings().SHOW_RULER.set(!view.getSettings().SHOW_RULER.get());
				view.refreshMap();
			}
		});
		
		final MapInfoControlRegInfo transparent = mapInfoControls.registerAppearanceWidget(R.drawable.widget_transparent_skin, R.string.map_widget_transparent,
				"transparent", view.getSettings().TRANSPARENT_MAP_THEME);
		transparent.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				view.getSettings().TRANSPARENT_MAP_THEME.set(!view.getSettings().TRANSPARENT_MAP_THEME.get());
				mapInfoLayer.recreateControls();
			}
		});
		
		final MapInfoControlRegInfo showDestinationArrow = mapInfoControls.registerAppearanceWidget(0, R.string.map_widget_show_destionation_arrow,
				"show_destination_arrow", view.getSettings().SHOW_DESTINATION_ARROW);
		showDestinationArrow.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				view.getSettings().SHOW_DESTINATION_ARROW.set(!view.getSettings().SHOW_DESTINATION_ARROW.get());
				mapInfoLayer.recreateControls();
			}
		});

		// FIXME delete strings from this code
//		final MapInfoControlRegInfo fluorescent = mapInfoControls.registerAppearanceWidget(R.drawable.widget_fluorescent_routes, R.string.map_widget_fluorescent,
//				"fluorescent", view.getSettings().FLUORESCENT_OVERLAYS);
//		fluorescent.setStateChangeListener(new Runnable() {
//			@Override
//			public void run() {
//				view.getSettings().FLUORESCENT_OVERLAYS.set(!view.getSettings().FLUORESCENT_OVERLAYS.get());
//				view.refreshMap();
//			}
//		});
		

//		final CommonPreference<Integer> posPref = view.getSettings().POSITION_ON_MAP;
//		final MapInfoControlRegInfo posMap = mapInfoControls.registerAppearanceWidget(R.drawable.widget_position_marker, R.string.position_on_map, 
//				"position_on_map", textSizePref);
//		posMap.setStateChangeListener(new Runnable() {
//			@Override
//			public void run() {
//				String[]  entries = new String[] { activity.getString(R.string.position_on_map_center), activity.getString(R.string.position_on_map_bottom) };
//				final Integer[] vals = new Integer[] { OsmandSettings.CENTER_CONSTANT, OsmandSettings.BOTTOM_CONSTANT };
//				Builder b = new AlertDialog.Builder(view.getContext());
//				int i = Arrays.binarySearch(vals, posPref.get());
//				b.setSingleChoiceItems(entries, i, new OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						posPref.set(vals[which]);
//						activity.updateApplicationModeSettings();
//						view.refreshMap(true);
//						dialog.dismiss();
//					}
//				});
//				b.show();
//			}
//		});
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
		
		
//		cat = new PreferenceCategory(app);
//		cat.setTitle(R.string.extra_settings);
//		PreferenceScreen routing = (PreferenceScreen) screen.findPreference(SettingsActivity.SCREEN_ID_NAVIGATION_SETTINGS);		
//		routing.addPreference(cat);
//		cat.addPreference(activity.createListPreference(settings.POSITION_ON_MAP,
//				new String[] { activity.getString(R.string.position_on_map_center), activity.getString(R.string.position_on_map_bottom) },
//				new Integer[] { OsmandSettings.CENTER_CONSTANT, OsmandSettings.BOTTOM_CONSTANT }, R.string.position_on_map,
//				R.string.position_on_map_descr));	
	}
}
