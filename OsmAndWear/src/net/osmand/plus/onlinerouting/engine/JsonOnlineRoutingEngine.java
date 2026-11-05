package net.osmand.plus.onlinerouting.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.router.RouteCalculationProgress;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.osmand.util.Algorithms.isEmpty;

public abstract class JsonOnlineRoutingEngine extends OnlineRoutingEngine {

	public JsonOnlineRoutingEngine(@Nullable Map<String, String> params) {
		super(params);
	}

	@Nullable
	public OnlineRoutingResponse responseByContent(@NonNull OsmandApplication app, @NonNull String content,
	                                               boolean leftSideNavigation, boolean initialCalculation,
	                                               @Nullable RouteCalculationProgress calculationProgress) throws JSONException {
		JSONObject root = parseRootResponseObject(content);
		return root != null ? parseServerResponse(root, app, leftSideNavigation) : null;
	}

	@Nullable
	protected JSONObject parseRootResponseObject(@NonNull String content) throws JSONException {
		JSONObject fullJSON = new JSONObject(content);
		String key = getRootArrayKey();
		JSONArray array = null;
		if (fullJSON.has(key)) {
			array = fullJSON.getJSONArray(key);
		}
		return array != null && array.length() > 0 ? array.getJSONObject(0) : null;
	}

	public boolean isResultOk(@NonNull StringBuilder errorMessage,
	                          @NonNull String content) throws JSONException {
		JSONObject obj = new JSONObject(content);
		String messageKey = getErrorMessageKey();
		if (obj.has(messageKey)) {
			String message = obj.getString(messageKey);
			errorMessage.append(message);
		}
		return obj.has(getRootArrayKey());
	}

	@Nullable
	protected abstract OnlineRoutingResponse parseServerResponse(@NonNull JSONObject root,
	                                                             @NonNull OsmandApplication app,
	                                                             boolean leftSideNavigation) throws JSONException;

	@NonNull
	protected abstract String getRootArrayKey();

	@NonNull
	protected abstract String getErrorMessageKey();

	@NonNull
	protected static List<Location> convertRouteToLocationsList(@NonNull List<LatLon> route) {
		List<Location> result = new ArrayList<>();
		if (!isEmpty(route)) {
			for (LatLon pt : route) {
				WptPt wpt = new WptPt();
				wpt.setLat(pt.getLatitude());
				wpt.setLon(pt.getLongitude());
				result.add(RouteProvider.createLocation(wpt));
			}
		}
		return result;
	}
}
