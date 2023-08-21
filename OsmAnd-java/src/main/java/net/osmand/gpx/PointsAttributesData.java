package net.osmand.gpx;

import java.util.ArrayList;
import java.util.List;

public class PointsAttributesData<T extends PointAttribute<?>> {

	private String key;
	private List<T> attributes = new ArrayList<>();
	private boolean hasData;

	public PointsAttributesData(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public List<T> getAttributes() {
		return attributes;
	}

	public T getPointAttribute(int index) {
		return attributes.get(index);
	}

	public boolean hasData() {
		return hasData;
	}

	public void setHasData(boolean hasData) {
		this.hasData = hasData;
	}

	public void addPointAttribute(T attribute) {
		attributes.add(attribute);

		if (!hasData() && attribute.hasValidValue()) {
			setHasData(true);
		}
	}
}
