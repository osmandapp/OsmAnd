package net.osmand.plus.views.mapwidgets.configure.settings;

import static net.osmand.plus.views.mapwidgets.widgetstates.InclinometerWidgetState.MAX_THRESHOLD_ANGLE;
import static net.osmand.plus.views.mapwidgets.widgetstates.InclinometerWidgetState.MIN_THRESHOLD_ANGLE;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.slider.RangeSlider;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.utils.InclinometerSensor;
import net.osmand.plus.views.mapwidgets.widgets.InclinometerWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.InclinometerWidgetState;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.SingleSelectionDialogFragment;

import java.util.List;
import java.util.Locale;

public class InclinometerWidgetInfoFragment extends BaseSimpleWidgetInfoFragment {

	private static final String KEY_PERCENT_UNITS = "percent_units";
	private static final String KEY_WARNING_ANGLE = "warning_angle";
	private static final String KEY_CRITICAL_ANGLE = "critical_angle";
	private static final String KEY_SOUND_ALERT = "sound_alert";
	private static final String UNITS_REQUEST_KEY = "units_request_key";

	private InclinometerWidget widget;
	private InclinometerWidgetState widgetState;
	private InclinometerSensor sensor;

	private boolean percentUnits;
	private int warningAngle;
	private int criticalAngle;
	private boolean soundAlert;

	private TextView unitsDescription;
	private RangeSlider slider;
	private TextView warningAngleLabel;
	private TextView criticalAngleLabel;
	private TextView warningAngleValue;
	private TextView criticalAngleValue;
	private TextView calibrationStatus;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return widgetState != null ? widgetState.getWidgetType() : WidgetType.INCLINOMETER_PITCH;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo != null && widgetInfo.widget instanceof InclinometerWidget inclinometerWidget) {
			widget = inclinometerWidget;
			widgetState = widget.getWidgetState();
			sensor = app.getInclinometerSensor();
			percentUnits = bundle.getBoolean(KEY_PERCENT_UNITS, widgetState.getPercentUnitsPref().getModeValue(appMode));
			warningAngle = bundle.getInt(KEY_WARNING_ANGLE, widgetState.getWarningAnglePref().getModeValue(appMode));
			criticalAngle = bundle.getInt(KEY_CRITICAL_ANGLE, widgetState.getCriticalAnglePref().getModeValue(appMode));
			soundAlert = bundle.getBoolean(KEY_SOUND_ALERT, widgetState.getSoundAlertPref().getModeValue(appMode));
		} else {
			dismiss();
		}
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		inflate(R.layout.fragment_widget_settings_inclinometer, container);

		unitsDescription = view.findViewById(R.id.units_description);
		View unitsButton = view.findViewById(R.id.units_container);
		unitsButton.setOnClickListener(v -> showUnitsDialog());
		unitsButton.setBackground(getPressedStateDrawable());
		updateUnitsDescription();
		SingleSelectionDialogFragment.setResultListener(this, UNITS_REQUEST_KEY, index -> {
			percentUnits = index == 1;
			updateUnitsDescription();
		});

		setupThresholdsSlider();

		SwitchCompat soundAlertToggle = view.findViewById(R.id.sound_alert_toggle);
		soundAlertToggle.setChecked(soundAlert);
		View soundAlertButton = view.findViewById(R.id.sound_alert_container);
		soundAlertButton.setOnClickListener(v -> {
			soundAlert = !soundAlert;
			soundAlertToggle.setChecked(soundAlert);
		});
		soundAlertButton.setBackground(getPressedStateDrawable());

		calibrationStatus = view.findViewById(R.id.calibration_status);
		View calibrationButton = view.findViewById(R.id.calibration_container);
		calibrationButton.setOnClickListener(v ->
				InclinometerCalibrationFragment.showInstance(requireActivity().getSupportFragmentManager()));
		calibrationButton.setBackground(getPressedStateDrawable());
		updateCalibrationStatus();
	}

	private void setupThresholdsSlider() {
		slider = view.findViewById(R.id.thresholds_slider);
		slider.setValueFrom(MIN_THRESHOLD_ANGLE);
		slider.setValueTo(MAX_THRESHOLD_ANGLE);
		slider.setValues((float) warningAngle, (float) criticalAngle);
		slider.addOnChangeListener((rangeSlider, value, fromUser) -> {
			List<Float> values = rangeSlider.getValues();
			if (values.size() > 1) {
				warningAngle = Math.round(values.get(0));
				criticalAngle = Math.round(values.get(1));
				updateAngleValues();
			}
		});
		UiUtilities.setupSlider(slider, nightMode, ColorUtilities.getActiveColor(app, nightMode), false);
		// The Material labels float in the window overlay and do not scroll with the page,
		// so the thumb values are drawn by plain text views positioned above the thumbs
		warningAngleLabel = view.findViewById(R.id.warning_angle_label);
		criticalAngleLabel = view.findViewById(R.id.critical_angle_label);
		slider.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> updateThumbLabels());

		TextView minLabel = view.findViewById(R.id.slider_min_label);
		TextView maxLabel = view.findViewById(R.id.slider_max_label);
		minLabel.setText(formatAngle(MIN_THRESHOLD_ANGLE));
		maxLabel.setText(formatAngle(MAX_THRESHOLD_ANGLE));

		warningAngleValue = view.findViewById(R.id.warning_angle_value);
		criticalAngleValue = view.findViewById(R.id.critical_angle_value);
		updateAngleValues();
	}

	@Override
	public void onResume() {
		super.onResume();
		updateCalibrationStatus();
	}

	private void showUnitsDialog() {
		CharSequence[] items = {getString(R.string.shared_string_degrees), getString(R.string.shared_string_percent)};
		AlertDialogData dialogData = new AlertDialogData(unitsDescription.getContext(), nightMode)
				.setTitle(R.string.inclinometer_slope_unit)
				.setControlsColor(ColorUtilities.getActiveColor(app, nightMode));
		SingleSelectionDialogFragment.showInstance(this, UNITS_REQUEST_KEY, dialogData, items, percentUnits ? 1 : 0);
	}

	@NonNull
	private static String formatAngle(float degrees) {
		return String.format(Locale.getDefault(), "%d°", Math.round(degrees));
	}

	@NonNull
	public static String formatOffset(float degrees) {
		float rounded = Math.abs(degrees) < 0.05f ? 0 : degrees;
		return String.format(Locale.getDefault(), "%.1f°", rounded);
	}

	private void updateUnitsDescription() {
		unitsDescription.setText(percentUnits ? R.string.shared_string_percent : R.string.shared_string_degrees);
	}

	private void updateAngleValues() {
		warningAngleValue.setText(formatAngle(warningAngle));
		criticalAngleValue.setText(formatAngle(criticalAngle));
		warningAngleLabel.setText(formatAngle(warningAngle));
		criticalAngleLabel.setText(formatAngle(criticalAngle));
		updateThumbLabels();
	}

	private void updateThumbLabels() {
		if (slider == null || slider.getWidth() == 0) {
			return;
		}
		positionThumbLabel(warningAngleLabel, warningAngle);
		positionThumbLabel(criticalAngleLabel, criticalAngle);
	}

	private void positionThumbLabel(@NonNull TextView label, int angle) {
		float fraction = (angle - slider.getValueFrom()) / (slider.getValueTo() - slider.getValueFrom());
		float thumbX = slider.getTrackSidePadding() + fraction * slider.getTrackWidth();
		if (label.getWidth() == 0) {
			label.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		}
		int width = label.getWidth() > 0 ? label.getWidth() : label.getMeasuredWidth();
		float x = Math.max(0, Math.min(slider.getWidth() - width, thumbX - width / 2f));
		label.setTranslationX(AndroidUtils.isLayoutRtl(app) ? -x : x);
	}

	private void updateCalibrationStatus() {
		if (calibrationStatus == null) {
			return;
		}
		float[] offset = sensor.getCalibrationOffset();
		String status;
		if (!sensor.isSensorAvailable()) {
			status = getString(R.string.inclinometer_sensor_unavailable);
		} else if (offset != null) {
			status = getString(R.string.ltr_or_rtl_combine_via_bold_point,
					getString(R.string.inclinometer_calibrated),
					getString(R.string.inclinometer_calibration_offset, formatOffset(offset[0]), formatOffset(offset[1])));
		} else {
			status = getString(R.string.inclinometer_not_calibrated);
		}
		calibrationStatus.setText(status);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_PERCENT_UNITS, percentUnits);
		outState.putInt(KEY_WARNING_ANGLE, warningAngle);
		outState.putInt(KEY_CRITICAL_ANGLE, criticalAngle);
		outState.putBoolean(KEY_SOUND_ALERT, soundAlert);
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		widgetState.getPercentUnitsPref().setModeValue(appMode, percentUnits);
		widgetState.getWarningAnglePref().setModeValue(appMode, warningAngle);
		widgetState.getCriticalAnglePref().setModeValue(appMode, criticalAngle);
		widgetState.getSoundAlertPref().setModeValue(appMode, soundAlert);
		widget.updateInfo(null);
	}
}
