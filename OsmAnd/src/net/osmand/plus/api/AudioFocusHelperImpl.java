package net.osmand.plus.api;

import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.os.Build;

import net.osmand.PlatformUtil;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.OsmandApplication;
//import net.osmand.plus.voice.CommandPlayer;
//import net.osmand.plus.routing.VoiceRouter;
import org.apache.commons.logging.Log;

import android.content.Context;
import android.media.AudioManager;

// Hardy, 2021-09-12, audio focus overhaul:
// - Use AudioAttributes, AudioFocusRequest for SDK_INT >= 26
// - Play only after immediate focus granted. (Delayed playback probably makes no sense.)
// - Stop playing on audio focus LOSS.
// - Treat LOSS_TRANSIENT like LOSS, delayed playback probably makes no sense.
// - Treat LOSS_TRANSIENT_CAN_DUCK like LOSS, ducked speech probably hard to understand.

public class AudioFocusHelperImpl implements AudioManager.OnAudioFocusChangeListener, AudioFocusHelper {
	public static boolean playbackAuthorized = false;
	//private static CommandPlayer player = null;
	private static final Log log = PlatformUtil.getLog(AudioFocusHelperImpl.class);

	@Override
	public boolean requestAudFocus(Context context, ApplicationMode applicationMode, int streamType) {
		AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		//player = ((OsmandApplication) context.getApplication()).getRoutingHelper().getVoiceRouter().getPlayer();
		if (android.os.Build.VERSION.SDK_INT < 26) {
			playbackAuthorized = AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.requestAudioFocus(this, streamType,
					((OsmandApplication) context.getApplicationContext()).getSettings().INTERRUPT_MUSIC.getModeValue(applicationMode)
					? AudioManager.AUDIOFOCUS_GAIN_TRANSIENT : AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
		} else {
			AudioAttributes mAudioAttributes = new AudioAttributes.Builder()
					.setUsage(((OsmandApplication) context.getApplicationContext()).getSettings().AUDIO_USAGE.get())
					.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
					.build();
			AudioFocusRequest mAudioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
					.setAudioAttributes(mAudioAttributes)
					.setAcceptsDelayedFocusGain(false)
					.setOnAudioFocusChangeListener(this)
					.build();
			final Object focusLock = new Object();
			int res = mAudioManager.requestAudioFocus(mAudioFocusRequest);
			synchronized(focusLock) {
				if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
					playbackAuthorized = true;
				} else if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
					playbackAuthorized = false;
				} else if (res == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
					playbackAuthorized = false;
				}
			}
		}
		return playbackAuthorized;
	}

	@Override
	public boolean abandonAudFocus(Context context, ApplicationMode applicationMode, int streamType) {
		AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		//player = ((OsmandApplication) context.getApplication()).getRoutingHelper().getVoiceRouter().getPlayer();
		playbackAuthorized = false;
		if (android.os.Build.VERSION.SDK_INT < 26) {
			return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.abandonAudioFocus(this);
		} else {
			AudioAttributes mAudioAttributes = new AudioAttributes.Builder()
					.setUsage(((OsmandApplication) context.getApplicationContext()).getSettings().AUDIO_USAGE.get())
					.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
					.build();
			AudioFocusRequest mAudioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
					.setAudioAttributes(mAudioAttributes)
					.setAcceptsDelayedFocusGain(false)
					.setOnAudioFocusChangeListener(this)
					.build();
			return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
		}
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		log.error("AudioFocusHelperImpl.onAudioFocusChange(): Unexpected audio focus change: " + focusChange);
		if (focusChange == AudioManager.AUDIOFOCUS_GAIN
				|| focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
				|| focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) {
			playbackAuthorized = true;
		} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS
				|| focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
				|| focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
			//System will not automatically duck apps with AudioAttributes.CONTENT_TYPE_SPEECH and instead notify AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK to e.g. enable pausing here: 
			playbackAuthorized = false;
			//if (player != null) {
			//	player.stop();
			//}
			//abandonAudioFocus() is in stop()
		}
	}
}
