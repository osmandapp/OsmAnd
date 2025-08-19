package net.osmand.plus.views.mapwidgets.configure.settings;

import static net.osmand.plus.views.mapwidgets.widgets.SunriseSunsetWidget.formatNextTime;
import static net.osmand.plus.views.mapwidgets.widgets.SunriseSunsetWidget.formatTimeLeft;

import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.SunPositionMode;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.SunriseSunsetWidget;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

public class SunriseSunsetInfoFragment extends BaseSimpleWidgetInfoFragment {

	private static final String SHOW_TIME_TO_LEFT = "show_time_to_left";
	private static final String SUN_POSITION_MODE = "sun_position_mode";

	private static final int UPDATE_UI_PERIOD_MS = 60_000; // every minute

	private OsmandPreference<Boolean> preference;
	@Nullable
	private OsmandPreference<SunPositionMode> sunPositionPreference;
	private SunriseSunsetWidget widget;
	private TextView timeDescription;
	private TextView sunPositionDescription;

	private int selectedSunPositionMode;
	private boolean showTimeToLeft;
	private boolean updateEnable;


	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo != null) {
			widget = (SunriseSunsetWidget) widgetInfo.widget;
			preference = widget.getPreference();
			sunPositionPreference = widget.getSunPositionPreference();
		}
		boolean defaultShowTimeToLeft = preference.getModeValue(appMode);
		showTimeToLeft = bundle.getBoolean(SHOW_TIME_TO_LEFT, defaultShowTimeToLeft);
		selectedSunPositionMode = bundle.getInt(SUN_POSITION_MODE, sunPositionPreference != null ?
				sunPositionPreference.getModeValue(appMode).ordinal() : SunPositionMode.SUN_POSITION_MODE.ordinal());
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		inflate(R.layout.fragment_widget_settings_sunrise_sunset, container);
		timeDescription = container.findViewById(R.id.preference_description);
		sunPositionDescription = container.findViewById(R.id.sun_position_description);

		View preferenceButton = container.findViewById(R.id.preference_container);
		preferenceButton.setOnClickListener(v -> showPreferenceDialog());
		preferenceButton.setBackground(getPressedStateDrawable());
		if (getWidget() == WidgetType.SUN_POSITION) {
			setupSunPositionMode();
		}
	}

	private void setupSunPositionMode() {
		View divider = view.findViewById(R.id.sun_position_divider);
		View sunPositionButton = view.findViewById(R.id.sun_position_container);
		sunPositionButton.setOnClickListener(v -> showSunPositionDialog());
		sunPositionButton.setBackground(getPressedStateDrawable());

		AndroidUiHelper.updateVisibility(divider, true);
		AndroidUiHelper.updateVisibility(sunPositionButton, true);
	}

	private void showSunPositionDialog() {
		CharSequence[] items = new CharSequence[SunPositionMode.values().length];
		for (int i = 0; i < SunPositionMode.values().length; i++) {
			items[i] = getString(SunPositionMode.values()[i].getTitleId());
		}

		AlertDialogData dialogData = new AlertDialogData(timeDescription.getContext(), nightMode)
				.setTitle(R.string.shared_string_mode)
				.setControlsColor(ColorUtilities.getActiveColor(app, nightMode));

		CustomAlert.showSingleSelection(dialogData, items, selectedSunPositionMode, v -> {
			selectedSunPositionMode = (int) v.getTag();
			updateSunPositionDescription();
		});
	}

	private void updateSunPositionDescription() {
		sunPositionDescription.setText(getString(SunPositionMode.values()[selectedSunPositionMode].getTitleId()));
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		preference.setModeValue(appMode, showTimeToLeft);
		if (sunPositionPreference != null) {
			sunPositionPreference.setModeValue(appMode, SunPositionMode.values()[selectedSunPositionMode]);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SHOW_TIME_TO_LEFT, showTimeToLeft);
		outState.putInt(SUN_POSITION_MODE, selectedSunPositionMode);
	}

	private void showPreferenceDialog() {
		CharSequence[] items = new CharSequence[2];
		items[0] = getFormattedTime(true);
		items[1] = getFormattedTime(false);
		int selected = showTimeToLeft ? 0 : 1;

		AlertDialogData dialogData = new AlertDialogData(timeDescription.getContext(), nightMode)
				.setTitle(R.string.shared_string_show)
				.setControlsColor(ColorUtilities.getActiveColor(app, nightMode));

		CustomAlert.showSingleSelection(dialogData, items, selected, v -> {
			int which = (int) v.getTag();
			showTimeToLeft = which == 0;
			updateTimeDescription();
		});
	}

	private void updateTimeDescription() {
		timeDescription.setText(getFormattedTime(showTimeToLeft));
	}

	private CharSequence getFormattedTime(boolean showTimeToLeft) {
		String title;
		String preview;
		if (showTimeToLeft) {
			title = getString(R.string.shared_string_time_left);
			preview = formatTimeLeft(app, widget.getTimeLeft());
		} else {
			title = getString(getNextEventStringId());
			preview = formatNextTime(widget.getNextTime());
		}
		if (preview != null) {
			String fullText = getString(R.string.ltr_or_rtl_combine_via_comma, title, preview);
			SpannableString spannable = new SpannableString(fullText);
			int secondaryTextColor = ColorUtilities.getSecondaryTextColor(app, nightMode);
			int startIndex = fullText.indexOf(preview);
			int endIndex = startIndex + preview.length();
			spannable.setSpan(new ForegroundColorSpan(secondaryTextColor), startIndex, endIndex, 0);
			return spannable;
		} else {
			return title;
		}
	}

	private int getNextEventStringId() {
		return switch (getWidget()) {
			case SUN_POSITION -> R.string.shared_string_next_event;
			case SUNSET -> R.string.shared_string_next_sunset;
			default -> R.string.shared_string_next_sunrise;
		};
	}

	@Override
	public void onResume() {
		super.onResume();
		updateEnable = true;
		updateTimeDescription();
		updateSunPositionDescription();
		startHandler();
	}

	@Override
	public void onPause() {
		super.onPause();
		updateEnable = false;
	}

	private void startHandler() {
		Handler handler = new Handler();
		handler.postDelayed(() -> {
			if (getView() != null && updateEnable) {
				updateTimeDescription();
				updateSunPositionDescription();
				startHandler();
			}
		}, UPDATE_UI_PERIOD_MS);
	}
}