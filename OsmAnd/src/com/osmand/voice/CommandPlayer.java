package com.osmand.voice;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

import com.osmand.Algoritms;
import com.osmand.LogUtil;
import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.ResourceManager;

public class CommandPlayer {
	
	public static final String VOICE_DIR = ResourceManager.APP_DIR + "/voice"; //$NON-NLS-1$
	public static final int VOICE_VERSION = 0;
	private static final Log log = LogUtil.getLog(CommandPlayer.class);
	
	private static CommandPlayer instance = null;

	protected Context ctx;
	private File voiceDir;
	
	// resolving commands to play
	private Prolog prologSystem;
	
	// playing media
	private MediaPlayer mediaPlayer;
	private List<String> filesToPlay = new ArrayList<String>();

	/**
	 * @param ctx
	 * @return null could be returned it means there is no available voice config
	 */
	public static CommandPlayer getInstance(Context ctx) {
		init(ctx);
		return instance;
	}
	
	
	public static String init(Context ctx){
		if(OsmandSettings.getVoiceProvider(ctx) == null && instance == null){
			return null;
		}
		if(instance == null){
			long time = System.currentTimeMillis();
			instance = new CommandPlayer(ctx);
			if (log.isInfoEnabled()) {
				log.info("Initializing prolog system : " + (System.currentTimeMillis() - time)); //$NON-NLS-1$
			}
		}
		instance.ctx = ctx;
		if(!Algoritms.objectEquals(OsmandSettings.getVoiceProvider(ctx), instance.getCurrentVoice())){
			return instance.init();
		}
		
		return null;
	}
	
	protected CommandPlayer(Context ctx){
		try {
			this.ctx = ctx;
			prologSystem = new Prolog(new String[]{"alice.tuprolog.lib.BasicLibrary"}); //$NON-NLS-1$
		} catch (InvalidLibraryException e) {
			log.error("Initializing error", e); //$NON-NLS-1$
			throw new RuntimeException(e);
		}
		mediaPlayer = new MediaPlayer();
	}
	
	public String getCurrentVoice(){
		if(voiceDir == null){
			return null;
		}
		return voiceDir.getName(); 
	}
	
	protected String init(){
		String voiceProvider = OsmandSettings.getVoiceProvider(ctx);
		prologSystem.clearTheory();
		voiceDir = null;
		if(voiceProvider != null){
			File parent = new File(Environment.getExternalStorageDirectory(), VOICE_DIR);
			voiceDir = new File(parent, voiceProvider);
			if(!voiceDir.exists()){
				voiceDir = null;
				return ctx.getString(R.string.voice_data_unavailable);
			}
		}
		if(voiceDir != null) {
			long time = System.currentTimeMillis();
			File config = new File(voiceDir, "_config.p"); //$NON-NLS-1$
			boolean wrong = !config.exists();
				
			if (!wrong) {

				try {
					prologSystem.setTheory(new Theory(new FileInputStream(config)));
				} catch (InvalidTheoryException e) {
					log.error("Loading voice config exception " + voiceProvider, e); //$NON-NLS-1$
				} catch (IOException e) {
					log.error("Loading voice config exception " + voiceProvider, e); //$NON-NLS-1$
				}
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
							versionSupported = ((Number) val).intValue() == VOICE_VERSION;
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
	
	private void playQueue() {
		boolean playNext = true;
		while (!filesToPlay.isEmpty() && playNext) {
			String f = filesToPlay.remove(0);
			if (f != null && voiceDir != null) {
				File file = new File(voiceDir, f);
				if (file.exists()) {
					try {
						mediaPlayer.setDataSource(file.getAbsolutePath());
						mediaPlayer.prepare();
						mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
							public void onCompletion(MediaPlayer mp) {
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
								playQueue();
							}
						});
						playNext = false;
						mediaPlayer.start();
					} catch (Exception e) {
						log.error("Error while playing voice command", e); //$NON-NLS-1$
						playNext = true;
						
					}
				}
			}
		}
	}
	
	
	
	protected static final String P_VERSION = "version";  //$NON-NLS-1$
	protected static final String P_RESOLVE = "resolve";  //$NON-NLS-1$
	
	protected static final String A_LEFT = "left"; //$NON-NLS-1$
	protected static final String A_LEFT_SH = "left_sh"; //$NON-NLS-1$
	protected static final String A_LEFT_SL = "left_sl"; //$NON-NLS-1$
	protected static final String A_RIGHT = "right"; //$NON-NLS-1$
	protected static final String A_RIGHT_SH = "right_sh"; //$NON-NLS-1$
	protected static final String A_RIGHT_SL = "right_sl"; //$NON-NLS-1$
	
	protected static final String С_PREPARE_TURN = "prepare_turn";  //$NON-NLS-1$
	protected static final String С_PREPARE_MAKE_UT = "prepare_make_ut";  //$NON-NLS-1$
	protected static final String С_GO_AHEAD = "go_ahead";  //$NON-NLS-1$
	protected static final String С_TURN = "turn";  //$NON-NLS-1$
	protected static final String С_MAKE_UT = "make_ut";  //$NON-NLS-1$
	protected static final String С_PREAMBLE = "preamble";  //$NON-NLS-1$
	
	protected static final String DELAY_CONST = "delay_"; //$NON-NLS-1$
	
	public class CommandBuilder {
		
		
		private boolean alreadyExecuted = false;
		private List<Struct> listStruct = new ArrayList<Struct>();
		
		public CommandBuilder(){
			this(true);
		}
		
		public CommandBuilder(boolean preamble) {
			if (preamble) {
				addCommand(С_PREAMBLE);
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
			listStruct.add(struct);
			return this;
		}
		
		
		public CommandBuilder goAhead(){
			return addCommand(С_GO_AHEAD);
		}
		
		public CommandBuilder goAhead(double dist){
			return addCommand(С_GO_AHEAD, dist);
		}
		
		public CommandBuilder makeUT(){
			return addCommand(С_MAKE_UT);
		}
		
		public CommandBuilder makeUT(double dist){
			return addCommand(С_MAKE_UT, dist);
		}
		
		public CommandBuilder prepareMakeUT(double dist){
			return addCommand(С_PREPARE_MAKE_UT, dist);
		}
		
		
		public CommandBuilder turnLeft(){
			return addCommand(С_TURN, A_LEFT);
		}
		
		public CommandBuilder turnSLLeft(){
			return addCommand(С_TURN, A_LEFT_SL);
		}
		
		public CommandBuilder turnSHLeft(){
			return addCommand(С_TURN, A_LEFT_SH);
		}
		
		public CommandBuilder turnRight(){
			return addCommand(С_TURN, A_RIGHT);
		}
		
		public CommandBuilder turnSLRight(){
			return addCommand(С_TURN, A_RIGHT_SL);
		}
		
		public CommandBuilder turnSHRight(){
			return addCommand(С_TURN, A_RIGHT_SL);
		}
		
		
		public CommandBuilder turnLeft(double dist){
			return addCommand(С_TURN, A_LEFT, dist);
		}
		
		public CommandBuilder turnSLLeft(double dist){
			return addCommand(С_TURN, A_LEFT_SL, dist);
		}
		
		public CommandBuilder turnSHLeft(double dist){
			return addCommand(С_TURN, A_LEFT_SH, dist);
		}
		
		public CommandBuilder turnRight(double dist){
			return addCommand(С_TURN, A_RIGHT, dist);
		}

		public CommandBuilder turnSLRight(double dist){
			return addCommand(С_TURN, A_RIGHT_SL, dist);
		}
		
		public CommandBuilder turnSHRight(double dist){
			return addCommand(С_TURN, A_RIGHT_SL, dist);
		}
		
		public CommandBuilder prepareTurnLeft(double dist){
			return addCommand(С_PREPARE_TURN, A_LEFT, dist);
		}
		
		public CommandBuilder prepareTurnSLLeft(double dist){
			return addCommand(С_PREPARE_TURN, A_LEFT_SL, dist);
		}
		public CommandBuilder prepareTurnSHLeft(double dist){
			return addCommand(С_PREPARE_TURN, A_LEFT_SH, dist);
		}
		
		public CommandBuilder prepareTurnRight(double dist){
			return addCommand(С_PREPARE_TURN, A_RIGHT, dist);
		}
		
		public CommandBuilder prepareTurnSLRight(double dist){
			return addCommand(С_PREPARE_TURN, A_RIGHT_SL, dist);
		}
		
		public CommandBuilder prepareTurnSHRight(double dist){
			return addCommand(С_PREPARE_TURN, A_RIGHT_SH, dist);
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
	
	

