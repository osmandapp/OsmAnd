package net.osmand.plus.settings;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.preference.Preference;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.router.GeneralRouter;

import java.util.Map;

import static net.osmand.plus.activities.SettingsNavigationActivity.getRouter;
import static net.osmand.plus.activities.SettingsNavigationActivity.showSeekbarSettingsDialog;

public class VehicleParametersFragment extends BaseSettingsFragment implements OnPreferenceChanged {

	public static final String TAG = VehicleParametersFragment.class.getSimpleName();

	@Override
	protected void setupPreferences() {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}

		Preference vehicleParametersInfo = findPreference("vehicle_parameters_info");
		vehicleParametersInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));
		vehicleParametersInfo.setTitle(getString(R.string.route_parameters_info, getSelectedAppMode().toHumanString(getContext())));

		RouteService routeService = getSelectedAppMode().getRouteService();
		if (routeService == RouteService.OSMAND) {
			GeneralRouter router = getRouter(app.getRoutingConfig(), getSelectedAppMode());
			if (router != null) {
				Map<String, GeneralRouter.RoutingParameter> parameters = router.getParameters();

				GeneralRouter.RoutingParameter vehicleHeight = parameters.get(GeneralRouter.VEHICLE_HEIGHT);
				if (vehicleHeight != null) {
					setupCustomRoutingPropertyPref(vehicleHeight);
				}
				GeneralRouter.RoutingParameter vehicleWeight = parameters.get(GeneralRouter.VEHICLE_WEIGHT);
				if (vehicleWeight != null) {
					setupCustomRoutingPropertyPref(vehicleWeight);
				}
				GeneralRouter.RoutingParameter vehicleWidth = parameters.get(GeneralRouter.VEHICLE_WIDTH);
				if (vehicleWidth != null) {
					setupCustomRoutingPropertyPref(vehicleWidth);
				}
				if (router.getProfile() != GeneralRouter.GeneralRouterProfile.PUBLIC_TRANSPORT) {
					setupDefaultSpeedPref();
				}
			}
		} else if (routeService == RouteService.STRAIGHT) {
			setupDefaultSpeedPref();
		}
	}

	private void setupCustomRoutingPropertyPref(GeneralRouter.RoutingParameter parameter) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		String parameterId = parameter.getId();
		String title = SettingsBaseActivity.getRoutingStringPropertyName(app, parameterId, parameter.getName());
		String description = SettingsBaseActivity.getRoutingStringPropertyDescription(app, parameterId, parameter.getDescription());

		String defValue = parameter.getType() == GeneralRouter.RoutingParameterType.NUMERIC ? "0.0" : "-";
		OsmandSettings.StringPreference pref = (OsmandSettings.StringPreference) app.getSettings().getCustomRoutingProperty(parameterId, defValue);

		Object[] values = parameter.getPossibleValues();
		String[] valuesStr = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			valuesStr[i] = values[i].toString();
		}

		ListPreferenceEx listPreference = createListPreferenceEx(pref.getId(), parameter.getPossibleValueDescriptions(), valuesStr, title, R.layout.preference_with_descr);
		listPreference.setDescription(description);
		listPreference.setIcon(getPreferenceIcon(parameterId));
		getPreferenceScreen().addPreference(listPreference);
	}

	private void setupDefaultSpeedPref() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		Preference defaultSpeedPref = new Preference(ctx);
		defaultSpeedPref.setKey(GeneralRouter.DEFAULT_SPEED);
		defaultSpeedPref.setTitle(R.string.default_speed_setting_title);
		defaultSpeedPref.setSummary(R.string.default_speed_setting_descr);
		defaultSpeedPref.setIcon(getPreferenceIcon(GeneralRouter.DEFAULT_SPEED));
		defaultSpeedPref.setLayoutResource(R.layout.preference_with_descr);
		getPreferenceScreen().addPreference(defaultSpeedPref);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals(GeneralRouter.DEFAULT_SPEED)) {
			RouteService routeService = getSelectedAppMode().getRouteService();
			showSeekbarSettingsDialog(getActivity(), routeService == RouteService.STRAIGHT, getSelectedAppMode());
			return true;
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public void onPreferenceChanged(String prefId) {
		recalculateRoute();
	}

	private void recalculateRoute() {
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (getSelectedAppMode().equals(routingHelper.getAppMode())
				&& (routingHelper.isRouteCalculated() || routingHelper.isRouteBeingCalculated())) {
			routingHelper.recalculateRouteDueToSettingsChange();
		}
	}

	private Drawable getPreferenceIcon(String prefId) {
		switch (prefId) {
			case GeneralRouter.DEFAULT_SPEED:
				return getContentIcon(R.drawable.ic_action_speed);
			case GeneralRouter.VEHICLE_HEIGHT:
				return getContentIcon(R.drawable.ic_action_height_limit);
			case GeneralRouter.VEHICLE_WEIGHT:
				return getContentIcon(R.drawable.ic_action_weight_limit);
			case GeneralRouter.VEHICLE_WIDTH:
				return getContentIcon(R.drawable.ic_action_width_limit);
			default:
				return null;
		}
	}
}