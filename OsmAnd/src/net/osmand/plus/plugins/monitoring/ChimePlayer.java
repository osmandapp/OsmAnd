package net.osmand.plus.plugins.monitoring;

import static android.media.MediaPlayer.OnCompletionListener;

import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.AudioFocusHelper;
import net.osmand.plus.api.AudioFocusHelperImpl;
import net.osmand.plus.settings.backend.OsmandSettings;

import org.apache.commons.logging.Log;

/**
 * This class is a simple chime player that will sound a chime
 * at an interval set in the settings.
 */
public class ChimePlayer implements OnCompletionListener {

    private static final Log log = PlatformUtil.getLog(ChimePlayer.class);
    private final OsmandApplication app;
    private final OsmandSettings settings;

    private MediaPlayer mediaPlayer;
    private AudioFocusHelper focusHelper;
    private float lastInterval;
    private float nextChime;

    protected ChimePlayer(@NonNull OsmandApplication app) {
        this.app = app;
        this.settings = app.getSettings();
        this.lastInterval = settings.CHIME_INTERVAL.get();
        this.nextChime = lastInterval;
        this.mediaPlayer = null;
    }

    /**
     * Starts the MediaPlayer playing a chime.  This method will return immediately.
     * OnCompletionListener() will be called when the MediaPlayer is done.
     */
    public void playChime(float distance) {
        float interval = settings.CHIME_INTERVAL.get();
        if (interval == 0) {
            return;
        }

        if (interval != lastInterval) {
            lastInterval = interval;
            nextChime = interval;
            advanceNextChime(distance);
        }

        if (distance < nextChime) {
            return;
        }

        advanceNextChime(distance);

        // If we have not already started to play audio, start.
        if (mediaPlayer == null) {
            requestAudioFocus();
            mediaPlayer = new MediaPlayer();
        }
        try {
            if (AudioFocusHelperImpl.playbackAuthorized) {
                log.debug("Playing chime");
                mediaPlayer.reset();
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());

                // Ding.wav by LanooskiProductions -- https://freesound.org/s/394795/ -- License: Creative Commons 0
                AssetFileDescriptor assetFileDescriptor = app.getAssets().openFd("sounds/recording_ding.ogg");
                mediaPlayer.setDataSource(assetFileDescriptor);
                mediaPlayer.prepare();
                mediaPlayer.setOnCompletionListener(this);
                mediaPlayer.start();
                assetFileDescriptor.close();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = null;
            abandonAudioFocus();
        }
    }

    private void advanceNextChime (float distance) {
        while (nextChime <= distance) {
            nextChime = nextChime + lastInterval;
        }
    }

    /**
     * Called when the MediaPlayer is done.  The call back is on the main thread.
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = null;
        abandonAudioFocus();
    }

    protected synchronized void requestAudioFocus() {
        log.debug("requestAudioFocus");
        focusHelper = createAudioFocusHelper();
        if (focusHelper != null && app != null) {
            focusHelper.requestAudFocus(app);
        }
    }

    protected synchronized void abandonAudioFocus() {
        log.debug("abandonAudioFocus");
        if (app != null && focusHelper != null) {
            focusHelper.abandonAudFocus(app);
        }
        focusHelper = null;
    }

    @Nullable
    private AudioFocusHelper createAudioFocusHelper() {
        try {
            return new AudioFocusHelperImpl();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
