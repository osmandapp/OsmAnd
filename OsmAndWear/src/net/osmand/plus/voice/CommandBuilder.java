package net.osmand.plus.voice;

import net.osmand.plus.routing.data.StreetName;

import java.util.ArrayList;
import java.util.List;

public abstract class CommandBuilder {

	protected static final String SET_MODE = "setMode";
	protected static final String SET_METRIC_CONST = "setMetricConst";

	protected static final String C_PREPARE_TURN = "prepare_turn";
	protected static final String C_PREPARE_ROUNDABOUT = "prepare_roundabout";
	protected static final String C_PREPARE_MAKE_UT = "prepare_make_ut";
	protected static final String C_ROUNDABOUT = "roundabout";
	protected static final String C_GO_AHEAD = "go_ahead";
	protected static final String C_TURN = "turn";
	protected static final String C_MAKE_UT = "make_ut";
	protected static final String C_MAKE_UTWP = "make_ut_wp";
	protected static final String C_AND_ARRIVE_DESTINATION = "and_arrive_destination";
	protected static final String C_REACHED_DESTINATION = "reached_destination";
	protected static final String C_AND_ARRIVE_INTERMEDIATE = "and_arrive_intermediate";
	protected static final String C_REACHED_INTERMEDIATE = "reached_intermediate";
	protected static final String C_AND_ARRIVE_WAYPOINT = "and_arrive_waypoint";
	protected static final String C_AND_ARRIVE_FAVORITE = "and_arrive_favorite";
	protected static final String C_AND_ARRIVE_POI_WAYPOINT = "and_arrive_poi";
	protected static final String C_REACHED_WAYPOINT = "reached_waypoint";
	protected static final String C_REACHED_FAVORITE = "reached_favorite";
	protected static final String C_REACHED_POI = "reached_poi";
	protected static final String C_THEN = "then";
	protected static final String C_SPEAD_ALARM = "speed_alarm";
	protected static final String C_ATTENTION = "attention";
	protected static final String C_OFF_ROUTE = "off_route";
	protected static final String C_BACK_ON_ROUTE = "back_on_route";
	protected static final String C_TAKE_EXIT = "take_exit";

	protected static final String C_BEAR_LEFT = "bear_left";
	protected static final String C_BEAR_RIGHT = "bear_right";
	protected static final String C_ROUTE_RECALC = "route_recalc";
	protected static final String C_ROUTE_NEW_CALC = "route_new_calc";
	protected static final String C_LOCATION_LOST = "location_lost";
	protected static final String C_LOCATION_RECOVERED = "location_recovered";

	protected final CommandPlayer commandPlayer;

	protected final List<String> listCommands = new ArrayList<>();
	protected final List<String> listStruct = new ArrayList<>();

	public CommandBuilder(CommandPlayer commandPlayer) {
		this.commandPlayer = commandPlayer;
	}

	public List<String> getCommandsList() {
		return listCommands;
	}

	public abstract List<String> play();

	public abstract List<String> execute();

	public abstract CommandBuilder goAhead();

	public abstract CommandBuilder goAhead(double dist, StreetName streetName);

	public abstract CommandBuilder makeUTwp();

	public abstract CommandBuilder makeUT(StreetName streetName);

	public abstract CommandBuilder speedAlarm(int maxSpeed, float speed);

	public abstract CommandBuilder attention(String type);

	public abstract CommandBuilder offRoute(double dist);

	public abstract CommandBuilder backOnRoute();

	public abstract CommandBuilder makeUT(double dist, StreetName streetName);

	public abstract CommandBuilder prepareMakeUT(double dist, StreetName streetName);

	public abstract CommandBuilder turn(String param, StreetName streetName);

	public abstract CommandBuilder turn(String param, double dist, StreetName streetName);

	public abstract CommandBuilder takeExit(String turnType, String exitString, int exitInt, StreetName streetName);

	public abstract CommandBuilder takeExit(String turnType, double dist, String exitString, int exitInt, StreetName streetName);

	/**
	 * @param turnType {@link CommandPlayer#A_LEFT},
	 *                 {@link CommandPlayer#A_LEFT},..
	 */
	public abstract CommandBuilder prepareTurn(String turnType, double dist, StreetName streetName);

	public abstract CommandBuilder prepareRoundAbout(double dist, int exit, StreetName streetName);

	public abstract CommandBuilder roundAbout(double dist, double angle, int exit, StreetName streetName);

	public abstract CommandBuilder roundAbout(double angle, int exit, StreetName streetName);

	public abstract CommandBuilder andArriveAtDestination(String name);

	public abstract CommandBuilder arrivedAtDestination(String name);

	public abstract CommandBuilder andArriveAtIntermediatePoint(String name);

	public abstract CommandBuilder arrivedAtIntermediatePoint(String name);

	public abstract CommandBuilder andArriveAtWayPoint(String name);

	public abstract CommandBuilder arrivedAtWayPoint(String name);

	public abstract CommandBuilder andArriveAtFavorite(String name);

	public abstract CommandBuilder arrivedAtFavorite(String name);

	public abstract CommandBuilder andArriveAtPoi(String name);

	public abstract CommandBuilder arrivedAtPoi(String name);

	public abstract CommandBuilder bearLeft(StreetName streetName);

	public abstract CommandBuilder bearRight(StreetName streetName);

	public abstract CommandBuilder then();

	public abstract CommandBuilder gpsLocationLost();

	public abstract CommandBuilder gpsLocationRecover();

	public abstract CommandBuilder newRouteCalculated(double dist, int time);

	public abstract CommandBuilder routeRecalculated(double dist, int time);

	protected CommandBuilder addCommand(String name, Object... args) {
		addToCommandList(name, args);
		return this;
	}

	private void addToCommandList(String name, Object... args) {
		listCommands.add(name);
		for (Object o : args) {
			if (o != null) {
				listCommands.add(o.toString());
			} else {
				listCommands.add("");
			}
		}
	}
}