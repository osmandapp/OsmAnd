package net.osmand.plus.development;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.os.Debug.MemoryInfo;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import net.osmand.plus.OsmAndLocationSimulation;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.util.SunriseSunset;

import java.text.SimpleDateFormat;

//import net.osmand.plus.development.OsmandDevelopmentPlugin;

public class SettingsDevelopmentActivity extends SettingsBaseActivity {

	@SuppressLint("SimpleDateFormat")
	@Override
    public void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getToolbar().setTitle(R.string.debugging_and_development);
		PreferenceScreen cat = getPreferenceScreen();
		Preference pref;

		cat.addPreference(createCheckBoxPreference(settings.USE_OPENGL_RENDER,
				R.string.use_opengl_render,R.string.use_opengl_render_descr));

		CheckBoxPreference nativeCheckbox = createCheckBoxPreference(settings.SAFE_MODE, R.string.safe_mode,
				R.string.safe_mode_description);
		// disable the checkbox if the library cannot be used
		if ((NativeOsmandLibrary.isLoaded() && !NativeOsmandLibrary.isSupported()) || settings.NATIVE_RENDERING_FAILED.get()) {
			nativeCheckbox.setEnabled(false);
			nativeCheckbox.setChecked(true);
		}
		cat.addPreference(nativeCheckbox);

		PreferenceCategory navigation = new PreferenceCategory(this);
		navigation.setTitle(R.string.routing_settings);
		cat.addPreference(navigation);
		navigation.addPreference(createCheckBoxPreference(settings.DISABLE_COMPLEX_ROUTING,
				R.string.disable_complex_routing, R.string.disable_complex_routing_descr));

		navigation.addPreference(createCheckBoxPreference(settings.USE_FAST_RECALCULATION,
				R.string.use_fast_recalculation, R.string.use_fast_recalculation_desc));

		navigation.addPreference(createCheckBoxPreference(settings.USE_OSM_LIVE_FOR_ROUTING,
				R.string.use_osm_live_routing,
				R.string.use_osm_live_routing_description));

		navigation.addPreference(createCheckBoxPreference(settings.USE_OSM_LIVE_FOR_PUBLIC_TRANSPORT,
				R.string.use_osm_live_public_transport,
				R.string.use_osm_live_public_transport_description));

		pref = new Preference(this);
		final Preference simulate = pref;
		final OsmAndLocationSimulation sim = getMyApplication().getLocationProvider().getLocationSimulation();
		final Runnable updateTitle = new Runnable(){

			@Override
			public void run() {
				simulate.setSummary(sim.isRouteAnimating() ?
						R.string.simulate_your_location_stop_descr : R.string.simulate_your_location_gpx_descr);
			}
		};
		pref.setTitle(R.string.simulate_your_location);
		updateTitle.run();
		pref.setKey("simulate_your_location");
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				updateTitle.run();
				sim.startStopRouteAnimation(SettingsDevelopmentActivity.this, true, updateTitle);
				return true;
			}
		});
		navigation.addPreference(pref);

		PreferenceCategory debug = new PreferenceCategory(this);
		debug.setTitle(R.string.debugging_and_development);
		cat.addPreference(debug);

		CheckBoxPreference dbg = createCheckBoxPreference(settings.DEBUG_RENDERING_INFO,
				R.string.trace_rendering, R.string.trace_rendering_descr);
		debug.addPreference(dbg);


		final Preference firstRunPreference = new Preference(this);
		firstRunPreference.setTitle(R.string.simulate_initial_startup);
		firstRunPreference.setSummary(R.string.simulate_initial_startup_descr);
		firstRunPreference.setSelectable(true);
		firstRunPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				getMyApplication().getAppInitializer().resetFirstTimeRun();
				OsmandSettings settings = getMyApplication().getSettings();
				settings.FIRST_MAP_IS_DOWNLOADED.set(false);
				settings.MAPILLARY_FIRST_DIALOG_SHOWN.set(false);
				settings.WEBGL_SUPPORTED.set(true);
				settings.METRIC_SYSTEM_CHANGED_MANUALLY.set(false);
				settings.WIKI_ARTICLE_SHOW_IMAGES_ASKED.set(false);

				getMyApplication().showToastMessage(R.string.shared_string_ok);
				return true;
			}
		});
		debug.addPreference(firstRunPreference);

		debug.addPreference(createCheckBoxPreference(settings.SHOULD_SHOW_FREE_VERSION_BANNER,
				R.string.show_free_version_banner,
				R.string.show_free_version_banner_description));
		

		


		pref = new Preference(this);
		pref.setTitle(R.string.test_voice_prompts);
		pref.setSummary(R.string.play_commands_of_currently_selected_voice);
		pref.setKey("test_voice_commands");
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(SettingsDevelopmentActivity.this, TestVoiceActivity.class));
				return true;
			}
		});
		cat.addPreference(pref);

		pref = new Preference(this);
		pref.setTitle(R.string.logcat_buffer);
		pref.setKey("logcat_buffer");
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(SettingsDevelopmentActivity.this, LogcatActivity.class));
				return true;
			}
		});
		cat.addPreference(pref);

		PreferenceCategory info = new PreferenceCategory(this);
		info.setTitle(R.string.info_button);
		cat.addPreference(info);

		pref = new Preference(this);
		pref.setTitle(R.string.global_app_allocated_memory);

		long javaAvailMem = (Runtime.getRuntime().totalMemory() -  Runtime.getRuntime().freeMemory())/ (1024*1024l);
		long javaTotal = Runtime.getRuntime().totalMemory() / (1024*1024l);
		long dalvikSize = android.os.Debug.getNativeHeapAllocatedSize() / (1024*1024l);
		pref.setSummary(getString(R.string.global_app_allocated_memory_descr, javaAvailMem, javaTotal, dalvikSize));
		pref.setSelectable(false);
		//setEnabled(false) creates bad readability on some devices
		//pref.setEnabled(false);
		info.addPreference(pref);

//		ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
//		ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
//		activityManager.getMemoryInfo(memoryInfo);
//		long totalSize = memoryInfo.availMem / (1024*1024l);
		MemoryInfo mem = new Debug.MemoryInfo();
		Debug.getMemoryInfo(mem);
		pref = new Preference(this);
		pref.setTitle(R.string.native_app_allocated_memory);
		pref.setSummary(getString(R.string.native_app_allocated_memory_descr
				, mem.nativePrivateDirty / 1024, mem.dalvikPrivateDirty / 1024 , mem.otherPrivateDirty / 1024
				, mem.nativePss / 1024, mem.dalvikPss / 1024 , mem.otherPss / 1024));
		pref.setSelectable(false);
		//setEnabled(false) creates bad readability on some devices
		//pref.setEnabled(false);
		info.addPreference(pref);

		final Preference agpspref = new Preference(this);
		agpspref.setTitle(R.string.agps_info);
		if (settings.AGPS_DATA_LAST_TIME_DOWNLOADED.get() != 0L) {
			SimpleDateFormat prt = new SimpleDateFormat("yyyy-MM-dd  HH:mm");
			agpspref.setSummary(getString(R.string.agps_data_last_downloaded, prt.format(settings.AGPS_DATA_LAST_TIME_DOWNLOADED.get())));
		} else {
			agpspref.setSummary(getString(R.string.agps_data_last_downloaded, "--"));
		}
		agpspref.setSelectable(true);
		//setEnabled(false) creates bad readability on some devices
		//pref.setEnabled(false);
		agpspref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if(getMyApplication().getSettings().isInternetConnectionAvailable(true)) {
					getMyApplication().getLocationProvider().redownloadAGPS();
					SimpleDateFormat prt = new SimpleDateFormat("yyyy-MM-dd  HH:mm");
					agpspref.setSummary(getString(R.string.agps_data_last_downloaded, prt.format(settings.AGPS_DATA_LAST_TIME_DOWNLOADED.get())));
				}
			return true;
			}
		});
		info.addPreference(agpspref);

		SunriseSunset sunriseSunset = getMyApplication().getDaynightHelper().getSunriseSunset();
		pref = new Preference(this);
		pref.setTitle(R.string.day_night_info);
		if (sunriseSunset != null) {
			SimpleDateFormat prt = new SimpleDateFormat("yyyy-MM-dd  HH:mm");
			pref.setSummary(getString(R.string.day_night_info_description, prt.format(sunriseSunset.getSunrise()),
					prt.format(sunriseSunset.getSunset())));
		} else {
			pref.setSummary(getString(R.string.day_night_info_description, "null", "null"));
		}
		pref.setSelectable(false);
		//setEnabled(false) creates bad readability on some devices
		//pref.setEnabled(false);
		info.addPreference(pref);
	}
}
