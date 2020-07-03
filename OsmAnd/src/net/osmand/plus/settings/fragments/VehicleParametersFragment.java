package net.osmand.plus.settings.fragments;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.VehicleParametersBottomSheet;
import net.osmand.plus.settings.bottomsheets.VehicleSizeAssets;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SizePreference;
import net.osmand.router.GeneralRouter;
import net.osmand.util.Algorithms;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;

import java.util.Map;

import static net.osmand.plus.activities.SettingsNavigationActivity.showSeekbarSettingsDialog;
import static net.osmand.router.GeneralRouter.*;

public class VehicleParametersFragment extends BaseSettingsFragment implements OnPreferenceChanged {

	public static final String TAG = VehicleParametersFragment.class.getSimpleName();

	private static final String ROUTING_PARAMETER_NUMERIC_DEFAULT = "0.0";
	private static final String ROUTING_PARAMETER_SYMBOLIC_DEFAULT = "-";

	@Override
	protected void setupPreferences() {
		ApplicationMode mode = getSelectedAppMode();
		Preference vehicleParametersInfo = findPreference("vehicle_parameters_info");
		vehicleParametersInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));
		vehicleParametersInfo.setTitle(getString(R.string.route_parameters_info, mode.toHumanString()));

		RouteService routeService = mode.getRouteService();
		if (routeService == RouteService.OSMAND) {
			GeneralRouter router = app.getRouter(mode);
			if (router != null) {
				Map<String, RoutingParameter> parameters = router.getParameters();
				setupCustomRoutingPropertyPref(parameters.get(VEHICLE_HEIGHT));
				setupCustomRoutingPropertyPref(parameters.get(VEHICLE_WEIGHT));
				setupCustomRoutingPropertyPref(parameters.get(VEHICLE_WIDTH));
				setupCustomRoutingPropertyPref(parameters.get(VEHICLE_LENGTH));
				if (router.getProfile() != GeneralRouterProfile.PUBLIC_TRANSPORT) {
					setupDefaultSpeedPref();
				}
			}
		} else {
			setupDefaultSpeedPref();
		}
	}

	private void setupCustomRoutingPropertyPref(RoutingParameter parameter) {
		GeneralRouterProfile routerProfile = app.getRouter(getSelectedAppMode()).getProfile();
		if (parameter == null) {
			return;
		}
		String parameterId = parameter.getId();
		String title = SettingsBaseActivity.getRoutingStringPropertyName(app, parameterId, parameter.getName());
		String description = SettingsBaseActivity.getRoutingStringPropertyDescription(app, parameterId,
				parameter.getDescription());
		String defValue = parameter.getType() == RoutingParameterType.NUMERIC
				? ROUTING_PARAMETER_NUMERIC_DEFAULT : ROUTING_PARAMETER_SYMBOLIC_DEFAULT;
		OsmandSettings.StringPreference pref = (OsmandSettings.StringPreference) app.getSettings()
				.getCustomRoutingProperty(parameterId, defValue);
		VehicleSizeAssets assets = VehicleSizeAssets.getAssets(parameterId, routerProfile);
		Object[] values = parameter.getPossibleValues();
		String[] valuesStr = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			valuesStr[i] = values[i].toString();
		}
		String[] entriesStr = parameter.getPossibleValueDescriptions().clone();
		entriesStr[0] = app.getString(R.string.shared_string_none);
		for (int i = 1; i < entriesStr.length; i++) {
			int firstCharIndex = Algorithms.findFirstNumberEndIndex(entriesStr[i]);
			entriesStr[i] = String.format(app.getString(R.string.ltr_or_rtl_combine_via_space),
					entriesStr[i].substring(0, firstCharIndex), getString(assets.getMetricShortRes()));
		}

		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		SizePreference vehicleSizePref = new SizePreference(ctx);
		vehicleSizePref.setKey(pref.getId());
		vehicleSizePref.setAssets(assets);
		vehicleSizePref.setDefaultValue(defValue);
		vehicleSizePref.setTitle(title);
		vehicleSizePref.setEntries(entriesStr);
		vehicleSizePref.setEntryValues(valuesStr);
		vehicleSizePref.setSummary(description);
		vehicleSizePref.setIcon(getPreferenceIcon(parameterId));
		vehicleSizePref.setLayoutResource(R.layout.preference_with_descr);
		getPreferenceScreen().addPreference(vehicleSizePref);
	}

	private void setupDefaultSpeedPref() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		Preference defaultSpeedPref = new Preference(ctx);
		defaultSpeedPref.setKey(DEFAULT_SPEED);
		defaultSpeedPref.setTitle(R.string.default_speed_setting_title);
		defaultSpeedPref.setSummary(R.string.default_speed_setting_descr);
		defaultSpeedPref.setIcon(getPreferenceIcon(DEFAULT_SPEED));
		defaultSpeedPref.setLayoutResource(R.layout.preference_with_descr);
		getPreferenceScreen().addPreference(defaultSpeedPref);
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		if (!DEFAULT_SPEED.equals(preference.getKey()) && preference instanceof ListPreferenceEx) {
			ImageView imageView = (ImageView) holder.findViewById(android.R.id.icon);
			if (imageView != null) {
				Object currentValue = ((ListPreferenceEx) preference).getValue();
				boolean enabled = preference.isEnabled() && !ROUTING_PARAMETER_NUMERIC_DEFAULT.equals(currentValue)
						&& !ROUTING_PARAMETER_SYMBOLIC_DEFAULT.equals(currentValue);
				imageView.setEnabled(enabled);
			}
		} else if (preference instanceof SizePreference) {
			ImageView imageView = (ImageView) holder.findViewById(android.R.id.icon);
			if (imageView != null) {
				Object currentValue = ((SizePreference) preference).getValue();
				boolean enabled = preference.isEnabled() && !ROUTING_PARAMETER_NUMERIC_DEFAULT.equals(currentValue)
						&& !ROUTING_PARAMETER_SYMBOLIC_DEFAULT.equals(currentValue);
				imageView.setEnabled(enabled);
			}
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals(DEFAULT_SPEED)) {
			RouteService routeService = getSelectedAppMode().getRouteService();
			boolean defaultSpeedOnly = routeService == RouteService.STRAIGHT || routeService == RouteService.DIRECT_TO;
			showSeekbarSettingsDialog(getActivity(), defaultSpeedOnly, getSelectedAppMode());
			return true;
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		if (preference instanceof SizePreference) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				VehicleParametersBottomSheet.showInstance(fragmentManager, preference.getKey(),
						this, false, getSelectedAppMode());
			}
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
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
			case DEFAULT_SPEED:
				return getPersistentPrefIcon(R.drawable.ic_action_speed);
			case VEHICLE_HEIGHT:
				return getPersistentPrefIcon(R.drawable.ic_action_height_limit);
			case VEHICLE_WEIGHT:
				return getPersistentPrefIcon(R.drawable.ic_action_weight_limit);
			case VEHICLE_WIDTH:
				return getPersistentPrefIcon(R.drawable.ic_action_width_limit);
			case VEHICLE_LENGTH:
				return getPersistentPrefIcon(R.drawable.ic_action_length_limit);
			default:
				return null;
		}
	}
}
