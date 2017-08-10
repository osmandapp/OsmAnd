package net.osmand.plus.measurementtool.command;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

public class AddPointCommand implements Command {

	private final MeasurementToolLayer measurementLayer;
	private final int position;
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
		point = measurementLayer.addPoint(position);
		measurementLayer.refreshMap();
		return point != null;
	}

	@Override
	public void undo() {
		measurementLayer.getMeasurementPoints().remove(position);
		measurementLayer.refreshMap();
	}

	@Override
	public void redo() {
		measurementLayer.getMeasurementPoints().add(position, point);
		measurementLayer.refreshMap();
		measurementLayer.moveMapToPoint(position);
	}
}
