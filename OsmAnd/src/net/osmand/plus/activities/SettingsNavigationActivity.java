package net.osmand.plus.activities;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.media.AudioAttributes;
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
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmAndFormatter;
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
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.GeneralRouter.RoutingParameterType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
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
	private Preference defaultSpeed;
	private Preference defaultSpeedOnly;
	private ListPreference routerServicePreference;
	private ListPreference speedLimitExceed;
	
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
		settings = getMyApplication().getSettings();
		if (!settings.hasAvailableApplicationMode()) {
			Toast.makeText(this, R.string.turn_on_profile_desc, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		getToolbar().setTitle(R.string.routing_settings);
		createUI();
    }

	@Override
	protected void onResume() {
		super.onResume();
		if(getIntent() != null && getIntent().hasExtra(INTENT_SKIP_DIALOG)) {
			setSelectedAppMode(settings.getApplicationMode());
		} else if (selectedAppMode == null) {
			selectAppModeDialog().show();
		}
	}

	private void createUI() {
		addPreferencesFromResource(R.xml.navigation_settings);
		PreferenceScreen screen = getPreferenceScreen();
		RouteService[] vls = RouteService.getAvailableRouters(getMyApplication());
		String[] entries = new String[vls.length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = vls[i].getName();
		}

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


		if (settings.METRIC_SYSTEM.get() == OsmandSettings.MetricsConstants.KILOMETERS_AND_METERS) {
			Float[] speedLimitsKm = new Float[]{-10f, -7f, -5f, 0f, 5f, 7f, 10f, 15f, 20f};
			Float[] speedLimitsKmPos = new Float[]{0f, 5f, 7f, 10f, 15f, 20f};
			String[] speedNames = new String[speedLimitsKm.length];
			String[] speedNamesPos = new String[speedLimitsKmPos.length];
			for (int i = 0; i < speedLimitsKm.length; i++) {
				speedNames[i] = speedLimitsKm[i].intValue() + " " + getString(R.string.km_h);
			}
			for (int i = 0; i < speedLimitsKmPos.length; i++) {
				speedNamesPos[i] = speedLimitsKmPos[i].intValue() + " " + getString(R.string.km_h);
			}
			registerListPreference(settings.SPEED_LIMIT_EXCEED, screen, speedNames, speedLimitsKm);
			registerListPreference(settings.SWITCH_MAP_DIRECTION_TO_COMPASS, screen, speedNamesPos, speedLimitsKmPos);
		} else {
			Float[] speedLimitsMiles = new Float[]{-7f, -5f, -3f, 0f, 3f, 5f, 7f, 10f, 15f};
			Float[] speedLimitsMilesPos = new Float[]{0f, 3f, 5f, 7f, 10f, 15f};

			String[] speedNames = new String[speedLimitsMiles.length];
			for (int i = 0; i < speedNames.length; i++) {
				speedNames[i] = speedLimitsMiles[i].intValue() + " " + getString(R.string.mile_per_hour);
			}
			String[] speedNamesPos = new String[speedLimitsMilesPos.length];
			for (int i = 0; i < speedNamesPos.length; i++) {
				speedNamesPos[i] = speedLimitsMiles[i].intValue() + " " + getString(R.string.mile_per_hour);
			}
			registerListPreference(settings.SPEED_LIMIT_EXCEED, screen, speedNames, speedLimitsMiles);
			registerListPreference(settings.SWITCH_MAP_DIRECTION_TO_COMPASS, screen, speedNamesPos, speedLimitsMilesPos);
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



		registerBooleanPreference(settings.ENABLE_TIME_CONDITIONAL_ROUTING, screen);

		addTurnScreenOn((PreferenceGroup) screen.findPreference("turn_screen_on"));

		addVoicePrefs((PreferenceGroup) screen.findPreference("voice"));
	}

	private void addTurnScreenOn(PreferenceGroup screen) {
		Integer[] screenPowerSaveValues = new Integer[] { 0, 5, 10, 15, 20, 30, 45, 60 };
		String[] screenPowerSaveNames = new String[screenPowerSaveValues.length];
		screenPowerSaveNames[0] = getString(R.string.shared_string_never);
		for (int i = 1; i < screenPowerSaveValues.length; i++) {
			screenPowerSaveNames[i] = screenPowerSaveValues[i] + " "
					+ getString(R.string.int_seconds);
		}
		registerListPreference(settings.TURN_SCREEN_ON_TIME_INT, screen, screenPowerSaveNames, screenPowerSaveValues);
		registerBooleanPreference(settings.TURN_SCREEN_ON_SENSOR, screen);
	}

	private void reloadVoiceListPreference(PreferenceScreen screen) {
		String[] entries;
		String[] entrieValues;
		Set<String> voiceFiles = getMyApplication().getRoutingOptionsHelper().getVoiceFiles(this);
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

	private void addVoicePrefs(PreferenceGroup cat) {
		if (!Version.isBlackberry((OsmandApplication) getApplication())) {
			String[] streamTypes = new String[]{getString(R.string.voice_stream_music),
					getString(R.string.voice_stream_notification), getString(R.string.voice_stream_voice_call)};
					//getString(R.string.shared_string_default)};
			Integer[] streamIntTypes = new Integer[]{AudioManager.STREAM_MUSIC,
					AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_VOICE_CALL};
					//AudioManager.USE_DEFAULT_STREAM_TYPE};
			ListPreference lp = createListPreference(
					settings.AUDIO_STREAM_GUIDANCE, streamTypes, streamIntTypes , R.string.choose_audio_stream,
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
					// Sync corresponding AUDIO_USAGE value
					ApplicationMode mode = getMyApplication().getSettings().getApplicationMode();
					int stream = settings.AUDIO_STREAM_GUIDANCE.getModeValue(mode);
					if (stream == AudioManager.STREAM_MUSIC) {
						settings.AUDIO_USAGE.setModeValue(mode, AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE);
					} else if (stream == AudioManager.STREAM_NOTIFICATION) {
						settings.AUDIO_USAGE.setModeValue(mode, AudioAttributes.USAGE_NOTIFICATION);
					} else if (stream == AudioManager.STREAM_VOICE_CALL) {
						settings.AUDIO_USAGE.setModeValue(mode, AudioAttributes.USAGE_VOICE_COMMUNICATION);
					}

					// Sync DEFAULT value with CAR value, as we have other way to set it for now
					settings.AUDIO_STREAM_GUIDANCE.setModeValue(ApplicationMode.DEFAULT, settings.AUDIO_STREAM_GUIDANCE.getModeValue(ApplicationMode.CAR));
					settings.AUDIO_USAGE.setModeValue(ApplicationMode.DEFAULT, settings.AUDIO_USAGE.getModeValue(ApplicationMode.CAR));
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
		RouteService routeService = settings.getApplicationMode().getRouteService();
		if (routeService != RouteService.OSMAND) {
			if (routeService == RouteService.STRAIGHT) {
				defaultSpeedOnly = new Preference(this);
				defaultSpeedOnly.setTitle(R.string.default_speed_setting_title);
				defaultSpeedOnly.setSummary(R.string.default_speed_setting_descr);
				defaultSpeedOnly.setOnPreferenceClickListener(this);
				cat.addPreference(defaultSpeedOnly);
			}
			cat.addPreference(fastRoute);
		} else {
			ApplicationMode am = settings.getApplicationMode();
			GeneralRouter router = getRouter(getMyApplication().getRoutingConfig(), am);
			clearParameters();
			if (router != null) {
				GeneralRouterProfile routerProfile = router.getProfile();
				if (routerProfile != GeneralRouterProfile.PUBLIC_TRANSPORT) {
					defaultSpeed = new Preference(this);
					defaultSpeed.setTitle(R.string.default_speed_setting_title);
					defaultSpeed.setSummary(R.string.default_speed_setting_descr);
					defaultSpeed.setOnPreferenceClickListener(this);
					cat.addPreference(defaultSpeed);
				}

				Map<String, RoutingParameter> parameters = router.getParameters();
				if(parameters.containsKey(GeneralRouter.USE_SHORTEST_WAY)) {
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
					} else if (!param.equals(GeneralRouter.USE_SHORTEST_WAY) && !RoutingOptionsHelper.DRIVING_STYLE.equals(routingParameter.getGroup())) {
						others.add(routingParameter);
					}
				}
				if (avoidParameters.size() > 0) {
					avoidRouting = new Preference(this);
					avoidRouting.setTitle(R.string.avoid_in_routing_title);
					avoidRouting.setSummary(R.string.avoid_in_routing_descr_);
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

	public static String getRoutinParameterTitle(Context context, RoutingParameter routingParameter) {
		return SettingsBaseActivity.getRoutingStringPropertyName(context, routingParameter.getId(),
				routingParameter.getName());
	}

	public static boolean isRoutingParameterSelected(OsmandSettings settings, ApplicationMode am, RoutingParameter routingParameter) {
		final OsmandSettings.CommonPreference<Boolean> property =
				settings.getCustomRoutingBooleanProperty(routingParameter.getId(), routingParameter.getDefaultBoolean());
		if(am != null) {
			return property.getModeValue(am);
		} else {
			return property.get();
		}
	}

	public static void setRoutingParameterSelected(OsmandSettings settings, ApplicationMode am, String routingParameterId, boolean defaultBoolean, boolean isChecked) {
		final OsmandSettings.CommonPreference<Boolean> property = settings.getCustomRoutingBooleanProperty(routingParameterId, defaultBoolean);
		if (am != null) {
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
		GeneralRouter router = builder.getRouter(am.getRoutingProfile());
		if(router == null && am.getParent() != null) {
			router = builder.getRouter(am.getParent().getStringKey());
		}
		return router;
	}

	public void updateAllSettings() {	
		prepareRoutingPrefs(getPreferenceScreen());
		reloadVoiceListPreference(getPreferenceScreen());
		super.updateAllSettings();
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
				getMyApplication().initVoiceCommandPlayer(this, settings.APPLICATION_MODE.get(),
						false, null, true, false, false);
			}
			return true;
		}
		super.onPreferenceChange(preference, newValue);
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
			showBooleanSettings(this, vals, bls, preference.getTitle());
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
			builder.setSingleChoiceItems(listAdapter, selectedIndex, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int position) {
					selectedPosition[0] = position;
				}
			});
			builder.setTitle(R.string.auto_zoom_map)
					.setPositiveButton(R.string.shared_string_ok, new OnClickListener() {

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
			builder.setSingleChoiceItems(listAdapter, selectedIndex, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int position) {
					selectedPosition[0] = position;
				}
			});
			builder.setTitle(SettingsBaseActivity.getRoutingStringPropertyName(this, reliefFactorParameters.get(0).getGroup(),
					Algorithms.capitalizeFirstLetterAndLowercase(reliefFactorParameters.get(0).getGroup().replace('_', ' '))))
					.setPositiveButton(R.string.shared_string_ok, new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							int position = selectedPosition[0];
							if (position >= 0 && position < reliefFactorParameters.size()) {
								for (int i = 0; i < reliefFactorParameters.size(); i++) {
									RoutingParameter parameter = reliefFactorParameters.get(i);
									setRoutingParameterSelected(settings, am, parameter.getId(), parameter.getDefaultBoolean(), i == position);
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
			showBooleanSettings(this, new String[] { getString(R.string.show_traffic_warnings), getString(R.string.show_pedestrian_warnings),
					getString(R.string.show_cameras), getString(R.string.show_lanes), getString(R.string.show_tunnels) }, new OsmandPreference[] { settings.SHOW_TRAFFIC_WARNINGS,
					settings.SHOW_PEDESTRIAN, settings.SHOW_CAMERAS, settings.SHOW_LANES, settings.SHOW_TUNNELS }, preference.getTitle());
			return true;
		} else if (preference == speakAlarms) {
			AlertDialog dlg = showBooleanSettings(this, new String[] { getString(R.string.speak_street_names),
					getString(R.string.speak_traffic_warnings), getString(R.string.speak_pedestrian),
					getString(R.string.speak_speed_limit), getString(R.string.speak_cameras), getString(R.string.show_tunnels),
					getString(R.string.shared_string_gpx_waypoints), getString(R.string.speak_favorites),
					getString(R.string.speak_poi) }, new OsmandPreference[] { settings.SPEAK_STREET_NAMES,
					settings.SPEAK_TRAFFIC_WARNINGS, settings.SPEAK_PEDESTRIAN, settings.SPEAK_SPEED_LIMIT,
					settings.SPEAK_SPEED_CAMERA, settings.SPEAK_TUNNELS,
					settings.ANNOUNCE_WPT, settings.ANNOUNCE_NEARBY_FAVORITES,
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
		} else if (preference == defaultSpeed) {
			showSeekbarSettingsDialog(this, false, settings.getApplicationMode());
		} else if (preference == defaultSpeedOnly) {
			showSeekbarSettingsDialog(this, true, settings.getApplicationMode());
		}
		return false;
	}
	
	private void confirmSpeedCamerasDlg() {
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
		bld.setMessage(R.string.confirm_usage_speed_cameras);
		bld.setPositiveButton(R.string.shared_string_yes, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				settings.SPEAK_SPEED_CAMERA.set(true);				
			}
		});
		bld.setNegativeButton(R.string.shared_string_cancel, null);
		bld.show();
	}

	public static AlertDialog showBooleanSettings(Context ctx, String[] vals, final OsmandPreference<Boolean>[] prefs, final CharSequence title) {
		AlertDialog.Builder bld = new AlertDialog.Builder(ctx);
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
		
		bld.setPositiveButton(R.string.shared_string_ok, new OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
				for (int i = 0; i < prefs.length; i++) {
					prefs[i].set(tempPrefs[i]);
				}
		    }
		});
		
		return bld.show();
	}

	public static void showSeekbarSettingsDialog(Activity activity, final boolean defaultSpeedOnly, final ApplicationMode mode) {
		if (activity == null || mode == null) {
			return;
		}
		final OsmandApplication app = (OsmandApplication) activity.getApplication();
		final OsmandSettings settings = app.getSettings();

		GeneralRouter router = getRouter(app.getRoutingConfig(), mode);
		SpeedConstants units = settings.SPEED_SYSTEM.get();
		String speedUnits = units.toShortString(activity);
		final float[] ratio = new float[1];
		switch (units) {
			case MILES_PER_HOUR:
				ratio[0] = 3600 / OsmAndFormatter.METERS_IN_ONE_MILE;
				break;
			case KILOMETERS_PER_HOUR:
				ratio[0] = 3600 / OsmAndFormatter.METERS_IN_KILOMETER;
				break;
			case MINUTES_PER_KILOMETER:
				ratio[0] = 3600 / OsmAndFormatter.METERS_IN_KILOMETER;
				speedUnits = activity.getString(R.string.km_h);
				break;
			case NAUTICALMILES_PER_HOUR:
				ratio[0] = 3600 / OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE;
				break;
			case MINUTES_PER_MILE:
				ratio[0] = 3600 / OsmAndFormatter.METERS_IN_ONE_MILE;
				speedUnits = activity.getString(R.string.mile_per_hour);
				break;
			case METERS_PER_SECOND:
				ratio[0] = 1;
				break;
		}

		float settingsMinSpeed = settings.MIN_SPEED.get();
		float settingsMaxSpeed = settings.MAX_SPEED.get();

		final int[] defaultValue = {Math.round(mode.getDefaultSpeed() * ratio[0])};
		final int[] minValue = new int[1];
		final int[] maxValue = new int[1];
		final int min;
		final int max;
		if (defaultSpeedOnly) {
			minValue[0] = Math.round(1 * ratio[0]);
			maxValue[0] = Math.round(300 * ratio[0]);
			min = minValue[0];
			max = maxValue[0];
		} else {
			minValue[0] = Math.round((settingsMinSpeed > 0 ? settingsMinSpeed : router.getMinSpeed()) * ratio[0]);
			maxValue[0] = Math.round((settingsMaxSpeed > 0 ? settingsMaxSpeed : router.getMaxSpeed()) * ratio[0]);
			min = Math.round(router.getMinSpeed() * ratio[0] / 2f);
			max = Math.round(router.getMaxSpeed() * ratio[0] * 1.5f);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		boolean lightMode = app.getSettings().isLightContent();
		int themeRes = lightMode ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		View seekbarView = LayoutInflater.from(new ContextThemeWrapper(activity, themeRes))
				.inflate(R.layout.default_speed_dialog, null, false);
		builder.setView(seekbarView);
		builder.setPositiveButton(R.string.shared_string_ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mode.setDefaultSpeed(app, defaultValue[0] / ratio[0]);
				if (!defaultSpeedOnly) {
					settings.MIN_SPEED.set(minValue[0] / ratio[0]);
					settings.MAX_SPEED.set(maxValue[0] / ratio[0]);
				}
				RoutingHelper routingHelper = app.getRoutingHelper();
				if (mode.equals(routingHelper.getAppMode()) && (routingHelper.isRouteCalculated() || routingHelper.isRouteBeingCalculated())) {
					routingHelper.recalculateRouteDueToSettingsChange();
				}
			}
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setNeutralButton(R.string.shared_string_revert, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mode.resetDefaultSpeed(app);
				if (!defaultSpeedOnly) {
					settings.MIN_SPEED.set(0f);
					settings.MAX_SPEED.set(0f);
				}
				RoutingHelper routingHelper = app.getRoutingHelper();
				if (mode.equals(routingHelper.getAppMode()) && (routingHelper.isRouteCalculated() || routingHelper.isRouteBeingCalculated())) {
					routingHelper.recalculateRouteDueToSettingsChange();
				}
			}
		});

		if (!defaultSpeedOnly) {
			setupSpeedSlider(SpeedSliderType.DEFAULT_SPEED, speedUnits, minValue, defaultValue, maxValue, min, max, seekbarView);
			setupSpeedSlider(SpeedSliderType.MIN_SPEED, speedUnits, minValue, defaultValue, maxValue, min, max, seekbarView);
			setupSpeedSlider(SpeedSliderType.MAX_SPEED, speedUnits, minValue, defaultValue, maxValue, min, max, seekbarView);
		} else {
			setupSpeedSlider(SpeedSliderType.DEFAULT_SPEED_ONLY, speedUnits, minValue, defaultValue, maxValue, min, max, seekbarView);
			seekbarView.findViewById(R.id.default_speed_div).setVisibility(View.GONE);
			seekbarView.findViewById(R.id.default_speed_container).setVisibility(View.GONE);
			seekbarView.findViewById(R.id.max_speed_div).setVisibility(View.GONE);
			seekbarView.findViewById(R.id.max_speed_container).setVisibility(View.GONE);
		}

		builder.show();
	}

	private enum SpeedSliderType {
		DEFAULT_SPEED_ONLY,
		DEFAULT_SPEED,
		MIN_SPEED,
		MAX_SPEED,
	}

	private static void setupSpeedSlider(final SpeedSliderType type, String speedUnits,
										 final int[] minValue, final int[] defaultValue, final int[] maxValue,
										 final int min, final int max, View seekbarView) {
		View seekbarLayout;
		int titleId;
		final int[] speedValue;
		switch (type) {
			case DEFAULT_SPEED_ONLY:
				speedValue = defaultValue;
				seekbarLayout = seekbarView.findViewById(R.id.min_speed_layout);
				titleId = R.string.default_speed_setting_title;
				break;
			case MIN_SPEED:
				speedValue = minValue;
				seekbarLayout = seekbarView.findViewById(R.id.min_speed_layout);
				titleId = R.string.shared_string_min_speed;
				break;
			case MAX_SPEED:
				speedValue = maxValue;
				seekbarLayout = seekbarView.findViewById(R.id.max_speed_layout);
				titleId = R.string.shared_string_max_speed;
				break;
			default:
				speedValue = defaultValue;
				seekbarLayout = seekbarView.findViewById(R.id.default_speed_layout);
				titleId = R.string.default_speed_setting_title;
				break;
		}
		final SeekBar speedSeekBar = seekbarLayout.findViewById(R.id.speed_seekbar);
		final TextView speedTitleTv = seekbarLayout.findViewById(R.id.speed_title);
		final TextView speedMinTv = seekbarLayout.findViewById(R.id.speed_seekbar_min_text);
		final TextView speedMaxTv = seekbarLayout.findViewById(R.id.speed_seekbar_max_text);
		final TextView speedUnitsTv = seekbarLayout.findViewById(R.id.speed_units);
		final TextView speedTv = seekbarLayout.findViewById(R.id.speed_text);

		speedTitleTv.setText(titleId);
		speedMinTv.setText(String.valueOf(min));
		speedMaxTv.setText(String.valueOf(max));
		speedTv.setText(String.valueOf(speedValue[0]));
		speedUnitsTv.setText(speedUnits);
		speedSeekBar.setMax(max - min);
		speedSeekBar.setProgress(Math.max(speedValue[0] - min, 0));
		speedSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				int value = progress + min;
				switch (type) {
					case DEFAULT_SPEED:
					case DEFAULT_SPEED_ONLY:
						if (value > maxValue[0]) {
							value = maxValue[0];
							speedSeekBar.setProgress(Math.max(value - min, 0));
						} else if (value < minValue[0]) {
							value = minValue[0];
							speedSeekBar.setProgress(Math.max(value - min, 0));
						}
						break;
					case MIN_SPEED:
						if (value > defaultValue[0]) {
							value = defaultValue[0];
							speedSeekBar.setProgress(Math.max(value - min, 0));
						}
						break;
					case MAX_SPEED:
						if (value < defaultValue[0]) {
							value = defaultValue[0];
							speedSeekBar.setProgress(Math.max(value - min, 0));
						}
						break;
					default:
						break;
				}
				speedValue[0] = value;
				speedTv.setText(String.valueOf(value));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
	}
}
