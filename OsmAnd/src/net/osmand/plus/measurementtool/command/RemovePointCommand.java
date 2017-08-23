package net.osmand.plus.measurementtool.command;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

public class RemovePointCommand extends MeasurementModeCommand {

	private final int position;
	private WptPt point;

	public RemovePointCommand(MeasurementToolLayer measurementLayer, int position) {
		this.measurementLayer = measurementLayer;
		this.position = position;
	}

	@Override
	public boolean execute() {
		point = measurementLayer.getEditingCtx().getPoints().remove(position);
		measurementLayer.refreshMap();
		return true;
	}

	@Override
	public void undo() {
		measurementLayer.getEditingCtx().getPoints().add(position, point);
		measurementLayer.refreshMap();
		measurementLayer.moveMapToPoint(position);
	}

	@Override
	public void redo() {
		measurementLayer.getEditingCtx().getPoints().remove(position);
		measurementLayer.refreshMap();
	}

	@Override
	MeasurementCommandType getType() {
		return MeasurementCommandType.REMOVE_POINT;
	}
}
