package net.osmand.plus.media;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioEncoder;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;

import org.apache.commons.logging.Log;

import java.io.File;

public class AudioRecorder {

	private static final Log log = PlatformUtil.getLog(AudioRecorder.class);

	public static final int AUDIO_FORMAT_DEFAULT = MediaRecorder.AudioEncoder.AAC; // AAC
	public static final int AUDIO_BITRATE_DEFAULT = 64 * 1024; // 64 kbps
	public static final int AUDIO_SAMPLE_RATE_DEFAULT = 48 * 1000; // 48 kHz

	private final OsmandApplication app;
	private final OsmandSettings settings;

	public final CommonPreference<Integer> AV_AUDIO_FORMAT;
	public final CommonPreference<Integer> AV_AUDIO_BITRATE;
	public final CommonPreference<Integer> AV_AUDIO_SAMPLE_RATE;

	public AudioRecorder(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();

		AV_AUDIO_FORMAT = settings.registerIntPreference("av_audio_format", AUDIO_FORMAT_DEFAULT);
		AV_AUDIO_BITRATE = settings.registerIntPreference("av_audio_bitrate", AUDIO_BITRATE_DEFAULT);
		AV_AUDIO_SAMPLE_RATE = settings.registerIntPreference("av_audio_sample_rate", AUDIO_SAMPLE_RATE_DEFAULT);
	}

	@NonNull
	public MediaRecorder createRecorder(@NonNull File file) {
		int audioEncoder = AV_AUDIO_FORMAT.get();
		MediaRecorder recorder = new MediaRecorder();
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		recorder.setAudioEncoder(audioEncoder);
		recorder.setAudioEncodingBitRate(AV_AUDIO_BITRATE.get());
		recorder.setOutputFile(file.getAbsolutePath());
		if (audioEncoder == AudioEncoder.AAC) {
			recorder.setAudioSamplingRate(AV_AUDIO_SAMPLE_RATE.get());
		}
		return recorder;
	}

	public void muteStreamMusicAndOutputGuidance() {
		adjustStreamMusicAndOutputGuidance(AudioManager.ADJUST_MUTE);
	}

	public void unmuteStreamMusicAndOutputGuidance() {
		adjustStreamMusicAndOutputGuidance(AudioManager.ADJUST_UNMUTE);
	}

	private void adjustStreamMusicAndOutputGuidance(int direction) {
		AudioManager manager = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
		int voiceGuidanceOutput = settings.AUDIO_MANAGER_STREAM.get();
		manager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0);
		if (voiceGuidanceOutput != AudioManager.STREAM_MUSIC) {
			manager.adjustStreamVolume(voiceGuidanceOutput, direction, 0);
		}
	}
}