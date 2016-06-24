package net.osmand.core.samples.android.sample1.search.objects;

import net.osmand.core.jni.PointI;

public abstract class SearchPositionObject extends SearchObject {

	private double distance;

	public SearchPositionObject(SearchObjectType type, Object internalObject) {
		super(type, internalObject);
	}

	public abstract PointI getPosition31();

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	@Override
	public String toString() {
		return "SearchPositionObject: " + getNativeName()
				+ " {x31:" + getPosition31().getX() + " y31: " + getPosition31().getY() + "}";
	}

}
