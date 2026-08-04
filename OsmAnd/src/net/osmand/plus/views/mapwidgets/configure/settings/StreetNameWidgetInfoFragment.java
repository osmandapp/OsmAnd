package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.StreetNameWidget;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.AlertDialogExtra;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.shared.settings.enums.MetricsConstants;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

public class StreetNameWidgetInfoFragment extends WidgetInfoBaseFragment {

	private static final String SHOW_NEXT_TURN = "show_next_turn";
	private static final String CUSTOM_APPROACH_THRESHOLDS = "custom_approach_thresholds";
	private static final String APPROACH_POI_DISTANCE = "approach_poi_distance";
	private static final String APPROACH_POI_TIME = "approach_poi_time";

	private boolean showNextTurn;
	private boolean customApproachThresholds;
	private int approachPoiDistance;
	private int approachPoiTime;
	private StreetNameWidget widget;

	private View approachPoiDistanceView;
	private View approachPoiTimeView;

	private TextView approachPoiDistanceValue;
	private TextView approachPoiTimeValue;
	private ImageView approachPoiDistanceIcon;
	private ImageView approachPoiTimeIcon;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return WidgetType.STREET_NAME;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo != null && widgetInfo.widget instanceof StreetNameWidget w) {
			widget = w;
			showNextTurn = bundle.getBoolean(SHOW_NEXT_TURN, widget.isShowNextTurnEnabled(appMode));
			customApproachThresholds = bundle.getBoolean(CUSTOM_APPROACH_THRESHOLDS, widget.getWidgetState().isCustomApproachThresholdsEnabled(appMode));
			approachPoiDistance = bundle.getInt(APPROACH_POI_DISTANCE, widget.getWidgetState().getApproachPoiDistance(appMode));
			approachPoiTime = bundle.getInt(APPROACH_POI_TIME, widget.getWidgetState().getApproachPoiTime(appMode));
		} else {
			dismiss();
		}
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		View nextTurnView = inflate(R.layout.widget_preference_with_switch, container, false);
		container.addView(nextTurnView);
		setupShowNextTurnInfoPref(nextTurnView);

		View thresholdsToggleView = inflate(R.layout.widget_preference_with_switch, container, false);
		container.addView(thresholdsToggleView);
		setupCustomApproachThresholdsPref(thresholdsToggleView);

		approachPoiDistanceView = inflate(R.layout.bottom_sheet_item_with_descr_72dp, container, false);
		container.addView(approachPoiDistanceView);
		setupApproachPoiDistancePref(approachPoiDistanceView);

		approachPoiTimeView = inflate(R.layout.bottom_sheet_item_with_descr_72dp, container, false);
		container.addView(approachPoiTimeView);
		setupApproachPoiTimePref(approachPoiTimeView);

		updateVisibility();
	}

	private void updateVisibility() {
		AndroidUiHelper.updateVisibility(approachPoiDistanceView, customApproachThresholds);
		AndroidUiHelper.updateVisibility(approachPoiTimeView, customApproachThresholds);
	}

	private void setupShowNextTurnInfoPref(@NonNull View view) {
		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		title.setText(R.string.next_turn_information);
		description.setText(R.string.next_turn_information_desc);
		updateShowNextTurnInfoPrefIcon(view);

		CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setChecked(showNextTurn);
		compoundButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
			showNextTurn = isChecked;
			updateShowNextTurnInfoPrefIcon(view);
		});

		view.setOnClickListener(v -> compoundButton.setChecked(!compoundButton.isChecked()));
		view.setBackground(getPressedStateDrawable());
	}

	private void updateShowNextTurnInfoPrefIcon(@NonNull View view) {
		ImageView icon = view.findViewById(R.id.icon);
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		int defaultColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		int iconColor = showNextTurn ? activeColor : defaultColor;
		icon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_next_turn, iconColor));
		icon.setVisibility(View.VISIBLE);
	}

	private void setupCustomApproachThresholdsPref(@NonNull View view) {
		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		title.setText(R.string.custom_approach_thresholds);
		description.setText(R.string.custom_approach_thresholds_descr);
		updateCustomApproachThresholdsPrefIcon(view);

		CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setChecked(customApproachThresholds);
		compoundButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
			customApproachThresholds = isChecked;
			updateCustomApproachThresholdsPrefIcon(view);
			updateVisibility();
		});

		view.setOnClickListener(v -> compoundButton.setChecked(!compoundButton.isChecked()));
		view.setBackground(getPressedStateDrawable());
	}

	private void updateCustomApproachThresholdsPrefIcon(@NonNull View view) {
		ImageView icon = view.findViewById(R.id.icon);
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		int defaultColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		int iconColor = customApproachThresholds ? activeColor : defaultColor;
		icon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_settings_outlined, iconColor));
		icon.setVisibility(View.VISIBLE);
	}

	private void setupApproachPoiDistancePref(@NonNull View view) {
		approachPoiDistanceIcon = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.title);
		approachPoiDistanceValue = view.findViewById(R.id.description);

		MetricsConstants mc = settings.METRIC_SYSTEM.getModeValue(appMode);
		String unitName = getSmallDistanceUnitName(mc);
		title.setText(getString(R.string.ltr_or_rtl_combine_via_space, getString(R.string.approach_poi_distance), "(" + unitName + ")"));
		updateApproachPoiDistanceValue();

		view.setOnClickListener(v -> showDistanceDialog());
		view.setBackground(getPressedStateDrawable());
	}

	private String getSmallDistanceUnitName(MetricsConstants mc) {
		return switch (mc) {
			case MILES_AND_FEET, NAUTICAL_MILES_AND_FEET -> getString(R.string.foot);
			case MILES_AND_YARDS -> getString(R.string.yard);
			default -> getString(R.string.m);
		};
	}

	private void updateApproachPoiDistanceValue() {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		int defaultColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		int iconColor = approachPoiDistance > 0 ? activeColor : defaultColor;
		approachPoiDistanceIcon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_waypoint, iconColor));

		if (approachPoiDistance > 0) {
			approachPoiDistanceValue.setText(OsmAndFormatter.getFormattedDistance(approachPoiDistance, app));
		} else {
			approachPoiDistanceValue.setText(R.string.shared_string_default);
		}
	}

	private void setupApproachPoiTimePref(@NonNull View view) {
		approachPoiTimeIcon = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.title);
		approachPoiTimeValue = view.findViewById(R.id.description);

		title.setText(getString(R.string.ltr_or_rtl_combine_via_space, getString(R.string.approach_poi_time), "(" + getString(R.string.shared_string_sec) + ")"));
		updateApproachPoiTimeValue();

		view.setOnClickListener(v -> showTimeDialog());
		view.setBackground(getPressedStateDrawable());
	}

	private void updateApproachPoiTimeValue() {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		int defaultColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		int iconColor = approachPoiTime > 0 ? activeColor : defaultColor;
		approachPoiTimeIcon.setImageDrawable(getPaintedIcon(R.drawable.ic_action_time, iconColor));

		if (approachPoiTime > 0) {
			approachPoiTimeValue.setText(app.getString(R.string.ltr_or_rtl_combine_via_space, "" + approachPoiTime, getString(R.string.shared_string_sec)));
		} else {
			approachPoiTimeValue.setText(R.string.shared_string_default);
		}
	}

	private void showDistanceDialog() {
		MetricsConstants mc = settings.METRIC_SYSTEM.getModeValue(appMode);
		String unitName = getSmallDistanceUnitName(mc);

		AlertDialogData data = new AlertDialogData(requireContext(), nightMode);
		data.setTitle(getString(R.string.ltr_or_rtl_combine_via_space, getString(R.string.approach_poi_distance), "(" + unitName + ")"));
		data.setPositiveButton(R.string.shared_string_apply, (dialog, which) -> {
			ExtendedEditText editText = (ExtendedEditText) data.getExtra(AlertDialogExtra.EDIT_TEXT);
			if (editText != null) {
				try {
					double value = Double.parseDouble(editText.getText().toString());
					approachPoiDistance = (int) convertToMeters(value, mc);
					updateApproachPoiDistanceValue();
				} catch (NumberFormatException e) {
					// ignore
				}
			}
		});
		data.setNeutralButton(R.string.shared_string_default, (dialog, which) -> {
			approachPoiDistance = 0;
			updateApproachPoiDistanceValue();
		});
		data.setNegativeButton(R.string.shared_string_cancel, null);

		double displayValue = convertFromMeters(approachPoiDistance, mc);
		CustomAlert.showInput(data, requireActivity(), String.valueOf((int) (displayValue + 0.5)), getString(R.string.approach_poi_distance_descr));
	}

	private double convertFromMeters(int meters, MetricsConstants mc) {
		if (mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
			return meters * OsmAndFormatter.FEET_IN_ONE_METER;
		} else if (mc == MetricsConstants.MILES_AND_YARDS) {
			return meters * OsmAndFormatter.YARDS_IN_ONE_METER;
		}
		return meters;
	}

	private double convertToMeters(double value, MetricsConstants mc) {
		if (mc == MetricsConstants.MILES_AND_FEET || mc == MetricsConstants.NAUTICAL_MILES_AND_FEET) {
			return value / OsmAndFormatter.FEET_IN_ONE_METER;
		} else if (mc == MetricsConstants.MILES_AND_YARDS) {
			return value / OsmAndFormatter.YARDS_IN_ONE_METER;
		}
		return value;
	}

	private void showTimeDialog() {
		AlertDialogData data = new AlertDialogData(requireContext(), nightMode);
		data.setTitle(getString(R.string.ltr_or_rtl_combine_via_space, getString(R.string.approach_poi_time), "(" + getString(R.string.shared_string_sec) + ")"));
		data.setPositiveButton(R.string.shared_string_apply, (dialog, which) -> {
			ExtendedEditText editText = (ExtendedEditText) data.getExtra(AlertDialogExtra.EDIT_TEXT);
			if (editText != null) {
				try {
					approachPoiTime = Integer.parseInt(editText.getText().toString());
					updateApproachPoiTimeValue();
				} catch (NumberFormatException e) {
					// ignore
				}
			}
		});
		data.setNeutralButton(R.string.shared_string_default, (dialog, which) -> {
			approachPoiTime = 0;
			updateApproachPoiTimeValue();
		});
		data.setNegativeButton(R.string.shared_string_cancel, null);

		CustomAlert.showInput(data, requireActivity(), String.valueOf(approachPoiTime), getString(R.string.approach_poi_time_descr));
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SHOW_NEXT_TURN, showNextTurn);
		outState.putBoolean(CUSTOM_APPROACH_THRESHOLDS, customApproachThresholds);
		outState.putInt(APPROACH_POI_DISTANCE, approachPoiDistance);
		outState.putInt(APPROACH_POI_TIME, approachPoiTime);
	}

	@Override
	protected void applySettings() {
		widget.setShowNextTurnEnabled(appMode, showNextTurn);
		widget.getWidgetState().setCustomApproachThresholdsEnabled(appMode, customApproachThresholds);
		widget.getWidgetState().setApproachPoiDistance(appMode, approachPoiDistance);
		widget.getWidgetState().setApproachPoiTime(appMode, approachPoiTime);
	}
}
