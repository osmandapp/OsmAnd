package net.osmand.plus.measurementtool.command;

import android.util.Pair;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.RoadSegmentData;
import net.osmand.plus.measurementtool.MeasurementToolLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SplitPointsCommand extends MeasurementModeCommand {

	private boolean after;
	private List<WptPt> points;
	private Map<Pair<WptPt, WptPt>, RoadSegmentData> roadSegmentData;
	private int pointPosition;

	public SplitPointsCommand(MeasurementToolLayer measurementLayer, boolean after) {
		super(measurementLayer);
		this.after = after;
		MeasurementEditingContext editingCtx = getEditingCtx();
		this.pointPosition = editingCtx.getSelectedPointPosition();
		if (this.pointPosition == -1) {
			this.after = true;
			this.pointPosition = editingCtx.getPoints().size() - 1;
		}
	}

	@Override
	public boolean execute() {
		executeCommand();
		return true;
	}

	private void executeCommand() {
		MeasurementEditingContext ctx = getEditingCtx();
		List<WptPt> pts = ctx.getPoints();
		points = new ArrayList<>(pts);
		roadSegmentData = ctx.getRoadSegmentData();
		ctx.splitPoints(pointPosition, after);
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
		return MeasurementCommandType.SPLIT_POINTS;
	}
}
