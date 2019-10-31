package net.osmand.aidl;

import net.osmand.aidl.map.ALatLon;
import net.osmand.aidl.maplayer.point.AMapPoint;
import net.osmand.data.LatLon;

import java.util.List;
import java.util.Map;

public class AidlMapPointWrapper {

	private String id;
	private String shortName;
	private String fullName;
	private String typeName;
	private String layerId;
	private int color;
	private LatLon location;
	private List<String> details;
	private Map<String, String> params;

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