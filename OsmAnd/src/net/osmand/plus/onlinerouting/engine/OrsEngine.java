package net.osmand.plus.onlinerouting.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.VehicleType;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.TurnType;
import net.osmand.shared.gpx.GpxFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import static net.osmand.plus.onlinerouting.engine.EngineType.ORS_TYPE;
import static net.osmand.util.Algorithms.isEmpty;

public class OrsEngine extends JsonOnlineRoutingEngine {

	public OrsEngine(@Nullable Map<String, String> params) {
		super(params);
	}

	@NonNull
	@Override
	public OnlineRoutingEngine getType() {
		return ORS_TYPE;
	}

	@Override
	@NonNull
	public String getTitle() {
		return "openrouteservice";
	}

	@NonNull
	@Override
	public String getTypeName() {
		return "ORS";
	}

	@NonNull
	@Override
	public String getStandardUrl() {
		return "https://api.openrouteservice.org/v2/directions/";
	}

	@NonNull
	@Override
	public String getHTTPMethod() {
		return "POST";
	}

	@NonNull
	@Override
	public Map<String, String> getRequestHeaders() {
		Map<String, String> headers = new HashMap<>();
		String apiKey = get(EngineParameter.API_KEY);
		if (!isEmpty(apiKey)) {
			headers.put("Authorization", apiKey);
		}
		headers.put("Content-Type", "application/json");
		return headers;
	}

	@Override
	protected void collectAllowedParameters(@NonNull Set<EngineParameter> params) {
		params.add(EngineParameter.KEY);
		params.add(EngineParameter.VEHICLE_KEY);
		params.add(EngineParameter.CUSTOM_NAME);
		params.add(EngineParameter.NAME_INDEX);
		params.add(EngineParameter.CUSTOM_URL);
		params.add(EngineParameter.API_KEY);
	}

	@Override
	public OnlineRoutingEngine newInstance(Map<String, String> params) {
		return new OrsEngine(params);
	}

	@Override
	protected void collectAllowedVehicles(@NonNull List<VehicleType> vehicles) {
		vehicles.add(new VehicleType("driving-car", R.string.routing_engine_vehicle_type_car));
		vehicles.add(new VehicleType("driving-hgv", R.string.routing_engine_vehicle_type_hgv));
		vehicles.add(new VehicleType("cycling-regular", R.string.routing_engine_vehicle_type_cycling_regular));
		vehicles.add(new VehicleType("cycling-road", R.string.routing_engine_vehicle_type_cycling_road));
		vehicles.add(new VehicleType("cycling-mountain", R.string.routing_engine_vehicle_type_cycling_mountain));
		vehicles.add(new VehicleType("cycling-electric", R.string.routing_engine_vehicle_type_cycling_electric));
		vehicles.add(new VehicleType("foot-walking", R.string.routing_engine_vehicle_type_walking));
		vehicles.add(new VehicleType("foot-hiking", R.string.routing_engine_vehicle_type_hiking));
		vehicles.add(new VehicleType("wheelchair", R.string.routing_engine_vehicle_type_wheelchair));
	}

	@Override
	protected void makeFullUrl(@NonNull StringBuilder sb, @NonNull List<LatLon> path,
							   @Nullable Float startBearing) {
		// add ORS routing profile (= vehicle key)
		String vehicleKey = getVehicleKeyForUrl();
		if (!isEmpty(vehicleKey)) {
			sb.append(vehicleKey);
		}
		sb.append("/geojson");
	}

	@NonNull
	@Override
	public String getRequestBody(@NonNull List<LatLon> path, @Nullable Float startBearing)
			throws JSONException {
		JSONObject jsonBody = new JSONObject();
		if (path.size() > 1) {
			JSONArray coordinates = new JSONArray();
			for (LatLon p : path) {
				coordinates.put(new JSONArray(Arrays.asList(p.getLongitude(), p.getLatitude())));
			}
			jsonBody.put("coordinates", coordinates);
		}
		return jsonBody.toString();
	}

	@Override
	public OnlineRoutingResponse responseByGpxFile(@NonNull OsmandApplication app, @NonNull GpxFile gpxFile, boolean initialCalculation, @Nullable RouteCalculationProgress calculationProgress) {
		return null;
	}

	@Nullable
	@Override
	public OnlineRoutingResponse parseServerResponse(@NonNull JSONObject root,
	                                                 @NonNull OsmandApplication app,
	                                                 boolean leftSideNavigation)
			throws JSONException {
		JSONArray coordinates = root.getJSONObject("geometry").getJSONArray("coordinates");
		List<LatLon> points = new ArrayList<>();
		for (int i = 0; i < coordinates.length(); i++) {
			JSONArray point = coordinates.getJSONArray(i);
			double lon = Double.parseDouble(point.getString(0));
			double lat = Double.parseDouble(point.getString(1));
			points.add(new LatLon(lat, lon));
		}

		List<RouteDirectionInfo> directions = new ArrayList<>();
		JSONArray segments = root.getJSONObject("properties").getJSONArray("segments");
		for (int i = 0; i < segments.length(); i++) {
			JSONArray steps = segments.getJSONObject(i).getJSONArray("steps");
			for (int j = 0; j < steps.length(); j++) {
				// parse JSON values
				JSONObject step = steps.getJSONObject(j);
				double distance = step.getDouble("distance");
				double duration = step.getDouble("duration");
				String instruction = step.getString("instruction");
				TurnType turnType = getTurnType(step.getInt("type"), leftSideNavigation);
				String streetName = step.getString("name");
				JSONArray wayPoints = step.getJSONArray("way_points");
				int routePointOffset = wayPoints.getInt(0);
				int routeEndPointOffset = wayPoints.getInt(1);
				float averageSpeed = (float) (distance / duration);

				// create direction step
				RouteDirectionInfo direction = new RouteDirectionInfo(averageSpeed, turnType);
				direction.routePointOffset = routePointOffset;
				direction.setDescriptionRoute(instruction);
				direction.setStreetName(streetName);
				direction.setDistance((int) Math.round(distance));
				directions.add(direction);
			}
		}

		if (!isEmpty(points)) {
			List<Location> route = convertRouteToLocationsList(points);
			return new OnlineRoutingResponse(route, directions);
		}
		return null;
	}

	/**
	 * This method takes an ORS instruction type integer and maps it to an OsmAnd TurnType object.
	 * source: https://github.com/GIScience/openrouteservice/blob/master/openrouteservice/src/main/java/org/heigit/ors/routing/instructions/InstructionType.java
	 * documentation: https://giscience.github.io/openrouteservice/documentation/Instruction-Types.html
	 */
	@NonNull
	private TurnType getTurnType(int orsInstructionType, boolean leftSide) {
		switch (orsInstructionType) {
			case 0: // TURN_LEFT
				return TurnType.fromString("TL", leftSide);
			case 1: // TURN_RIGHT
				return TurnType.fromString("TR", leftSide);
			case 2: // TURN_SHARP_LEFT
				return TurnType.fromString("TSHL", leftSide);
			case 3: // TURN_SHARP_RIGHT
				return TurnType.fromString("TSHR", leftSide);
			case 4: // TURN_SLIGHT_LEFT
				return TurnType.fromString("TSLL", leftSide);
			case 5: // TURN_SLIGHT_RIGHT
				return TurnType.fromString("TSLR", leftSide);
			case 6: // CONTINUE
				return TurnType.fromString("C", leftSide);
			case 7: // ENTER_ROUNDABOUT
				return TurnType.fromString("RNDB", leftSide);
			case 8: // EXIT_ROUNDABOUT
				return TurnType.fromString(leftSide ? "TL" : "TR", leftSide);
			case 9: // UTURN
				return TurnType.fromString("TU", leftSide);
			case 11: // DEPART
				return TurnType.fromString(leftSide ? "KL" : "KR", leftSide);
			case 12: // KEEP_LEFT
				return TurnType.fromString("KL", leftSide);
			case 13: // KEEP_RIGHT
				return TurnType.fromString("KR", leftSide);
			case 10: // FINISH
			case 14: // UNKNOWN
			default:
				// CONTINUE per default
				return TurnType.fromString("C", leftSide);
		}
	}

	@NonNull
	@Override
	protected String getErrorMessageKey() {
		return "error";
	}

	@NonNull
	@Override
	protected String getRootArrayKey() {
		return "features";
	}

}
