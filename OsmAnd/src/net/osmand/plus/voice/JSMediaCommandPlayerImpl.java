package net.osmand.plus.voice;

import android.media.AudioAttributes;
import android.media.MediaPlayer;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.AudioFocusHelperImpl;
import net.osmand.plus.routing.VoiceRouter;
import net.osmand.plus.settings.backend.ApplicationMode;

import org.apache.commons.logging.Log;
import org.mozilla.javascript.ScriptableObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.media.MediaPlayer.OnCompletionListener;

/**
 * That class represents command player.
 * It gets commands from input, analyze what files should be played and play
 * them using media player
 */
public class JSMediaCommandPlayerImpl extends BaseCommandPlayer implements OnCompletionListener {

	private static final Log log = PlatformUtil.getLog(JSMediaCommandPlayerImpl.class);

	private final ScriptableObject jsScope;
	private final VoiceRouter voiceRouter;
	private MediaPlayer mediaPlayer;
	// indicates that player is ready to play first file
	private final List<String> filesToPlay = Collections.synchronizedList(new ArrayList<>());

	public JSMediaCommandPlayerImpl(OsmandApplication app, ApplicationMode applicationMode,
			VoiceRouter voiceRouter, String voiceProvider) throws CommandPlayerException {
		super(app, applicationMode, voiceProvider);
		this.voiceRouter = voiceRouter;

		org.mozilla.javascript.Context context = org.mozilla.javascript.Context.enter();
		context.setOptimizationLevel(-1);
		jsScope = context.initSafeStandardObjects();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(
					app.getAppPath(IndexConstants.VOICE_INDEX_DIR).getAbsolutePath() +
							"/" + voiceProvider + "/" + language + "_tts.js")));
			context.evaluateReader(jsScope, br, "JS", 1, null);
			br.close();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} finally {
			org.mozilla.javascript.Context.exit();
		}
	}

	@Override
	public JsCommandBuilder newCommandBuilder() {
		JsCommandBuilder commandBuilder = new JsCommandBuilder(this);
		commandBuilder.setJSContext(jsScope);
		commandBuilder.setParameters(app.getSettings().METRIC_SYSTEM.get().toTTSString(), false);
		return commandBuilder;
	}

	@Override
	public void stop() {
		if (filesToPlay != null) {
			filesToPlay.clear();
		}
		if (mediaPlayer != null) {
			mediaPlayer.stop();
		}
		abandonAudioFocus();
	}

	@Override
	public void clear() {
		super.clear();
		if (filesToPlay != null) {
			filesToPlay.clear();
		}
		if (mediaPlayer != null) {
			mediaPlayer.release();
		}
		mediaPlayer = null;
	}

	//  Called from the calculating route thread.
	@Override
	public synchronized List<String> playCommands(JsCommandBuilder builder) {
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
				int vpd = app.getSettings().VOICE_PROMPT_DELAY[app.getSettings().AUDIO_MANAGER_STREAM.getModeValue(getApplicationMode())].get();
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
		while (!filesToPlay.isEmpty() && filesToPlay.get(0).startsWith(DELAY_CONST)) {
			String s = filesToPlay.remove(0).substring(DELAY_CONST.length());
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
	private void playFile(File file) {
		if (!file.exists() || file.isDirectory()) {
			log.error("Unable to play, does not exist: " + file);
			playQueue();
			return;
		}
		try {
			log.debug("Playing file : " + file);
			mediaPlayer.reset();
			mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
					.setUsage(app.getSettings().AUDIO_USAGE.get())
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

	public static boolean isMyData(File voiceDir) {
		if (voiceDir.getName().contains("tts")) {
			return false;
		}
		return new File(voiceDir, voiceDir.getName() + "_" + IndexConstants.TTSVOICE_INDEX_EXT_JS).exists();
	}
}