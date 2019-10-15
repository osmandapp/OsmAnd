package net.osmand.aidlapi.maplayer.point;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;
import net.osmand.aidlapi.map.ALatLon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AMapPoint extends AidlParams {

	public static final String POINT_IMAGE_URI_PARAM = "point_image_uri_param";
	public static final String POINT_SPEED_PARAM = "point_speed_param";
	public static final String POINT_TYPE_ICON_NAME_PARAM = "point_type_icon_name_param";
	public static final String POINT_STALE_LOC_PARAM = "point_stale_loc_param";
	public static final String POINT_BEARING_PARAM = "point_bearing_param";

	private String id;
	private String shortName;
	private String fullName;
	private String typeName;
	private String layerId;
	private int color;
	private ALatLon location;
	private ArrayList<String> details = new ArrayList<>();
	private HashMap<String, String> params = new HashMap<>();

	public AMapPoint(String id, String shortName, String fullName, String typeName, String layerId,
	                 int color, ALatLon location, List<String> details, Map<String, String> params) {
		this.id = id;
		this.shortName = shortName;
		this.fullName = fullName;
		this.typeName = typeName;
		this.layerId = layerId;
		this.color = color;
		this.location = location;
		if (details != null) {
			this.details.addAll(details);
		}
		if (params != null) {
			this.params.putAll(params);
		}
	}

	public AMapPoint(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AMapPoint> CREATOR = new Creator<AMapPoint>() {
		@Override
		public AMapPoint createFromParcel(Parcel in) {
			return new AMapPoint(in);
		}

		@Override
		public AMapPoint[] newArray(int size) {
			return new AMapPoint[size];
		}
	};

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

	public ALatLon getLocation() {
		return location;
	}

	public List<String> getDetails() {
		return details;
	}

	public Map<String, String> getParams() {
		return params;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("id", id);
		bundle.putString("shortName", shortName);
		bundle.putString("fullName", fullName);
		bundle.putString("typeName", typeName);
		bundle.putInt("color", color);
		bundle.putParcelable("location", location);
		bundle.putStringArrayList("details", details);
		bundle.putSerializable("params", params);
		bundle.putString("layerId", layerId);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(ALatLon.class.getClassLoader());

		id = bundle.getString("id");
		shortName = bundle.getString("shortName");
		fullName = bundle.getString("fullName");
		typeName = bundle.getString("typeName");
		color = bundle.getInt("color");
		location = bundle.getParcelable("location");
		details = bundle.getStringArrayList("details");
		params = (HashMap<String, String>) bundle.getSerializable("params");
		layerId = bundle.getString("layerId");
	}
}