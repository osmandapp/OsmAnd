package net.osmand.plus.settings;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.views.ListFloatPreference;
import net.osmand.plus.views.ListIntPreference;
import net.osmand.plus.voice.CommandPlayer;

import java.util.Set;

import static net.osmand.plus.activities.SettingsNavigationActivity.MORE_VALUE;

public class SettingsVoiceAnnouncesFragment extends SettingsBaseProfileDependentFragment {

	public static final String TAG = "SettingsVoiceAnnouncesFragment";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		return view;
	}

	@Override
	protected int getPreferenceResId() {
		return R.xml.voice_announces;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar;
	}

	protected String getToolbarTitle() {
		return getString(R.string.voice_announces);
	}

	protected void createUI() {
		PreferenceScreen screen = getPreferenceScreen();

		SwitchPreference SPEAK_ROUTING_ALARMS = (SwitchPreference) screen.findPreference(settings.SPEAK_ROUTING_ALARMS.getId());

		Preference voiceAnnouncesInfo = screen.findPreference("voice_announces_info");
		voiceAnnouncesInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		SwitchPreference SPEAK_STREET_NAMES = (SwitchPreference) screen.findPreference(settings.SPEAK_STREET_NAMES.getId());
		SwitchPreference SPEAK_TRAFFIC_WARNINGS = (SwitchPreference) screen.findPreference(settings.SPEAK_TRAFFIC_WARNINGS.getId());
		SwitchPreference SPEAK_PEDESTRIAN = (SwitchPreference) screen.findPreference(settings.SPEAK_PEDESTRIAN.getId());
		SwitchPreference SPEAK_SPEED_LIMIT = (SwitchPreference) screen.findPreference(settings.SPEAK_SPEED_LIMIT.getId());
		SwitchPreference SPEAK_SPEED_CAMERA = (SwitchPreference) screen.findPreference(settings.SPEAK_SPEED_CAMERA.getId());
		SwitchPreference SPEAK_TUNNELS = (SwitchPreference) screen.findPreference(settings.SPEAK_TUNNELS.getId());
		SwitchPreference ANNOUNCE_WPT = (SwitchPreference) screen.findPreference(settings.ANNOUNCE_WPT.getId());
		SwitchPreference ANNOUNCE_NEARBY_FAVORITES = (SwitchPreference) screen.findPreference(settings.ANNOUNCE_NEARBY_FAVORITES.getId());
		SwitchPreference ANNOUNCE_NEARBY_POI = (SwitchPreference) screen.findPreference(settings.ANNOUNCE_NEARBY_POI.getId());

		String[] speedNames;
		float[] speedLimits;
		if (settings.METRIC_SYSTEM.get() == OsmandSettings.MetricsConstants.KILOMETERS_AND_METERS) {
			speedLimits = new float[]{-10f, -7f, -5f, 0f, 5f, 7f, 10f, 15f, 20f};
			speedNames = new String[speedLimits.length];
			for (int i = 0; i < speedLimits.length; i++) {
				speedNames[i] = (int) speedLimits[i] + " " + getString(R.string.km_h);
			}
		} else {
			speedLimits = new float[]{-7f, -5f, -3f, 0f, 3f, 5f, 7f, 10f, 15f};
			speedNames = new String[speedLimits.length];
			for (int i = 0; i < speedNames.length; i++) {
				speedNames[i] = (int) speedLimits[i] + " " + getString(R.string.mile_per_hour);
			}
		}
		ListFloatPreference SPEED_LIMIT_EXCEED = (ListFloatPreference) screen.findPreference(settings.SPEED_LIMIT_EXCEED.getId());
		SPEED_LIMIT_EXCEED.setEntries(speedNames);
		SPEED_LIMIT_EXCEED.setEntryValues(speedLimits);

		//keep informing option:
		int[] keepInformingValues = new int[]{0, 1, 2, 3, 5, 7, 10, 15, 20, 25, 30};
		String[] keepInformingNames = new String[keepInformingValues.length];
		keepInformingNames[0] = getString(R.string.keep_informing_never);
		for (int i = 1; i < keepInformingValues.length; i++) {
			keepInformingNames[i] = keepInformingValues[i] + " " + getString(R.string.int_min);
		}
		ListIntPreference KEEP_INFORMING = (ListIntPreference) screen.findPreference(settings.KEEP_INFORMING.getId());
		KEEP_INFORMING.setEntries(keepInformingNames);
		KEEP_INFORMING.setEntryValues(keepInformingValues);

		float[] arrivalValues = new float[]{1.5f, 1f, 0.5f, 0.25f};
		String[] arrivalNames = new String[]{
				getString(R.string.arrival_distance_factor_early),
				getString(R.string.arrival_distance_factor_normally),
				getString(R.string.arrival_distance_factor_late),
				getString(R.string.arrival_distance_factor_at_last)
		};

		ListFloatPreference ARRIVAL_DISTANCE_FACTOR = (ListFloatPreference) screen.findPreference(settings.ARRIVAL_DISTANCE_FACTOR.getId());
		ARRIVAL_DISTANCE_FACTOR.setEntries(arrivalNames);
		ARRIVAL_DISTANCE_FACTOR.setEntryValues(arrivalValues);

		reloadVoiceListPreference(screen);
		addVoicePrefs(screen);
	}

	private void reloadVoiceListPreference(PreferenceScreen screen) {
		String[] entries;
		String[] entrieValues;
		Set<String> voiceFiles = getMyApplication().getRoutingOptionsHelper().getVoiceFiles(getActivity());
		entries = new String[voiceFiles.size() + 2];
		entrieValues = new String[voiceFiles.size() + 2];
		int k = 0;
		// entries[k++] = getString(R.string.shared_string_none);
		entrieValues[k] = OsmandSettings.VOICE_PROVIDER_NOT_USE;
		entries[k++] = getString(R.string.shared_string_do_not_use);
		for (String s : voiceFiles) {
			entries[k] = (s.contains("tts") ? getString(R.string.ttsvoice) + " " : "") +
					FileNameTranslationHelper.getVoiceName(getMyActivity(), s);
			entrieValues[k] = s;
			k++;
		}
		entrieValues[k] = MORE_VALUE;
		entries[k] = getString(R.string.install_more);
		ListPreference voiceProvider = (ListPreference) screen.findPreference(settings.VOICE_PROVIDER.getId());
		voiceProvider.setEntries(entries);
		voiceProvider.setEntryValues(entrieValues);
		voiceProvider.setIcon(getContentIcon(R.drawable.ic_action_volume_mute));
	}

	private void addVoicePrefs(PreferenceScreen screen) {
		if (!Version.isBlackberry(getMyApplication())) {
			String[] streamTypes = new String[]{getString(R.string.voice_stream_music),
					getString(R.string.voice_stream_notification), getString(R.string.voice_stream_voice_call)};
			//getString(R.string.shared_string_default)};
			Integer[] streamIntTypes = new Integer[]{AudioManager.STREAM_MUSIC,
					AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_VOICE_CALL};
			String[] streamIntTypesStr = new String[]{String.valueOf(AudioManager.STREAM_MUSIC),
					String.valueOf(AudioManager.STREAM_NOTIFICATION), String.valueOf(AudioManager.STREAM_VOICE_CALL)};
			//AudioManager.USE_DEFAULT_STREAM_TYPE};
			ListIntPreference lp = new ListIntPreference(getContext());
			lp.setTitle(R.string.choose_audio_stream);
			lp.setKey(settings.AUDIO_STREAM_GUIDANCE.getId());
			lp.setDialogTitle(R.string.choose_audio_stream);
			lp.setSummary(R.string.choose_audio_stream_descr);
			lp.setEntries(streamTypes);
			lp.setEntryValues(streamIntTypesStr);
//			final Preference.OnPreferenceChangeListener prev = lp.getOnPreferenceChangeListener();
			lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
//					prev.onPreferenceChange(preference, newValue);
					CommandPlayer player = getMyApplication().getPlayer();
					if (player != null) {
						player.updateAudioStream(settings.AUDIO_STREAM_GUIDANCE.get());
					}
					// Sync corresponding AUDIO_USAGE value
					ApplicationMode mode = getMyApplication().getSettings().getApplicationMode();
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
			});
			screen.addPreference(lp);
			screen.addPreference(createSwitchPreference(settings.INTERRUPT_MUSIC, R.string.interrupt_music,
					R.string.interrupt_music_descr));
		}
	}

	public static boolean showInstance(FragmentManager fragmentManager) {
		try {
			SettingsVoiceAnnouncesFragment settingsNavigationFragment = new SettingsVoiceAnnouncesFragment();
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, settingsNavigationFragment, SettingsVoiceAnnouncesFragment.TAG)
					.addToBackStack(SettingsVoiceAnnouncesFragment.TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}