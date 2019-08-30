package net.osmand.plus.settings;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import net.osmand.StateChangedListener;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.SettingsNavigationActivity;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.voice.CommandPlayer;

import java.util.Set;

import static net.osmand.plus.activities.SettingsNavigationActivity.MORE_VALUE;

public class VoiceAnnouncesFragment extends BaseSettingsFragment {

	public static final String TAG = "VoiceAnnouncesFragment";

	@Override
	protected int getPreferencesResId() {
		return R.xml.voice_announces;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar;
	}

	protected String getToolbarTitle() {
		return getString(R.string.voice_announces);
	}

	protected void setupPreferences() {
		SwitchPreference speakRoutingAlarms = (SwitchPreference) findPreference(settings.SPEAK_ROUTING_ALARMS.getId());
		speakRoutingAlarms.setSummaryOn(R.string.shared_string_on);
		speakRoutingAlarms.setSummaryOff(R.string.shared_string_off);

		Preference voiceAnnouncesInfo = findPreference("voice_announces_info");
		voiceAnnouncesInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		SwitchPreference speakStreetNames = (SwitchPreference) findPreference(settings.SPEAK_STREET_NAMES.getId());
		SwitchPreference speakTrafficWarnings = (SwitchPreference) findPreference(settings.SPEAK_TRAFFIC_WARNINGS.getId());
		SwitchPreference speakPedestrian = (SwitchPreference) findPreference(settings.SPEAK_PEDESTRIAN.getId());
		SwitchPreference speakSpeedLimit = (SwitchPreference) findPreference(settings.SPEAK_SPEED_LIMIT.getId());

		setupSpeedLimitExceedPref();
		setupSpeakSpeedCameraPref();

		SwitchPreference speakTunnels = (SwitchPreference) findPreference(settings.SPEAK_TUNNELS.getId());
		SwitchPreference announceWpt = (SwitchPreference) findPreference(settings.ANNOUNCE_WPT.getId());
		SwitchPreference announceNearbyFavorites = (SwitchPreference) findPreference(settings.ANNOUNCE_NEARBY_FAVORITES.getId());
		SwitchPreference announceNearbyPoi = (SwitchPreference) findPreference(settings.ANNOUNCE_NEARBY_POI.getId());

		setupKeepInformingPref();
		setupArrivalAnnouncementPref();
		setupVoiceProviderPref();

		if (!Version.isBlackberry(app)) {
			setupAudioStreamGuidancePref();
			setupInterruptMusicPref();
		}
	}

	private void setupSpeedLimitExceedPref() {
		Float[] speedLimitValues;
		String[] speedLimitNames;

		if (settings.METRIC_SYSTEM.get() == OsmandSettings.MetricsConstants.KILOMETERS_AND_METERS) {
			speedLimitValues = new Float[] {-10f, -7f, -5f, 0f, 5f, 7f, 10f, 15f, 20f};
			speedLimitNames = new String[speedLimitValues.length];

			for (int i = 0; i < speedLimitValues.length; i++) {
				speedLimitNames[i] = speedLimitValues[i].intValue() + " " + getString(R.string.km_h);
			}
		} else {
			speedLimitValues = new Float[] {-7f, -5f, -3f, 0f, 3f, 5f, 7f, 10f, 15f};
			speedLimitNames = new String[speedLimitValues.length];

			for (int i = 0; i < speedLimitNames.length; i++) {
				speedLimitNames[i] = speedLimitValues[i].intValue() + " " + getString(R.string.mile_per_hour);
			}
		}

		ListPreferenceEx voiceProvider = (ListPreferenceEx) findPreference(settings.SPEED_LIMIT_EXCEED.getId());
		voiceProvider.setEntries(speedLimitNames);
		voiceProvider.setEntryValues(speedLimitValues);
	}

	private void setupSpeakSpeedCameraPref() {
		settings.SPEAK_SPEED_CAMERA.addListener(new StateChangedListener<Boolean>() {
			@Override
			public void stateChanged(Boolean change) {
				SwitchPreference speakSpeedCamera = (SwitchPreference) findPreference(settings.SPEAK_SPEED_CAMERA.getId());
				if (speakSpeedCamera != null) {
					speakSpeedCamera.setChecked(change);
				}
			}
		});
	}

	private void setupKeepInformingPref() {
		Integer[] keepInformingValues = new Integer[] {0, 1, 2, 3, 5, 7, 10, 15, 20, 25, 30};
		String[] keepInformingNames = new String[keepInformingValues.length];
		keepInformingNames[0] = getString(R.string.keep_informing_never);
		for (int i = 1; i < keepInformingValues.length; i++) {
			keepInformingNames[i] = keepInformingValues[i] + " " + getString(R.string.int_min);
		}

		ListPreferenceEx voiceProvider = (ListPreferenceEx) findPreference(settings.KEEP_INFORMING.getId());
		voiceProvider.setEntries(keepInformingNames);
		voiceProvider.setEntryValues(keepInformingValues);
	}

	private void setupArrivalAnnouncementPref() {
		Float[] arrivalValues = new Float[] {1.5f, 1f, 0.5f, 0.25f};
		String[] arrivalNames = new String[] {
				getString(R.string.arrival_distance_factor_early),
				getString(R.string.arrival_distance_factor_normally),
				getString(R.string.arrival_distance_factor_late),
				getString(R.string.arrival_distance_factor_at_last)
		};

		ListPreferenceEx voiceProvider = (ListPreferenceEx) findPreference(settings.ARRIVAL_DISTANCE_FACTOR.getId());
		voiceProvider.setEntries(arrivalNames);
		voiceProvider.setEntryValues(arrivalValues);
	}

	private void setupVoiceProviderPref() {
		Set<String> voiceFiles = app.getRoutingOptionsHelper().getVoiceFiles(getActivity());
		String[] entries = new String[voiceFiles.size() + 2];
		String[] entryValues = new String[voiceFiles.size() + 2];

		int k = 0;
		// entries[k++] = getString(R.string.shared_string_none);
		entryValues[k] = OsmandSettings.VOICE_PROVIDER_NOT_USE;
		entries[k++] = getString(R.string.shared_string_do_not_use);
		for (String s : voiceFiles) {
			entries[k] = (s.contains("tts") ? getString(R.string.ttsvoice) + " " : "") + FileNameTranslationHelper.getVoiceName(getActivity(), s);
			entryValues[k] = s;
			k++;
		}
		entryValues[k] = MORE_VALUE;
		entries[k] = getString(R.string.install_more);

		ListPreferenceEx voiceProvider = (ListPreferenceEx) findPreference(settings.VOICE_PROVIDER.getId());
		voiceProvider.setEntries(entries);
		voiceProvider.setEntryValues(entryValues);
		voiceProvider.setIcon(getContentIcon(R.drawable.ic_action_volume_up));
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

		ListPreferenceEx lp = createListPreferenceEx(settings.AUDIO_STREAM_GUIDANCE.getId(), streamTypes, streamIntTypes, R.string.choose_audio_stream, R.layout.preference_with_descr);
		getPreferenceScreen().addPreference(lp);
	}

	private void setupInterruptMusicPref() {
		Preference interruptMusicPref = createSwitchPreference(settings.INTERRUPT_MUSIC, R.string.interrupt_music, R.string.interrupt_music_descr, R.layout.preference_switch);
		getPreferenceScreen().addPreference(interruptMusicPref);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();

		if (prefId.equals(settings.ANNOUNCE_NEARBY_POI.getId())) {
			settings.SHOW_NEARBY_POI.set(settings.ANNOUNCE_NEARBY_POI.get());
		}
		if (prefId.equals(settings.ANNOUNCE_NEARBY_FAVORITES.getId())) {
			settings.SHOW_NEARBY_FAVORITES.set(settings.ANNOUNCE_NEARBY_FAVORITES.get());
		}
		if (prefId.equals(settings.ANNOUNCE_WPT.getId())) {
			settings.SHOW_WPT.set(settings.ANNOUNCE_WPT.get());
		}
		if (prefId.equals(settings.SPEAK_SPEED_CAMERA.getId())) {
			if (!settings.SPEAK_SPEED_CAMERA.get()) {
				SettingsNavigationActivity.confirmSpeedCamerasDlg(getActivity(), settings);
				return false;
			} else {
				return true;
			}
		}
		if (prefId.equals(settings.AUDIO_STREAM_GUIDANCE.getId())) {
			CommandPlayer player = app.getPlayer();
			if (player != null) {
				player.updateAudioStream(settings.AUDIO_STREAM_GUIDANCE.get());
			}
			// Sync corresponding AUDIO_USAGE value
			ApplicationMode mode = getSelectedAppMode();
			int stream = settings.AUDIO_STREAM_GUIDANCE.getModeValue(mode);
			if (stream == AudioManager.STREAM_MUSIC) {
				settings.AUDIO_USAGE.setModeValue(mode, AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE);
			} else if (stream == AudioManager.STREAM_NOTIFICATION) {
				settings.AUDIO_USAGE.setModeValue(mode, AudioAttributes.USAGE_NOTIFICATION);
			} else if (stream == AudioManager.STREAM_VOICE_CALL) {
				settings.AUDIO_USAGE.setModeValue(mode, AudioAttributes.USAGE_VOICE_COMMUNICATION);
			}

			// Sync DEFAULT value with CAR value, as we have other way to set it for now
			settings.AUDIO_STREAM_GUIDANCE.setModeValue(ApplicationMode.DEFAULT, settings.AUDIO_STREAM_GUIDANCE.getModeValue(ApplicationMode.CAR));
			settings.AUDIO_USAGE.setModeValue(ApplicationMode.DEFAULT, settings.AUDIO_USAGE.getModeValue(ApplicationMode.CAR));
			return true;
		}

		return super.onPreferenceChange(preference, newValue);
	}
}