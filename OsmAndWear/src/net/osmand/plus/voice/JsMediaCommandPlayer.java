package net.osmand.plus.voice;

import static android.media.MediaPlayer.OnCompletionListener;
import static net.osmand.IndexConstants.TTSVOICE_INDEX_EXT_JS;

import android.media.AudioAttributes;
import android.media.MediaPlayer;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.AudioFocusHelperImpl;
import net.osmand.plus.routing.VoiceRouter;
import net.osmand.plus.settings.backend.ApplicationMode;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * That class represents command player.
 * It gets commands from input, analyze what files should be played and play
 * them using media player
 */
public class JsMediaCommandPlayer extends CommandPlayer implements OnCompletionListener {

	private static final Log log = PlatformUtil.getLog(JsMediaCommandPlayer.class);

	private static final String DELAY_PREFIX = "delay_";

	private MediaPlayer mediaPlayer;
	// indicates that player is ready to play first file
	private final List<String> filesToPlay = Collections.synchronizedList(new ArrayList<>());

	protected JsMediaCommandPlayer(@NonNull OsmandApplication app,
	                               @NonNull ApplicationMode applicationMode,
	                               @NonNull VoiceRouter voiceRouter,
	                               @NonNull File voiceProviderDir) throws CommandPlayerException {
		super(app, applicationMode, voiceRouter, voiceProviderDir);
	}

	@NonNull
	@Override
	public File getTtsFileFromDir(@NonNull File voiceProviderDir) {
		String fileName = language + "_" + TTSVOICE_INDEX_EXT_JS;
		return new File(voiceProviderDir, fileName);
	}

	@NonNull
	@Override
	public CommandBuilder newCommandBuilder() {
		JsCommandBuilder commandBuilder = new JsCommandBuilder(this);
		commandBuilder.setJSContext(jsScope);
		commandBuilder.setParameters(settings.METRIC_SYSTEM.get().toTTSString(), false);
		return commandBuilder;
	}

	@Override
	public void stop() {
		filesToPlay.clear();
		if (mediaPlayer != null) {
			mediaPlayer.stop();
		}
		abandonAudioFocus();
	}

	@Override
	public void clear() {
		super.clear();
		filesToPlay.clear();
		if (mediaPlayer != null) {
			mediaPlayer.release();
		}
		mediaPlayer = null;
	}

	//  Called from the calculating route thread.
	@NonNull
	@Override
	public synchronized List<String> playCommands(@NonNull CommandBuilder builder) {
		if (voiceRouter.isMute()) {
			return Collections.emptyList();
		}
		List<String> lst = splitAnnouncements(builder.execute());
		filesToPlay.addAll(lst);

		// If we have not already started to play audio, start.
		if (mediaPlayer == null) {
			requestAudioFocus();
			// Delay first prompt of each batch to allow BT SCO link being established, or when VOICE_PROMPT_DELAY is set >0 for the other stream types
			if (app != null) {
				int vpd = settings.VOICE_PROMPT_DELAY[settings.AUDIO_MANAGER_STREAM.getModeValue(app.getRoutingHelper().getAppMode())].get();
				if (vpd > 0) {
					try {
						Thread.sleep(vpd);
					} catch (InterruptedException e) {
					}
				}
			}
		}
		if (AudioFocusHelperImpl.playbackAuthorized) {
			playQueue();
			return lst;
		} else {
			return Collections.emptyList();
		}
	}

	@NonNull
	private List<String> splitAnnouncements(List<String> execute) {
		List<String> result = new ArrayList<>();
		for (String files : execute) {
			result.addAll(Arrays.asList(files.split(" ")));
		}
		return result;
	}

	/**
	 * Called when the MediaPlayer is done.  The call back is on the main thread.
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {
		// Work on the next file to play.
		playQueue();
	}

	private synchronized void playQueue() {
		if (mediaPlayer == null) {
			mediaPlayer = new MediaPlayer();
		}

		performDelays();
		File file = getNextFileToPlay();
		if (file != null) {
			playFile(file);
			// Will continue with onCompletion()
		} else {
			// Release the media player only when we are done speaking.  
			if (mediaPlayer != null) {
				mediaPlayer.release();
				mediaPlayer = null;
				abandonAudioFocus();
			}
		}
	}

	private void performDelays() {
		int sleep = 0;
		while (!filesToPlay.isEmpty() && filesToPlay.get(0).startsWith(DELAY_PREFIX)) {
			String s = filesToPlay.remove(0).substring(DELAY_PREFIX.length());
			try {
				sleep += Integer.parseInt(s);
			} catch (NumberFormatException e) {
			}
		}
		try {
			if (sleep != 0) {
				log.debug("Delaying " + sleep);
				Thread.sleep(sleep);
			}
		} catch (InterruptedException e) {
		}
	}

	private File getNextFileToPlay() {
		while (!filesToPlay.isEmpty()) {
			String f = filesToPlay.remove(0);
			if (f != null && voiceProviderDir != null) {
				File file = new File(voiceProviderDir, f);
				return file;
			}
		}
		return null;
	}

	/**
	 * Starts the MediaPlayer playing a file.  This method will return immediately.
	 * OnCompletionListener() will be called when the MediaPlayer is done.
	 *
	 * @param file
	 */
	private void playFile(@NonNull File file) {
		if (!file.exists() || file.isDirectory()) {
			log.error("Unable to play, does not exist: " + file);
			playQueue();
			return;
		}
		try {
			log.debug("Playing file : " + file);
			mediaPlayer.reset();
			mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
					.setUsage(settings.AUDIO_USAGE[settings.AUDIO_MANAGER_STREAM.getModeValue(app.getRoutingHelper().getAppMode())].get())
					.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
					.build());

			mediaPlayer.setDataSource(file.getAbsolutePath());
			mediaPlayer.prepare();
			mediaPlayer.setOnCompletionListener(this);
			mediaPlayer.start();
		} catch (Exception e) {
			log.error("Error while playing voice command", e);
			playQueue();
		}
	}

	@Override
	public boolean supportsStructuredStreetNames() {
		return false;
	}

	public static boolean isMyData(@NonNull File voiceDir) {
		if (voiceDir.getName().contains("tts")) {
			return false;
		}
		return new File(voiceDir, voiceDir.getName() + "_" + TTSVOICE_INDEX_EXT_JS).exists();
	}
}
