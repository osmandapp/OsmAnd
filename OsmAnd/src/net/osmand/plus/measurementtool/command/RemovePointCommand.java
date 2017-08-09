package net.osmand.plus.measurementtool.command;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

public class RemovePointCommand implements Command {

	private final MeasurementToolLayer measurementLayer;
	private final int position;
	private WptPt point;

	public RemovePointCommand(MeasurementToolLayer measurementLayer, int position) {
		this.measurementLayer = measurementLayer;
		this.position = position;
	}

	@Override
	public boolean execute() {
		point = measurementLayer.removePoint(position);
		return true;
	}

	@Override
	public void undo() {
		measurementLayer.addPoint(position, point);
		measurementLayer.moveMapToPoint(position);
	}

	@Override
	public void redo() {
		measurementLayer.removePoint(position);
	}
}
