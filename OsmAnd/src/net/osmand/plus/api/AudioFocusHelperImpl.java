package net.osmand.plus.api;

import net.osmand.PlatformUtil;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import org.apache.commons.logging.Log;

import android.content.Context;
import android.media.AudioManager;

/**
 * This helper class allows API level 8 calls to be isolated from the rest of the app. This class is only be instantiated on OS versions
 * which support it.
 *
 * @author genly
 */
public class AudioFocusHelperImpl implements AudioManager.OnAudioFocusChangeListener, AudioFocusHelper
{
    private static final Log log = PlatformUtil.getLog(AudioFocusHelperImpl.class);

    @Override
    public boolean requestFocus(Context context, ApplicationMode applicationMode, int streamType) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.requestAudioFocus(this, streamType, 
              ((OsmandApplication) context.getApplicationContext()).getSettings().INTERRUPT_MUSIC.getModeValue(applicationMode)?
              AudioManager.AUDIOFOCUS_GAIN_TRANSIENT: 
              AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK );
    }

    @Override
    public boolean abandonFocus(Context context, ApplicationMode applicationMode, int streamType)
    {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.abandonAudioFocus(this);
    }

    @Override
    public void onAudioFocusChange(int focusChange)
    {
	// Basically we ignore audio focus changes. There's not much we can do when we have interrupted audio
	// for our speech, and we in turn get interrupted. Ignore it until a scenario comes up which gives us
	// reason to change this strategy.
	log.error("MediaCommandPlayerImpl.onAudioFocusChange(): Unexpected audio focus change: " + focusChange);

	// Hardy, 2017-05-25: (See https://developer.android.com/guide/topics/media-apps/volume-and-earphones.html)
//	if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
		// Usually: Permanent loss of audio focus, stop playback here
//	} else if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT) {
		// Usually: Pause playback
//	} else if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
		// Usually: Lower the volume, keep playing
//	} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
		// Usually: App has been granted audio focus again, raise volume to normal
//	}
    }
}
