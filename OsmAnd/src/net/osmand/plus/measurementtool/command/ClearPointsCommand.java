package net.osmand.plus.measurementtool.command;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

import java.util.LinkedList;
import java.util.List;

public class ClearPointsCommand extends MeasurementModeCommand {

	private List<WptPt> points;

	public ClearPointsCommand(MeasurementToolLayer measurementLayer) {
		this.measurementLayer = measurementLayer;
	}

	@Override
	public boolean execute() {
		List<WptPt> pts = measurementLayer.getEditingCtx().getPoints();
		points = new LinkedList<>(pts);
		pts.clear();
		measurementLayer.refreshMap();
		return true;
	}

	@Override
	public void undo() {
		measurementLayer.getEditingCtx().getPoints().addAll(points);
		measurementLayer.refreshMap();
	}

	@Override
	public void redo() {
		measurementLayer.getEditingCtx().getPoints().clear();
		measurementLayer.refreshMap();
	}

	@Override
	MeasurementCommandType getType() {
		return MeasurementCommandType.CLEAR_POINTS;
	}
}
