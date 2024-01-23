package net.osmand.plus.onlinerouting.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.VehicleType;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.router.TurnType;
import net.osmand.util.GeoPolylineParserUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.plus.onlinerouting.engine.EngineType.GRAPHHOPPER_TYPE;
import static net.osmand.util.Algorithms.isEmpty;

public class GraphhopperEngine extends JsonOnlineRoutingEngine {

	public GraphhopperEngine(@Nullable Map<String, String> params) {
		super(params);
	}

	@NonNull
	@Override
	public OnlineRoutingEngine getType() {
		return GRAPHHOPPER_TYPE;
	}

	@Override
	@NonNull
	public String getTitle() {
		return "Graphhopper";
	}

	@NonNull
	@Override
	public String getTypeName() {
		return "GRAPHHOPPER";
	}

	@NonNull
	@Override
	public String getStandardUrl() {
		return "https://graphhopper.com/api/1/route";
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
		return new GraphhopperEngine(params);
	}

	@Override
	protected void collectAllowedVehicles(@NonNull List<VehicleType> vehicles) {
		vehicles.add(new VehicleType("car", R.string.routing_engine_vehicle_type_car));
		vehicles.add(new VehicleType("bike", R.string.routing_engine_vehicle_type_bike));
		vehicles.add(new VehicleType("foot", R.string.routing_engine_vehicle_type_foot));
		vehicles.add(new VehicleType("hike", R.string.routing_engine_vehicle_type_hiking));
		vehicles.add(new VehicleType("mtb", R.string.routing_engine_vehicle_type_mtb));
		vehicles.add(new VehicleType("racingbike", R.string.routing_engine_vehicle_type_racingbike));
		vehicles.add(new VehicleType("scooter", R.string.routing_engine_vehicle_type_scooter));
		vehicles.add(new VehicleType("truck", R.string.routing_engine_vehicle_type_truck));
		vehicles.add(new VehicleType("small_truck", R.string.routing_engine_vehicle_type_small_truck));
	}

	@Override
	protected void makeFullUrl(@NonNull StringBuilder sb, @NonNull List<LatLon> path, @Nullable Float startBearing) {
		sb.append("?");
		for (int i = 0; i < path.size(); i++) {
			LatLon point = path.get(i);
			sb.append("point=")
					.append(point.getLatitude())
					.append(',')
					.append(point.getLongitude());
			if (i < path.size() - 1) {
				sb.append('&');
			}
		}
		String vehicle = getVehicleKeyForUrl();
		if (!isEmpty(vehicle)) {
			if (!isCustomParameterizedVehicle()) {
				sb.append('&').append("profile=");
			}
			sb.append(vehicle);
		}
		String apiKey = get(EngineParameter.API_KEY);
		if (!isEmpty(apiKey)) {
			sb.append('&').append("key=").append(apiKey);
		}
	}

	@Nullable
	protected OnlineRoutingResponse parseServerResponse(@NonNull JSONObject root,
	                                                    @NonNull OsmandApplication app,
	                                                    boolean leftSideNavigation) throws JSONException {
		String encoded = root.getString("points");
		List<LatLon> points = GeoPolylineParserUtil.parse(encoded, GeoPolylineParserUtil.PRECISION_5);
		if (isEmpty(points)) return null;
		List<Location> route = convertRouteToLocationsList(points);

		JSONArray instructions = root.getJSONArray("instructions");
		List<RouteDirectionInfo> directions = new ArrayList<>();
		for (int i = 0; i < instructions.length(); i++) {
			JSONObject instruction = instructions.getJSONObject(i);
			int distance = (int) Math.round(instruction.getDouble("distance"));
			String description = instruction.getString("text");
			String streetName = instruction.getString("street_name");
			int timeInSeconds = Math.round(instruction.getInt("time") / 1000f);
			JSONArray interval = instruction.getJSONArray("interval");
			int startPointOffset = interval.getInt(0);
			int endPointOffset = interval.getInt(1);

			float averageSpeed = (float) distance / timeInSeconds;
			TurnType turnType = parseTurnType(instruction, leftSideNavigation);
			RouteDirectionInfo direction = new RouteDirectionInfo(averageSpeed, turnType);

			direction.routePointOffset = startPointOffset;
			direction.setDescriptionRoute(description);
			direction.setStreetName(streetName);
			direction.setDistance(distance);
			directions.add(direction);
		}
		return new OnlineRoutingResponse(route, directions);
	}

	@NonNull
	private TurnType parseTurnType(@NonNull JSONObject instruction,
	                               boolean leftSide) throws JSONException {
		int sign = instruction.getInt("sign");
		TurnType turnType = identifyTurnType(sign, leftSide);

		if (turnType == null) {
			turnType = TurnType.straight();
		} else if (turnType.isRoundAbout()) {
			if (instruction.has("exit_number")) {
				int exit = instruction.getInt("exit_number");
				turnType.setExitOut(exit);
			}
			if (instruction.has("turn_angle")) {
				float angle = (float) instruction.getDouble("turn_angle");
				turnType.setTurnAngle(angle);
			}
		} else {
			// TODO turnType.setTurnAngle()
		}

		return turnType;
	}

	@Nullable
	public static TurnType identifyTurnType(int sign, boolean leftSide) {
		Integer id = null;

		if (sign == -98) {
			// an U-turn without the knowledge
			// if it is a right or left U-turn
			id = TurnType.TU;

		} else if (sign == -8) {
			// a left U-turn
			leftSide = false;
			id = TurnType.TU;

		} else if (sign == -7) {
			// keep left
			id = TurnType.KL;

		} else if (sign == -6) {
			// not yet used: leave roundabout

		} else if (sign == -3) {
			// turn sharp left
			id = TurnType.TSHL;

		} else if (sign == -2) {
			// turn left
			id = TurnType.TL;

		} else if (sign == -1) {
			// turn slight left
			id = TurnType.TSLL;

		} else if (sign == 0) {
			// continue on street
			id = TurnType.C;

		} else if (sign == 1) {
			// turn slight right
			id = TurnType.TSLR;

		} else if (sign == 2) {
			// turn right
			id = TurnType.TR;

		} else if (sign == 3) {
			// turn sharp right
			id = TurnType.TSHR;

		} else if (sign == 4) {
			// the finish instruction before the last point
			id = TurnType.C;

		} else if (sign == 5) {
			// the instruction before a via point

		} else if (sign == 6) {
			// the instruction before entering a roundabout
			id = TurnType.RNDB;

		} else if (sign == 7) {
			// keep right
			id = TurnType.KR;

		} else if (sign == 8) {
			// a right U-turn
			id = TurnType.TRU;
		}

		return id != null ? TurnType.valueOf(id, leftSide) : null;
	}

	@NonNull
	@Override
	protected String getErrorMessageKey() {
		return "message";
	}

	@NonNull
	@Override
	protected String getRootArrayKey() {
		return "paths";
	}

}
