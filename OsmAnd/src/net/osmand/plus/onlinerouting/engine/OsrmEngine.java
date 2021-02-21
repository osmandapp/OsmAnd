package net.osmand.plus.onlinerouting.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.VehicleType;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.util.Algorithms.isEmpty;

public class OsrmEngine extends OnlineRoutingEngine {

	public OsrmEngine(@Nullable Map<String, String> params) {
		super(params);
	}

	@Override
	public @NonNull
	EngineType getType() {
		return EngineType.OSRM;
	}

	@NonNull
	@Override
	public String getStandardUrl() {
		return "https://router.project-osrm.org/route/v1/";
	}

	@Override
	protected void collectAllowedParameters(@NonNull Set<EngineParameter> params) {
		params.add(EngineParameter.KEY);
		params.add(EngineParameter.VEHICLE_KEY);
		params.add(EngineParameter.CUSTOM_NAME);
		params.add(EngineParameter.NAME_INDEX);
		params.add(EngineParameter.CUSTOM_URL);
	}

	@Override
	protected void collectAllowedVehicles(@NonNull List<VehicleType> vehicles) {
		vehicles.add(new VehicleType("car", R.string.routing_engine_vehicle_type_car));
		vehicles.add(new VehicleType("bike", R.string.routing_engine_vehicle_type_bike));
		vehicles.add(new VehicleType("foot", R.string.routing_engine_vehicle_type_foot));
	}

	@Override
	protected void makeFullUrl(@NonNull StringBuilder sb,
	                           @NonNull List<LatLon> path) {
		String vehicleKey = get(EngineParameter.VEHICLE_KEY);
		if (!isEmpty(vehicleKey)) {
			sb.append(vehicleKey).append('/');
		}
		for (int i = 0; i < path.size(); i++) {
			LatLon point = path.get(i);
			sb.append(point.getLongitude()).append(',').append(point.getLatitude());
			if (i < path.size() - 1) {
				sb.append(';');
			}
		}
		sb.append('?');
		sb.append("overview=full");
		sb.append('&').append("steps=true");
	}

}
