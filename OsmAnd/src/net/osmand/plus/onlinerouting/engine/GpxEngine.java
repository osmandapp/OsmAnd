package net.osmand.plus.onlinerouting.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.VehicleType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GpxEngine extends OnlineRoutingEngine {

	public GpxEngine(@Nullable Map<String, String> params) {
		super(params);
	}

	@NonNull
	@Override
	public EngineType getType() {
		return EngineType.GPX;
	}

	@Override
	protected void makeFullUrl(@NonNull StringBuilder sb,
	                           @NonNull List<LatLon> path) {
		for (int i = 0; i < path.size(); i++) {
			LatLon point = path.get(i);
			sb.append(point.getLongitude()).append(',').append(point.getLatitude());
			if (i < path.size() - 1) {
				sb.append(';');
			}
		}
	}

	@NonNull
	@Override
	public String getStandardUrl() {
		return ""; // TODO will be determined in the future
	}

	@Override
	protected void collectAllowedVehicles(@NonNull List<VehicleType> vehicles) {

	}

	@Override
	protected void collectAllowedParameters(@NonNull Set<EngineParameter> params) {
		params.add(EngineParameter.KEY);
		params.add(EngineParameter.CUSTOM_NAME);
		params.add(EngineParameter.NAME_INDEX);
		params.add(EngineParameter.CUSTOM_URL);
	}

}
