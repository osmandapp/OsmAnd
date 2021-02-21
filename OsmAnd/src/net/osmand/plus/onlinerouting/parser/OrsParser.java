package net.osmand.plus.onlinerouting.parser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.util.Algorithms.isEmpty;

public class OrsParser extends JSONParser {

	@Nullable
	@Override
	public OnlineRoutingResponse parseServerResponse(@NonNull JSONObject root,
	                                                 @NonNull OsmandApplication app,
	                                                 boolean leftSideNavigation) throws JSONException {
		JSONArray array = root.getJSONObject("geometry").getJSONArray("coordinates");
		List<LatLon> points = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			JSONArray point = array.getJSONArray(i);
			double lon = Double.parseDouble(point.getString(0));
			double lat = Double.parseDouble(point.getString(1));
			points.add(new LatLon(lat, lon));
		}
		if (!isEmpty(points)) {
			List<Location> route = convertRouteToLocationsList(points);
			new OnlineRoutingResponse(route, null);
		}
		return null;
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
