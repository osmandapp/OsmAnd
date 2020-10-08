package net.osmand.plus.voice;


import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;

import net.osmand.PlatformUtil;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.plus.routing.VoiceRouter;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * That class represents command player. 
 * It gets commands from input, analyze what files should be played and play 
 * them using media player 
 */
public class MediaCommandPlayerImpl extends AbstractPrologCommandPlayer implements MediaPlayer.OnCompletionListener {
	
	private static final String CONFIG_FILE = "_config.p";
	private static final int[] MEDIA_VOICE_VERSION = new int[] { 0 }; // MUST BE SORTED, list of supported versions

	private static final Log log = PlatformUtil.getLog(MediaCommandPlayerImpl.class);
	
	// playing media
	MediaPlayer mediaPlayer;
	// indicates that player is ready to play first file
	List<String> filesToPlay = Collections.synchronizedList(new ArrayList<String>());
	VoiceRouter vrt;

	
	public MediaCommandPlayerImpl(OsmandApplication ctx, ApplicationMode applicationMode, VoiceRouter vrt, String voiceProvider)
		throws CommandPlayerException
	{
		super(ctx, applicationMode, voiceProvider, CONFIG_FILE, MEDIA_VOICE_VERSION);
		this.vrt = vrt;
	}
	
	@Override
	public void clear() {
		super.clear();
		if (filesToPlay != null){
			filesToPlay.clear();
		}
		if (mediaPlayer != null){
			mediaPlayer.release();
		}
		mediaPlayer = null;
	}

	@Override
	public void stop(){
		if (filesToPlay != null){
			filesToPlay.clear();
		}
		if (mediaPlayer != null){
			mediaPlayer.stop();
		}
		abandonAudioFocus();
	}

	//  Called from the calculating route thread.
	@Override
	public synchronized List<String> playCommands(CommandBuilder builder) {
		if(vrt.isMute()) {
			StringBuilder bld = new StringBuilder();
			for (String s : builder.execute()) {
				bld.append(s).append(' ');
			}
			if (ctx != null) {
				// sendAlertToAndroidWear(ctx, bld.toString());
			}
			return Collections.emptyList();
		}
		List<String> lst = builder.execute();

		filesToPlay.addAll(lst);
		
		// If we have not already started to play audio, start.
		if (mediaPlayer == null) {
			requestAudioFocus();
			// Delay first prompt of each batch to allow BT SCO link being established, or when VOICE_PROMPT_DELAY is set >0 for the other stream types
			if (ctx != null) {
				Integer stream = ctx.getSettings().AUDIO_MANAGER_STREAM.getModeValue(getApplicationMode());
				OsmandPreference<Integer> pref = ctx.getSettings().VOICE_PROMPT_DELAY[stream];
				if (pref.getModeValue(getApplicationMode()) > 0) {
					try {
						Thread.sleep(pref.getModeValue(getApplicationMode()));
					} catch (InterruptedException e) {
					}
				}
			}
		}
		playQueue();
		return lst;
	}
	
	synchronized void playQueue() {
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
		if (!file.exists() || file.isDirectory()) {
			log.error("Unable to play, does not exist: "+file);
			playQueue();
			return;
		}
		try {
			log.debug("Playing file : " + file); //$NON-NLS-1$
			mediaPlayer.reset();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
						.setUsage(ctx.getSettings().AUDIO_USAGE.get())
						.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
						.build());

			} else {
				// Deprecated in API Level 26, use above AudioAtrributes instead
				mediaPlayer.setAudioStreamType(streamType);
			}
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

	@Override
	public boolean supportsStructuredStreetNames() {
		return false;
	}
	
}
