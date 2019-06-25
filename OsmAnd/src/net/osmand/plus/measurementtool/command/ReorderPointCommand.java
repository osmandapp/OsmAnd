package net.osmand.plus.measurementtool.command;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

import java.util.List;

public class ReorderPointCommand extends MeasurementModeCommand {

	private final int from;
	private final int to;

	public ReorderPointCommand(MeasurementToolLayer measurementLayer, int from, int to) {
		this.measurementLayer = measurementLayer;
		this.from = from;
		this.to = to;
	}

	@Override
	public boolean execute() {
		measurementLayer.getEditingCtx().setNeedUpdateCacheForSnap(true);
		measurementLayer.refreshMap();
		return true;
	}

	@Override
	public void undo() {
		reorder(from, to);
	}

	@Override
	public void redo() {
		reorder(to, from);
	}

	private void reorder(int addTo, int removeFrom) {
		List<WptPt> points = measurementLayer.getEditingCtx().getPoints();
		points.add(addTo, points.remove(removeFrom));
		measurementLayer.getEditingCtx().setNeedUpdateCacheForSnap(true);
		measurementLayer.refreshMap();
	}

	@Override
	MeasurementCommandType getType() {
		return MeasurementCommandType.REORDER_POINT;
	}
}
