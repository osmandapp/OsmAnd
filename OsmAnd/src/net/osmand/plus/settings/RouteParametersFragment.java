package net.osmand.plus.settings;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import net.osmand.GPXUtilities;
import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.BooleanPreference;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.activities.SettingsNavigationActivity;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.GeneralRouter.RoutingParameterType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.DRIVING_STYLE;

public class RouteParametersFragment extends BaseSettingsFragment implements OnPreferenceChanged {

	public static final String TAG = RouteParametersFragment.class.getSimpleName();

	private static final String AVOID_ROUTING_PARAMETER_PREFIX = "avoid_";
	private static final String PREFER_ROUTING_PARAMETER_PREFIX = "prefer_";
	private static final String ROUTE_PARAMETERS_INFO = "route_parameters_info";
	private static final String ROUTE_PARAMETERS_IMAGE = "route_parameters_image";
	private static final String RELIEF_SMOOTHNESS_FACTOR = "relief_smoothness_factor";

	private List<RoutingParameter> avoidParameters = new ArrayList<RoutingParameter>();
	private List<RoutingParameter> preferParameters = new ArrayList<RoutingParameter>();
	private List<RoutingParameter> drivingStyleParameters = new ArrayList<RoutingParameter>();
	private List<RoutingParameter> reliefFactorParameters = new ArrayList<RoutingParameter>();
	private List<RoutingParameter> otherRoutingParameters = new ArrayList<RoutingParameter>();

	private StateChangedListener<Boolean> booleanRoutingPrefListener;
	private StateChangedListener<String> customRoutingPrefListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		booleanRoutingPrefListener = new StateChangedListener<Boolean>() {
			@Override
			public void stateChanged(Boolean change) {
				recalculateRoute();
			}
		};
		customRoutingPrefListener = new StateChangedListener<String>() {
			@Override
			public void stateChanged(String change) {
				recalculateRoute();
			}
		};
	}

	@Override
	protected void setupPreferences() {
		setupRouteParametersImage();

		Preference routeParametersInfo = findPreference(ROUTE_PARAMETERS_INFO);
		routeParametersInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));
		routeParametersInfo.setTitle(getString(R.string.route_parameters_info, getSelectedAppMode().toHumanString()));

		setupRoutingPrefs();
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
			return;
		}
		String key = preference.getKey();
		if (ROUTE_PARAMETERS_INFO.equals(key)) {
			int colorRes = isNightMode() ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
			holder.itemView.setBackgroundColor(ContextCompat.getColor(app, colorRes));
		} else if (ROUTE_PARAMETERS_IMAGE.equals(key)) {
			ImageView imageView = (ImageView) holder.itemView.findViewById(R.id.device_image);
			if (imageView != null) {
				int bgResId = isNightMode() ? R.drawable.img_settings_device_bottom_dark : R.drawable.img_settings_device_bottom_light;
				Drawable layerDrawable = app.getUIUtilities().getLayeredIcon(bgResId, R.drawable.img_settings_sreen_route_parameters);

				imageView.setImageDrawable(layerDrawable);
			}
		}
	}

	private void setupRouteParametersImage() {
		Preference routeParametersImage = findPreference(ROUTE_PARAMETERS_IMAGE);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
			routeParametersImage.setVisible(false);
		}
	}

	private void setupTimeConditionalRoutingPref() {
		SwitchPreferenceEx timeConditionalRouting = createSwitchPreferenceEx(settings.ENABLE_TIME_CONDITIONAL_ROUTING.getId(),
				R.string.temporary_conditional_routing, R.layout.preference_with_descr_dialog_and_switch);
		timeConditionalRouting.setIcon(getRoutingPrefIcon(settings.ENABLE_TIME_CONDITIONAL_ROUTING.getId()));
		timeConditionalRouting.setSummaryOn(R.string.shared_string_on);
		timeConditionalRouting.setSummaryOff(R.string.shared_string_off);
		getPreferenceScreen().addPreference(timeConditionalRouting);
	}

	private void setupRoutingPrefs() {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		PreferenceScreen screen = getPreferenceScreen();

		ApplicationMode am = getSelectedAppMode();

		SwitchPreferenceEx fastRoute = createSwitchPreferenceEx(app.getSettings().FAST_ROUTE_MODE.getId(), R.string.fast_route_mode, R.layout.preference_with_descr_dialog_and_switch);
		fastRoute.setIcon(getRoutingPrefIcon(app.getSettings().FAST_ROUTE_MODE.getId()));
		fastRoute.setDescription(getString(R.string.fast_route_mode_descr));
		fastRoute.setSummaryOn(R.string.shared_string_on);
		fastRoute.setSummaryOff(R.string.shared_string_off);

		float defaultAllowedDeviation = RoutingHelper.getDefaultAllowedDeviation(settings, am,
				RoutingHelper.getPosTolerance(0));
		setupSelectRouteRecalcDistance(screen, defaultAllowedDeviation);

		if (am.getRouteService() == RouteProvider.RouteService.OSMAND){
			GeneralRouter router = app.getRouter(am);
			clearParameters();
			if (router != null) {
				Map<String, RoutingParameter> parameters = router.getParameters();
				if (!am.isDerivedRoutingFrom(ApplicationMode.CAR)) {
					screen.addPreference(fastRoute);
				}
				for (Map.Entry<String, RoutingParameter> e : parameters.entrySet()) {
					String param = e.getKey();
					RoutingParameter routingParameter = e.getValue();
					if (param.startsWith(AVOID_ROUTING_PARAMETER_PREFIX)) {
						avoidParameters.add(routingParameter);
					} else if (param.startsWith(PREFER_ROUTING_PARAMETER_PREFIX)) {
						preferParameters.add(routingParameter);
					} else if (RELIEF_SMOOTHNESS_FACTOR.equals(routingParameter.getGroup())) {
						reliefFactorParameters.add(routingParameter);
					} else if (DRIVING_STYLE.equals(routingParameter.getGroup())) {
						drivingStyleParameters.add(routingParameter);
					} else if ((!param.equals(GeneralRouter.USE_SHORTEST_WAY) || am.isDerivedRoutingFrom(ApplicationMode.CAR))
							&& !param.equals(GeneralRouter.VEHICLE_HEIGHT)
							&& !param.equals(GeneralRouter.VEHICLE_WEIGHT)
							&& !param.equals(GeneralRouter.VEHICLE_WIDTH)) {
						otherRoutingParameters.add(routingParameter);
					}
				}
				if (drivingStyleParameters.size() > 0) {
					ListPreferenceEx drivingStyleRouting = createRoutingBooleanListPreference(DRIVING_STYLE, drivingStyleParameters);
					screen.addPreference(drivingStyleRouting);
				}
				if (avoidParameters.size() > 0) {
					String title;
					String description;
					if (am.isDerivedRoutingFrom(ApplicationMode.PUBLIC_TRANSPORT)) {
						title = getString(R.string.avoid_pt_types);
						description = getString(R.string.avoid_pt_types_descr);
					} else {
						title = getString(R.string.impassable_road);
						description = getString(R.string.avoid_in_routing_descr_);
					}
					MultiSelectBooleanPreference avoidRouting = createRoutingBooleanMultiSelectPref(AVOID_ROUTING_PARAMETER_PREFIX, title, description, avoidParameters);
					screen.addPreference(avoidRouting);
				}
				if (preferParameters.size() > 0) {
					String title = getString(R.string.prefer_in_routing_title);
					String descr = getString(R.string.prefer_in_routing_descr);
					MultiSelectBooleanPreference preferRouting = createRoutingBooleanMultiSelectPref(PREFER_ROUTING_PARAMETER_PREFIX, title, descr, preferParameters);
					screen.addPreference(preferRouting);
				}
				if (reliefFactorParameters.size() > 0) {
					ListPreferenceEx reliefFactorRouting = createRoutingBooleanListPreference(RELIEF_SMOOTHNESS_FACTOR, reliefFactorParameters);
					reliefFactorRouting.setDescription(R.string.relief_smoothness_factor_descr);

					screen.addPreference(reliefFactorRouting);
				}
				for (RoutingParameter p : otherRoutingParameters) {
					String title = SettingsBaseActivity.getRoutingStringPropertyName(app, p.getId(), p.getName());
					String description = SettingsBaseActivity.getRoutingStringPropertyDescription(app, p.getId(), p.getDescription());

					if (p.getType() == RoutingParameterType.BOOLEAN) {
						OsmandSettings.OsmandPreference pref = settings.getCustomRoutingBooleanProperty(p.getId(), p.getDefaultBoolean());

						SwitchPreferenceEx switchPreferenceEx = (SwitchPreferenceEx) createSwitchPreferenceEx(pref.getId(), title, description, R.layout.preference_with_descr_dialog_and_switch);
						switchPreferenceEx.setDescription(description);
						switchPreferenceEx.setIcon(getRoutingPrefIcon(p.getId()));
						switchPreferenceEx.setSummaryOn(R.string.shared_string_on);
						switchPreferenceEx.setSummaryOff(R.string.shared_string_off);

						screen.addPreference(switchPreferenceEx);
					} else {
						Object[] vls = p.getPossibleValues();
						String[] svlss = new String[vls.length];
						int i = 0;
						for (Object o : vls) {
							svlss[i++] = o.toString();
						}
						OsmandSettings.OsmandPreference pref = settings.getCustomRoutingProperty(p.getId(), p.getType() == RoutingParameterType.NUMERIC ? "0.0" : "-");

						ListPreferenceEx listPreferenceEx = (ListPreferenceEx) createListPreferenceEx(pref.getId(), p.getPossibleValueDescriptions(), svlss, title, R.layout.preference_with_descr);
						listPreferenceEx.setDescription(description);
						listPreferenceEx.setIcon(getRoutingPrefIcon(p.getId()));

						screen.addPreference(listPreferenceEx);
					}
				}
			}
			setupTimeConditionalRoutingPref();
		} else if (am.getRouteService() == RouteProvider.RouteService.BROUTER) {
			screen.addPreference(fastRoute);
			setupTimeConditionalRoutingPref();
		} else if (am.getRouteService() == RouteProvider.RouteService.STRAIGHT) {
			Preference straightAngle = new Preference(app.getApplicationContext());
			straightAngle.setPersistent(false);
			straightAngle.setKey(settings.ROUTE_STRAIGHT_ANGLE.getId());
			straightAngle.setTitle(getString(R.string.recalc_angle_dialog_title));
			straightAngle.setSummary(String.format(getString(R.string.shared_string_angle_param), (int) am.getStrAngle()));
			straightAngle.setLayoutResource(R.layout.preference_with_descr);
			straightAngle.setIcon(getRoutingPrefIcon("routing_recalc_distance")); //TODO change for appropriate icon when available
			getPreferenceScreen().addPreference(straightAngle);
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals(settings.ROUTE_STRAIGHT_ANGLE.getId())) {
			showSeekbarSettingsDialog(getActivity(), getSelectedAppMode());
		}
		return super.onPreferenceClick(preference);
	}

	private void showSeekbarSettingsDialog(Activity activity, final ApplicationMode mode) {
		if (activity == null || mode == null) {
			return;
		}
		final OsmandApplication app = (OsmandApplication) activity.getApplication();
		final float[] angleValue = new float[] {mode.getStrAngle()};
		boolean nightMode = !app.getSettings().isLightContentForMode(mode);
		Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		View seekbarView = LayoutInflater.from(themedContext).inflate(R.layout.recalculation_angle_dialog, null, false);
		builder.setView(seekbarView);
		builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mode.setStrAngle(angleValue[0]);
				updateAllSettings();
				RoutingHelper routingHelper = app.getRoutingHelper();
				if (mode.equals(routingHelper.getAppMode()) && (routingHelper.isRouteCalculated() || routingHelper.isRouteBeingCalculated())) {
					routingHelper.recalculateRouteDueToSettingsChange();
				}
			}
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);

		int selectedModeColor = ContextCompat.getColor(app, mode.getIconColorInfo().getColor(nightMode));
		setupAngleSlider(angleValue, seekbarView, nightMode, selectedModeColor);
		builder.show();
	}

	private static void setupAngleSlider(final float[] angleValue,
	                                     View seekbarView,
	                                     final boolean nightMode,
	                                     final int activeColor) {

		final SeekBar angleBar = seekbarView.findViewById(R.id.angle_seekbar);
		final TextView angleTv = seekbarView.findViewById(R.id.angle_text);

		angleTv.setText(String.valueOf(angleValue[0]));
		angleBar.setProgress((int) angleValue[0]);
		angleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				int value = progress - (progress % 5);
				angleValue[0] = value;
				angleTv.setText(String.valueOf(value));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});
		UiUtilities.setupSeekBar(angleBar, activeColor, nightMode);
	}

	private void setupSelectRouteRecalcDistance(PreferenceScreen screen, float defaultAllowedDeviation) {
		Float[] entryValues;
		OsmandSettings settings = app.getSettings();
		OsmandSettings.MetricsConstants mc = settings.METRIC_SYSTEM.get();
		if (mc == OsmandSettings.MetricsConstants.KILOMETERS_AND_METERS) {
			entryValues = new Float[] {-1.0f, 0.f, 10.f, 20.0f, 30.0f, 50.0f, 100.0f, 200.0f, 500.0f, 1000.0f, 1500.0f};
		} else {
			entryValues = new Float[] {-1.0f, 0.f, 9.1f, 18.3f, 30.5f, 45.7f, 91.5f, 183.0f, 482.0f, 965.0f, 1609.0f};
		}

		String[] entries = new String[entryValues.length];
		entries[0] = getString(R.string.no_recalculation_setting);
		String defaultDistance =  defaultAllowedDeviation < 0 ? getString(R.string.no_recalculation_setting) :
				OsmAndFormatter.getFormattedDistance(defaultAllowedDeviation , app, false);
		entries[1] = String.format(getString(R.string.shared_string_app_default_w_val), defaultDistance);

		for (int i = 2; i < entryValues.length; i++) {
			entries[i] = OsmAndFormatter.getFormattedDistance(entryValues[i], app, false);
		}

		ListPreferenceEx routeRecalculationDist = createListPreferenceEx(settings.ROUTE_RECALCULATION_DISTANCE.getId(),
				entries, entryValues, R.string.route_recalculation_dist_title, R.layout.preference_with_descr);
		routeRecalculationDist.setEntries(entries);
		routeRecalculationDist.setEntryValues(entryValues);
		routeRecalculationDist.setDescription(getString(R.string.route_recalculation_dist_descr));
		routeRecalculationDist.setIcon(getRoutingPrefIcon("routing_recalc_distance"));


		screen.addPreference(routeRecalculationDist);
	}

	@Override
	public void onResume() {
		super.onResume();
		addRoutingPrefListeners();
	}

	@Override
	public void onPause() {
		super.onPause();
		removeRoutingPrefListeners();
	}

	@Override
	public void onAppModeChanged(ApplicationMode appMode) {
		removeRoutingPrefListeners();
		super.onAppModeChanged(appMode);
		addRoutingPrefListeners();
	}

	private void addRoutingPrefListeners() {
		settings.FAST_ROUTE_MODE.addListener(booleanRoutingPrefListener);
		settings.ENABLE_TIME_CONDITIONAL_ROUTING.addListener(booleanRoutingPrefListener);

		for (RoutingParameter parameter : otherRoutingParameters) {
			if (parameter.getType() == RoutingParameterType.BOOLEAN) {
				OsmandSettings.CommonPreference<Boolean> pref = settings.getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
				pref.addListener(booleanRoutingPrefListener);
			} else {
				OsmandSettings.CommonPreference<String> pref = settings.getCustomRoutingProperty(parameter.getId(), parameter.getType() == RoutingParameterType.NUMERIC ? "0.0" : "-");
				pref.addListener(customRoutingPrefListener);
			}
		}
	}

	private void removeRoutingPrefListeners() {
		settings.FAST_ROUTE_MODE.removeListener(booleanRoutingPrefListener);
		settings.ENABLE_TIME_CONDITIONAL_ROUTING.removeListener(booleanRoutingPrefListener);

		for (RoutingParameter parameter : otherRoutingParameters) {
			if (parameter.getType() == RoutingParameterType.BOOLEAN) {
				OsmandSettings.CommonPreference<Boolean> pref = settings.getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
				pref.removeListener(booleanRoutingPrefListener);
			} else {
				OsmandSettings.CommonPreference<String> pref = settings.getCustomRoutingProperty(parameter.getId(), parameter.getType() == RoutingParameterType.NUMERIC ? "0.0" : "-");
				pref.removeListener(customRoutingPrefListener);
			}
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();

		if ((RELIEF_SMOOTHNESS_FACTOR.equals(key) || DRIVING_STYLE.equals(key)) && newValue instanceof String) {
			ApplicationMode appMode = getSelectedAppMode();
			String selectedParameterId = (String) newValue;
			List<RoutingParameter> routingParameters = DRIVING_STYLE.equals(key) ? drivingStyleParameters : reliefFactorParameters;
			for (RoutingParameter p : routingParameters) {
				String parameterId = p.getId();
				SettingsNavigationActivity.setRoutingParameterSelected(settings, appMode, parameterId, p.getDefaultBoolean(), parameterId.equals(selectedParameterId));
			}
			recalculateRoute();
			return true;
		} else if ("prouting_short_way".equals(key) && newValue instanceof Boolean) {
			return app.getSettings().FAST_ROUTE_MODE.setModeValue(getSelectedAppMode(), !(Boolean) newValue);
		} else if (settings.ROUTE_RECALCULATION_DISTANCE.getId().equals(key) && newValue instanceof Float) {
			if ((float) newValue == -1.f) {
				settings.DISABLE_OFFROUTE_RECALC.setModeValue(getSelectedAppMode(), true);
			} else {
				settings.DISABLE_OFFROUTE_RECALC.setModeValue(getSelectedAppMode(), false);
			}
		}

		return super.onPreferenceChange(preference, newValue);
	}

	@Override
	public void onPreferenceChanged(String prefId) {
		if (AVOID_ROUTING_PARAMETER_PREFIX.equals(prefId) || PREFER_ROUTING_PARAMETER_PREFIX.equals(prefId)) {
			recalculateRoute();
		}
	}

	private ListPreferenceEx createRoutingBooleanListPreference(String groupKey, List<RoutingParameter> routingParameters) {
		String defaultTitle = Algorithms.capitalizeFirstLetterAndLowercase(groupKey.replace('_', ' '));
		String title = SettingsBaseActivity.getRoutingStringPropertyName(app, groupKey, defaultTitle);
		ApplicationMode am = getSelectedAppMode();

		Object[] entryValues = new Object[routingParameters.size()];
		String[] entries = new String[entryValues.length];

		String selectedParameterId = null;
		for (int i = 0; i < routingParameters.size(); i++) {
			RoutingParameter parameter = routingParameters.get(i);
			entryValues[i] = parameter.getId();
			entries[i] = SettingsNavigationActivity.getRoutinParameterTitle(app, parameter);
			if (SettingsNavigationActivity.isRoutingParameterSelected(settings, am, parameter)) {
				selectedParameterId = parameter.getId();
			}
		}

		ListPreferenceEx routingListPref = createListPreferenceEx(groupKey, entries, entryValues, title, R.layout.preference_with_descr);
		routingListPref.setPersistent(false);
		routingListPref.setValue(selectedParameterId);
		routingListPref.setIcon(getRoutingPrefIcon(groupKey));

		return routingListPref;
	}

	private MultiSelectBooleanPreference createRoutingBooleanMultiSelectPref(String groupKey, String title, String descr, List<RoutingParameter> routingParameters) {
		MultiSelectBooleanPreference multiSelectPref = new MultiSelectBooleanPreference(app);
		multiSelectPref.setKey(groupKey);
		multiSelectPref.setTitle(title);
		multiSelectPref.setSummary(descr);
		multiSelectPref.setDescription(descr);
		multiSelectPref.setLayoutResource(R.layout.preference_with_descr);
		multiSelectPref.setIcon(getRoutingPrefIcon(groupKey));
		multiSelectPref.setIconSpaceReserved(true);

		String[] entries = new String[routingParameters.size()];
		String[] prefsIds = new String[routingParameters.size()];
		Set<String> enabledPrefsIds = new HashSet<>();

		ApplicationMode selectedMode = getSelectedAppMode();
		for (int i = 0; i < routingParameters.size(); i++) {
			RoutingParameter p = routingParameters.get(i);
			BooleanPreference booleanRoutingPref = (BooleanPreference) settings.getCustomRoutingBooleanProperty(p.getId(), p.getDefaultBoolean());

			entries[i] = SettingsBaseActivity.getRoutingStringPropertyName(app, p.getId(), p.getName());
			prefsIds[i] = booleanRoutingPref.getId();

			if (booleanRoutingPref.getModeValue(selectedMode)) {
				enabledPrefsIds.add(booleanRoutingPref.getId());
			}
		}

		multiSelectPref.setEntries(entries);
		multiSelectPref.setEntryValues(prefsIds);
		multiSelectPref.setValues(enabledPrefsIds);

		return multiSelectPref;
	}

	private void recalculateRoute() {
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (getSelectedAppMode().equals(routingHelper.getAppMode())
				&& (routingHelper.isRouteCalculated() || routingHelper.isRouteBeingCalculated())) {
			routingHelper.recalculateRouteDueToSettingsChange();
		}
	}

	private void clearParameters() {
		avoidParameters.clear();
		preferParameters.clear();
		drivingStyleParameters.clear();
		reliefFactorParameters.clear();
		otherRoutingParameters.clear();
	}

	private Drawable getRoutingPrefIcon(String prefId) {
		switch (prefId) {
			case GeneralRouter.ALLOW_PRIVATE:
				return getPersistentPrefIcon(R.drawable.ic_action_private_access);
			case GeneralRouter.USE_SHORTEST_WAY:
				return getPersistentPrefIcon(R.drawable.ic_action_fuel);
			case GeneralRouter.ALLOW_MOTORWAYS:
				Drawable disabled = getContentIcon(R.drawable.ic_action_avoid_motorways);
				Drawable enabled = getActiveIcon(R.drawable.ic_action_motorways);
				return getPersistentPrefIcon(enabled, disabled);
			case GeneralRouter.USE_HEIGHT_OBSTACLES:
			case RELIEF_SMOOTHNESS_FACTOR:
				return getPersistentPrefIcon(R.drawable.ic_action_elevation);
			case AVOID_ROUTING_PARAMETER_PREFIX:
				return getPersistentPrefIcon(R.drawable.ic_action_alert);
			case DRIVING_STYLE:
				return getPersistentPrefIcon(R.drawable.ic_action_bicycle_dark);
			case "fast_route_mode":
				return getPersistentPrefIcon(R.drawable.ic_action_fastest_route);
			case "enable_time_conditional_routing":
				return getPersistentPrefIcon(R.drawable.ic_action_road_works_dark);
			case "routing_recalc_distance":
				return getPersistentPrefIcon(R.drawable.ic_action_minimal_distance);

			default:
				return null;
		}
	}
}