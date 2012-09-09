package net.osmand.plus.voice;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.LogUtil;
import net.osmand.plus.OsmandSettings;

import org.apache.commons.logging.Log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
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
	private final Context mCtx;
	private AudioFocusHelper mAudioFocusHelper;

	
	public MediaCommandPlayerImpl(Context ctx, OsmandSettings settings, String voiceProvider)
		throws CommandPlayerException
	{
		super(ctx, settings, voiceProvider, CONFIG_FILE, MEDIA_VOICE_VERSION);
		mCtx = ctx;
		this.streamType = settings.AUDIO_STREAM_GUIDANCE.get();  
	}
	
	@Override
	public void updateAudioStream(int streamType) {
		this.streamType = streamType;
	}
	
	@Override
	public void clear() {
		super.clear();
		mediaPlayer = null;
	}
	
	@Override
	public synchronized void playCommands(CommandBuilder builder) {
		filesToPlay.addAll(builder.execute());
		
		// If we have not already started to play audio, start.
		if (mediaPlayer == null) {
			if (android.os.Build.VERSION.SDK_INT >= 8) {
			    mAudioFocusHelper = new AudioFocusHelper(mCtx);
			} else {
			    mAudioFocusHelper = null;
			}
			if (mAudioFocusHelper != null)
				mAudioFocusHelper.requestFocus();
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

				if (mAudioFocusHelper != null)
					mAudioFocusHelper.abandonFocus();
			}
		}
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
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
			if (sleep != 0)
				Thread.sleep(sleep);
		} catch (InterruptedException e) {
		}
	}

	private File getNextFileToPlay() {
		while (!filesToPlay.isEmpty()) {
			String f = filesToPlay.remove(0);
			if (f != null && voiceDir != null) {
				File file = new File(voiceDir, f);
				if (file.exists()) 
					return file;
				else
					log.error("Unable to play, does not exist: "+file);
			}
		}
		return null;
	}
	
	private void playFile(File file)  {
		log.debug("Playing file : " + file); //$NON-NLS-1$
		try {
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
	
	/**
	 * This helper class allows API level 8 calls to be isolated from the rest of the app.
	 * This class is only be instantiated on OS versions which support it. 
	 * @author genly
	 *
	 */
	// We Use API level 8 calls here, suppress warnings.
	@SuppressLint("NewApi")
    public class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener {
		private Context mContext;
		private AudioManager mAudioManager;

		public AudioFocusHelper(Context context) {
			mContext = context;
			mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		}
		
		public boolean requestFocus() {
			return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
					mAudioManager.requestAudioFocus(this, streamType, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
		}

		public boolean abandonFocus() {
			return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.abandonAudioFocus(this);
		}
	    @Override
	    public void onAudioFocusChange(int focusChange) {
	    		log.error("MediaCommandPlayerImpl.onAudioFocusChange(): Unexpected audio focus change: "+focusChange);
	    }
	}
}
