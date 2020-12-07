package net.osmand.plus.measurementtool.command;

import android.util.Pair;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementEditingContext.RoadSegmentData;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChangeProfileConfigCommand extends MeasurementModeCommand {

	private List<WptPt> oldPoints;
	private Map<Pair<WptPt, WptPt>, RoadSegmentData> oldRoadSegmentData;

	public ChangeProfileConfigCommand(MeasurementToolLayer measurementLayer) {
		super(measurementLayer);
	}

	@Override
	public boolean execute() {
		MeasurementEditingContext editingCtx = getEditingCtx();
		oldPoints = new ArrayList<>(editingCtx.getPoints());
		oldRoadSegmentData = editingCtx.getRoadSegmentData();
		executeCommand();
		return true;
	}

	@Override
	public void undo() {
		MeasurementEditingContext editingCtx = getEditingCtx();
		editingCtx.getPoints().clear();
		editingCtx.addPoints(oldPoints);
		editingCtx.setRoadSegmentData(oldRoadSegmentData);
		editingCtx.updateSegmentsForSnap();
	}

	@Override
	public void redo() {
		executeCommand();
	}

	@Override
	public MeasurementCommandType getType() {
		return MeasurementCommandType.CHANGE_PROFILE_CONFIG;
	}

	private void executeCommand() {
		MeasurementEditingContext editingCtx = getEditingCtx();
		editingCtx.clearRouteSegmentsByAppMode();
		editingCtx.updateSegmentsForSnap();
	}
}