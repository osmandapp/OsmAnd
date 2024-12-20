package net.osmand.plus.plugins.accessibility;

import static net.osmand.plus.plugins.PluginInfoFragment.PLUGIN_INFO;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.ResetProfilePrefsBottomSheet;
import net.osmand.plus.settings.bottomsheets.ResetProfilePrefsBottomSheet.ResetAppModePrefsListener;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

public class AccessibilitySettingsFragment extends BaseSettingsFragment implements CopyAppModePrefsListener, ResetAppModePrefsListener {

	private static final String ACCESSIBILITY_OPTIONS = "accessibility_options";
	private static final String COPY_PLUGIN_SETTINGS = "copy_plugin_settings";
	private static final String RESET_TO_DEFAULT = "reset_to_default";

	private AccessibilityStateChangeListener accessibilityListener;

	boolean showSwitchProfile;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		accessibilityListener = enabled -> {
			if (isResumed() && useSystemAccessibility()) {
				updateAllSettings();
			}
		};

		Bundle args = getArguments();
		if (args != null) {
			showSwitchProfile = args.getBoolean(PLUGIN_INFO, false);
		}
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);

		View switchProfile = view.findViewById(R.id.profile_button);
		if (switchProfile != null) {
			AndroidUiHelper.updateVisibility(switchProfile, showSwitchProfile);
		}
	}

	@Override
	public Bundle buildArguments() {
		Bundle args = super.buildArguments();
		args.putBoolean(PLUGIN_INFO, showSwitchProfile);
		return args;
	}

	@Override
	protected void setupPreferences() {
		setupAccessibilityPermissionPref();
		setupAccessibilityModePref();
		setupSpeechRatePref();

		setupSmartAutoAnnouncePref();
		setupAutoAnnouncePeriodPref();

		setupDirectionStylePref();
		setupDirectionAudioFeedbackPref();
		setupDirectionHapticFeedbackPref();

		setupCopyProfileSettingsPref();
		setupResetToDefaultPref();

		updateAccessibilityOptions();
	}

	@Override
	public void onResume() {
		super.onResume();
		Preference accessibilityPrefs = findPreference(ACCESSIBILITY_OPTIONS);
		if (useSystemAccessibility() && accessibilityPrefs.isVisible() == app.systemAccessibilityEnabled()) {
			updateAllSettings();
		}
		AccessibilityManager accessibilityManager = (AccessibilityManager) app.getSystemService(Context.ACCESSIBILITY_SERVICE);
		accessibilityManager.addAccessibilityStateChangeListener(accessibilityListener);
	}

	@Override
	public void onPause() {
		super.onPause();
		AccessibilityManager accessibilityManager = (AccessibilityManager) app.getSystemService(Context.ACCESSIBILITY_SERVICE);
		accessibilityManager.removeAccessibilityStateChangeListener(accessibilityListener);
	}

	private void setupAccessibilityPermissionPref() {
		Preference accessibilityPrefs = findPreference(ACCESSIBILITY_OPTIONS);
		if (!useSystemAccessibility() || app.systemAccessibilityEnabled()) {
			accessibilityPrefs.setVisible(false);
		} else {
			Intent accessibilitySettings = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
			accessibilityPrefs.setVisible(true);
			if (accessibilitySettings.resolveActivity(app.getPackageManager()) != null) {
				accessibilityPrefs.setIntent(accessibilitySettings);
			} else {
				accessibilityPrefs.setSummary(null);
			}
		}
	}

	private boolean useSystemAccessibility() {
		return AccessibilityMode.DEFAULT == settings.ACCESSIBILITY_MODE.getModeValue(getSelectedAppMode());
	}

	private void setupAccessibilityModePref() {
		AccessibilityMode[] accessibilityModes = AccessibilityMode.values();
		String[] entries = new String[accessibilityModes.length];
		Integer[] entryValues = new Integer[accessibilityModes.length];

		for (int i = 0; i < entries.length; i++) {
			entries[i] = accessibilityModes[i].toHumanString(app);
			entryValues[i] = accessibilityModes[i].ordinal();
		}

		ListPreferenceEx accessibilityMode = findPreference(settings.ACCESSIBILITY_MODE.getId());
		accessibilityMode.setEntries(entries);
		accessibilityMode.setEntryValues(entryValues);
		accessibilityMode.setIcon(getPersistentPrefIcon(R.drawable.ic_action_android));
		accessibilityMode.setDescription(R.string.accessibility_mode_descr);
	}

	private void setupSpeechRatePref() {
		Float[] entryValues = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f};
		String[] entries = new String[entryValues.length];

		for (int i = 0; i < entries.length; i++) {
			entries[i] = (int) (entryValues[i] * 100) + " %";
		}

		ListPreferenceEx speechRate = findPreference(settings.SPEECH_RATE.getId());
		speechRate.setEntries(entries);
		speechRate.setEntryValues(entryValues);
		speechRate.setIcon(getContentIcon(R.drawable.ic_world_globe_dark));
		speechRate.setDescription(R.string.speech_rate_descr);
	}

	private void setupSmartAutoAnnouncePref() {
		SwitchPreferenceEx smartAutoAnnounce = findPreference(settings.ACCESSIBILITY_SMART_AUTOANNOUNCE.getId());
		smartAutoAnnounce.setDescription(getString(R.string.access_smart_autoannounce_descr));
	}

	private void setupAutoAnnouncePeriodPref() {
		int[] seconds = {5, 10, 15, 20, 30, 45, 60, 90};
		int[] minutes = {2, 3, 5};

		Integer[] entryValues = new Integer[seconds.length + minutes.length];
		String[] entries = new String[entryValues.length];
		int k = 0;
		for (int second : seconds) {
			entryValues[k] = second * 1000;
			entries[k] = second + " " + getString(R.string.int_seconds);
			k++;
		}
		for (int minute : minutes) {
			entryValues[k] = (minute * 60) * 1000;
			entries[k] = minute + " " + getString(R.string.int_min);
			k++;
		}

		ListPreferenceEx autoAnnouncePeriod = findPreference(settings.ACCESSIBILITY_AUTOANNOUNCE_PERIOD.getId());
		autoAnnouncePeriod.setEntries(entries);
		autoAnnouncePeriod.setEntryValues(entryValues);
		autoAnnouncePeriod.setDescription(R.string.access_autoannounce_period_descr);
	}

	private void setupDirectionStylePref() {
		RelativeDirectionStyle[] relativeDirectionStyles = RelativeDirectionStyle.values();
		String[] entries = new String[relativeDirectionStyles.length];
		Integer[] entryValues = new Integer[relativeDirectionStyles.length];

		for (int i = 0; i < entries.length; i++) {
			entries[i] = relativeDirectionStyles[i].toHumanString(app);
			entryValues[i] = relativeDirectionStyles[i].ordinal();
		}

		ListPreferenceEx directionStyle = findPreference(settings.DIRECTION_STYLE.getId());
		directionStyle.setEntries(entries);
		directionStyle.setEntryValues(entryValues);
		directionStyle.setDescription(R.string.settings_direction_style_descr);
	}

	private void setupDirectionAudioFeedbackPref() {
		SwitchPreferenceEx directionAudioFeedback = findPreference(settings.DIRECTION_AUDIO_FEEDBACK.getId());
		directionAudioFeedback.setDescription(getString(R.string.access_direction_audio_feedback_descr));
	}

	private void setupDirectionHapticFeedbackPref() {
		SwitchPreferenceEx directionHapticFeedback = findPreference(settings.DIRECTION_HAPTIC_FEEDBACK.getId());
		directionHapticFeedback.setDescription(getString(R.string.access_direction_haptic_feedback_descr));
	}

	private void setupCopyProfileSettingsPref() {
		Preference copyProfilePrefs = findPreference(COPY_PLUGIN_SETTINGS);
		copyProfilePrefs.setIcon(getActiveIcon(R.drawable.ic_action_copy));
	}

	private void setupResetToDefaultPref() {
		Preference resetToDefault = findPreference(RESET_TO_DEFAULT);
		resetToDefault.setIcon(getActiveIcon(R.drawable.ic_action_reset_to_default_dark));
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		String prefId = preference.getKey();
		if (ACCESSIBILITY_OPTIONS.equals(prefId)) {
			setupPrefRoundedBg(holder);
		} else if (settings.ACCESSIBILITY_MODE.getId().equals(prefId)) {
			ImageView imageView = (ImageView) holder.findViewById(android.R.id.icon);
			if (imageView != null) {
				boolean enabled = preference.isEnabled() && app.accessibilityEnabledForMode(getSelectedAppMode());
				imageView.setEnabled(enabled);
			}
		}
	}

	@Override
	public void onPreferenceChanged(@NonNull String prefId) {
		if (settings.ACCESSIBILITY_MODE.getId().equals(prefId)) {
			updateAllSettings();
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (COPY_PLUGIN_SETTINGS.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				SelectCopyAppModeBottomSheet.showInstance(fragmentManager, this, getSelectedAppMode());
			}
		} else if (RESET_TO_DEFAULT.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				ResetProfilePrefsBottomSheet.showInstance(fragmentManager, getSelectedAppMode(), this);
			}
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public void copyAppModePrefs(@NonNull ApplicationMode appMode) {
		OsmandMonitoringPlugin plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
		if (plugin != null) {
			app.getSettings().copyProfilePreferences(appMode, getSelectedAppMode(), plugin.getPreferences());
			updateAllSettings();
		}
	}

	@Override
	public void resetAppModePrefs(ApplicationMode appMode) {
		OsmandMonitoringPlugin plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
		if (plugin != null) {
			app.getSettings().resetProfilePreferences(appMode, plugin.getPreferences());
			app.showToastMessage(R.string.plugin_prefs_reset_successful);
			updateAllSettings();
		}
	}

	private void updateAccessibilityOptions() {
		boolean accessibilityEnabled = app.accessibilityEnabledForMode(getSelectedAppMode());
		PreferenceScreen screen = getPreferenceScreen();
		OsmandMonitoringPlugin plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
		if (screen != null && plugin != null) {
			for (int i = 0; i < screen.getPreferenceCount(); i++) {
				Preference preference = screen.getPreference(i);
				String prefId = preference.getKey();
				if (!settings.ACCESSIBILITY_MODE.getId().equals(prefId)
						&& !settings.SPEECH_RATE.getId().equals(prefId)
						&& !RESET_TO_DEFAULT.equals(prefId)
						&& !COPY_PLUGIN_SETTINGS.equals(prefId)
						&& !ACCESSIBILITY_OPTIONS.equals(prefId))
					preference.setEnabled(accessibilityEnabled);
			}
		}
	}
}