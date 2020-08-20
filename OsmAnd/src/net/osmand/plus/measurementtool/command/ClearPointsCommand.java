package net.osmand.plus.measurementtool.command;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

import java.util.ArrayList;
import java.util.List;

public class ClearPointsCommand extends MeasurementModeCommand {

	private List<WptPt> points;

	public ClearPointsCommand(MeasurementToolLayer measurementLayer) {
		super(measurementLayer);
	}

	@Override
	public boolean execute() {
		List<WptPt> pts = getEditingCtx().getPoints();
		points = new ArrayList<>(pts);
		pts.clear();
		getEditingCtx().clearSegments();
		refreshMap();
		return true;
	}

	@Override
	public void undo() {
		getEditingCtx().addPoints(points);
		getEditingCtx().updateCacheForSnap();
		refreshMap();
	}

	@Override
	public void redo() {
		getEditingCtx().clearSegments();
		refreshMap();
	}

	@Override
	MeasurementCommandType getType() {
		return MeasurementCommandType.CLEAR_POINTS;
	}
}
