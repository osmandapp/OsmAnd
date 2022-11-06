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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

import net.osmand.plus.DialogListItemAdapter;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.SunriseSunsetWidget;

import java.util.Timer;
import java.util.TimerTask;

public class SunriseSunsetSettingsFragment extends WidgetSettingsBaseFragment {

	private static final String SHOW_TIME_TO_LEFT = "show_time_to_left";

	private static final int UPDATE_UI_PERIOD_MS = 60_000; // every minute

	private TextView tvPreferenceDesc;
	private Timer updateUiTimer;
	private Handler handler;

	private OsmandPreference<Boolean> preference;
	private SunriseSunsetWidget widget;
	boolean showTimeToLeft;
	boolean sunriseMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler();
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
		View preferenceButton = container.findViewById(R.id.preference_container);
		tvPreferenceDesc = container.findViewById(R.id.preference_description);
		preferenceButton.setOnClickListener(v -> {
			showPreferenceDialog();
		});
		preferenceButton.setBackground(getPressedStateDrawable());
	}

	private void showPreferenceDialog() {
		AlertDialog.Builder bld = new AlertDialog.Builder(new ContextThemeWrapper(getMapActivity(), getThemeRes(nightMode)));
		bld.setTitle(R.string.shared_string_show);
		CharSequence[] items = new CharSequence[2];

		items[0] = getFormattedTime(true);
		items[1] = getFormattedTime(false);

		int selected = showTimeToLeft ? 0 : 1;
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);

		DialogListItemAdapter dialogAdapter = DialogListItemAdapter.createSingleChoiceAdapter(
				items, nightMode, selected, app, activeColor, getThemeRes(nightMode), v -> {
					int which = (int) v.getTag();
					showTimeToLeft = which == 0;
					updatePreferenceDescription();
				}
		);
		bld.setAdapter(dialogAdapter, null);
		dialogAdapter.setDialog(bld.show());
	}

	private void updatePreferenceDescription() {
		String description = getFormattedTime(showTimeToLeft).toString();
		tvPreferenceDesc.setText(description);
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

	@NonNull
	@Override
	public WidgetType getWidget() {
		return sunriseMode ? WidgetType.SUNRISE : WidgetType.SUNSET;
	}

	@Override
	protected void applySettings() {
		preference.setModeValue(appMode, showTimeToLeft);
	}

	@Override
	public void onResume() {
		super.onResume();
		startAutoUiUpdate();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopAutoUiUpdate();
	}

	private void startAutoUiUpdate() {
		if (updateUiTimer != null) {
			updateUiTimer.cancel();
		}
		updatePreferenceDescription();
		long delay = 0;
		long timeLeft = widget.getTimeLeft();
		if (timeLeft > 0) {
			delay = timeLeft % UPDATE_UI_PERIOD_MS;
		}
		updateUiTimer = new Timer();
		updateUiTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.post(() -> {
					updatePreferenceDescription();
				});
			}
		}, delay, UPDATE_UI_PERIOD_MS);
	}

	private void stopAutoUiUpdate() {
		if (updateUiTimer != null) {
			updateUiTimer.cancel();
			updateUiTimer = null;
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SHOW_TIME_TO_LEFT, showTimeToLeft);
	}
}
