package net.osmand.plus.measurementtool.command;

import net.osmand.data.LatLon;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

public class AddPointCommand extends MeasurementModeCommand {

	private int position;
	private WptPt point;
	private boolean center;

	public AddPointCommand(MeasurementToolLayer measurementLayer, boolean center) {
		init(measurementLayer, null, center);
	}

	public AddPointCommand(MeasurementToolLayer measurementLayer, LatLon latLon) {
		init(measurementLayer, latLon, false);
	}

	private void init(MeasurementToolLayer measurementLayer, LatLon latLon, boolean center) {
		this.measurementLayer = measurementLayer;
		if (latLon != null) {
			point = new WptPt();
			point.lat = latLon.getLatitude();
			point.lon = latLon.getLongitude();
			point.setProfileType(measurementLayer.getEditingCtx().getSnapToRoadAppMode().getStringKey());
		}
		this.center = center;
		position = measurementLayer.getEditingCtx().getPointsCount();
	}

	@Override
	public boolean execute() {
		if (point != null) {
			measurementLayer.getEditingCtx().addPoint(point);
			measurementLayer.moveMapToPoint(position);
		} else if (center) {
			point = measurementLayer.addCenterPoint();
		} else {
			point = measurementLayer.addPoint();
		}
		measurementLayer.refreshMap();
		return point != null;
	}

	@Override
	public void undo() {
		measurementLayer.getEditingCtx().removePoint(position, true);
		measurementLayer.refreshMap();
	}

	@Override
	public void redo() {
		measurementLayer.getEditingCtx().addPoint(position, point);
		measurementLayer.refreshMap();
		measurementLayer.moveMapToPoint(position);
	}

	@Override
	MeasurementCommandType getType() {
		return MeasurementCommandType.ADD_POINT;
	}
}
