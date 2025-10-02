package net.osmand.plus.settings.fragments;

import static net.osmand.plus.settings.backend.OsmandSettings.ROUTING_PREFERENCE_PREFIX;
import static net.osmand.plus.settings.fragments.RouteParametersFragment.createRoutingParameterPref;
import static net.osmand.router.GeneralRouter.*;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.StringPreference;
import net.osmand.plus.settings.bottomsheets.FuelTankCapacityBottomSheet;
import net.osmand.plus.settings.bottomsheets.SimpleSingleSelectionBottomSheet;
import net.osmand.plus.settings.bottomsheets.VehicleParametersBottomSheet;
import net.osmand.plus.settings.enums.DrivingRegion;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SizePreference;
import net.osmand.plus.settings.vehiclesize.SizeType;
import net.osmand.plus.settings.vehiclesize.VehicleSizes;
import net.osmand.plus.settings.vehiclesize.WeightMetric;
import net.osmand.plus.settings.vehiclesize.containers.Metric;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.router.GeneralRouter;
import net.osmand.shared.settings.enums.MetricsConstants;

import java.util.Map;

public class VehicleParametersFragment extends BaseSettingsFragment {

	public static final String TAG = VehicleParametersFragment.class.getSimpleName();

	private static final String ROUTING_PARAMETER_NUMERIC_DEFAULT = "0.0";
	private static final String ROUTING_PARAMETER_SYMBOLIC_DEFAULT = "-";

	private static final String MOTOR_TYPE_PREF_ID = ROUTING_PREFERENCE_PREFIX + MOTOR_TYPE;

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
				GeneralRouterProfile routerProfile = router.getProfile();
				String derivedProfile = mode.getDerivedProfile();
				Map<String, RoutingParameter> parameters = RoutingHelperUtils.getParametersForDerivedProfile(mode, router);

				showDimensionsCategory(parameters, routerProfile, derivedProfile);
				showFuelCategory(parameters, routerProfile);
				showOtherCategory(parameters, routerProfile, derivedProfile);
			}
		} else {
			setupCategoryPref(R.string.shared_string_other);
			setupDefaultSpeedPref();
		}
	}

	private void showOtherCategory(@NonNull Map<String, RoutingParameter> parameters, @Nullable GeneralRouterProfile routerProfile,
	                               @Nullable String derivedProfile) {
		boolean shouldShowOtherCategory = parameters.get(MAX_AXLE_LOAD) != null
				|| parameters.get(WEIGHT_RATING) != null
				|| routerProfile != GeneralRouterProfile.PUBLIC_TRANSPORT;
		if (shouldShowOtherCategory) {
			addDividerPref();
			setupCategoryPref(R.string.shared_string_other);
		}

		setupVehiclePropertyPref(parameters.get(MAX_AXLE_LOAD), routerProfile, derivedProfile);
		setupVehiclePropertyPref(parameters.get(WEIGHT_RATING), routerProfile, derivedProfile);
		if (routerProfile != GeneralRouterProfile.PUBLIC_TRANSPORT) {
			setupDefaultSpeedPref();
		}
	}

	private void showFuelCategory(@NonNull Map<String, RoutingParameter> parameters, @Nullable GeneralRouterProfile routerProfile) {
		boolean showFuelTankCapacity = routerProfile != null
				&& routerProfile != GeneralRouterProfile.PEDESTRIAN
				&& routerProfile != GeneralRouterProfile.BICYCLE
				&& routerProfile != GeneralRouterProfile.HORSEBACKRIDING
				&& routerProfile != GeneralRouterProfile.SKI;
		RoutingParameter motorTypeParameter = parameters.get(MOTOR_TYPE);
		boolean showCategory = showFuelTankCapacity || motorTypeParameter != null;
		if (showCategory) {
			addDividerPref();
			setupCategoryPref(R.string.poi_filter_fuel);
		}
		setupRoutingParameterPref(motorTypeParameter);
		if (showFuelTankCapacity) {
			setupFuelTankCapacityPref();
		}
	}

	private void showDimensionsCategory(@NonNull Map<String, RoutingParameter> parameters,
	                                    @Nullable GeneralRouterProfile routerProfile,
	                                    @Nullable String derivedProfile) {
		if (routerProfile == null) {
			return;
		}
		RoutingParameter height = parameters.get(VEHICLE_HEIGHT);
		RoutingParameter weight = parameters.get(VEHICLE_WEIGHT);
		RoutingParameter width = parameters.get(VEHICLE_WIDTH);
		RoutingParameter length = parameters.get(VEHICLE_LENGTH);
		if (height != null || weight != null || width != null || length != null) {
			setupCategoryPref(R.string.shared_strings_dimensions);
		}

		setupVehiclePropertyPref(height, routerProfile, derivedProfile);
		setupVehiclePropertyPref(weight, routerProfile, derivedProfile);
		setupVehiclePropertyPref(width, routerProfile, derivedProfile);
		setupVehiclePropertyPref(length, routerProfile, derivedProfile);
	}

	private void setupCategoryPref(int titleId) {
		PreferenceCategory preferenceCategory = new PreferenceCategory(requireContext());
		preferenceCategory.setTitle(titleId);
		preferenceCategory.setLayoutResource(R.layout.preference_category_with_descr);

		PreferenceScreen screen = getPreferenceScreen();
		screen.addPreference(preferenceCategory);
	}

	private void setupFuelTankCapacityPref() {
		Context ctx = requireContext();
		ApplicationMode selectedMode = getSelectedAppMode();
		Preference defaultSpeedPref = new Preference(ctx);
		defaultSpeedPref.setKey(settings.FUEL_TANK_CAPACITY.getId());
		defaultSpeedPref.setTitle(R.string.fuel_tank_capacity);
		defaultSpeedPref.setSummary(OsmAndFormatter.getFormattedFuelCapacityValue(app, settings.UNIT_OF_VOLUME.getModeValue(selectedMode), selectedMode).format(app));
		defaultSpeedPref.setIcon(getPersistentPrefIcon(R.drawable.ic_action_fuel_tank));
		defaultSpeedPref.setLayoutResource(R.layout.preference_with_descr);
		getPreferenceScreen().addPreference(defaultSpeedPref);
	}

	private void setupVehiclePropertyPref(@Nullable RoutingParameter parameter,
	                                      @Nullable GeneralRouterProfile profile,
	                                      @Nullable String derivedProfile) {
		if (parameter == null || profile == null) {
			return;
		}

		String parameterId = parameter.getId();
		VehicleSizes vehicle = VehicleSizes.newInstance(profile, derivedProfile);
		SizeType type = SizeType.getByKey(parameterId);
		if (vehicle == null || type == null) {
			return;
		}

		String title = AndroidUtils.getRoutingStringPropertyName(app, parameterId, parameter.getName());
		String description = AndroidUtils.getRoutingStringPropertyDescription(app, parameterId, parameter.getDescription());
		String defValue = parameter.getDefaultString();
		ApplicationMode appMode = getSelectedAppMode();
		StringPreference preference = (StringPreference) settings.getCustomRoutingProperty(parameterId, defValue);

		SizePreference uiPreference = new SizePreference(requireContext());
		uiPreference.setKey(preference.getId());
		uiPreference.setSizeType(type);
		uiPreference.setVehicleSizes(vehicle);
		uiPreference.setDefaultValue(defValue);
		uiPreference.setMetric(createMetrics(appMode));
		uiPreference.setTitle(title);
		uiPreference.setSummary(description);
		uiPreference.setIcon(getPreferenceIcon(parameterId));
		uiPreference.setLayoutResource(R.layout.preference_with_descr);

		PreferenceScreen screen = getPreferenceScreen();
		screen.addPreference(uiPreference);
	}

	@NonNull
	private Metric createMetrics(@NonNull ApplicationMode appMode) {
		boolean usePounds = settings.DRIVING_REGION.getModeValue(appMode) == DrivingRegion.US;
		WeightMetric weightMetric = usePounds ? WeightMetric.POUNDS : WeightMetric.TONES;
		MetricsConstants lengthMetric = settings.METRIC_SYSTEM.getModeValue(appMode);
		return new Metric(weightMetric, lengthMetric);
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

	private void setupRoutingParameterPref(@Nullable RoutingParameter parameter) {
		if (parameter != null) {
			Preference preference = createRoutingParameterPref(requireContext(), parameter);
			preference.setIcon(getPreferenceIcon(parameter.getId()));
			getPreferenceScreen().addPreference(preference);
		}
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
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
		String key = preference.getKey();
		if (DEFAULT_SPEED.equals(key)) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				ApplicationMode mode = getSelectedAppMode();
				VehicleSpeedHelper speedHelper = new VehicleSpeedHelper(app, mode);
				speedHelper.showSeekbarSettingsDialog(activity);
			}
			return true;
		} else if (settings.FUEL_TANK_CAPACITY.getId().equals(key)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				FuelTankCapacityBottomSheet.showInstance(fragmentManager, preference.getKey(),
						this, false, getSelectedAppMode());
			}
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
		} else if (MOTOR_TYPE_PREF_ID.equals(preference.getKey())) {
			FragmentManager manager = getFragmentManager();
			if (manager != null) {
				ListPreferenceEx pref = (ListPreferenceEx) preference;
				SimpleSingleSelectionBottomSheet.showInstance(manager, this, preference.getKey(),
						pref.getTitle().toString(), pref.getDescription(),
						getSelectedAppMode(), false, pref.getEntries(),
						pref.getEntryValues(), pref.getValueIndex());
			}
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	@Override
	public void onApplyPreferenceChange(String prefId, boolean applyToAllProfiles, Object newValue) {
		super.onApplyPreferenceChange(prefId, applyToAllProfiles, newValue);
		if (MOTOR_TYPE_PREF_ID.equals(prefId)) {
			if (getActivity() instanceof MapActivity) {
				((MapActivity) getActivity()).getMapRouteInfoMenu().updateMenu();
			}
		}
	}

	@Override
	public void onPreferenceChanged(@NonNull String prefId) {
		recalculateRoute();
	}

	private void recalculateRoute() {
		app.getRoutingHelper().onSettingsChanged(getSelectedAppMode());
	}

	@Nullable
	private Drawable getPreferenceIcon(String prefId) {
		return switch (prefId) {
			case DEFAULT_SPEED -> getPersistentPrefIcon(R.drawable.ic_action_speed);
			case VEHICLE_HEIGHT -> getPersistentPrefIcon(R.drawable.ic_action_height_limit);
			case VEHICLE_WEIGHT -> getPersistentPrefIcon(R.drawable.ic_action_weight_limit);
			case VEHICLE_WIDTH -> getPersistentPrefIcon(R.drawable.ic_action_width_limit);
			case VEHICLE_LENGTH -> getPersistentPrefIcon(R.drawable.ic_action_length_limit);
			case MOTOR_TYPE -> getPersistentPrefIcon(R.drawable.ic_action_fuel);
			case MAX_AXLE_LOAD -> getPersistentPrefIcon(R.drawable.ic_action_hgv_axle_load);
			case WEIGHT_RATING -> getPersistentPrefIcon(R.drawable.ic_action_hgv_full_load);
			default -> null;
		};
	}
}
