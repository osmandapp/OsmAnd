package net.osmand.plus.settings.fragments;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.util.Algorithms;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;
import static net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin.MAX_INTERVAL_TO_SEND_MINUTES;
import static net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin.MINUTES;
import static net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin.SECONDS;

public class LiveMonitoringFragment extends BaseSettingsFragment {

	@Override
	protected void setupPreferences() {
		Preference liveMonitoringInfo = findPreference("live_monitoring_info");
		liveMonitoringInfo.setIconSpaceReserved(false);

		setupLiveMonitoringUrlPref();
		setupLiveMonitoringIntervalPref();
		setupLiveMonitoringBufferPref();
		enableDisablePreferences(settings.LIVE_MONITORING.getModeValue(getSelectedAppMode()));
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);

		view.findViewById(R.id.toolbar_switch_container).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ApplicationMode appMode = getSelectedAppMode();
				boolean checked = !settings.LIVE_MONITORING.getModeValue(appMode);
				onConfirmPreferenceChange(settings.LIVE_MONITORING.getId(), checked, ApplyQueryType.SNACK_BAR);
				updateToolbarSwitch();
				enableDisablePreferences(checked);
			}
		});
		TextView title = view.findViewById(R.id.switchButtonText);
		title.setTextColor(ContextCompat.getColor(app, ColorUtilities.getActiveColorId(isNightMode())));
	}

	@Override
	protected void updateToolbar() {
		super.updateToolbar();
		updateToolbarSwitch();
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals(settings.LIVE_MONITORING_URL.getId())) {
			if (Algorithms.isValidMessageFormat((String) newValue)) {
				return super.onPreferenceChange(preference, newValue);
			} else {
				Toast.makeText(app, R.string.wrong_format, Toast.LENGTH_SHORT).show();
				return false;
			}
		}
		return super.onPreferenceChange(preference, newValue);
	}

	private void updateToolbarSwitch() {
		View view = getView();
		if (view == null) {
			return;
		}
		boolean checked = settings.LIVE_MONITORING.getModeValue(getSelectedAppMode());
		int color = checked ? getActiveProfileColor() : ContextCompat.getColor(app, R.color.preference_top_switch_off);

		View selectableView = view.findViewById(R.id.selectable_item);
		View switchContainer = view.findViewById(R.id.toolbar_switch_container);

		AndroidUtils.setBackground(switchContainer, new ColorDrawable(color));

		SwitchCompat switchView = selectableView.findViewById(R.id.switchWidget);
		switchView.setChecked(checked);
		UiUtilities.setupCompoundButton(switchView, isNightMode(), TOOLBAR);

		TextView title = selectableView.findViewById(R.id.switchButtonText);
		title.setText(checked ? R.string.shared_string_enabled : R.string.shared_string_disabled);
		title.setTextColor(ColorUtilities.getActiveTabTextColor(app, isNightMode()));

		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, getActiveProfileColor(), 0.3f);
		AndroidUtils.setBackground(selectableView, drawable);
	}

	private void setupLiveMonitoringUrlPref() {
		ApplicationMode appMode = getSelectedAppMode();
		String summary;
		if (settings.LIVE_MONITORING_URL.isSetForMode(appMode)) {
			summary = settings.LIVE_MONITORING_URL.getModeValue(appMode);
		} else {
			summary = getString(R.string.shared_string_disabled);
		}

		EditTextPreferenceEx liveMonitoringUrl = findPreference(settings.LIVE_MONITORING_URL.getId());
		liveMonitoringUrl.setSummary(summary);
		liveMonitoringUrl.setDescription(R.string.live_monitoring_adress_descr);
		liveMonitoringUrl.setIcon(getPersistentPrefIcon(R.drawable.ic_world_globe_dark));
	}

	private void setupLiveMonitoringIntervalPref() {
		Integer[] entryValues = new Integer[SECONDS.length + MINUTES.length];
		String[] entries = new String[entryValues.length];
		int k = 0;
		for (int second : SECONDS) {
			entryValues[k] = second * 1000;
			entries[k] = second + " " + getString(R.string.int_seconds);
			k++;
		}
		for (int minute : MINUTES) {
			entryValues[k] = (minute * 60) * 1000;
			entries[k] = minute + " " + getString(R.string.int_min);
			k++;
		}

		ListPreferenceEx liveMonitoringInterval = findPreference(settings.LIVE_MONITORING_INTERVAL.getId());
		liveMonitoringInterval.setEntries(entries);
		liveMonitoringInterval.setEntryValues(entryValues);
		liveMonitoringInterval.setIcon(getPersistentPrefIcon(R.drawable.ic_action_time_span));
		liveMonitoringInterval.setDescription(R.string.live_monitoring_interval_descr);
	}

	private void setupLiveMonitoringBufferPref() {
		Integer[] entryValues = new Integer[MAX_INTERVAL_TO_SEND_MINUTES.length];
		String[] entries = new String[entryValues.length];

		for (int i = 0; i < MAX_INTERVAL_TO_SEND_MINUTES.length; i++) {
			int minute = MAX_INTERVAL_TO_SEND_MINUTES[i];
			entryValues[i] = (minute * 60) * 1000;
			entries[i] = OsmAndFormatter.getFormattedDuration(minute * 60, app);
		}

		ListPreferenceEx liveMonitoringBuffer = findPreference(settings.LIVE_MONITORING_MAX_INTERVAL_TO_SEND.getId());
		liveMonitoringBuffer.setEntries(entries);
		liveMonitoringBuffer.setEntryValues(entryValues);
		liveMonitoringBuffer.setIcon(getPersistentPrefIcon(R.drawable.ic_action_time_span));
		liveMonitoringBuffer.setDescription(R.string.live_monitoring_max_interval_to_send_desrc);
	}
}