package net.osmand.plus.smartnaviwatch;

import android.app.Activity;

import com.jwetherell.openmap.common.GreatCircle;
import com.jwetherell.openmap.common.LatLonPoint;

import net.osmand.Location;
import net.osmand.ValueHolder;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import ch.hsr.navigationmessagingapi.IMessageListener;
import ch.hsr.navigationmessagingapi.MapPolygon;
import ch.hsr.navigationmessagingapi.MapPolygonCollection;
import ch.hsr.navigationmessagingapi.MapPolygonTypes;
import ch.hsr.navigationmessagingapi.MessageDataKeys;
import ch.hsr.navigationmessagingapi.MessageTypes;
import ch.hsr.navigationmessagingapi.NavigationMessage;
import ch.hsr.navigationmessagingapi.PolygonPoint;
import ch.hsr.navigationmessagingapi.services.NavigationServiceConnector;

public class SmartNaviWatchPlugin extends OsmandPlugin implements IMessageListener{

    // Connections to the OsmAnd Application Framework
    private OsmandApplication application;
    private RoutingHelper routing;
    private OsmAndLocationProvider location;

    // Connection to the Messaging API
    private NavigationServiceConnector messageService;

    // Data with information about the current routing progress
    private RouteCalculationResult.NextDirectionInfo currentInfo;
    private Location lastKnownLocation;
    private boolean hasNotified = false;

    /**
     * Removes all data from the current navigation status
     */
    private void cleanRouteData() {
        currentInfo = null;
        hasNotified = false;
    }

    /**
     * Gets a connector to a navigation service that can send and receive messages
     * @return API connector
     */
    private NavigationServiceConnector getServiceConnector() {
        if (messageService == null)
        {
            messageService = new NavigationServiceConnector(application.getApplicationContext());
        }
        return messageService;
    }

    /**
     * Compares the two passed navigation steps and returns true if they are different
     * @param info1 First step, can be null
     * @param info2 Second step, can be null
     * @return boolean, that indicates if a difference was found
     */
    private boolean stepsAreDifferent(RouteCalculationResult.NextDirectionInfo info1, RouteCalculationResult.NextDirectionInfo info2)
    {
        // Difference by null
        if ((info1 != null && info2 == null) || info1 == null && info2 != null) return true;

        // Not different because of reference equality
        if (info1 == info2) return false;

        // If the other checks failed, check for route point offset
        return info1.directionInfo.routePointOffset != info2.directionInfo.routePointOffset;
    }

    /**
     * Checks for a necessity to update the user and the infos about the current step
     */
    private void updateNavigationSteps() {
        RouteCalculationResult.NextDirectionInfo n = routing.getNextRouteDirectionInfo(new RouteCalculationResult.NextDirectionInfo(), false);

        // Check if the next step changed
        if(stepsAreDifferent(currentInfo, n)) {
            currentInfo = n;
            hasNotified = false;
        }

        // Check distance to the current step, if smaller than a certain radius,
        // tell the user to execute the step
        if (currentInfo != null && lastKnownLocation != null) {
            Location p1 = routing.getRoute().getLocationFromRouteDirection(currentInfo.directionInfo);

            // Closer than 15m? Then notify the user.
            double delta = MapUtils.getDistance(p1.getLatitude(), p1.getLongitude(), lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            if (delta < 30 && !hasNotified) {
                HashMap<String, Object> msgData = createCurrentStepBundle(currentInfo.directionInfo);
                msgData.put(MessageDataKeys.MapPolygonData, createCurrentPositionMap());
                sendMessage(MessageTypes.NextStepMessage, msgData);
                hasNotified = true;
            }
        }
    }

    /**
     * Called when a message from the navigation message api is received
     * @param message Message passed from the API
     */
    @Override
    public void messageReceived(NavigationMessage message) {
        switch(message.getMessageType()) {
            case MessageTypes.PositionRequest:
                findLocationAndRespond();
                break;
            default:
                break;
        }
    }

    /**
     * Finds the current location of the user and gets an appropriate street name / location description
     */
    private void findLocationAndRespond() {
        boolean isNavigating = currentInfo!= null;
        HashMap<String, Object> msgData = isNavigating ? createCurrentStepBundle(currentInfo.directionInfo) : new HashMap<String, Object>();
        MapPolygonCollection map = createCurrentPositionMap();
        msgData.put(MessageDataKeys.MapPolygonData, map);
        msgData.put(MessageDataKeys.LocationName, map.getLocationName());
        msgData.put(MessageDataKeys.LocationAccuracy, lastKnownLocation.getAccuracy());
        sendMessage(isNavigating ? MessageTypes.NextStepMessage : MessageTypes.PositionMessage, msgData);
    }

    /**
     * Builds a map around the current position
     */
    private MapPolygonCollection createCurrentPositionMap() {
        // Read map and show it to the user
        BinaryMapIndexReader[] readers = application.getResourceManager().getRoutingMapFiles();

        MapPolygonCollection c = new MapPolygonCollection();

        if(readers.length > 0 && lastKnownLocation != null) {
            BinaryMapIndexReader reader = readers[0];

            // Calculate distance to the next navigation step
            double mapBorderSpacing = 100;
            double minVisibleRange = 100;
            double maxVisibleRange = 200;
            double distanceToNextPoint = 0;
            if (currentInfo != null) {
                Location p2 = routing.getRoute().getLocationFromRouteDirection(currentInfo.directionInfo);
                distanceToNextPoint = MapUtils.getDistance(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), p2.getLatitude(), p2.getLongitude());
            }
            double visibleRange = Math.min(maxVisibleRange, Math.max(minVisibleRange, distanceToNextPoint + mapBorderSpacing));

            BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> request = buildRequestAround(lastKnownLocation, visibleRange);

            List<BinaryMapDataObject> res = null;
            try {
                 res = reader.searchMapIndex(request);
            } catch(IOException ex) {}

            if (res != null){
                // Set the users position and the current view range
                c.setUserPosition(new PolygonPoint(MapUtils.get31TileNumberX(lastKnownLocation.getLongitude()), MapUtils.get31TileNumberY(lastKnownLocation.getLatitude())));
                c.setTopLeftViewRange(new PolygonPoint(request.getLeft(), request.getTop()));
                c.setBottomRightViewRange(new PolygonPoint(request.getRight(), request.getBottom()));

                //set the bearing only if the appropriate setting is set
                if (application.getSettings().ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING) {
                    // set only when speed is fast enough
                    if (lastKnownLocation.getSpeed() >= 0.5) {
                        c.setRotation(-lastKnownLocation.getBearing());
                    }
                }


                // Add all the objects on the map
                for(BinaryMapDataObject o : res) {
                    MapPolygon poly = polygonFromDataObject(o);
                    if (poly.getType() != MapPolygonTypes.UNKNOWN) c.add(poly);
                }

                // Add the current routing path if found
                if (currentInfo != null) {
                    List<Location> routePoints = routing.getRoute().getImmutableAllLocations();
                    PolygonPoint[] pointPath = new PolygonPoint[routePoints.size()];
                    PolygonPoint[][] emptyInner = new PolygonPoint[0][];

                    for(int rp = 0; rp < routePoints.size(); rp++){
                        Location l = routePoints.get(rp);
                        pointPath[rp] = new PolygonPoint(MapUtils.get31TileNumberX(l.getLongitude()), MapUtils.get31TileNumberY(l.getLatitude()));
                    }

                    c.add(new MapPolygon(MapPolygonTypes.ROUTE_PATH, pointPath, emptyInner));
                }

                Collections.sort(c.getPolygons());
                c.normalize();


            }
        }

        return c;
    }

    /**
     * Converts a MapDataObject into a polygon
     * @param obj    Map Data Object
     * @return       Polygon
     */
    private MapPolygon polygonFromDataObject(BinaryMapDataObject obj) {

        // convert points on the outside of the polygon
        PolygonPoint[] outsidePoints = new PolygonPoint[obj.getPointsLength()];
        for(int p = 0; p < outsidePoints.length; p++) {
            outsidePoints[p] = new PolygonPoint(obj.getPoint31XTile(p), obj.getPoint31YTile(p));
        }

        // convert inner polygons
        int[][] insidePointData = obj.getPolygonInnerCoordinates();
        PolygonPoint[][] insidePoints = new PolygonPoint[insidePointData.length][];

        // loop through list of inner polygons
        for(int poly = 0; poly < insidePointData.length; poly++) {
            insidePoints[poly] = new PolygonPoint[insidePointData[poly].length/2];

            // Loop through point coordinate array [x,y,x1,y2 ...]
            for(int innerPoint = 0, currentPoint = 0; innerPoint < insidePointData[poly].length; currentPoint++, innerPoint+=2) {
                insidePoints[poly][currentPoint] = new PolygonPoint(insidePointData[poly][innerPoint], insidePointData[poly][innerPoint + 1]);
            }
        }

        // Deduct type for rendering
        int[] typeCodes = obj.getTypes();
        MapPolygonTypes type = MapPolygonTypes.UNKNOWN;
        for(int t = 0; t < typeCodes.length && type == MapPolygonTypes.UNKNOWN; t++) {
            BinaryMapIndexReader.TagValuePair pair = obj.getMapIndex().decodeType(typeCodes[t]);
            type = deductType(pair.tag + "." + pair.value);
        }

        // Set name and return
        MapPolygon mapObj = new MapPolygon(type, outsidePoints, insidePoints);
        mapObj.setName(obj.getName());
        return mapObj;
    }

    private MapPolygonTypes deductType(String s) {
        MapPolygonTypes result = MapPolygonTypes.UNKNOWN;

        switch(s){
            case "highway.footway":
                result = MapPolygonTypes.ROAD_FOOTWAY;
                break;
            case "highway.residential":
                result = MapPolygonTypes.ROAD_RESIDENTIAL;
                break;
            case "highway.secondary":
                result = MapPolygonTypes.ROAD_SECONDARY;
                break;
            case "highway.tertiary":
                result = MapPolygonTypes.ROAD_TERTIARY;
                break;
            case "highway.motorway":
            case "highway.motorway_link":
                result = MapPolygonTypes.ROAD_MOTORWAY;
                break;
            case "highway.primary":
            case "highway.trunk":
                result = MapPolygonTypes.ROAD_PRIMARY;
                break;
            case "building.null":
            case "building.yes":
                result = MapPolygonTypes.BUILDING;
                break;
            case "natural.water":
                result = MapPolygonTypes.WATER;
            default:
                if (s.startsWith("highway.")) {
                    result = MapPolygonTypes.ROAD_DEFAULT;
                }
                else if (s.startsWith("building.")) {
                    result = MapPolygonTypes.BUILDING;
                }
                else if (s.startsWith("railway.")) {
                    result = MapPolygonTypes.RAILWAY;
                }
                break;
        }

        return result;
    }

    /**
     * Creates a SearchRequest that searches a square (side = 2*distanceInMeters) with it's center
     * aligned to the specified location
     * @param position Location of the center of the square
     * @param distanceInMeters Half of the length of the square
     * @return SearchRequest
     */
    private BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> buildRequestAround(Location position, double distanceInMeters) {
        // Calculate the upper left and lower right corners of the map part
        double arcDistance = distanceInMeters / 6371000d; // Winkel in rad = Bogenlänge / Radius
        LatLonPoint loc = new LatLonPoint(position.getLatitude(), position.getLongitude());
        LatLonPoint upperLeft = GreatCircle.sphericalBetween(loc.getRadLat(), loc.getRadLon(), arcDistance, -Math.PI / 4.0);
        LatLonPoint lowerRight = GreatCircle.sphericalBetween(loc.getRadLat(), loc.getRadLon(), arcDistance, Math.PI / 4.0 * 3.0);

        int leftX = MapUtils.get31TileNumberX(upperLeft.getLongitude());
        int rightX = MapUtils.get31TileNumberX(lowerRight.getLongitude());
        int topY = MapUtils.get31TileNumberY(upperLeft.getLatitude());
        int bottomY = MapUtils.get31TileNumberY(lowerRight.getLatitude());

        return BinaryMapIndexReader.buildSearchRequest(leftX, rightX, topY, bottomY, 17, null);
    }

    /**
     * Adapts to routing events (start and end of navigation)
     */
    private class RoutingAdapter implements RoutingHelper.IRouteInformationListener {

        /**
         * Adapts to routing events (start and end of navigation)
         * @param newRoute Indicates wether a completely new route or an alternate route to an existing
         *                 route was used.
         */
        @Override
        public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
            cleanRouteData();
            updateNavigationSteps();

            // Get the first instruction
            List<RouteDirectionInfo> directionInfos = routing.getRouteDirections();
            if (directionInfos.size() > 0) {
                RouteDirectionInfo firstStep = directionInfos.get(0);
                HashMap<String, Object> data = createCurrentStepBundle(firstStep);
                data.put(MessageDataKeys.MapPolygonData, createCurrentPositionMap());
                sendMessage(MessageTypes.NewRouteMessage, data);
            }
        }

        /**
         * Navigation has ended
         */
        @Override
        public void routeWasCancelled() {
            cleanRouteData();

            //sendMessage(MessageTypes.CancellationMessage, null);
        }
    }

    /**
     * Sends a new message via the connected API
     * @param messageType Type of the message
     * @param payload Content of the message
     */
    private void sendMessage(String messageType, Object payload) {
        NavigationMessage msg = NavigationMessage.create(messageType, payload);
        getServiceConnector().sendMessage(msg);
    }

    /**
     * Creates a HashMap of data from the route direction info
     * @param info Information about the next step
     * @return Bundle filled with the data
     */
    private HashMap<String, Object> createCurrentStepBundle(RouteDirectionInfo info) {
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put(MessageDataKeys.TurnType, info.getTurnType().toXmlString());
        m.put(MessageDataKeys.TurnAngle, info.getTurnType().getTurnAngle());
        m.put(MessageDataKeys.Distance, info.distance); // evtl. via currentInfo.distanceTo
        m.put(MessageDataKeys.RoutingDescription, info.getDescriptionRoute(application));
        m.put(MessageDataKeys.RouteLeftTime, routing.getRoute().getLeftTime(lastKnownLocation));
        m.put(MessageDataKeys.LocationAccuracy, lastKnownLocation.getAccuracy());
        return m;
    }

    /**
     * Adapts to the location service from OsmAnd. Receives updates when the users location
     * changes.
     */
    private class LocationAdapter implements OsmAndLocationProvider.OsmAndLocationListener{
        /**
         * New location is known. Store it and check if updates should be sent to the user
         * @param location Location info
         */
        @Override
        public void updateLocation(Location location) {
            if (locationChangeIsSignificant(lastKnownLocation, location) && location != null) {
                lastKnownLocation = location;
                updateNavigationSteps();
            }
        }
    }

    /**
     * Checks wether the two locations differ significantly from each other or not
     * @param lastKnownLocation Location 1
     * @param location Location 2
     * @return True if significant change found
     */
    private boolean locationChangeIsSignificant(Location lastKnownLocation, Location location) {
        if (lastKnownLocation == null && location != null) return true;
        if (lastKnownLocation != null && location == null) return true;
        if (lastKnownLocation == null && location == null) return false;

        double delta = MapUtils.getDistance(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), location.getLatitude(), location.getLongitude());

        return delta >= 1;
    }

    /**
     * Creates a new instance of SmartNaviWatchPlugin
     * @param app OsmAnd application this plugin runs in
     */
    public SmartNaviWatchPlugin(OsmandApplication app)
    {
        application = app;
    }

    /**
     * Initializes the plugin and creates references to the OsmAnd and NavigationAPI services
     * @param app OsmAnd application this plugin runs in
     * @param activity Activity that initialized the plugin
     * @return Initialization success
     */
    @Override
    public boolean init(OsmandApplication app, Activity activity) {
        // Listen for new routes and cancellation
        routing = app.getRoutingHelper();
        routing.addListener(new RoutingAdapter());

        // Listen for location updates
        location = app.getLocationProvider();
        location.addLocationListener(new LocationAdapter());

        // Listen to messages
        getServiceConnector().addMessageListener(this);

        return true;
    }

    /**
     * Gets the activity that should be started for additional settings. Not Used.
     * @return null
     */
    @Override
    public Class<? extends Activity> getSettingsActivity() {
        return null;
    }

    /**
     * Gets the plugin id
     * @return Plugin id
     */
    @Override
    public String getId() {
        return "ch.hsr.smart-navi-watch";
    }

    /**
     * Gets the plugin description
     * @return Plugin description
     */
    @Override
    public String getDescription() {
        return "This plugin sends data to the corresponding SmartNaviWatch-App on your connected Android Wear device. It enables you to see your current location and navigation infos directly on your wrist.";
    }

    /**
     * Gets the display name of the plugin
     * @return Plugin display name
     */
    @Override
    public String getName() {
        return "Smart Navi Watch";
    }

    /**
     * Gets the asset name for the icon
     * @return Icon asset name
     */
    @Override
    public int getAssetResourceName() {
        return R.drawable.parking_position;
    }
}
