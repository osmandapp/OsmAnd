package net.osmand.plus.activities;


import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.osmand.IndexConstants;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.DeviceAdminRecv;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.AutoZoomMap;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.OsmandSettings.SpeedConstants;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.GeneralRouter.RoutingParameterType;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingsNavigationActivity extends SettingsBaseActivity {

	public static final String MORE_VALUE = "MORE_VALUE";

	private Preference avoidRouting;
	private Preference preferRouting;
	private Preference reliefFactorRouting;
	private Preference autoZoom;
	private Preference showAlarms;
	private Preference speakAlarms;
	private ListPreference routerServicePreference;
	private ListPreference speedLimitExceed;
	
	private ComponentName mDeviceAdmin;
	private static final int DEVICE_ADMIN_REQUEST = 5;
	
	private List<RoutingParameter> avoidParameters = new ArrayList<RoutingParameter>();
	private List<RoutingParameter> preferParameters = new ArrayList<RoutingParameter>();
	private List<RoutingParameter> reliefFactorParameters = new ArrayList<RoutingParameter>();
	public static final String INTENT_SKIP_DIALOG = "INTENT_SKIP_DIALOG";
	
	public SettingsNavigationActivity() {
		super(true);
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getToolbar().setTitle(R.string.routing_settings);
	
		createUI();
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == DEVICE_ADMIN_REQUEST) {
			if (resultCode == RESULT_OK) {
//				Log.d("DeviceAdmin", "Lock screen permission approved.");
			} else {
				settings.WAKE_ON_VOICE_INT.set(0);
//				Log.d("DeviceAdmin", "Lock screen permission refused.");
			}
			return;
		}
	}

	private void requestLockScreenAdmin() {
		mDeviceAdmin = new ComponentName(getApplicationContext(),
				DeviceAdminRecv.class);

		DevicePolicyManager mDevicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);

		if (!mDevicePolicyManager.isAdminActive(mDeviceAdmin)) {
			// request permission from user
			Intent intent = new Intent(
					DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
					mDeviceAdmin);
			intent.putExtra(
					DevicePolicyManager.EXTRA_ADD_EXPLANATION,
					getString(R.string.lock_screen_request_explanation,
							Version.getAppName(getMyApplication())));
			startActivityForResult(intent, DEVICE_ADMIN_REQUEST);
		}
	};


	private void createUI() {
		addPreferencesFromResource(R.xml.navigation_settings);
		PreferenceScreen screen = getPreferenceScreen();
		settings = getMyApplication().getSettings();
		routerServicePreference = (ListPreference) screen.findPreference(settings.ROUTER_SERVICE.getId());
		
		RouteService[] vls = RouteService.getAvailableRouters(getMyApplication());
		String[] entries = new String[vls.length];
		for(int i=0; i<entries.length; i++){
			entries[i] = vls[i].getName();
		}
		registerListPreference(settings.ROUTER_SERVICE, screen, entries, vls);
		
		
		registerBooleanPreference(settings.SNAP_TO_ROAD, screen);

		Integer[] intValues = new Integer[] { 0, 5, 10, 15, 20, 25, 30, 45, 60, 90};
		entries = new String[intValues.length];
		entries[0] = getString(R.string.shared_string_never);
		for (int i = 1; i < intValues.length; i++) {
			entries[i] = (int) intValues[i] + " " + getString(R.string.int_seconds);
		}
		registerListPreference(settings.AUTO_FOLLOW_ROUTE, screen, entries, intValues);

		autoZoom = screen.findPreference("auto_zoom_map_on_off");
		autoZoom.setOnPreferenceClickListener(this);

		//keep informing option:
        Integer[] keepInformingValues = new Integer[]{0, 1, 2, 3, 5, 7, 10, 15, 20, 25, 30};
        String[] keepInformingNames = new String[keepInformingValues.length];
        keepInformingNames[0] = getString(R.string.keep_informing_never);
        for (int i = 1; i < keepInformingValues.length; i++)
        {
            keepInformingNames[i] = keepInformingValues[i] + " " + getString(R.string.int_min);
        }
        registerListPreference(settings.KEEP_INFORMING, screen, keepInformingNames, keepInformingValues);
        

		SpeedConstants[] speedValues = SpeedConstants.values();
		String[] speedNamesVls = new String[speedValues.length];
		for(int i = 0; i < speedValues.length; i++) {
			speedNamesVls[i] = speedValues[i].toHumanString(this);
		};
		registerListPreference(settings.SPEED_SYSTEM, screen, speedNamesVls, speedValues);
        
		// screen power save option:
		Integer[] screenPowerSaveValues = new Integer[] { 0, 5, 10, 15, 20, 30, 45, 60 };
		String[] screenPowerSaveNames = new String[screenPowerSaveValues.length];
		screenPowerSaveNames[0] = getString(R.string.shared_string_never);
		for (int i = 1; i < screenPowerSaveValues.length; i++) {
			screenPowerSaveNames[i] = screenPowerSaveValues[i] + " "
					+ getString(R.string.int_seconds);
		}
		registerListPreference(settings.WAKE_ON_VOICE_INT, screen, screenPowerSaveNames, screenPowerSaveValues);
        
//         registerBooleanPreference(settings.SHOW_ZOOM_BUTTONS_NAVIGATION, screen);
		
		showAlarms = (Preference) screen.findPreference("show_routing_alarms");
		showAlarms.setOnPreferenceClickListener(this);
		
		speakAlarms = (Preference) screen.findPreference("speak_routing_alarms");
		speakAlarms.setOnPreferenceClickListener(this);
		
		Float[] arrivalValues = new Float[] {1.5f, 1f, 0.5f, 0.25f} ;
		String[] arrivalNames = new String[] {
				getString(R.string.arrival_distance_factor_early),
				getString(R.string.arrival_distance_factor_normally),
				getString(R.string.arrival_distance_factor_late),
				getString(R.string.arrival_distance_factor_at_last)
		};
		registerListPreference(settings.ARRIVAL_DISTANCE_FACTOR, screen, arrivalNames, arrivalValues);

		//array size should be equal!
		Float[] speedLimitsKm = new Float[]{-10f, -7f,-5f, 0f, 5f, 7f, 10f, 15f, 20f};
		Float[] speedLimitsMiles = new Float[]{-7f, -5f, -3f, 0f, 3f, 5f, 7f, 10f, 15f};
		if (settings.METRIC_SYSTEM.get() == OsmandSettings.MetricsConstants.KILOMETERS_AND_METERS) {
			String[] speedNames = new String[speedLimitsKm.length];
			for (int i =0; i<speedLimitsKm.length;i++){
				speedNames[i] = speedLimitsKm[i] + " " + getString(R.string.km_h);
			}
			registerListPreference(settings.SPEED_LIMIT_EXCEED, screen, speedNames, speedLimitsKm);
			registerListPreference(settings.SWITCH_MAP_DIRECTION_TO_COMPASS, screen, speedNames, speedLimitsKm);
		} else {
			String[] speedNames = new String[speedLimitsKm.length];
			for (int i =0; i<speedNames.length;i++){
				speedNames[i] = speedLimitsMiles[i] + " " + getString(R.string.mile_per_hour);
			}
			registerListPreference(settings.SPEED_LIMIT_EXCEED, screen, speedNames, speedLimitsKm);
			registerListPreference(settings.SWITCH_MAP_DIRECTION_TO_COMPASS, screen, speedNames, speedLimitsKm);
		}

		PreferenceCategory category = (PreferenceCategory) screen.findPreference("guidance_preferences");
		speedLimitExceed = (ListPreference) category.findPreference("speed_limit_exceed");
		ApplicationMode mode = getMyApplication().getSettings().getApplicationMode();
		if (!mode.isDerivedRoutingFrom(ApplicationMode.CAR)) {
			category.removePreference(speedLimitExceed);
		}

		// deprecated 2.2
//		Integer[] delayIntervals = new Integer[] { -1, 3, 5, 7, 10, 15, 20 };
//		String[] delayIntervalNames = new String[delayIntervals.length];
//		for (int i = 0; i < delayIntervals.length; i++) {
//			if (i == 0) {
//				delayIntervalNames[i] = getString(R.string.auto_follow_route_never);
//			} else {
//				delayIntervalNames[i] = delayIntervals[i] + " " + getString(R.string.int_seconds);
//			}
//		}
		// registerListPreference(settings.DELAY_TO_START_NAVIGATION, screen, delayIntervalNames, delayIntervals);


		if(getIntent() != null && getIntent().hasExtra(INTENT_SKIP_DIALOG)) {
			setSelectedAppMode(settings.getApplicationMode());
		} else {
			profileDialog();
		}

		addVoicePrefs((PreferenceGroup) screen.findPreference("voice"));
	}

	private void reloadVoiceListPreference(PreferenceScreen screen) {
		String[] entries;
		String[] entrieValues;
		Set<String> voiceFiles = getVoiceFiles();
		entries = new String[voiceFiles.size() + 2];
		entrieValues = new String[voiceFiles.size() + 2];
		int k = 0;
		// entries[k++] = getString(R.string.shared_string_none);
		entrieValues[k] = OsmandSettings.VOICE_PROVIDER_NOT_USE;
		entries[k++] = getString(R.string.shared_string_do_not_use);
		for (String s : voiceFiles) {
			entries[k] = (s.contains("tts") ? getString(R.string.ttsvoice) + " " : "") +
					FileNameTranslationHelper.getVoiceName(this, s);
			entrieValues[k] = s;
			k++;
		}
		entrieValues[k] = MORE_VALUE;
		entries[k] = getString(R.string.install_more);
		registerListPreference(settings.VOICE_PROVIDER, screen, entries, entrieValues);
	}


	private Set<String> getVoiceFiles() {
		// read available voice data
		File extStorage = getMyApplication().getAppPath(IndexConstants.VOICE_INDEX_DIR);
		Set<String> setFiles = new LinkedHashSet<String>();
		if (extStorage.exists()) {
			for (File f : extStorage.listFiles()) {
				if (f.isDirectory()) {
					setFiles.add(f.getName());
				}
			}
		}
		return setFiles;
	}

	private void addVoicePrefs(PreferenceGroup cat) {
		if (!Version.isBlackberry((OsmandApplication) getApplication())) {
			ListPreference lp = createListPreference(
					settings.AUDIO_STREAM_GUIDANCE,
					new String[]{getString(R.string.voice_stream_music), getString(R.string.voice_stream_notification),
							getString(R.string.voice_stream_voice_call)}, new Integer[]{AudioManager.STREAM_MUSIC,
							AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_VOICE_CALL}, R.string.choose_audio_stream,
					R.string.choose_audio_stream_descr);
			final Preference.OnPreferenceChangeListener prev = lp.getOnPreferenceChangeListener();
			lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					prev.onPreferenceChange(preference, newValue);
					CommandPlayer player = getMyApplication().getPlayer();
					if (player != null) {
						player.updateAudioStream(settings.AUDIO_STREAM_GUIDANCE.get());
					}
					// Sync DEFAULT value with CAR value, as we have other way to set it for now
					settings.AUDIO_STREAM_GUIDANCE.setModeValue(ApplicationMode.DEFAULT, settings.AUDIO_STREAM_GUIDANCE.getModeValue(ApplicationMode.CAR));
					return true;
				}
			});
			cat.addPreference(lp);
			cat.addPreference(createCheckBoxPreference(settings.INTERRUPT_MUSIC, R.string.interrupt_music,
					R.string.interrupt_music_descr));
		}
	}

	private void prepareRoutingPrefs(PreferenceScreen screen) {
		PreferenceCategory cat = (PreferenceCategory) screen.findPreference("routing_preferences");
		cat.removeAll();
		CheckBoxPreference fastRoute = createCheckBoxPreference(settings.FAST_ROUTE_MODE, R.string.fast_route_mode, R.string.fast_route_mode_descr);
		if(settings.ROUTER_SERVICE.get() != RouteService.OSMAND) {
			cat.addPreference(fastRoute);
		} else {
			ApplicationMode am = settings.getApplicationMode();
			GeneralRouter router = getRouter(getMyApplication().getDefaultRoutingConfig(), am);
			clearParameters();
			if (router != null) {
				Map<String, RoutingParameter> parameters = router.getParameters();
				if(parameters.containsKey("short_way")) {
					cat.addPreference(fastRoute);
				}
				List<RoutingParameter> others = new ArrayList<GeneralRouter.RoutingParameter>();
				for(Map.Entry<String, RoutingParameter> e : parameters.entrySet()) {
					String param = e.getKey();
					RoutingParameter routingParameter = e.getValue();
					if (param.startsWith("avoid_")) {
						avoidParameters.add(routingParameter);
					} else if (param.startsWith("prefer_")) {
						preferParameters.add(routingParameter);
					} else if ("relief_smoothness_factor".equals(routingParameter.getGroup())) {
						reliefFactorParameters.add(routingParameter);
					} else if (!param.equals("short_way") && !"driving_style".equals(routingParameter.getGroup())) {
						others.add(routingParameter);
					}
				}
				if (avoidParameters.size() > 0) {
					avoidRouting = new Preference(this);
					avoidRouting.setTitle(R.string.avoid_in_routing_title);
					avoidRouting.setSummary(R.string.avoid_in_routing_descr);
					avoidRouting.setOnPreferenceClickListener(this);
					cat.addPreference(avoidRouting);
				}
				if (preferParameters.size() > 0) {
					preferRouting = new Preference(this);
					preferRouting.setTitle(R.string.prefer_in_routing_title);
					preferRouting.setSummary(R.string.prefer_in_routing_descr);
					preferRouting.setOnPreferenceClickListener(this);
					cat.addPreference(preferRouting);
				}
				if (reliefFactorParameters.size() > 0) {
					reliefFactorRouting = new Preference(this);
					reliefFactorRouting.setTitle(SettingsBaseActivity.getRoutingStringPropertyName(this, reliefFactorParameters.get(0).getGroup(),
							Algorithms.capitalizeFirstLetterAndLowercase(reliefFactorParameters.get(0).getGroup().replace('_', ' '))));
					reliefFactorRouting.setSummary(R.string.relief_smoothness_factor_descr);
					reliefFactorRouting.setOnPreferenceClickListener(this);
					cat.addPreference(reliefFactorRouting);
				}
				for(RoutingParameter p : others) {
					Preference basePref;
					if(p.getType() == RoutingParameterType.BOOLEAN) {
						basePref = createCheckBoxPreference(settings.getCustomRoutingBooleanProperty(p.getId(), p.getDefaultBoolean()));
					} else {
						Object[] vls = p.getPossibleValues();
						String[] svlss = new String[vls.length];
						int i = 0;
						for(Object o : vls) {
							svlss[i++] = o.toString();
						}
						basePref = createListPreference(settings.getCustomRoutingProperty(p.getId(), 
								p.getType() == RoutingParameterType.NUMERIC ? "0.0" : "-"), 
								p.getPossibleValueDescriptions(), svlss, 
								SettingsBaseActivity.getRoutingStringPropertyName(this, p.getId(), p.getName()), 
								SettingsBaseActivity.getRoutingStringPropertyDescription(this, p.getId(), p.getDescription()));
					}
					basePref.setTitle(SettingsBaseActivity.getRoutingStringPropertyName(this, p.getId(), p.getName()));
					basePref.setSummary(SettingsBaseActivity.getRoutingStringPropertyDescription(this, p.getId(), p.getDescription()));
					cat.addPreference(basePref);
				}
			}
			ApplicationMode mode = getMyApplication().getSettings().getApplicationMode();
			if (mode.isDerivedRoutingFrom(ApplicationMode.CAR)) {
				PreferenceCategory category = (PreferenceCategory) screen.findPreference("guidance_preferences");
				category.addPreference(speedLimitExceed);
			} else {
				PreferenceCategory category = (PreferenceCategory) screen.findPreference("guidance_preferences");
				category.removePreference(speedLimitExceed);
			}
		}
	}

	public String getRoutinParameterTitle(Context context, RoutingParameter routingParameter) {
		return SettingsBaseActivity.getRoutingStringPropertyName(context, routingParameter.getId(),
				routingParameter.getName());
	}

	public boolean isRoutingParameterSelected(OsmandSettings settings, ApplicationMode am, RoutingParameter routingParameter) {
		final OsmandSettings.CommonPreference<Boolean> property =
				settings.getCustomRoutingBooleanProperty(routingParameter.getId(), routingParameter.getDefaultBoolean());
		if(am != null) {
			return property.getModeValue(am);
		} else {
			return property.get();
		}
	}

	public void setRoutingParameterSelected(OsmandSettings settings, ApplicationMode am, RoutingParameter routingParameter, boolean isChecked) {
		final OsmandSettings.CommonPreference<Boolean> property =
				settings.getCustomRoutingBooleanProperty(routingParameter.getId(), routingParameter.getDefaultBoolean());
		if(am != null) {
			property.setModeValue(am, isChecked);
		} else {
			property.set(isChecked);
		}
	}


	private void clearParameters() {
		preferParameters.clear();
		avoidParameters.clear();
		reliefFactorParameters.clear();
	}


	public static GeneralRouter getRouter(net.osmand.router.RoutingConfiguration.Builder builder, ApplicationMode am) {
		GeneralRouter router = builder.getRouter(am.getStringKey());
		if(router == null && am.getParent() != null) {
			router = builder.getRouter(am.getParent().getStringKey());
		}
		return router;
	}

	public void updateAllSettings() {	
		prepareRoutingPrefs(getPreferenceScreen());
		reloadVoiceListPreference(getPreferenceScreen());
		super.updateAllSettings();
		routerServicePreference.setSummary(getString(R.string.router_service_descr) + "  [" + settings.ROUTER_SERVICE.get() + "]");
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String id = preference.getKey();
		if (id.equals(settings.VOICE_PROVIDER.getId())) {
			if (MORE_VALUE.equals(newValue)) {
				// listPref.set(oldValue); // revert the change..
				final Intent intent = new Intent(this, DownloadActivity.class);
				intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
				intent.putExtra(DownloadActivity.FILTER_CAT, DownloadActivityType.VOICE_FILE.getTag());
				startActivity(intent);
			} else {
				super.onPreferenceChange(preference, newValue);
				getMyApplication().initVoiceCommandPlayer(
						this, settings.APPLICATION_MODE.get(), false, null, true, false);
			}
			return true;
		}
		super.onPreferenceChange(preference, newValue);
		if (id.equals(settings.ROUTER_SERVICE.getId())) {
			routerServicePreference.setSummary(getString(R.string.router_service_descr) + "  ["
					+ settings.ROUTER_SERVICE.get() + "]");
			prepareRoutingPrefs(getPreferenceScreen());
			super.updateAllSettings();
		} else if (id.equals(settings.WAKE_ON_VOICE_INT.getId())) {
			Integer value;
			try {
				value = Integer.parseInt(newValue.toString());
			} catch (NumberFormatException e) {
				value = 0;
			}
			if (value > 0) {
				requestLockScreenAdmin();
			}
		}
		return true;
	}


	@SuppressWarnings("unchecked")
	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == avoidRouting || preference == preferRouting) {
			List<RoutingParameter> prms = preference == avoidRouting ? avoidParameters : preferParameters;
			String[] vals = new String[prms.size()];
			OsmandPreference[] bls = new OsmandPreference[prms.size()];
			for (int i = 0; i < prms.size(); i++) {
				RoutingParameter p = prms.get(i);
				vals[i] = SettingsBaseActivity.getRoutingStringPropertyName(this, p.getId(), p.getName());
				bls[i] = settings.getCustomRoutingBooleanProperty(p.getId(), p.getDefaultBoolean());
			}
			showBooleanSettings(vals, bls, preference.getTitle());
			return true;
		} else if (preference == autoZoom) {
			final ApplicationMode am = settings.getApplicationMode();
			final ContextMenuAdapter adapter = new ContextMenuAdapter();
			int i = 0;
			int selectedIndex = -1;
			adapter.addItem(ContextMenuItem.createBuilder(getString(R.string.auto_zoom_none))
					.setSelected(false).createItem());
			if (!settings.AUTO_ZOOM_MAP.get()) {
				selectedIndex = 0;
			}
			i++;
			for (AutoZoomMap autoZoomMap : AutoZoomMap.values()) {
				adapter.addItem(ContextMenuItem.createBuilder(getString(autoZoomMap.name))
						.setSelected(false).createItem());
				if (selectedIndex == -1 && settings.AUTO_ZOOM_MAP_SCALE.get() == autoZoomMap) {
					selectedIndex = i;
				}
				i++;
			}
			if (selectedIndex == -1) {
				selectedIndex = 0;
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final int layout = R.layout.list_menu_item_native_singlechoice;

			final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, layout, R.id.text1,
					adapter.getItemNames()) {
				@NonNull
				@Override
				public View getView(final int position, View convertView, ViewGroup parent) {
					// User super class to create the View
					View v = convertView;
					if (v == null) {
						v = SettingsNavigationActivity.this.getLayoutInflater().inflate(layout, null);
					}
					final ContextMenuItem item = adapter.getItem(position);
					TextView tv = (TextView) v.findViewById(R.id.text1);
					tv.setText(item.getTitle());
					tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);

					return v;
				}
			};

			final int[] selectedPosition = {selectedIndex};
			builder.setSingleChoiceItems(listAdapter, selectedIndex, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int position) {
					selectedPosition[0] = position;
				}
			});
			builder.setTitle(R.string.auto_zoom_map)
					.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							int position = selectedPosition[0];
							if (position == 0) {
								settings.AUTO_ZOOM_MAP.set(false);
							} else {
								settings.AUTO_ZOOM_MAP.set(true);
								settings.AUTO_ZOOM_MAP_SCALE.set(AutoZoomMap.values()[position -1]);
							}
						}
					})
					.setNegativeButton(R.string.shared_string_cancel, null);

			builder.create().show();
			return true;
		} else if (preference == reliefFactorRouting) {
			final ApplicationMode am = settings.getApplicationMode();
			final ContextMenuAdapter adapter = new ContextMenuAdapter();
			int i = 0;
			int selectedIndex = -1;
			for (RoutingParameter p : reliefFactorParameters) {
				adapter.addItem(ContextMenuItem.createBuilder(getRoutinParameterTitle(this, p))
						.setSelected(false).createItem());
				if (isRoutingParameterSelected(settings, am, p)) {
					selectedIndex = i;
				}
				i++;
			}
			if (selectedIndex == -1) {
				selectedIndex = 0;
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final int layout = R.layout.list_menu_item_native_singlechoice;

			final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, layout, R.id.text1,
					adapter.getItemNames()) {
				@NonNull
				@Override
				public View getView(final int position, View convertView, ViewGroup parent) {
					// User super class to create the View
					View v = convertView;
					if (v == null) {
						v = SettingsNavigationActivity.this.getLayoutInflater().inflate(layout, null);
					}
					final ContextMenuItem item = adapter.getItem(position);
					TextView tv = (TextView) v.findViewById(R.id.text1);
					tv.setText(item.getTitle());
					tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);

					return v;
				}
			};

			final int[] selectedPosition = {selectedIndex};
			builder.setSingleChoiceItems(listAdapter, selectedIndex, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int position) {
					selectedPosition[0] = position;
				}
			});
			builder.setTitle(SettingsBaseActivity.getRoutingStringPropertyName(this, reliefFactorParameters.get(0).getGroup(),
					Algorithms.capitalizeFirstLetterAndLowercase(reliefFactorParameters.get(0).getGroup().replace('_', ' '))))
					.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							int position = selectedPosition[0];
							if (position >= 0 && position < reliefFactorParameters.size()) {
								for (int i = 0; i < reliefFactorParameters.size(); i++) {
									setRoutingParameterSelected(settings, am, reliefFactorParameters.get(i), i == position);
								}
								//mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
								//updateParameters();
							}
						}
					})
					.setNegativeButton(R.string.shared_string_cancel, null);

			builder.create().show();
			return true;
		} else if (preference == showAlarms) {
			showBooleanSettings(new String[] { getString(R.string.show_traffic_warnings), getString(R.string.show_pedestrian_warnings),
					getString(R.string.show_cameras), getString(R.string.show_lanes), getString(R.string.show_tunnels) }, new OsmandPreference[] { settings.SHOW_TRAFFIC_WARNINGS,
					settings.SHOW_PEDESTRIAN, settings.SHOW_CAMERAS, settings.SHOW_LANES, settings.SHOW_TUNNELS }, preference.getTitle());
			return true;
		} else if (preference == speakAlarms) {
			AlertDialog dlg = showBooleanSettings(new String[] { getString(R.string.speak_street_names),
					getString(R.string.speak_traffic_warnings), getString(R.string.speak_pedestrian),
					getString(R.string.speak_speed_limit), getString(R.string.speak_cameras),
					getString(R.string.announce_gpx_waypoints), getString(R.string.speak_favorites),
					getString(R.string.speak_poi) }, new OsmandPreference[] { settings.SPEAK_STREET_NAMES,
					settings.SPEAK_TRAFFIC_WARNINGS, settings.SPEAK_PEDESTRIAN, settings.SPEAK_SPEED_LIMIT,
					settings.SPEAK_SPEED_CAMERA, settings.ANNOUNCE_WPT, settings.ANNOUNCE_NEARBY_FAVORITES,
					settings.ANNOUNCE_NEARBY_POI }, preference.getTitle());
			final boolean initialSpeedCam = settings.SPEAK_SPEED_CAMERA.get();
			final boolean initialFavorites = settings.ANNOUNCE_NEARBY_FAVORITES.get();
			final boolean initialPOI = settings.ANNOUNCE_NEARBY_POI.get();
			// final boolean initialWpt = settings.ANNOUNCE_WPT.get();

			dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					if (settings.ANNOUNCE_NEARBY_POI.get() != initialPOI) {
						settings.SHOW_NEARBY_POI.set(settings.ANNOUNCE_NEARBY_POI.get());
					}
					if (settings.ANNOUNCE_NEARBY_FAVORITES.get() != initialFavorites) {
						settings.SHOW_NEARBY_FAVORITES.set(settings.ANNOUNCE_NEARBY_FAVORITES.get());
					}
					if (settings.ANNOUNCE_WPT.get()) {
						settings.SHOW_WPT.set(settings.ANNOUNCE_WPT.get());
					}
					if (!initialSpeedCam) {
						if (settings.SPEAK_SPEED_CAMERA.get()) {
							settings.SPEAK_SPEED_CAMERA.set(false);
							confirmSpeedCamerasDlg();
						}
					}

				}

			});
			return true;
		}
		return false;
	}
	
	private void confirmSpeedCamerasDlg() {
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
		bld.setMessage(R.string.confirm_usage_speed_cameras);
		bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				settings.SPEAK_SPEED_CAMERA.set(true);				
			}
		});
		bld.setNegativeButton(R.string.shared_string_cancel, null);
		bld.show();
	}

	public AlertDialog showBooleanSettings(String[] vals, final OsmandPreference<Boolean>[] prefs, final CharSequence title) {
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
		boolean[] checkedItems = new boolean[prefs.length];
		for (int i = 0; i < prefs.length; i++) {
			checkedItems[i] = prefs[i].get();
		}
		
		final boolean[] tempPrefs = new boolean[prefs.length];
		for (int i = 0; i < prefs.length; i++) {
			tempPrefs[i] = prefs[i].get();
		}
		
		bld.setMultiChoiceItems(vals, checkedItems, new OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				tempPrefs[which] = isChecked;
			}
		});
		
		bld.setTitle(title);
		
		bld.setNegativeButton(R.string.shared_string_cancel, null);
		
		bld.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
				for (int i = 0; i < prefs.length; i++) {
					prefs[i].set(tempPrefs[i]);
				}
		    }
		});
		
		return bld.show();
	}

	
}
