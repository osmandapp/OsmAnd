package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.SpeedCamerasBottomSheet;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.helpers.enums.MetricsConstants;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.bottomsheets.AnnouncementTimeBottomSheet;
import net.osmand.plus.settings.preferences.ListPreferenceEx;

import java.util.Set;

import static net.osmand.plus.UiUtilities.CompoundButtonType.TOOLBAR;
import static net.osmand.plus.routing.data.AnnounceTimeDistances.ARRIVAL_VALUES;
import static net.osmand.plus.settings.backend.OsmandSettings.VOICE_PROVIDER_NOT_USE;

public class VoiceAnnouncesFragment extends BaseSettingsFragment implements OnPreferenceChanged {

	public static final String TAG = VoiceAnnouncesFragment.class.getSimpleName();

	private static final String MORE_VALUE = "MORE_VALUE";

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
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

		SwitchCompat switchView = (SwitchCompat) switchContainer.findViewById(R.id.switchWidget);
		switchView.setChecked(checked);
		UiUtilities.setupCompoundButton(switchView, isNightMode(), TOOLBAR);

		TextView title = switchContainer.findViewById(R.id.switchButtonText);
		title.setText(checked ? R.string.shared_string_on : R.string.shared_string_off);
	}

	@Override
	protected void setupPreferences() {
		Preference voiceAnnouncesInfo = findPreference("voice_announces_info");
		voiceAnnouncesInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		setupSpeedLimitExceedPref();

		setupKeepInformingPref();
		setupArrivalAnnouncementPref();
		setupVoiceProviderPref();

		setupAudioStreamGuidancePref();
		setupInterruptMusicPref();

		enableDisablePreferences(!settings.VOICE_MUTE.getModeValue(getSelectedAppMode()));
		setupSpeakCamerasPref();
		setupSpeedCamerasAlert();
	}

	private void setupSpeedLimitExceedPref() {
		//array size must be equal!
		Float[] valuesKmh = new Float[] {-10f, -7f, -5f, 0f, 5f, 7f, 10f, 15f, 20f};
		Float[] valuesMph = new Float[] {-7f, -5f, -3f, 0f, 3f, 5f, 7f, 10f, 15f};
		String[] names;
		if (settings.METRIC_SYSTEM.getModeValue(getSelectedAppMode()) == MetricsConstants.KILOMETERS_AND_METERS) {
			names = new String[valuesKmh.length];
			for (int i = 0; i < names.length; i++) {
				names[i] = valuesKmh[i].intValue() + " " + getString(R.string.km_h);
			}
		} else {
			names = new String[valuesMph.length];
			for (int i = 0; i < names.length; i++) {
				names[i] = valuesMph[i].intValue() + " " + getString(R.string.mile_per_hour);
			}
		}
		ListPreferenceEx voiceProvider = (ListPreferenceEx) findPreference(settings.SPEED_LIMIT_EXCEED_KMH.getId());
		voiceProvider.setEntries(names);
		voiceProvider.setEntryValues(valuesKmh);
	}

	private void setupKeepInformingPref() {
		Integer[] keepInformingValues = new Integer[] {0, 1, 2, 3, 5, 7, 10, 15, 20, 25, 30};
		String[] keepInformingNames = new String[keepInformingValues.length];
		keepInformingNames[0] = getString(R.string.keep_informing_never);
		for (int i = 1; i < keepInformingValues.length; i++) {
			keepInformingNames[i] = keepInformingValues[i] + " " + getString(R.string.int_min);
		}

		ListPreferenceEx keepInforming = (ListPreferenceEx) findPreference(settings.KEEP_INFORMING.getId());
		keepInforming.setEntries(keepInformingNames);
		keepInforming.setEntryValues(keepInformingValues);
	}

	private void setupArrivalAnnouncementPref() {
		String[] arrivalNames = new String[] {
				getString(R.string.arrival_distance_factor_early),
				getString(R.string.arrival_distance_factor_normally),
				getString(R.string.arrival_distance_factor_late),
				getString(R.string.arrival_distance_factor_at_last)
		};

		ListPreferenceEx arrivalDistanceFactor = (ListPreferenceEx) findPreference(settings.ARRIVAL_DISTANCE_FACTOR.getId());
		arrivalDistanceFactor.setEntries(arrivalNames);
		arrivalDistanceFactor.setEntryValues(ARRIVAL_VALUES);
	}

	private void setupVoiceProviderPref() {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}
		Set<String> voiceFiles = app.getRoutingOptionsHelper().getVoiceFiles(activity);
		String[] entries = new String[voiceFiles.size() + 2];
		String[] entryValues = new String[voiceFiles.size() + 2];

		int k = 0;
		// entries[k++] = getString(R.string.shared_string_none);
		entryValues[k] = OsmandSettings.VOICE_PROVIDER_NOT_USE;
		entries[k++] = getString(R.string.shared_string_do_not_use);
		for (String s : voiceFiles) {
			entries[k] = (s.contains("tts") ? getString(R.string.ttsvoice) + " " : "") + FileNameTranslationHelper.getVoiceName(activity, s);
			entryValues[k] = s;
			k++;
		}
		entryValues[k] = MORE_VALUE;
		entries[k] = getString(R.string.install_more);

		Drawable disabled = getContentIcon(R.drawable.ic_action_volume_mute);
		Drawable enabled = getActiveIcon(R.drawable.ic_action_volume_up);
		Drawable icon = getPersistentPrefIcon(enabled, disabled);

		ListPreferenceEx voiceProvider = (ListPreferenceEx) findPreference(settings.VOICE_PROVIDER.getId());
		voiceProvider.setEntries(entries);
		voiceProvider.setEntryValues(entryValues);
		voiceProvider.setIcon(icon);
	}

	private void setupAudioStreamGuidancePref() {
		String[] streamTypes = new String[] {
				getString(R.string.voice_stream_music),
				getString(R.string.voice_stream_notification),
				getString(R.string.voice_stream_voice_call)
		};
		//getString(R.string.shared_string_default)};

		Integer[] streamIntTypes = new Integer[] {
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
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		if (settings.VOICE_PROVIDER.getId().equals(preference.getKey()) && preference instanceof ListPreferenceEx) {
			TextView titleView = (TextView) holder.findViewById(android.R.id.title);
			if (titleView != null) {
				titleView.setTextColor(preference.isEnabled() ? getActiveTextColor() : getDisabledTextColor());
			}
			ImageView imageView = (ImageView) holder.findViewById(android.R.id.icon);
			if (imageView != null) {
				Object currentValue = ((ListPreferenceEx) preference).getValue();
				imageView.setEnabled(preference.isEnabled() && !OsmandSettings.VOICE_PROVIDER_NOT_USE.equals(currentValue));
			}
		} else if (settings.SPEED_CAMERAS_UNINSTALLED.getId().equals(preference.getKey())) {
			setupPrefRoundedBg(holder);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();
		ApplicationMode selectedMode = getSelectedAppMode();

		if (prefId.equals(settings.VOICE_PROVIDER.getId())) {
			if (MORE_VALUE.equals(newValue)) {
				// listPref.set(oldValue); // revert the change..
				final Intent intent = new Intent(getContext(), DownloadActivity.class);
				intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
				intent.putExtra(DownloadActivity.FILTER_CAT, DownloadActivityType.VOICE_FILE.getTag());
				startActivity(intent);
				return false;
			} else if (newValue instanceof String) {
				onConfirmPreferenceChange(settings.VOICE_PROVIDER.getId(), newValue, ApplyQueryType.SNACK_BAR);
			}
			return true;
		}
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
		if (prefId.equals(settings.VOICE_PROVIDER.getId()) && newValue instanceof String) {
			if (VOICE_PROVIDER_NOT_USE.equals(newValue)) {
				applyPreference(settings.VOICE_MUTE.getId(), applyToAllProfiles, true);
				updateToolbar();
			}
			applyPreference(settings.VOICE_PROVIDER.getId(), applyToAllProfiles, newValue);
			app.initVoiceCommandPlayer(getActivity(), getSelectedAppMode(),
					false, null, true, false, applyToAllProfiles);
		} else if (prefId.equals(settings.AUDIO_MANAGER_STREAM.getId())) {
			// Sync DEFAULT value with CAR value, as we have other way to set it for now

			if (getSelectedAppMode().equals(ApplicationMode.CAR) && newValue instanceof Integer) {
				settings.AUDIO_MANAGER_STREAM.setModeValue(ApplicationMode.DEFAULT, (Integer) newValue);
			} else {
				settings.AUDIO_MANAGER_STREAM.setModeValue(ApplicationMode.DEFAULT, settings.AUDIO_MANAGER_STREAM.getModeValue(ApplicationMode.CAR));
			}
			settings.AUDIO_USAGE.setModeValue(ApplicationMode.DEFAULT, settings.AUDIO_USAGE.getModeValue(ApplicationMode.CAR));
		} else {
			super.onApplyPreferenceChange(prefId, applyToAllProfiles, newValue);
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (settings.SPEED_CAMERAS_UNINSTALLED.getId().equals(prefId)) {
			SpeedCamerasBottomSheet.showInstance(requireActivity().getSupportFragmentManager(), this);
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
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	@Override
	public void onPreferenceChanged(String prefId) {
		if (prefId.equals(settings.SPEED_CAMERAS_UNINSTALLED.getId())) {
			setupSpeakCamerasPref();
			setupSpeedCamerasAlert();
		}
	}

	private void setupSpeakCamerasPref() {
		SwitchPreferenceCompat showCameras = (SwitchPreferenceCompat) findPreference(settings.SPEAK_SPEED_CAMERA.getId());
		showCameras.setVisible(!settings.SPEED_CAMERAS_UNINSTALLED.get());
	}
}