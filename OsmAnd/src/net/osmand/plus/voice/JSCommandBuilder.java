package net.osmand.plus.voice;

import java.util.ArrayList;
import java.util.List;

import net.osmand.PlatformUtil;
import net.osmand.plus.routing.data.StreetName;

import org.apache.commons.logging.Log;
import org.json.JSONObject;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class JsCommandBuilder {

	private static final Log log = PlatformUtil.getLog(JsCommandBuilder.class);

	private static final String SET_MODE = "setMode";
	private static final String SET_METRIC_CONST = "setMetricConst";

	private static final String C_PREPARE_TURN = "prepare_turn";
	private static final String C_PREPARE_ROUNDABOUT = "prepare_roundabout";
	private static final String C_PREPARE_MAKE_UT = "prepare_make_ut";
	private static final String C_ROUNDABOUT = "roundabout";
	private static final String C_GO_AHEAD = "go_ahead";
	private static final String C_TURN = "turn";
	private static final String C_MAKE_UT = "make_ut";
	private static final String C_MAKE_UTWP = "make_ut_wp";
	private static final String C_AND_ARRIVE_DESTINATION = "and_arrive_destination";
	private static final String C_REACHED_DESTINATION = "reached_destination";
	private static final String C_AND_ARRIVE_INTERMEDIATE = "and_arrive_intermediate";
	private static final String C_REACHED_INTERMEDIATE = "reached_intermediate";
	private static final String C_AND_ARRIVE_WAYPOINT = "and_arrive_waypoint";
	private static final String C_AND_ARRIVE_FAVORITE = "and_arrive_favorite";
	private static final String C_AND_ARRIVE_POI_WAYPOINT = "and_arrive_poi";
	private static final String C_REACHED_WAYPOINT = "reached_waypoint";
	private static final String C_REACHED_FAVORITE = "reached_favorite";
	private static final String C_REACHED_POI = "reached_poi";
	private static final String C_THEN = "then";
	private static final String C_SPEAD_ALARM = "speed_alarm";
	private static final String C_ATTENTION = "attention";
	private static final String C_OFF_ROUTE = "off_route";
	private static final String C_BACK_ON_ROUTE = "back_on_route";
	private static final String C_TAKE_EXIT = "take_exit";

	private static final String C_BEAR_LEFT = "bear_left";
	private static final String C_BEAR_RIGHT = "bear_right";
	private static final String C_ROUTE_RECALC = "route_recalc";
	private static final String C_ROUTE_NEW_CALC = "route_new_calc";
	private static final String C_LOCATION_LOST = "location_lost";
	private static final String C_LOCATION_RECOVERED = "location_recovered";

	private final CommandPlayer commandPlayer;

	private Context jsContext;
	private ScriptableObject jsScope;

	private final List<String> listCommands = new ArrayList<>();
	private final List<String> listStruct = new ArrayList<>();

	public JsCommandBuilder(CommandPlayer commandPlayer) {
		this.commandPlayer = commandPlayer;
	}

	public void setJSContext(ScriptableObject jsScope) {
		jsContext = Context.enter();
		this.jsScope = jsScope;
	}

	public void setParameters(String metricCons, boolean tts) {
		Object mode = jsScope.get(SET_MODE, jsScope);
		Object metrics = jsScope.get(SET_METRIC_CONST, jsScope);
		callVoidJsFunction(mode, new Object[]{tts});
		callVoidJsFunction(metrics, new Object[]{metricCons});
	}

	private void callVoidJsFunction(Object function, Object[] params) {
		if (function instanceof Function) {
			Function jsFunction = (Function) function;
			jsFunction.call(jsContext, jsScope, jsScope, params);
		}
	}

	public List<String> getListCommands() {
		return listCommands;
	}

	public List<String> play() {
		return commandPlayer.playCommands(this);
	}

	public List<String> execute() {
		return listStruct;
	}

	public JsCommandBuilder goAhead() {
		return goAhead(-1, new StreetName());
	}

	public JsCommandBuilder goAhead(double dist, StreetName streetName) {
		return addCommand(C_GO_AHEAD, dist, convertStreetName(streetName));
	}

	public JsCommandBuilder makeUTwp() {
		return addCommand(C_MAKE_UTWP);
	}

	public JsCommandBuilder makeUT(StreetName streetName) {
		return makeUT(-1, streetName);
	}

	public JsCommandBuilder speedAlarm(int maxSpeed, float speed) {
		return addCommand(C_SPEAD_ALARM, maxSpeed, speed);
	}

	public JsCommandBuilder attention(String type) {
		return addCommand(C_ATTENTION, type);
	}

	public JsCommandBuilder offRoute(double dist) {
		return addCommand(C_OFF_ROUTE, dist);
	}

	public JsCommandBuilder backOnRoute() {
		return addCommand(C_BACK_ON_ROUTE);
	}

	public JsCommandBuilder makeUT(double dist, StreetName streetName) {
		return addCommand(C_MAKE_UT, dist, convertStreetName(streetName));
	}

	public JsCommandBuilder prepareMakeUT(double dist, StreetName streetName) {
		return addCommand(C_PREPARE_MAKE_UT, dist, convertStreetName(streetName));
	}

	public JsCommandBuilder turn(String param, StreetName streetName) {
		return turn(param, -1, streetName);
	}

	public JsCommandBuilder turn(String param, double dist, StreetName streetName) {
		return addCommand(C_TURN, param, dist, convertStreetName(streetName));
	}

	public JsCommandBuilder takeExit(String turnType, String exitString, int exitInt, StreetName streetName) {
		return takeExit(turnType, -1, exitString, exitInt, streetName);
	}

	public JsCommandBuilder takeExit(String turnType, double dist, String exitString, int exitInt, StreetName streetName) {
		return isJSCommandExists(C_TAKE_EXIT) ?
				addCommand(C_TAKE_EXIT, turnType, dist, exitString, exitInt, convertStreetName(streetName)) :
				addCommand(C_TURN, turnType, dist, convertStreetName(streetName));
	}

	/**
	 * @param turnType {@link net.osmand.plus.voice.BaseCommandPlayer#A_LEFT},
	 *                 {@link net.osmand.plus.voice.BaseCommandPlayer#A_LEFT},..
	 */
	public JsCommandBuilder prepareTurn(String turnType, double dist, StreetName streetName) {
		return addCommand(C_PREPARE_TURN, turnType, dist, convertStreetName(streetName));
	}

	public JsCommandBuilder prepareRoundAbout(double dist, int exit, StreetName streetName) {
		return addCommand(C_PREPARE_ROUNDABOUT, dist, exit, convertStreetName(streetName));
	}

	public JsCommandBuilder roundAbout(double dist, double angle, int exit, StreetName streetName) {
		return addCommand(C_ROUNDABOUT, dist, angle, exit, convertStreetName(streetName));
	}

	public JsCommandBuilder roundAbout(double angle, int exit, StreetName streetName) {
		return roundAbout(-1, angle, exit, streetName);
	}

	public JsCommandBuilder andArriveAtDestination(String name) {
		return addCommand(C_AND_ARRIVE_DESTINATION, name);
	}

	public JsCommandBuilder arrivedAtDestination(String name) {
		return addCommand(C_REACHED_DESTINATION, name);
	}

	public JsCommandBuilder andArriveAtIntermediatePoint(String name) {
		return addCommand(C_AND_ARRIVE_INTERMEDIATE, name);
	}

	public JsCommandBuilder arrivedAtIntermediatePoint(String name) {
		return addCommand(C_REACHED_INTERMEDIATE, name);
	}

	public JsCommandBuilder andArriveAtWayPoint(String name) {
		return addCommand(C_AND_ARRIVE_WAYPOINT, name);
	}

	public JsCommandBuilder arrivedAtWayPoint(String name) {
		return addCommand(C_REACHED_WAYPOINT, name);
	}

	public JsCommandBuilder andArriveAtFavorite(String name) {
		return addCommand(C_AND_ARRIVE_FAVORITE, name);
	}

	public JsCommandBuilder arrivedAtFavorite(String name) {
		return addCommand(C_REACHED_FAVORITE, name);
	}

	public JsCommandBuilder andArriveAtPoi(String name) {
		return addCommand(C_AND_ARRIVE_POI_WAYPOINT, name);
	}

	public JsCommandBuilder arrivedAtPoi(String name) {
		return addCommand(C_REACHED_POI, name);
	}

	public JsCommandBuilder bearLeft(StreetName streetName) {
		return addCommand(C_BEAR_LEFT, convertStreetName(streetName));
	}

	public JsCommandBuilder bearRight(StreetName streetName) {
		return addCommand(C_BEAR_RIGHT, convertStreetName(streetName));
	}

	public JsCommandBuilder then() {
		return addCommand(C_THEN);
	}

	public JsCommandBuilder gpsLocationLost() {
		return addCommand(C_LOCATION_LOST);
	}

	public JsCommandBuilder gpsLocationRecover() {
		return addCommand(C_LOCATION_RECOVERED);
	}

	public JsCommandBuilder newRouteCalculated(double dist, int time) {
		return addCommand(C_ROUTE_NEW_CALC, dist, time);
	}

	public JsCommandBuilder routeRecalculated(double dist, int time) {
		return addCommand(C_ROUTE_RECALC, dist, time);
	}

	private JsCommandBuilder addCommand(String name, Object... args) {
		addToCommandList(name, args);
		Object obj = jsScope.get(name);
		if (obj instanceof Function) {
			Function jsFunction = (Function) obj;
			Object jsResult = jsFunction.call(jsContext, jsScope, jsScope, args);
			listStruct.add(Context.toString(jsResult));
		}
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

	private Object convertStreetName(StreetName streetName) {
		String jsonText = new JSONObject(streetName.toMap()).toString();
		return NativeJSON.parse(jsContext, jsScope, jsonText, new NullCallable());
	}

	private boolean isJSCommandExists(String name) {
		return jsScope.get(name) instanceof Function;
	}

	private static class NullCallable implements Callable {

		@Override
		public Object call(Context context, Scriptable scope, Scriptable holdable, Object[] objects) {
			return objects[1];
		}
	}
}