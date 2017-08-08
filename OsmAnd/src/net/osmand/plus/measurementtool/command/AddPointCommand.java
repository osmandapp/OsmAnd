package net.osmand.plus.measurementtool.command;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

public class AddPointCommand implements Command {

	private MeasurementToolLayer measurementLayer;
	private int position;
	private WptPt point;

	public AddPointCommand(MeasurementToolLayer measurementLayer, int position) {
		this.measurementLayer = measurementLayer;
		this.position = position;
	}

	public AddPointCommand(MeasurementToolLayer measurementLayer) {
		this.measurementLayer = measurementLayer;
		position = measurementLayer.getPointsCount();
	}

	@Override
	public boolean execute() {
		return (point = measurementLayer.addPoint(position)) != null;
	}

	@Override
	public void undo() {
		measurementLayer.removePoint(position);
	}

	@Override
	public void redo() {
		measurementLayer.addPoint(position, point);
		measurementLayer.moveMapToPoint(position);
	}
}
