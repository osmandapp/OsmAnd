package net.osmand.plus.mapcontextmenu;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;

public class MapContextMenuData {

	private final LatLon latLon;
	private final PointDescription pointDescription;
	private final Object object;
	private final boolean backAction;

	public MapContextMenuData(LatLon latLon, PointDescription pointDescription, Object object,
			boolean backAction) {
		this.latLon = latLon;
		this.pointDescription = pointDescription;
		this.object = object;
		this.backAction = backAction;
	}

	public LatLon getLatLon() {
		return latLon;
	}

	public PointDescription getPointDescription() {
		return pointDescription;
	}

	public Object getObject() {
		return object;
	}

	public boolean hasBackAction() {
		return backAction;
	}
}
