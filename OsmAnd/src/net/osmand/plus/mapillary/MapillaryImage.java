package net.osmand.plus.mapillary;

import net.osmand.util.Algorithms;

import java.util.Map;

public class MapillaryImage {

	// Image location
	private double latitude;
	private double longitude;
	// Camera heading.  -1 if not found.
	private double ca = -1;
	// When the image was captured, expressed as UTC epoch time in milliseconds. Must be non-negative integer;  0 if not found.
	private long capturedAt;
	// Image key.
	private String key;
	// Whether the image is panorama ( 1 ), or not ( 0 ).
	private boolean pano;
	// Sequence key.
	private String sKey;
	// User key. Empty if not found.
	private String userKey;

	public MapillaryImage(double latitude, double longitude, double ca, long capturedAt, String key, boolean pano, String sKey, String userKey) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.ca = ca;
		this.capturedAt = capturedAt;
		this.key = key;
		this.pano = pano;
		this.sKey = sKey;
		this.userKey = userKey;
	}

	public MapillaryImage(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public boolean setData(Map userData) {
		boolean res = true;
		try {
			this.ca = (Double) userData.get("ca");
			this.capturedAt = (Long) userData.get("captured_at");
			this.key = (String) userData.get("key");
			this.pano = ((Long) userData.get("pano")) == 1;
			this.sKey = (String) userData.get("skey");
			this.userKey = (String) userData.get("userkey");

		} catch (Exception e) {
			res = false;
		}
		return res && this.key != null;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getCa() {
		return ca;
	}

	public long getCapturedAt() {
		return capturedAt;
	}

	public String getKey() {
		return key;
	}

	public boolean isPano() {
		return pano;
	}

	public String getsKey() {
		return sKey;
	}

	public String getUserKey() {
		return userKey;
	}
}
