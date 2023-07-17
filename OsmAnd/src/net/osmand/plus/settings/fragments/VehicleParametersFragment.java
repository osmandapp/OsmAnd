package net.osmand.plus.settings.fragments;

import static net.osmand.plus.settings.backend.OsmandSettings.ROUTING_PREFERENCE_PREFIX;
import static net.osmand.plus.settings.fragments.RouteParametersFragment.createRoutingParameterPref;
import static net.osmand.router.GeneralRouter.DEFAULT_SPEED;
import static net.osmand.router.GeneralRouter.MOTOR_TYPE;
import static net.osmand.router.GeneralRouter.RoutingParameter;
import static net.osmand.router.GeneralRouter.VEHICLE_HEIGHT;
import static net.osmand.router.GeneralRouter.VEHICLE_LENGTH;
import static net.osmand.router.GeneralRouter.VEHICLE_WEIGHT;
import static net.osmand.router.GeneralRouter.VEHICLE_WIDTH;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.StringPreference;
import net.osmand.plus.settings.bottomsheets.SimpleSingleSelectionBottomSheet;
import net.osmand.plus.settings.bottomsheets.VehicleParametersBottomSheet;
import net.osmand.plus.settings.enums.DrivingRegion;
import net.osmand.plus.settings.vehiclesize.SizeType;
import net.osmand.plus.settings.vehiclesize.VehicleSizes;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.settings.enums.SpeedConstants;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SizePreference;
import net.osmand.plus.settings.vehiclesize.WeightMetric;
import net.osmand.plus.settings.vehiclesize.containers.Metric;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;

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
				setupVehiclePropertyPref(parameters.get(VEHICLE_HEIGHT), routerProfile, derivedProfile);
				setupVehiclePropertyPref(parameters.get(VEHICLE_WEIGHT), routerProfile, derivedProfile);
				setupVehiclePropertyPref(parameters.get(VEHICLE_WIDTH), routerProfile, derivedProfile);
				setupVehiclePropertyPref(parameters.get(VEHICLE_LENGTH), routerProfile, derivedProfile);

				setupRoutingParameterPref(parameters.get(MOTOR_TYPE));
				if (routerProfile != GeneralRouterProfile.PUBLIC_TRANSPORT) {
					setupDefaultSpeedPref();
				}
			}
		} else {
			setupDefaultSpeedPref();
		}
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
			FragmentActivity activity = getActivity();
			if (activity != null) {
				showSeekbarSettingsDialog(activity, defaultSpeedOnly);
			}
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
			MapActivity activity = getMapActivity();
			if (activity != null) {
				activity.getMapRouteInfoMenu().updateMenu();
			}
		}
	}

	@Override
	public void onPreferenceChanged(String prefId) {
		recalculateRoute();
	}

	private void recalculateRoute() {
		app.getRoutingHelper().onSettingsChanged(getSelectedAppMode());
	}

	private void showSeekbarSettingsDialog(@NonNull Activity activity, boolean defaultSpeedOnly) {
		ApplicationMode mode = getSelectedAppMode();

		SpeedConstants units = app.getSettings().SPEED_SYSTEM.getModeValue(mode);
		String speedUnits = units.toShortString(activity);
		float[] ratio = new float[1];
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

		GeneralRouter router = app.getRouter(mode);

		float settingsMinSpeed = mode.getMinSpeed();
		float settingsMaxSpeed = mode.getMaxSpeed();
		float settingsDefaultSpeed = mode.getDefaultSpeed();
		boolean decimalPrecision = !defaultSpeedOnly
				&& router != null
				&& mode.getMaxSpeedConfigLimit() <= ApplicationMode.FAST_SPEED_THRESHOLD;

		float[] defaultValue = {roundSpeed(settingsDefaultSpeed * ratio[0], decimalPrecision)};
		float[] minValue = new float[1];
		float[] maxValue = new float[1];
		int min;
		int max;

		if (defaultSpeedOnly || router == null) {
			minValue[0] = Math.round(Math.min(1, settingsDefaultSpeed) * ratio[0]);
			maxValue[0] = Math.round(Math.max(300, settingsDefaultSpeed) * ratio[0]);
			min = (int) minValue[0];
		} else {
			float minSpeedValue = settingsMinSpeed > 0 ? settingsMinSpeed : router.getMinSpeed();
			float maxSpeedValue = settingsMaxSpeed > 0 ? settingsMaxSpeed : mode.getMaxSpeedConfigLimit();

			minValue[0] = roundSpeed(Math.min(minSpeedValue, settingsDefaultSpeed) * ratio[0], decimalPrecision);
			maxValue[0] = roundSpeed(Math.max(maxSpeedValue, settingsDefaultSpeed) * ratio[0], decimalPrecision);

			min = Math.round(Math.min(minValue[0], router.getMinSpeed() * ratio[0] / 2f));
		}
		max = Math.round(Math.max(maxValue[0], mode.getMaxSpeedConfigLimit() * ratio[0] * 1.5f));
		max = Math.max(min, max);

		boolean nightMode = !app.getSettings().isLightContentForMode(mode);
		Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		View seekbarView = LayoutInflater.from(themedContext).inflate(R.layout.default_speed_dialog, null, false);
		builder.setView(seekbarView);
		builder.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
			mode.setDefaultSpeed(defaultValue[0] / ratio[0]);
			if (!defaultSpeedOnly) {
				mode.setMinSpeed(minValue[0] / ratio[0]);
				mode.setMaxSpeed(maxValue[0] / ratio[0]);
			}
			app.getRoutingHelper().onSettingsChanged(mode);
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setNeutralButton(R.string.shared_string_revert, (dialog, which) -> {
			mode.resetDefaultSpeed();
			if (!defaultSpeedOnly) {
				mode.setMinSpeed(0f);
				mode.setMaxSpeed(0f);
			}
			app.getRoutingHelper().onSettingsChanged(mode);
		});

		int selectedModeColor = mode.getProfileColor(nightMode);
		if (!defaultSpeedOnly) {
			setupSpeedSlider(SpeedSliderType.DEFAULT_SPEED, speedUnits, defaultValue, minValue, maxValue,
					min, max, decimalPrecision, seekbarView, selectedModeColor);
			setupSpeedSlider(SpeedSliderType.MIN_SPEED, speedUnits, defaultValue, minValue, maxValue,
					min, max, decimalPrecision, seekbarView, selectedModeColor);
			setupSpeedSlider(SpeedSliderType.MAX_SPEED, speedUnits, defaultValue, minValue, maxValue,
					min, max, decimalPrecision, seekbarView, selectedModeColor);
		} else {
			setupSpeedSlider(SpeedSliderType.DEFAULT_SPEED_ONLY, speedUnits, defaultValue, minValue,
					maxValue, min, max, false, seekbarView, selectedModeColor);
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

	private void setupSpeedSlider(@NonNull SpeedSliderType type,
	                              @NonNull String speedUnits,
	                              @NonNull float[] defaultValue,
	                              @NonNull float[] minValue,
	                              @NonNull float[] maxValue,
	                              int min,
	                              int max,
	                              boolean decimalPrecision,
	                              @NonNull View seekbarView,
	                              @ColorInt int activeColor) {
		View sliderLayout;
		int titleId;
		float[] speedValue;
		switch (type) {
			case DEFAULT_SPEED_ONLY:
				speedValue = defaultValue;
				sliderLayout = seekbarView.findViewById(R.id.min_speed_layout);
				titleId = R.string.default_speed_setting_title;
				break;
			case MIN_SPEED:
				speedValue = minValue;
				sliderLayout = seekbarView.findViewById(R.id.min_speed_layout);
				titleId = R.string.shared_string_min_speed;
				break;
			case MAX_SPEED:
				speedValue = maxValue;
				sliderLayout = seekbarView.findViewById(R.id.max_speed_layout);
				titleId = R.string.shared_string_max_speed;
				break;
			default:
				speedValue = defaultValue;
				sliderLayout = seekbarView.findViewById(R.id.default_speed_layout);
				titleId = R.string.default_speed_setting_title;
				break;
		}
		Slider slider = sliderLayout.findViewById(R.id.speed_slider);
		TextView speedTitleTv = sliderLayout.findViewById(R.id.speed_title);
		TextView speedMinTv = sliderLayout.findViewById(R.id.speed_seekbar_min_text);
		TextView speedMaxTv = sliderLayout.findViewById(R.id.speed_seekbar_max_text);
		TextView speedUnitsTv = sliderLayout.findViewById(R.id.speed_units);
		TextView selectedSpeedTv = sliderLayout.findViewById(R.id.speed_text);

		speedTitleTv.setText(titleId);
		speedMinTv.setText(String.valueOf(min));
		speedMaxTv.setText(String.valueOf(max));
		selectedSpeedTv.setText(formatSpeed(speedValue[0], decimalPrecision));
		speedUnitsTv.setText(speedUnits);

		slider.setValueTo(max - min);
		slider.setValue(Math.max(speedValue[0] - min, 0));
		slider.addOnChangeListener((slider1, val, fromUser) -> {
			float progress = decimalPrecision ? Math.round(val * 10) / 10f : (int) val;
			float value = min + progress;
			switch (type) {
				case DEFAULT_SPEED:
				case DEFAULT_SPEED_ONLY:
					if (value > maxValue[0]) {
						value = maxValue[0];
						slider1.setValue(Math.max(value - min, 0));
					} else if (value < minValue[0]) {
						value = minValue[0];
						slider1.setValue(Math.max(value - min, 0));
					}
					break;
				case MIN_SPEED:
					if (value > defaultValue[0]) {
						value = defaultValue[0];
						slider1.setValue(Math.max(value - min, 0));
					}
					break;
				case MAX_SPEED:
					if (value < defaultValue[0]) {
						value = defaultValue[0];
						slider1.setValue(Math.max(value - min, 0));
					}
					break;
				default:
					break;
			}
			speedValue[0] = value;
			selectedSpeedTv.setText(formatSpeed(value, decimalPrecision));
		});
		UiUtilities.setupSlider(slider, isNightMode(), activeColor);
	}

	@NonNull
	private String formatSpeed(float speed, boolean decimalPrecision) {
		return decimalPrecision
				? OsmAndFormatter.formatValue(speed, "", true, 1, app).value
				: String.valueOf((int) speed);
	}

	private float roundSpeed(float speed, boolean decimalPrecision) {
		return decimalPrecision ? Math.round(speed * 10) / 10f : Math.round(speed);
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
			case MOTOR_TYPE:
				return getPersistentPrefIcon(R.drawable.ic_action_fuel);
			default:
				return null;
		}
	}
}
