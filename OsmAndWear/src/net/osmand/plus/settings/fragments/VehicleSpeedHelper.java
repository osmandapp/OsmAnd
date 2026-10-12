package net.osmand.plus.settings.fragments;

import static net.osmand.plus.routing.RouteService.DIRECT_TO;
import static net.osmand.plus.routing.RouteService.STRAIGHT;
import static net.osmand.plus.settings.backend.ApplicationMode.FAST_SPEED_THRESHOLD;
import static net.osmand.plus.settings.enums.SpeedSliderType.DEFAULT_SPEED;
import static net.osmand.plus.settings.enums.SpeedSliderType.DEFAULT_SPEED_ONLY;
import static net.osmand.plus.settings.enums.SpeedSliderType.MAX_SPEED;
import static net.osmand.plus.settings.enums.SpeedSliderType.MIN_SPEED;

import android.app.Activity;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnChangeListener;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.shared.settings.enums.SpeedConstants;
import net.osmand.plus.settings.enums.SpeedSliderType;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.router.GeneralRouter;

public class VehicleSpeedHelper {

	private static final float MAX_DEFAULT_SPEED = 300;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final ApplicationMode mode;
	private final boolean nightMode;

	public VehicleSpeedHelper(@NonNull OsmandApplication app, @NonNull ApplicationMode mode) {
		this.app = app;
		this.mode = mode;
		this.settings = app.getSettings();
		this.nightMode = !settings.isLightContentForMode(mode);
	}

	public void showSeekbarSettingsDialog(@NonNull Activity activity) {
		GeneralRouter router = app.getRouter(mode);
		RouteService routeService = mode.getRouteService();

		float maxSpeedLimit = VehicleSpeedConfigLimits.getMaxSpeedConfigLimit(app, mode);
		boolean defaultSpeedOnly = routeService == STRAIGHT || routeService == DIRECT_TO;
		boolean decimalPrecision = !defaultSpeedOnly && router != null && maxSpeedLimit / 1.5f <= FAST_SPEED_THRESHOLD;

		float[] ratio = getSpeedRatio();
		float[] minValue = new float[1];
		float[] maxValue = new float[1];

		Pair<Integer, Integer> pair = getMinMax(router, ratio, minValue, maxValue, defaultSpeedOnly, decimalPrecision);
		int min = pair.first;
		int max = pair.second;

		showDialog(activity, ratio, minValue, maxValue, min, max, defaultSpeedOnly, decimalPrecision);
	}

	private void showDialog(@NonNull Activity activity, float[] ratio,
	                        float[] minValue, float[] maxValue, int min, int max,
	                        boolean defaultSpeedOnly, boolean decimalPrecision) {
		float settingsDefaultSpeed = mode.getDefaultSpeed();
		float[] defaultValue = {roundSpeed(settingsDefaultSpeed * ratio[0], decimalPrecision)};

		Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
		View view = LayoutInflater.from(themedContext).inflate(R.layout.default_speed_dialog, null, false);
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		builder.setView(view);
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
		setupSliders(view, defaultValue, minValue, maxValue, min, max, defaultSpeedOnly, decimalPrecision);
		builder.show();
	}

	private void setupSliders(@NonNull View view, float[] defaultValue, float[] minValue, float[] maxValue,
	                          int min, int max, boolean defaultSpeedOnly, boolean decimalPrecision) {
		String speedUnits = getSpeedUnits();
		int color = mode.getProfileColor(nightMode);
		if (!defaultSpeedOnly) {
			setupSpeedSlider(DEFAULT_SPEED, speedUnits, defaultValue, minValue, maxValue, min, max, decimalPrecision, view, color);
			setupSpeedSlider(MIN_SPEED, speedUnits, defaultValue, minValue, maxValue, min, max, decimalPrecision, view, color);
			setupSpeedSlider(MAX_SPEED, speedUnits, defaultValue, minValue, maxValue, min, max, decimalPrecision, view, color);
		} else {
			setupSpeedSlider(DEFAULT_SPEED_ONLY, speedUnits, defaultValue, minValue, maxValue, min, max, false, view, color);

			AndroidUiHelper.updateVisibility(view.findViewById(R.id.default_speed_div), false);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.default_speed_container), false);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.max_speed_div), false);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.max_speed_container), false);
		}
	}

	private void setupSpeedSlider(@NonNull SpeedSliderType type, @NonNull String speedUnits,
	                              @NonNull float[] defaultValue, @NonNull float[] minValue,
	                              @NonNull float[] maxValue, int min, int max, boolean decimalPrecision,
	                              @NonNull View seekbarView, @ColorInt int activeColor) {
		View sliderLayout = seekbarView.findViewById(type.layoutId);
		float[] speedValue = getSpeedValue(type, defaultValue, minValue, maxValue);

		Slider slider = sliderLayout.findViewById(R.id.speed_slider);
		TextView speedTitleTv = sliderLayout.findViewById(R.id.speed_title);
		TextView speedMinTv = sliderLayout.findViewById(R.id.speed_seekbar_min_text);
		TextView speedMaxTv = sliderLayout.findViewById(R.id.speed_seekbar_max_text);
		TextView speedUnitsTv = sliderLayout.findViewById(R.id.speed_units);
		TextView selectedSpeedTv = sliderLayout.findViewById(R.id.speed_text);

		speedTitleTv.setText(type.titleId);
		speedMinTv.setText(String.valueOf(min));
		speedMaxTv.setText(String.valueOf(max));
		selectedSpeedTv.setText(formatSpeed(speedValue[0], decimalPrecision));
		speedUnitsTv.setText(speedUnits);

		slider.setValueTo(max - min);
		slider.setValue(Math.max(speedValue[0] - min, 0));
		slider.addOnChangeListener(getChangeListener(type, selectedSpeedTv, defaultValue, speedValue, minValue, maxValue, min, decimalPrecision));
		UiUtilities.setupSlider(slider, nightMode, activeColor);
	}

	@NonNull
	private OnChangeListener getChangeListener(@NonNull SpeedSliderType type, @NonNull TextView textView,
	                                           @NonNull float[] defaultValue, @NonNull float[] speedValue,
	                                           @NonNull float[] minValue, @NonNull float[] maxValue,
	                                           int min, boolean decimalPrecision) {
		return (slider, val, fromUser) -> {
			float progress = decimalPrecision ? Math.round(val * 10) / 10f : (int) val;
			float value = min + progress;
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
			textView.setText(formatSpeed(value, decimalPrecision));
		};
	}

	@NonNull
	private Pair<Integer, Integer> getMinMax(@Nullable GeneralRouter router, float[] ratio,
	                                         float[] minValue, float[] maxValue,
	                                         boolean defaultSpeedOnly, boolean decimalPrecision) {
		int min;
		int max;

		float settingsDefaultSpeed = mode.getDefaultSpeed();
		if (defaultSpeedOnly || router == null) {
			minValue[0] = Math.round(Math.min(1, settingsDefaultSpeed) * ratio[0]);
			maxValue[0] = Math.round(Math.max(MAX_DEFAULT_SPEED, settingsDefaultSpeed) * ratio[0]);
			min = Math.round(minValue[0]);
		} else {
			float settingsMinSpeed = mode.getMinSpeed();
			float settingsMaxSpeed = mode.getMaxSpeed();

			float minSpeedValue = settingsMinSpeed > 0 ? settingsMinSpeed : router.getMinSpeed();
			float maxSpeedValue = settingsMaxSpeed > 0 ? settingsMaxSpeed : router.getMaxSpeed();

			minValue[0] = roundSpeed(Math.min(minSpeedValue, settingsDefaultSpeed) * ratio[0], decimalPrecision);
			maxValue[0] = roundSpeed(Math.max(maxSpeedValue, settingsDefaultSpeed) * ratio[0], decimalPrecision);

			float minSpeed = router.getMinSpeed() / 2f;
			min = Math.round(Math.min(minValue[0], minSpeed * ratio[0]));
		}
		float maxSpeedConfigLimit = VehicleSpeedConfigLimits.getMaxSpeedConfigLimit(app, mode);
		max = Math.round(Math.max(maxValue[0], maxSpeedConfigLimit * ratio[0]));
		return new Pair<>(min, max);
	}

	@NonNull
	private float[] getSpeedRatio() {
		float[] ratio = new float[1];
		SpeedConstants constants = settings.SPEED_SYSTEM.getModeValue(mode);
		switch (constants) {
			case MILES_PER_HOUR -> ratio[0] = 3600 / OsmAndFormatter.METERS_IN_ONE_MILE;
			case KILOMETERS_PER_HOUR -> ratio[0] = 3600 / OsmAndFormatter.METERS_IN_KILOMETER;
			case MINUTES_PER_KILOMETER -> ratio[0] = 3600 / OsmAndFormatter.METERS_IN_KILOMETER;
			case NAUTICALMILES_PER_HOUR ->
					ratio[0] = 3600 / OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE;
			case MINUTES_PER_MILE -> ratio[0] = 3600 / OsmAndFormatter.METERS_IN_ONE_MILE;
			case METERS_PER_SECOND -> ratio[0] = 1;
		}
		return ratio;
	}

	@NonNull
	private String getSpeedUnits() {
		SpeedConstants constants = settings.SPEED_SYSTEM.getModeValue(mode);
		switch (constants) {
			case MINUTES_PER_KILOMETER:
				return app.getString(R.string.km_h);
			case MINUTES_PER_MILE:
				return app.getString(R.string.mile_per_hour);
		}
		return constants.toShortString();
	}

	@NonNull
	private float[] getSpeedValue(@NonNull SpeedSliderType type, @NonNull float[] defaultValue,
	                              @NonNull float[] minValue, @NonNull float[] maxValue) {
		switch (type) {
			case MIN_SPEED:
				return minValue;
			case MAX_SPEED:
				return maxValue;
		}
		return defaultValue;
	}

	@NonNull
	private String formatSpeed(float speed, boolean decimalPrecision) {
		return decimalPrecision ? OsmAndFormatter.formatValue(speed, "", true, 1, app).value : String.valueOf((int) speed);
	}

	private float roundSpeed(float speed, boolean decimalPrecision) {
		return decimalPrecision ? Math.round(speed * 10) / 10f : Math.round(speed);
	}
}
