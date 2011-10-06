package net.osmand.data;

public class WayBoundary extends Boundary {

	public WayBoundary(boolean closedWay) {
		super(closedWay);
	}

	@Override
	public String toString() {
		return  getName() + " alevel:" + getAdminLevel() + " type: way closed:" + isClosedWay();
	}
}
