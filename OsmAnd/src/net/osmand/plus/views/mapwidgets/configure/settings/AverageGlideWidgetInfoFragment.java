package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.GlideAverageWidget;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

public class AverageGlideWidgetInfoFragment extends BaseSimpleWidgetInfoFragment {

	private static final String KEY_TIME_INTERVAL = "time_interval";
	private static final String KEY_VERTICAL_SPEED_MODE = "vertical_speed_mode";

	private GlideAverageWidget widget;

	private long initialIntervalMillis;
	private boolean verticalSpeedMode;

	private TimeIntervalCard timeIntervalCard;
	private TextView modeDescription;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return WidgetType.GLIDE_AVERAGE;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo != null) {
			widget = ((GlideAverageWidget) widgetInfo.widget);
			long defaultInterval = widget.getMeasuredInterval(appMode);
			initialIntervalMillis = bundle.getLong(KEY_TIME_INTERVAL, defaultInterval);
			boolean defaultMode = widget.getModePreference().getModeValue(appMode);
			verticalSpeedMode = bundle.getBoolean(KEY_VERTICAL_SPEED_MODE, defaultMode);
		} else {
			dismiss();
		}
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		inflate(R.layout.fragment_widget_settings_average_glide_ratio, container);

		View modeButton = view.findViewById(R.id.mode_container);
		modeButton.setOnClickListener(v -> showModeDialog());
		modeButton.setBackground(getPressedStateDrawable());
		modeDescription = view.findViewById(R.id.mode_description);
		updateModeDescription();

		timeIntervalCard = new TimeIntervalCard(requireMyActivity(), appMode, isUsedOnMap(), initialIntervalMillis);
		ViewGroup cardContainer = view.findViewById(R.id.time_interval_card_container);
		cardContainer.addView(timeIntervalCard.build(cardContainer.getContext()));
	}

	private void showModeDialog() {
		CharSequence[] items = {getString(getModeTitleId(false)), getString(getModeTitleId(true))};
		AlertDialogData dialogData = new AlertDialogData(modeDescription.getContext(), nightMode)
				.setTitle(R.string.shared_string_mode)
				.setControlsColor(ColorUtilities.getActiveColor(app, nightMode));

		CustomAlert.showSingleSelection(dialogData, items, verticalSpeedMode ? 1 : 0, v -> {
			verticalSpeedMode = (int) v.getTag() == 1;
			updateModeDescription();
		});
	}

	private void updateModeDescription() {
		modeDescription.setText(getModeTitleId(verticalSpeedMode));
	}

	@StringRes
	private static int getModeTitleId(boolean verticalSpeedMode) {
		return verticalSpeedMode ? R.string.average_vertical_speed : R.string.average_glide_ratio;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(KEY_TIME_INTERVAL, timeIntervalCard.getSelectedIntervalMillis());
		outState.putBoolean(KEY_VERTICAL_SPEED_MODE, verticalSpeedMode);
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		widget.setMeasuredInterval(appMode, timeIntervalCard.getSelectedIntervalMillis());
		widget.getModePreference().setModeValue(appMode, verticalSpeedMode);
	}

	@Override
	public String getAnalyticsScreen() {
		return "average_glide_widget_info";
	}
}
