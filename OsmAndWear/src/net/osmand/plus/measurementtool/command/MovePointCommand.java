package net.osmand.plus.measurementtool.command;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

public class MovePointCommand extends MeasurementModeCommand {

	private final WptPt oldPoint;
	private final WptPt newPoint;
	private final int position;

	public MovePointCommand(@NonNull MeasurementToolLayer measurementLayer, WptPt oldPoint, WptPt newPoint, int position) {
		super(measurementLayer);
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
		getEditingCtx().removePoint(position, false);
		getEditingCtx().addPoint(position, oldPoint);
		refreshMap();
	}

	@Override
	public void redo() {
		getEditingCtx().removePoint(position, false);
		getEditingCtx().addPoint(position, newPoint);
		refreshMap();
	}

	@NonNull
	@Override
	public MeasurementCommandType getType() {
		return MeasurementCommandType.MOVE_POINT;
	}
}
