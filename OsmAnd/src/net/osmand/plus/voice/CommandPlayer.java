package net.osmand.plus.voice;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.osmand.LogUtil;
import net.osmand.data.index.IndexConstants;
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
import android.media.MediaPlayer;
import android.os.Environment;

/**
 * That class represents command player. 
 * It gets commands from input, analyze what files should be played and play 
 * them using media player 
 */
public class CommandPlayer {
	
	private static final Log log = LogUtil.getLog(CommandPlayer.class);
	
	protected Context ctx;
	// or zip file
	private File voiceDir;
//	private ZipFile voiceZipFile;
	
	// resolving commands to play
	private Prolog prologSystem;
	
	// playing media
	private MediaPlayer mediaPlayer;
	// indicates that player is ready to play first file
	private boolean playNext = true;
	private List<String> filesToPlay = Collections.synchronizedList(new ArrayList<String>());

	
	public CommandPlayer(Context ctx){
		long time = System.currentTimeMillis();
		try {
			this.ctx = ctx;
			prologSystem = new Prolog(new String[]{"alice.tuprolog.lib.BasicLibrary"}); //$NON-NLS-1$
		} catch (InvalidLibraryException e) {
			log.error("Initializing error", e); //$NON-NLS-1$
			throw new RuntimeException(e);
		}
		mediaPlayer = new MediaPlayer();
		if (log.isInfoEnabled()) {
			log.info("Initializing prolog system : " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
		}
	}
	
	
	public String getCurrentVoice(){
		if(voiceDir == null){
			return null;
		}
		return voiceDir.getName(); 
	}
	
	public String init(String voiceProvider){
		prologSystem.clearTheory();
		voiceDir = null;
		if(voiceProvider != null){
			File parent = new File(Environment.getExternalStorageDirectory(), ResourceManager.VOICE_PATH);
			voiceDir = new File(parent, voiceProvider);
			if(!voiceDir.exists()){
				voiceDir = null;
				return ctx.getString(R.string.voice_data_unavailable);
			}
		}
		
		// see comments below why it is impossible to read from zip (don't know how to play file from zip) 
//		voiceZipFile = null;
		if(voiceDir != null) {
			long time = System.currentTimeMillis();
			boolean wrong = false;
			try {
				InputStream config;
//				if (voiceDir.getName().endsWith(".zip")) { //$NON-NLS-1$
//					voiceZipFile = new ZipFile(voiceDir);
//					config = voiceZipFile.getInputStream(voiceZipFile.getEntry("_config.p")); //$NON-NLS-1$
//				} else {
					config = new FileInputStream(new File(voiceDir, "_config.p")); //$NON-NLS-1$
//				}
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
			if(wrong){
				return ctx.getString(R.string.voice_data_corrupted);
			} else {
				boolean versionSupported = false;
				Var v = new Var("VERSION"); //$NON-NLS-1$
				SolveInfo s = prologSystem.solve(new Struct(P_VERSION, v));
				if(s.isSuccess()){
					prologSystem.solveEnd();
					try {
						Term val = s.getVarValue(v.getName());
						if(val instanceof Number){
							versionSupported = ((Number) val).intValue() == IndexConstants.VOICE_VERSION;
						}
					} catch (NoSolutionException e) {
					}
				}
				if(!versionSupported){
					return ctx.getString(R.string.voice_data_not_supported);
				}
			}
			
			if (log.isInfoEnabled()) {
				log.info("Initializing voice subsystem  " + voiceProvider + " : " + (System.currentTimeMillis() - time)); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
		}
		return null;
	}
	
	public CommandBuilder newCommandBuilder(){
		return new CommandBuilder();
	}
	
	
	protected List<String> execute(List<Struct> listCmd){
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
	
	
	
	protected static final String P_VERSION = "version";  //$NON-NLS-1$
	protected static final String P_RESOLVE = "resolve";  //$NON-NLS-1$
	
	public static final String A_LEFT = "left"; //$NON-NLS-1$
	public static final String A_LEFT_SH = "left_sh"; //$NON-NLS-1$
	public static final String A_LEFT_SL = "left_sl"; //$NON-NLS-1$
	public static final String A_RIGHT = "right"; //$NON-NLS-1$
	public static final String A_RIGHT_SH = "right_sh"; //$NON-NLS-1$
	public static final String A_RIGHT_SL = "right_sl"; //$NON-NLS-1$
	
	protected static final String C_PREPARE_TURN = "prepare_turn";  //$NON-NLS-1$
	protected static final String C_PREPARE_ROUNDABOUT = "prepare_roundabout";  //$NON-NLS-1$
	protected static final String C_PREPARE_MAKE_UT = "prepare_make_ut";  //$NON-NLS-1$
	protected static final String C_ROUNDABOUT = "roundabout";  //$NON-NLS-1$
	protected static final String C_GO_AHEAD = "go_ahead";  //$NON-NLS-1$
	protected static final String C_TURN = "turn";  //$NON-NLS-1$
	protected static final String C_MAKE_UT = "make_ut";  //$NON-NLS-1$
	protected static final String C_PREAMBLE = "preamble";  //$NON-NLS-1$
	protected static final String C_AND_ARRIVE_DESTINATION = "and_arrive_destination";  //$NON-NLS-1$
	protected static final String C_THEN = "then";  //$NON-NLS-1$
	protected static final String C_REACHED_DESTINATION = "reached_destination";  //$NON-NLS-1$
	protected static final String C_BEAR_LEFT = "bear_left";  //$NON-NLS-1$
	protected static final String C_BEAR_RIGHT = "bear_right";  //$NON-NLS-1$
	protected static final String C_ROUTE_RECALC = "route_recalc";  //$NON-NLS-1$
	protected static final String C_ROUTE_NEW_CALC = "route_new_calc";  //$NON-NLS-1$
	
	
	protected static final String DELAY_CONST = "delay_"; //$NON-NLS-1$
	
	public class CommandBuilder {
		
		
		private boolean alreadyExecuted = false;
		private List<Struct> listStruct = new ArrayList<Struct>();
		
		public CommandBuilder(){
			this(true);
		}
		
		public CommandBuilder(boolean preamble) {
			if (preamble) {
				addCommand(C_PREAMBLE);
			}
		}
		
		private void checkState(){
			if(alreadyExecuted){
				throw new IllegalArgumentException();
			}
		}
		
		private CommandBuilder addCommand(String name, Object... args){
			checkState();
			Term[] list = new Term[args.length];
			for (int i = 0; i < args.length; i++) {
				Object o = args[i];
				if(o instanceof java.lang.Number){
					if(o instanceof java.lang.Double){
						list[i] = new alice.tuprolog.Double((Double) o);
					} else if(o instanceof java.lang.Float){
						list[i] = new alice.tuprolog.Float((Float) o);
					} else if(o instanceof java.lang.Long){
						list[i] = new alice.tuprolog.Long((Long) o);
					} else {
						list[i] = new alice.tuprolog.Int(((java.lang.Number)o).intValue());
					}
				} else if(o instanceof String){
					list[i] = new Struct((String) o);
				}
				if(list[i]== null){
					throw new NullPointerException(name +" " + o); //$NON-NLS-1$
				}
			}
			Struct struct = new Struct(name, list);
			if(log.isDebugEnabled()){
				log.debug("Adding command : " + name + " " + Arrays.toString(args)); //$NON-NLS-1$ //$NON-NLS-2$
			}
			listStruct.add(struct);
			return this;
		}
		
		
		public CommandBuilder goAhead(){
			return addCommand(C_GO_AHEAD);
		}
		
		public CommandBuilder goAhead(double dist){
			return addCommand(C_GO_AHEAD, dist);
		}
		
		public CommandBuilder makeUT(){
			return addCommand(C_MAKE_UT);
		}
		
		public CommandBuilder makeUT(double dist){
			return addCommand(C_MAKE_UT, dist);
		}
		
		public CommandBuilder prepareMakeUT(double dist){
			return addCommand(C_PREPARE_MAKE_UT, dist);
		}
		
		
		public CommandBuilder turn(String param){
			return addCommand(C_TURN, param);
		}
		
		public CommandBuilder turn(String param, double dist){
			return addCommand(C_TURN, param, dist);
		}
		
		/**
		 * 
		 * @param param A_LEFT, A_RIGHT, ...
		 * @param dist
		 * @return
		 */
		public CommandBuilder prepareTurn(String param, double dist){
			return addCommand(C_PREPARE_TURN, param, dist);
		}
		
		public CommandBuilder prepareRoundAbout(double dist){
			return addCommand(C_PREPARE_ROUNDABOUT, dist);
		}
		
		public CommandBuilder roundAbout(double dist, double angle, int exit){
			return addCommand(C_ROUNDABOUT, dist, angle, exit);
		}
		
		public CommandBuilder roundAbout(double angle, int exit){
			return addCommand(C_ROUNDABOUT, angle, exit);
		}
		
		public CommandBuilder andArriveAtDestination(){
			return addCommand(C_AND_ARRIVE_DESTINATION);
		}
		
		public CommandBuilder arrivedAtDestination(){
			return addCommand(C_REACHED_DESTINATION);
		}
		
		public CommandBuilder bearLeft(){
			return addCommand(C_BEAR_LEFT);
		}
		
		public CommandBuilder bearRight(){
			return addCommand(C_BEAR_RIGHT);
		}
		
		public CommandBuilder then(){
			return addCommand(C_THEN);
		}
		
		public CommandBuilder newRouteCalculated(double dist){
			return addCommand(C_ROUTE_NEW_CALC, dist);
		}
		
		public CommandBuilder routeRecalculated(double dist){
			return addCommand(C_ROUTE_RECALC, dist);
		}
	
		
		
		public void play(){
			CommandPlayer.this.playCommands(this);
		}
		
		protected List<String> execute(){
			alreadyExecuted = true;
			return CommandPlayer.this.execute(listStruct);
		}
		
	}
	
	
}	
	
	

