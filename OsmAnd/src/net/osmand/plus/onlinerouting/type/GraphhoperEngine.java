package net.osmand.plus.onlinerouting.type;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.onlinerouting.OnlineRoutingEngine;
import net.osmand.plus.onlinerouting.OnlineRoutingEngine.EngineParameter;
import net.osmand.util.Algorithms;
import net.osmand.util.GeoPolylineParserUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class GraphhoperEngine extends EngineType {

	@Override
	public String getStringKey() {
		return "graphhoper";
	}

	@Override
	public String getTitle() {
		return "Graphhoper";
	}

	@Override
	public String getStandardUrl() {
		return "https://graphhopper.com/api/1/route";
	}

	@Override
	protected void createFullUrl(StringBuilder sb,
	                             OnlineRoutingEngine engine,
	                             List<LatLon> path) {
		sb.append("?");
		for (LatLon point : path) {
			sb.append("point=")
					.append(point.getLatitude())
					.append(',')
					.append(point.getLongitude())
					.append('&');
		}
		sb.append("vehicle=").append(engine.getVehicleKey());

		String apiKey = engine.getParameter(EngineParameter.API_KEY);
		if (!Algorithms.isEmpty(apiKey)) {
			sb.append('&').append("key=").append(apiKey);
		}
	}

	@Override
	protected List<LatLon> parseResponse(JSONObject obj, @NonNull String content) throws JSONException {
		return GeoPolylineParserUtil.parse(
				obj.getJSONArray("routes").getJSONObject(0).getString("geometry"),
				GeoPolylineParserUtil.PRECISION_5);
	}

}
