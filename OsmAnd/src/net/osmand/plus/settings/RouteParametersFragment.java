package net.osmand.plus.settings;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.widget.ImageView;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.BooleanPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.activities.SettingsNavigationActivity;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.router.GeneralRouter;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.plus.activities.SettingsNavigationActivity.getRouter;

public class RouteParametersFragment extends BaseSettingsFragment {

	public static final String TAG = "RouteParametersFragment";

	private static final String AVOID_ROUTING_PARAMETER_PREFIX = "avoid_";
	private static final String PREFER_ROUTING_PARAMETER_PREFIX = "prefer_";
	private static final String ROUTE_PARAMETERS_INFO = "route_parameters_info";
	private static final String ROUTE_PARAMETERS_IMAGE = "route_parameters_image";
	private static final String RELIEF_SMOOTHNESS_FACTOR = "relief_smoothness_factor";

	private List<GeneralRouter.RoutingParameter> avoidParameters = new ArrayList<GeneralRouter.RoutingParameter>();
	private List<GeneralRouter.RoutingParameter> preferParameters = new ArrayList<GeneralRouter.RoutingParameter>();
	private List<GeneralRouter.RoutingParameter> reliefFactorParameters = new ArrayList<GeneralRouter.RoutingParameter>();
	private List<GeneralRouter.RoutingParameter> otherRoutingParameters = new ArrayList<GeneralRouter.RoutingParameter>();

	@Override
	protected int getPreferencesResId() {
		return R.xml.route_parameters;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar;
	}

	@Override
	protected int getToolbarTitle() {
		return R.string.route_parameters;
	}

	@Override
	protected void setupPreferences() {
		setupRouteParametersImage();

		Preference vehicleParametersInfo = findPreference(ROUTE_PARAMETERS_INFO);
		vehicleParametersInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));
		vehicleParametersInfo.setTitle(getString(R.string.route_parameters_info, getSelectedAppMode().toHumanString(getContext())));

		setupRoutingPrefs();
		setupTimeConditionalRoutingPref();
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
		timeConditionalRouting.setSummaryOn(R.string.shared_string_enable);
		timeConditionalRouting.setSummaryOff(R.string.shared_string_disable);
		getPreferenceScreen().addPreference(timeConditionalRouting);
	}

	private void setupRoutingPrefs() {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		PreferenceScreen screen = getPreferenceScreen();

		SwitchPreferenceEx fastRoute = createSwitchPreferenceEx(app.getSettings().FAST_ROUTE_MODE.getId(), R.string.fast_route_mode, R.layout.preference_with_descr_dialog_and_switch);
		fastRoute.setIcon(getRoutingPrefIcon(app.getSettings().FAST_ROUTE_MODE.getId()));
		fastRoute.setDescription(getString(R.string.fast_route_mode_descr));
		fastRoute.setSummaryOn(R.string.shared_string_enable);
		fastRoute.setSummaryOff(R.string.shared_string_disable);

		if (app.getSettings().getApplicationMode().getRouteService() != RouteProvider.RouteService.OSMAND) {
			screen.addPreference(fastRoute);
		} else {
			ApplicationMode am = app.getSettings().getApplicationMode();
			GeneralRouter router = getRouter(getMyApplication().getRoutingConfig(), am);
			clearParameters();
			if (router != null) {
				Map<String, GeneralRouter.RoutingParameter> parameters = router.getParameters();
				if (parameters.containsKey(GeneralRouter.USE_SHORTEST_WAY)) {
					screen.addPreference(fastRoute);
				}
				for (Map.Entry<String, GeneralRouter.RoutingParameter> e : parameters.entrySet()) {
					String param = e.getKey();
					GeneralRouter.RoutingParameter routingParameter = e.getValue();
					if (param.startsWith(AVOID_ROUTING_PARAMETER_PREFIX)) {
						avoidParameters.add(routingParameter);
					} else if (param.startsWith(PREFER_ROUTING_PARAMETER_PREFIX)) {
						preferParameters.add(routingParameter);
					} else if (RELIEF_SMOOTHNESS_FACTOR.equals(routingParameter.getGroup())) {
						reliefFactorParameters.add(routingParameter);
					} else if (!param.equals(GeneralRouter.USE_SHORTEST_WAY)
							&& !param.equals(GeneralRouter.VEHICLE_HEIGHT)
							&& !param.equals(GeneralRouter.VEHICLE_WEIGHT)
							&& !RoutingOptionsHelper.DRIVING_STYLE.equals(routingParameter.getGroup())) {
						otherRoutingParameters.add(routingParameter);
					}
				}
				if (avoidParameters.size() > 0) {
					MultiSelectBooleanPreference avoidRouting = new MultiSelectBooleanPreference(app);
					avoidRouting.setKey(AVOID_ROUTING_PARAMETER_PREFIX);
					avoidRouting.setTitle(R.string.avoid_in_routing_title);
					avoidRouting.setSummary(R.string.avoid_in_routing_descr);
					avoidRouting.setDescription(R.string.avoid_in_routing_descr);
					avoidRouting.setLayoutResource(R.layout.preference_with_descr);
					avoidRouting.setIcon(getRoutingPrefIcon(AVOID_ROUTING_PARAMETER_PREFIX));

					String[] entries = new String[avoidParameters.size()];
					String[] prefsIds = new String[avoidParameters.size()];
					Set<String> enabledPrefsIds = new HashSet<>();

					for (int i = 0; i < avoidParameters.size(); i++) {
						GeneralRouter.RoutingParameter p = avoidParameters.get(i);
						BooleanPreference booleanRoutingPref = (BooleanPreference) settings.getCustomRoutingBooleanProperty(p.getId(), p.getDefaultBoolean());

						entries[i] = SettingsBaseActivity.getRoutingStringPropertyName(app, p.getId(), p.getName());
						prefsIds[i] = booleanRoutingPref.getId();

						if (booleanRoutingPref.get()) {
							enabledPrefsIds.add(booleanRoutingPref.getId());
						}
					}

					avoidRouting.setEntries(entries);
					avoidRouting.setEntryValues(prefsIds);
					avoidRouting.setValues(enabledPrefsIds);

					screen.addPreference(avoidRouting);
				}
				if (preferParameters.size() > 0) {
					Preference preferRouting = new Preference(app);
					preferRouting.setKey(PREFER_ROUTING_PARAMETER_PREFIX);
					preferRouting.setTitle(R.string.prefer_in_routing_title);
					preferRouting.setSummary(R.string.prefer_in_routing_descr);
					preferRouting.setLayoutResource(R.layout.preference_with_descr);
					preferRouting.setIconSpaceReserved(true);
					screen.addPreference(preferRouting);
				}
				if (reliefFactorParameters.size() > 0) {
					String defaultTitle = Algorithms.capitalizeFirstLetterAndLowercase(RELIEF_SMOOTHNESS_FACTOR.replace('_', ' '));
					String title = SettingsBaseActivity.getRoutingStringPropertyName(app, RELIEF_SMOOTHNESS_FACTOR, defaultTitle);

					Object[] entryValues = new Object[reliefFactorParameters.size()];
					String[] entries = new String[entryValues.length];

					String selectedParameterId = null;
					for (int i = 0; i < reliefFactorParameters.size(); i++) {
						GeneralRouter.RoutingParameter parameter = reliefFactorParameters.get(i);
						entryValues[i] = parameter.getId();
						entries[i] = SettingsNavigationActivity.getRoutinParameterTitle(app, parameter);
						if (SettingsNavigationActivity.isRoutingParameterSelected(settings, am, parameter)) {
							selectedParameterId = parameter.getId();
						}
					}

					ListPreferenceEx reliefFactorRouting = createListPreferenceEx(RELIEF_SMOOTHNESS_FACTOR, entries, entryValues, title, R.layout.preference_with_descr);
					reliefFactorRouting.setPersistent(false);
					reliefFactorRouting.setValue(selectedParameterId);
					reliefFactorRouting.setDescription(R.string.relief_smoothness_factor_descr);
					reliefFactorRouting.setIconSpaceReserved(true);

					screen.addPreference(reliefFactorRouting);
				}
				for (GeneralRouter.RoutingParameter p : otherRoutingParameters) {
					String title = SettingsBaseActivity.getRoutingStringPropertyName(app, p.getId(), p.getName());
					String description = SettingsBaseActivity.getRoutingStringPropertyDescription(app, p.getId(), p.getDescription());

					if (p.getType() == GeneralRouter.RoutingParameterType.BOOLEAN) {
						OsmandSettings.OsmandPreference pref = settings.getCustomRoutingBooleanProperty(p.getId(), p.getDefaultBoolean());

						SwitchPreferenceEx switchPreferenceEx = (SwitchPreferenceEx) createSwitchPreferenceEx(pref.getId(), title, description, R.layout.preference_with_descr_dialog_and_switch);
						switchPreferenceEx.setDescription(description);
						switchPreferenceEx.setIcon(getRoutingPrefIcon(p.getId()));
						switchPreferenceEx.setSummaryOn(R.string.shared_string_enable);
						switchPreferenceEx.setSummaryOff(R.string.shared_string_disable);
						switchPreferenceEx.setIconSpaceReserved(true);

						screen.addPreference(switchPreferenceEx);
					} else {
						Object[] vls = p.getPossibleValues();
						String[] svlss = new String[vls.length];
						int i = 0;
						for (Object o : vls) {
							svlss[i++] = o.toString();
						}
						OsmandSettings.OsmandPreference pref = settings.getCustomRoutingProperty(p.getId(), p.getType() == GeneralRouter.RoutingParameterType.NUMERIC ? "0.0" : "-");

						ListPreferenceEx listPreferenceEx = (ListPreferenceEx) createListPreferenceEx(pref.getId(), p.getPossibleValueDescriptions(), svlss, title, R.layout.preference_with_descr);
						listPreferenceEx.setDescription(description);
						listPreferenceEx.setIcon(getRoutingPrefIcon(p.getId()));
						listPreferenceEx.setIconSpaceReserved(true);

						screen.addPreference(listPreferenceEx);
					}
				}
			}
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();

		if (RELIEF_SMOOTHNESS_FACTOR.equals(key)) {
			if (newValue instanceof String) {
				String selectedParameterId = (String) newValue;
				for (GeneralRouter.RoutingParameter parameter : reliefFactorParameters) {
					String parameterId = parameter.getId();
					SettingsNavigationActivity.setRoutingParameterSelected(settings, getSelectedAppMode(), parameterId, parameter.getDefaultBoolean(), parameterId.equals(selectedParameterId));
				}
			}
			return true;
		}

		return super.onPreferenceChange(preference, newValue);
	}

	private void clearParameters() {
		avoidParameters.clear();
		preferParameters.clear();
		reliefFactorParameters.clear();
		otherRoutingParameters.clear();
	}

	private Drawable getRoutingPrefIcon(String prefId) {
		switch (prefId) {
			case GeneralRouter.ALLOW_PRIVATE:
				return getIcon(R.drawable.ic_action_private_access);
			case AVOID_ROUTING_PARAMETER_PREFIX:
				return getContentIcon(R.drawable.ic_action_alert);
			case "fast_route_mode":
				return getIcon(R.drawable.ic_action_fastest_route);
			case "enable_time_conditional_routing":
				return getContentIcon(R.drawable.ic_action_road_works_dark);
			default:
				return null;
		}
	}
}