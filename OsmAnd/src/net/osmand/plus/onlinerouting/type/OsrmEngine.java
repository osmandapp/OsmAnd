package net.osmand.plus.onlinerouting.type;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.onlinerouting.OnlineRoutingEngine;
import net.osmand.util.GeoPolylineParserUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class OsrmEngine extends EngineType {

	@Override
	public String getStringKey() {
		return "osrm";
	}

	@Override
	public String getTitle() {
		return "OSRM";
	}

	@Override
	public String getStandardUrl() {
		return "https://router.project-osrm.org/route/v1/";
	}

	@Override
	protected void createFullUrl(StringBuilder sb,
	                             OnlineRoutingEngine engine,
	                             List<LatLon> path) {
		sb.append(engine.getVehicleKey()).append('/');
		for (int i = 0; i < path.size(); i++) {
			LatLon point = path.get(i);
			sb.append(point.getLongitude()).append(',').append(point.getLatitude());
			if (i < path.size() - 1) {
				sb.append(';');
			}
		}
	}

	@Override
	protected List<LatLon> parseResponse(@NonNull JSONObject obj, @NonNull String content) throws JSONException {
		return GeoPolylineParserUtil.parse(
				obj.getJSONArray("routes").getJSONObject(0).getString("geometry"),
				GeoPolylineParserUtil.PRECISION_5);
	}

}
