package net.osmand.plus.voice;

import android.content.Context;
import android.media.AudioManager;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.api.AudioFocusHelper;
import net.osmand.plus.api.AudioFocusHelperImpl;
import net.osmand.plus.settings.backend.ApplicationMode;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.Collections;
import java.util.List;

import alice.tuprolog.Struct;
import androidx.annotation.NonNull;

public abstract class BaseCommandPlayer implements CommandPlayer {

	private static final Log log = PlatformUtil.getLog(BaseCommandPlayer.class);

	protected OsmandApplication app;
	protected File voiceProviderDir;
	protected static final String P_VERSION = "version";
	protected static final String P_RESOLVE = "resolve";

	public static final String A_LEFT = "left";
	public static final String A_LEFT_SH = "left_sh";
	public static final String A_LEFT_SL = "left_sl";
	public static final String A_LEFT_KEEP = "left_keep";
	public static final String A_RIGHT = "right";
	public static final String A_RIGHT_SH = "right_sh";
	public static final String A_RIGHT_SL = "right_sl";
	public static final String A_RIGHT_KEEP = "right_keep";

	protected static final String DELAY_CONST = "delay_";
	private static AudioFocusHelper mAudioFocusHelper;
	protected String language;
	protected int streamType;
	private ApplicationMode applicationMode;


	protected BaseCommandPlayer(OsmandApplication app, ApplicationMode applicationMode,
	                            String voiceProvider) throws CommandPlayerException {
		this.app = app;
		this.applicationMode = applicationMode;
		this.streamType = app.getSettings().AUDIO_MANAGER_STREAM.getModeValue(applicationMode);
		voiceProviderDir = getVoiceProviderDir(voiceProvider);
		this.language = voiceProvider.replace(IndexConstants.VOICE_PROVIDER_SUFFIX, "")
				.replace("-formal", "")
				.replace("-casual", "");
	}

	public ApplicationMode getApplicationMode() {
		return applicationMode;
	}

	public String getLanguage() {
		return language;
	}

	public String[] getLibraries(){
		return new String[] { "alice.tuprolog.lib.BasicLibrary",
					"alice.tuprolog.lib.ISOLibrary"/*, "alice.tuprolog.lib.IOLibrary"*/};
	}

	private File getVoiceProviderDir(String voiceProvider) throws CommandPlayerException {
		if (voiceProvider != null) {
			File voicesDir = app.getAppPath(IndexConstants.VOICE_INDEX_DIR);
			File voiceProviderDir = new File(voicesDir, voiceProvider);
			if (!voiceProviderDir.exists()) {
				throw new CommandPlayerException(app.getString(R.string.voice_data_unavailable));
			}
			return voiceProviderDir;
		}
		return null;
	}

	@Override
	public List<String> execute(List<Struct> listCmd) {
		return Collections.emptyList();
	}
	
	@Override
	public String getCurrentVoice() {
		if (voiceProviderDir == null) {
			return null;
		}
		return voiceProviderDir.getName();
	}

	@Override
	public CommandBuilder newCommandBuilder() {
		return new CommandBuilder(this);
	}

	@Override
	public void clear() {
		abandonAudioFocus();
		app = null;
	}

	@Override
	public void updateAudioStream(int streamType) {
		this.streamType = streamType;
	}

	protected synchronized void requestAudioFocus() {
		log.debug("requestAudioFocus");
		mAudioFocusHelper = getAudioFocus();
		if (mAudioFocusHelper != null && app != null) {
			boolean audioFocusGranted = mAudioFocusHelper.requestAudFocus(app, applicationMode, streamType);
			if (audioFocusGranted && app.getSettings().AUDIO_MANAGER_STREAM.getModeValue(applicationMode) == 0) {
				toggleBtSco(true);
			}
		}
	}

	private AudioFocusHelper getAudioFocus() {
		try {
			return new AudioFocusHelperImpl();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	protected synchronized void abandonAudioFocus() {
		log.debug("abandonAudioFocus");
		if ((app != null && app.getSettings().AUDIO_MANAGER_STREAM.getModeValue(applicationMode) == 0)
				|| btScoStatus) {
			toggleBtSco(false);
		}
		if (app != null && mAudioFocusHelper != null) {
			mAudioFocusHelper.abandonAudFocus(app, applicationMode, streamType);
		}
		mAudioFocusHelper = null;
	}

	public static boolean btScoStatus = false;

	// This only needed for init debugging in TestVoiceActivity:
	public static String btScoInit = "-";

	private synchronized boolean toggleBtSco(boolean on) {
	// Hardy, 2016-07-03: Establish a low quality BT SCO (Synchronous Connection-Oriented) link to interrupt e.g. a car stereo FM radio
		if (on) {
			try {
				AudioManager mAudioManager = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
				if (mAudioManager == null || !mAudioManager.isBluetoothScoAvailableOffCall()) {
					btScoInit = "Reported not available.";
					return false;
				}
				mAudioManager.setMode(0);
				mAudioManager.startBluetoothSco();
				mAudioManager.setBluetoothScoOn(true);
				mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
				btScoStatus = true;
			} catch (Exception e) {
				System.out.println("Exception starting BT SCO " + e.getMessage() );
				btScoStatus = false;
				btScoInit = "Available, but not initialized.\n(" + e.getMessage() + ")";
				return false;
			}
			btScoInit = "Available, initialized OK.";
			return true;
		} else {
			AudioManager mAudioManager = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
			if (mAudioManager == null) {
				return false;
			}
			mAudioManager.setBluetoothScoOn(false);
			mAudioManager.stopBluetoothSco();
			mAudioManager.setMode(AudioManager.MODE_NORMAL);
			btScoStatus = false;
			return true;
		}
	}
}