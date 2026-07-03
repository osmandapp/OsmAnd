package net.osmand.plus.onlinerouting.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.VehicleType;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.TurnType;
import net.osmand.shared.gpx.GpxFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.plus.onlinerouting.engine.EngineType.TOMTOM_TYPE;
import static net.osmand.util.Algorithms.isEmpty;

public class TomTomEngine extends JsonOnlineRoutingEngine {

	public TomTomEngine(@Nullable Map<String, String> params) {
		super(params);
	}

	@NonNull
	@Override
	public OnlineRoutingEngine getType() {
		return TOMTOM_TYPE;
	}

	@NonNull
	@Override
	public String getTitle() {
		return "TomTom";
	}

	@NonNull
	@Override
	public String getTypeName() {
		return "TOMTOM";
	}

	@NonNull
	@Override
	public String getStandardUrl() {
		return "https://api.tomtom.com/routing/1/calculateRoute/";
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
	protected void collectAllowedVehicles(@NonNull List<VehicleType> vehicles) {
		vehicles.add(new VehicleType("car", R.string.routing_engine_vehicle_type_car));
		vehicles.add(new VehicleType("truck", R.string.routing_engine_vehicle_type_hgv));
		vehicles.add(new VehicleType("bicycle", R.string.routing_engine_vehicle_type_bike));
		vehicles.add(new VehicleType("pedestrian", R.string.routing_engine_vehicle_type_foot));
	}

	@Override
	public OnlineRoutingEngine newInstance(Map<String, String> params) {
		return new TomTomEngine(params);
	}

	@Override
	public OnlineRoutingResponse responseByGpxFile(@NonNull OsmandApplication app, @NonNull GpxFile gpxFile,
	                                               boolean initialCalculation, @Nullable RouteCalculationProgress calculationProgress) {
		return null;
	}

	@Override
	protected void makeFullUrl(@NonNull StringBuilder sb, @NonNull List<LatLon> path, @Nullable Float startBearing) {
		for (int i = 0; i < path.size(); i++) {
			LatLon point = path.get(i);
			sb.append(point.getLatitude()).append(',').append(point.getLongitude());
			if (i < path.size() - 1) {
				sb.append(':');
			}
		}
		sb.append("/json");
		sb.append("?key=").append(getApiKey());
		sb.append("&traffic=true");
		sb.append("&routeType=fastest");
		sb.append("&instructionsType=text");
		String travelMode = getVehicleKeyForUrl();
		if (!isEmpty(travelMode)) {
			sb.append("&travelMode=").append(travelMode);
		}
	}

	@NonNull
	private String getApiKey() {
		String apiKey = get(EngineParameter.API_KEY);
		return apiKey != null ? apiKey : "";
	}

	@Nullable
	@Override
	protected OnlineRoutingResponse parseServerResponse(@NonNull JSONObject root,
	                                                    @NonNull OsmandApplication app,
	                                                    boolean leftSideNavigation) throws JSONException {
		List<LatLon> points = new ArrayList<>();
		JSONArray legs = root.getJSONArray("legs");
		for (int i = 0; i < legs.length(); i++) {
			JSONArray legPoints = legs.getJSONObject(i).getJSONArray("points");
			for (int j = 0; j < legPoints.length(); j++) {
				JSONObject point = legPoints.getJSONObject(j);
				points.add(new LatLon(point.getDouble("latitude"), point.getDouble("longitude")));
			}
		}
		if (isEmpty(points)) {
			return null;
		}
		List<Location> route = convertRouteToLocationsList(points);
		return new OnlineRoutingResponse(route, parseDirections(root, app, leftSideNavigation));
	}

	@NonNull
	private List<RouteDirectionInfo> parseDirections(@NonNull JSONObject root, @NonNull OsmandApplication app,
	                                                 boolean leftSideNavigation) throws JSONException {
		List<RouteDirectionInfo> directions = new ArrayList<>();
		JSONObject guidance = root.optJSONObject("guidance");
		if (guidance == null) {
			return directions;
		}
		JSONArray instructions = guidance.getJSONArray("instructions");
		for (int i = 0; i < instructions.length(); i++) {
			JSONObject instruction = instructions.getJSONObject(i);
			int offset = instruction.getInt("routeOffsetInMeters");
			int time = instruction.getInt("travelTimeInSeconds");
			boolean last = i == instructions.length() - 1;
			int distance = last ? 0 : instructions.getJSONObject(i + 1).getInt("routeOffsetInMeters") - offset;
			int duration = last ? 0 : instructions.getJSONObject(i + 1).getInt("travelTimeInSeconds") - time;

			String maneuver = instruction.getString("maneuver");
			TurnType turnType = identifyTurnType(maneuver, instruction, leftSideNavigation);
			RouteDirectionInfo direction = new RouteDirectionInfo(duration > 0 ? (float) distance / duration : 1f, turnType);
			direction.setDistance(distance);
			direction.routePointOffset = instruction.getInt("pointIndex");

			String street = instruction.optString("street", "");
			direction.setStreetName(street);
			boolean terminal = "DEPART".equals(maneuver) || maneuver.startsWith("ARRIVE");
			direction.setDescriptionRoute(terminal ? "" : (RouteCalculationResult.toString(turnType, app, false) + " " + street).trim());
			directions.add(direction);
		}
		return directions;
	}

	@NonNull
	private TurnType identifyTurnType(@NonNull String maneuver, @NonNull JSONObject instruction, boolean leftSide) {
		switch (maneuver) {
			case "TURN_LEFT":
				return TurnType.fromString("TL", leftSide);
			case "TURN_RIGHT":
				return TurnType.fromString("TR", leftSide);
			case "SHARP_LEFT":
				return TurnType.fromString("TSHL", leftSide);
			case "SHARP_RIGHT":
				return TurnType.fromString("TSHR", leftSide);
			case "BEAR_LEFT":
				return TurnType.fromString("TSLL", leftSide);
			case "BEAR_RIGHT":
				return TurnType.fromString("TSLR", leftSide);
			case "KEEP_LEFT":
			case "MOTORWAY_EXIT_LEFT":
				return TurnType.fromString("KL", leftSide);
			case "KEEP_RIGHT":
			case "MOTORWAY_EXIT_RIGHT":
			case "TAKE_EXIT":
				return TurnType.fromString("KR", leftSide);
			case "MAKE_UTURN":
			case "TRY_MAKE_UTURN":
				return TurnType.fromString("TU", leftSide);
			case "ROUNDABOUT_LEFT":
			case "ROUNDABOUT_RIGHT":
			case "ROUNDABOUT_CROSS":
			case "ROUNDABOUT_BACK":
				int exit = instruction.optInt("roundaboutExitNumber", 0);
				return exit > 0 ? TurnType.getExitTurn(exit, 0f, leftSide) : TurnType.fromString("RNDB", leftSide);
			default:
				return TurnType.fromString("C", leftSide);
		}
	}

	@NonNull
	@Override
	protected String getRootArrayKey() {
		return "routes";
	}

	@NonNull
	@Override
	protected String getErrorMessageKey() {
		return "message";
	}
}
