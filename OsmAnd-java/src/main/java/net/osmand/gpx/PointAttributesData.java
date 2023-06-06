package net.osmand.gpx;

import static net.osmand.gpx.GPXUtilities.POINT_ELEVATION;
import static net.osmand.gpx.GPXUtilities.POINT_SPEED;
import static net.osmand.gpx.GPXUtilities.SENSOR_TAG_CADENCE;
import static net.osmand.gpx.GPXUtilities.SENSOR_TAG_BIKE_POWER;
import static net.osmand.gpx.GPXUtilities.SENSOR_TAG_HEART_RATE;
import static net.osmand.gpx.GPXUtilities.SENSOR_TAG_SPEED;
import static net.osmand.gpx.GPXUtilities.SENSOR_TAG_TEMPERATURE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PointAttributesData<T extends PointAttribute> {

	private String key;
	private List<T> attributes = new ArrayList<>();
	private boolean hasData;

	public PointAttributesData(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public List<T> getAttributes() {
		return Collections.unmodifiableList(attributes);
	}

	public boolean hasData() {
		return hasData;
	}

	public void setHasData(boolean hasData) {
		this.hasData = hasData;
	}

	public void addPointAttribute(T attribute) {
		attributes.add(attribute);
	}

	public static String[] getSupportedDataTypes() {
		return new String[] {POINT_ELEVATION, POINT_SPEED, SENSOR_TAG_SPEED, SENSOR_TAG_HEART_RATE,
				SENSOR_TAG_BIKE_POWER, SENSOR_TAG_CADENCE, SENSOR_TAG_TEMPERATURE};
	}
}
