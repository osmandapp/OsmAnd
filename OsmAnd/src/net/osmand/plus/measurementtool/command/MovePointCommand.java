package net.osmand.plus.measurementtool.command;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

public class MovePointCommand implements Command {

	private final MeasurementToolLayer measurementLayer;
	private final WptPt oldPoint;
	private final WptPt newPoint;
	private final int position;

	public MovePointCommand (MeasurementToolLayer measurementLayer, WptPt oldPoint, WptPt newPoint, int position) {
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
		replacePointWithOldOne();
	}

	@Override
	public void redo() {
		replacePointWithNewOne();
	}

	private void replacePointWithOldOne() {
		measurementLayer.getMeasurementPoints().remove(position);
		measurementLayer.getMeasurementPoints().add(position, oldPoint);
	}

	private void replacePointWithNewOne() {
		measurementLayer.getMeasurementPoints().remove(position);
		measurementLayer.getMeasurementPoints().add(position, newPoint);
	}
}
