package net.osmand.plus.measurementtool.command;

import android.util.Pair;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.RoadSegmentData;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JoinPointsCommand extends MeasurementModeCommand {

	private List<WptPt> points;
	private Map<Pair<WptPt, WptPt>, RoadSegmentData> roadSegmentData;
	private int pointPosition;

	public JoinPointsCommand(MeasurementToolLayer measurementLayer) {
		super(measurementLayer);
	}

	@Override
	public boolean execute() {
		pointPosition = getEditingCtx().getSelectedPointPosition();
		executeCommand();
		return true;
	}

	private void executeCommand() {
		MeasurementEditingContext ctx = getEditingCtx();
		List<WptPt> pts = ctx.getPoints();
		points = new ArrayList<>(pts);
		roadSegmentData = ctx.getRoadSegmentData();
		ctx.joinPoints(pointPosition);
		refreshMap();
	}

	@Override
	public void undo() {
		MeasurementEditingContext ctx = getEditingCtx();
		ctx.clearSegments();
		ctx.setRoadSegmentData(roadSegmentData);
		ctx.addPoints(points);
		refreshMap();
	}

	@Override
	public void redo() {
		executeCommand();
	}

	@Override
	public MeasurementCommandType getType() {
		return MeasurementCommandType.JOIN_POINTS;
	}
}