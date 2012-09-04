package net.osmand.plus.development;

import java.text.SimpleDateFormat;

import net.osmand.SunriseSunset;
import net.osmand.plus.OptionsMenuHelper;
import net.osmand.plus.OptionsMenuHelper.OnOptionsMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.routing.RouteAnimation;
import net.osmand.plus.routing.RoutingHelper;
import android.content.Intent;
import android.os.Debug;
import android.os.Debug.MemoryInfo;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;

public class OsmandDevelopmentPlugin extends OsmandPlugin {
	private static final String ID = "osmand.development";
	private OsmandSettings settings;
	private OsmandApplication app;
	private RouteAnimation routeAnimation = new RouteAnimation();
	
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
		return app.getString(R.string.debugging_and_development);
	}
	@Override
	public void registerLayers(MapActivity activity) {
		
	}
	
	@Override
	public void registerOptionsMenuItems(final MapActivity mapActivity, OptionsMenuHelper helper) {
		helper.registerOptionsMenuItem(R.string.animate_route, R.string.animate_route, false, new OnOptionsMenuClick() {
			@Override
			public void prepareOptionsMenu(Menu menu, MenuItem animateMenu) {
				animateMenu.setTitle(routeAnimation.isRouteAnimating() ? R.string.animate_route_off : R.string.animate_route);
				animateMenu.setVisible(settings.getPointToNavigate() != null);
			}
			
			@Override
			public boolean onClick(MenuItem item) {
				RoutingHelper routingHelper = mapActivity.getRoutingHelper();
				// animate moving on route
				routeAnimation.startStopRouteAnimation(routingHelper, mapActivity);
				return true;
			}
		});
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
		
		pref = new Preference(app);
		pref.setTitle(R.string.global_app_allocated_memory);
		
		long javaAvailMem = (Runtime.getRuntime().totalMemory() -  Runtime.getRuntime().freeMemory())/ (1024*1024l);
		long javaTotal = Runtime.getRuntime().totalMemory() / (1024*1024l);
		long dalvikSize = android.os.Debug.getNativeHeapAllocatedSize() / (1024*1024l);
		pref.setSummary(activity.getString(R.string.global_app_allocated_memory_descr, javaAvailMem, javaTotal, dalvikSize));
		cat.addPreference(pref);
		
//		ActivityManager activityManager = (ActivityManager)activity.getSystemService(Context.ACTIVITY_SERVICE);
//		ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
//		activityManager.getMemoryInfo(memoryInfo);
//		long totalSize = memoryInfo.availMem / (1024*1024l);
		MemoryInfo mem = new Debug.MemoryInfo();
		Debug.getMemoryInfo(mem);
		pref = new Preference(app);
		pref.setTitle(R.string.native_app_allocated_memory);
		pref.setSummary(activity.getString(R.string.native_app_allocated_memory_descr 
				, mem.nativePrivateDirty / 1024, mem.dalvikPrivateDirty / 1024 , mem.otherPrivateDirty / 1024
				, mem.nativePss / 1024, mem.dalvikPss / 1024 , mem.otherPss / 1024));
		cat.addPreference(pref);
		
		
		SunriseSunset sunriseSunset = app.getDaynightHelper().getSunriseSunset();
		pref = new Preference(app);
		pref.setTitle(R.string.day_night_info);
		if (sunriseSunset != null) {
			SimpleDateFormat prt = new SimpleDateFormat("yyyy-MM-dd  HH:mm");
			pref.setSummary(activity.getString(R.string.day_night_info_description, prt.format(sunriseSunset.getSunrise()),
					prt.format(sunriseSunset.getSunset())));
		} else {
			pref.setSummary(activity.getString(R.string.day_night_info_description, "null",
					"null"));
		}
		cat.addPreference(pref);		
	}
}
