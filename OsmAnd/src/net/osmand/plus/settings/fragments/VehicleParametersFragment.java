package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.slider.Slider;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.helpers.enums.SpeedConstants;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.StringPreference;
import net.osmand.plus.settings.bottomsheets.VehicleParametersBottomSheet;
import net.osmand.plus.settings.bottomsheets.VehicleSizeAssets;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SizePreference;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.util.Algorithms;

import java.util.Map;

import static net.osmand.router.GeneralRouter.DEFAULT_SPEED;
import static net.osmand.router.GeneralRouter.RoutingParameter;
import static net.osmand.router.GeneralRouter.RoutingParameterType;
import static net.osmand.router.GeneralRouter.VEHICLE_HEIGHT;
import static net.osmand.router.GeneralRouter.VEHICLE_LENGTH;
import static net.osmand.router.GeneralRouter.VEHICLE_WEIGHT;
import static net.osmand.router.GeneralRouter.VEHICLE_WIDTH;

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
				GeneralRouterProfile routerProfile = router.getProfile();
				Map<String, RoutingParameter> parameters = router.getParameters();
				setupCustomRoutingPropertyPref(parameters.get(VEHICLE_HEIGHT), routerProfile);
				setupCustomRoutingPropertyPref(parameters.get(VEHICLE_WEIGHT), routerProfile);
				setupCustomRoutingPropertyPref(parameters.get(VEHICLE_WIDTH), routerProfile);
				setupCustomRoutingPropertyPref(parameters.get(VEHICLE_LENGTH), routerProfile);
				if (routerProfile != GeneralRouterProfile.PUBLIC_TRANSPORT) {
					setupDefaultSpeedPref();
				}
			}
		} else {
			setupDefaultSpeedPref();
		}
	}

	private void setupCustomRoutingPropertyPref(@Nullable RoutingParameter parameter,
												GeneralRouterProfile routerProfile) {
		if (parameter == null) {
			return;
		}
		String parameterId = parameter.getId();
		String title = AndroidUtils.getRoutingStringPropertyName(app, parameterId, parameter.getName());
		String description = AndroidUtils.getRoutingStringPropertyDescription(app, parameterId,
				parameter.getDescription());
		String defValue = parameter.getType() == RoutingParameterType.NUMERIC
				? ROUTING_PARAMETER_NUMERIC_DEFAULT : ROUTING_PARAMETER_SYMBOLIC_DEFAULT;
		StringPreference pref = (StringPreference) app.getSettings()
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
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	@Override
	public void onPreferenceChanged(String prefId) {
		recalculateRoute();
	}

	private void recalculateRoute() {
		app.getRoutingHelper().onSettingsChanged(getSelectedAppMode());
	}

	private void showSeekbarSettingsDialog(@NonNull Activity activity, final boolean defaultSpeedOnly) {
		final ApplicationMode mode = getSelectedAppMode();

		SpeedConstants units = app.getSettings().SPEED_SYSTEM.getModeValue(mode);
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

		float settingsMinSpeed = mode.getMinSpeed();
		float settingsMaxSpeed = mode.getMaxSpeed();
		float settingsDefaultSpeed = mode.getDefaultSpeed();

		final int[] defaultValue = {Math.round(settingsDefaultSpeed * ratio[0])};
		final int[] minValue = new int[1];
		final int[] maxValue = new int[1];
		final int min;
		final int max;

		GeneralRouter router = app.getRouter(mode);
		if (defaultSpeedOnly || router == null) {
			minValue[0] = Math.round(Math.min(1, settingsDefaultSpeed) * ratio[0]);
			maxValue[0] = Math.round(Math.max(300, settingsDefaultSpeed) * ratio[0]);
			min = minValue[0];
			max = maxValue[0];
		} else {
			float minSpeedValue = settingsMinSpeed > 0 ? settingsMinSpeed : router.getMinSpeed();
			float maxSpeedValue = settingsMaxSpeed > 0 ? settingsMaxSpeed : router.getMaxSpeed();
			minValue[0] = Math.round(Math.min(minSpeedValue, settingsDefaultSpeed) * ratio[0]);
			maxValue[0] = Math.round(Math.max(maxSpeedValue, settingsDefaultSpeed) * ratio[0]);

			min = Math.round(Math.min(minValue[0], router.getMinSpeed() * ratio[0] / 2f));
			max = Math.round(Math.max(maxValue[0], router.getMaxSpeed() * ratio[0] * 1.5f));
		}

		boolean nightMode = !app.getSettings().isLightContentForMode(mode);
		Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		View seekbarView = LayoutInflater.from(themedContext).inflate(R.layout.default_speed_dialog, null, false);
		builder.setView(seekbarView);
		builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mode.setDefaultSpeed(defaultValue[0] / ratio[0]);
				if (!defaultSpeedOnly) {
					mode.setMinSpeed(minValue[0] / ratio[0]);
					mode.setMaxSpeed(maxValue[0] / ratio[0]);
				}
				app.getRoutingHelper().onSettingsChanged(mode);
			}
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setNeutralButton(R.string.shared_string_revert, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mode.resetDefaultSpeed();
				if (!defaultSpeedOnly) {
					mode.setMinSpeed(0f);
					mode.setMaxSpeed(0f);
				}
				app.getRoutingHelper().onSettingsChanged(mode);
			}
		});

		int selectedModeColor = mode.getProfileColor(nightMode);
		if (!defaultSpeedOnly) {
			setupSpeedSlider(SpeedSliderType.DEFAULT_SPEED, speedUnits, defaultValue, minValue, maxValue, min, max, seekbarView, selectedModeColor);
			setupSpeedSlider(SpeedSliderType.MIN_SPEED, speedUnits, defaultValue, minValue, maxValue, min, max, seekbarView, selectedModeColor);
			setupSpeedSlider(SpeedSliderType.MAX_SPEED, speedUnits, defaultValue, minValue, maxValue, min, max, seekbarView, selectedModeColor);
		} else {
			setupSpeedSlider(SpeedSliderType.DEFAULT_SPEED_ONLY, speedUnits, defaultValue, minValue, maxValue, min, max, seekbarView, selectedModeColor);
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

	private void setupSpeedSlider(final SpeedSliderType type, String speedUnits, final int[] defaultValue,
								  final int[] minValue, final int[] maxValue, final int min, int max,
								  View seekbarView, int activeColor) {
		View sliderLayout;
		int titleId;
		final int[] speedValue;
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
		final Slider slider = sliderLayout.findViewById(R.id.speed_slider);
		final TextView speedTitleTv = sliderLayout.findViewById(R.id.speed_title);
		final TextView speedMinTv = sliderLayout.findViewById(R.id.speed_seekbar_min_text);
		final TextView speedMaxTv = sliderLayout.findViewById(R.id.speed_seekbar_max_text);
		final TextView speedUnitsTv = sliderLayout.findViewById(R.id.speed_units);
		final TextView speedTv = sliderLayout.findViewById(R.id.speed_text);

		speedTitleTv.setText(titleId);
		speedMinTv.setText(String.valueOf(min));
		speedMaxTv.setText(String.valueOf(max));
		speedTv.setText(String.valueOf(speedValue[0]));
		speedUnitsTv.setText(speedUnits);
		slider.setValueTo(max - min);
		slider.setValue(Math.max(speedValue[0] - min, 0));
		slider.addOnChangeListener(new Slider.OnChangeListener() {
			@Override
			public void onValueChange(@NonNull Slider slider, float val, boolean fromUser) {
				int progress = (int) val;
				int value = progress + min;
				switch (type) {
					case DEFAULT_SPEED:
					case DEFAULT_SPEED_ONLY:
						if (value > maxValue[0]) {
							value = maxValue[0];
							slider.setValue(Math.max(value - min, 0));
						} else if (value < minValue[0]) {
							value = minValue[0];
							slider.setValue(Math.max(value - min, 0));
						}
						break;
					case MIN_SPEED:
						if (value > defaultValue[0]) {
							value = defaultValue[0];
							slider.setValue(Math.max(value - min, 0));
						}
						break;
					case MAX_SPEED:
						if (value < defaultValue[0]) {
							value = defaultValue[0];
							slider.setValue(Math.max(value - min, 0));
						}
						break;
					default:
						break;
				}
				speedValue[0] = value;
				speedTv.setText(String.valueOf(value));
			}
		});
		UiUtilities.setupSlider(slider, isNightMode(), activeColor);
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
