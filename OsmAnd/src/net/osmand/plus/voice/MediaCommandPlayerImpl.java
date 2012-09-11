package net.osmand.plus.voice;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.LogUtil;
import net.osmand.plus.OsmandSettings;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.media.MediaPlayer;


/**
 * That class represents command player. 
 * It gets commands from input, analyze what files should be played and play 
 * them using media player 
 */
public class MediaCommandPlayerImpl extends AbstractPrologCommandPlayer implements MediaPlayer.OnCompletionListener {
	
	private static final String CONFIG_FILE = "_config.p";
	private static final int[] MEDIA_VOICE_VERSION = new int[] { 0 }; // MUST BE SORTED, list of supported versions

	private static final Log log = LogUtil.getLog(MediaCommandPlayerImpl.class);
	
	// playing media
	private MediaPlayer mediaPlayer;
	// indicates that player is ready to play first file
	private List<String> filesToPlay = Collections.synchronizedList(new ArrayList<String>());
	private int streamType;

	
	public MediaCommandPlayerImpl(Context ctx, OsmandSettings settings, String voiceProvider)
		throws CommandPlayerException
	{
		super(ctx, settings, voiceProvider, CONFIG_FILE, MEDIA_VOICE_VERSION);
	}
	
	@Override
	public void clear() {
		super.clear();
		mediaPlayer = null;
	}
	
	//  Called from the calculating route thread.
	@Override
	public synchronized void playCommands(CommandBuilder builder) {
		filesToPlay.addAll(builder.execute());
		
		// If we have not already started to play audio, start.
		if (mediaPlayer == null) {
			requestAudioFocus();
			playQueue();
		}
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
	
	/**
	 * Called when the MediaPlayer is done.  The call back is on the main thread.
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {
		// Work on the next file to play.
		playQueue();
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
				log.debug("Delaying "+sleep);
				Thread.sleep(sleep);
			}
		} catch (InterruptedException e) {
		}
	}

	private File getNextFileToPlay() {
		while (!filesToPlay.isEmpty()) {
			String f = filesToPlay.remove(0);
			if (f != null && voiceDir != null) {
				File file = new File(voiceDir, f);
				return file;
			}
		}
		return null;
	}
	
	/**
	 * Starts the MediaPlayer playing a file.  This method will return immediately.
	 * OnCompletionListener() will be called when the MediaPlayer is done.
	 * @param file
	 */
	private void playFile(File file)  {
		if (!file.exists()) {
			log.error("Unable to play, does not exist: "+file);
			playQueue();
			return;
		}
		try {
			log.debug("Playing file : " + file); //$NON-NLS-1$
			mediaPlayer.reset();
			mediaPlayer.setAudioStreamType(streamType);
			mediaPlayer.setDataSource(file.getAbsolutePath());
			mediaPlayer.prepare();
			mediaPlayer.setOnCompletionListener(this);
			mediaPlayer.start();
		} catch (Exception e) {
			log.error("Error while playing voice command", e); //$NON-NLS-1$
			playQueue();
		}
	}
	
	public static boolean isMyData(File voiceDir) {
		return new File(voiceDir, CONFIG_FILE).exists();
	}
	
}
