package net.osmand.plus.measurementtool.command;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

import java.util.List;

public class SnapToRoadCommand extends MeasurementModeCommand {

	private List<WptPt> snappedPoints;

	public SnapToRoadCommand(MeasurementToolLayer measurementLayer, List<WptPt> points) {
		this.measurementLayer = measurementLayer;
		this.snappedPoints = points;
	}

	@Override
	public boolean execute() {
//		measurementLayer.getSnappedToRoadPoints().clear();
//		measurementLayer.getSnappedToRoadPoints().addAll(snappedPoints);
//		measurementLayer.refreshMap();
		return true;
	}

	@Override
	public void undo() {
//		measurementLayer.getSnappedToRoadPoints().clear();
		measurementLayer.refreshMap();
	}

	@Override
	public void redo() {
//		measurementLayer.getSnappedToRoadPoints().addAll(snappedPoints);
		measurementLayer.refreshMap();
	}

	@Override
	MeasurementCommandType getType() {
		return MeasurementCommandType.SNAP_TO_ROAD;
	}
}
