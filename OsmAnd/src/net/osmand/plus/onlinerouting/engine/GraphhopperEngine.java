package net.osmand.plus.onlinerouting.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.OnlineRoutingResponse;
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

import static net.osmand.util.Algorithms.isEmpty;

public class GraphhopperEngine extends OnlineRoutingEngine {

	public GraphhopperEngine(@Nullable Map<String, String> params) {
		super(params);
	}

	@Override
	public @NonNull EngineType getType() {
		return EngineType.GRAPHHOPPER;
	}

	@NonNull
	@Override
	public String getStandardUrl() {
		return "https://graphhopper.com/api/1/route";
	}

	@Override
	protected void collectAllowedParameters() {
		allowParameters(EngineParameter.API_KEY);
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
	protected void makeFullUrl(@NonNull StringBuilder sb,
	                           @NonNull List<LatLon> path) {
		sb.append("?");
		for (LatLon point : path) {
			sb.append("point=")
					.append(point.getLatitude())
					.append(',')
					.append(point.getLongitude())
					.append('&');
		}
		String vehicle = get(EngineParameter.VEHICLE_KEY);
		if (!isEmpty(vehicle)) {
			sb.append("vehicle=").append(vehicle);
		}
		String apiKey = get(EngineParameter.API_KEY);
		if (!isEmpty(apiKey)) {
			sb.append('&').append("key=").append(apiKey);
		}
		sb.append('&').append("details=").append("lanes");
	}

	@NonNull
	@Override
	public OnlineRoutingResponse parseServerResponse(@NonNull String content,
	                                                 boolean leftSideNavigation) throws JSONException {
		JSONObject obj = new JSONObject(content);
		JSONObject root = obj.getJSONArray("paths").getJSONObject(0);

		String encoded = root.getString("points");
		List<LatLon> route = GeoPolylineParserUtil.parse(encoded, GeoPolylineParserUtil.PRECISION_5);

		JSONArray instructions = root.getJSONArray("instructions");
		List<RouteDirectionInfo> directions = new ArrayList<>();
		for (int i = 0; i < instructions.length(); i++) {
			JSONObject item = instructions.getJSONObject(i);
			int sign = Integer.parseInt(item.getString("sign"));
			int distance = (int) Math.round(Double.parseDouble(item.getString("distance")));
			String description = item.getString("text");
			String streetName = item.getString("street_name");
			int timeInSeconds = (int) Math.round(Integer.parseInt(item.getString("time")) / 1000f);
			JSONArray interval = item.getJSONArray("interval");
			int startPointOffset = interval.getInt(0);
			int endPointOffset = interval.getInt(1);

			float averageSpeed = (float) distance / timeInSeconds;
			TurnType turnType = identifyTurnType(sign, leftSideNavigation);
			// TODO turnType.setTurnAngle()

			RouteDirectionInfo direction = new RouteDirectionInfo(averageSpeed, turnType);
			direction.routePointOffset = startPointOffset;
			if (turnType != null && turnType.isRoundAbout()) {
				direction.routeEndPointOffset = endPointOffset;
			}
			direction.setDescriptionRoute(description);
			direction.setStreetName(streetName);
			direction.setDistance(distance);
			directions.add(direction);
		}
		return new OnlineRoutingResponse(route, directions);
	}

	@Override
	public boolean parseServerMessage(@NonNull StringBuilder sb,
	                                  @NonNull String content) throws JSONException {
		JSONObject obj = new JSONObject(content);
		if (obj.has("message")) {
			String message = obj.getString("message");
			sb.append(message);
		}
		return obj.has("paths");
	}

	/**
	 * @param sign - a number which specifies the turn type to show (Graphhopper API value)
	 * @return a TurnType object defined in OsmAnd which is equivalent to a value from the Graphhopper API
	 *
	 * For future compatibility it is important that all clients
	 * are able to handle also unknown instruction sign numbers
	 */
	@Nullable
	public static TurnType identifyTurnType(int sign, boolean leftSide) {
		int id = INVALID_ID;

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

		return id != INVALID_ID ? TurnType.valueOf(id, leftSide) : null;
	}
}
