package net.osmand.plus.plugins.development;

import static net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet.showResetSettingsDialog;
import static net.osmand.plus.simulation.OsmAndLocationSimulation.LocationSimulationListener;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import net.osmand.core.android.MapRendererView;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.aistracker.AisLoadTask;
import net.osmand.plus.plugins.aistracker.AisTrackerPlugin;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.bottomsheets.BooleanRadioButtonsBottomSheet;
import net.osmand.plus.settings.bottomsheets.ConfirmationBottomSheet.ConfirmationDialogListener;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.plus.simulation.OsmAndLocationSimulation;
import net.osmand.plus.simulation.SimulateLocationFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.SunriseSunset;

import java.text.SimpleDateFormat;

public class DevelopmentSettingsFragment extends BaseSettingsFragment implements ConfirmationDialogListener {

	private static final String SIMULATE_INITIAL_STARTUP = "simulate_initial_startup";
	private static final String SIMULATE_YOUR_LOCATION = "simulate_your_location";
	private static final String AGPS_DATA_DOWNLOADED = "agps_data_downloaded";
	private static final String RESET_TO_DEFAULT = "reset_to_default";
	private static final String AISTRACKER_SIMULATION = "aistracker_simulation";

	private static final int OPEN_AIS_FILE_REQUEST = 1001;

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd  HH:mm");

	private OsmandDevelopmentPlugin plugin;
	private LocationSimulationListener simulationListener;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		plugin = PluginsHelper.getPlugin(OsmandDevelopmentPlugin.class);
		simulationListener = simulating -> app.runInUIThread(this::setupSimulateYourLocationPref);
	}

	@Override
	protected void setupPreferences() {
		Preference developmentInfo = findPreference("development_info");
		developmentInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		Preference heightmapCategoryPref = findPreference("heightmap");
		heightmapCategoryPref.setIconSpaceReserved(false);
		setupHeightmapRelatedPrefs();

		Preference safeCategory = findPreference("safe");
		safeCategory.setIconSpaceReserved(false);
		setupSafeModePref();

		Preference routingCategory = findPreference("routing");
		routingCategory.setIconSpaceReserved(false);

		setupSimulateYourLocationPref();

		Preference debuggingAndDevelopment = findPreference("debugging_and_development");
		debuggingAndDevelopment.setIconSpaceReserved(false);

		setupBatterySavingModePref();
		setupSimulateOBDDataPref();
		setupSimulateInitialStartupPref();
		setupFullscreenMapDrawingModePref();
		setupShouldShowFreeVersionBannerPref();
		setupTestVoiceCommandsPref();
		setupLogcatBufferPref();
		setupPressedKeyInfoPref();

		setupTripRecordingPrefs();

		setupMapRenderingPrefs();
		setupAisTrackerPrefs();

		Preference info = findPreference("info");
		info.setIconSpaceReserved(false);

		setupMaxRenderingThreadsPref();
		setupMemoryAllocatedForRoutingPref();
		setupGlobalAppAllocatedMemoryPref();
		setupNativeAppAllocatedMemoryPref();
		setupAgpsDataDownloadedPref();
		setupDayNightInfoPref();
		setupLoadAvgInfoPref();

		setupResetToDefaultButton();
	}

	private void setupSafeModePref() {
		SwitchPreferenceEx safeMode = findPreference(settings.SAFE_MODE.getId());
		safeMode.setDescription(getString(R.string.safe_mode_description));
		safeMode.setIconSpaceReserved(false);
		// disable the switch if the library cannot be used
		if ((NativeOsmandLibrary.isLoaded() && !NativeOsmandLibrary.isSupported()) || settings.NATIVE_RENDERING_FAILED.get()) {
			safeMode.setEnabled(false);
			safeMode.setChecked(true);
		}
	}

	private void setupHeightmapRelatedPrefs() {
		SwitchPreferenceEx preference = findPreference(plugin.USE_RASTER_SQLITEDB.getId());
		preference.setIconSpaceReserved(false);
		SRTMPlugin srtmPlugin = PluginsHelper.getActivePlugin(SRTMPlugin.class);
		boolean enabled = srtmPlugin != null && srtmPlugin.is3DReliefAllowed();
		preference.setEnabled(enabled);
	}

	private void setupSimulateYourLocationPref() {
		Preference simulateYourLocation = findPreference(SIMULATE_YOUR_LOCATION);
		simulateYourLocation.setIconSpaceReserved(false);
		OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
		simulateYourLocation.setSummary(sim.isRouteAnimating() ? R.string.shared_string_in_progress : R.string.simulate_your_location_descr);
	}

	private void setupBatterySavingModePref() {
		SwitchPreferenceEx debugRenderingInfo = findPreference(settings.BATTERY_SAVING_MODE.getId());
		debugRenderingInfo.setDescription(getString(R.string.battery_saving_mode));
		debugRenderingInfo.setIconSpaceReserved(false);
	}

	private void setupSimulateOBDDataPref() {
		SwitchPreferenceEx debugRenderingInfo = findPreference(settings.SIMULATE_OBD_DATA.getId());
		debugRenderingInfo.setDescription(getString(R.string.simulate_obd));
		debugRenderingInfo.setIconSpaceReserved(false);
	}


	private void setupSimulateInitialStartupPref() {
		Preference simulateInitialStartup = findPreference(SIMULATE_INITIAL_STARTUP);
		simulateInitialStartup.setIconSpaceReserved(false);
	}

	private void setupShouldShowFreeVersionBannerPref() {
		SwitchPreferenceEx shouldShowFreeVersionBanner = findPreference(settings.SHOULD_SHOW_FREE_VERSION_BANNER.getId());
		shouldShowFreeVersionBanner.setDescription(getString(R.string.show_free_version_banner_description));
		shouldShowFreeVersionBanner.setIconSpaceReserved(false);
	}

	private void setupFullscreenMapDrawingModePref() {
		SwitchPreferenceEx fullscreenMapDrawingMode = findPreference(settings.TRANSPARENT_STATUS_BAR.getId());
		fullscreenMapDrawingMode.setDescription(getString(R.string.transparent_status_bar_descr));
		fullscreenMapDrawingMode.setIconSpaceReserved(false);
	}

	private void setupTestVoiceCommandsPref() {
		Preference testVoiceCommands = findPreference("test_voice_commands");
		testVoiceCommands.setIntent(new Intent(getActivity(), TestVoiceActivity.class));
		testVoiceCommands.setIconSpaceReserved(false);
	}

	private void setupLogcatBufferPref() {
		Preference logcatBuffer = findPreference("logcat_buffer");
		logcatBuffer.setIntent(new Intent(getActivity(), LogcatActivity.class));
		logcatBuffer.setIconSpaceReserved(false);
	}

	private void setupPressedKeyInfoPref() {
		SwitchPreferenceEx debugRenderingInfo = findPreference(settings.SHOW_INFO_ABOUT_PRESSED_KEY.getId());
		debugRenderingInfo.setDescription(getString(R.string.show_toast_about_key_pressed));
		debugRenderingInfo.setIconSpaceReserved(false);
	}

	private void setupTripRecordingPrefs() {
		Preference routingCategory = findPreference("trip_recording");
		routingCategory.setIconSpaceReserved(false);

		SwitchPreferenceEx bearingPref = findPreference(plugin.SAVE_BEARING_TO_GPX.getId());
		bearingPref.setIconSpaceReserved(false);
		bearingPref.setDescription(R.string.write_bearing_description);

		SwitchPreferenceEx headingPref = findPreference(plugin.SAVE_HEADING_TO_GPX.getId());
		headingPref.setIconSpaceReserved(false);
		headingPref.setDescription(R.string.write_heading_description);

		SwitchPreferenceEx locationProviderPref = findPreference(plugin.SAVE_LOCATION_PROVIDER_TO_GPX.getId());
		locationProviderPref.setIconSpaceReserved(false);
		locationProviderPref.setDescription(R.string.write_location_provider_description);
	}

	private void setupMapRenderingPrefs() {
		Preference textsCategory = findPreference("texts");
		textsCategory.setIconSpaceReserved(false);

		SwitchPreferenceEx symRasterTilePref = findPreference(plugin.SHOW_PRIMITIVES_DEBUG_INFO.getId());
		symRasterTilePref.setIconSpaceReserved(false);
		symRasterTilePref.setDescription(R.string.show_debug_tile_description);

		SwitchPreferenceEx msaaPref = findPreference(settings.ENABLE_MSAA.getId());
		msaaPref.setIconSpaceReserved(false);
		msaaPref.setVisible(MapRendererView.isMSAASupported());

		SwitchPreferenceEx disableMapLayers = findPreference(settings.DISABLE_MAP_LAYERS.getId());
		disableMapLayers.setDescription(getString(R.string.disable_map_layers_descr));
		disableMapLayers.setIconSpaceReserved(false);

		SwitchPreferenceEx symtopPref = findPreference(plugin.ALLOW_SYMBOLS_DISPLAY_ON_TOP.getId());
		symtopPref.setIconSpaceReserved(false);
		symtopPref.setDescription(R.string.allow_display_on_top_description);

		SwitchPreferenceEx debugRenderingInfo = findPreference(settings.DEBUG_RENDERING_INFO.getId());
		debugRenderingInfo.setDescription(getString(R.string.trace_rendering_descr));
		debugRenderingInfo.setIconSpaceReserved(false);

		SwitchPreferenceEx spherePref = findPreference(settings.SPHERICAL_MAP.getId());
		spherePref.setIconSpaceReserved(false);
		spherePref.setDescription(R.string.show_spherical_map_description);
	}

	private void setupMaxRenderingThreadsPref() {
		MapRendererView mapRenderer = app.getOsmandMap().getMapView().getMapRenderer();
		int value = settings.MAX_RENDERING_THREADS.get();
		Preference preference = findPreference(settings.MAX_RENDERING_THREADS.getId());
		if (value == 0) {
			value = mapRenderer != null ? mapRenderer.getResourceWorkerThreadsLimit() : -1;
		}
		preference.setSummary(getString(R.string.ltr_or_rtl_combine_via_space, String.valueOf(value), (value == 1 ? "thread" : "threads")));
		preference.setIconSpaceReserved(false);
		preference.setVisible(mapRenderer != null);
	}

	private void setupMemoryAllocatedForRoutingPref() {
		int value = settings.MEMORY_ALLOCATED_FOR_ROUTING.get();
		Preference preference = findPreference(settings.MEMORY_ALLOCATED_FOR_ROUTING.getId());
		preference.setSummary(getString(R.string.ltr_or_rtl_combine_via_space, String.valueOf(value), "MB"));
		preference.setIconSpaceReserved(false);
	}

	private void setupAisTrackerPrefs() {
		AisTrackerPlugin plugin = PluginsHelper.getPlugin(AisTrackerPlugin.class);

		Preference category = findPreference("aistracker");
		Preference preference = findPreference(AISTRACKER_SIMULATION);

		category.setVisible(plugin != null);
		preference.setVisible(plugin != null);

		category.setIconSpaceReserved(false);
		preference.setIconSpaceReserved(false);
	}

	private void setupGlobalAppAllocatedMemoryPref() {
		long javaAvailMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024L);
		long javaTotal = Runtime.getRuntime().totalMemory() / (1024 * 1024L);
		long dalvikSize = android.os.Debug.getNativeHeapAllocatedSize() / (1024 * 1024L);

		Preference globalAppAllocatedMemory = findPreference("global_app_allocated_memory");
		globalAppAllocatedMemory.setSummary(getString(R.string.global_app_allocated_memory_descr,
				String.valueOf(javaAvailMem), String.valueOf(javaTotal), String.valueOf(dalvikSize)));
		globalAppAllocatedMemory.setIconSpaceReserved(false);
	}

	private void setupNativeAppAllocatedMemoryPref() {
//		ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
//		ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
//		activityManager.getMemoryInfo(memoryInfo);
//		long totalSize = memoryInfo.availMem / (1024*1024l);
		Debug.MemoryInfo mem = new Debug.MemoryInfo();
		Debug.getMemoryInfo(mem);
		//setEnabled(false) creates bad readability on some devices
		//pref.setEnabled(false);
		Preference nativeAppAllocatedMemory = findPreference("native_app_allocated_memory");
		nativeAppAllocatedMemory.setIconSpaceReserved(false);
		nativeAppAllocatedMemory.setSummary(getString(R.string.native_app_allocated_memory_descr
				, mem.nativePrivateDirty / 1024, mem.dalvikPrivateDirty / 1024, mem.otherPrivateDirty / 1024
				, mem.nativePss / 1024, mem.dalvikPss / 1024, mem.otherPss / 1024));
	}

	private void setupAgpsDataDownloadedPref() {
		Preference agpsDataDownloaded = findPreference("agps_data_downloaded");
		agpsDataDownloaded.setSummary(getAgpsDataDownloadedSummary());
		agpsDataDownloaded.setIconSpaceReserved(false);
	}

	private String getAgpsDataDownloadedSummary() {
		if (settings.AGPS_DATA_LAST_TIME_DOWNLOADED.get() != 0L) {
			return getString(R.string.agps_data_last_downloaded, DATE_FORMAT.format(settings.AGPS_DATA_LAST_TIME_DOWNLOADED.get()));
		} else {
			return getString(R.string.agps_data_last_downloaded, "--");
		}
	}

	private void setupDayNightInfoPref() {
		Preference dayNightInfo = findPreference("day_night_info");
		SunriseSunset sunriseSunset = app.getDaynightHelper().getSunriseSunset();
		String sunrise = sunriseSunset != null ? DATE_FORMAT.format(sunriseSunset.getSunrise()) : "null";
		String sunset = sunriseSunset != null ? DATE_FORMAT.format(sunriseSunset.getSunset()) : "null";
		dayNightInfo.setSummary(getString(R.string.day_night_info_description, sunrise, sunset));
		dayNightInfo.setIconSpaceReserved(false);
	}

	private void setupLoadAvgInfoPref() {
		OsmandDevelopmentPlugin.AvgStatsEntry m1 = plugin.getAvgStats(1);
		OsmandDevelopmentPlugin.AvgStatsEntry m5 = plugin.getAvgStats(5);
		OsmandDevelopmentPlugin.AvgStatsEntry m15 = plugin.getAvgStats(15);

		final int AUTO_DETECT_MICROAMPERES = 10000;
		// Samsung's BatteryManager API reported instantaneous and
		// average battery current in milliamperes (mA) rather than
		// in microamperes (µA) as specified in the API documentation.
		if (Math.abs(m1.energyConsumption) > AUTO_DETECT_MICROAMPERES) m1.energyConsumption /= 1000;
		if (Math.abs(m5.energyConsumption) > AUTO_DETECT_MICROAMPERES) m5.energyConsumption /= 1000;
		if (Math.abs(m15.energyConsumption) > AUTO_DETECT_MICROAMPERES)
			m15.energyConsumption /= 1000;

		String fps = String.format("%.0f / %.0f / %.0f", m1.fps1k, m5.fps1k, m15.fps1k);
		String gpu = String.format("%.2f / %.2f / %.2f", m1.gpu1k, m5.gpu1k, m15.gpu1k);
		String idle = String.format("%.2f / %.2f / %.2f", m1.idle1k, m5.idle1k, m15.idle1k);
		String cpu = String.format("%.2f / %.2f / %.2f", m1.cpuBasic, m5.cpuBasic, m15.cpuBasic);
		String battery = String.format("%.2f%% / %.2f%% / %.2f%%", m1.batteryLevel, m5.batteryLevel, m15.batteryLevel);
		String energy = String.format("%.0f / %.0f / %.0f", m1.energyConsumption, m5.energyConsumption, m15.energyConsumption);

		Preference energyAvgInfo = findPreference("energy_avg_info");
		energyAvgInfo.setSummary(getString(R.string.energy_avg_info_description, battery, energy));
		energyAvgInfo.setIconSpaceReserved(false);

		Preference renderingAvgInfo = findPreference("rendering_avg_info");
		renderingAvgInfo.setSummary(getString(R.string.rendering_avg_info_details, fps, cpu, idle, gpu));
		renderingAvgInfo.setIconSpaceReserved(false);
	}

	private void setupResetToDefaultButton() {
		Preference resetToDefault = findPreference(RESET_TO_DEFAULT);
		resetToDefault.setIcon(getActiveIcon(R.drawable.ic_action_reset_to_default_dark));
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (SIMULATE_YOUR_LOCATION.equals(prefId)) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				SimulateLocationFragment.showInstance(activity.getSupportFragmentManager(), null, false);
			}
			return true;
		} else if (SIMULATE_INITIAL_STARTUP.equals(prefId)) {
			app.getAppInitializer().resetFirstTimeRun();
			settings.FIRST_MAP_IS_DOWNLOADED.resetToDefault();
			settings.WEBGL_SUPPORTED.resetToDefault();
			settings.WIKI_ARTICLE_SHOW_IMAGES_ASKED.resetToDefault();

			MapillaryPlugin mapillaryPlugin = PluginsHelper.getPlugin(MapillaryPlugin.class);
			if (mapillaryPlugin != null) {
				mapillaryPlugin.MAPILLARY_FIRST_DIALOG_SHOWN.resetToDefault();
			}

			app.showToastMessage(R.string.shared_string_ok);
			return true;
		} else if (AGPS_DATA_DOWNLOADED.equals(prefId)) {
			if (settings.isInternetConnectionAvailable(true)) {
				app.getLocationProvider().redownloadAGPS();
				preference.setSummary(getAgpsDataDownloadedSummary());
			}
			return true;
		} else if (settings.MAX_RENDERING_THREADS.getId().equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				MaxRenderingThreadsBottomSheet.showInstance(fragmentManager, preference.getKey(), this, getSelectedAppMode());
			}
		} else if (settings.MEMORY_ALLOCATED_FOR_ROUTING.getId().equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				AllocatedRoutingMemoryBottomSheet.showInstance(fragmentManager, preference.getKey(), this, getSelectedAppMode());
			}
		} else if (RESET_TO_DEFAULT.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				showResetSettingsDialog(fragmentManager, this, R.string.debugging_and_development);
			}
		} else if (AISTRACKER_SIMULATION.equals(prefId)) {
			Intent intent = ImportHelper.getImportFileIntent();
			AndroidUtils.startActivityForResultIfSafe(this, intent, OPEN_AIS_FILE_REQUEST);
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == OPEN_AIS_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				Uri uri = data.getData();
				if (uri != null) {
					AisLoadTask task = new AisLoadTask(app, uri);
					OsmAndTaskManager.executeTask(task);
				}
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onApplyPreferenceChange(String prefId, boolean applyToAllProfiles, Object newValue) {
		if (prefId.equals(settings.MEMORY_ALLOCATED_FOR_ROUTING.getId())) {
			applyPreference(settings.MEMORY_ALLOCATED_FOR_ROUTING.getId(), applyToAllProfiles, newValue);
			setupMemoryAllocatedForRoutingPref();
		} else if (prefId.equals(settings.MAX_RENDERING_THREADS.getId())) {
			applyPreference(settings.MAX_RENDERING_THREADS.getId(), applyToAllProfiles, newValue);
			setupMaxRenderingThreadsPref();
		} else {
			super.onApplyPreferenceChange(prefId, applyToAllProfiles, newValue);
		}
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		String prefId = preference.getKey();

		if (plugin.SAVE_BEARING_TO_GPX.getId().equals(prefId) || plugin.SAVE_HEADING_TO_GPX.getId().equals(prefId)
				|| plugin.SAVE_LOCATION_PROVIDER_TO_GPX.getId().equals(prefId)) {
			FragmentManager manager = getFragmentManager();
			if (manager != null) {
				BooleanRadioButtonsBottomSheet.showInstance(manager, prefId, getApplyQueryType(),
						this, getSelectedAppMode(), false, isProfileDependent());
			}
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();
		if (settings.SAFE_MODE.getId().equals(prefId) && newValue instanceof Boolean) {
			loadNativeLibrary();
			return true;
		} else if (settings.TRANSPARENT_STATUS_BAR.getId().equals(prefId) && newValue instanceof Boolean) {
			restartActivity();
			return true;
		}
		return super.onPreferenceChange(preference, newValue);
	}

	@Override
	public void onResume() {
		super.onResume();
		app.getLocationProvider().getLocationSimulation().addSimulationListener(simulationListener);
	}

	@Override
	public void onPause() {
		super.onPause();
		app.getLocationProvider().getLocationSimulation().removeSimulationListener(simulationListener);
	}

	@Override
	public void onActionConfirmed(int actionId) {
		CommonPreference<Boolean> safeMode = (CommonPreference<Boolean>) settings.SAFE_MODE;
		CommonPreference<Boolean> transparentStatusBar = (CommonPreference<Boolean>) settings.TRANSPARENT_STATUS_BAR;

		boolean shouldLoadNativeLibrary = safeMode.get() != safeMode.getDefaultValue();
		boolean shouldRestartActivity = transparentStatusBar.get() != transparentStatusBar.getDefaultValue();

		settings.resetGlobalPreferences(plugin.getPreferences());
		app.showToastMessage(R.string.plugin_prefs_reset_successful);

		if (shouldLoadNativeLibrary) {
			loadNativeLibrary();
		}
		if (shouldRestartActivity) {
			restartActivity();
		}
		updateAllSettings();
	}

	private void restartActivity() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.restart();
		}
	}

	private void loadNativeLibrary() {
		FragmentActivity activity = getActivity();
		if (!NativeOsmandLibrary.isLoaded() && activity != null) {
			RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();
			NativeLibraryLoadTask nativeLibraryLoadTask = new NativeLibraryLoadTask(activity, storage);
			OsmAndTaskManager.executeTask(nativeLibraryLoadTask);
		}
	}
}