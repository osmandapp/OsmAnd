package net.osmand.plus.voice;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;
//import org.liquidplayer.javascript.JSContext;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSCommandBuilder extends CommandBuilder {

    private static final Log log = PlatformUtil.getLog(JSCommandBuilder.class);

//    private JSContext jsContext;
    private List<String> listStruct = new ArrayList<>();

    public JSCommandBuilder(CommandPlayer commandPlayer) {
        super(commandPlayer);
//        jsContext = new JSContext();
    }

    public void setJSContext(String path) {
//        String script = null;
//        try {
//            byte[] encoded = Files.readAllBytes(Paths.get(path));
//            script = new String(encoded, "UTF-8");
//        } catch (IOException e) {
//            log.error(e.getMessage());
//        }
//        if (script != null)
////            jsContext.evaluateScript(script);
    }

    public void setParameters(String metricCons, boolean tts) {
        // TODO Set the parameters to js context
    }

    private JSCommandBuilder addCommand(String name, Object... args){
        //  TODO add JSCore
        listStruct.add(name);
        return this;
    }

    public JSCommandBuilder goAhead(){
        return goAhead(-1, new HashMap<String, String>());
    }

    public JSCommandBuilder goAhead(double dist, Map<String, String> streetName){
        return addCommand(C_GO_AHEAD, dist, streetName);
    }

    public JSCommandBuilder makeUTwp(){
        return makeUT(new HashMap<String, String>());
    }

    public JSCommandBuilder makeUT(Map<String, String> streetName){
        return addCommand(C_MAKE_UT, streetName);
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

    public JSCommandBuilder makeUT(double dist, Map<String,String> streetName){
        return addCommand(C_MAKE_UT, dist, streetName);
    }

    public JSCommandBuilder prepareMakeUT(double dist, Map<String, String> streetName){
        return addCommand(C_PREPARE_MAKE_UT, dist, streetName);
    }


    public JSCommandBuilder turn(String param, Map<String, String> streetName) {
        return addCommand(C_TURN, param, streetName);
    }

    public JSCommandBuilder turn(String param, double dist, Map<String, String> streetName){
        return addCommand(C_TURN, param, dist, streetName);
    }

    /**
     *
     * @param param A_LEFT, A_RIGHT, ...
     * @param dist
     * @return
     */
    public JSCommandBuilder prepareTurn(String param, double dist, Map<String, String> streetName){
        return addCommand(C_PREPARE_TURN, param, dist, streetName);
    }

    public JSCommandBuilder prepareRoundAbout(double dist, int exit, Map<String, String> streetName){
        return addCommand(C_PREPARE_ROUNDABOUT, dist, exit, streetName);
    }

    public JSCommandBuilder roundAbout(double dist, double angle, int exit, Map<String, String> streetName){
        return addCommand(C_ROUNDABOUT, dist, angle, exit, streetName);
    }

    public JSCommandBuilder roundAbout(double angle, int exit, Map<String, String> streetName) {
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

    public JSCommandBuilder bearLeft(Map<String,String> streetName){
        return addCommand(C_BEAR_LEFT, streetName);
    }

    public JSCommandBuilder bearRight(Map<String, String> streetName){
        return addCommand(C_BEAR_RIGHT, streetName);
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
    public void play(){
        this.commandPlayer.playCommands(this);
    }

    @Override
    protected List<String> execute(){
        alreadyExecuted = true;
        return listStruct;
    }
}
