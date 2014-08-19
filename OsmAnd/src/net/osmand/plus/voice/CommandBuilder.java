package net.osmand.plus.voice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

import alice.tuprolog.Struct;
import alice.tuprolog.Term;

public class CommandBuilder {
	
	private static final Log log = PlatformUtil.getLog(CommandBuilder.class);
	
	protected static final String C_PREPARE_TURN = "prepare_turn";  //$NON-NLS-1$
	protected static final String C_PREPARE_ROUNDABOUT = "prepare_roundabout";  //$NON-NLS-1$
	protected static final String C_PREPARE_MAKE_UT = "prepare_make_ut";  //$NON-NLS-1$
	protected static final String C_ROUNDABOUT = "roundabout";  //$NON-NLS-1$
	protected static final String C_GO_AHEAD = "go_ahead";  //$NON-NLS-1$
	protected static final String C_TURN = "turn";  //$NON-NLS-1$
	protected static final String C_MAKE_UT = "make_ut";  //$NON-NLS-1$
	protected static final String C_MAKE_UTWP = "make_ut_wp";  //$NON-NLS-1$
	protected static final String C_AND_ARRIVE_DESTINATION = "and_arrive_destination";  //$NON-NLS-1$
	protected static final String C_REACHED_DESTINATION = "reached_destination";  //$NON-NLS-1$
	protected static final String C_AND_ARRIVE_INTERMEDIATE = "and_arrive_intermediate";  //$NON-NLS-1$
	protected static final String C_REACHED_INTERMEDIATE = "reached_intermediate";  //$NON-NLS-1$
	protected static final String C_AND_ARRIVE_WAYPOINT = "and_arrive_waypoint";  //$NON-NLS-1$
	protected static final String C_AND_ARRIVE_FAVORITE = "and_arrive_favorite";  //$NON-NLS-1$
	protected static final String C_AND_ARRIVE_POI_WAYPOINT = "and_arrive_poi";  //$NON-NLS-1$
	protected static final String C_REACHED_WAYPOINT = "reached_waypoint";  //$NON-NLS-1$
	protected static final String C_REACHED_FAVORITE = "reached_favorite";  //$NON-NLS-1$
	protected static final String C_REACHED_POI = "reached_poi";  //$NON-NLS-1$
	protected static final String C_THEN = "then";  //$NON-NLS-1$
	protected static final String C_SPEAD_ALARM = "speed_alarm";  //$NON-NLS-1$
	protected static final String C_ATTENTION = "attention";  //$NON-NLS-1$
	protected static final String C_OFF_ROUTE = "off_route";  //$NON-NLS-1$
	
	
	protected static final String C_BEAR_LEFT = "bear_left";  //$NON-NLS-1$
	protected static final String C_BEAR_RIGHT = "bear_right";  //$NON-NLS-1$
	protected static final String C_ROUTE_RECALC = "route_recalc";  //$NON-NLS-1$
	protected static final String C_ROUTE_NEW_CALC = "route_new_calc";  //$NON-NLS-1$
	protected static final String C_LOCATION_LOST = "location_lost";  //$NON-NLS-1$
	protected static final String C_LOCATION_RECOVERED = "location_recovered";  //$NON-NLS-1$
	
	/**
	 * 
	 */
	private final CommandPlayer commandPlayer;
	private boolean alreadyExecuted = false;
	private List<Struct> listStruct = new ArrayList<Struct>();
	
	public CommandBuilder(CommandPlayer commandPlayer){
		this.commandPlayer = commandPlayer;
	}
	
	private void checkState()	{
		if(alreadyExecuted){
			throw new IllegalArgumentException();
		}
	}
	
	private CommandBuilder addCommand(String name, Object... args){
		Struct struct = prepareStruct(name, args);
		listStruct.add(struct);
		return this;
	}

	private Struct prepareStruct(String name, Object... args) {
		checkState();
		Term[] list = new Term[args.length];
		for (int i = 0; i < args.length; i++) {
			Object o = args[i];
			if(o instanceof Term){
				list[i] = (Term) o;
			} else if(o instanceof java.lang.Number){
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
			if(o == null){
				list[i] = new Struct("");
			}
		}
		Struct struct = new Struct(name, list);
		if(log.isDebugEnabled()){
			log.debug("Adding command : " + name + " " + Arrays.toString(args)); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return struct;
	}
	
	private CommandBuilder alt(Struct... s1) {
		if (s1.length == 1) {
			listStruct.add(s1[0]);
		} else {
			listStruct.add(new Struct(s1));
		}
		return this;
	}
	
	
	public CommandBuilder goAhead(double dist, Term streetName){
		return alt(prepareStruct(C_GO_AHEAD, dist, streetName), prepareStruct(C_GO_AHEAD, dist));
	}
	
	public CommandBuilder makeUTwp(){
		return addCommand(C_MAKE_UTWP);
	}

	public CommandBuilder makeUT(Term streetName){
		return alt(prepareStruct(C_MAKE_UT, streetName), prepareStruct(C_MAKE_UT));
	}
	
	public CommandBuilder speedAlarm(){
		return addCommand(C_SPEAD_ALARM);
	}
	
	public CommandBuilder attention(String type){
		return addCommand(C_ATTENTION, type);
	}
	
	public CommandBuilder offRoute(double dist){
		return addCommand(C_OFF_ROUTE, dist);
	}
	
	
	
	public CommandBuilder makeUT(double dist, Term streetName){
		return alt(prepareStruct(C_MAKE_UT, dist, streetName), prepareStruct(C_MAKE_UT, dist));
	}
	
	public CommandBuilder prepareMakeUT(double dist, Term streetName){
		return alt(prepareStruct(C_PREPARE_MAKE_UT, dist, streetName), prepareStruct(C_PREPARE_MAKE_UT, dist));
	}
	
	
	public CommandBuilder turn(String param, Term streetName) {
		return alt(prepareStruct(C_TURN, param, streetName), prepareStruct(C_TURN, param));
	}
	
	public CommandBuilder turn(String param, double dist, Term streetName){
		return alt(prepareStruct(C_TURN, param, dist, streetName), prepareStruct(C_TURN, param, dist));
	}
	
	/**
	 * 
	 * @param param A_LEFT, A_RIGHT, ...
	 * @param dist
	 * @return
	 */
	public CommandBuilder prepareTurn(String param, double dist, Term streetName){
		return alt(prepareStruct(C_PREPARE_TURN, param, dist, streetName), prepareStruct(C_PREPARE_TURN, param, dist));
	}
	
	public CommandBuilder prepareRoundAbout(double dist, int exit, Term streetName){
		return alt(prepareStruct(C_PREPARE_ROUNDABOUT, dist, exit, streetName), prepareStruct(C_PREPARE_ROUNDABOUT, dist));
	}
	
	public CommandBuilder roundAbout(double dist, double angle, int exit, Term streetName){
		return alt(prepareStruct(C_ROUNDABOUT, dist, angle, exit, streetName), prepareStruct(C_ROUNDABOUT, dist, angle, exit));
	}
	
	public CommandBuilder roundAbout(double angle, int exit, Term streetName) {
		return alt(prepareStruct(C_ROUNDABOUT, angle, exit, streetName), prepareStruct(C_ROUNDABOUT, angle, exit));
	}
	
	public CommandBuilder andArriveAtDestination(String name){
		return alt(prepareStruct(C_AND_ARRIVE_DESTINATION, name), prepareStruct(C_AND_ARRIVE_DESTINATION));
	}
	
	public CommandBuilder arrivedAtDestination(String name){
		return alt(prepareStruct(C_REACHED_DESTINATION, name), prepareStruct(C_REACHED_DESTINATION));
	}
	
	public CommandBuilder arrivedAtIntermediatePoint(String name) {
		return alt(prepareStruct(C_REACHED_INTERMEDIATE, name), prepareStruct(C_REACHED_INTERMEDIATE));
	}
	
	public CommandBuilder andArriveAtIntermediatePoint(String name){
		return alt(prepareStruct(C_AND_ARRIVE_INTERMEDIATE, name), prepareStruct(C_AND_ARRIVE_INTERMEDIATE));
	}
	
	public CommandBuilder arrivedAtWayPoint(String name) {
		return addCommand(C_REACHED_WAYPOINT, name);
	}

	public CommandBuilder arrivedAtFavorite(String name) {
		return addCommand(C_REACHED_FAVORITE, name);
	}

	public CommandBuilder arrivedAtPoi(String name) {
		return addCommand(C_REACHED_POI, name);
	}
	
	public CommandBuilder bearLeft(Term streetName){
		return alt(prepareStruct(C_BEAR_LEFT, streetName), prepareStruct(C_BEAR_LEFT));
	}
	
	public CommandBuilder bearRight(Term streetName){
		return alt(prepareStruct(C_BEAR_RIGHT, streetName), prepareStruct(C_BEAR_RIGHT));
	}
	
	public CommandBuilder then(){
		return addCommand(C_THEN);
	}
	
	public CommandBuilder gpsLocationLost() {
		return addCommand(C_LOCATION_LOST);
	}
	
	public CommandBuilder gpsLocationRecover() {
		return addCommand(C_LOCATION_RECOVERED);
	}
	
	public CommandBuilder newRouteCalculated(double dist, int time){
		return alt(prepareStruct(C_ROUTE_NEW_CALC, dist, time), prepareStruct(C_ROUTE_NEW_CALC, dist));
	}
	
	public CommandBuilder routeRecalculated(double dist, int time){
		return alt(prepareStruct(C_ROUTE_RECALC, dist, time), prepareStruct(C_ROUTE_RECALC, dist));
	}

	public void play(){
		this.commandPlayer.playCommands(this);
	}
	
	protected List<String> execute(){
		alreadyExecuted = true;
		return this.commandPlayer.execute(listStruct);
	}

	public CommandBuilder andArriveAtWayPoint(String name){
		return addCommand(C_AND_ARRIVE_WAYPOINT, name);
	}

	public CommandBuilder andArriveAtPoiWaypoint(String name) {
		return addCommand(C_AND_ARRIVE_POI_WAYPOINT, name);
	}

	public CommandBuilder andArriveAtFavorite(String name) {
		return addCommand(C_AND_ARRIVE_FAVORITE, name);
	}
}