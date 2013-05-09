package net.osmand.plus.development;


import java.text.SimpleDateFormat;

import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.util.SunriseSunset;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.os.Debug.MemoryInfo;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;

public class SettingsDevelopmentActivity extends SettingsBaseActivity {

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setTitle(R.string.debugging_and_development);
		PreferenceScreen cat = getPreferenceScreen();
		
		CheckBoxPreference dbg = createCheckBoxPreference(settings.DEBUG_RENDERING_INFO, 
				R.string.trace_rendering, R.string.trace_rendering_descr);
		cat.addPreference(dbg);
		
		Preference pref = new Preference(this);
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
		pref.setTitle(R.string.global_app_allocated_memory);
		
		long javaAvailMem = (Runtime.getRuntime().totalMemory() -  Runtime.getRuntime().freeMemory())/ (1024*1024l);
		long javaTotal = Runtime.getRuntime().totalMemory() / (1024*1024l);
		long dalvikSize = android.os.Debug.getNativeHeapAllocatedSize() / (1024*1024l);
		pref.setSummary(getString(R.string.global_app_allocated_memory_descr, javaAvailMem, javaTotal, dalvikSize));
		cat.addPreference(pref);
		
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
		cat.addPreference(pref);
		
		
		SunriseSunset sunriseSunset = getMyApplication().getDaynightHelper().getSunriseSunset();
		pref = new Preference(this);
		pref.setTitle(R.string.day_night_info);
		if (sunriseSunset != null) {
			SimpleDateFormat prt = new SimpleDateFormat("yyyy-MM-dd  HH:mm");
			pref.setSummary(getString(R.string.day_night_info_description, prt.format(sunriseSunset.getSunrise()),
					prt.format(sunriseSunset.getSunset())));
		} else {
			pref.setSummary(getString(R.string.day_night_info_description, "null",
					"null"));
		}
		cat.addPreference(pref);	
	}



}
