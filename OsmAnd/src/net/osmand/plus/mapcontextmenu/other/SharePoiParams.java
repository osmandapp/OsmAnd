package net.osmand.plus.mapcontextmenu.other;

import android.util.Pair;

import androidx.annotation.Nullable;

import net.osmand.LocationConvert;
import net.osmand.data.LatLon;
import net.osmand.util.Algorithms;

import java.util.HashMap;

public class SharePoiParams {
	private final HashMap<String, String> params = new HashMap<>();
	public String frag;

	public SharePoiParams(LatLon latLon) {
		addPin(latLon);
		Pair<String, String> formattedLatLon = getFormattedShareLatLon(latLon);
		frag = 15 + "/" + formattedLatLon.first + "/" + formattedLatLon.second;
	}

	public void addName(@Nullable String name) {
		params.put("name", name);
	}

	@Nullable
	public String getName() {
		return params.get("name");
	}

	public void addType(@Nullable String type) {
		if (!Algorithms.isEmpty(type)) {
			params.put("type", type);
		}
	}

	public void addWikidataId(@Nullable String wikidataId) {
		if (!Algorithms.isEmpty(wikidataId)) {
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
			Pair<String, String> formattedLatLon = getFormattedShareLatLon(latLon);
			String pin = formattedLatLon.first + "," + formattedLatLon.second;
			params.put("pin", pin);
		}
	}

	public static Pair<String, String> getFormattedShareLatLon(LatLon latLon){
		String formattedLat = LocationConvert.convertLatitude(latLon.getLatitude(), LocationConvert.FORMAT_DEGREES, false);
		String formattedlon = LocationConvert.convertLongitude(latLon.getLongitude(), LocationConvert.FORMAT_DEGREES, false);
		formattedLat = formattedLat.substring(0, formattedLat.length() - 1);
		formattedlon = formattedlon.substring(0, formattedlon.length() - 1);

		return new Pair<>(formattedLat, formattedlon);
	}

	public HashMap<String, String> getParams() {
		return params;
	}
}
