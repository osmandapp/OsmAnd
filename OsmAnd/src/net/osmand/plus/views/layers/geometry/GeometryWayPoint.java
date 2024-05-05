package net.osmand.plus.views.layers.geometry;

public class GeometryWayPoint {
	public int index;
	public float tx, ty;
	public float height;
	public int tx31, ty31;
	public double angle;
	public double distance;
	public GeometryWayStyle<?> style;

	public GeometryWayPoint() {

	}

	public GeometryWayPoint(int ind, float tx, float ty) {
		this.index = ind;
		this.tx = tx;
		this.ty = ty;
	}
}