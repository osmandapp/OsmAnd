package net.osmand.plus.measurementtool.command;

import net.osmand.plus.GPXUtilities;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

import java.util.LinkedList;
import java.util.List;

public class ClearPointsCommand implements Command {

	private final MeasurementToolLayer measurementLayer;
	private List<GPXUtilities.WptPt> points;

	public ClearPointsCommand(MeasurementToolLayer measurementLayer) {
		this.measurementLayer = measurementLayer;
	}

	@Override
	public boolean execute() {
		List<GPXUtilities.WptPt> pts = measurementLayer.getMeasurementPoints();
		points = new LinkedList<>(pts);
		pts.clear();
		measurementLayer.refreshMap();
		return true;
	}

	@Override
	public void undo() {
		measurementLayer.getMeasurementPoints().addAll(points);
		measurementLayer.refreshMap();
	}

	@Override
	public void redo() {
		measurementLayer.getMeasurementPoints().clear();
		measurementLayer.refreshMap();
	}
}
