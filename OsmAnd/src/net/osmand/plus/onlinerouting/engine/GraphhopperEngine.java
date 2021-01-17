package net.osmand.plus.onlinerouting.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.VehicleType;
import net.osmand.util.GeoPolylineParserUtil;

import org.json.JSONException;
import org.json.JSONObject;

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
		if (isEmpty(vehicle)) {
			sb.append("vehicle=").append(vehicle);
		}
		String apiKey = get(EngineParameter.API_KEY);
		if (isEmpty(apiKey)) {
			sb.append('&').append("key=").append(apiKey);
		}
	}

	@NonNull
	@Override
	public List<LatLon> parseServerResponse(@NonNull String content) throws JSONException {
		JSONObject obj = new JSONObject(content);
		return GeoPolylineParserUtil.parse(
				obj.getJSONArray("paths").getJSONObject(0).getString("points"),
				GeoPolylineParserUtil.PRECISION_5);
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
}
