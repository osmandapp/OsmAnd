package net.osmand.plus.plugins.mapillary;

import java.util.Map;

public class MapillaryImage {

	public static final String CAPTURED_AT_KEY = "captured_at";
	public static final String COMPASS_ANGLE_KEY = "compass_angle";
	public static final String IMAGE_ID_KEY = "id";
	public static final String SEQUENCE_ID_KEY = "sequence_id";
	public static final String ORGANIZATION_ID_KEY = "organization_id";
	public static final String IS_PANORAMIC_KEY = "is_pano";

	// Image location
	private final double latitude;
	private final double longitude;
	// Camera heading.  -1 if not found.
	private double compassAngle = -1;
	// When the image was captured, expressed as UTC epoch time in milliseconds. Must be non-negative integer;  0 if not found.
	private long capturedAt;
	private String imageId;
	private boolean panoramicImage;
	private String sequenceId;
	// Can be absent
	private String organizationId;

	public MapillaryImage(double latitude, double longitude, double compassAngle, long capturedAt,
	                      String imageId, boolean panoramicImage, String sequenceId, String organizationId) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.compassAngle = compassAngle;
		this.capturedAt = capturedAt;
		this.imageId = imageId;
		this.panoramicImage = panoramicImage;
		this.sequenceId = sequenceId;
		this.organizationId = organizationId;
	}

	public MapillaryImage(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public boolean setData(Map userData) {
		boolean res = true;
		try {
			this.capturedAt = ((Number) userData.get(CAPTURED_AT_KEY)).longValue();
			this.compassAngle = ((Number) userData.get(COMPASS_ANGLE_KEY)).doubleValue();
			this.imageId = userData.get(IMAGE_ID_KEY).toString();
			this.sequenceId = (String) userData.get(SEQUENCE_ID_KEY);
			if (userData.get(ORGANIZATION_ID_KEY) != null) {
				this.organizationId = userData.get(ORGANIZATION_ID_KEY).toString();
			}
			this.panoramicImage = (boolean) userData.get(IS_PANORAMIC_KEY);
		} catch (Exception e) {
			res = false;
		}
		return res && this.imageId != null;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getCompassAngle() {
		return compassAngle;
	}

	public long getCapturedAt() {
		return capturedAt;
	}

	public String getImageId() {
		return imageId;
	}

	public boolean isPanoramicImage() {
		return panoramicImage;
	}

	public String getSKey() {
		return sequenceId;
	}

	public String getOrganizationId() {
		return organizationId;
	}
}