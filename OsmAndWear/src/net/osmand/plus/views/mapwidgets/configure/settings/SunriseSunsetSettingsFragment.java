package net.osmand.plus.views.mapwidgets.configure.settings;

import static net.osmand.plus.views.mapwidgets.widgets.SunriseSunsetWidget.formatNextTime;
import static net.osmand.plus.views.mapwidgets.widgets.SunriseSunsetWidget.formatTimeLeft;

import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.SunriseSunsetWidget;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

public class SunriseSunsetSettingsFragment extends BaseSimpleWidgetSettingsFragment {

	private static final String SHOW_TIME_TO_LEFT = "show_time_to_left";

	private static final int UPDATE_UI_PERIOD_MS = 60_000; // every minute

	private OsmandPreference<Boolean> preference;
	private SunriseSunsetWidget widget;
	private TextView timeDescription;

	private boolean sunriseMode;
	private boolean showTimeToLeft;
	private boolean updateEnable;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return sunriseMode ? WidgetType.SUNRISE : WidgetType.SUNSET;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		MapWidgetInfo widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
		if (widgetInfo != null) {
			widget = (SunriseSunsetWidget) widgetInfo.widget;
			sunriseMode = widget.isSunriseMode();
			preference = widget.getPreference();
		}
		boolean defaultShowTimeToLeft = preference.getModeValue(appMode);
		showTimeToLeft = bundle.getBoolean(SHOW_TIME_TO_LEFT, defaultShowTimeToLeft);
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		themedInflater.inflate(R.layout.fragment_widget_settings_sunrise_sunset, container);
		timeDescription = container.findViewById(R.id.preference_description);

		View preferenceButton = container.findViewById(R.id.preference_container);
		preferenceButton.setOnClickListener(v -> showPreferenceDialog());
		preferenceButton.setBackground(getPressedStateDrawable());
		themedInflater.inflate(R.layout.divider, container);
		super.setupContent(themedInflater, container);
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		preference.setModeValue(appMode, showTimeToLeft);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SHOW_TIME_TO_LEFT, showTimeToLeft);
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
			title = getString(sunriseMode ? R.string.shared_string_next_sunrise : R.string.shared_string_next_sunset);
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

	@Override
	public void onResume() {
		super.onResume();
		updateEnable = true;
		updateTimeDescription();
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
				startHandler();
			}
		}, UPDATE_UI_PERIOD_MS);
	}
}