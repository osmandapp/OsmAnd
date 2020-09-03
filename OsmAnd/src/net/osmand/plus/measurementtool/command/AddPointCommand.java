package net.osmand.plus.measurementtool.command;

import net.osmand.data.LatLon;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolLayer;
import net.osmand.plus.settings.backend.ApplicationMode;

public class AddPointCommand extends MeasurementModeCommand {

	private int position;
	private WptPt point;
	private boolean center;

	public AddPointCommand(MeasurementToolLayer measurementLayer, boolean center) {
		super(measurementLayer);
		init(measurementLayer, null, center);
	}

	public AddPointCommand(MeasurementToolLayer measurementLayer, LatLon latLon) {
		super(measurementLayer);
		init(measurementLayer, latLon, false);
	}

	private void init(MeasurementToolLayer measurementLayer, LatLon latLon, boolean center) {
		if (latLon != null) {
			point = new WptPt();
			point.lat = latLon.getLatitude();
			point.lon = latLon.getLongitude();
			ApplicationMode appMode = measurementLayer.getEditingCtx().getAppMode();
			if (appMode != MeasurementEditingContext.DEFAULT_APP_MODE) {
				point.setProfileType(appMode.getStringKey());
			}
		}
		this.center = center;
		position = measurementLayer.getEditingCtx().getPointsCount();
	}

	@Override
	public boolean execute() {
		if (point != null) {
			getEditingCtx().addPoint(point);
			measurementLayer.moveMapToPoint(position);
		} else if (center) {
			point = measurementLayer.addCenterPoint();
		} else {
			point = measurementLayer.addPoint();
		}
		refreshMap();
		return point != null;
	}

	@Override
	public void undo() {
		getEditingCtx().removePoint(position, true);
		refreshMap();
	}

	@Override
	public void redo() {
		getEditingCtx().addPoint(position, point);
		refreshMap();
		measurementLayer.moveMapToPoint(position);
	}

	@Override
	public MeasurementCommandType getType() {
		return MeasurementCommandType.ADD_POINT;
	}
}
