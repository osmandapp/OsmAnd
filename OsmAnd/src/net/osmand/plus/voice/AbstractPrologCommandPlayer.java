package net.osmand.plus.voice;

import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import android.media.AudioManager;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.osmand.plus.R;
import net.osmand.plus.api.AudioFocusHelper;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import alice.tuprolog.InvalidLibraryException;
import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.Number;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;
import alice.tuprolog.Var;

public abstract class AbstractPrologCommandPlayer implements CommandPlayer, StateChangedListener<ApplicationMode> {

	private static final Log log = PlatformUtil.getLog(AbstractPrologCommandPlayer.class);

	protected OsmandApplication ctx;
	protected File voiceDir;
	protected Prolog prologSystem;
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
	private static final String WEAR_ALERT = "WEAR_ALERT";
	/** Must be sorted array! */
	private int[] sortedVoiceVersions;
	private static AudioFocusHelper mAudioFocusHelper;
	protected String language = "";
	protected int streamType;
	private static int currentVersion;
	private ApplicationMode applicationMode;


	protected AbstractPrologCommandPlayer(OsmandApplication ctx, ApplicationMode applicationMode,
										  String voiceProvider, String configFile, int[] sortedVoiceVersions)
			throws CommandPlayerException {
		this.ctx = ctx;
		this.sortedVoiceVersions = sortedVoiceVersions;
		this.applicationMode = applicationMode;
		long time = System.currentTimeMillis();
		this.ctx = ctx;

		this.streamType = ctx.getSettings().AUDIO_STREAM_GUIDANCE.getModeValue(applicationMode);
		initVoiceDir(voiceProvider);
		if (voiceDir != null && (MediaCommandPlayerImpl.isMyData(voiceDir) || TTSCommandPlayerImpl.isMyData(voiceDir))) {
			if (log.isInfoEnabled()) {
				log.info("Initializing prolog system : " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
			}
			try {
				prologSystem = new Prolog(getLibraries());
			} catch (InvalidLibraryException e) {
				log.error("Initializing error", e); //$NON-NLS-1$
				throw new RuntimeException(e);
			}
			init(voiceProvider, ctx.getSettings(), configFile);
			final Term langVal = solveSimplePredicate("language");
			if (langVal instanceof Struct) {
				language = ((Struct) langVal).getName();
			}
		} else {
			language = voiceProvider.replace("-tts", "").replace("-formal", "");
		}
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
	
	public void sendAlertToAndroidWear(Context ctx, String message) {
		int notificationId = 1;
		NotificationCompat.Builder notificationBuilder =
				new NotificationCompat.Builder(ctx)
						.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
						.setSmallIcon(R.mipmap.icon)
						.setContentTitle(ctx.getString(R.string.app_name))
						.setContentText(message)
						.setGroup(WEAR_ALERT);

		// Get an instance of the NotificationManager service
		NotificationManagerCompat notificationManager =
				NotificationManagerCompat.from(ctx);
		// Build the notification and issues it with notification manager.
		notificationManager.notify(notificationId, notificationBuilder.build());
	}

	@Override
	public void stateChanged(ApplicationMode change) {
		if(prologSystem != null) {
			try {
				prologSystem.getTheoryManager().retract(new Struct("appMode", new Var()));
			} catch (Exception e) {
				log.error("Retract error: ", e);
			}
			prologSystem.getTheoryManager()
				.assertA(
						new Struct("appMode", new Struct(ctx.getSettings().APPLICATION_MODE.get().getStringKey()
								.toLowerCase())), true, "", true);
		}
	}
	
	private void init(String voiceProvider, OsmandSettings settings, String configFile) throws CommandPlayerException {
		prologSystem.clearTheory();

		// see comments below why it is impossible to read from zip (don't know
		// how to play file from zip)
		// voiceZipFile = null;
		if (voiceDir != null) {
			long time = System.currentTimeMillis();
			boolean wrong = false;
			try {
				InputStream config;
				//			if (voiceDir.getName().endsWith(".zip")) { //$NON-NLS-1$
				// voiceZipFile = new ZipFile(voiceDir);
				//				config = voiceZipFile.getInputStream(voiceZipFile.getEntry("_config.p")); //$NON-NLS-1$
				// } else {
				config = new FileInputStream(new File(voiceDir, configFile)); //$NON-NLS-1$
				// }
				MetricsConstants mc = settings.METRIC_SYSTEM.get();
				settings.APPLICATION_MODE.addListener(this);
				prologSystem.getTheoryManager()
				.assertA(
						new Struct("appMode", new Struct(ctx.getSettings().APPLICATION_MODE.get().getStringKey()
								.toLowerCase())), true, "", true);
				prologSystem.addTheory(new Theory("measure('"+mc.toTTSString()+"')."));
				prologSystem.addTheory(new Theory(config));
				config.close();
			} catch (InvalidTheoryException e) {
				log.error("Loading voice config exception " + voiceProvider, e); //$NON-NLS-1$
				wrong = true;
			} catch (IOException e) {
				log.error("Loading voice config exception " + voiceProvider, e); //$NON-NLS-1$
				wrong = true;
			}
			if (wrong) {
				throw new CommandPlayerException(ctx.getString(R.string.voice_data_corrupted));
			} else {
				Term val = solveSimplePredicate(P_VERSION);
				if (!(val instanceof Number) ||  Arrays.binarySearch(sortedVoiceVersions,((Number)val).intValue()) < 0) {
					throw new CommandPlayerException(ctx.getString(R.string.voice_data_not_supported));
				}
				currentVersion = ((Number)val).intValue();
			}
			if (log.isInfoEnabled()) {
				log.info("Initializing voice subsystem  " + voiceProvider + " : " + (System.currentTimeMillis() - time)); //$NON-NLS-1$ //$NON-NLS-2$
			}

		}
	}

	private void initVoiceDir(String voiceProvider) throws CommandPlayerException {
		if (voiceProvider != null) {
			File parent = ctx.getAppPath(IndexConstants.VOICE_INDEX_DIR);
			voiceDir = new File(parent, voiceProvider);
			if (!voiceDir.exists()) {
				voiceDir = null;
				throw new CommandPlayerException(
						ctx.getString(R.string.voice_data_unavailable));
			}
		}
	}

	protected Term solveSimplePredicate(String predicate) {
		Term val = null;
		Var v = new Var("MyVariable"); //$NON-NLS-1$
		SolveInfo s = prologSystem.solve(new Struct(predicate, v));
		if (s.isSuccess()) {
			prologSystem.solveEnd();
			try {
				val = s.getVarValue(v.getName());
			} catch (NoSolutionException e) {
			}
		}
		return val;
	}

	@Override
	public List<String> execute(List<Struct> listCmd){
		Struct list = new Struct(listCmd.toArray(new Term[listCmd.size()]));
		Var result = new Var("RESULT"); //$NON-NLS-1$
		List<String> files = new ArrayList<String>();
		if(prologSystem == null) {
			return files;
		}
		if (log.isInfoEnabled()) {
			log.info("Query speak files " + listCmd);
		}
		SolveInfo res = prologSystem.solve(new Struct(P_RESOLVE, list, result));
		
		if (res.isSuccess()) {
			try {
				prologSystem.solveEnd();	
				Term solution = res.getVarValue(result.getName());
				
				Iterator<?> listIterator = ((Struct) solution).listIterator();
				while(listIterator.hasNext()){
					Object term = listIterator.next();
					if(term instanceof Struct){
						files.add(((Struct) term).getName());
					}
				}
				
			} catch (NoSolutionException e) {
			}
		}
		if (log.isInfoEnabled()) {
			log.info("Speak files " + files);
		}
		return files;
	}
	
	public static int getCurrentVersion() {
		return currentVersion;
	}
	
	@Override
	public String getCurrentVoice() {
		if (voiceDir == null) {
			return null;
		}
		return voiceDir.getName();
	}

	@Override
	public CommandBuilder newCommandBuilder() {
		return new CommandBuilder(this);
	}

	@Override
	public void clear() {
		if(ctx != null && ctx.getSettings() != null) {
			ctx.getSettings().APPLICATION_MODE.removeListener(this);
		}
		abandonAudioFocus();
		ctx = null;
		prologSystem = null;
	}

	@Override
	public void updateAudioStream(int streamType) {
		this.streamType = streamType;
	}

	protected synchronized void requestAudioFocus() {
		log.debug("requestAudioFocus");
		if (android.os.Build.VERSION.SDK_INT >= 8) {
			mAudioFocusHelper = getAudioFocus();
		}
		if (mAudioFocusHelper != null && ctx != null) {
			boolean audioFocusGranted = mAudioFocusHelper.requestFocus(ctx, applicationMode, streamType);
			// If AudioManager.STREAM_VOICE_CALL try using BT SCO:
			if (audioFocusGranted && ctx.getSettings().AUDIO_STREAM_GUIDANCE.getModeValue(applicationMode) == 0) {
				toggleBtSco(true);
			}
		}
	}

	private AudioFocusHelper getAudioFocus() {
		try {
			return new net.osmand.plus.api.AudioFocusHelperImpl();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}
	
	protected synchronized void abandonAudioFocus() {
		log.debug("abandonAudioFocus");
		if ((ctx != null && ctx.getSettings().AUDIO_STREAM_GUIDANCE.getModeValue(applicationMode) == 0) || (btScoStatus == true)) {
			toggleBtSco(false);
		}
		if (ctx != null && mAudioFocusHelper != null) {
			mAudioFocusHelper.abandonFocus(ctx, applicationMode, streamType);
		}
		mAudioFocusHelper = null;
	}

	public static boolean btScoStatus = false;

	// BT_SCO_DELAY now in Settings. 1500 ms works for most configurations.
	//public static final int BT_SCO_DELAY = 1500;

	// This only needed for init debugging in TestVoiceActivity:
	public static String btScoInit = "-";

	private synchronized boolean toggleBtSco(boolean on) {
	// Hardy, 2016-07-03: Establish a low quality BT SCO (Synchronous Connection-Oriented) link to interrupt e.g. a car stereo FM radio
		if (on) {
			try {
				AudioManager mAudioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
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
				btScoInit = "Available, but not initializad.\n(" + e.getMessage() + ")";
				return false;
			}
			btScoInit = "Available, initialized OK.";
			return true;
		} else {
			AudioManager mAudioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
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
