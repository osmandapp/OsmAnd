package net.osmand.plus.voice;

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

import java.util.List;

public class JsCommandBuilder extends CommandBuilder{

	private static final Log log = PlatformUtil.getLog(JsCommandBuilder.class);

	private Context jsContext;
	private ScriptableObject jsScope;

	public JsCommandBuilder(CommandPlayer commandPlayer) {
		super(commandPlayer);
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

	@Override
	public List<String> play() {
		return commandPlayer.playCommands(this);
	}

	@Override
	public List<String> execute() {
		return listStruct;
	}

	@Override
	public CommandBuilder goAhead() {
		return goAhead(-1, new StreetName());
	}

	@Override
	public CommandBuilder goAhead(double dist, StreetName streetName) {
		return addCommand(C_GO_AHEAD, dist, convertStreetName(streetName));
	}

	@Override
	public CommandBuilder makeUTwp() {
		return addCommand(C_MAKE_UTWP);
	}

	@Override
	public CommandBuilder makeUT(StreetName streetName) {
		return makeUT(-1, streetName);
	}

	@Override
	public CommandBuilder speedAlarm(int maxSpeed, float speed) {
		return addCommand(C_SPEAD_ALARM, maxSpeed, speed);
	}

	@Override
	public CommandBuilder attention(String type) {
		return addCommand(C_ATTENTION, type);
	}

	@Override
	public CommandBuilder offRoute(double dist) {
		return addCommand(C_OFF_ROUTE, dist);
	}

	@Override
	public CommandBuilder backOnRoute() {
		return addCommand(C_BACK_ON_ROUTE);
	}

	@Override
	public CommandBuilder makeUT(double dist, StreetName streetName) {
		return addCommand(C_MAKE_UT, dist, convertStreetName(streetName));
	}

	@Override
	public CommandBuilder prepareMakeUT(double dist, StreetName streetName) {
		return addCommand(C_PREPARE_MAKE_UT, dist, convertStreetName(streetName));
	}

	@Override
	public CommandBuilder turn(String param, StreetName streetName) {
		return turn(param, -1, streetName);
	}

	@Override
	public CommandBuilder turn(String param, double dist, StreetName streetName) {
		return addCommand(C_TURN, param, dist, convertStreetName(streetName));
	}

	@Override
	public CommandBuilder takeExit(String turnType, String exitString, int exitInt, StreetName streetName) {
		return takeExit(turnType, -1, exitString, exitInt, streetName);
	}

	@Override
	public CommandBuilder takeExit(String turnType, double dist, String exitString, int exitInt, StreetName streetName) {
		return isJsCommandExists(C_TAKE_EXIT)
				? addCommand(C_TAKE_EXIT, turnType, dist, exitString, exitInt, convertStreetName(streetName))
				: addCommand(C_TURN, turnType, dist, convertStreetName(streetName));
	}

	@Override
	public CommandBuilder prepareTurn(String turnType, double dist, StreetName streetName) {
		return addCommand(C_PREPARE_TURN, turnType, dist, convertStreetName(streetName));
	}

	@Override
	public CommandBuilder prepareRoundAbout(double dist, int exit, StreetName streetName) {
		return addCommand(C_PREPARE_ROUNDABOUT, dist, exit, convertStreetName(streetName));
	}

	@Override
	public CommandBuilder roundAbout(double dist, double angle, int exit, StreetName streetName) {
		return addCommand(C_ROUNDABOUT, dist, angle, exit, convertStreetName(streetName));
	}

	@Override
	public CommandBuilder roundAbout(double angle, int exit, StreetName streetName) {
		return roundAbout(-1, angle, exit, streetName);
	}

	@Override
	public CommandBuilder andArriveAtDestination(String name) {
		return addCommand(C_AND_ARRIVE_DESTINATION, name);
	}

	@Override
	public CommandBuilder arrivedAtDestination(String name) {
		return addCommand(C_REACHED_DESTINATION, name);
	}

	@Override
	public CommandBuilder andArriveAtIntermediatePoint(String name) {
		return addCommand(C_AND_ARRIVE_INTERMEDIATE, name);
	}

	@Override
	public CommandBuilder arrivedAtIntermediatePoint(String name) {
		return addCommand(C_REACHED_INTERMEDIATE, name);
	}

	@Override
	public CommandBuilder andArriveAtWayPoint(String name) {
		return addCommand(C_AND_ARRIVE_WAYPOINT, name);
	}

	@Override
	public CommandBuilder arrivedAtWayPoint(String name) {
		return addCommand(C_REACHED_WAYPOINT, name);
	}

	@Override
	public CommandBuilder andArriveAtFavorite(String name) {
		return addCommand(C_AND_ARRIVE_FAVORITE, name);
	}

	@Override
	public CommandBuilder arrivedAtFavorite(String name) {
		return addCommand(C_REACHED_FAVORITE, name);
	}

	@Override
	public CommandBuilder andArriveAtPoi(String name) {
		return addCommand(C_AND_ARRIVE_POI_WAYPOINT, name);
	}

	@Override
	public CommandBuilder arrivedAtPoi(String name) {
		return addCommand(C_REACHED_POI, name);
	}

	@Override
	public CommandBuilder bearLeft(StreetName streetName) {
		return addCommand(C_BEAR_LEFT, convertStreetName(streetName));
	}

	@Override
	public CommandBuilder bearRight(StreetName streetName) {
		return addCommand(C_BEAR_RIGHT, convertStreetName(streetName));
	}

	@Override
	public CommandBuilder then() {
		return addCommand(C_THEN);
	}

	@Override
	public CommandBuilder gpsLocationLost() {
		return addCommand(C_LOCATION_LOST);
	}

	@Override
	public CommandBuilder gpsLocationRecover() {
		return addCommand(C_LOCATION_RECOVERED);
	}

	@Override
	public CommandBuilder newRouteCalculated(double dist, int time) {
		return addCommand(C_ROUTE_NEW_CALC, dist, time);
	}

	@Override
	public CommandBuilder routeRecalculated(double dist, int time) {
		return addCommand(C_ROUTE_RECALC, dist, time);
	}

	@Override
	protected CommandBuilder addCommand(String name, Object... args) {
		super.addCommand(name, args);
		Object obj = jsScope.get(name);
		if (obj instanceof Function) {
			Function jsFunction = (Function) obj;
			Object jsResult = jsFunction.call(jsContext, jsScope, jsScope, args);
			listStruct.add(Context.toString(jsResult));
		}
		return this;
	}

	private Object convertStreetName(StreetName streetName) {
		String jsonText = new JSONObject(streetName.toMap()).toString();
		return NativeJSON.parse(jsContext, jsScope, jsonText, new NullCallable());
	}

	private boolean isJsCommandExists(String name) {
		return jsScope.get(name) instanceof Function;
	}

	private static class NullCallable implements Callable {

		@Override
		public Object call(Context context, Scriptable scope, Scriptable holdable, Object[] objects) {
			return objects[1];
		}
	}
}