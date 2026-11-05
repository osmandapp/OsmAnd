package net.osmand.aidl;

import net.osmand.aidl.map.ALatLon;
import net.osmand.aidl.maplayer.point.AMapPoint;
import net.osmand.data.LatLon;

import java.util.List;
import java.util.Map;

public class AidlMapPointWrapper {

	private final String id;
	private final String shortName;
	private final String fullName;
	private final String typeName;
	private final String layerId;
	private final int color;
	private final LatLon location;
	private final List<String> details;
	private final Map<String, String> params;

	public AidlMapPointWrapper(AMapPoint aMapPoint) {
		id = aMapPoint.getId();
		shortName = aMapPoint.getShortName();
		fullName = aMapPoint.getFullName();
		typeName = aMapPoint.getTypeName();
		layerId = aMapPoint.getLayerId();
		color = aMapPoint.getColor();

		ALatLon aLatLon = aMapPoint.getLocation();
		location = new LatLon(aLatLon.getLatitude(), aLatLon.getLongitude());
		details = aMapPoint.getDetails();
		params = aMapPoint.getParams();
	}

	public AidlMapPointWrapper(net.osmand.aidlapi.maplayer.point.AMapPoint aMapPoint) {
		id = aMapPoint.getId();
		shortName = aMapPoint.getShortName();
		fullName = aMapPoint.getFullName();
		typeName = aMapPoint.getTypeName();
		layerId = aMapPoint.getLayerId();
		color = aMapPoint.getColor();

		net.osmand.aidlapi.map.ALatLon aLatLon = aMapPoint.getLocation();
		location = new LatLon(aLatLon.getLatitude(), aLatLon.getLongitude());
		details = aMapPoint.getDetails();
		params = aMapPoint.getParams();
	}

	public String getId() {
		return id;
	}

	public String getShortName() {
		return shortName;
	}

	public String getFullName() {
		return fullName;
	}

	public String getTypeName() {
		return typeName;
	}

	public String getLayerId() {
		return layerId;
	}

	public int getColor() {
		return color;
	}

	public LatLon getLocation() {
		return location;
	}

	public List<String> getDetails() {
		return details;
	}

	public Map<String, String> getParams() {
		return params;
	}
}