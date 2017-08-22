package net.osmand.plus.measurementtool.command;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

public class AddPointCommand extends MeasurementModeCommand {

	private final int position;
	private WptPt point;
	private boolean center;

	public AddPointCommand(MeasurementToolLayer measurementLayer, int position) {
		this.measurementLayer = measurementLayer;
		this.center = true;
		this.position = position;
	}

	public AddPointCommand(MeasurementToolLayer measurementLayer, boolean center) {
		this.measurementLayer = measurementLayer;
		this.center = center;
		position = measurementLayer.getMeasurementPoints().size();
	}

	@Override
	public boolean execute() {
		if (center) {
			point = measurementLayer.addCenterPoint(position);
		} else {
			point = measurementLayer.addPoint(position);
		}
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
