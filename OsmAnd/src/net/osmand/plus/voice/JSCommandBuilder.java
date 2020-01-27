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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JSCommandBuilder extends CommandBuilder {

    private static final Log log = PlatformUtil.getLog(JSCommandBuilder.class);
    private static final String SET_MODE = "setMode";
    private static final String SET_METRIC_CONST = "setMetricConst";

    private Context jsContext;
    private List<String> listStruct = new ArrayList<>();
    private ScriptableObject jsScope;

    JSCommandBuilder(CommandPlayer commandPlayer) {
        super(commandPlayer);
    }

    public void setJSContext(ScriptableObject jsScope) {
        jsContext = Context.enter();
        this.jsScope = jsScope;
    }

    private Object convertStreetName(StreetName streetName) {
        return NativeJSON.parse(jsContext, jsScope, new JSONObject(streetName.toMap()).toString(),
                new NullCallable());
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

    private JSCommandBuilder addCommand(String name, Object... args){
        addToCommandList(name, args);
	    Object obj = jsScope.get(name);
        if (obj instanceof Function) {
            Function jsFunction = (Function) obj;
            Object jsResult = jsFunction.call(jsContext, jsScope, jsScope, args);
            listStruct.add(Context.toString(jsResult));
        }
        return this;
    }

    private boolean isJSCommandExists(String name) {
        return jsScope.get(name) instanceof Function;
    }

    public JSCommandBuilder goAhead(){
        return goAhead(-1, new StreetName());
    }

    public JSCommandBuilder goAhead(double dist, StreetName streetName){
        return addCommand(C_GO_AHEAD, dist, convertStreetName(streetName));
    }

    @Override
    public JSCommandBuilder makeUTwp(){
        return makeUT(new StreetName());
    }

    public JSCommandBuilder makeUT(StreetName streetName){
        return makeUT(-1, streetName);
    }
    @Override
    public JSCommandBuilder speedAlarm(int maxSpeed, float speed){
        return addCommand(C_SPEAD_ALARM, maxSpeed, speed);
    }
    @Override
    public JSCommandBuilder attention(String type){
        return addCommand(C_ATTENTION, type);
    }
    @Override
    public JSCommandBuilder offRoute(double dist){
        return addCommand(C_OFF_ROUTE, dist);
    }
    @Override
    public CommandBuilder backOnRoute(){
        return addCommand(C_BACK_ON_ROUTE);
    }

    public JSCommandBuilder makeUT(double dist, StreetName streetName){
        return addCommand(C_MAKE_UT, dist, convertStreetName(streetName));
    }

    public JSCommandBuilder prepareMakeUT(double dist, StreetName streetName){
        return addCommand(C_PREPARE_MAKE_UT, dist, convertStreetName(streetName));
    }


    public JSCommandBuilder turn(String param, StreetName streetName) {
        return turn(param, -1, streetName);
    }

    public JSCommandBuilder turn(String param, double dist, StreetName streetName) {
        return addCommand(C_TURN, param, dist, convertStreetName(streetName));
    }

    public JSCommandBuilder takeExit(String turnType, String exitString, int exitInt, StreetName streetName) {
        return takeExit(turnType, -1, exitString, exitInt, streetName);
    }

    public JSCommandBuilder takeExit(String turnType, double dist, String exitString, int exitInt, StreetName streetName) {
        return isJSCommandExists(C_TAKE_EXIT) ?
                addCommand(C_TAKE_EXIT, turnType, dist, exitString, exitInt, convertStreetName(streetName)) :
                addCommand(C_TURN, turnType, dist, convertStreetName(streetName));
    }

    /**
     *
     * @param param A_LEFT, A_RIGHT, ...
     * @param dist
     * @return
     */
    public JSCommandBuilder prepareTurn(String param, double dist, StreetName streetName){
        return addCommand(C_PREPARE_TURN, param, dist, convertStreetName(streetName));
    }

    public JSCommandBuilder prepareRoundAbout(double dist, int exit, StreetName streetName){
        return addCommand(C_PREPARE_ROUNDABOUT, dist, exit, convertStreetName(streetName));
    }

    public JSCommandBuilder roundAbout(double dist, double angle, int exit, StreetName streetName){
        return addCommand(C_ROUNDABOUT, dist, angle, exit, convertStreetName(streetName));
    }

    public JSCommandBuilder roundAbout(double angle, int exit, StreetName streetName) {
        return roundAbout(-1, angle, exit, streetName);
    }
    @Override
    public JSCommandBuilder andArriveAtDestination(String name){
        return addCommand(C_AND_ARRIVE_DESTINATION, name);
    }
    @Override
    public JSCommandBuilder arrivedAtDestination(String name){
        return addCommand(C_REACHED_DESTINATION, name);
    }
    @Override
    public JSCommandBuilder andArriveAtIntermediatePoint(String name){
        return addCommand(C_AND_ARRIVE_INTERMEDIATE, name);
    }
    @Override
    public JSCommandBuilder arrivedAtIntermediatePoint(String name) {
        return addCommand(C_REACHED_INTERMEDIATE, name);
    }
    @Override
    public JSCommandBuilder andArriveAtWayPoint(String name){
        return addCommand(C_AND_ARRIVE_WAYPOINT, name);
    }
    @Override
    public JSCommandBuilder arrivedAtWayPoint(String name) {
        return addCommand(C_REACHED_WAYPOINT, name);
    }
    @Override
    public JSCommandBuilder andArriveAtFavorite(String name) {
        return addCommand(C_AND_ARRIVE_FAVORITE, name);
    }
    @Override
    public JSCommandBuilder arrivedAtFavorite(String name) {
        return addCommand(C_REACHED_FAVORITE, name);
    }
    @Override
    public JSCommandBuilder andArriveAtPoi(String name) {
        return addCommand(C_AND_ARRIVE_POI_WAYPOINT, name);
    }
    @Override
    public JSCommandBuilder arrivedAtPoi(String name) {
        return addCommand(C_REACHED_POI, name);
    }

    public JSCommandBuilder bearLeft(StreetName streetName){
        return addCommand(C_BEAR_LEFT, convertStreetName(streetName));
    }

    public JSCommandBuilder bearRight(StreetName streetName){
        return addCommand(C_BEAR_RIGHT, convertStreetName(streetName));
    }

    @Override
    public JSCommandBuilder then(){
        return addCommand(C_THEN);
    }

    @Override
    public JSCommandBuilder gpsLocationLost() {
        return addCommand(C_LOCATION_LOST);
    }

    @Override
    public JSCommandBuilder gpsLocationRecover() {
        return addCommand(C_LOCATION_RECOVERED);
    }

    @Override
    public JSCommandBuilder newRouteCalculated(double dist, int time){
        return addCommand(C_ROUTE_NEW_CALC, dist, time);
    }

    @Override
    public JSCommandBuilder routeRecalculated(double dist, int time){
        return addCommand(C_ROUTE_RECALC, dist, time);
    }

    @Override
    public List<String> play(){
        return this.commandPlayer.playCommands(this);
    }

    @Override
    protected List<String> execute(){
        alreadyExecuted = true;
        return listStruct;
    }

    public class NullCallable implements Callable
    {
        @Override
        public Object call(Context context, Scriptable scope, Scriptable holdable, Object[] objects)
        {
            return objects[1];
        }
    }
}
