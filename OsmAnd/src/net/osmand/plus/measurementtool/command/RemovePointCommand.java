package net.osmand.plus.measurementtool.command;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

public class RemovePointCommand extends MeasurementModeCommand {

	private final int position;
	private WptPt point;

	public RemovePointCommand(MeasurementToolLayer measurementLayer, int position) {
		super(measurementLayer);
		this.position = position;
	}

	@Override
	public boolean execute() {
		point = getEditingCtx().removePoint(position, true);
		refreshMap();
		return true;
	}

	@Override
	public void undo() {
		getEditingCtx().addPoint(position, point);
		refreshMap();
		measurementLayer.moveMapToPoint(position);
	}

	@Override
	public void redo() {
		getEditingCtx().removePoint(position, true);
		refreshMap();
	}

	@Override
	MeasurementCommandType getType() {
		return MeasurementCommandType.REMOVE_POINT;
	}
}
