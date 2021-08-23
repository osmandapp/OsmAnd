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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
		return "Openroute Service";
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
	protected void makeFullUrl(@NonNull StringBuilder sb,
	                           @NonNull List<LatLon> path) {
		if (path.size() > 1) {
			String vehicleKey = getVehicleKeyForUrl();
			if (!isEmpty(vehicleKey)) {
				sb.append(vehicleKey);
			}
			sb.append('?');
			String apiKey = get(EngineParameter.API_KEY);
			if (!isEmpty(apiKey)) {
				sb.append("api_key=").append(apiKey);
			}
			LatLon start = path.get(0);
			LatLon end = path.get(path.size() - 1);
			sb.append('&').append("start=")
					.append(start.getLongitude()).append(',').append(start.getLatitude());
			sb.append('&').append("end=")
					.append(end.getLongitude()).append(',').append(end.getLatitude());
		}
	}

	@Nullable
	@Override
	public OnlineRoutingResponse parseServerResponse(@NonNull JSONObject root,
	                                                 @NonNull OsmandApplication app,
	                                                 boolean leftSideNavigation) throws JSONException {
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
				JSONObject step = steps.getJSONObject(j);
				double distance = step.getDouble("distance");
				double duration = step.getDouble("duration");
				String instruction = step.getString("instruction");
				TurnType turnType = getTurnType(step.getInt("type"));
				String streetName = step.getString("name");
				float averageSpeed = (float) (distance / duration);

				RouteDirectionInfo direction = new RouteDirectionInfo(averageSpeed, turnType);
				direction.setDescriptionRoute(instruction);
				direction.setStreetName(streetName);
				direction.setDistance((int) distance);
				directions.add(direction);
			}
		}

		if (!isEmpty(points)) {
			List<Location> route = convertRouteToLocationsList(points);
			return new OnlineRoutingResponse(route, directions);
		}
		return null;
	}

	@NonNull
	private TurnType getTurnType(int orsTurnIdx) {
		boolean leftSide = false; // TODO: change
		// go straight per default
		String turnTypeStr = "C";
		OrsInstructionType orsInstructionType = OrsInstructionType.values()[orsTurnIdx];
	    switch (orsInstructionType) {
			case TURN_LEFT:
				turnTypeStr = "TL";
				break;
			case TURN_RIGHT:
				turnTypeStr = "TR";
			    break;
			case TURN_SHARP_LEFT:
				turnTypeStr = "TSHL";
				break;
			case TURN_SHARP_RIGHT:
				turnTypeStr = "TSHR";
				break;
			case TURN_SLIGHT_LEFT:
				turnTypeStr = "TSLL";
				break;
			case TURN_SLIGHT_RIGHT:
				turnTypeStr = "TSLR";
				break;
			case CONTINUE:
				turnTypeStr = "C";
				break;
			case ENTER_ROUNDABOUT:
				turnTypeStr = "RNDB";
				break;
			case EXIT_ROUNDABOUT:
			    turnTypeStr = leftSide ? "TL" : "TR";
				break;
			case UTURN:
			    turnTypeStr = "TU";
				break;
			case FINISH:
			    // not supported -> default
				break;
			case DEPART:
			    turnTypeStr = leftSide ? "KL" : "KR";
				break;
			case KEEP_LEFT:
				turnTypeStr = "KL";
				break;
			case KEEP_RIGHT:
				turnTypeStr = "KR";
				break;
			case UNKNOWN:
			default:
				// go straight per default
				break;
		}
		return TurnType.fromString(turnTypeStr, leftSide);
	}

	// taken from https://github.com/giscience/openrouteservice
	private enum OrsInstructionType
	{
		TURN_LEFT,              /*0*/
		TURN_RIGHT,             /*1*/
		TURN_SHARP_LEFT,        /*2*/
		TURN_SHARP_RIGHT,       /*3*/
		TURN_SLIGHT_LEFT,       /*4*/
		TURN_SLIGHT_RIGHT,      /*5*/
		CONTINUE,               /*6*/
		ENTER_ROUNDABOUT,       /*7*/
		EXIT_ROUNDABOUT,        /*8*/
		UTURN,                  /*9*/
		FINISH,                 /*10*/
		DEPART,                 /*11*/
		KEEP_LEFT,              /*12*/
		KEEP_RIGHT,             /*13*/
		UNKNOWN                 /*14*/;

		public boolean isSlightLeftOrRight() {
			return this == TURN_SLIGHT_RIGHT || this == TURN_SLIGHT_LEFT;
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
