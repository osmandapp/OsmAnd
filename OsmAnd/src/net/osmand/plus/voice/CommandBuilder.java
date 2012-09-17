package net.osmand.plus.voice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.osmand.LogUtil;

import org.apache.commons.logging.Log;

import alice.tuprolog.Struct;
import alice.tuprolog.Term;

public class CommandBuilder {
	
	private static final Log log = LogUtil.getLog(CommandBuilder.class);
	
	protected static final String C_PREPARE_TURN = "prepare_turn";  //$NON-NLS-1$
	protected static final String C_PREPARE_ROUNDABOUT = "prepare_roundabout";  //$NON-NLS-1$
	protected static final String C_PREPARE_MAKE_UT = "prepare_make_ut";  //$NON-NLS-1$
	protected static final String C_ROUNDABOUT = "roundabout";  //$NON-NLS-1$
	protected static final String C_GO_AHEAD = "go_ahead";  //$NON-NLS-1$
	protected static final String C_TURN = "turn";  //$NON-NLS-1$
	protected static final String C_MAKE_UT = "make_ut";  //$NON-NLS-1$
	protected static final String C_MAKE_UTWP = "make_ut_wp";  //$NON-NLS-1$
	protected static final String C_PREAMBLE = "preamble";  //$NON-NLS-1$
	protected static final String C_AND_ARRIVE_DESTINATION = "and_arrive_destination";  //$NON-NLS-1$
	protected static final String C_REACHED_DESTINATION = "reached_destination";  //$NON-NLS-1$
	protected static final String C_AND_ARRIVE_INTERMEDIATE = "and_arrive_intermediate";  //$NON-NLS-1$
	protected static final String C_REACHED_INTERMEDIATE = "reached_intermediate";  //$NON-NLS-1$
	protected static final String C_THEN = "then";  //$NON-NLS-1$
	
	protected static final String C_BEAR_LEFT = "bear_left";  //$NON-NLS-1$
	protected static final String C_BEAR_RIGHT = "bear_right";  //$NON-NLS-1$
	protected static final String C_ROUTE_RECALC = "route_recalc";  //$NON-NLS-1$
	protected static final String C_ROUTE_NEW_CALC = "route_new_calc";  //$NON-NLS-1$
	protected static final String C_LOCATION_LOST = "location_lost";  //$NON-NLS-1$
	
	/**
	 * 
	 */
	private final CommandPlayer commandPlayer;
	private boolean alreadyExecuted = false;
	private List<Struct> listStruct = new ArrayList<Struct>();
	
	public CommandBuilder(CommandPlayer commandPlayer){
		this(commandPlayer, true);
	}
	
	public CommandBuilder(CommandPlayer commandPlayer, boolean preamble) {
		this.commandPlayer = commandPlayer;
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
	
	public CommandBuilder makeUTwp(){
		return addCommand(C_MAKE_UTWP);
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
	
	public CommandBuilder arrivedAtIntermediatePoint() {
		return addCommand(C_REACHED_INTERMEDIATE);
	}
	
	public CommandBuilder andArriveAtIntermediatePoint(){
		return addCommand(C_AND_ARRIVE_INTERMEDIATE);
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
	
	public CommandBuilder gpsLocationLost() {
		return addCommand(C_LOCATION_LOST);
	}
	
	public CommandBuilder newRouteCalculated(double dist){
		return addCommand(C_ROUTE_NEW_CALC, dist);
	}
	
	public CommandBuilder routeRecalculated(double dist){
		return addCommand(C_ROUTE_RECALC, dist);
	}

	
	
	public void play(){
		this.commandPlayer.playCommands(this);
	}
	
	protected List<String> execute(){
		alreadyExecuted = true;
		return this.commandPlayer.execute(listStruct);
	}


	
}