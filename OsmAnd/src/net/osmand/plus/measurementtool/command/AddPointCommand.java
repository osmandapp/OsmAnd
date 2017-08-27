package net.osmand.plus.measurementtool.command;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

public class AddPointCommand extends MeasurementModeCommand {

	private final int position;
	private WptPt point;
	private final boolean center;

	public AddPointCommand(MeasurementToolLayer measurementLayer, boolean center) {
		this.measurementLayer = measurementLayer;
		this.center = center;
		position = measurementLayer.getEditingCtx().getPointsCount();
	}

	@Override
	public boolean execute() {
		if (center) {
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
