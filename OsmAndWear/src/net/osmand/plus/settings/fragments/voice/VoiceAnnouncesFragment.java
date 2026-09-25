package net.osmand.plus.settings.fragments.voice;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;

import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.SpeedCamerasBottomSheet;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.AnnouncementTimeBottomSheet;
import net.osmand.plus.settings.bottomsheets.SpeedLimitBottomSheet;
import net.osmand.shared.settings.enums.SpeedConstants;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;

public class VoiceAnnouncesFragment extends BaseSettingsFragment {

	public static final String TAG = VoiceAnnouncesFragment.class.getSimpleName();

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);

		view.findViewById(R.id.toolbar_switch_container).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ApplicationMode selectedMode = getSelectedAppMode();
				boolean checked = !settings.VOICE_MUTE.getModeValue(selectedMode);
				onConfirmPreferenceChange(
						settings.VOICE_MUTE.getId(), checked, ApplyQueryType.SNACK_BAR);
				updateToolbarSwitch();
				enableDisablePreferences(!checked);
				updateMenu();
			}
		});
	}

	@Override
	protected void updateToolbar() {
		super.updateToolbar();
		View view = getView();
		ImageView profileIcon = view.findViewById(R.id.profile_icon);
		profileIcon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_help_online, ColorUtilities.getDefaultIconColorId(isNightMode())));
		profileIcon.setOnClickListener(v -> {
			if (getContext() != null) {
				AndroidUtils.openUrl(getContext(), R.string.docs_troubleshooting_voice_navigation, isNightMode());
			}
		});
		updateToolbarSwitch();
	}

	private void updateToolbarSwitch() {
		View view = getView();
		if (view == null) {
			return;
		}
		boolean checked = !settings.VOICE_MUTE.getModeValue(getSelectedAppMode());

		int color = checked ? getActiveProfileColor() : ContextCompat.getColor(app, R.color.preference_top_switch_off);
		View switchContainer = view.findViewById(R.id.toolbar_switch_container);
		AndroidUtils.setBackground(switchContainer, new ColorDrawable(color));

		SwitchCompat switchView = switchContainer.findViewById(R.id.switchWidget);
		switchView.setChecked(checked);
		UiUtilities.setupCompoundButton(switchView, isNightMode(), TOOLBAR);

		TextView title = switchContainer.findViewById(R.id.switchButtonText);
		title.setText(checked ? R.string.shared_string_on : R.string.shared_string_off);
	}

	@Override
	protected void setupPreferences() {
		Preference voiceAnnouncesInfo = findPreference("voice_announces_info");
		voiceAnnouncesInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));
		setupVoiceProviderPref();

		setupKeepInformingPref();
		setupArrivalAnnouncementPref();

		setupAudioStreamGuidancePref();
		setupInterruptMusicPref();

		enableDisablePreferences(!settings.VOICE_MUTE.getModeValue(getSelectedAppMode()));
		setupSpeedTolerance();
		setupSpeakCamerasPref();
		setupSpeedCamerasAlert();
		setupSpeedLimitExceedPref();
	}

	private void setupSpeedTolerance() {
		Preference preference = findPreference(settings.SPEED_LIMIT_EXCEED_KMH.getId());
		float selectedValue = settings.SPEED_LIMIT_EXCEED_KMH.getModeValue(getSelectedAppMode());
		ApplicationMode mode = getSelectedAppMode();
		SpeedConstants speedFormat = OsmAndFormatter.getSpeedModeForPaceMode(app.getSettings().SPEED_SYSTEM.getModeValue(mode));
		String value = OsmAndFormatter.getFormattedSpeed(selectedValue / 3.6f, app, mode.isSpeedToleranceBigRange(), speedFormat);
		preference.setSummary(value);
		preference.setVisible(settings.SPEAK_SPEED_LIMIT.getModeValue(getSelectedAppMode()));
	}

	private void setupVoiceProviderPref() {
		Preference languageSetting = findPreference(settings.VOICE_PROVIDER.getId());
		languageSetting.setIcon(getContentIcon(R.drawable.ic_action_map_language));
		String voiceIndexBasename = settings.VOICE_PROVIDER.getModeValue(getSelectedAppMode());
		String summary = voiceIndexBasename == null || voiceIndexBasename.equals(OsmandSettings.VOICE_PROVIDER_NOT_USE)
				? getString(R.string.shared_string_not_selected)
				: FileNameTranslationHelper.getVoiceName(getContext(), voiceIndexBasename);
		languageSetting.setSummary(summary);
	}

	private void setupSpeedLimitExceedPref() {
		Preference preference = findPreference(settings.SPEED_LIMIT_EXCEED_KMH.getId());
		preference.setEnabled(settings.SPEAK_SPEED_LIMIT.getModeValue(getSelectedAppMode()));
	}

	private void setupKeepInformingPref() {
		Integer[] keepInformingValues = {0, 1, 2, 3, 5, 7, 10, 15, 20, 25, 30};
		String[] keepInformingNames = new String[keepInformingValues.length];
		keepInformingNames[0] = getString(R.string.keep_informing_never);
		for (int i = 1; i < keepInformingValues.length; i++) {
			keepInformingNames[i] = keepInformingValues[i] + " " + getString(R.string.int_min);
		}

		ListPreferenceEx keepInforming = findPreference(settings.KEEP_INFORMING.getId());
		keepInforming.setEntries(keepInformingNames);
		keepInforming.setEntryValues(keepInformingValues);
	}

	private void setupArrivalAnnouncementPref() {
		Float[] arrivalValues = {1.5f, 1f, 0.5f, 0.25f};
		String[] arrivalNames = {
				getString(R.string.arrival_distance_factor_early),
				getString(R.string.arrival_distance_factor_normally),
				getString(R.string.arrival_distance_factor_late),
				getString(R.string.arrival_distance_factor_at_last)
		};

		ListPreferenceEx arrivalDistanceFactor = findPreference(settings.ARRIVAL_DISTANCE_FACTOR.getId());
		arrivalDistanceFactor.setEntries(arrivalNames);
		arrivalDistanceFactor.setEntryValues(arrivalValues);
	}

	private void setupAudioStreamGuidancePref() {
		String[] streamTypes = {
				getString(R.string.voice_stream_music),
				getString(R.string.voice_stream_notification),
				getString(R.string.voice_stream_voice_call)
		};
		//getString(R.string.shared_string_default)};

		Integer[] streamIntTypes = {
				AudioManager.STREAM_MUSIC,
				AudioManager.STREAM_NOTIFICATION,
				AudioManager.STREAM_VOICE_CALL
		};
		//AudioManager.USE_DEFAULT_STREAM_TYPE};

		ListPreferenceEx audioStreamGuidance = createListPreferenceEx(settings.AUDIO_MANAGER_STREAM.getId(), streamTypes, streamIntTypes, R.string.choose_audio_stream, R.layout.preference_with_descr);
		getPreferenceScreen().addPreference(audioStreamGuidance);
	}

	private void setupInterruptMusicPref() {
		Preference interruptMusicPref = createSwitchPreference(settings.INTERRUPT_MUSIC, R.string.interrupt_music, R.string.interrupt_music_descr, R.layout.preference_switch_with_descr);
		getPreferenceScreen().addPreference(interruptMusicPref);
	}

	private void updateMenu() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapRouteInfoMenu().updateMenu();
		}
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		if (settings.SPEED_CAMERAS_UNINSTALLED.getId().equals(preference.getKey())) {
			setupPrefRoundedBg(holder);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();
		ApplicationMode selectedMode = getSelectedAppMode();
		if (prefId.equals(settings.SPEAK_SPEED_CAMERA.getId())) {
			if (!settings.SPEAK_SPEED_CAMERA.getModeValue(selectedMode)) {
				return onConfirmPreferenceChange(
						settings.SPEAK_SPEED_CAMERA.getId(), true, ApplyQueryType.SNACK_BAR);
			} else {
				return onConfirmPreferenceChange(
						settings.SPEAK_SPEED_CAMERA.getId(), false, ApplyQueryType.SNACK_BAR);
			}
		}
		if (prefId.equals(settings.AUDIO_MANAGER_STREAM.getId())) {
			return onConfirmPreferenceChange(
					settings.AUDIO_MANAGER_STREAM.getId(), newValue, ApplyQueryType.SNACK_BAR);
		}

		return super.onPreferenceChange(preference, newValue);
	}

	@Override
	public void onApplyPreferenceChange(String prefId, boolean applyToAllProfiles, Object newValue) {
		super.onApplyPreferenceChange(prefId, applyToAllProfiles, newValue);

		if (prefId.equals(settings.AUDIO_MANAGER_STREAM.getId())) {
			// Sync DEFAULT value with CAR value, as we have other way to set it for now
			if (getSelectedAppMode().equals(ApplicationMode.CAR) && newValue instanceof Integer) {
				settings.AUDIO_MANAGER_STREAM.setModeValue(ApplicationMode.DEFAULT, (Integer) newValue);
			} else {
				settings.AUDIO_MANAGER_STREAM.setModeValue(ApplicationMode.DEFAULT, settings.AUDIO_MANAGER_STREAM.getModeValue(ApplicationMode.CAR));
			}
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (settings.SPEED_CAMERAS_UNINSTALLED.getId().equals(prefId)) {
			SpeedCamerasBottomSheet.showInstance(requireFragmentManager(), this);
		} else if (settings.SPEED_LIMIT_EXCEED_KMH.getId().equals(prefId)) {
			ApplicationMode mode = getSelectedAppMode();
			SpeedLimitBottomSheet.showInstance(requireFragmentManager(), this, prefId, mode);
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		String prefId = preference.getKey();
		if (settings.ARRIVAL_DISTANCE_FACTOR.getId().equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				AnnouncementTimeBottomSheet.showInstance(fragmentManager, preference.getKey(), this, getSelectedAppMode(), false);
			}
		} else if (settings.VOICE_PROVIDER.getId().equals(prefId)) {
			VoiceLanguageBottomSheetFragment.showInstance(requireActivity().getSupportFragmentManager(), this, getSelectedAppMode(), false);
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	@Override
	public void onPreferenceChanged(@NonNull String prefId) {
		if (prefId.equals(settings.SPEED_CAMERAS_UNINSTALLED.getId())) {
			setupSpeakCamerasPref();
			setupSpeedCamerasAlert();
		} else if (prefId.equals(settings.VOICE_PROVIDER.getId())) {
			setupVoiceProviderPref();
		}
	}

	private void setupSpeakCamerasPref() {
		SwitchPreferenceCompat showCameras = findPreference(settings.SPEAK_SPEED_CAMERA.getId());
		showCameras.setVisible(!settings.SPEED_CAMERAS_UNINSTALLED.get());
	}

}
