package net.osmand.plus.voice;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.LogUtil;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.media.MediaPlayer;

/**
 * That class represents command player. 
 * It gets commands from input, analyze what files should be played and play 
 * them using media player 
 */
public class MediaCommandPlayerImpl extends AbstractPrologCommandPlayer {
	
	private static final String CONFIG_FILE = "_config.p";
	private static final int[] MEDIA_VOICE_VERSION = new int[] { 0 }; // MUST BE SORTED, list of supported versions

	private static final Log log = LogUtil.getLog(MediaCommandPlayerImpl.class);
	
	// playing media
	private MediaPlayer mediaPlayer;
	// indicates that player is ready to play first file
	private boolean playNext = true;
	private List<String> filesToPlay = Collections.synchronizedList(new ArrayList<String>());

	
	public MediaCommandPlayerImpl(Context ctx, String voiceProvider)
		throws CommandPlayerException
	{
		super(ctx, voiceProvider, CONFIG_FILE, MEDIA_VOICE_VERSION);
		mediaPlayer = new MediaPlayer();
	}
	
	@Override
	public void clear() {
		super.clear();
		mediaPlayer = null;
	}
	
	@Override
	public String[] getLibraries() {
		return new String[] { "alice.tuprolog.lib.BasicLibrary"};
	}
	
	@Override
	public void playCommands(CommandBuilder builder){
		filesToPlay.addAll(builder.execute());
		playQueue();
	}
	
	private synchronized void playQueue() {
		while (!filesToPlay.isEmpty() && playNext) {
			String f = filesToPlay.remove(0);
			if (f != null && voiceDir != null) {
				boolean exists = false;
//				if(voiceZipFile != null){
//					ZipEntry entry = voiceZipFile.getEntry(f);
//					exists = entry != null;
//					voiceZipFile.getInputStream(entry);
//					
//				} else {
					File file = new File(voiceDir, f);
					exists = file.exists();
//				}
				if (exists) {
					log.debug("Playing file : " + f); //$NON-NLS-1$
					playNext = false;
					try {
						// Can't play sound file from zip it seams to be impossible only unpack and play!!!
						mediaPlayer.setDataSource(file.getAbsolutePath());
						mediaPlayer.prepare();
						mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
							public void onCompletion(MediaPlayer mp) {
								mp.release();
								mediaPlayer = new MediaPlayer();
								int sleep = 60;
								boolean delay = true;
								while (!filesToPlay.isEmpty() && delay) {
									delay = filesToPlay.get(0).startsWith(DELAY_CONST);
									if (delay) {
										String s = filesToPlay.remove(0).substring(DELAY_CONST.length());
										try {
											sleep += Integer.parseInt(s);
										} catch (NumberFormatException e) {
										}
									}
								}
								try {
									Thread.sleep(sleep);
								} catch (InterruptedException e) {
								}
								playNext = true;
								playQueue();
							}
						});
						
						mediaPlayer.start();
					} catch (Exception e) {
						log.error("Error while playing voice command", e); //$NON-NLS-1$
						playNext = true;
						
					}
				} else {
					log.info("Play file not found : " + f); //$NON-NLS-1$
				}
			} 
		}
	}
	
	public static boolean isMyData(File voiceDir) {
		return new File(voiceDir, CONFIG_FILE).exists();
	}
}
