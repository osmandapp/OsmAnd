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
import android.content.Context;

public abstract class AbstractPrologCommandPlayer implements CommandPlayer {

	private static final Log log = LogUtil
			.getLog(AbstractPrologCommandPlayer.class);

	protected Context ctx;
	protected File voiceDir;
	protected Prolog prologSystem;
	protected static final String P_VERSION = "version";
	protected static final String P_RESOLVE = "resolve";
	public static final String A_LEFT = "left";
	public static final String A_LEFT_SH = "left_sh";
	public static final String A_LEFT_SL = "left_sl";
	public static final String A_RIGHT = "right";
	public static final String A_RIGHT_SH = "right_sh";
	public static final String A_RIGHT_SL = "right_sl";
	protected static final String DELAY_CONST = "delay_";
	/** Must be sorted array! */
	private final int[] sortedVoiceVersions;

	protected AbstractPrologCommandPlayer(Context ctx, String voiceProvider, String configFile, int[] sortedVoiceVersions)
		throws CommandPlayerException 
	{
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
		init(voiceProvider, configFile);
	}
	
	public String[] getLibraries(){
		return new String[] { "alice.tuprolog.lib.BasicLibrary",
					"alice.tuprolog.lib.ISOLibrary"};
	}

	private void init(String voiceProvider, String configFile) throws CommandPlayerException {
		prologSystem.clearTheory();
		voiceDir = null;
		if (voiceProvider != null) {
			File parent = OsmandApplication.getSettings().extendOsmandPath(ResourceManager.VOICE_PATH);
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
					prologSystem.setTheory(new Theory(config));
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

}
