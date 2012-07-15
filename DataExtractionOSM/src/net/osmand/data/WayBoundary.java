package net.osmand.data;

public class WayBoundary extends Boundary {

	public WayBoundary() {
		super();
	}

	@Override
	public String toString() {
		return  getName() + " alevel:" + getAdminLevel() + " type: way closed:" + !hasOpenedPolygons();
	}
}
