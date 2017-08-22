package net.osmand.plus.measurementtool.command;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

public class MovePointCommand extends MeasurementModeCommand {

	private final WptPt oldPoint;
	private final WptPt newPoint;
	private final int position;

	public MovePointCommand(MeasurementToolLayer measurementLayer, WptPt oldPoint, WptPt newPoint, int position) {
		this.measurementLayer = measurementLayer;
		this.oldPoint = oldPoint;
		this.newPoint = newPoint;
		this.position = position;
	}

	@Override
	public boolean execute() {
		return true;
	}

	@Override
	public void undo() {
		measurementLayer.getMeasurementPoints().remove(position);
		measurementLayer.getMeasurementPoints().add(position, oldPoint);
		measurementLayer.refreshMap();
	}

	@Override
	public void redo() {
		measurementLayer.getMeasurementPoints().remove(position);
		measurementLayer.getMeasurementPoints().add(position, newPoint);
		measurementLayer.refreshMap();
	}

	@Override
	MeasurementCommandType getType() {
		return MeasurementCommandType.MOVE_POINT;
	}
}
