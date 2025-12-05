package net.osmand.plus.mapcontextmenu.other;

import androidx.annotation.Nullable;

import net.osmand.data.LatLon;

import java.util.HashMap;

public class SharePoiParams {
	private final HashMap<String, String> params = new HashMap<>();
	public String frag;

	public SharePoiParams(LatLon latLon) {
		addPin(latLon);
		frag = 15 + "/" + latLon.getLatitude() + "/" + latLon.getLongitude();
	}

	public void addName(@Nullable String name) {
		params.put("name", name);
	}

	@Nullable
	public String getName() {
		return params.get("name");
	}

	public void addType(@Nullable String type) {
		if (type != null) {
			params.put("type", type);
		}
	}

	public void addWikidataId(@Nullable String wikidataId) {
		if (wikidataId != null) {
			params.put("wikidataId", wikidataId.startsWith("Q") ? wikidataId.substring(1) : wikidataId);
		}
	}

	public void addOsmId(@Nullable Long osmId) {
		if (osmId != null) {
			params.put("osmId", osmId.toString());
		}
	}

	public void addPin(@Nullable LatLon latLon) {
		if (latLon != null) {
			String pin = latLon.getLatitude() + "," + latLon.getLongitude();
			params.put("pin", pin);
		}
	}

	public HashMap<String, String> getParams() {
		return params;
	}
}
