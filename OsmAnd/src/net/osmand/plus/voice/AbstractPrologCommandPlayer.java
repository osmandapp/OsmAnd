package net.osmand.plus.voice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.osmand.LogUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;

import org.apache.commons.logging.Log;

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
import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;

public abstract class AbstractPrologCommandPlayer implements CommandPlayer {

	private static final Log log = LogUtil.getLog(AbstractPrologCommandPlayer.class);

	protected Context ctx;
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
	/** Must be sorted array! */
	private final int[] sortedVoiceVersions;
	private AudioFocusHelper mAudioFocusHelper;

	protected int streamType;

	protected AbstractPrologCommandPlayer(Context ctx, OsmandSettings settings, String voiceProvider, String configFile, int[] sortedVoiceVersions)
		throws CommandPlayerException 
	{
		this.ctx = ctx;
		this.sortedVoiceVersions = sortedVoiceVersions;
		long time = System.currentTimeMillis();
		try {
			this.ctx = ctx;
			prologSystem = new Prolog(getLibraries());
		} catch (InvalidLibraryException e) {
			log.error("Initializing error", e); //$NON-NLS-1$
			throw new RuntimeException(e);
		}
		if (log.isInfoEnabled()) {
			log.info("Initializing prolog system : " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
		}
		this.streamType = settings.AUDIO_STREAM_GUIDANCE.get();  
		init(voiceProvider, settings, configFile);
	}
	
	public String[] getLibraries(){
		return new String[] { "alice.tuprolog.lib.BasicLibrary",
					"alice.tuprolog.lib.ISOLibrary"};
	}

	private void init(String voiceProvider, OsmandSettings settings, String configFile) throws CommandPlayerException {
		prologSystem.clearTheory();
		voiceDir = null;
		if (voiceProvider != null) {
			File parent = ((OsmandApplication)ctx.getApplicationContext()).getSettings().extendOsmandPath(ResourceManager.VOICE_PATH);
			voiceDir = new File(parent, voiceProvider);
			if (!voiceDir.exists()) {
				voiceDir = null;
				throw new CommandPlayerException(
						ctx.getString(R.string.voice_data_unavailable));
			}
		}

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
				if (!wrong) {
					MetricsConstants mc = settings.METRIC_SYSTEM.get();
					prologSystem.addTheory(new Theory("measure('"+mc.toTTSString()+"')."));
					prologSystem.addTheory(new Theory(config));
					
				}
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
			}
			if (log.isInfoEnabled()) {
				log.info("Initializing voice subsystem  " + voiceProvider + " : " + (System.currentTimeMillis() - time)); //$NON-NLS-1$ //$NON-NLS-2$
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
		return files;
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
		ctx = null;
		prologSystem = null;
	}

	@Override
	public void updateAudioStream(int streamType) {
		this.streamType = streamType;
	}
	
	protected void requestAudioFocus() {
		log.debug("requestAudioFocus");
		if (android.os.Build.VERSION.SDK_INT >= 8) {
		    mAudioFocusHelper = new AudioFocusHelper(ctx);
		}
		if (mAudioFocusHelper != null)
			mAudioFocusHelper.requestFocus();
	}
	
	protected void abandonAudioFocus() {
		log.debug("abandonAudioFocus");
		if (mAudioFocusHelper != null) {
			mAudioFocusHelper.abandonFocus();
			mAudioFocusHelper = null;
		}
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
	    	// Basically we ignore audio focus changes.  There's not much we can do when we have interrupted audio
	    	// for our speech, and we in turn get interrupted.  Ignore it until a scenario comes up which gives us
	    	// reason to change this strategy.
	    	log.error("MediaCommandPlayerImpl.onAudioFocusChange(): Unexpected audio focus change: "+focusChange);
	    }
	}
}
